package com.robertomr.tally.framework.ai.modelo

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import com.robertomr.tally.domain.expenses.data.DownloadConditions
import com.robertomr.tally.domain.expenses.data.ModelDownloader
import com.robertomr.tally.domain.expenses.entities.EstadoModelo
import com.robertomr.tally.domain.expenses.entities.ModeloLocal
import com.robertomr.tally.domain.expenses.entities.MotivoFallo
import com.robertomr.tally.domain.expenses.entities.MotivoPausa
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

/**
 * [ModelDownloader] sobre WorkManager (spec descarga-modelo): el estado sale de
 * combinar el `WorkInfo` del trabajo unico, el sistema de ficheros (un modelo ya
 * presente cuenta como Descargado, R-DM4) y los cambios de red.
 */
internal class WorkManagerModelDownloader(
    private val context: Context,
    private val conditions: DownloadConditions,
) : ModelDownloader {

    private val workManager: WorkManager by lazy { WorkManager.getInstance(context) }
    private val refresco = MutableStateFlow(0)

    override val estado: Flow<EstadoModelo> by lazy {
        combine(
            workManager.getWorkInfosForUniqueWorkFlow(ModelDownloadWorker.NOMBRE_TRABAJO),
            refresco,
            cambiosDeRed(),
        ) { infos, _, _ -> calcular(infos.lastOrNull()) }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
    }

    override suspend fun iniciar() {
        val peticion = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED).build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SEGUNDOS, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(
            ModelDownloadWorker.NOMBRE_TRABAJO,
            ExistingWorkPolicy.REPLACE,
            peticion,
        ).await()
        refresco.value++
    }

    override suspend fun cancelar() {
        workManager.cancelUniqueWork(ModelDownloadWorker.NOMBRE_TRABAJO).await()
        // El worker detecta la cancelacion en el siguiente bloque; margen breve antes de
        // descartar el parcial (D5: cancelar descarta el .part).
        delay(ESPERA_CANCELACION_MS)
        // Descarta trabajos terminados: un FAILED antiguo no debe reaparecer como "Error".
        workManager.pruneWork().await()
        withContext(Dispatchers.IO) { ModelPaths.archivoParcial(context).delete() }
        refresco.value++
    }

    override suspend fun eliminar() {
        workManager.cancelUniqueWork(ModelDownloadWorker.NOMBRE_TRABAJO).await()
        delay(ESPERA_CANCELACION_MS)
        // Descarta trabajos terminados: un FAILED antiguo no debe reaparecer como "Error".
        workManager.pruneWork().await()
        withContext(Dispatchers.IO) {
            ModelPaths.archivoFinal(context).delete()
            ModelPaths.archivoParcial(context).delete()
        }
        refresco.value++
    }

    override fun rutaModelo(): String? {
        val f = ModelPaths.archivoFinal(context)
        return if (f.isFile && f.length() > 0) f.absolutePath else null
    }

    private fun calcular(info: WorkInfo?): EstadoModelo {
        val final = ModelPaths.archivoFinal(context)
        if (final.isFile && final.length() > 0) return EstadoModelo.Descargado(final.length())

        val parcial = ModelPaths.archivoParcial(context)
        val bytesParcial = if (parcial.isFile) parcial.length() else 0L
        val esperado = ModeloLocal.TAMANO_ESPERADO_BYTES
        return when (info?.state) {
            WorkInfo.State.RUNNING -> EstadoModelo.Descargando(
                bytes = info.progress.getLong(ModelDownloadWorker.CLAVE_BYTES, bytesParcial),
                total = info.progress.getLong(ModelDownloadWorker.CLAVE_TOTAL, esperado),
            )
            WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED ->
                if (conditions.esRedSinCoste()) {
                    EstadoModelo.Descargando(bytesParcial, esperado)
                } else {
                    EstadoModelo.EnPausa(bytesParcial, esperado, MotivoPausa.EsperandoWifi)
                }
            WorkInfo.State.FAILED -> EstadoModelo.Fallo(
                motivo = motivoDe(info.outputData.getString(ModelDownloadWorker.CLAVE_MOTIVO)),
                bytes = bytesParcial,
            )
            WorkInfo.State.SUCCEEDED, WorkInfo.State.CANCELLED, null -> EstadoModelo.NoDescargado
        }
    }

    private fun motivoDe(valor: String?): MotivoFallo = when (valor) {
        ModelDownloadWorker.MOTIVO_AUTORIZACION -> MotivoFallo.Autorizacion
        ModelDownloadWorker.MOTIVO_INTEGRIDAD -> MotivoFallo.Integridad
        ModelDownloadWorker.MOTIVO_SIN_ESPACIO -> MotivoFallo.SinEspacio
        else -> MotivoFallo.Red
    }

    /** Emite al cambiar la red activa para que "En pausa"/"Descargando" no se queden viejos. */
    private fun cambiosDeRed(): Flow<Unit> = callbackFlow {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(Unit)
            }

            override fun onLost(network: Network) {
                trySend(Unit)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                trySend(Unit)
            }
        }
        cm.registerDefaultNetworkCallback(callback)
        awaitClose { cm.unregisterNetworkCallback(callback) }
    }.onStart { emit(Unit) }

    private companion object {
        const val BACKOFF_SEGUNDOS = 30L
        const val ESPERA_CANCELACION_MS = 400L
    }
}

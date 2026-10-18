package com.robertomr.tally.framework.ai.modelo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.robertomr.tally.domain.expenses.entities.ModeloLocal
import java.io.IOException
import kotlinx.coroutines.CancellationException

/**
 * Trabajo unico de descarga del modelo (spec descarga-modelo). Construye el
 * [ModelDownloadEngine] desde `applicationContext` (sin `hilt-work`).
 *
 * - Constraint de red no medida (la fija quien lo encola): si se pierde el WiFi
 *   WorkManager detiene el trabajo y lo reanuda al volver, conservando el `.part`.
 * - `IOException` -> `Result.retry()` con backoff exponencial, maximo
 *   [MAX_INTENTOS] intentos; despues fallo `Red`.
 * - Servicio en primer plano `dataSync` con notificacion de progreso. Si el
 *   sistema no permite arrancarlo (Android 12+ en segundo plano) o las
 *   notificaciones estan denegadas, la descarga continua sin notificacion (R-DM5).
 * - Nunca se escribe el token en logs.
 */
internal class ModelDownloadWorker(
    contexto: Context,
    parametros: WorkerParameters,
) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        val destino = ModelPaths.archivoFinal(applicationContext)
        if (destino.isFile && destino.length() > 0) return Result.success()

        val token = FileModelAccessTokenStore(applicationContext).leer()
        val motor = ModelDownloadEngine(
            url = ModelPaths.URL_MODELO,
            destino = destino,
            token = token,
            log = { Log.i(TAG, it) },
        )
        publicarForeground(0L, ModeloLocal.TAMANO_ESPERADO_BYTES)

        return try {
            val resultado = motor.descargar { bytes, total ->
                val totalMostrado = if (total > 0) total else ModeloLocal.TAMANO_ESPERADO_BYTES
                setProgress(workDataOf(CLAVE_BYTES to bytes, CLAVE_TOTAL to totalMostrado))
                publicarForeground(bytes, totalMostrado)
            }
            when (resultado) {
                is ResultadoMotor.Completado -> {
                    Log.i(TAG, "Descarga completa: ${resultado.bytes} bytes (inicio en ${motor.bytesIniciales})")
                    Result.success()
                }
                ResultadoMotor.Autorizacion -> fallo(MOTIVO_AUTORIZACION)
                is ResultadoMotor.Integridad -> {
                    Log.w(TAG, "Integridad: esperado=${resultado.esperado} obtenido=${resultado.obtenido}")
                    fallo(MOTIVO_INTEGRIDAD)
                }
                is ResultadoMotor.ErrorHttp -> {
                    Log.w(TAG, "HTTP ${resultado.codigo}")
                    fallo(MOTIVO_RED)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.w(TAG, "Corte de descarga (intento $runAttemptCount): ${e.javaClass.simpleName}")
            val msg = e.message.orEmpty()
            when {
                msg.contains("ENOSPC") || msg.contains("No space left", ignoreCase = true) ->
                    fallo(MOTIVO_SIN_ESPACIO)
                runAttemptCount + 1 >= MAX_INTENTOS -> fallo(MOTIVO_RED)
                else -> Result.retry()
            }
        }
    }

    private fun fallo(motivo: String): Result = Result.failure(workDataOf(CLAVE_MOTIVO to motivo))

    private suspend fun publicarForeground(bytes: Long, total: Long) {
        try {
            setForeground(infoForeground(bytes, total))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            // Android 12+ puede negar el servicio en primer plano (R-DM5): seguimos sin el.
            Log.w(TAG, "Sin servicio en primer plano: ${e.javaClass.simpleName}")
        }
    }

    private fun infoForeground(bytes: Long, total: Long): ForegroundInfo {
        val gestor = applicationContext.getSystemService(NotificationManager::class.java)
        gestor.createNotificationChannel(
            NotificationChannel(CANAL, "Descarga del modelo", NotificationManager.IMPORTANCE_LOW),
        )
        val porcentaje = if (total > 0) ((bytes * 100) / total).toInt().coerceIn(0, 100) else 0
        val notificacion = NotificationCompat.Builder(applicationContext, CANAL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Descargando modelo local")
            .setContentText("$porcentaje %")
            .setProgress(100, porcentaje, total <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(ID_NOTIFICACION, notificacion, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(ID_NOTIFICACION, notificacion)
        }
    }

    companion object {
        const val NOMBRE_TRABAJO = "modelo-descarga"
        const val CLAVE_BYTES = "bytes"
        const val CLAVE_TOTAL = "total"
        const val CLAVE_MOTIVO = "motivo"
        const val MOTIVO_AUTORIZACION = "Autorizacion"
        const val MOTIVO_INTEGRIDAD = "Integridad"
        const val MOTIVO_RED = "Red"
        const val MOTIVO_SIN_ESPACIO = "SinEspacio"
        const val MAX_INTENTOS = 5

        private const val TAG = "ModeloDescarga"
        private const val CANAL = "modelo_descarga"
        private const val ID_NOTIFICACION = 4101
    }
}

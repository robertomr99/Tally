package com.robertomr.tally.domain.expenses.data

import com.robertomr.tally.domain.expenses.entities.EstadoModelo
import kotlinx.coroutines.flow.Flow

/**
 * Descarga del modelo local (spec descarga-modelo). La implementacion vive en
 * `framework/ai`; el dominio solo conoce este contrato. Esta interfaz NO carga
 * ni ejecuta el modelo: solo expone [estado] y [rutaModelo].
 */
interface ModelDownloader {
    val estado: Flow<EstadoModelo>

    /** Encola la descarga (sin comprobar WiFi ni espacio: eso es de `DescargarModelo`). */
    suspend fun iniciar()

    /** Cancela la descarga en curso y descarta el parcial. */
    suspend fun cancelar()

    /** Borra el modelo descargado (y cualquier parcial). */
    suspend fun eliminar()

    /** Ruta absoluta del modelo, o `null` si no esta [EstadoModelo.Descargado]. */
    fun rutaModelo(): String?
}

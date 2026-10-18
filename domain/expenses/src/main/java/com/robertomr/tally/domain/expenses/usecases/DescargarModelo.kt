package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.data.DownloadConditions
import com.robertomr.tally.domain.expenses.data.ModelDownloader
import com.robertomr.tally.domain.expenses.entities.ModeloLocal
import com.robertomr.tally.domain.expenses.entities.ResultadoDescarga
import javax.inject.Inject

/**
 * Pide la descarga del modelo local (spec descarga-modelo). Orden de
 * comprobaciones: primero la politica WiFi (sin red no medida no se encola nada
 * ni se abre conexion), despues el espacio (resto por descargar + margen) y por
 * ultimo [ModelDownloader.iniciar].
 */
class DescargarModelo @Inject constructor(
    private val downloader: ModelDownloader,
    private val conditions: DownloadConditions,
) {
    suspend operator fun invoke(): ResultadoDescarga {
        if (!conditions.esRedSinCoste()) return ResultadoDescarga.RequiereWifi

        val restante = (ModeloLocal.TAMANO_ESPERADO_BYTES - conditions.bytesYaDescargados())
            .coerceAtLeast(0L)
        val necesarios = restante + ModeloLocal.MARGEN_ESPACIO_BYTES
        val libres = conditions.espacioLibreBytes()
        if (libres < necesarios) return ResultadoDescarga.EspacioInsuficiente(necesarios, libres)

        downloader.iniciar()
        return ResultadoDescarga.Iniciada
    }
}

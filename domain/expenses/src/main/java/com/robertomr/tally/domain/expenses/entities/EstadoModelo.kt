package com.robertomr.tally.domain.expenses.entities

/**
 * Estado del modelo local (spec descarga-modelo). Lo expone `ModelDownloader`
 * y lo consumen Ajustes y, mas adelante, `extraccion-local`/`router-automatico`.
 */
sealed interface EstadoModelo {
    data object NoDescargado : EstadoModelo

    data class Descargando(val bytes: Long, val total: Long) : EstadoModelo

    data class EnPausa(val bytes: Long, val total: Long, val motivo: MotivoPausa) : EstadoModelo

    data class Descargado(val bytes: Long) : EstadoModelo

    data class Fallo(val motivo: MotivoFallo, val bytes: Long) : EstadoModelo
}

/** Por que una descarga esta en pausa (solo automatica; no hay pausa manual). */
enum class MotivoPausa { EsperandoWifi }

/** Causa de un fallo definitivo de la descarga. */
enum class MotivoFallo { Autorizacion, Integridad, Red, SinEspacio }

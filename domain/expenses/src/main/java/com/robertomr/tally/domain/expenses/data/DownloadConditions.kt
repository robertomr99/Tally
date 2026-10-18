package com.robertomr.tally.domain.expenses.data

/** Condiciones del dispositivo que gobiernan la descarga (spec descarga-modelo). */
interface DownloadConditions {
    /** `true` si la red activa es no medida (WiFi). */
    fun esRedSinCoste(): Boolean

    /** Bytes libres donde se guarda el modelo. */
    fun espacioLibreBytes(): Long

    /** Bytes ya descargados del parcial (0 si no hay). */
    fun bytesYaDescargados(): Long
}

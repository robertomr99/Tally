package com.robertomr.tally.test.unit

import com.robertomr.tally.domain.expenses.data.DownloadConditions

/** Doble de [DownloadConditions] (spec descarga-modelo) con valores editables. */
class FakeDownloadConditions(
    var redSinCoste: Boolean = true,
    var espacioLibre: Long = Long.MAX_VALUE,
    var bytesYaDescargados: Long = 0L,
) : DownloadConditions {
    override fun esRedSinCoste(): Boolean = redSinCoste

    override fun espacioLibreBytes(): Long = espacioLibre

    override fun bytesYaDescargados(): Long = bytesYaDescargados
}

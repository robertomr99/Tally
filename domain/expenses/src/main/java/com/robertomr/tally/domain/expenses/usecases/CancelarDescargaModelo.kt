package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.data.ModelDownloader
import javax.inject.Inject

/** Cancela la descarga del modelo y descarta el parcial (spec descarga-modelo, D5). */
class CancelarDescargaModelo @Inject constructor(
    private val downloader: ModelDownloader,
) {
    suspend operator fun invoke() = downloader.cancelar()
}

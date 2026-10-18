package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.data.ModelDownloader
import javax.inject.Inject

/** Borra el modelo local descargado (la confirmacion es de la UI). */
class EliminarModelo @Inject constructor(
    private val downloader: ModelDownloader,
) {
    suspend operator fun invoke() = downloader.eliminar()
}

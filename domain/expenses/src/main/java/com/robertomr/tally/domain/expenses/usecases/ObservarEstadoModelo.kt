package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.data.ModelDownloader
import com.robertomr.tally.domain.expenses.entities.EstadoModelo
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Observa el estado del modelo local (spec descarga-modelo). */
class ObservarEstadoModelo @Inject constructor(
    private val downloader: ModelDownloader,
) {
    operator fun invoke(): Flow<EstadoModelo> = downloader.estado
}

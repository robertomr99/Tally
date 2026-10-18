package com.robertomr.tally.test.unit

import com.robertomr.tally.domain.expenses.data.ModelDownloader
import com.robertomr.tally.domain.expenses.entities.EstadoModelo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Doble de [ModelDownloader] (spec descarga-modelo): estado editable y contadores. */
class FakeModelDownloader(
    initial: EstadoModelo = EstadoModelo.NoDescargado,
    var ruta: String? = null,
) : ModelDownloader {

    val estadoFlow = MutableStateFlow(initial)
    override val estado: Flow<EstadoModelo> = estadoFlow

    var iniciarCount = 0
        private set
    var cancelarCount = 0
        private set
    var eliminarCount = 0
        private set

    override suspend fun iniciar() {
        iniciarCount++
    }

    override suspend fun cancelar() {
        cancelarCount++
    }

    override suspend fun eliminar() {
        eliminarCount++
    }

    override fun rutaModelo(): String? = ruta
}

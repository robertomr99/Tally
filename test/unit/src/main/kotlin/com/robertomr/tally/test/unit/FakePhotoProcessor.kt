package com.robertomr.tally.test.unit

import com.robertomr.tally.domain.expenses.data.CropRegion
import com.robertomr.tally.domain.expenses.data.PhotoProcessor
import kotlinx.coroutines.CompletableDeferred

/**
 * Doble compartido de [PhotoProcessor] (spec captura-foto + captura-recorte-encuadre):
 * registra los bytes y el recorte recibidos y devuelve una salida configurable.
 * [gate] permite retener el procesado para observar el estado "Procesando" en
 * tests de ViewModel.
 */
class FakePhotoProcessor(
    var processedBytes: ByteArray = byteArrayOf(),
    var error: Throwable? = null,
) : PhotoProcessor {

    val received = mutableListOf<ByteArray>()
    val receivedCrops = mutableListOf<CropRegion>()

    /** Si se asigna, [process] espera a que se complete antes de responder. */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun process(rawJpeg: ByteArray, crop: CropRegion): ByteArray {
        received += rawJpeg
        receivedCrops += crop
        gate?.await()
        error?.let { throw it }
        return processedBytes
    }
}

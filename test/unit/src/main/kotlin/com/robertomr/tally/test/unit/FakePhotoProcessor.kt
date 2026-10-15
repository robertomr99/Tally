package com.robertomr.tally.test.unit

import com.robertomr.tally.domain.expenses.data.PhotoProcessor
import kotlinx.coroutines.CompletableDeferred

/**
 * Doble compartido de [PhotoProcessor] (spec captura-foto): registra los bytes
 * recibidos y devuelve una salida configurable. [gate] permite retener el
 * procesado para observar el estado "Procesando" en tests de ViewModel.
 */
class FakePhotoProcessor(
    var processedBytes: ByteArray = byteArrayOf(),
    var error: Throwable? = null,
) : PhotoProcessor {

    val received = mutableListOf<ByteArray>()

    /** Si se asigna, [process] espera a que se complete antes de responder. */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun process(rawJpeg: ByteArray): ByteArray {
        received += rawJpeg
        gate?.await()
        error?.let { throw it }
        return processedBytes
    }
}

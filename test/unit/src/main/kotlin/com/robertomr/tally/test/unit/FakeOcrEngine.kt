package com.robertomr.tally.test.unit

import com.robertomr.tally.domain.expenses.data.OcrEngine
import kotlinx.coroutines.CompletableDeferred

/**
 * Doble compartido de [OcrEngine] (spec ocr-mlkit): registra las invocaciones
 * (bytes recibidos) y devuelve un texto crudo configurable, o lanza un error
 * configurable. [gate] permite retener el OCR para observar el estado
 * "Procesando" en tests de ViewModel.
 */
class FakeOcrEngine(
    var result: String = "",
    var error: Throwable? = null,
) : OcrEngine {

    val received = mutableListOf<ByteArray>()

    /** Si se asigna, [recognize] espera a que se complete antes de responder. */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun recognize(image: ByteArray): String {
        received += image
        gate?.await()
        error?.let { throw it }
        return result
    }
}

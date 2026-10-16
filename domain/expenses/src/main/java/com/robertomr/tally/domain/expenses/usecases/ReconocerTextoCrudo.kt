package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.data.OcrEngine
import com.robertomr.tally.domain.expenses.data.PhotoFileStore
import javax.inject.Inject

/**
 * Reconoce el texto crudo del ticket de la foto ya guardada (spec ocr-mlkit,
 * etapa 1 del ADR 0002): lee la foto con [PhotoFileStore.read] y corre el
 * [OcrEngine] sobre sus bytes JPEG.
 *
 * Politica de errores: la foto ausente no es un error (se devuelve `""`); los
 * errores del motor se propagan al llamador (la politica de "no bloquear la
 * captura" vive en la presentacion). Una [kotlinx.coroutines.CancellationException]
 * jamas se convierte en resultado: este caso de uso no captura excepciones.
 *
 * El texto devuelto se registra con el Borrador (en memoria) y viaja en
 * `Ticket.textoCrudoOcr` cuando `revision-datos` persista; `reintento-otro-motor`
 * lo reutiliza sin repetir el OCR.
 */
class ReconocerTextoCrudo @Inject constructor(
    private val photoFileStore: PhotoFileStore,
    private val ocrEngine: OcrEngine,
) {

    /**
     * @param rutaFoto nombre relativo bajo `filesDir` de la foto ya guardada
     * (contrato de [PhotoFileStore.save]).
     * @return texto crudo del ticket; `""` si la foto no existe o el motor no
     * detecta texto.
     */
    suspend operator fun invoke(rutaFoto: String): String {
        val jpeg = photoFileStore.read(rutaFoto) ?: return ""
        return ocrEngine.recognize(jpeg)
    }
}

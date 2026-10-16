package com.robertomr.tally.framework.ai.ocr

import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.robertomr.tally.domain.expenses.data.OcrEngine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Implementacion de [OcrEngine] con ML Kit Text Recognition v2 latin BUNDLED
 * (spec ocr-mlkit, etapa 1 del ADR 0002): modelo dentro del APK
 * (`com.google.mlkit:text-recognition`), on-device, sin Play Services, sin red
 * y sin clave de API (R-OCR3: ~+6 MB de APK aceptado por offline-first).
 *
 * - El trabajo pesado (decode + OCR) corre en [Dispatchers.IO], nunca en el
 *   hilo principal.
 * - La foto llega con la orientacion EXIF horneada por `captura-foto`, de modo
 *   que [InputImage.fromBitmap] usa rotacion 0.
 * - La Task de ML Kit se envuelve en [suspendCancellableCoroutine] (sin
 *   dependencia extra de coroutines-play-services).
 * - Salida = texto crudo tal cual (`Text.text`); `""` si no se detecta texto o
 *   los bytes no son una imagen decodificable. Los errores del motor se
 *   propagan al llamador.
 */
internal class MlKitOcrEngine : OcrEngine {

    /** Cliente unico por instancia (el modelo se carga en memoria una vez). */
    private val recognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    override suspend fun recognize(image: ByteArray): String = withContext(Dispatchers.IO) {
        val bitmap = BitmapFactory.decodeByteArray(image, 0, image.size)
            ?: return@withContext "" // bytes sin imagen decodificable: sin texto
        val entrada = InputImage.fromBitmap(bitmap, 0)
        suspendCancellableCoroutine { continuacion ->
            recognizer
                .process(entrada)
                .addOnSuccessListener { texto -> if (continuacion.isActive) continuacion.resume(texto.text) }
                .addOnFailureListener { error -> if (continuacion.isActive) continuacion.resumeWithException(error) }
        }
    }
}

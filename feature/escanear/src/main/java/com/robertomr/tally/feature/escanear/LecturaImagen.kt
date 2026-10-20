package com.robertomr.tally.feature.escanear

import android.content.ContentResolver
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lectura de la imagen elegida en el selector del sistema (spec
 * galeria-escaneo). Vive en la capa Android de `:feature:escanear`: el [Uri]
 * nunca cruza al dominio, que solo recibe los bytes (igual que con la cámara).
 */

/** Tope de lectura de una imagen de la galería: 32 MiB. */
internal const val MAX_BYTES_GALERIA: Int = 32 * 1024 * 1024

internal const val MENSAJE_IMAGEN_ILEGIBLE = "No se pudo leer la imagen. Prueba con otra foto."
internal const val MENSAJE_IMAGEN_GRANDE = "La imagen es demasiado grande. Elige una de menos de 32 MB."

/** La imagen supera el tope de lectura. */
internal class ImagenDemasiadoGrandeException(maxBytes: Int) :
    IOException("La imagen supera el máximo de $maxBytes bytes")

/**
 * Lee el [Uri] completo, con tope. Corre en [Dispatchers.IO]. Lanza
 * [ImagenDemasiadoGrandeException] si supera [maxBytes] y [IOException] si no
 * se puede abrir o leer.
 */
internal suspend fun leerBytesAcotado(
    resolver: ContentResolver,
    uri: Uri,
    maxBytes: Int = MAX_BYTES_GALERIA,
): ByteArray = withContext(Dispatchers.IO) {
    val entrada = resolver.openInputStream(uri) ?: throw IOException("No se pudo abrir $uri")
    entrada.use { leerAcotado(it, maxBytes) }
}

/**
 * Lee [entrada] por bloques y aborta en cuanto se supera [maxBytes] (nunca
 * `readBytes()` sin tope: un fichero enorme no llega a ocupar memoria).
 */
internal fun leerAcotado(entrada: InputStream, maxBytes: Int): ByteArray {
    val salida = ByteArrayOutputStream()
    val bloque = ByteArray(64 * 1024)
    var total = 0
    while (true) {
        val leidos = entrada.read(bloque)
        if (leidos < 0) break
        total += leidos
        if (total > maxBytes) throw ImagenDemasiadoGrandeException(maxBytes)
        salida.write(bloque, 0, leidos)
    }
    return salida.toByteArray()
}

/** Mensaje en español para un fallo de lectura. */
internal fun mensajeDeLectura(error: Throwable): String =
    if (error is ImagenDemasiadoGrandeException || error is OutOfMemoryError) {
        MENSAJE_IMAGEN_GRANDE
    } else {
        MENSAJE_IMAGEN_ILEGIBLE
    }

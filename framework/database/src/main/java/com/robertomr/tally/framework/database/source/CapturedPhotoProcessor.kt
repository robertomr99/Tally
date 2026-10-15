package com.robertomr.tally.framework.database.source

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.robertomr.tally.domain.expenses.data.PhotoProcessor
import com.robertomr.tally.framework.database.photo.PhotoPipelineMath
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pipeline de la foto capturada (spec captura-foto), interna de
 * `:framework:database` junto a [PhotoFileStoreImpl] (crear `:framework:camera`
 * exige aprobacion del usuario; AGENTS.md).
 *
 * Pasos: lee la orientacion EXIF del JPEG crudo, la hornea en los pixeles
 * (rotacion), escala a <= [PhotoPipelineMath.MAX_LADO_LARGO] px de lado largo
 * conservando la proporcion y recomprime JPEG sin tag de orientacion. El
 * original de camara NO se guarda. Todo el trabajo pesado va en
 * [Dispatchers.IO].
 */
internal class CapturedPhotoProcessor : PhotoProcessor {

    override suspend fun process(rawJpeg: ByteArray): ByteArray = withContext(Dispatchers.IO) {
        val exif = ExifInterface(ByteArrayInputStream(rawJpeg))
        val orientacion = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED,
        )
        val grados = PhotoPipelineMath.gradosParaOrientacion(orientacion)

        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(rawJpeg, 0, rawJpeg.size, limites)
        check(limites.outWidth > 0 && limites.outHeight > 0) { "La captura no es una imagen valida" }

        // Dimensiones ya rotadas (90/270 intercambian los lados).
        val rotaLados = grados % 180 != 0
        val rotAncho = if (rotaLados) limites.outHeight else limites.outWidth
        val rotAlto = if (rotaLados) limites.outWidth else limites.outHeight
        val (objAncho, objAlto) = PhotoPipelineMath.dimensionesObjetivo(rotAncho, rotAlto)

        // Se escala ANTES de rotar: el tamano previo a la rotacion es el
        // objetivo intercambiado, de modo que tras rotar queda justo el objetivo.
        val preAncho = if (rotaLados) objAlto else objAncho
        val preAlto = if (rotaLados) objAncho else objAlto

        val opciones = BitmapFactory.Options().apply {
            inSampleSize = PhotoPipelineMath.inSampleSize(
                limites.outWidth, limites.outHeight, preAncho, preAlto,
            )
        }
        val decodificada = BitmapFactory.decodeByteArray(rawJpeg, 0, rawJpeg.size, opciones)
            ?: throw IllegalStateException("No se pudo decodificar la captura")

        val escalada = if (decodificada.width != preAncho || decodificada.height != preAlto) {
            Bitmap.createScaledBitmap(decodificada, preAncho, preAlto, true).also {
                if (it !== decodificada) decodificada.recycle()
            }
        } else {
            decodificada
        }

        val orientada = if (grados != 0) {
            val matriz = Matrix().apply { postRotate(grados.toFloat()) }
            Bitmap.createBitmap(escalada, 0, 0, escalada.width, escalada.height, matriz, true).also {
                if (it !== escalada) escalada.recycle()
            }
        } else {
            escalada
        }

        val salida = ByteArrayOutputStream()
        orientada.compress(Bitmap.CompressFormat.JPEG, PhotoPipelineMath.CALIDAD_JPEG, salida)
        orientada.recycle()
        // Bitmap.compress no escribe EXIF: la salida queda sin tag de orientacion.
        salida.toByteArray()
    }
}

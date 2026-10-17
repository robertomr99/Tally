package com.robertomr.tally.framework.database.source

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.robertomr.tally.domain.expenses.data.CropRegion
import com.robertomr.tally.domain.expenses.data.PhotoProcessor
import com.robertomr.tally.framework.database.photo.PhotoPipelineMath
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pipeline de la foto capturada (spec captura-foto + captura-recorte-encuadre),
 * interna de `:framework:database` junto a [PhotoFileStoreImpl] (crear
 * `:framework:camera` exige aprobacion del usuario; AGENTS.md).
 *
 * Pasos: lee la orientacion EXIF del JPEG crudo, la hornea en los pixeles
 * (rotacion), recorta la region del encuadre guiado (mapeada con el mismo
 * FILL_CENTER de la preview), escala a <= [PhotoPipelineMath.MAX_LADO_LARGO] px
 * de lado largo conservando la proporcion y recomprime JPEG sin tag de
 * orientacion. El original de camara NO se guarda. Todo el trabajo pesado va en
 * [Dispatchers.IO].
 */
internal class CapturedPhotoProcessor : PhotoProcessor {

    override suspend fun process(rawJpeg: ByteArray, crop: CropRegion): ByteArray =
        withContext(Dispatchers.IO) {
            val exif = ExifInterface(ByteArrayInputStream(rawJpeg))
            val orientacion = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED,
            )
            val grados = PhotoPipelineMath.gradosParaOrientacion(orientacion)

            val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(rawJpeg, 0, rawJpeg.size, limites)
            check(limites.outWidth > 0 && limites.outHeight > 0) {
                "La captura no es una imagen valida"
            }

            // Dimensiones ya rotadas (90/270 intercambian los lados): la imagen
            // orientada comparte la orientacion de visualizacion con la vista.
            val rotaLados = grados % 180 != 0
            val rotAncho = if (rotaLados) limites.outHeight else limites.outWidth
            val rotAlto = if (rotaLados) limites.outWidth else limites.outHeight

            // Region del recuadro dentro de la imagen orientada y tamano final.
            val recorte = PhotoPipelineMath.recorteEnImagen(crop, rotAncho, rotAlto)
            val recorteAncho = maxOf(1, (rotAncho * (recorte.right - recorte.left)).roundToInt())
            val recorteAlto = maxOf(1, (rotAlto * (recorte.bottom - recorte.top)).roundToInt())
            val (objAncho, objAlto) = PhotoPipelineMath.dimensionesObjetivo(recorteAncho, recorteAlto)

            // La decodificacion se muestrea para que el recorte no quede por
            // debajo del objetivo; asi el recuadro conserva el detalle del sensor.
            val opciones = BitmapFactory.Options().apply {
                inSampleSize = PhotoPipelineMath.inSampleSize(recorteAncho, recorteAlto, objAncho, objAlto)
            }
            val decodificada = BitmapFactory.decodeByteArray(rawJpeg, 0, rawJpeg.size, opciones)
                ?: throw IllegalStateException("No se pudo decodificar la captura")

            val orientada = if (grados != 0) {
                val matriz = Matrix().apply { postRotate(grados.toFloat()) }
                Bitmap.createBitmap(
                    decodificada,
                    0,
                    0,
                    decodificada.width,
                    decodificada.height,
                    matriz,
                    true,
                ).also {
                    if (it !== decodificada) decodificada.recycle()
                }
            } else {
                decodificada
            }

            val x = (orientada.width * recorte.left).roundToInt().coerceIn(0, orientada.width - 1)
            val y = (orientada.height * recorte.top).roundToInt().coerceIn(0, orientada.height - 1)
            val ancho = (orientada.width * (recorte.right - recorte.left)).roundToInt()
                .coerceIn(1, orientada.width - x)
            val alto = (orientada.height * (recorte.bottom - recorte.top)).roundToInt()
                .coerceIn(1, orientada.height - y)
            val recortada = Bitmap.createBitmap(orientada, x, y, ancho, alto).also {
                if (it !== orientada) orientada.recycle()
            }

            val escalada = if (recortada.width != objAncho || recortada.height != objAlto) {
                Bitmap.createScaledBitmap(recortada, objAncho, objAlto, true).also {
                    if (it !== recortada) recortada.recycle()
                }
            } else {
                recortada
            }

            val salida = ByteArrayOutputStream()
            escalada.compress(Bitmap.CompressFormat.JPEG, PhotoPipelineMath.CALIDAD_JPEG, salida)
            escalada.recycle()
            // Bitmap.compress no escribe EXIF: la salida queda sin tag de orientacion.
            salida.toByteArray()
        }
}

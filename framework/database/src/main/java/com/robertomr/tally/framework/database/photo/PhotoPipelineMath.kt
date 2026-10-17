package com.robertomr.tally.framework.database.photo

import com.robertomr.tally.domain.expenses.data.CropRegion
import kotlin.math.roundToInt

/**
 * Matematica pura del pipeline de foto (spec captura-foto + captura-recorte-encuadre):
 * dimensiones objetivo del escalado, muestra de decodificacion, mapeo de la
 * orientacion EXIF a grados y recorte del encuadre guiado. Sin dependencias de
 * Android para poder probarla en JVM.
 *
 * Pipeline: horneado de la orientacion EXIF en los pixeles + recorte de la
 * region del recuadro + escala a [MAX_LADO_LARGO] px de lado largo + JPEG
 * calidad [CALIDAD_JPEG].
 */
internal object PhotoPipelineMath {

    /** Lado largo maximo de la foto original guardada (~1280 px). */
    const val MAX_LADO_LARGO = 1280

    /** Calidad de la recompresion JPEG (~85). */
    const val CALIDAD_JPEG = 85

    /**
     * Grados de rotacion (en el sentido de las agujas del reloj) que hay que
     * aplicar a los pixeles para dejar la imagen derecha, segun el tag EXIF
     * Orientation (0x0112).
     *
     * Valores del tag: 1=normal, 2=espejo horizontal, 3=180, 4=espejo vertical,
     * 5=transposicion, 6=90 CW, 7=transversa, 8=270 CW. Solo se corrige la
     * rotacion de las orientaciones habituales de camara (3/6/8); las de espejo
     * (2/4/5/7) quedan sin corregir en este MVP (documentado en el spec).
     */
    fun gradosParaOrientacion(orientacion: Int): Int = when (orientacion) {
        3 -> 180
        6 -> 90
        8 -> 270
        else -> 0
    }

    /**
     * Dimensiones objetivo para que el lado largo quede en [MAX_LADO_LARGO]
     * conservando la proporcion. Si la entrada ya es <= [MAX_LADO_LARGO] no se
     * escala.
     */
    fun dimensionesObjetivo(ancho: Int, alto: Int, ladoLargoMax: Int = MAX_LADO_LARGO): Pair<Int, Int> {
        require(ancho > 0 && alto > 0) { "Dimensiones invalidas: ${ancho}x$alto" }
        val ladoLargo = maxOf(ancho, alto)
        if (ladoLargo <= ladoLargoMax) return ancho to alto
        val factor = ladoLargoMax.toDouble() / ladoLargo
        val objAncho = maxOf(1, (ancho * factor).roundToInt())
        val objAlto = maxOf(1, (alto * factor).roundToInt())
        return objAncho to objAlto
    }

    /**
     * `inSampleSize` (potencia de 2) para que la decodificacion con
     * `BitmapFactory` no sea mucho mayor que las dimensiones objetivo.
     */
    fun inSampleSize(anchoSrc: Int, altoSrc: Int, anchoObj: Int, altoObj: Int): Int {
        require(anchoSrc > 0 && altoSrc > 0 && anchoObj > 0 && altoObj > 0) {
            "Dimensiones invalidas: ${anchoSrc}x$altoSrc -> ${anchoObj}x$altoObj"
        }
        var muestra = 1
        while (anchoSrc / (muestra * 2) >= anchoObj && altoSrc / (muestra * 2) >= altoObj) {
            muestra *= 2
        }
        return muestra
    }

    /**
     * Mapea el recorte normalizado a la VISTA del encuadre a coordenadas
     * normalizadas [0,1] de la imagen capturada ya orientada (misma orientacion
     * de visualizacion que la vista).
     *
     * La preview se pinta con FILL_CENTER: la imagen cubre la vista y el
     * sobrante se recorta centrado. Por eso el factor de escala entre vista e
     * imagen es `aspectoVista / max(aspectoVista, aspectoImagen)` en horizontal
     * y `aspectoImagen / max(aspectoVista, aspectoImagen)` en vertical, con el
     * centro en 0.5. Con [CropRegion.viewAspectRatio] igual al de la imagen el
     * recorte es identidad (fotograma completo).
     */
    fun recorteEnImagen(
        recorte: CropRegion,
        anchoImagen: Int,
        altoImagen: Int,
    ): RecorteImagen {
        require(anchoImagen > 0 && altoImagen > 0) {
            "Dimensiones de imagen invalidas: ${anchoImagen}x$altoImagen"
        }
        require(recorte.viewAspectRatio > 0f) {
            "Aspecto de vista invalido: ${recorte.viewAspectRatio}"
        }
        val aspectImagen = anchoImagen.toFloat() / altoImagen
        val mayor = maxOf(recorte.viewAspectRatio, aspectImagen)
        val factorX = recorte.viewAspectRatio / mayor
        val factorY = aspectImagen / mayor
        return RecorteImagen(
            left = (0.5f + (recorte.left - 0.5f) * factorX).coerceIn(0f, 1f),
            top = (0.5f + (recorte.top - 0.5f) * factorY).coerceIn(0f, 1f),
            right = (0.5f + (recorte.right - 0.5f) * factorX).coerceIn(0f, 1f),
            bottom = (0.5f + (recorte.bottom - 0.5f) * factorY).coerceIn(0f, 1f),
        )
    }
}

/** Recorte normalizado [0,1] dentro de la imagen ya orientada. */
internal data class RecorteImagen(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

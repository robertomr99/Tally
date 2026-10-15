package com.robertomr.tally.framework.database.photo

import kotlin.math.roundToInt

/**
 * Matematica pura del pipeline de foto (spec captura-foto): dimensiones
 * objetivo del escalado, muestra de decodificacion y mapeo de la orientacion
 * EXIF a grados. Sin dependencias de Android para poder probarla en JVM.
 *
 * Pipeline: horneado de la orientacion EXIF en los pixeles + escala a
 * [MAX_LADO_LARGO] px de lado largo + JPEG calidad [CALIDAD_JPEG].
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
}

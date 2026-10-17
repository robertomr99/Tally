package com.robertomr.tally.feature.escanear

/**
 * Geometria pura del encuadre guiado (mockup 04), sin dependencias de Compose
 * para poder probarla en JVM.
 *
 * La MISMA funcion dibuja la reticula (scrim + esquinas) y calcula la region
 * que se recorta de la foto, de modo que "lo que se ve dentro del recuadro" es
 * "lo que se guarda" (WYSIWYG por construccion).
 */
object EncuadreGeometria {

    /** Margen lateral del recuadro respecto a la vista (dp). */
    const val MARGEN_DP = 24f

    /** Ancho maximo del recuadro (dp). */
    const val ANCHO_MAX_DP = 320f

    /** Alto del recuadro como fraccion del alto de la vista. */
    const val ALTO_FRAC = 0.68f

    /** Tope de alto del recuadro (dp). */
    const val ALTO_MAX_DP = 500f

    /**
     * Rectangulo del recuadro centrado en la vista (coordenadas en px de la
     * vista): margen [margenPx] a cada lado, ancho <= [anchoMaxPx] y alto
     * <= min(alto * [altoFrac], [altoMaxPx]).
     */
    fun zona(
        anchoVista: Float,
        altoVista: Float,
        margenPx: Float,
        anchoMaxPx: Float,
        altoFrac: Float,
        altoMaxPx: Float,
    ): ZonaEncuadre {
        val ancho = minOf(anchoVista - 2 * margenPx, anchoMaxPx).coerceAtLeast(1f)
        val alto = minOf(altoVista * altoFrac, altoMaxPx).coerceAtLeast(1f)
        val left = (anchoVista - ancho) / 2
        val top = (altoVista - alto) / 2
        return ZonaEncuadre(left, top, left + ancho, top + alto)
    }

    /** Coordenadas [0,1] del recuadro respecto a la vista. */
    fun rectNormalizado(
        zona: ZonaEncuadre,
        anchoVista: Float,
        altoVista: Float,
    ): RectNormalizado {
        require(anchoVista > 0f && altoVista > 0f) {
            "Vista invalida: ${anchoVista}x$altoVista"
        }
        return RectNormalizado(
            left = zona.left / anchoVista,
            top = zona.top / altoVista,
            right = zona.right / anchoVista,
            bottom = zona.bottom / altoVista,
        )
    }
}

/** Rectangulo del recuadro en px de la vista. */
data class ZonaEncuadre(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

/** Rectangulo del recuadro normalizado [0,1] respecto a la vista. */
data class RectNormalizado(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

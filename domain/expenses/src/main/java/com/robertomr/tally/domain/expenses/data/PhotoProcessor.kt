package com.robertomr.tally.domain.expenses.data

/**
 * Procesado de la foto cruda de camara en la foto original del ticket
 * (interfaz de dominio; la implementacion vive en `:framework:database`).
 *
 * La foto original es evidencia inmutable del ticket (CONTEXT.md): el pipeline
 * hornea la orientacion EXIF en los pixeles, recorta la region del encuadre
 * guiado, escala a ~1280 px de lado largo y recomprime JPEG (~85) sin tag de
 * orientacion. El original de camara NO se guarda.
 */
interface PhotoProcessor {

    /**
     * Procesa los bytes JPEG crudos de la captura, quedandose con [crop].
     *
     * @return bytes JPEG de la foto original ya recortada, orientada y
     * comprimida.
     */
    suspend fun process(rawJpeg: ByteArray, crop: CropRegion): ByteArray
}

/**
 * Region del encuadre guiado (mockup 04) a recortar de la captura.
 *
 * Coordenadas normalizadas [0,1] respecto a la VISTA del encuadre (no a la
 * imagen): [left]/[top]/[right]/[bottom] describen el area interior del
 * recuadro y [viewAspectRatio] (ancho/alto) es el aspecto de esa vista,
 * necesario para corregir el FILL_CENTER de la preview al mapear el recorte a
 * la imagen capturada.
 *
 * Spec galeria-escaneo: [Completo] es el centinela de "imagen entera" para una
 * foto elegida de la galería, que no tiene vista de encuadre.
 */
data class CropRegion(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val viewAspectRatio: Float,
) {
    companion object {
        /**
         * Imagen completa, sin recorte. No hay vista de encuadre, así que el
         * aspecto (`viewAspectRatio = 0`) es un centinela: el procesador lo
         * resuelve con las dimensiones de la propia imagen ya orientada.
         */
        val Completo: CropRegion = CropRegion(0f, 0f, 1f, 1f, viewAspectRatio = 0f)
    }
}

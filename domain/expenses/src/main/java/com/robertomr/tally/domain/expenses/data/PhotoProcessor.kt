package com.robertomr.tally.domain.expenses.data

/**
 * Procesado de la foto cruda de camara en la foto original del ticket
 * (interfaz de dominio; la implementacion vive en `:framework:database`).
 *
 * La foto original es evidencia inmutable del ticket (CONTEXT.md): el pipeline
 * hornea la orientacion EXIF en los pixeles, escala a ~1280 px de lado largo y
 * recomprime JPEG (~85) sin tag de orientacion. El original de camara NO se
 * guarda.
 */
interface PhotoProcessor {

    /**
     * Procesa los bytes JPEG crudos de la captura.
     *
     * @return bytes JPEG de la foto original ya orientada y comprimida.
     */
    suspend fun process(rawJpeg: ByteArray): ByteArray
}

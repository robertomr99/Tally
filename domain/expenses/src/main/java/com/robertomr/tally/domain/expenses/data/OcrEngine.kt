package com.robertomr.tally.domain.expenses.data

/**
 * Motor de OCR: de los bytes JPEG de la foto ya persistida al TEXTO CRUDO del
 * ticket (interfaz de dominio; la implementacion ML Kit vive en
 * `:framework:ai`). Es la etapa 1 del ADR 0002: la etapa 2 (`TextEngine`, la
 * extraccion estructurada) es de otras features.
 *
 * Sin imports de Android ni de ML Kit (AGENTS.md): el contrato solo habla de
 * bytes y texto.
 */
interface OcrEngine {

    /**
     * Reconoce el texto del ticket en los bytes JPEG de la foto ya procesada
     * por `captura-foto` (orientacion EXIF horneada, ~1280 px de lado largo).
     *
     * @return texto crudo tal cual (lineas separadas por `\n`); `""` si no se
     * detecta texto o la imagen no es decodificable.
     * @throws Exception si el motor falla; el error se propaga al llamador
     * (la politica de "no bloquear la captura" vive en la presentacion). Una
     * [kotlinx.coroutines.CancellationException] jamas se convierte en
     * resultado: siempre se propaga.
     */
    suspend fun recognize(image: ByteArray): String
}

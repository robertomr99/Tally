package com.robertomr.tally.domain.expenses.data

import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.TicketExtraido

/**
 * Motor de extraccion estructurada: del TEXTO CRUDO del ticket (etapa 1,
 * `OcrEngine`) a un [TicketExtraido] validado (etapa 2 del ADR 0002).
 *
 * Implementaciones (siempre `internal` en `:framework:ai`, AGENTS.md):
 * - Nube: `GeminiCloudEngine` (Gemini via Firebase AI Logic; el texto sale al
 *   servicio — implicacion de privacidad asumida en ADR 0001).
 * - Local: `GemmaLocalEngine` (feature `extraccion-local`, sin red).
 * Un futuro `EngineRouter` (ADR 0001) seleccionara entre ellos sin cambiar
 * este contrato.
 *
 * Sin imports de Android ni de Firebase (AGENTS.md): el contrato solo habla de
 * texto y del dominio.
 */
interface TextEngine {

    /** Motor que implementa este extractor; queda registrado en el resultado. */
    val motor: Motor

    /**
     * Extrae los datos estructurados del texto crudo del ticket.
     *
     * @return [TicketExtraido] validado (el parser compartido ya rechazo
     * respuestas malformadas con `InvalidExtractionException`).
     * @throws Exception si el motor falla (red, cuota, config ausente); el
     * error se propaga al llamador SIN resultados inventados. Una
     * [kotlinx.coroutines.CancellationException] jamas se convierte en
     * resultado: siempre se propaga.
     */
    suspend fun extract(textoCrudo: String): TicketExtraido
}

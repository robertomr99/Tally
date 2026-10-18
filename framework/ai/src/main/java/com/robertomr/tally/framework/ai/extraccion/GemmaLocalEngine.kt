package com.robertomr.tally.framework.ai.extraccion

import com.robertomr.tally.domain.expenses.data.ModelDownloader
import com.robertomr.tally.domain.expenses.data.TextEngine
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.ModeloLocalNoDisponibleException
import com.robertomr.tally.domain.expenses.entities.TicketExtraido
import com.robertomr.tally.domain.expenses.entities.TicketExtraidoParser
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * [TextEngine] de la via LOCAL (spec extraccion-local, ADR 0002): Gemma 3n E2B
 * on-device via MediaPipe (detras de [LocalLlm]), sin red. Mismo contrato y
 * mismo [TicketExtraidoParser] que la nube; devuelve `motor = Motor.LOCAL`.
 *
 * - Modelo ausente -> [ModeloLocalNoDisponibleException] sin tocar MediaPipe.
 * - La generacion se detiene al cerrarse el primer objeto JSON ([JsonObjectStreamDetector]).
 * - Un [Mutex] evita dos cargas simultaneas del modelo (~3,6 GB).
 * - Sin reintento ni resultados inventados (ADR 0001); la cancelacion se propaga.
 */
internal class GemmaLocalEngine(
    private val modelDownloader: ModelDownloader,
    private val llm: LocalLlm,
) : TextEngine {

    override val motor: Motor = Motor.LOCAL

    private val mutex = Mutex()

    override suspend fun extract(textoCrudo: String): TicketExtraido {
        val ruta = modelDownloader.rutaModelo() ?: throw ModeloLocalNoDisponibleException()
        val prompt = TicketExtractionPrompt.buildLocalPrompt(textoCrudo)
        val run = mutex.withLock {
            val detector = JsonObjectStreamDetector()
            llm.generate(ruta, prompt, MAX_TOKENS) { acumulado -> detector.accept(acumulado) }
        }
        return TicketExtraidoParser.parse(JsonObjectStreamDetector.recortar(run.text), Motor.LOCAL)
    }

    internal companion object {
        /** Suma de entrada + salida (D3 del spec). */
        const val MAX_TOKENS = 2048
    }
}

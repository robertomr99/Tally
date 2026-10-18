package com.robertomr.tally.framework.ai.extraccion

/**
 * Costura testable del motor local (spec extraccion-local): oculta MediaPipe
 * para poder probar [GemmaLocalEngine] en JVM con un runner falso.
 */
internal interface LocalLlm {

    /**
     * Carga el modelo de [modelPath] (fuera del hilo principal), genera la
     * respuesta a [prompt] y libera el modelo. [onPartial] recibe el texto
     * ACUMULADO tras cada fragmento y devuelve `true` para detener la generacion.
     * Una cancelacion de la corrutina aborta la generacion nativa y cierra el
     * modelo antes de relanzar.
     *
     * @throws com.robertomr.tally.domain.expenses.entities.InvalidExtractionException
     *   si el prompt no cabe en [maxTokens] con margen de salida.
     */
    suspend fun generate(
        modelPath: String,
        prompt: String,
        maxTokens: Int,
        onPartial: (String) -> Boolean,
    ): LlmRun
}

/** Resultado crudo de una generacion local y sus latencias (ms). */
internal data class LlmRun(
    val text: String,
    val loadMillis: Long,
    val generationMillis: Long,
    val promptTokens: Int,
    val responseTokens: Int,
    val ranOnMainThread: Boolean,
)

package com.robertomr.tally.framework.ai.extraccion

import android.content.Context
import android.os.Looper
import android.util.Log
import android.os.SystemClock
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.robertomr.tally.domain.expenses.entities.InvalidExtractionException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * [LocalLlm] real sobre MediaPipe `tasks-genai` (derivado del spike R1). Carga
 * el modelo por extraccion (D2), todo en [Dispatchers.IO], y lo cierra siempre
 * en `finally`, tambien tras cancelar (se espera a que la generacion nativa
 * termine antes de cerrar sesion y modelo).
 */
internal class MediaPipeLocalLlm(private val context: Context) : LocalLlm {

    override suspend fun generate(
        modelPath: String,
        prompt: String,
        maxTokens: Int,
        onPartial: (String) -> Boolean,
    ): LlmRun = withContext(Dispatchers.IO) {
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .setMaxTokens(maxTokens)
            .build()

        val loadStart = SystemClock.elapsedRealtime()
        val inference = LlmInference.createFromOptions(context, options)
        val loadMillis = SystemClock.elapsedRealtime() - loadStart
        try {
            val promptTokens = inference.sizeInTokens(prompt)
            if (promptTokens > maxTokens - MARGEN_SALIDA_TOKENS) {
                throw InvalidExtractionException("ticket demasiado largo para el motor local")
            }
            val session = LlmInferenceSession.createFromOptions(
                inference,
                LlmInferenceSession.LlmInferenceSessionOptions.builder()
                    .setTopK(TOP_K)
                    .setTemperature(TEMPERATURE)
                    .setRandomSeed(SEED)
                    .build(),
            )
            try {
                val generationStart = SystemClock.elapsedRealtime()
                val texto = generarCancelable(session, prompt, onPartial)
                val generationMillis = SystemClock.elapsedRealtime() - generationStart
                val ranOnMain = Looper.myLooper() == Looper.getMainLooper()
                val run = LlmRun(
                    text = texto,
                    loadMillis = loadMillis,
                    generationMillis = generationMillis,
                    promptTokens = promptTokens,
                    responseTokens = inference.sizeInTokens(texto),
                    ranOnMainThread = ranOnMain,
                )
                Log.i(
                    TAG,
                    "metricas ranOnMainThread=${run.ranOnMainThread} loadMillis=${run.loadMillis} " +
                        "generationMillis=${run.generationMillis} promptTokens=${run.promptTokens} " +
                        "responseTokens=${run.responseTokens}",
                )
                Log.i(TAG, "respuesta_cruda=<<<${run.text}>>>")
                run
            } finally {
                session.close()
            }
        } finally {
            inference.close()
        }
    }

    private suspend fun generarCancelable(
        session: LlmInferenceSession,
        prompt: String,
        onPartial: (String) -> Boolean,
    ): String {
        val acumulado = StringBuilder()
        var detenido = false
        session.addQueryChunk(prompt)
        val future = session.generateResponseAsync { parcial, _ ->
            synchronized(acumulado) {
                if (!detenido) {
                    acumulado.append(parcial)
                    if (onPartial(acumulado.toString())) {
                        detenido = true
                        session.cancelGenerateResponseAsync()
                    }
                }
            }
        }
        val terminado = CompletableDeferred<Unit>()
        future.addListener({ terminado.complete(Unit) }, DIRECT_EXECUTOR)
        try {
            terminado.await()
        } catch (e: CancellationException) {
            session.cancelGenerateResponseAsync()
            // Cierre ordenado: esperar a que la generacion nativa acabe antes de cerrar.
            withContext(NonCancellable) { withTimeoutOrNull(ESPERA_CANCELACION_MS) { terminado.await() } }
            throw e
        }
        val fueDetenido = synchronized(acumulado) { detenido }
        try {
            future.get()
        } catch (e: ExecutionException) {
            if (!fueDetenido) throw IllegalStateException("Fallo de generacion del modelo local", e)
        } catch (e: java.util.concurrent.CancellationException) {
            if (!fueDetenido) throw IllegalStateException("Generacion del modelo local cancelada", e)
        }
        return synchronized(acumulado) { acumulado.toString() }
    }

    private companion object {
        const val TAG = "ExtraccionLocal"
        const val TOP_K = 1
        const val TEMPERATURE = 0.2f
        const val SEED = 42
        const val MARGEN_SALIDA_TOKENS = 512
        const val ESPERA_CANCELACION_MS = 15_000L
        val DIRECT_EXECUTOR = Executor { it.run() }
    }
}

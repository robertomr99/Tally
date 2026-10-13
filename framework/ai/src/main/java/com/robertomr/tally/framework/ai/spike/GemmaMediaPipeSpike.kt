package com.robertomr.tally.framework.ai.spike

import android.content.Context
import android.os.Looper
import android.os.SystemClock
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import java.io.File
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Spike técnico sin UI (riesgo R1): valida que un modelo Gemma para MediaPipe
 * GenAI/LLM Inference carga y genera **fuera del hilo principal** en el
 * dispositivo objetivo.
 *
 * No es el contrato `TextEngine` ni el motor local definitivo (`GemmaLocalEngine`):
 * solo mide viabilidad y latencia. Es `internal` y vive en `framework/ai`.
 *
 * La carga y la generación se ejecutan siempre en [dispatcher] (por defecto
 * `Dispatchers.IO`), nunca en el hilo principal.
 */
internal class GemmaMediaPipeSpike(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    /** Texto generado y latencias crudas medidas en milisegundos. */
    data class SpikeResult(
        val response: String,
        val loadMillis: Long,
        val generationMillis: Long,
        val promptTokens: Int,
        val responseTokens: Int,
        val ranOnMainThread: Boolean,
    )

    /**
     * Carga [modelFile] y responde a [prompt]. Devuelve la respuesta y las
     * latencias de carga y generación.
     *
     * @throws IllegalArgumentException si [modelFile] no es un fichero existente.
     */
    suspend fun run(
        modelFile: File,
        prompt: String,
        maxTokens: Int = DEFAULT_MAX_TOKENS,
    ): SpikeResult = withContext(dispatcher) {
        require(modelFile.isFile) {
            "Modelo no encontrado en '${modelFile.absolutePath}'. Copia el .litertlm/.task " +
                "con adb push antes de ejecutar el harness."
        }

        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelFile.absolutePath)
            .setMaxTokens(maxTokens)
            .build()

        val loadStart = SystemClock.elapsedRealtime()
        val inference = LlmInference.createFromOptions(context, options)
        val loadMillis = SystemClock.elapsedRealtime() - loadStart

        try {
            val promptTokens = inference.sizeInTokens(prompt)
            val generationStart = SystemClock.elapsedRealtime()
            val response = inference.generateCancellable(prompt)
            val generationMillis = SystemClock.elapsedRealtime() - generationStart

            SpikeResult(
                response = response,
                loadMillis = loadMillis,
                generationMillis = generationMillis,
                promptTokens = promptTokens,
                responseTokens = inference.sizeInTokens(response),
                ranOnMainThread = isOnMainThread(),
            )
        } finally {
            inference.close()
        }
    }

    /**
     * Expone la generación asíncrona de MediaPipe como un `suspend` cancelable:
     * si se cancela la corrutina, se aborta la generación en curso con
     * [LlmInferenceSession.cancelGenerateResponseAsync].
     */
    private suspend fun LlmInference.generateCancellable(prompt: String): String {
        val session = LlmInferenceSession.createFromOptions(
            this,
            LlmInferenceSession.LlmInferenceSessionOptions.builder().build(),
        )
        return try {
            suspendCancellableCoroutine { continuation ->
                session.addQueryChunk(prompt)
                val future = session.generateResponseAsync()
                continuation.invokeOnCancellation { session.cancelGenerateResponseAsync() }
                future.addListener(
                    {
                        try {
                            continuation.resume(future.get())
                        } catch (error: Exception) {
                            continuation.resumeWithException(error)
                        }
                    },
                    DIRECT_EXECUTOR,
                )
            }
        } finally {
            session.close()
        }
    }

    private fun isOnMainThread(): Boolean = Looper.myLooper() == Looper.getMainLooper()

    companion object {
        const val DEFAULT_MAX_TOKENS = 512
        private val DIRECT_EXECUTOR = Executor { it.run() }
    }
}

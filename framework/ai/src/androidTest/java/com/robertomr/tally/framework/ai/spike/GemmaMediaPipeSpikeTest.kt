package com.robertomr.tally.framework.ai.spike

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Harness instrumentado sin UI del spike R1 (Gemma + MediaPipe).
 *
 * Manual y deliberadamente **fuera** de `init.sh`: el modelo (~3,7 GB) no viaja
 * en el APK ni en el repo. Antes de ejecutarlo, copia el `.litertlm` a la ruta
 * que loguea este test:
 *
 *   adb push <modelo>.litertlm /sdcard/Android/data/<paquete>/files/models/
 *
 * Ejecución en la Tab S9+:
 *   gradlew.bat :framework:ai:connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class GemmaMediaPipeSpikeTest {

    @Test
    fun gemmaLoadsOffMainThreadAndResponds() = runBlocking {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val modelsDir = requireNotNull(targetContext.getExternalFilesDir(MODELS_DIR)) {
            "getExternalFilesDir(\"$MODELS_DIR\") devolvió null"
        }
        Log.i(TAG, "Directorio de modelos resuelto: ${modelsDir.absolutePath}")
        Log.i(TAG, "Copia el modelo con: adb push <modelo>.litertlm ${modelsDir.absolutePath}/")

        val modelFile = modelsDir.listFiles()
            ?.firstOrNull { it.isFile && MODEL_EXTENSIONS.any(it.name::endsWith) }
        if (modelFile == null) {
            fail(
                "No hay modelo (${MODEL_EXTENSIONS.joinToString("/")}) en ${modelsDir.absolutePath}. " +
                    "Copia el artefacto de Gemma con adb push antes de ejecutar el harness.",
            )
            return@runBlocking
        }

        val spike = GemmaMediaPipeSpike(targetContext.applicationContext)
        val result = spike.run(modelFile, PROMPT)
        Log.i(
            TAG,
            "ranOnMainThread=${result.ranOnMainThread} loadMillis=${result.loadMillis} " +
                "generationMillis=${result.generationMillis} promptTokens=${result.promptTokens} " +
                "responseTokens=${result.responseTokens}",
        )
        Log.i(TAG, "Respuesta del modelo: ${result.response}")

        assertFalse("La carga/inferencia corrió en el hilo principal", result.ranOnMainThread)
        assertTrue("El modelo devolvió una respuesta vacía", result.response.isNotBlank())
    }

    private companion object {
        const val TAG = "GemmaSpike"
        const val MODELS_DIR = "models"
        val MODEL_EXTENSIONS = listOf(".litertlm", ".task")
        const val PROMPT = "Responde en una sola frase: ¿cuánto es 2 + 2?"
    }
}

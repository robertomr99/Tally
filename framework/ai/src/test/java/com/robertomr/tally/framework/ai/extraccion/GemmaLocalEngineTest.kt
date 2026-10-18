package com.robertomr.tally.framework.ai.extraccion

import com.robertomr.tally.domain.expenses.data.ModelDownloader
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.EstadoModelo
import com.robertomr.tally.domain.expenses.entities.InvalidExtractionException
import com.robertomr.tally.domain.expenses.entities.ModeloLocalNoDisponibleException
import com.robertomr.tally.domain.expenses.entities.Motor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Tests JVM de [GemmaLocalEngine] con un [LocalLlm] falso (spec extraccion-local,
 * scenarios 3 y 4): cableado del parser compartido, modelo ausente, cancelacion,
 * prompt local y serializacion por Mutex.
 */
class GemmaLocalEngineTest {

    private class FakeDownloader(private val ruta: String?) : ModelDownloader {
        override val estado: Flow<EstadoModelo> = emptyFlow()
        override suspend fun iniciar() = Unit
        override suspend fun cancelar() = Unit
        override suspend fun eliminar() = Unit
        override fun rutaModelo(): String? = ruta
    }

    private class FakeLlm(
        private val respuesta: suspend (onPartial: (String) -> Boolean) -> String,
    ) : LocalLlm {
        var llamadas = 0
        var ultimoPrompt: String? = null
        var ultimaRuta: String? = null
        var ultimoMax: Int? = null
        var concurrentes = 0
        var maxConcurrentes = 0

        override suspend fun generate(
            modelPath: String,
            prompt: String,
            maxTokens: Int,
            onPartial: (String) -> Boolean,
        ): LlmRun {
            llamadas++
            ultimaRuta = modelPath
            ultimoPrompt = prompt
            ultimoMax = maxTokens
            concurrentes++
            if (concurrentes > maxConcurrentes) maxConcurrentes = concurrentes
            try {
                return LlmRun(respuesta(onPartial), 1, 2, 3, 4, false)
            } finally {
                concurrentes--
            }
        }
    }

    private val jsonValido =
        """{"comercio":"BAR","total":3.5,"categoria":"ALIMENTACION","lineas":[{"concepto":"cafe","cantidad":1,"precio_unitario":3.5}]}"""

    private fun engine(ruta: String?, llm: LocalLlm) = GemmaLocalEngine(FakeDownloader(ruta), llm)

    private suspend fun assertInvalida(respuesta: String) {
        val llm = FakeLlm { respuesta }
        try {
            engine("/m", llm).extract("x")
            fail("esperaba InvalidExtractionException")
        } catch (_: InvalidExtractionException) {
        }
    }

    @Test
    fun modelAbsentThrowsWithoutTouchingTheRunner() = runTest {
        val llm = FakeLlm { error("no debe invocarse") }
        try {
            engine(null, llm).extract("texto")
            fail("esperaba ModeloLocalNoDisponibleException")
        } catch (e: ModeloLocalNoDisponibleException) {
            assertTrue(e.message!!.contains("Ajustes"))
        }
        assertEquals(0, llm.llamadas)
    }

    @Test
    fun validResponseYieldsLocalMotorAndSendsLocalPrompt() = runTest {
        val llm = FakeLlm { jsonValido }
        val extraido = engine("/m/model.litertlm", llm).extract("MERCADONA TOTAL 3,50")
        assertEquals(Motor.LOCAL, extraido.motor)
        assertEquals(Categoria.ALIMENTACION, extraido.categoria)
        assertEquals(3.5, extraido.total!!, 0.001)
        assertEquals("/m/model.litertlm", llm.ultimaRuta)
        assertEquals(GemmaLocalEngine.MAX_TOKENS, llm.ultimoMax)
        assertEquals(TicketExtractionPrompt.buildLocalPrompt("MERCADONA TOTAL 3,50"), llm.ultimoPrompt)
    }

    @Test
    fun trailingGarbageAfterTheObjectIsTrimmed() = runTest {
        val llm = FakeLlm { "$jsonValido\n{\"basura\": <end_of_turn> mas texto" }
        assertEquals(Motor.LOCAL, engine("/m", llm).extract("x").motor)
    }

    @Test
    fun detectorIsWiredToOnPartialAndStopsOnCompleteObject() = runTest {
        var incompletoParo = true
        var completoParo = false
        val llm = FakeLlm { onPartial ->
            incompletoParo = onPartial(jsonValido.dropLast(5))
            completoParo = onPartial(jsonValido)
            jsonValido
        }
        engine("/m", llm).extract("x")
        assertTrue(!incompletoParo)
        assertTrue(completoParo)
    }

    @Test
    fun malformedJsonThrowsInvalidExtraction() = runTest { assertInvalida("{\"comercio\": \"BAR\", ") }

    @Test
    fun noJsonObjectThrowsInvalidExtraction() = runTest { assertInvalida("Lo siento, no puedo.") }

    @Test
    fun categoryOutsideEnumThrowsInvalidExtraction() = runTest {
        assertInvalida("""{"categoria":"VIAJES","total":1.0}""")
    }

    @Test
    fun responseTruncatedByTokenLimitThrowsInvalidExtraction() = runTest {
        assertInvalida("""{"comercio":"BAR","categoria":"OCIO","lineas":[{"concepto":"a","cantid""")
    }

    @Test
    fun cancellationIsPropagatedAndNeverBecomesAResult() = runTest {
        val entrada = CompletableDeferred<Unit>()
        val llm = FakeLlm {
            entrada.complete(Unit)
            CompletableDeferred<String>().await() // espera hasta cancelar
        }
        val job = async { engine("/m", llm).extract("x") }
        advanceUntilIdle()
        assertTrue(entrada.isCompleted)
        job.cancel()
        try {
            job.await()
            fail("esperaba CancellationException")
        } catch (_: CancellationException) {
        }
        assertTrue(job.isCancelled)
    }

    @Test
    fun concurrentExtractionsAreSerializedByTheMutex() = runTest {
        val liberar = CompletableDeferred<Unit>()
        val llm = FakeLlm {
            liberar.await()
            jsonValido
        }
        val e = engine("/m", llm)
        val a = async { e.extract("1") }
        val b = async { e.extract("2") }
        advanceUntilIdle()
        assertEquals(1, llm.llamadas) // la segunda espera al Mutex
        liberar.complete(Unit)
        a.await()
        b.await()
        assertEquals(2, llm.llamadas)
        assertEquals(1, llm.maxConcurrentes)
    }
}

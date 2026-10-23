package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.TicketExtraido
import com.robertomr.tally.test.unit.FakeTextEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests JVM de [ExtraerTicket] con [FakeTextEngine] (spec extraccion-nube):
 * texto blank -> vacio SIN invocar el motor, texto valido -> invocacion con el
 * texto tal cual y resultado passthrough (motor registrado), error del motor
 * propagado y CancellationException re-lanzada (nunca convertida en resultado).
 */
class ExtraerTicketTest {

    private val engine = FakeTextEngine(
        result = TicketExtraido(
            comercio = "MERCADONA",
            fechaEmision = null,
            total = 12.34,
            lineas = emptyList(),
            categoria = Categoria.ALIMENTACION,
            motor = Motor.NUBE,
        ),
    )
    private val casoDeUso = ExtraerTicket(engine)

    @Test
    fun motorIsAPassthroughOfTheEngineMotorEvenBeforeExtracting() {
        // Spec procesado-ui T1: el badge necesita el motor ANTES de llamar al
        // motor (la extraccion puede tardar decenas de segundos, R2).
        assertEquals(Motor.NUBE, casoDeUso.motor)

        val local = ExtraerTicket(FakeTextEngine(motor = Motor.LOCAL))
        assertEquals(Motor.LOCAL, local.motor)
    }

    @Test
    fun blankTextReturnsAnEmptyExtractionWithoutTouchingTheEngine() = runBlocking {
        listOf("", "   ", "\n\t").forEach { texto ->
            val extraido = casoDeUso(texto)

            assertEquals("", extraido.comercio)
            assertNull(extraido.fechaEmision)
            assertNull(extraido.total)
            assertTrue(extraido.lineas.isEmpty())
            assertEquals(Categoria.OTROS, extraido.categoria)
            assertEquals(Motor.NUBE, extraido.motor)
        }
        assertTrue("el motor no debe invocarse con texto blank", engine.received.isEmpty())
    }

    @Test
    fun nonBlankTextIsSentVerbatimAndTheResultIsReturnedWithTheEngineMotor() = runBlocking {
        val texto = "MERCADONA\nTOTAL 12,34"

        val extraido = casoDeUso(texto)

        assertEquals(listOf(texto), engine.received)
        assertEquals("MERCADONA", extraido.comercio)
        assertEquals(12.34, extraido.total!!, 0.0)
        assertEquals(Categoria.ALIMENTACION, extraido.categoria)
        assertEquals(Motor.NUBE, extraido.motor)
    }

    @Test(expected = IllegalStateException::class)
    fun engineErrorPropagatesToTheCaller(): Unit = runBlocking {
        engine.error = IllegalStateException("Firebase AI no configurado")

        casoDeUso("MERCADONA\nTOTAL 12,34")
    }

    @Test
    fun cancellationIsRethrownAndNeverConvertedIntoAResult() = runBlocking {
        engine.gate = CompletableDeferred() // nunca se completa: retiene en extract

        var capturada: Throwable? = null
        val trabajo = launch(Dispatchers.Default) {
            try {
                casoDeUso("MERCADONA\nTOTAL 12,34")
            } catch (e: Throwable) {
                capturada = e
                throw e
            }
        }
        // Deja que el caso de uso entre en extract (retenido por el gate) y
        // cancela: la CancellationException debe salir tal cual.
        val limite = System.currentTimeMillis() + 2_000
        while (engine.received.isEmpty() && System.currentTimeMillis() < limite) Thread.sleep(5)
        assertTrue("el motor no llego a invocarse", engine.received.isNotEmpty())
        trabajo.cancelAndJoin()

        assertTrue("esperaba CancellationException, fue $capturada", capturada is CancellationException)
    }
}

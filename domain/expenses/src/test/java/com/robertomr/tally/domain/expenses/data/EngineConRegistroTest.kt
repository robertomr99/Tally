package com.robertomr.tally.domain.expenses.data

import com.robertomr.tally.domain.expenses.entities.InvalidExtractionException
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.MotorNoDisponibleException
import com.robertomr.tally.domain.expenses.entities.OrigenExtraccion
import com.robertomr.tally.domain.expenses.entities.PreferenciaMotor
import com.robertomr.tally.domain.expenses.entities.ResultadoExtraccion
import com.robertomr.tally.domain.expenses.entities.TicketExtraido
import com.robertomr.tally.domain.expenses.entities.TipoFallo
import com.robertomr.tally.test.unit.FakeCapacidadDispositivo
import com.robertomr.tally.test.unit.FakeModelDownloader
import com.robertomr.tally.test.unit.FakeRegistroExtracciones
import com.robertomr.tally.test.unit.FakeReloj
import com.robertomr.tally.test.unit.FakeTextEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Tests JVM de [EngineConRegistro] (spec log-extracciones, escenarios 1-7) con
 * el router real sobre dobles y [FakeReloj].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EngineConRegistroTest {

    private val reloj = FakeReloj(epochMillis = 5_000L)
    private val registro = FakeRegistroExtracciones()
    private val capacidad = FakeCapacidadDispositivo(true)
    private val downloader = FakeModelDownloader(ruta = "/m/model")
    private var preferencia = PreferenciaMotor.AUTO

    /** Motor que consume tiempo del reloj fake durante la extraccion. */
    private inner class MotorLento(
        override val motor: Motor,
        private val ms: Long,
        private val error: Throwable? = null,
    ) : TextEngine {
        override suspend fun extract(textoCrudo: String): TicketExtraido {
            reloj.avanzar(ms)
            error?.let { throw it }
            return FakeTextEngine(motor = motor).extract(textoCrudo)
        }
    }

    private fun decorador(local: TextEngine, nube: TextEngine): EngineConRegistro {
        val router = EngineRouter(local, nube, capacidad, downloader) { preferencia }
        return EngineConRegistro(router, router, registro, reloj)
    }

    @Test
    fun exitoPorRouterRegistraMotorOrigenDuracionYLongitud() = runTest {
        val engine = decorador(MotorLento(Motor.LOCAL, 1_234), MotorLento(Motor.NUBE, 10))

        engine.extract("texto de ticket")

        val fila = registro.registrados.single()
        assertEquals(Motor.LOCAL, fila.motor) // AUTO + capaz + modelo -> Local
        assertEquals(OrigenExtraccion.ROUTER, fila.origen)
        assertEquals(ResultadoExtraccion.EXITO, fila.resultado)
        assertEquals(15L, fila.longitudTexto.toLong())
        assertEquals(1_234L, fila.duracionMs)
        assertEquals(5_000L, fila.marcaTiempo)
        assertNull(fila.tipoFallo)
        assertNull(fila.claseError)
    }

    @Test
    fun reintentoExplicitoRegistraOrigenReintentoYElMotorPedido() = runTest {
        val engine = decorador(MotorLento(Motor.LOCAL, 10), MotorLento(Motor.NUBE, 700))

        engine.extract(Motor.NUBE, "abc")

        val fila = registro.registrados.single()
        assertEquals(Motor.NUBE, fila.motor)
        assertEquals(OrigenExtraccion.REINTENTO, fila.origen)
        assertEquals(ResultadoExtraccion.EXITO, fila.resultado)
        assertEquals(700L, fila.duracionMs)
        assertEquals(3, fila.longitudTexto)
    }

    @Test
    fun fallosGenericosSeRegistranYSeRelanzanIntactos() = runTest {
        val original = IllegalStateException("fragmento secreto del ticket")
        val engine = decorador(MotorLento(Motor.LOCAL, 5), MotorLento(Motor.NUBE, 40, original))
        downloader.ruta = null // AUTO sin modelo -> Nube

        try {
            engine.extract("x")
            fail("debia lanzar")
        } catch (e: IllegalStateException) {
            assertSame(original, e)
        }

        val fila = registro.registrados.single()
        assertEquals(Motor.NUBE, fila.motor)
        assertEquals(ResultadoExtraccion.FALLO, fila.resultado)
        assertEquals(TipoFallo.OTRO, fila.tipoFallo)
        assertEquals("IllegalStateException", fila.claseError)
        assertEquals(40L, fila.duracionMs)
    }

    @Test
    fun respuestaInvalidaSeClasificaComoRespuestaInvalida() = runTest {
        val error = InvalidExtractionException("JSON roto con texto del ticket")
        val engine = decorador(MotorLento(Motor.LOCAL, 1), MotorLento(Motor.NUBE, 1, error))
        preferencia = PreferenciaMotor.NUBE

        try {
            engine.extract("x")
            fail("debia lanzar")
        } catch (e: InvalidExtractionException) {
            assertSame(error, e)
        }

        val fila = registro.registrados.single()
        assertEquals(TipoFallo.RESPUESTA_INVALIDA, fila.tipoFallo)
        assertEquals("InvalidExtractionException", fila.claseError)
    }

    @Test
    fun localForzadoSinModeloSeRegistraComoMotorNoDisponibleConDuracionCero() = runTest {
        val local = FakeTextEngine(motor = Motor.LOCAL)
        val engine = decorador(local, FakeTextEngine(motor = Motor.NUBE))
        preferencia = PreferenciaMotor.LOCAL
        downloader.ruta = null

        try {
            engine.extract("x")
            fail("debia lanzar")
        } catch (e: MotorNoDisponibleException) {
            assertEquals(Motor.LOCAL, e.motor)
        }

        val fila = registro.registrados.single()
        assertEquals(Motor.LOCAL, fila.motor)
        assertEquals(ResultadoExtraccion.FALLO, fila.resultado)
        assertEquals(TipoFallo.MOTOR_NO_DISPONIBLE, fila.tipoFallo)
        assertEquals("MotorNoDisponibleException", fila.claseError)
        assertEquals(0L, fila.duracionMs)
        assertTrue(local.received.isEmpty())
    }

    @Test
    fun reintentoAMotorNoDisponibleSeRegistraComoFallo() = runTest {
        val engine = decorador(FakeTextEngine(motor = Motor.LOCAL), FakeTextEngine(motor = Motor.NUBE))
        capacidad.capaz = false

        try {
            engine.extract(Motor.LOCAL, "x")
            fail("debia lanzar")
        } catch (_: MotorNoDisponibleException) {
        }

        val fila = registro.registrados.single()
        assertEquals(OrigenExtraccion.REINTENTO, fila.origen)
        assertEquals(Motor.LOCAL, fila.motor)
        assertEquals(TipoFallo.MOTOR_NO_DISPONIBLE, fila.tipoFallo)
    }

    @Test
    fun cancelacionRegistraCanceladaConLaDuracionHastaCancelarYRelanzaLaCancelacion() = runTest {
        val nube = FakeTextEngine(motor = Motor.NUBE).apply { gate = CompletableDeferred() }
        val engine = decorador(FakeTextEngine(motor = Motor.LOCAL), nube)
        preferencia = PreferenciaMotor.NUBE
        var relanzada: Throwable? = null

        val job = launch {
            try {
                engine.extract("0123456789")
            } catch (e: CancellationException) {
                relanzada = e
                throw e
            }
        }
        runCurrent()
        reloj.avanzar(250)
        job.cancel()
        job.join()

        assertTrue(relanzada is CancellationException)
        val fila = registro.registrados.single()
        assertEquals(ResultadoExtraccion.CANCELADA, fila.resultado)
        assertEquals(Motor.NUBE, fila.motor)
        assertEquals(250L, fila.duracionMs)
        assertEquals(10, fila.longitudTexto)
    }

    @Test
    fun siElRegistroFallaLaExtraccionNoSeAltera() = runTest {
        val engine = decorador(MotorLento(Motor.LOCAL, 1), MotorLento(Motor.NUBE, 1))
        registro.errorAlRegistrar = IllegalStateException("disco lleno")

        val resultado = engine.extract("x")

        assertEquals(Motor.LOCAL, resultado.motor)
        assertTrue(registro.registrados.isEmpty())
    }

    @Test
    fun siElRegistroFallaTrasUnErrorSeRelanzaElErrorOriginal() = runTest {
        val original = IllegalArgumentException("original")
        val engine = decorador(MotorLento(Motor.LOCAL, 1, original), MotorLento(Motor.NUBE, 1))
        registro.errorAlRegistrar = IllegalStateException("disco lleno")

        try {
            engine.extract("x")
            fail("debia lanzar")
        } catch (e: IllegalArgumentException) {
            assertSame(original, e)
        }
    }

    @Test
    fun nuncaSeGuardaElMensajeDeLaExcepcionNiElTexto() = runTest {
        val mensaje = "MENSAJE-SECRETO-123"
        val texto = "TEXTO-CRUDO-DEL-TICKET"
        val engine = decorador(MotorLento(Motor.LOCAL, 1, RuntimeException(mensaje)), MotorLento(Motor.NUBE, 1))

        try {
            engine.extract(texto)
        } catch (_: RuntimeException) {
        }

        val volcado = registro.registrados.single().toString()
        assertFalse(volcado.contains(mensaje))
        assertFalse(volcado.contains(texto))
        assertEquals(texto.length, registro.registrados.single().longitudTexto)
    }

    @Test
    fun motorYDisponibilidadDelegaSinRegistrar() = runTest {
        val engine = decorador(MotorLento(Motor.LOCAL, 1), MotorLento(Motor.NUBE, 1))

        assertEquals(Motor.LOCAL, engine.motor)
        engine.disponibilidad(Motor.NUBE)

        assertTrue(registro.registrados.isEmpty())
    }
}

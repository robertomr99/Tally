package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.data.EngineRouter
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.MotorNoDisponibleException
import com.robertomr.tally.domain.expenses.entities.PreferenciaMotor
import com.robertomr.tally.test.unit.FakeCapacidadDispositivo
import com.robertomr.tally.test.unit.FakeModelDownloader
import com.robertomr.tally.test.unit.FakeTextEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Tests JVM de [ExtraerTicketConMotor] (spec reintento-otro-motor). */
class ExtraerTicketConMotorTest {

    private val local = FakeTextEngine(motor = Motor.LOCAL)
    private val nube = FakeTextEngine(motor = Motor.NUBE)
    private val downloader = FakeModelDownloader(ruta = "/m/model")
    private val router = EngineRouter(local, nube, FakeCapacidadDispositivo(true), downloader) {
        PreferenciaMotor.AUTO
    }
    private val casoDeUso = ExtraerTicketConMotor(router)

    @Test
    fun textoBlankDevuelveVacioConElMotorPedidoSinInvocarlo() = runBlocking {
        for (motor in Motor.values()) for (texto in listOf("", "  ", "\n")) {
            val r = casoDeUso(texto, motor)
            assertEquals(motor, r.motor)
            assertEquals("", r.comercio)
            assertNull(r.total)
        }
        assertTrue(local.received.isEmpty())
        assertTrue(nube.received.isEmpty())
    }

    @Test
    fun delegaEnElMotorPedidoConElTextoTalCual() = runBlocking {
        val r = casoDeUso("TEXTO CRUDO", Motor.NUBE)
        assertEquals(Motor.NUBE, r.motor)
        assertEquals(listOf("TEXTO CRUDO"), nube.received)
        assertTrue(local.received.isEmpty())
    }

    @Test
    fun noCapturaErrores() = runBlocking {
        downloader.ruta = null
        try {
            casoDeUso("t", Motor.LOCAL)
            fail("debia lanzar")
        } catch (e: MotorNoDisponibleException) {
            assertEquals(Motor.LOCAL, e.motor)
        }
        nube.error = IllegalStateException("red")
        try {
            casoDeUso("t", Motor.NUBE)
            fail("debia lanzar")
        } catch (e: IllegalStateException) {
            assertEquals("red", e.message)
        }
    }
}

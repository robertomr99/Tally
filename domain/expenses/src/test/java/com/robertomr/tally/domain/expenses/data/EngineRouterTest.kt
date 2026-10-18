package com.robertomr.tally.domain.expenses.data

import com.robertomr.tally.domain.expenses.entities.DisponibilidadMotor
import com.robertomr.tally.domain.expenses.entities.ModeloLocalNoDisponibleException
import com.robertomr.tally.domain.expenses.entities.MotorNoDisponibleException
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.PreferenciaMotor
import com.robertomr.tally.test.unit.FakeCapacidadDispositivo
import com.robertomr.tally.test.unit.FakeModelDownloader
import com.robertomr.tally.test.unit.FakeTextEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Tests JVM de la politica `elegirMotor` y de [EngineRouter] (spec router-automatico). */
class EngineRouterTest {

    private val local = FakeTextEngine(motor = Motor.LOCAL)
    private val nube = FakeTextEngine(motor = Motor.NUBE)
    private val capacidad = FakeCapacidadDispositivo()
    private val downloader = FakeModelDownloader()
    private var pref = PreferenciaMotor.AUTO
    private val router = EngineRouter(local, nube, capacidad, downloader) { pref }

    private fun entorno(capaz: Boolean, modelo: Boolean) {
        capacidad.capaz = capaz
        downloader.ruta = if (modelo) "/m/model.task" else null
    }

    @Test
    fun politicaCubreTresPreferenciasPorCuatroCombinaciones() {
        for (capaz in listOf(true, false)) for (modelo in listOf(true, false)) {
            assertEquals(
                if (capaz && modelo) Motor.LOCAL else Motor.NUBE,
                elegirMotor(PreferenciaMotor.AUTO, capaz, modelo),
            )
            assertEquals(Motor.NUBE, elegirMotor(PreferenciaMotor.NUBE, capaz, modelo))
            assertEquals(Motor.LOCAL, elegirMotor(PreferenciaMotor.LOCAL, capaz, modelo))
        }
    }

    @Test
    fun delegaSoloEnElMotorElegidoYDevuelveSuMotor() = runBlocking {
        for (p in PreferenciaMotor.values()) for (capaz in listOf(true, false)) for (modelo in listOf(true, false)) {
            local.received.clear()
            nube.received.clear()
            pref = p
            entorno(capaz, modelo)
            val esperado = elegirMotor(p, capaz, modelo)
            assertEquals(esperado, router.motor)
            val r = router.extract("texto")
            assertEquals(esperado, r.motor)
            if (esperado == Motor.LOCAL) {
                assertEquals(listOf("texto"), local.received)
                assertTrue(nube.received.isEmpty())
            } else {
                assertEquals(listOf("texto"), nube.received)
                assertTrue(local.received.isEmpty())
            }
        }
    }

    @Test
    fun errorDelMotorElegidoSePropagaSinFallback() = runBlocking {
        entorno(capaz = true, modelo = true)
        val boom = IllegalStateException("boom")
        local.error = boom
        try {
            router.extract("t")
            fail("debia propagar")
        } catch (e: IllegalStateException) {
            assertEquals(boom, e)
        }
        assertTrue(nube.received.isEmpty())
    }

    @Test
    fun preferenciaLocalSinModeloPropagaModeloLocalNoDisponible() = runBlocking {
        pref = PreferenciaMotor.LOCAL
        entorno(capaz = false, modelo = false)
        local.error = ModeloLocalNoDisponibleException()
        try {
            router.extract("t")
            fail("debia propagar")
        } catch (e: ModeloLocalNoDisponibleException) {
            // esperado: no hay caida a Nube
        }
        assertTrue(nube.received.isEmpty())
    }

    @Test
    fun cancelacionSePropagaSinTocarElOtroMotor() = runBlocking {
        entorno(capaz = false, modelo = false)
        nube.gate = CompletableDeferred()
        var cancelado = false
        val job = launch(Dispatchers.Default) {
            try {
                router.extract("t")
            } catch (e: CancellationException) {
                cancelado = true
                throw e
            }
        }
        while (nube.received.isEmpty()) yield()
        job.cancelAndJoin()
        assertTrue(cancelado)
        assertTrue(local.received.isEmpty())
    }

    // --- Spec reintento-otro-motor: extraccion explicita por motor ---

    @Test
    fun disponibilidadMotorCubreTodasLasCombinaciones() {
        for (capaz in listOf(true, false)) for (modelo in listOf(true, false)) {
            assertEquals(DisponibilidadMotor.DISPONIBLE, disponibilidadMotor(Motor.NUBE, capaz, modelo))
        }
        assertEquals(DisponibilidadMotor.DISPONIBLE, disponibilidadMotor(Motor.LOCAL, true, true))
        assertEquals(DisponibilidadMotor.SIN_MODELO, disponibilidadMotor(Motor.LOCAL, true, false))
        assertEquals(DisponibilidadMotor.NO_CAPAZ, disponibilidadMotor(Motor.LOCAL, false, true))
        assertEquals(DisponibilidadMotor.NO_CAPAZ, disponibilidadMotor(Motor.LOCAL, false, false))
    }

    @Test
    fun disponibilidadLeeElEntornoActual() {
        entorno(capaz = true, modelo = false)
        assertEquals(DisponibilidadMotor.SIN_MODELO, router.disponibilidad(Motor.LOCAL))
        entorno(capaz = true, modelo = true)
        assertEquals(DisponibilidadMotor.DISPONIBLE, router.disponibilidad(Motor.LOCAL))
    }

    @Test
    fun extractPorMotorSoloInvocaElPedidoEIgnoraLaPreferencia() = runBlocking {
        entorno(capaz = true, modelo = true)
        for (p in PreferenciaMotor.values()) {
            pref = p
            local.received.clear()
            nube.received.clear()
            assertEquals(Motor.NUBE, router.extract(Motor.NUBE, "t1").motor)
            assertEquals(listOf("t1"), nube.received)
            assertTrue(local.received.isEmpty())
            nube.received.clear()
            assertEquals(Motor.LOCAL, router.extract(Motor.LOCAL, "t2").motor)
            assertEquals(listOf("t2"), local.received)
            assertTrue(nube.received.isEmpty())
        }
    }

    @Test
    fun extractPorMotorNoDisponibleLanzaSinInvocarElEngine() = runBlocking {
        entorno(capaz = true, modelo = false)
        try {
            router.extract(Motor.LOCAL, "t")
            fail("debia lanzar")
        } catch (e: MotorNoDisponibleException) {
            assertEquals(Motor.LOCAL, e.motor)
            assertEquals(DisponibilidadMotor.SIN_MODELO, e.disponibilidad)
        }
        assertTrue(local.received.isEmpty())
        assertTrue(nube.received.isEmpty())
    }

    @Test
    fun extractPorMotorPropagaErroresSinFallback() = runBlocking {
        entorno(capaz = true, modelo = true)
        val boom = IllegalStateException("boom")
        nube.error = boom
        try {
            router.extract(Motor.NUBE, "t")
            fail("debia propagar")
        } catch (e: IllegalStateException) {
            assertEquals(boom, e)
        }
        assertTrue(local.received.isEmpty())
    }

    @Test
    fun extractPorMotorPropagaLaCancelacion() = runBlocking {
        entorno(capaz = true, modelo = true)
        local.gate = CompletableDeferred()
        var cancelado = false
        val job = launch(Dispatchers.Default) {
            try {
                router.extract(Motor.LOCAL, "t")
            } catch (e: CancellationException) {
                cancelado = true
                throw e
            }
        }
        while (local.received.isEmpty()) yield()
        job.cancelAndJoin()
        assertTrue(cancelado)
        assertTrue(nube.received.isEmpty())
    }
}

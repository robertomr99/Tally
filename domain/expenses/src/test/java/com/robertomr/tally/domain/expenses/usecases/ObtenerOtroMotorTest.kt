package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.data.EngineRouter
import com.robertomr.tally.domain.expenses.entities.DisponibilidadMotor
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.OfertaMotor
import com.robertomr.tally.domain.expenses.entities.PreferenciaMotor
import com.robertomr.tally.test.unit.FakeCapacidadDispositivo
import com.robertomr.tally.test.unit.FakeModelDownloader
import com.robertomr.tally.test.unit.FakeTextEngine
import org.junit.Assert.assertEquals
import org.junit.Test

/** Tests JVM de [ObtenerOtroMotor] (spec reintento-otro-motor, S1/S3). */
class ObtenerOtroMotorTest {

    private val capacidad = FakeCapacidadDispositivo(true)
    private val downloader = FakeModelDownloader(ruta = "/m/model")
    private val router = EngineRouter(
        FakeTextEngine(motor = Motor.LOCAL),
        FakeTextEngine(motor = Motor.NUBE),
        capacidad,
        downloader,
    ) { PreferenciaMotor.AUTO }
    private val casoDeUso = ObtenerOtroMotor(router)

    @Test
    fun elOtroDeLocalEsNubeSiempreDisponible() {
        capacidad.capaz = false
        downloader.ruta = null
        assertEquals(OfertaMotor(Motor.NUBE, DisponibilidadMotor.DISPONIBLE), casoDeUso(Motor.LOCAL))
    }

    @Test
    fun elOtroDeNubeEsLocalConSuDisponibilidad() {
        assertEquals(OfertaMotor(Motor.LOCAL, DisponibilidadMotor.DISPONIBLE), casoDeUso(Motor.NUBE))
        downloader.ruta = null
        assertEquals(OfertaMotor(Motor.LOCAL, DisponibilidadMotor.SIN_MODELO), casoDeUso(Motor.NUBE))
        capacidad.capaz = false
        assertEquals(OfertaMotor(Motor.LOCAL, DisponibilidadMotor.NO_CAPAZ), casoDeUso(Motor.NUBE))
    }
}

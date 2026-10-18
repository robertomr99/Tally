package com.robertomr.tally.domain.expenses.usecases

import app.cash.turbine.test
import com.robertomr.tally.domain.expenses.entities.PreferenciaMotor
import com.robertomr.tally.test.unit.FakePreferenciaMotorStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Tests JVM de [ObservarPreferenciaMotor] y [ConfigurarPreferenciaMotor] (spec ajustes-motor). */
class PreferenciaMotorUseCasesTest {

    private val store = FakePreferenciaMotorStore()

    @Test
    fun observarEmiteAutoPorDefectoYLosCambiosPosteriores() = runTest {
        ObservarPreferenciaMotor(store)().test {
            assertEquals(PreferenciaMotor.AUTO, awaitItem())
            ConfigurarPreferenciaMotor(store)(PreferenciaMotor.NUBE)
            assertEquals(PreferenciaMotor.NUBE, awaitItem())
            ConfigurarPreferenciaMotor(store)(PreferenciaMotor.LOCAL)
            assertEquals(PreferenciaMotor.LOCAL, awaitItem())
        }
    }

    @Test
    fun configurarFijaLaPreferenciaQueLeeElRouter() {
        ConfigurarPreferenciaMotor(store)(PreferenciaMotor.LOCAL)
        assertEquals(PreferenciaMotor.LOCAL, store.actual())
    }
}

package com.robertomr.tally.framework.database.source

import com.robertomr.tally.domain.expenses.entities.Motor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Lectura tolerante de la columna `motor` (spec gasto-manual): LOCAL y NUBE se
 * conservan; "MANUAL" y cualquier valor desconocido son `null` (gasto manual) y
 * nunca lanzan, a diferencia del antiguo `Motor.valueOf`.
 */
class MotorDesdeTest {

    @Test
    fun motoresConocidosSeConservan() {
        assertEquals(Motor.LOCAL, motorDesde("LOCAL"))
        assertEquals(Motor.NUBE, motorDesde("NUBE"))
    }

    @Test
    fun manualYDesconocidosSonNulosSinLanzar() {
        assertNull(motorDesde(MOTOR_MANUAL))
        assertNull(motorDesde("FUTURO"))
        assertNull(motorDesde(""))
        assertNull(motorDesde("local"))
    }

    @Test
    fun laConstanteNoColisionaConUnMotorReal() {
        assertNull(Motor.entries.firstOrNull { it.name == MOTOR_MANUAL })
    }
}

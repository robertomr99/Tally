package com.robertomr.tally.domain.expenses.data

import com.robertomr.tally.domain.expenses.entities.PreferenciaMotor

/**
 * Origen de la preferencia de motor (sincrono y barato). La implementacion real es
 * [PreferenciaMotorStore] (spec ajustes-motor).
 */
fun interface PreferenciaMotorSource {
    fun actual(): PreferenciaMotor
}

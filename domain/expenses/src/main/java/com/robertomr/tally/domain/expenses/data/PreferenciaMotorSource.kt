package com.robertomr.tally.domain.expenses.data

import com.robertomr.tally.domain.expenses.entities.PreferenciaMotor

/**
 * Origen de la preferencia de motor (sincrono y barato). Hoy fijo en AUTO;
 * `ajustes-motor` lo reimplementara leyendo su almacen.
 */
fun interface PreferenciaMotorSource {
    fun actual(): PreferenciaMotor
}

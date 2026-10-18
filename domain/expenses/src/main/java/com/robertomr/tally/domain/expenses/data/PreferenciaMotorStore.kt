package com.robertomr.tally.domain.expenses.data

import com.robertomr.tally.domain.expenses.entities.PreferenciaMotor
import kotlinx.coroutines.flow.Flow

/**
 * Almacen persistente de la preferencia de motor (spec ajustes-motor, D2).
 * [actual] (sincrono) es lo que lee el router en cada extraccion; [preferencia]
 * alimenta la UI de Ajustes. Un valor ilegible o desconocido se lee como AUTO.
 */
interface PreferenciaMotorStore : PreferenciaMotorSource {
    val preferencia: Flow<PreferenciaMotor>

    fun guardar(preferencia: PreferenciaMotor)
}

package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.data.PreferenciaMotorStore
import com.robertomr.tally.domain.expenses.entities.PreferenciaMotor
import javax.inject.Inject

/** Fija la preferencia de motor; el router la lee en la siguiente extraccion. */
class ConfigurarPreferenciaMotor @Inject constructor(
    private val store: PreferenciaMotorStore,
) {
    operator fun invoke(preferencia: PreferenciaMotor) = store.guardar(preferencia)
}

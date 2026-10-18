package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.data.PreferenciaMotorStore
import com.robertomr.tally.domain.expenses.entities.PreferenciaMotor
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Preferencia de motor vigente (AUTO por defecto). */
class ObservarPreferenciaMotor @Inject constructor(
    private val store: PreferenciaMotorStore,
) {
    operator fun invoke(): Flow<PreferenciaMotor> = store.preferencia
}

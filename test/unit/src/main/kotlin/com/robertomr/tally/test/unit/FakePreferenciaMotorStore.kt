package com.robertomr.tally.test.unit

import com.robertomr.tally.domain.expenses.data.PreferenciaMotorStore
import com.robertomr.tally.domain.expenses.entities.PreferenciaMotor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Doble de [PreferenciaMotorStore] (spec ajustes-motor). */
class FakePreferenciaMotorStore(initial: PreferenciaMotor = PreferenciaMotor.AUTO) : PreferenciaMotorStore {
    private val flow = MutableStateFlow(initial)
    override val preferencia: Flow<PreferenciaMotor> = flow

    override fun actual(): PreferenciaMotor = flow.value

    override fun guardar(preferencia: PreferenciaMotor) {
        flow.value = preferencia
    }
}

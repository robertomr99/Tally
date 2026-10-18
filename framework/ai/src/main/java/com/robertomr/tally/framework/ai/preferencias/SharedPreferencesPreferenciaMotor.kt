package com.robertomr.tally.framework.ai.preferencias

import android.content.SharedPreferences
import com.robertomr.tally.domain.expenses.data.PreferenciaMotorStore
import com.robertomr.tally.domain.expenses.entities.PreferenciaMotor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Preferencia de motor en SharedPreferences (spec ajustes-motor, D1/D3): API de
 * plataforma, sin dependencia nueva. El valor se lee una vez al construir
 * (valor ilegible o desconocido -> AUTO) y se mantiene en un [MutableStateFlow];
 * [guardar] actualiza el flow y persiste con `apply()`. [actual] es sincrono y
 * barato: lo consulta el router en cada extraccion.
 */
internal class SharedPreferencesPreferenciaMotor(
    private val prefs: SharedPreferences,
) : PreferenciaMotorStore {

    private val flow = MutableStateFlow(leer())

    override val preferencia: Flow<PreferenciaMotor> = flow

    override fun actual(): PreferenciaMotor = flow.value

    override fun guardar(preferencia: PreferenciaMotor) {
        flow.value = preferencia
        prefs.edit().putString(CLAVE, preferencia.name).apply()
    }

    private fun leer(): PreferenciaMotor {
        val valor = runCatching { prefs.getString(CLAVE, null) }.getOrNull()
        return PreferenciaMotor.values().firstOrNull { it.name == valor } ?: PreferenciaMotor.AUTO
    }

    internal companion object {
        const val FICHERO = "tally_ajustes"
        const val CLAVE = "preferencia_motor"
    }
}

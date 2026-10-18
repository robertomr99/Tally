package com.robertomr.tally.framework.ai.preferencias

import android.content.SharedPreferences
import com.robertomr.tally.domain.expenses.entities.PreferenciaMotor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/** Tests JVM de [SharedPreferencesPreferenciaMotor] con un SharedPreferences en memoria. */
class SharedPreferencesPreferenciaMotorTest {

    private val prefs = MemoriaPrefs()

    @Test
    fun sinValorGuardadoEsAuto() = runBlocking {
        val store = SharedPreferencesPreferenciaMotor(prefs)
        assertEquals(PreferenciaMotor.AUTO, store.actual())
        assertEquals(PreferenciaMotor.AUTO, store.preferencia.first())
    }

    @Test
    fun valorDesconocidoOIlegibleEsAuto() {
        prefs.datos[SharedPreferencesPreferenciaMotor.CLAVE] = "INVENTADO"
        assertEquals(PreferenciaMotor.AUTO, SharedPreferencesPreferenciaMotor(prefs).actual())
        prefs.datos[SharedPreferencesPreferenciaMotor.CLAVE] = 42 // tipo incorrecto
        assertEquals(PreferenciaMotor.AUTO, SharedPreferencesPreferenciaMotor(prefs).actual())
    }

    @Test
    fun guardarActualizaActualYFlowAlMomento() = runBlocking {
        val store = SharedPreferencesPreferenciaMotor(prefs)
        store.guardar(PreferenciaMotor.NUBE)
        assertEquals(PreferenciaMotor.NUBE, store.actual())
        assertEquals(PreferenciaMotor.NUBE, store.preferencia.first())
    }

    @Test
    fun persisteEntreInstanciasSobreElMismoFichero() {
        SharedPreferencesPreferenciaMotor(prefs).guardar(PreferenciaMotor.LOCAL)
        assertEquals(PreferenciaMotor.LOCAL, SharedPreferencesPreferenciaMotor(prefs).actual())
        SharedPreferencesPreferenciaMotor(prefs).guardar(PreferenciaMotor.AUTO)
        assertEquals(PreferenciaMotor.AUTO, SharedPreferencesPreferenciaMotor(prefs).actual())
    }
}

/** SharedPreferences minimo en memoria (solo lo que usa la implementacion). */
private class MemoriaPrefs : SharedPreferences {
    val datos = mutableMapOf<String, Any?>()

    override fun getString(key: String, defValue: String?): String? =
        datos[key] as String? ?: defValue

    override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
        private val pendientes = mutableMapOf<String, Any?>()
        override fun putString(key: String, value: String?) = apply { pendientes[key] = value }
        override fun apply() { datos.putAll(pendientes) }
        override fun commit(): Boolean { apply(); return true }
        override fun putStringSet(key: String, values: MutableSet<String>?) = apply { pendientes[key] = values }
        override fun putInt(key: String, value: Int) = apply { pendientes[key] = value }
        override fun putLong(key: String, value: Long) = apply { pendientes[key] = value }
        override fun putFloat(key: String, value: Float) = apply { pendientes[key] = value }
        override fun putBoolean(key: String, value: Boolean) = apply { pendientes[key] = value }
        override fun remove(key: String) = apply { pendientes[key] = null }
        override fun clear() = apply { datos.clear() }
    }

    override fun getAll(): MutableMap<String, *> = datos
    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? = defValues
    override fun getInt(key: String, defValue: Int) = defValue
    override fun getLong(key: String, defValue: Long) = defValue
    override fun getFloat(key: String, defValue: Float) = defValue
    override fun getBoolean(key: String, defValue: Boolean) = defValue
    override fun contains(key: String) = datos.containsKey(key)
    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener,
    ) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener,
    ) = Unit
}

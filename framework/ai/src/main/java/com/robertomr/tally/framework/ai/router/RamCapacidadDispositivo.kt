package com.robertomr.tally.framework.ai.router

import android.app.ActivityManager
import android.content.Context
import com.robertomr.tally.domain.expenses.data.CapacidadDispositivo

/** Umbral de RAM total para el modelo local (~3,6 GB residentes, R-LOC2); heuristica, no benchmark (D3). */
internal const val UMBRAL_RAM_BYTES = 10_000_000_000L

/** Funcion pura del umbral, testeable en JVM. */
internal fun ramSuficiente(totalMemBytes: Long): Boolean = totalMemBytes >= UMBRAL_RAM_BYTES

/** Capacidad por RAM total del dispositivo (spec router-automatico, D3). */
internal class RamCapacidadDispositivo(private val context: Context) : CapacidadDispositivo {
    override fun puedeEjecutarModeloLocal(): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return ramSuficiente(info.totalMem)
    }
}

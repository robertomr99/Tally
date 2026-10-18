package com.robertomr.tally.domain.expenses.data

/** Capacidad del dispositivo para ejecutar el modelo local (spec router-automatico, D3). */
interface CapacidadDispositivo {
    fun puedeEjecutarModeloLocal(): Boolean
}

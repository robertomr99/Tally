package com.robertomr.tally.test.unit

import com.robertomr.tally.domain.expenses.data.CapacidadDispositivo

/** Doble de [CapacidadDispositivo] (spec router-automatico). */
class FakeCapacidadDispositivo(var capaz: Boolean = true) : CapacidadDispositivo {
    override fun puedeEjecutarModeloLocal(): Boolean = capaz
}

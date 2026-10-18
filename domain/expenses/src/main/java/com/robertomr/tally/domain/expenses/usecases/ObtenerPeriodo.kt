package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.entities.Periodo
import com.robertomr.tally.domain.expenses.entities.PeriodoTipo
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Período de [PeriodoTipo] que contiene "hoy". El `Clock` se inyecta para que
 * "hoy" sea la fecha local correcta (también en cambios de hora) y testeable.
 */
class ObtenerPeriodo @Inject constructor(
    private val clock: Clock,
) {
    operator fun invoke(tipo: PeriodoTipo): Periodo = Periodo.de(tipo, LocalDate.now(clock))
}

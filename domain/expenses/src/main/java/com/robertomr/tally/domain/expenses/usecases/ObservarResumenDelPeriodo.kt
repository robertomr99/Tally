package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.entities.Periodo
import com.robertomr.tally.domain.expenses.entities.ResumenPeriodo
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Métricas reactivas del [Periodo]: combina los tickets del período y del
 * anterior (ambos Flow de Room), así que cualquier cambio recalcula la tarjeta.
 */
class ObservarResumenDelPeriodo @Inject constructor(
    private val observarTickets: ObservarTicketsDelPeriodo,
    private val calcular: CalcularResumenPeriodo,
    private val clock: Clock,
) {
    operator fun invoke(periodo: Periodo): Flow<ResumenPeriodo> =
        combine(observarTickets(periodo), observarTickets(periodo.anterior())) { actual, anterior ->
            calcular(periodo, LocalDate.now(clock), actual, anterior)
        }
}

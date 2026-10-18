package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.entities.DistribucionCategorias
import com.robertomr.tally.domain.expenses.entities.Periodo
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Reparto por categoría del [Periodo], reactivo: parte del Flow de tickets de
 * Room, así que guardar o borrar un ticket recalcula el donut solo.
 */
class ObservarDistribucionDelPeriodo @Inject constructor(
    private val observarTickets: ObservarTicketsDelPeriodo,
    private val calcular: CalcularDistribucionCategorias,
) {
    operator fun invoke(periodo: Periodo): Flow<DistribucionCategorias> =
        observarTickets(periodo).map { calcular(it) }
}

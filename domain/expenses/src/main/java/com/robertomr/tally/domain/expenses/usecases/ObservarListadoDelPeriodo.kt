package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.entities.GrupoDiaTickets
import com.robertomr.tally.domain.expenses.entities.Periodo
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Listado del [Periodo] agrupado por día, reactivo: parte del Flow de tickets de
 * Room, así que guardar o borrar un ticket actualiza la lista sola.
 */
class ObservarListadoDelPeriodo @Inject constructor(
    private val observarTickets: ObservarTicketsDelPeriodo,
    private val agrupar: AgruparTicketsPorDia,
) {
    operator fun invoke(periodo: Periodo): Flow<List<GrupoDiaTickets>> =
        observarTickets(periodo).map { agrupar(it) }
}

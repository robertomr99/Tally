package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.data.TicketRepository
import com.robertomr.tally.domain.expenses.entities.Periodo
import com.robertomr.tally.domain.expenses.entities.Ticket
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Tickets cuya fecha de emisión cae en el [Periodo] (rango inclusivo). Punto
 * de entrada común del seguimiento: total, donut y listado lo reutilizan.
 */
class ObservarTicketsDelPeriodo @Inject constructor(
    private val repository: TicketRepository,
) {
    operator fun invoke(periodo: Periodo): Flow<List<Ticket>> =
        repository.observeByDateRange(periodo.desde, periodo.hasta)
}

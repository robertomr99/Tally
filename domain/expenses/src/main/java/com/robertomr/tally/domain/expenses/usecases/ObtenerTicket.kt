package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.data.TicketRepository
import com.robertomr.tally.domain.expenses.entities.Ticket
import javax.inject.Inject

/**
 * Lee una vez un ticket guardado con sus lineas (spec detalle-ticket, D6); `null`
 * si ya no existe.
 */
class ObtenerTicket @Inject constructor(
    private val ticketRepository: TicketRepository,
) {

    suspend operator fun invoke(id: Long): Ticket? = ticketRepository.getById(id)
}

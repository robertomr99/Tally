package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.data.TicketRepository
import javax.inject.Inject

/**
 * Borra un ticket guardado en cascada: fila, lineas y fichero de foto (spec
 * detalle-ticket; el orden DB-antes-foto y el best-effort de la foto viven en
 * `TicketRepository.delete`). Un id inexistente es un no-op.
 */
class BorrarTicket @Inject constructor(
    private val ticketRepository: TicketRepository,
) {

    suspend operator fun invoke(id: Long) = ticketRepository.delete(id)
}

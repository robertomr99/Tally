package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.entities.FiltroFormaPago
import com.robertomr.tally.domain.expenses.entities.Ticket
import javax.inject.Inject

/**
 * Deja solo los tickets que admite el [FiltroFormaPago] (función pura, spec
 * filtro-forma-pago). Conserva el orden de entrada; con TODOS devuelve la misma lista.
 */
class FiltrarTicketsPorFormaPago @Inject constructor() {

    operator fun invoke(tickets: List<Ticket>, filtro: FiltroFormaPago): List<Ticket> =
        if (filtro == FiltroFormaPago.TODOS) tickets else tickets.filter { filtro.admite(it.formaPago) }
}

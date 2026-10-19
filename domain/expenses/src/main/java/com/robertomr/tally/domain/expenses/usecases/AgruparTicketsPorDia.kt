package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.entities.GrupoDiaTickets
import com.robertomr.tally.domain.expenses.entities.Ticket
import javax.inject.Inject

/**
 * Agrupa los tickets por día de emisión (función pura, spec listado-gastos).
 * Orden estricto: fecha de emisión descendente y, en empate, id descendente; los
 * grupos salen del día más reciente al más antiguo. El total del día usa la misma
 * conversión a céntimos que la distribución por categoría; un total no finito o
 * <= 0 no suma, pero el ticket sí aparece.
 */
class AgruparTicketsPorDia @Inject constructor() {

    operator fun invoke(tickets: List<Ticket>): List<GrupoDiaTickets> =
        tickets
            .sortedWith(compareByDescending<Ticket> { it.fechaEmision }.thenByDescending { it.id })
            .groupBy { it.fechaEmision.toLocalDate() }
            .map { (fecha, delDia) ->
                GrupoDiaTickets(
                    fecha = fecha,
                    tickets = delDia,
                    totalCentimos = delDia.sumOf { (aCentimos(it.total) ?: 0L).coerceAtLeast(0L) },
                )
            }
}

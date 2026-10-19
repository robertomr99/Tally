package com.robertomr.tally.domain.expenses.entities

import java.time.LocalDate

/**
 * Tickets de un mismo día (por fecha de emisión) para el listado (spec
 * listado-gastos). [totalCentimos] suma solo los tickets con total finito y
 * positivo; los demás aparecen en [tickets] pero no suman.
 */
data class GrupoDiaTickets(
    val fecha: LocalDate,
    val tickets: List<Ticket>,
    val totalCentimos: Long,
)

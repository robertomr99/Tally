package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.entities.Periodo
import com.robertomr.tally.domain.expenses.entities.ResumenPeriodo
import com.robertomr.tally.domain.expenses.entities.Ticket
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Cálculo puro de las métricas del período. El promedio diario divide entre los
 * días transcurridos si el período está en curso (hasta [hoy] inclusive) y entre
 * los días totales si ya terminó; siempre >= 1 día. La variación compara contra
 * el período anterior completo y es `null` si este no tiene gasto.
 */
class CalcularResumenPeriodo @Inject constructor() {

    operator fun invoke(
        periodo: Periodo,
        hoy: LocalDate,
        tickets: List<Ticket>,
        ticketsAnterior: List<Ticket>,
    ): ResumenPeriodo {
        val total = tickets.sumOf { it.total }
        val totalAnterior = ticketsAnterior.sumOf { it.total }
        val dias = (ChronoUnit.DAYS.between(periodo.desde, minOf(periodo.hasta, hoy)) + 1).coerceAtLeast(1)
        return ResumenPeriodo(
            total = total,
            numTickets = tickets.size,
            promedioDiario = total / dias,
            totalAnterior = totalAnterior,
            variacion = if (totalAnterior <= 0.0) null else (total - totalAnterior) / totalAnterior,
        )
    }
}

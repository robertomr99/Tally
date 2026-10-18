package com.robertomr.tally.domain.expenses.entities

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/**
 * Ventana de fechas del panel (CONTEXT.md > Período). Ambos extremos son
 * inclusivos y se comparan contra la fecha de EMISIÓN del ticket, no la de
 * captura. La semana es siempre lunes-domingo (no depende del `Locale`).
 */
data class Periodo(
    val tipo: PeriodoTipo,
    val desde: LocalDate,
    val hasta: LocalDate,
) {
    init {
        require(!desde.isAfter(hasta)) { "desde ($desde) no puede ser posterior a hasta ($hasta)" }
    }

    companion object {
        /** Período de [tipo] que contiene la fecha [referencia]. */
        fun de(tipo: PeriodoTipo, referencia: LocalDate): Periodo = when (tipo) {
            PeriodoTipo.DIA -> Periodo(tipo, referencia, referencia)
            PeriodoTipo.SEMANA -> {
                val lunes = referencia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                Periodo(tipo, lunes, lunes.plusDays(6))
            }
            PeriodoTipo.MES -> {
                val mes = YearMonth.from(referencia)
                Periodo(tipo, mes.atDay(1), mes.atEndOfMonth())
            }
        }
    }
}

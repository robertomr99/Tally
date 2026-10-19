package com.robertomr.tally.domain.expenses.entities

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
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

    /**
     * Período inmediatamente anterior del mismo [tipo], siempre COMPLETO aunque
     * el actual esté en curso: día = ayer, semana = lunes-domingo previo y mes =
     * mes natural previo (nunca `minusDays(longitud)`, los meses difieren). El
     * personalizado de N días tiene como anterior los N días inmediatamente previos.
     */
    fun anterior(): Periodo = when (tipo) {
        PeriodoTipo.DIA -> desde.minusDays(1).let { Periodo(tipo, it, it) }
        PeriodoTipo.SEMANA -> Periodo(tipo, desde.minusDays(7), hasta.minusDays(7))
        PeriodoTipo.MES -> YearMonth.from(desde).minusMonths(1).let { Periodo(tipo, it.atDay(1), it.atEndOfMonth()) }
        PeriodoTipo.PERSONALIZADO -> (ChronoUnit.DAYS.between(desde, hasta) + 1).let { dias ->
            Periodo(tipo, desde.minusDays(dias), desde.minusDays(1))
        }
    }

    companion object {
        /** Rango elegido a mano; ambos extremos inclusivos y `desde <= hasta`. */
        fun personalizado(desde: LocalDate, hasta: LocalDate): Periodo =
            Periodo(PeriodoTipo.PERSONALIZADO, desde, hasta)

        /** Período de [tipo] que contiene la fecha [referencia] (no admite PERSONALIZADO). */
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
            PeriodoTipo.PERSONALIZADO ->
                throw IllegalArgumentException("un período personalizado necesita desde y hasta")
        }
    }
}

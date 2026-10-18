package com.robertomr.tally.domain.expenses.entities

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** Bordes de calendario de [Periodo] (spec periodo-selector, escenarios 1 y 2). */
class PeriodoTest {

    private fun semana(ref: LocalDate) = Periodo.de(PeriodoTipo.SEMANA, ref)
    private fun mes(ref: LocalDate) = Periodo.de(PeriodoTipo.MES, ref)

    @Test
    fun diaEsElPropioDiaInclusivo() {
        val p = Periodo.de(PeriodoTipo.DIA, LocalDate.of(2026, 10, 18))
        assertEquals(LocalDate.of(2026, 10, 18), p.desde)
        assertEquals(LocalDate.of(2026, 10, 18), p.hasta)
    }

    @Test
    fun semanaDeUnLunesEmpiezaEseMismoDia() {
        val p = semana(LocalDate.of(2026, 10, 12))
        assertEquals(LocalDate.of(2026, 10, 12), p.desde)
        assertEquals(LocalDate.of(2026, 10, 18), p.hasta)
    }

    @Test
    fun semanaDeUnDomingoTerminaEseMismoDia() {
        val p = semana(LocalDate.of(2026, 10, 18))
        assertEquals(LocalDate.of(2026, 10, 12), p.desde)
        assertEquals(LocalDate.of(2026, 10, 18), p.hasta)
    }

    @Test
    fun semanaSiempreEsLunesADomingoDeSieteDias() {
        var d = LocalDate.of(2026, 1, 1)
        repeat(400) {
            val p = semana(d)
            assertEquals(DayOfWeek.MONDAY, p.desde.dayOfWeek)
            assertEquals(DayOfWeek.SUNDAY, p.hasta.dayOfWeek)
            assertEquals(6L, ChronoUnit.DAYS.between(p.desde, p.hasta))
            d = d.plusDays(1)
        }
    }

    @Test
    fun semanaCruzaDeMesAbrilAMayo2026() {
        val p = semana(LocalDate.of(2026, 4, 29))
        assertEquals(LocalDate.of(2026, 4, 27), p.desde)
        assertEquals(LocalDate.of(2026, 5, 3), p.hasta)
    }

    @Test
    fun semanaCruzaDeAnioDiciembre2025AEnero2026() {
        val p = semana(LocalDate.of(2025, 12, 31))
        assertEquals(LocalDate.of(2025, 12, 29), p.desde)
        assertEquals(LocalDate.of(2026, 1, 4), p.hasta)
    }

    @Test
    fun mesDe31DiasYDe30Dias() {
        assertEquals(LocalDate.of(2026, 10, 1), mes(LocalDate.of(2026, 10, 18)).desde)
        assertEquals(LocalDate.of(2026, 10, 31), mes(LocalDate.of(2026, 10, 18)).hasta)
        assertEquals(LocalDate.of(2026, 4, 30), mes(LocalDate.of(2026, 4, 15)).hasta)
    }

    @Test
    fun febreroTiene28DiasEn2026Y29EnBisiesto2028() {
        assertEquals(LocalDate.of(2026, 2, 28), mes(LocalDate.of(2026, 2, 10)).hasta)
        val bisiesto = mes(LocalDate.of(2028, 2, 29))
        assertEquals(LocalDate.of(2028, 2, 1), bisiesto.desde)
        assertEquals(LocalDate.of(2028, 2, 29), bisiesto.hasta)
    }

    @Test
    fun ultimoDiaDeMesYPrimeroDeMesPertenecenASuMes() {
        assertEquals(LocalDate.of(2026, 1, 1), mes(LocalDate.of(2026, 1, 31)).desde)
        assertEquals(LocalDate.of(2026, 1, 31), mes(LocalDate.of(2026, 1, 1)).hasta)
    }

    @Test
    fun diciembreYEneroNoRompenElAnio() {
        val dic = mes(LocalDate.of(2025, 12, 31))
        assertEquals(LocalDate.of(2025, 12, 1), dic.desde)
        assertEquals(LocalDate.of(2025, 12, 31), dic.hasta)
        val ene = mes(LocalDate.of(2026, 1, 1))
        assertEquals(LocalDate.of(2026, 1, 1), ene.desde)
        assertEquals(LocalDate.of(2026, 1, 31), ene.hasta)
    }

    @Test
    fun rechazaDesdePosteriorAHasta() {
        assertThrows(IllegalArgumentException::class.java) {
            Periodo(PeriodoTipo.DIA, LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 1))
        }
    }
}

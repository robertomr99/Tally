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

    private fun anterior(tipo: PeriodoTipo, ref: LocalDate) = Periodo.de(tipo, ref).anterior()

    @Test
    fun anteriorDeDiaCruzaMesYAnio() {
        assertEquals(LocalDate.of(2026, 2, 28), anterior(PeriodoTipo.DIA, LocalDate.of(2026, 3, 1)).desde)
        assertEquals(LocalDate.of(2028, 2, 29), anterior(PeriodoTipo.DIA, LocalDate.of(2028, 3, 1)).hasta)
        assertEquals(LocalDate.of(2025, 12, 31), anterior(PeriodoTipo.DIA, LocalDate.of(2026, 1, 1)).desde)
    }

    @Test
    fun anteriorDeSemanaEsLaSemanaLunesDomingoPrevia() {
        val p = anterior(PeriodoTipo.SEMANA, LocalDate.of(2026, 4, 27)) // lunes
        assertEquals(LocalDate.of(2026, 4, 20), p.desde)
        assertEquals(LocalDate.of(2026, 4, 26), p.hasta)
        // 1-ene-2026 es jueves: su semana es 29 dic - 4 ene; la anterior, 22-28 dic.
        val cruza = anterior(PeriodoTipo.SEMANA, LocalDate.of(2026, 1, 1))
        assertEquals(LocalDate.of(2025, 12, 22), cruza.desde)
        assertEquals(LocalDate.of(2025, 12, 28), cruza.hasta)
    }

    @Test
    fun anteriorDeMesEsElMesNaturalCompleto() {
        val feb = anterior(PeriodoTipo.MES, LocalDate.of(2026, 3, 15))
        assertEquals(LocalDate.of(2026, 2, 1), feb.desde)
        assertEquals(LocalDate.of(2026, 2, 28), feb.hasta)
        assertEquals(LocalDate.of(2028, 2, 29), anterior(PeriodoTipo.MES, LocalDate.of(2028, 3, 1)).hasta)
        val dic = anterior(PeriodoTipo.MES, LocalDate.of(2026, 1, 10))
        assertEquals(LocalDate.of(2025, 12, 1), dic.desde)
        assertEquals(LocalDate.of(2025, 12, 31), dic.hasta)
        assertEquals(LocalDate.of(2026, 4, 30), anterior(PeriodoTipo.MES, LocalDate.of(2026, 5, 31)).hasta)
    }

    @Test
    fun anteriorConservaTipoYEncadenaSinHuecos() {
        PeriodoTipo.entries.forEach { tipo ->
            val p = Periodo.de(tipo, LocalDate.of(2026, 3, 18))
            val a = p.anterior()
            assertEquals(tipo, a.tipo)
            assertEquals(p.desde.minusDays(1), a.hasta)
            assertEquals(a.desde.minusDays(1), a.anterior().hasta)
        }
    }

    @Test
    fun rechazaDesdePosteriorAHasta() {
        assertThrows(IllegalArgumentException::class.java) {
            Periodo(PeriodoTipo.DIA, LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 1))
        }
    }
}

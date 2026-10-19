package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.entities.Periodo
import com.robertomr.tally.domain.expenses.entities.PeriodoTipo
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * "Hoy" y período con un Clock fijo en Europe/Madrid, incluidos los cambios de
 * hora (spec periodo-selector, escenario 3).
 */
class ObtenerPeriodoTest {

    private val madrid = ZoneId.of("Europe/Madrid")

    /** `local` en hora de Madrid, p. ej. "2026-03-29T00:30". */
    private fun casoDe(local: String): ObtenerPeriodo {
        val instante = LocalDateTime.parse(local).atZone(madrid).toInstant()
        return ObtenerPeriodo(Clock.fixed(instante, madrid))
    }

    @Test
    fun cambioDeHoraDeMarzoNoDesplazaElDia() {
        listOf("2026-03-29T00:30", "2026-03-29T01:30", "2026-03-29T03:30", "2026-03-29T23:30").forEach {
            val obtener = casoDe(it)
            val dia = obtener(PeriodoTipo.DIA)
            assertEquals(it, LocalDate.of(2026, 3, 29), dia.desde)
            assertEquals(it, LocalDate.of(2026, 3, 29), dia.hasta)
            // 29-mar-2026 es domingo: la semana es lun 23 - dom 29.
            val semana = obtener(PeriodoTipo.SEMANA)
            assertEquals(it, LocalDate.of(2026, 3, 23), semana.desde)
            assertEquals(it, LocalDate.of(2026, 3, 29), semana.hasta)
            val mes = obtener(PeriodoTipo.MES)
            assertEquals(it, LocalDate.of(2026, 3, 1), mes.desde)
            assertEquals(it, LocalDate.of(2026, 3, 31), mes.hasta)
        }
    }

    @Test
    fun cambioDeHoraDeOctubreNoDesplazaElDia() {
        listOf("2026-10-25T00:30", "2026-10-25T01:30", "2026-10-25T03:30", "2026-10-25T23:30").forEach {
            val obtener = casoDe(it)
            val dia = obtener(PeriodoTipo.DIA)
            assertEquals(it, LocalDate.of(2026, 10, 25), dia.desde)
            assertEquals(it, LocalDate.of(2026, 10, 25), dia.hasta)
            // 25-oct-2026 es domingo.
            val semana = obtener(PeriodoTipo.SEMANA)
            assertEquals(it, LocalDate.of(2026, 10, 19), semana.desde)
            assertEquals(it, LocalDate.of(2026, 10, 25), semana.hasta)
            assertEquals(it, LocalDate.of(2026, 10, 31), obtener(PeriodoTipo.MES).hasta)
        }
    }

    @Test
    fun lasDosVecesQueOcurreLas0230DeOctubreSiguenSiendoElMismoDia() {
        // 02:30 existe dos veces el 25-oct (CEST y CET): ambos instantes son el 25.
        val base = ZonedDateTime.of(2026, 10, 25, 2, 30, 0, 0, madrid)
        listOf(base.withEarlierOffsetAtOverlap(), base.withLaterOffsetAtOverlap()).forEach {
            val dia = ObtenerPeriodo(Clock.fixed(it.toInstant(), madrid))(PeriodoTipo.DIA)
            assertEquals(LocalDate.of(2026, 10, 25), dia.desde)
        }
    }

    @Test
    fun nocheDel31DeDiciembreSigueEnEseDiaYAnio() {
        val obtener = casoDe("2025-12-31T23:30")
        assertEquals(LocalDate.of(2025, 12, 31), obtener(PeriodoTipo.DIA).desde)
        val semana = obtener(PeriodoTipo.SEMANA)
        assertEquals(LocalDate.of(2025, 12, 29), semana.desde)
        assertEquals(LocalDate.of(2026, 1, 4), semana.hasta)
        assertEquals(LocalDate.of(2025, 12, 1), obtener(PeriodoTipo.MES).desde)
    }

    @Test
    fun personalizadoDevuelveElRangoDado() {
        val rango = Periodo.personalizado(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 20))
        assertEquals(rango, casoDe("2026-10-25T10:00")(PeriodoTipo.PERSONALIZADO, rango))
    }

    @Test
    fun personalizadoSinRangoOConRangoDeOtroTipoLanza() {
        val obtener = casoDe("2026-10-25T10:00")
        assertThrows(IllegalArgumentException::class.java) { obtener(PeriodoTipo.PERSONALIZADO) }
        assertThrows(IllegalArgumentException::class.java) {
            obtener(PeriodoTipo.PERSONALIZADO, Periodo.de(PeriodoTipo.MES, LocalDate.of(2026, 10, 1)))
        }
    }

    @Test
    fun losDemasTiposIgnoranElRangoPersonalizado() {
        val rango = Periodo.personalizado(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 3))
        assertEquals(LocalDate.of(2026, 10, 25), casoDe("2026-10-25T10:00")(PeriodoTipo.DIA, rango).desde)
    }
}

package com.robertomr.tally.feature.common

import com.robertomr.tally.domain.expenses.entities.Periodo
import com.robertomr.tally.domain.expenses.entities.PeriodoTipo
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class EtiquetaPeriodoTest {

    @Test
    fun diaMuestraUnaSolaFecha() {
        assertEquals("18 Oct", etiquetaPeriodo(Periodo.de(PeriodoTipo.DIA, LocalDate.of(2026, 10, 18))))
    }

    @Test
    fun semanaMuestraElRangoLunesDomingo() {
        assertEquals("12 Oct – 18 Oct", etiquetaPeriodo(Periodo.de(PeriodoTipo.SEMANA, LocalDate.of(2026, 10, 18))))
    }

    @Test
    fun semanaQueCruzaMesMuestraAmbosMeses() {
        assertEquals("27 Abr – 3 May", etiquetaPeriodo(Periodo.de(PeriodoTipo.SEMANA, LocalDate.of(2026, 4, 29))))
    }

    @Test
    fun mesMuestraDelPrimeroAlUltimoDia() {
        assertEquals("1 Oct – 31 Oct", etiquetaPeriodo(Periodo.de(PeriodoTipo.MES, LocalDate.of(2026, 10, 18))))
        assertEquals("1 Feb – 28 Feb", etiquetaPeriodo(Periodo.de(PeriodoTipo.MES, LocalDate.of(2026, 2, 3))))
    }

    @Test
    fun importeSeFormateaEnEspanol() {
        assertEquals("1.428,50 €", formatoImporte(1428.5))
        assertEquals("0,00 €", formatoImporte(0.0))
    }

    // --- Personalizado (spec rango-personalizado, D7) ---

    private val hoy = LocalDate.of(2026, 10, 18)

    @Test
    fun personalizadoDelAnioActualNoLlevaAnio() {
        val p = Periodo.personalizado(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 20))
        assertEquals("12 Oct – 20 Oct", etiquetaPeriodo(p, hoy))
        assertEquals("18 Oct", etiquetaPeriodo(Periodo.personalizado(hoy, hoy), hoy))
    }

    @Test
    fun personalizadoQueCruzaAnioLlevaAnioEnAmbosExtremos() {
        val p = Periodo.personalizado(LocalDate.of(2025, 12, 28), LocalDate.of(2026, 1, 4))
        assertEquals("28 Dic 2025 – 4 Ene 2026", etiquetaPeriodo(p, hoy))
        assertEquals("28 Dic 2025 – 4 Ene 2026", etiquetaPeriodo(p))
    }

    @Test
    fun personalizadoDeOtroAnioLlevaAnio() {
        val p = Periodo.personalizado(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 5))
        assertEquals("1 Mar 2025 – 5 Mar 2025", etiquetaPeriodo(p, hoy))
        assertEquals("1 Mar 2025", etiquetaPeriodo(Periodo.personalizado(p.desde, p.desde), hoy))
    }

    @Test
    fun diaSemanaYMesNuncaLlevanAnio() {
        val dic = LocalDate.of(2025, 12, 31)
        assertEquals("29 Dic – 4 Ene", etiquetaPeriodo(Periodo.de(PeriodoTipo.SEMANA, dic), hoy))
        assertEquals("1 Dic – 31 Dic", etiquetaPeriodo(Periodo.de(PeriodoTipo.MES, dic), hoy))
    }
}

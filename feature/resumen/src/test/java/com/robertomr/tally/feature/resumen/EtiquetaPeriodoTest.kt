package com.robertomr.tally.feature.resumen

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
}

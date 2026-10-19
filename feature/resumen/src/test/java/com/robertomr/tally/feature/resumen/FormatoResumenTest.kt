package com.robertomr.tally.feature.resumen

import com.robertomr.tally.feature.common.formatoImporte
import org.junit.Assert.assertEquals
import org.junit.Test

/** Formato español de la tarjeta de total (spec resumen-total, escenario 6). */
class FormatoResumenTest {

    @Test
    fun importeConMilesYDecimalesEnEspanol() {
        assertEquals("1.428,50 €", formatoImporte(1428.5))
        assertEquals("0,00 €", formatoImporte(0.0))
    }

    @Test
    fun promedioDiarioLlevaElSufijo() {
        assertEquals("46,08 € / día", formatoPromedio(46.08))
        assertEquals("0,00 € / día", formatoPromedio(0.0))
    }

    @Test
    fun variacionConComaUnDecimalYSignoExplicitoEnPositivos() {
        assertEquals("-4,2 %", formatoVariacion(-0.042))
        assertEquals("+51,3 %", formatoVariacion(0.5125))
        assertEquals("-100,0 %", formatoVariacion(-1.0))
    }

    @Test
    fun variacionCeroNoLlevaSigno() {
        assertEquals("0,0 %", formatoVariacion(0.0))
        assertEquals("0,0 %", formatoVariacion(0.0004))
        assertEquals("0,0 %", formatoVariacion(-0.0004))
    }

    @Test
    fun variacionNulaEsTextoSinReferencia() {
        assertEquals("Sin datos del período anterior", formatoVariacion(null))
    }

    @Test
    fun textoDeTicketsSingularYPlural() {
        assertEquals("1 ticket", textoTickets(1))
        assertEquals("0 tickets", textoTickets(0))
        assertEquals("38 tickets", textoTickets(38))
    }
}

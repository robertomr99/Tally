package com.robertomr.tally.domain.expenses.entities

import org.junit.Assert.assertEquals
import org.junit.Test

/** Tests JVM de [FiltroFormaPago.admite] (spec filtro-forma-pago): 3 formas x 3 filtros. */
class FiltroFormaPagoTest {

    private fun admitidas(filtro: FiltroFormaPago) = FormaPago.entries.filter { filtro.admite(it) }.toSet()

    @Test
    fun todosAdmiteLasTresFormasIncluidaNoIndicada() {
        assertEquals(setOf(FormaPago.EFECTIVO, FormaPago.TARJETA, FormaPago.NO_INDICADA), admitidas(FiltroFormaPago.TODOS))
    }

    @Test
    fun efectivoSoloAdmiteEfectivo() {
        assertEquals(setOf(FormaPago.EFECTIVO), admitidas(FiltroFormaPago.EFECTIVO))
    }

    @Test
    fun tarjetaSoloAdmiteTarjeta() {
        assertEquals(setOf(FormaPago.TARJETA), admitidas(FiltroFormaPago.TARJETA))
    }
}

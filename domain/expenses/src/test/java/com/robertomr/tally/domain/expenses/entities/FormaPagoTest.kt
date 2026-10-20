package com.robertomr.tally.domain.expenses.entities

import org.junit.Assert.assertEquals
import org.junit.Test

/** Tests JVM de [FormaPago.desde] (spec forma-pago): tolerante, nunca lanza. */
class FormaPagoTest {

    @Test
    fun valoresExactosSeMapean() {
        assertEquals(FormaPago.EFECTIVO, FormaPago.desde("EFECTIVO"))
        assertEquals(FormaPago.TARJETA, FormaPago.desde("TARJETA"))
        assertEquals(FormaPago.NO_INDICADA, FormaPago.desde("NO_INDICADA"))
    }

    @Test
    fun ignoraMayusculasEspaciosYTildes() {
        assertEquals(FormaPago.EFECTIVO, FormaPago.desde("  efectivo "))
        assertEquals(FormaPago.TARJETA, FormaPago.desde("Tarjeta"))
        assertEquals(FormaPago.EFECTIVO, FormaPago.desde("EFÉCTIVO"))
    }

    @Test
    fun desconocidoONuloEsNoIndicada() {
        assertEquals(FormaPago.NO_INDICADA, FormaPago.desde(null))
        assertEquals(FormaPago.NO_INDICADA, FormaPago.desde(""))
        assertEquals(FormaPago.NO_INDICADA, FormaPago.desde("BIZUM"))
        assertEquals(FormaPago.NO_INDICADA, FormaPago.desde("mixto"))
        assertEquals(FormaPago.NO_INDICADA, FormaPago.desde("valor futuro de otra version"))
    }
}

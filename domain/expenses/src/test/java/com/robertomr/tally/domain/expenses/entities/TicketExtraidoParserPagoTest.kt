package com.robertomr.tally.domain.expenses.entities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests JVM del parser con la forma de pago (spec forma-pago). Los JSON de
 * ejemplo reproducen lo que un motor DEBERIA devolver para los 4 textos crudos
 * de `app/src/androidTest/assets` (la garantia fuerte, porque la cuota de
 * Gemini puede no estar disponible).
 */
class TicketExtraidoParserPagoTest {

    private fun parse(json: String) = TicketExtraidoParser.parse(json, Motor.NUBE)

    @Test
    fun texto1SinIndiciosEsNoIndicada() {
        val r = parse("""{"comercio":"MERCADONA","total":35.30,"categoria":"ALIMENTACION","forma_pago":"NO_INDICADA","entregado":null,"cambio":null}""")
        assertEquals(35.30, r.total!!, 0.0)
        assertEquals(FormaPago.NO_INDICADA, r.formaPago)
        assertNull(r.entregado)
        assertNull(r.cambio)
    }

    @Test
    fun texto2TarjetaSinEntregadoNiCambio() {
        // "A DEVOLVER 0,00" no es cambio guardado: el modelo devuelve null.
        val r = parse("""{"total":3.50,"categoria":"ALIMENTACION","forma_pago":"TARJETA","entregado":null,"cambio":null}""")
        assertEquals(3.50, r.total!!, 0.0)
        assertEquals(FormaPago.TARJETA, r.formaPago)
        assertNull(r.entregado)
        assertNull(r.cambio)
    }

    @Test
    fun texto3EfectivoConEntregadoYCambio() {
        val r = parse("""{"total":13.90,"categoria":"ALIMENTACION","forma_pago":"EFECTIVO","entregado":50.00,"cambio":36.10}""")
        assertEquals(13.90, r.total!!, 0.0)
        assertEquals(FormaPago.EFECTIVO, r.formaPago)
        assertEquals(50.0, r.entregado!!, 0.0)
        assertEquals(36.10, r.cambio!!, 0.0)
    }

    @Test
    fun texto4EfectivoDerivaEntregadoDeTotalMasCambio() {
        val r = parse("""{"total":7.91,"categoria":"ALIMENTACION","forma_pago":"EFECTIVO","cambio":2.09}""")
        assertEquals(7.91, r.total!!, 0.0)
        assertEquals(FormaPago.EFECTIVO, r.formaPago)
        assertEquals(10.0, r.entregado!!, 0.0)
        assertEquals(2.09, r.cambio!!, 0.0)
    }

    @Test
    fun texto4ConEntregadoExplicito() {
        val r = parse("""{"total":7.91,"categoria":"OTROS","forma_pago":"EFECTIVO","entregado":10.00,"cambio":2.09}""")
        assertEquals(10.0, r.entregado!!, 0.0)
    }

    @Test
    fun jsonSinLosCamposNuevosSigueSiendoValido() {
        val r = parse("""{"total":5.0,"categoria":"OTROS"}""")
        assertEquals(FormaPago.NO_INDICADA, r.formaPago)
        assertNull(r.entregado)
        assertNull(r.cambio)
        assertEquals(5.0, r.total!!, 0.0)
    }

    @Test
    fun formaDesconocidaEsNoIndicada() {
        assertEquals(FormaPago.NO_INDICADA, parse("""{"categoria":"OTROS","forma_pago":"BIZUM"}""").formaPago)
        assertEquals(FormaPago.NO_INDICADA, parse("""{"categoria":"OTROS","forma_pago":"MIXTO"}""").formaPago)
        assertEquals(FormaPago.NO_INDICADA, parse("""{"categoria":"OTROS","forma_pago":null}""").formaPago)
    }

    @Test
    fun formaEnMinusculasSeAcepta() {
        assertEquals(FormaPago.TARJETA, parse("""{"categoria":"OTROS","forma_pago":"tarjeta"}""").formaPago)
    }

    @Test
    fun entregadoConTarjetaSeLimpia() {
        val r = parse("""{"total":3.5,"categoria":"OTROS","forma_pago":"TARJETA","entregado":10.0,"cambio":6.5}""")
        assertEquals(FormaPago.TARJETA, r.formaPago)
        assertNull(r.entregado)
        assertNull(r.cambio)
    }

    @Test
    fun entregadoSinFormaInfiereEfectivo() {
        val r = parse("""{"total":13.9,"categoria":"OTROS","entregado":"50,00"}""")
        assertEquals(FormaPago.EFECTIVO, r.formaPago)
        assertEquals(50.0, r.entregado!!, 0.0)
        assertEquals(36.10, r.cambio!!, 0.0)
    }

    @Test
    fun importesIlegiblesONegativosSeDescartanSinInvalidarLaExtraccion() {
        val r = parse("""{"total":13.9,"categoria":"OTROS","forma_pago":"EFECTIVO","entregado":"mucho","cambio":-2}""")
        assertEquals(13.9, r.total!!, 0.0)
        assertEquals(FormaPago.EFECTIVO, r.formaPago)
        assertNull(r.entregado)
        assertNull(r.cambio)
    }

    @Test
    fun elTotalNuncaSeSustituyePorEntregadoOCambio() {
        val r = parse("""{"total":13.90,"categoria":"OTROS","forma_pago":"EFECTIVO","entregado":50.0,"cambio":36.10}""")
        assertEquals(13.90, r.total!!, 0.0)
    }
}

package com.robertomr.tally.domain.expenses.entities

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests JVM de las reglas puras de pago (spec forma-pago): normalizacion,
 * aviso de coherencia y reductores de edicion. El total NUNCA cambia por
 * entregado o cambio.
 */
class PagoTest {

    private val base = DatosRevision(
        comercio = "SUPER",
        fechaEmision = LocalDateTime.of(2026, 10, 15, 13, 45),
        total = 13.90,
        categoria = Categoria.ALIMENTACION,
        lineas = emptyList(),
    )

    // --- normalizarPago -------------------------------------------------------

    @Test
    fun entregadoOCambioSinFormaInfierenEfectivo() {
        val r = normalizarPago(FormaPago.NO_INDICADA, 13.90, 50.0, 36.10)
        assertEquals(FormaPago.EFECTIVO, r.formaPago)
        assertEquals(50.0, r.entregado!!, 0.0)
        assertEquals(36.10, r.cambio!!, 0.0)
        assertEquals(FormaPago.EFECTIVO, normalizarPago(FormaPago.NO_INDICADA, 7.91, null, 2.09).formaPago)
    }

    @Test
    fun tarjetaYNoIndicadaSinImportesLimpianEntregadoYCambio() {
        val tarjeta = normalizarPago(FormaPago.TARJETA, 3.50, 10.0, 6.50)
        assertEquals(FormaPago.TARJETA, tarjeta.formaPago)
        assertNull(tarjeta.entregado)
        assertNull(tarjeta.cambio)
        val nada = normalizarPago(FormaPago.NO_INDICADA, 3.50, null, null)
        assertEquals(FormaPago.NO_INDICADA, nada.formaPago)
        assertNull(nada.entregado)
    }

    @Test
    fun efectivoDerivaCambioDeEntregadoYTotal() {
        val r = normalizarPago(FormaPago.EFECTIVO, 13.90, 50.0, null)
        assertEquals(36.10, r.cambio!!, 0.0)
    }

    @Test
    fun efectivoDerivaEntregadoDeTotalMasCambio() {
        // Texto crudo 4: total 7,91 y cambio 2,09 -> entregado 10,00.
        val r = normalizarPago(FormaPago.EFECTIVO, 7.91, null, 2.09)
        assertEquals(10.0, r.entregado!!, 0.0)
        assertEquals(2.09, r.cambio!!, 0.0)
    }

    @Test
    fun entregadoMenorQueTotalNoDerivaCambio() {
        val r = normalizarPago(FormaPago.EFECTIVO, 13.90, 5.0, null)
        assertEquals(5.0, r.entregado!!, 0.0)
        assertNull(r.cambio)
    }

    @Test
    fun negativosYNoFinitosSeDescartan() {
        val r = normalizarPago(FormaPago.EFECTIVO, 10.0, -5.0, Double.NaN)
        assertNull(r.entregado)
        assertNull(r.cambio)
        assertEquals(FormaPago.EFECTIVO, r.formaPago)
        val sinForma = normalizarPago(FormaPago.NO_INDICADA, 10.0, -1.0, Double.POSITIVE_INFINITY)
        assertEquals(FormaPago.NO_INDICADA, sinForma.formaPago)
    }

    @Test
    fun totalDesconocidoNoDeriva() {
        val r = normalizarPago(FormaPago.EFECTIVO, 0.0, 20.0, null)
        assertNull(r.cambio)
        assertNull(normalizarPago(FormaPago.EFECTIVO, null, null, 2.0).entregado)
    }

    @Test
    fun normalizadaDeDatosNoTocaElTotal() {
        val r = base.copy(formaPago = FormaPago.TARJETA, entregado = 50.0, cambio = 36.10).normalizada()
        assertEquals(13.90, r.total, 0.0)
        assertNull(r.entregado)
        assertNull(r.cambio)
    }

    // --- avisoPago ------------------------------------------------------------

    @Test
    fun avisoEntregadoMenorQueTotal() {
        val d = base.copy(formaPago = FormaPago.EFECTIVO, entregado = 10.0)
        assertEquals(AvisoPago.ENTREGADO_MENOR_QUE_TOTAL, avisoPago(d))
    }

    @Test
    fun avisoCambioNoCuadra() {
        val d = base.copy(formaPago = FormaPago.EFECTIVO, entregado = 50.0, cambio = 30.0)
        assertEquals(AvisoPago.CAMBIO_NO_CUADRA, avisoPago(d))
    }

    @Test
    fun sinAvisoSiCuadraDentroDeLaTolerancia() {
        assertNull(avisoPago(base.copy(formaPago = FormaPago.EFECTIVO, entregado = 50.0, cambio = 36.10)))
        assertNull(avisoPago(base.copy(formaPago = FormaPago.EFECTIVO, entregado = 50.0, cambio = 36.105)))
        assertNull(avisoPago(base.copy(formaPago = FormaPago.EFECTIVO)))
    }

    @Test
    fun sinAvisoSiNoEsEfectivo() {
        assertNull(avisoPago(base.copy(formaPago = FormaPago.TARJETA, entregado = 1.0, cambio = 99.0)))
        assertNull(avisoPago(base))
    }

    // --- reductores -----------------------------------------------------------

    @Test
    fun entregadoCalculaElCambioYNoTocaElTotal() {
        val r = base.conFormaPago(FormaPago.EFECTIVO).conEntregadoTexto("50,00")!!
        assertEquals(50.0, r.entregado!!, 0.0)
        assertEquals(36.10, r.cambio!!, 0.0)
        assertEquals(13.90, r.total, 0.0)
    }

    @Test
    fun cambioEsEditableTrasElCalculo() {
        val r = base.conFormaPago(FormaPago.EFECTIVO)
            .conEntregadoTexto("50")!!
            .conCambioTexto("30,00")!!
        assertEquals(30.0, r.cambio!!, 0.0)
        assertEquals(AvisoPago.CAMBIO_NO_CUADRA, avisoPago(r))
    }

    @Test
    fun cambiarElTotalRecalculaElCambio() {
        val r = base.conFormaPago(FormaPago.EFECTIVO).conEntregadoTexto("50")!!
            .conTotalTexto("20")!!
        assertEquals(30.0, r.cambio!!, 0.0)
    }

    @Test
    fun textoInvalidoSeIgnoraYBlancoLimpia() {
        val r = base.conFormaPago(FormaPago.EFECTIVO).conEntregadoTexto("50")!!
        assertNull(r.conEntregadoTexto("cincuenta"))
        assertNull(r.conEntregadoTexto("-3"))
        assertNull(r.conCambioTexto("abc"))
        assertNull(r.conEntregadoTexto("")!!.entregado)
        assertNull(r.conCambioTexto("  ")!!.cambio)
    }

    @Test
    fun deEfectivoATarjetaConservaOcultosYSeLimpianAlGuardar() {
        val efectivo = base.conFormaPago(FormaPago.EFECTIVO).conEntregadoTexto("50")!!
        val tarjeta = efectivo.conFormaPago(FormaPago.TARJETA)
        assertEquals(50.0, tarjeta.entregado!!, 0.0)
        assertEquals(36.10, tarjeta.cambio!!, 0.0)
        // Volver a efectivo antes de guardar los recupera.
        assertEquals(50.0, tarjeta.conFormaPago(FormaPago.EFECTIVO).entregado!!, 0.0)
        // Al guardar se limpian.
        val guardado = tarjeta.normalizada()
        assertNull(guardado.entregado)
        assertNull(guardado.cambio)
        assertEquals(FormaPago.TARJETA, guardado.formaPago)
    }

    @Test
    fun datosRevisionInicialYDeTicketCopianLosCamposDePago() {
        val extraido = TicketExtraido(
            total = 13.90,
            motor = Motor.NUBE,
            formaPago = FormaPago.EFECTIVO,
            entregado = 50.0,
            cambio = 36.10,
        )
        val d = datosRevisionInicial(extraido)
        assertEquals(FormaPago.EFECTIVO, d.formaPago)
        assertEquals(50.0, d.entregado!!, 0.0)
        val t = Ticket(
            comercio = "X",
            fechaEmision = LocalDateTime.of(2026, 1, 1, 0, 0),
            total = 13.90,
            categoria = Categoria.OTROS,
            fechaCaptura = LocalDateTime.of(2026, 1, 1, 0, 0),
            motor = Motor.NUBE,
            rutaFoto = "f",
            formaPago = FormaPago.TARJETA,
        )
        assertEquals(FormaPago.TARJETA, datosRevisionDe(t).formaPago)
    }
}

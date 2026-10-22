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
    fun cambioNoEsEditableSeDerivaSiempreDeEntregadoYTotal() {
        // spec gasto-manual-2: ya no hay reductor de cambio; entregado 50 -> 36,10
        // y un cambio ajeno en el estado se recalcula al tocar entregado o total.
        val r = base.conFormaPago(FormaPago.EFECTIVO)
            .conEntregadoTexto("50")!!
            .copy(cambio = 30.0)
            .conEntregadoTexto("50")!!
        assertEquals(36.10, r.cambio!!, 0.0)
        assertNull(avisoPago(r))
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
        assertNull(r.conEntregadoTexto("")!!.entregado)
        assertNull(r.conEntregadoTexto("  ")!!.entregado)
        assertNull(r.conEntregadoTexto("")!!.cambio)
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

    // --- cambio derivado (spec gasto-manual-2, A) ----------------------------------

    @Test
    fun entregadoMenorQueTotalDejaCambioNuloNuncaNegativo() {
        val r = base.conFormaPago(FormaPago.EFECTIVO).conEntregadoTexto("5")!!
        assertNull(r.cambio)
        assertEquals(AvisoPago.ENTREGADO_MENOR_QUE_TOTAL, avisoPago(r))
    }

    @Test
    fun entregadoVacioLimpiaEntregadoYCambio() {
        val r = base.conFormaPago(FormaPago.EFECTIVO).conEntregadoTexto("50")!!
        val vacio = r.conEntregadoTexto("")!!
        assertNull(vacio.entregado)
        assertNull(vacio.cambio)
        assertNull(avisoPago(vacio))
    }

    @Test
    fun entregadoExactoDaCambioCero() {
        val r = base.conFormaPago(FormaPago.EFECTIVO).conEntregadoTexto("13,90")!!
        assertEquals(0.0, r.cambio!!, 0.0)
    }

    @Test
    fun cambiarFormaPagoAEfectivoRecalculaConLoYaEntregado() {
        val r = base.copy(entregado = 20.0).conFormaPago(FormaPago.EFECTIVO)
        assertEquals(6.10, r.cambio!!, 0.0)
    }

    @Test
    fun normalizarPagoIgnoraElCambioExtraidoQueContradice() {
        // E=50, C=30 (la IA se equivoco), T=13,90 -> 36,10.
        val r = normalizarPago(FormaPago.EFECTIVO, 13.90, 50.0, 30.0)
        assertEquals(50.0, r.entregado!!, 0.0)
        assertEquals(36.10, r.cambio!!, 0.0)
        // Sin cubrir el total el cambio extraido tampoco se conserva.
        assertNull(normalizarPago(FormaPago.EFECTIVO, 13.90, 5.0, 30.0).cambio)
    }

    @Test
    fun normalizadaPersisteElCambioCalculadoAunqueElDatoTraigaOtro() {
        val d = base.copy(formaPago = FormaPago.EFECTIVO, entregado = 50.0, cambio = 30.0).normalizada()
        assertEquals(36.10, d.cambio!!, 0.0)
    }

    @Test
    fun conTotalDesconocidoNormalizarPagoConservaLoExtraido() {
        val r = normalizarPago(FormaPago.EFECTIVO, 0.0, 20.0, 5.0)
        assertEquals(20.0, r.entregado!!, 0.0)
        assertEquals(5.0, r.cambio!!, 0.0)
    }

    @Test
    fun soloCambioExtraidoInfiereEntregadoAlAbrirYElUsuarioPuedeBorrarlo() {
        val extraido = TicketExtraido(
            total = 7.91,
            motor = Motor.NUBE,
            formaPago = FormaPago.EFECTIVO,
            entregado = null,
            cambio = 2.09,
        )
        val d = datosRevisionInicial(extraido)
        assertEquals(10.0, d.entregado!!, 0.0)
        assertEquals(2.09, d.cambio!!, 0.0)
        val borrado = d.conEntregadoTexto("")!!
        assertNull(borrado.entregado)
        assertNull(borrado.cambio)
        // Tocar el total despues no reinventa entregado.
        assertNull(borrado.conTotalTexto("9")!!.entregado)
    }

    @Test
    fun datosRevisionInicialRecalculaElCambioExtraidoQueContradice() {
        val extraido = TicketExtraido(
            total = 13.90,
            motor = Motor.NUBE,
            formaPago = FormaPago.EFECTIVO,
            entregado = 50.0,
            cambio = 30.0,
        )
        assertEquals(36.10, datosRevisionInicial(extraido).cambio!!, 0.0)
    }

    @Test
    fun tarjetaYNoIndicadaConservanOcultosSinRecalcular() {
        val tarjeta = base.copy(formaPago = FormaPago.TARJETA, entregado = 50.0, cambio = 30.0)
        val r = tarjeta.conTotalTexto("20")!!
        assertEquals(50.0, r.entregado!!, 0.0)
        assertEquals(30.0, r.cambio!!, 0.0)
    }
}

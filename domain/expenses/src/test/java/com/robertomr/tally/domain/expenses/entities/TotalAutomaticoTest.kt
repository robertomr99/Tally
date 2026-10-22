package com.robertomr.tally.domain.expenses.entities

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests JVM del total unico (spec gasto-manual-2, B): modos AUTOMATICO / TECLEADO. */
class TotalAutomaticoTest {

    private val manual = datosGastoManualInicial()

    private fun linea(id: Long, concepto: String, cantidad: Int, precio: Double) =
        LineaEditable(id, concepto, cantidad, precio)

    private fun extraido(total: Double?, vararg lineas: Linea) = TicketExtraido(
        total = total,
        lineas = lineas.toList(),
        motor = Motor.NUBE,
    )

    // --- totalDeLineas ---------------------------------------------------------

    @Test
    fun totalDeLineasIgnoraConceptoEnBlancoYRedondea() {
        val lineas = listOf(
            linea(0, "PAN", 3, 1.50),
            linea(1, "  ", 5, 9.99),
            linea(2, "AGUA", 1, 0.337),
        )
        assertEquals(4.84, totalDeLineas(lineas), 0.0)
        assertEquals(0.0, totalDeLineas(emptyList()), 0.0)
    }

    // --- B3: estado inicial -----------------------------------------------------

    @Test
    fun nuevoGastoArrancaTecleadoConTotalCero() {
        assertFalse(manual.totalAutomatico)
        assertEquals(0.0, manual.total, 0.0)
    }

    @Test
    fun inicialSinLineasEsTecleadoYConservaElTotal() {
        val d = datosRevisionInicial(extraido(13.90))
        assertFalse(d.totalAutomatico)
        assertEquals(13.90, d.total, 0.0)
    }

    @Test
    fun inicialConLineasQueCuadranEsAutomatico() {
        val d = datosRevisionInicial(extraido(6.50, Linea("PAN", 3, 1.50), Linea("AGUA", 1, 2.0)))
        assertTrue(d.totalAutomatico)
        assertEquals(6.50, d.total, 0.0)
    }

    @Test
    fun inicialConLineasYTotalAusenteTomaLaSuma() {
        val d = datosRevisionInicial(extraido(null, Linea("PAN", 2, 1.05)))
        assertTrue(d.totalAutomatico)
        assertEquals(2.10, d.total, 0.0)
        val cero = datosRevisionInicial(extraido(0.0, Linea("PAN", 2, 1.05)))
        assertTrue(cero.totalAutomatico)
        assertEquals(2.10, cero.total, 0.0)
    }

    @Test
    fun inicialConLineasQueNoCuadranEsTecleadoYConservaElTotalDelPapel() {
        val d = datosRevisionInicial(extraido(35.30, Linea("PAN", 2, 1.05)))
        assertFalse(d.totalAutomatico)
        assertEquals(35.30, d.total, 0.0)
    }

    @Test
    fun datosRevisionDeAplicaLosMismosCasos() {
        fun ticket(total: Double, vararg lineas: Linea) = Ticket(
            comercio = "X",
            fechaEmision = LocalDateTime.of(2026, 1, 1, 0, 0),
            total = total,
            categoria = Categoria.OTROS,
            fechaCaptura = LocalDateTime.of(2026, 1, 1, 0, 0),
            motor = Motor.NUBE,
            rutaFoto = "f",
            lineas = lineas.toList(),
        )
        assertTrue(datosRevisionDe(ticket(2.10, Linea("PAN", 2, 1.05))).totalAutomatico)
        val noCuadra = datosRevisionDe(ticket(10.0, Linea("PAN", 3, 1.30)))
        assertFalse(noCuadra.totalAutomatico)
        assertEquals(10.0, noCuadra.total, 0.0)
        assertFalse(datosRevisionDe(ticket(10.0)).totalAutomatico)
    }

    // --- B4: reductores de linea -------------------------------------------------

    @Test
    fun primeraLineaConTotalCeroPasaAAutomaticoYSigueLasEdiciones() {
        var d = manual.conLineaAnadida(0)
        assertTrue(d.totalAutomatico)
        d = d.conLineaConcepto(0, "PAN").conLineaCantidad(0, "3")!!.conLineaPrecio(0, "1,50")!!
        assertEquals(4.50, d.total, 0.0)
        d = d.conLineaAnadida(1).conLineaConcepto(1, "AGUA").conLineaPrecio(1, "2")!!
        assertEquals(6.50, d.total, 0.0)
        d = d.conLineaCantidad(0, "2")!!
        assertEquals(5.0, d.total, 0.0)
        d = d.sinLinea(1)
        assertEquals(3.0, d.total, 0.0)
        assertTrue(d.totalAutomatico)
    }

    @Test
    fun borrarLaUltimaLineaVuelveATecleadoConservandoLaCifra() {
        val d = manual.conLineaAnadida(0).conLineaConcepto(0, "PAN").conLineaPrecio(0, "6,5")!!
            .sinLinea(0)
        assertFalse(d.totalAutomatico)
        assertEquals(6.50, d.total, 0.0)
    }

    @Test
    fun primeraLineaConTotalTecleadoNoLoToca() {
        val d = manual.conTotalTexto("20")!!.conLineaAnadida(0)
            .conLineaConcepto(0, "PAN").conLineaPrecio(0, "5")!!
        assertFalse(d.totalAutomatico)
        assertEquals(20.0, d.total, 0.0)
    }

    @Test
    fun tecleadoQueSeCuadraTrasEditarPasaAAutomatico() {
        var d = manual.conTotalTexto("20")!!.conLineaAnadida(0)
            .conLineaConcepto(0, "PAN").conLineaPrecio(0, "5")!!
        assertFalse(d.totalAutomatico)
        d = d.conLineaPrecio(0, "20")!!
        assertTrue(d.totalAutomatico)
        assertEquals(20.0, d.total, 0.0)
        d = d.conLineaPrecio(0, "21")!!
        assertEquals(21.0, d.total, 0.0)
    }

    @Test
    fun enTecleadoEditarLineasQueNoCuadranNoCambiaElTotal() {
        val d = datosRevisionInicial(extraido(35.30, Linea("PAN", 2, 1.05)))
            .conLineaAnadida(5).conLineaConcepto(5, "X").conLineaPrecio(5, "1")!!
        assertFalse(d.totalAutomatico)
        assertEquals(35.30, d.total, 0.0)
    }

    // --- B5: transiciones -----------------------------------------------------------

    @Test
    fun usarLaSumaYEditarAMano() {
        val base = manual.conTotalTexto("20")!!.conLineaAnadida(0)
            .conLineaConcepto(0, "PAN").conLineaPrecio(0, "5")!!
        val auto = base.conTotalAutomatico(true)
        assertTrue(auto.totalAutomatico)
        assertEquals(5.0, auto.total, 0.0)
        val tecleado = auto.conTotalAutomatico(false)
        assertFalse(tecleado.totalAutomatico)
        assertEquals(5.0, tecleado.total, 0.0)
    }

    @Test
    fun usarLaSumaSinLineasNoHaceNada() {
        val d = manual.conTotalTexto("12,5")!!.conTotalAutomatico(true)
        assertFalse(d.totalAutomatico)
        assertEquals(12.5, d.total, 0.0)
    }

    @Test
    fun conTotalTextoSeIgnoraEnAutomatico() {
        val auto = manual.conLineaAnadida(0).conLineaConcepto(0, "PAN").conLineaPrecio(0, "3")!!
        assertNull(auto.conTotalTexto("99"))
    }

    @Test
    fun totalTecleadoSinLineasSigueEditable() {
        val d = manual.conTotalTexto("12,5")!!
        assertEquals(12.5, d.total, 0.0)
        assertFalse(d.totalAutomatico)
    }

    @Test
    fun alCambiarElTotalAutomaticoElCambioSeRecalcula() {
        val d = manual.conFormaPago(FormaPago.EFECTIVO).conEntregadoTexto("20")!!
            .conLineaAnadida(0).conLineaConcepto(0, "PAN").conLineaPrecio(0, "6,5")!!
        assertEquals(13.50, d.cambio!!, 0.0)
        assertEquals(10.50, d.conLineaPrecio(0, "9,5")!!.cambio!!, 0.0)
    }

    // --- normalizada ------------------------------------------------------------------

    @Test
    fun normalizadaReSincronizaElTotalAutomatico() {
        val d = manual.conLineaAnadida(0).conLineaConcepto(0, "PAN").conLineaPrecio(0, "6,5")!!
            .copy(total = 1.0)
        assertEquals(6.50, d.normalizada().total, 0.0)
        // Tecleado: no se toca.
        assertEquals(1.0, d.copy(totalAutomatico = false).normalizada().total, 0.0)
    }
}

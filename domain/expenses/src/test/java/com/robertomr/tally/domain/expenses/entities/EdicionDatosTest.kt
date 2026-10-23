package com.robertomr.tally.domain.expenses.entities

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests JVM de los reductores puros de edicion (spec detalle-ticket): texto
 * invalido = null, `conservarHora` true/false, `datosRevisionDe` con 0 y N
 * lineas e ids estables.
 */
class EdicionDatosTest {

    private val base = DatosRevision(
        comercio = "MERCADONA",
        fechaEmision = LocalDateTime.of(2026, 10, 15, 13, 45),
        total = 35.30,
        categoria = Categoria.ALIMENTACION,
        lineas = listOf(
            LineaEditable(0, "LECHE", 2, 1.05),
            LineaEditable(1, "PAN", 1, 0.90),
        ),
    )

    @Test
    fun fechaValidaSinConservarHoraVaAMedianoche() {
        val r = base.conFechaTexto("3/11/2026", conservarHora = false)
        assertEquals(LocalDateTime.of(2026, 11, 3, 0, 0), r?.fechaEmision)
    }

    @Test
    fun fechaValidaConservandoHoraMantieneLaHora() {
        val r = base.conFechaTexto(" 3/11/2026 ", conservarHora = true)
        assertEquals(LocalDateTime.of(2026, 11, 3, 13, 45), r?.fechaEmision)
    }

    @Test
    fun fechaInvalidaDevuelveNull() {
        assertNull(base.conFechaTexto("32/1/2026", conservarHora = true))
        assertNull(base.conFechaTexto("abc", conservarHora = false))
        assertNull(base.conFechaTexto("", conservarHora = false))
    }

    @Test
    fun totalAceptaComaYPuntoEIgnoraTextoInvalido() {
        assertEquals(12.5, base.conTotalTexto("12,5")?.total ?: 0.0, 0.0)
        assertEquals(12.5, base.conTotalTexto("12.5")?.total ?: 0.0, 0.0)
        assertNull(base.conTotalTexto("doce"))
        assertNull(base.conTotalTexto(""))
    }

    @Test
    fun anadirLineaUsaElIdDadoYNoTocaElTotal() {
        val r = base.conLineaAnadida(7)
        assertEquals(3, r.lineas.size)
        assertEquals(nuevaLineaEditable(7), r.lineas.last())
        assertEquals(base.total, r.total, 0.0)
    }

    @Test
    fun editarLineaSoloAfectaALaDireccionadaPorId() {
        val r = base.conLineaConcepto(1, "PAN INTEGRAL")
        assertEquals("LECHE", r.lineas[0].concepto)
        assertEquals("PAN INTEGRAL", r.lineas[1].concepto)
    }

    @Test
    fun cantidadYPrecioInvalidosDevuelvenNull() {
        assertNull(base.conLineaCantidad(0, "0"))
        assertNull(base.conLineaCantidad(0, "1,5"))
        assertNull(base.conLineaCantidad(0, "x"))
        assertEquals(4, base.conLineaCantidad(0, "4")?.lineas?.get(0)?.cantidad)
        assertNull(base.conLineaPrecio(0, "-1"))
        assertNull(base.conLineaPrecio(0, "abc"))
        assertEquals(2.5, base.conLineaPrecio(0, "2,5")?.lineas?.get(0)?.precioUnitario ?: 0.0, 0.0)
        assertEquals(0.0, base.conLineaPrecio(0, "")?.lineas?.get(0)?.precioUnitario ?: -1.0, 0.0)
    }

    @Test
    fun sinLineaEliminaPorIdSinCambiarElTotal() {
        val r = base.sinLinea(0)
        assertEquals(listOf(1L), r.lineas.map { it.id })
        assertEquals(base.total, r.total, 0.0)
    }

    @Test
    fun datosRevisionDeTicketConLineasAsignaIdsCorrelativos() {
        val ticket = Ticket(
            id = 9,
            comercio = "LIDL",
            fechaEmision = LocalDateTime.of(2026, 10, 1, 8, 5),
            total = 10.0,
            categoria = Categoria.HOGAR,
            fechaCaptura = LocalDateTime.of(2026, 10, 1, 9, 0),
            motor = Motor.LOCAL,
            rutaFoto = "fotos/a.jpg",
            lineas = listOf(Linea("A", 2, 1.0), Linea("B", 1, 2.0)),
        )

        val datos = datosRevisionDe(ticket)

        assertEquals("LIDL", datos.comercio)
        assertEquals(ticket.fechaEmision, datos.fechaEmision)
        assertEquals(10.0, datos.total, 0.0)
        assertEquals(Categoria.HOGAR, datos.categoria)
        assertEquals(listOf(0L, 1L), datos.lineas.map { it.id })
        assertEquals(listOf("A", "B"), datos.lineas.map { it.concepto })
        assertNotNull(datos)
    }

    @Test
    fun datosRevisionDeTicketSinLineasTieneListaVacia() {
        val ticket = Ticket(
            comercio = "X",
            fechaEmision = LocalDateTime.of(2026, 10, 1, 8, 5),
            total = 0.0,
            categoria = Categoria.OTROS,
            fechaCaptura = LocalDateTime.of(2026, 10, 1, 9, 0),
            motor = Motor.NUBE,
            rutaFoto = "",
        )
        assertTrue(datosRevisionDe(ticket).lineas.isEmpty())
    }
}

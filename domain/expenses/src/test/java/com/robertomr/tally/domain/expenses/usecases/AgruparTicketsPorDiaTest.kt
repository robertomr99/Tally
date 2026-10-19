package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.Ticket
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Agrupación por día del listado (spec listado-gastos, T1). */
class AgruparTicketsPorDiaTest {

    private val agrupar = AgruparTicketsPorDia()

    private fun ticket(id: Long, emision: LocalDateTime, total: Double) = Ticket(
        id = id,
        comercio = "T$id",
        fechaEmision = emision,
        total = total,
        categoria = Categoria.OTROS,
        fechaCaptura = emision,
        motor = Motor.LOCAL,
        rutaFoto = "x.jpg",
    )

    @Test
    fun listaVaciaDaCeroGrupos() {
        assertTrue(agrupar(emptyList()).isEmpty())
    }

    @Test
    fun agrupaPorDiaEnOrdenDescendenteYSumaElTotalDelDia() {
        val grupos = agrupar(
            listOf(
                ticket(1, LocalDateTime.of(2026, 10, 23, 20, 45), 5.0),
                ticket(2, LocalDateTime.of(2026, 10, 24, 11, 15), 10.5),
                ticket(3, LocalDateTime.of(2026, 10, 24, 14, 32), 4.5),
            ),
        )
        assertEquals(listOf(LocalDate.of(2026, 10, 24), LocalDate.of(2026, 10, 23)), grupos.map { it.fecha })
        assertEquals(listOf(3L, 2L), grupos[0].tickets.map { it.id })
        assertEquals(1500L, grupos[0].totalCentimos)
        assertEquals(500L, grupos[1].totalCentimos)
    }

    @Test
    fun empateDeFechaSeOrdenaPorIdDescendente() {
        val hora = LocalDateTime.of(2026, 10, 24, 10, 0)
        val grupos = agrupar(listOf(ticket(4, hora, 1.0), ticket(9, hora, 1.0), ticket(7, hora, 1.0)))
        assertEquals(listOf(9L, 7L, 4L), grupos.single().tickets.map { it.id })
    }

    @Test
    fun medianocheSeparaLosDias() {
        val grupos = agrupar(
            listOf(
                ticket(1, LocalDateTime.of(2026, 10, 24, 23, 59), 1.0),
                ticket(2, LocalDateTime.of(2026, 10, 25, 0, 0), 2.0),
            ),
        )
        assertEquals(listOf(LocalDate.of(2026, 10, 25), LocalDate.of(2026, 10, 24)), grupos.map { it.fecha })
        assertEquals(listOf(200L, 100L), grupos.map { it.totalCentimos })
    }

    @Test
    fun totalCeroNegativoONoFinitoApareceperoNoSuma() {
        val dia = LocalDateTime.of(2026, 10, 24, 10, 0)
        val grupo = agrupar(
            listOf(
                ticket(1, dia, 0.0),
                ticket(2, dia.plusMinutes(1), -5.0),
                ticket(3, dia.plusMinutes(2), Double.NaN),
                ticket(4, dia.plusMinutes(3), 3.0),
            ),
        ).single()
        assertEquals(4, grupo.tickets.size)
        assertEquals(300L, grupo.totalCentimos)
    }

    @Test
    fun sumaDeCentimosNoArrastraErrorBinario() {
        val dia = LocalDateTime.of(2026, 10, 24, 10, 0)
        val grupo = agrupar(listOf(ticket(1, dia, 0.10), ticket(2, dia, 0.20))).single()
        assertEquals(30L, grupo.totalCentimos)
    }

    @Test
    fun redondeaHalfUpComoLaDistribucion() {
        val dia = LocalDateTime.of(2026, 10, 24, 10, 0)
        assertEquals(101L, agrupar(listOf(ticket(1, dia, 1.005))).single().totalCentimos)
    }
}

package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.FiltroFormaPago
import com.robertomr.tally.domain.expenses.entities.FormaPago
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.Ticket
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests JVM de [FiltrarTicketsPorFormaPago] (spec filtro-forma-pago). */
class FiltrarTicketsPorFormaPagoTest {

    private val filtrar = FiltrarTicketsPorFormaPago()
    private val fecha = LocalDateTime.of(2026, 10, 15, 12, 0)

    private fun ticket(id: Long, forma: FormaPago) = Ticket(
        id = id,
        comercio = "T$id",
        fechaEmision = fecha,
        total = 10.0,
        categoria = Categoria.OTROS,
        fechaCaptura = fecha,
        motor = Motor.LOCAL,
        rutaFoto = "x.jpg",
        formaPago = forma,
    )

    private val mezcla = listOf(
        ticket(1, FormaPago.TARJETA),
        ticket(2, FormaPago.NO_INDICADA),
        ticket(3, FormaPago.EFECTIVO),
        ticket(4, FormaPago.TARJETA),
        ticket(5, FormaPago.EFECTIVO),
    )

    @Test
    fun listaVaciaDevuelveVacia() {
        FiltroFormaPago.entries.forEach { assertTrue(filtrar(emptyList(), it).isEmpty()) }
    }

    @Test
    fun todosEsIdentidad() {
        assertSame(mezcla, filtrar(mezcla, FiltroFormaPago.TODOS))
    }

    @Test
    fun soloEfectivoConservaElOrden() {
        assertEquals(listOf(3L, 5L), filtrar(mezcla, FiltroFormaPago.EFECTIVO).map { it.id })
    }

    @Test
    fun soloTarjetaConservaElOrden() {
        assertEquals(listOf(1L, 4L), filtrar(mezcla, FiltroFormaPago.TARJETA).map { it.id })
    }

    @Test
    fun noIndicadaQuedaExcluidaConEfectivoYTarjeta() {
        val soloNoIndicada = listOf(ticket(9, FormaPago.NO_INDICADA))
        assertTrue(filtrar(soloNoIndicada, FiltroFormaPago.EFECTIVO).isEmpty())
        assertTrue(filtrar(soloNoIndicada, FiltroFormaPago.TARJETA).isEmpty())
        assertEquals(1, filtrar(soloNoIndicada, FiltroFormaPago.TODOS).size)
    }
}

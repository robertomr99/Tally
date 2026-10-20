package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.data.TicketRepository
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.DatosRevision
import com.robertomr.tally.domain.expenses.entities.FormaPago
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.Ticket
import com.robertomr.tally.domain.expenses.entities.TicketExtraido
import com.robertomr.tally.domain.expenses.entities.datosRevisionDe
import com.robertomr.tally.test.unit.FakePhotoFileStore
import com.robertomr.tally.test.unit.FakeTicketDataSource
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Tests JVM de la persistencia de la forma de pago (spec forma-pago, GuardarTicket/ActualizarTicket). */
class PagoUsecasesTest {

    private val repository = TicketRepository(FakeTicketDataSource(), FakePhotoFileStore())
    private val guardar = GuardarTicket(repository)
    private val actualizar = ActualizarTicket(repository)
    private val extraido = TicketExtraido(comercio = "X", total = 13.90, motor = Motor.NUBE)

    private fun datos(forma: FormaPago, entregado: Double?, cambio: Double?) = DatosRevision(
        comercio = "X",
        fechaEmision = LocalDateTime.of(2026, 10, 15, 13, 45),
        total = 13.90,
        categoria = Categoria.ALIMENTACION,
        lineas = emptyList(),
        formaPago = forma,
        entregado = entregado,
        cambio = cambio,
    )

    @Test
    fun guardarEfectivoPersisteEntregadoYCambioSinTocarElTotal() = runTest {
        val id = guardar(extraido, "f.jpg", "t", datos(FormaPago.EFECTIVO, 50.0, 36.10))
        val t = repository.getById(id)!!
        assertEquals(FormaPago.EFECTIVO, t.formaPago)
        assertEquals(50.0, t.entregado!!, 0.0)
        assertEquals(36.10, t.cambio!!, 0.0)
        assertEquals(13.90, t.total, 0.0)
    }

    @Test
    fun guardarTarjetaLimpiaEntregadoYCambioOcultos() = runTest {
        val id = guardar(extraido, "f.jpg", "t", datos(FormaPago.TARJETA, 50.0, 36.10))
        val t = repository.getById(id)!!
        assertEquals(FormaPago.TARJETA, t.formaPago)
        assertNull(t.entregado)
        assertNull(t.cambio)
    }

    @Test
    fun guardarSinFormaNiImportesEsNoIndicada() = runTest {
        val id = guardar(extraido, "f.jpg", "t", datos(FormaPago.NO_INDICADA, null, null))
        assertEquals(FormaPago.NO_INDICADA, repository.getById(id)!!.formaPago)
    }

    @Test
    fun actualizarCambiaLaFormaYLimpiaAlPasarATarjeta() = runTest {
        val id = guardar(extraido, "f.jpg", "t", datos(FormaPago.EFECTIVO, 50.0, 36.10))
        val original = repository.getById(id)!!
        val aTarjeta = datosRevisionDe(original).copy(formaPago = FormaPago.TARJETA)
        actualizar(original, aTarjeta)
        val t = repository.getById(id)!!
        assertEquals(FormaPago.TARJETA, t.formaPago)
        assertNull(t.entregado)
        assertNull(t.cambio)
        assertEquals(13.90, t.total, 0.0)
    }

    @Test
    fun ticketAntiguoSigueNoIndicadaAlActualizar() = runTest {
        val id = repository.save(
            Ticket(
                comercio = "VIEJO",
                fechaEmision = LocalDateTime.of(2026, 9, 1, 10, 0),
                total = 4.0,
                categoria = Categoria.OTROS,
                fechaCaptura = LocalDateTime.of(2026, 9, 1, 10, 5),
                motor = Motor.NUBE,
                rutaFoto = "v.jpg",
            ),
        )
        val original = repository.getById(id)!!
        actualizar(original, datosRevisionDe(original))
        assertEquals(FormaPago.NO_INDICADA, repository.getById(id)!!.formaPago)
    }
}

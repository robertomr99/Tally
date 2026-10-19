package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.data.TicketRepository
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.DatosRevision
import com.robertomr.tally.domain.expenses.entities.Linea
import com.robertomr.tally.domain.expenses.entities.LineaEditable
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.Ticket
import com.robertomr.tally.test.unit.FakePhotoFileStore
import com.robertomr.tally.test.unit.FakeTicketDataSource
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests JVM de [ActualizarTicket], [BorrarTicket] y [ObtenerTicket] (spec
 * detalle-ticket, T2 y T3) con los dobles de `:test:unit`.
 */
class DetalleTicketUsecasesTest {

    private val dataSource = FakeTicketDataSource()
    private val photoFileStore = FakePhotoFileStore()
    private val repository = TicketRepository(dataSource, photoFileStore)
    private val actualizar = ActualizarTicket(repository)
    private val borrar = BorrarTicket(repository)
    private val obtener = ObtenerTicket(repository)

    private val horaOriginal = LocalDateTime.of(2026, 10, 15, 13, 45)
    private val captura = LocalDateTime.of(2026, 10, 15, 14, 0)

    private suspend fun sembrar(): Ticket {
        val id = repository.save(
            Ticket(
                comercio = "MERCADONA",
                fechaEmision = horaOriginal,
                total = 10.0,
                categoria = Categoria.ALIMENTACION,
                fechaCaptura = captura,
                motor = Motor.NUBE,
                rutaFoto = "fotos/x.jpg",
                nota = "nota",
                textoCrudoOcr = "crudo",
                lineas = listOf(Linea("LECHE", 2, 1.0), Linea("PAN", 1, 3.0)),
            ),
        )
        return repository.getById(id)!!
    }

    @Test
    fun actualizarPersisteElMismoIdConservandoCamposNoEditables() = runTest {
        val original = sembrar()
        val datos = DatosRevision(
            comercio = "CARREFOUR",
            fechaEmision = LocalDateTime.of(2026, 11, 2, 13, 45),
            total = 99.0,
            categoria = Categoria.HOGAR,
            lineas = listOf(
                LineaEditable(0, "  HUEVOS ", 3, 2.0),
                LineaEditable(5, "   ", 1, 1.0),
            ),
        )

        val id = actualizar(original, datos)

        assertEquals(original.id, id)
        val guardado = repository.getById(id)!!
        assertEquals("CARREFOUR", guardado.comercio)
        assertEquals(LocalDateTime.of(2026, 11, 2, 13, 45), guardado.fechaEmision)
        assertEquals(99.0, guardado.total, 0.0)
        assertEquals(Categoria.HOGAR, guardado.categoria)
        assertEquals(captura, guardado.fechaCaptura)
        assertEquals(Motor.NUBE, guardado.motor)
        assertEquals("fotos/x.jpg", guardado.rutaFoto)
        assertEquals("crudo", guardado.textoCrudoOcr)
        assertEquals("nota", guardado.nota)
        assertEquals(listOf(Linea("HUEVOS", 3, 2.0)), guardado.lineas)
    }

    @Test
    fun actualizarNoDerivaElTotalDeLasLineas() = runTest {
        val original = sembrar()
        val datos = DatosRevision(
            comercio = original.comercio,
            fechaEmision = original.fechaEmision,
            total = 10.0,
            categoria = original.categoria,
            lineas = listOf(LineaEditable(0, "CARO", 1, 500.0)),
        )

        actualizar(original, datos)

        assertEquals(10.0, repository.getById(original.id)!!.total, 0.0)
    }

    @Test
    fun borrarEliminaElTicketYSuFoto() = runTest {
        val original = sembrar()

        borrar(original.id)

        assertNull(obtener(original.id))
        assertEquals(listOf("fotos/x.jpg"), photoFileStore.deletedPaths)
    }

    @Test
    fun obtenerDevuelveNullSiNoExiste() = runTest {
        assertNull(obtener(404L))
    }
}

package com.robertomr.tally.domain.expenses.data

import app.cash.turbine.test
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.Linea
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.Ticket
import com.robertomr.tally.test.unit.FakePhotoFileStore
import com.robertomr.tally.test.unit.FakeTicketDataSource
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests unitarios de [TicketRepository] con los dobles compartidos de
 * `:test:unit` (spec persistencia-room, escenarios 1-3 a nivel de repositorio:
 * delegación de save, borrado con foto best-effort, no-op y consulta por rango
 * vía Flow). El borrado en cascada de líneas se prueba en el DAO (Room
 * in-memory).
 */
class TicketRepositoryTest {

    private val dataSource = FakeTicketDataSource()
    private val photoFileStore = FakePhotoFileStore()
    private val repository = TicketRepository(dataSource, photoFileStore)

    @Test
    fun saveAssignsIdAndReturnsAggregateFromGetById() = runTest {
        val id = repository.save(
            ticket(lineas = listOf(linea("Pan"), linea("Leche")))
        )

        val guardado = repository.getById(id)
        assertEquals("Mercadona", guardado?.comercio)
        assertEquals(2, guardado?.lineas?.size)
    }

    @Test
    fun saveWithExistingIdUpdatesTicketInPlace() = runTest {
        val id = repository.save(ticket(comercio = "Mercadona"))

        val idDevuelto = repository.save(
            ticket(id = id, comercio = "Carrefour", lineas = listOf(linea("Huevos")))
        )

        assertEquals(id, idDevuelto)
        assertEquals("Carrefour", repository.getById(id)?.comercio)
        assertEquals(1, repository.getById(id)?.lineas?.size)
    }

    @Test
    fun deleteRemovesTicketAndDeletesPhotoFile() = runTest {
        val id = repository.save(ticket(rutaFoto = "/data/fotos/ticket.jpg"))

        repository.delete(id)

        assertNull(repository.getById(id))
        assertEquals(listOf("/data/fotos/ticket.jpg"), photoFileStore.deletedPaths)
    }

    @Test
    fun deleteWithUnknownIdIsNoOpAndDoesNotTouchPhotoStore() = runTest {
        repository.delete(999L)

        assertNull(repository.getById(999L))
        assertTrue(photoFileStore.deletedPaths.isEmpty())
    }

    @Test
    fun deleteRemovesTicketEvenIfPhotoDeletionFails() = runTest {
        photoFileStore.deleteResult = false
        val id = repository.save(ticket(rutaFoto = "/data/fotos/ticket.jpg"))

        repository.delete(id)

        assertNull(repository.getById(id))
        assertEquals(listOf("/data/fotos/ticket.jpg"), photoFileStore.deletedPaths)
    }

    @Test
    fun observeByDateRangeEmitsInRangeTicketsIncludingBoundariesAndUpdatesOnSave() = runTest {
        val from = LocalDate.of(2026, 10, 1)
        val to = LocalDate.of(2026, 10, 31)

        repository.observeByDateRange(from, to).test {
            assertTrue(awaitItem().isEmpty())

            val idInicio = repository.save(
                ticket(fechaEmision = LocalDateTime.of(2026, 10, 1, 0, 0))
            )
            assertEquals(listOf(idInicio), awaitItem().map { it.id })

            val idFinal = repository.save(
                ticket(fechaEmision = LocalDateTime.of(2026, 10, 31, 23, 59))
            )
            assertEquals(setOf(idInicio, idFinal), awaitItem().map { it.id }.toSet())

            // Fuera del rango: no deben emitir nada nuevo.
            repository.save(ticket(fechaEmision = LocalDateTime.of(2026, 9, 30, 23, 59)))
            repository.save(ticket(fechaEmision = LocalDateTime.of(2026, 11, 1, 0, 0)))
            expectNoEvents()
        }
    }

    private fun ticket(
        id: Long = 0L,
        comercio: String = "Mercadona",
        fechaEmision: LocalDateTime = LocalDateTime.of(2026, 10, 15, 12, 0),
        rutaFoto: String = "/data/fotos/ticket.jpg",
        lineas: List<Linea> = emptyList(),
    ): Ticket = Ticket(
        id = id,
        comercio = comercio,
        fechaEmision = fechaEmision,
        total = 10.0,
        categoria = Categoria.ALIMENTACION,
        fechaCaptura = LocalDateTime.of(2026, 10, 15, 13, 0),
        motor = Motor.LOCAL,
        rutaFoto = rutaFoto,
        lineas = lineas,
    )

    private fun linea(concepto: String): Linea =
        Linea(concepto = concepto, cantidad = 1, precioUnitario = 1.5)
}

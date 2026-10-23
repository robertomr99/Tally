package com.robertomr.tally.domain.expenses.usecases

import app.cash.turbine.test
import com.robertomr.tally.domain.expenses.data.TicketRepository
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.Periodo
import com.robertomr.tally.domain.expenses.entities.PeriodoTipo
import com.robertomr.tally.domain.expenses.entities.Ticket
import com.robertomr.tally.test.unit.FakePhotoFileStore
import com.robertomr.tally.test.unit.FakeTicketDataSource
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Reactividad del listado (spec listado-gastos). */
class ObservarListadoDelPeriodoTest {

    private val repository = TicketRepository(FakeTicketDataSource(), FakePhotoFileStore())
    private val observar = ObservarListadoDelPeriodo(ObservarTicketsDelPeriodo(repository), AgruparTicketsPorDia())
    private val semana = Periodo.de(PeriodoTipo.SEMANA, LocalDate.of(2026, 10, 14)) // 12..18 oct

    private fun ticket(emision: LocalDateTime, total: Double = 10.0) = Ticket(
        comercio = "T",
        fechaEmision = emision,
        total = total,
        categoria = Categoria.OTROS,
        fechaCaptura = emision,
        motor = Motor.LOCAL,
        rutaFoto = "x.jpg",
    )

    @Test
    fun guardarAnadeAlGrupoYBorrarLoQuita() = runTest {
        val dia = LocalDateTime.of(2026, 10, 15, 10, 0)
        repository.save(ticket(dia))
        observar(semana).test {
            assertEquals(1, awaitItem().single().tickets.size)
            val id = repository.save(ticket(dia.plusHours(2), 5.0))
            val conDos = awaitItem().single()
            assertEquals(2, conDos.tickets.size)
            assertEquals(1500L, conDos.totalCentimos)
            repository.delete(id)
            assertEquals(1, awaitItem().single().tickets.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun incluyeLosBordesDelPeriodoYExcluyeLoDeFuera() = runTest {
        repository.save(ticket(LocalDateTime.of(2026, 10, 12, 0, 0)))
        repository.save(ticket(LocalDateTime.of(2026, 10, 18, 23, 59)))
        repository.save(ticket(LocalDateTime.of(2026, 10, 11, 23, 59)))
        repository.save(ticket(LocalDateTime.of(2026, 10, 19, 0, 0)))
        observar(semana).test {
            val grupos = awaitItem()
            assertEquals(listOf(LocalDate.of(2026, 10, 18), LocalDate.of(2026, 10, 12)), grupos.map { it.fecha })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun periodoSinTicketsDaListaVacia() = runTest {
        observar(semana).test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }
}

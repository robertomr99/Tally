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
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Reactividad del resumen y uso del Clock (spec resumen-total). */
class ObservarResumenDelPeriodoTest {

    private val madrid = ZoneId.of("Europe/Madrid")
    // 25-oct-2026 es el día del cambio de hora en Madrid.
    private val clock = Clock.fixed(LocalDateTime.of(2026, 10, 25, 12, 0).atZone(madrid).toInstant(), madrid)
    private val repository = TicketRepository(FakeTicketDataSource(), FakePhotoFileStore())
    private val observar = ObservarResumenDelPeriodo(ObservarTicketsDelPeriodo(repository), CalcularResumenPeriodo(), clock)
    private val octubre = Periodo.de(PeriodoTipo.MES, clock.instant().atZone(madrid).toLocalDate())

    private fun ticket(emision: LocalDateTime, total: Double) = Ticket(
        comercio = "T",
        fechaEmision = emision,
        total = total,
        categoria = Categoria.OTROS,
        fechaCaptura = emision,
        motor = Motor.LOCAL,
        rutaFoto = "x.jpg",
    )

    @Test
    fun ticketNuevoDelPeriodoActualizaTotalYPromedioConDiasDelClock() = runTest {
        observar(octubre).test {
            assertEquals(0.0, awaitItem().total, 1e-9)
            repository.save(ticket(LocalDateTime.of(2026, 10, 10, 9, 0), 250.0))
            val r = awaitItem()
            assertEquals(250.0, r.total, 1e-9)
            assertEquals(1, r.numTickets)
            assertEquals(10.0, r.promedioDiario, 1e-9) // 250 / 25 días transcurridos
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun ticketDelPeriodoAnteriorSoloActualizaLaComparativa() = runTest {
        repository.save(ticket(LocalDateTime.of(2026, 10, 10, 9, 0), 60.0))
        observar(octubre).test {
            val inicial = awaitItem()
            assertNull(inicial.variacion)
            repository.save(ticket(LocalDateTime.of(2026, 9, 10, 9, 0), 40.0))
            val r = awaitItem()
            assertEquals(60.0, r.total, 1e-9)
            assertEquals(1, r.numTickets)
            assertEquals(40.0, r.totalAnterior, 1e-9)
            assertEquals(0.5, r.variacion!!, 1e-9)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

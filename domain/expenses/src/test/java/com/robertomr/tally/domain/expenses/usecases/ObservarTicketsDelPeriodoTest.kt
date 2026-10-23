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
import org.junit.Test

/** Filtro por fecha de emisión inclusivo y reactivo (spec periodo-selector). */
class ObservarTicketsDelPeriodoTest {

    private val repository = TicketRepository(FakeTicketDataSource(), FakePhotoFileStore())
    private val observar = ObservarTicketsDelPeriodo(repository)
    private val semana = Periodo.de(PeriodoTipo.SEMANA, LocalDate.of(2026, 10, 14)) // 12..18 oct

    private fun ticket(
        comercio: String,
        emision: LocalDateTime,
        captura: LocalDateTime = emision,
    ) = Ticket(
        comercio = comercio,
        fechaEmision = emision,
        total = 10.0,
        categoria = Categoria.OTROS,
        fechaCaptura = captura,
        motor = Motor.LOCAL,
        rutaFoto = "x.jpg",
    )

    @Test
    fun losBordesDelRangoSonInclusivosYElDiaSiguienteQuedaFuera() = runTest {
        repository.save(ticket("inicio", LocalDateTime.of(2026, 10, 12, 0, 0)))
        repository.save(ticket("fin", LocalDateTime.of(2026, 10, 18, 23, 59)))
        repository.save(ticket("antes", LocalDateTime.of(2026, 10, 11, 23, 59)))
        repository.save(ticket("despues", LocalDateTime.of(2026, 10, 19, 0, 0)))

        observar(semana).test {
            assertEquals(setOf("inicio", "fin"), awaitItem().map { it.comercio }.toSet())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun filtraPorFechaDeEmisionNoPorFechaDeCaptura() = runTest {
        // Emitido en el rango pero capturado meses después: entra.
        repository.save(
            ticket("emitidoDentro", LocalDateTime.of(2026, 10, 13, 12, 0), LocalDateTime.of(2026, 12, 1, 9, 0)),
        )
        // Capturado dentro del rango pero emitido fuera: no entra.
        repository.save(
            ticket("capturadoDentro", LocalDateTime.of(2026, 8, 1, 12, 0), LocalDateTime.of(2026, 10, 14, 9, 0)),
        )

        observar(semana).test {
            assertEquals(listOf("emitidoDentro"), awaitItem().map { it.comercio })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emiteDeNuevoAlGuardarUnTicketDelPeriodo() = runTest {
        observar(semana).test {
            assertEquals(emptyList<Ticket>(), awaitItem())
            repository.save(ticket("nuevo", LocalDateTime.of(2026, 10, 15, 10, 0)))
            assertEquals(listOf("nuevo"), awaitItem().map { it.comercio })
            cancelAndIgnoreRemainingEvents()
        }
    }
}

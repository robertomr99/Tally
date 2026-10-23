package com.robertomr.tally.domain.expenses.usecases

import app.cash.turbine.test
import com.robertomr.tally.domain.expenses.data.TicketRepository
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.DistribucionCategorias
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

/** Reactividad de la distribución (spec grafica-donut). */
class ObservarDistribucionDelPeriodoTest {

    private val repository = TicketRepository(FakeTicketDataSource(), FakePhotoFileStore())
    private val observar = ObservarDistribucionDelPeriodo(
        ObservarTicketsDelPeriodo(repository),
        CalcularDistribucionCategorias(),
    )
    private val semana = Periodo.de(PeriodoTipo.SEMANA, LocalDate.of(2026, 10, 14)) // 12..18 oct

    private fun ticket(categoria: Categoria, total: Double, emision: LocalDateTime) = Ticket(
        comercio = "T",
        fechaEmision = emision,
        total = total,
        categoria = categoria,
        fechaCaptura = emision,
        motor = Motor.LOCAL,
        rutaFoto = "x.jpg",
    )

    @Test
    fun guardarYBorrarUnTicketAnadeYQuitaLaPorcion() = runTest {
        val dentro = LocalDateTime.of(2026, 10, 15, 10, 0)
        repository.save(ticket(Categoria.HOGAR, 20.0, dentro))
        observar(semana).test {
            assertEquals(listOf(Categoria.HOGAR), awaitItem().porciones.map { it.categoria })
            val id = repository.save(ticket(Categoria.OCIO, 10.0, dentro))
            val conOcio = awaitItem()
            assertEquals(listOf(Categoria.HOGAR, Categoria.OCIO), conOcio.porciones.map { it.categoria })
            assertEquals(3000L, conOcio.totalCentimos)
            repository.delete(id)
            assertEquals(listOf(Categoria.HOGAR), awaitItem().porciones.map { it.categoria })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun unTicketFueraDelPeriodoNoCuentaYSinTicketsEsVacia() = runTest {
        repository.save(ticket(Categoria.SALUD, 50.0, LocalDateTime.of(2026, 9, 1, 10, 0)))
        observar(semana).test {
            assertEquals(DistribucionCategorias.VACIA, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}

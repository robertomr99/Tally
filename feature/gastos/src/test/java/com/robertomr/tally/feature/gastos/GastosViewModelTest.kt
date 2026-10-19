package com.robertomr.tally.feature.gastos

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.robertomr.tally.domain.expenses.data.TicketRepository
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.PeriodoTipo
import com.robertomr.tally.domain.expenses.entities.Ticket
import com.robertomr.tally.domain.expenses.usecases.AgruparTicketsPorDia
import com.robertomr.tally.domain.expenses.usecases.ObservarListadoDelPeriodo
import com.robertomr.tally.domain.expenses.usecases.ObservarTicketsDelPeriodo
import com.robertomr.tally.domain.expenses.usecases.ObtenerPeriodo
import com.robertomr.tally.test.unit.CoroutineTestRule
import com.robertomr.tally.test.unit.FakePhotoFileStore
import com.robertomr.tally.test.unit.FakeTicketDataSource
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Tests JVM de [GastosViewModel] (spec listado-gastos, T5). Hoy = jueves
 * 2026-10-15; semana 12..18 oct; mes 1..31 oct.
 */
class GastosViewModelTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private val madrid = ZoneId.of("Europe/Madrid")
    private val hoy = LocalDate.of(2026, 10, 15)
    private val clock = Clock.fixed(hoy.atTime(12, 0).atZone(madrid).toInstant(), madrid)
    private val repository = TicketRepository(FakeTicketDataSource(), FakePhotoFileStore())

    private fun viewModel() = GastosViewModel(
        obtenerPeriodo = ObtenerPeriodo(clock),
        observarListadoDelPeriodo = ObservarListadoDelPeriodo(
            ObservarTicketsDelPeriodo(repository),
            AgruparTicketsPorDia(),
        ),
    )

    private fun ticket(emision: LocalDateTime, total: Double) = Ticket(
        comercio = "T",
        fechaEmision = emision,
        total = total,
        categoria = Categoria.OTROS,
        fechaCaptura = emision,
        motor = Motor.LOCAL,
        rutaFoto = "x.jpg",
    )

    private suspend fun ReceiveTurbine<GastosUiState>.esperar(tipo: PeriodoTipo): GastosUiState {
        var s = awaitItem()
        while (s.cargando || s.tipo != tipo) s = awaitItem()
        return s
    }

    @Test
    fun arrancaEnMesCargandoYTerminaDeCargar() = runTest {
        val vm = viewModel()
        assertTrue(vm.uiState.value.cargando)
        assertNull(vm.uiState.value.grupos)
        vm.uiState.test {
            val s = esperar(PeriodoTipo.MES)
            assertEquals(LocalDate.of(2026, 10, 1), s.periodo?.desde)
            assertEquals(hoy, s.hoy)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun alternarTipoCambiaElContenidoYAgrupaPorDia() = runTest {
        repository.save(ticket(hoy.atTime(9, 0), 10.0))
        repository.save(ticket(hoy.atTime(18, 0), 5.0))
        repository.save(ticket(hoy.minusDays(3).atTime(9, 0), 20.0)) // lunes 12
        repository.save(ticket(hoy.minusDays(14).atTime(9, 0), 40.0)) // 1 oct
        repository.save(ticket(hoy.minusDays(40).atTime(9, 0), 80.0)) // septiembre
        val vm = viewModel()
        vm.uiState.test {
            var s = esperar(PeriodoTipo.MES)
            assertEquals(3, s.grupos?.size)
            assertEquals(listOf(hoy, hoy.minusDays(3), hoy.minusDays(14)), s.grupos?.map { it.fecha })

            vm.onSeleccionarPeriodo(PeriodoTipo.DIA)
            s = esperar(PeriodoTipo.DIA)
            assertEquals(1, s.grupos?.size)
            assertEquals(2, s.grupos?.single()?.tickets?.size)
            assertEquals(1500L, s.grupos?.single()?.totalCentimos)

            vm.onSeleccionarPeriodo(PeriodoTipo.SEMANA)
            s = esperar(PeriodoTipo.SEMANA)
            assertEquals(2, s.grupos?.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun sinTicketsLosGruposSonVaciosNoNulos() = runTest {
        viewModel().uiState.test {
            val s = esperar(PeriodoTipo.MES)
            assertNotNull(s.grupos)
            assertTrue(s.grupos!!.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun guardarUnTicketDelPeriodoActualizaLaListaSola() = runTest {
        viewModel().uiState.test {
            var s = esperar(PeriodoTipo.MES)
            assertTrue(s.grupos!!.isEmpty())
            repository.save(ticket(hoy.atTime(10, 0), 12.5))
            while (s.grupos.isNullOrEmpty()) s = awaitItem()
            assertEquals(1, s.grupos?.single()?.tickets?.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

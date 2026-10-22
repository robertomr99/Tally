package com.robertomr.tally.feature.gastos

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.robertomr.tally.domain.expenses.data.TicketRepository
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.FiltroFormaPago
import com.robertomr.tally.domain.expenses.entities.FormaPago
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.PeriodoTipo
import com.robertomr.tally.domain.expenses.entities.Ticket
import com.robertomr.tally.domain.expenses.usecases.AgruparTicketsPorDia
import com.robertomr.tally.domain.expenses.usecases.FiltrarTicketsPorFormaPago
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
import org.junit.Assert.assertFalse
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
        observarTicketsDelPeriodo = ObservarTicketsDelPeriodo(repository),
        filtrarPorFormaPago = FiltrarTicketsPorFormaPago(),
        agruparPorDia = AgruparTicketsPorDia(),
    )

    private fun ticket(emision: LocalDateTime, total: Double, forma: FormaPago = FormaPago.NO_INDICADA) = Ticket(
        comercio = "T",
        fechaEmision = emision,
        total = total,
        categoria = Categoria.OTROS,
        fechaCaptura = emision,
        motor = Motor.LOCAL,
        rutaFoto = "x.jpg",
        formaPago = forma,
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

    // --- Rango personalizado (spec rango-personalizado, T7) ---

    private suspend fun sembrar() {
        repository.save(ticket(hoy.atTime(9, 0), 10.0))
        repository.save(ticket(hoy.minusDays(3).atTime(9, 0), 20.0)) // 12 oct
        repository.save(ticket(hoy.minusDays(14).atTime(23, 59), 40.0)) // 1 oct
        repository.save(ticket(hoy.minusDays(40).atTime(9, 0), 80.0)) // septiembre
    }

    @Test
    fun elRangoFiltraElListadoConLimitesInclusivos() = runTest {
        sembrar()
        val vm = viewModel()
        vm.uiState.test {
            esperar(PeriodoTipo.MES)
            vm.onSeleccionarRango(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 12))
            val s = esperar(PeriodoTipo.PERSONALIZADO)
            assertEquals(listOf(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 1)), s.grupos?.map { it.fecha })
            assertEquals(s.periodo, s.rango)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun rangoDeUnDiaEsValidoEInvertidoSeIgnora() = runTest {
        sembrar()
        val vm = viewModel()
        vm.uiState.test {
            esperar(PeriodoTipo.MES)
            vm.onSeleccionarRango(hoy, hoy)
            val s = esperar(PeriodoTipo.PERSONALIZADO)
            assertEquals(1, s.grupos?.size)
            vm.onSeleccionarRango(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 1))
            expectNoEvents()
            assertEquals(s, vm.uiState.value)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun personalizadoSinRangoSeIgnoraYConPrevioSeReaplica() = runTest {
        sembrar()
        val vm = viewModel()
        vm.uiState.test {
            esperar(PeriodoTipo.MES)
            vm.onSeleccionarPeriodo(PeriodoTipo.PERSONALIZADO)
            expectNoEvents()
            vm.onSeleccionarRango(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 12))
            esperar(PeriodoTipo.PERSONALIZADO)
            vm.onSeleccionarPeriodo(PeriodoTipo.DIA)
            assertEquals(LocalDate.of(2026, 10, 1), esperar(PeriodoTipo.DIA).rango?.desde)
            vm.onSeleccionarPeriodo(PeriodoTipo.PERSONALIZADO)
            assertEquals(2, esperar(PeriodoTipo.PERSONALIZADO).grupos?.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- Filtro de forma de pago (spec filtro-forma-pago) ---

    private suspend fun ReceiveTurbine<GastosUiState>.esperar(tipo: PeriodoTipo, filtro: FiltroFormaPago): GastosUiState {
        var s = awaitItem()
        while (s.cargando || s.tipo != tipo || s.filtro != filtro) s = awaitItem()
        return s
    }

    private fun GastosUiState.ids() = grupos.orEmpty().flatMap { g -> g.tickets.map { it.total } }

    /** Hoy: efectivo 10 + tarjeta 5 + sin indicar 1; 12 oct: tarjeta 20; 1 oct: efectivo 40. */
    private suspend fun sembrarMixto() {
        repository.save(ticket(hoy.atTime(9, 0), 10.0, FormaPago.EFECTIVO))
        repository.save(ticket(hoy.atTime(10, 0), 5.0, FormaPago.TARJETA))
        repository.save(ticket(hoy.atTime(11, 0), 1.0, FormaPago.NO_INDICADA))
        repository.save(ticket(hoy.minusDays(3).atTime(9, 0), 20.0, FormaPago.TARJETA))
        repository.save(ticket(hoy.minusDays(14).atTime(9, 0), 40.0, FormaPago.EFECTIVO))
    }

    @Test
    fun porDefectoElFiltroEsTodosYMuestraNoIndicada() = runTest {
        sembrarMixto()
        viewModel().uiState.test {
            val s = esperar(PeriodoTipo.MES)
            assertEquals(FiltroFormaPago.TODOS, s.filtro)
            assertEquals(5, s.grupos.orEmpty().sumOf { it.tickets.size })
            assertTrue(s.hayTicketsEnPeriodo)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun tarjetaYEfectivoExcluyenNoIndicadaYRecalculanTotalesPorDia() = runTest {
        sembrarMixto()
        val vm = viewModel()
        vm.uiState.test {
            esperar(PeriodoTipo.MES)
            vm.onSeleccionarFiltro(FiltroFormaPago.TARJETA)
            var s = esperar(PeriodoTipo.MES, FiltroFormaPago.TARJETA)
            assertEquals(listOf(hoy, hoy.minusDays(3)), s.grupos?.map { it.fecha })
            assertEquals(listOf(500L, 2000L), s.grupos?.map { it.totalCentimos })

            vm.onSeleccionarFiltro(FiltroFormaPago.EFECTIVO)
            s = esperar(PeriodoTipo.MES, FiltroFormaPago.EFECTIVO)
            assertEquals(listOf(hoy, hoy.minusDays(14)), s.grupos?.map { it.fecha })
            assertEquals(listOf(1000L, 4000L), s.grupos?.map { it.totalCentimos })

            vm.onSeleccionarFiltro(FiltroFormaPago.TODOS)
            s = esperar(PeriodoTipo.MES, FiltroFormaPago.TODOS)
            assertEquals(5, s.grupos.orEmpty().sumOf { it.tickets.size })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun elFiltroSeCombinaConDiaSemanaMesYRangoYSeConservaAlCambiarDePeriodo() = runTest {
        sembrarMixto()
        val vm = viewModel()
        vm.uiState.test {
            esperar(PeriodoTipo.MES)
            vm.onSeleccionarFiltro(FiltroFormaPago.TARJETA)
            assertEquals(2, esperar(PeriodoTipo.MES, FiltroFormaPago.TARJETA).grupos?.size)

            vm.onSeleccionarPeriodo(PeriodoTipo.DIA)
            var s = esperar(PeriodoTipo.DIA, FiltroFormaPago.TARJETA)
            assertEquals(listOf(5.0), s.ids())

            vm.onSeleccionarPeriodo(PeriodoTipo.SEMANA)
            s = esperar(PeriodoTipo.SEMANA, FiltroFormaPago.TARJETA)
            assertEquals(listOf(5.0, 20.0), s.ids())

            vm.onSeleccionarRango(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 12))
            s = esperar(PeriodoTipo.PERSONALIZADO, FiltroFormaPago.TARJETA)
            assertEquals(listOf(20.0), s.ids())

            vm.onSeleccionarFiltro(FiltroFormaPago.TODOS)
            s = esperar(PeriodoTipo.PERSONALIZADO, FiltroFormaPago.TODOS)
            assertEquals(listOf(20.0, 40.0), s.ids())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun hayTicketsEnPeriodoDistingueVacioPorFiltroDeVacioReal() = runTest {
        repository.save(ticket(hoy.atTime(9, 0), 10.0, FormaPago.EFECTIVO))
        val vm = viewModel()
        vm.uiState.test {
            esperar(PeriodoTipo.MES)
            vm.onSeleccionarFiltro(FiltroFormaPago.TARJETA)
            var s = esperar(PeriodoTipo.MES, FiltroFormaPago.TARJETA)
            assertTrue(s.grupos!!.isEmpty())
            assertTrue(s.hayTicketsEnPeriodo)

            // Un periodo sin ningun ticket sigue siendo vacio real aunque haya filtro.
            vm.onSeleccionarRango(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))
            s = esperar(PeriodoTipo.PERSONALIZADO, FiltroFormaPago.TARJETA)
            assertTrue(s.grupos!!.isEmpty())
            assertFalse(s.hayTicketsEnPeriodo)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun elFiltroEsReactivoAAltasYBorrados() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            esperar(PeriodoTipo.MES)
            vm.onSeleccionarFiltro(FiltroFormaPago.TARJETA)
            var s = esperar(PeriodoTipo.MES, FiltroFormaPago.TARJETA)
            assertTrue(s.grupos!!.isEmpty())

            repository.save(ticket(hoy.atTime(9, 0), 10.0, FormaPago.EFECTIVO))
            repository.save(ticket(hoy.atTime(10, 0), 7.0, FormaPago.TARJETA))
            while (s.ids() != listOf(7.0)) s = awaitItem()
            assertTrue(s.hayTicketsEnPeriodo)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

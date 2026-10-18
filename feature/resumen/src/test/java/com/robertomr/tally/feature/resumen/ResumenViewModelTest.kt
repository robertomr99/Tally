package com.robertomr.tally.feature.resumen

import app.cash.turbine.test
import com.robertomr.tally.domain.expenses.data.TicketRepository
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.DistribucionCategorias
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.PeriodoTipo
import com.robertomr.tally.domain.expenses.entities.Ticket
import com.robertomr.tally.domain.expenses.usecases.CalcularDistribucionCategorias
import com.robertomr.tally.domain.expenses.usecases.CalcularResumenPeriodo
import com.robertomr.tally.domain.expenses.usecases.ObservarDistribucionDelPeriodo
import com.robertomr.tally.domain.expenses.usecases.ObservarResumenDelPeriodo
import com.robertomr.tally.domain.expenses.usecases.ObservarTicketsDelPeriodo
import com.robertomr.tally.domain.expenses.usecases.ObtenerPeriodo
import com.robertomr.tally.test.unit.CoroutineTestRule
import com.robertomr.tally.test.unit.FakePhotoFileStore
import com.robertomr.tally.test.unit.FakeTicketDataSource
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

/**
 * Tests JVM de [ResumenViewModel] (spec periodo-selector, T5): Mes por defecto,
 * alternar cambia periodo y cifra, filtro por emisión y reactividad al guardar.
 * Hoy = jueves 2026-10-15; semana 12..18 oct; mes 1..31 oct.
 */
class ResumenViewModelTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private val madrid = ZoneId.of("Europe/Madrid")
    private val hoy = LocalDate.of(2026, 10, 15)
    private val clock = Clock.fixed(hoy.atTime(12, 0).atZone(madrid).toInstant(), madrid)
    private val repository = TicketRepository(FakeTicketDataSource(), FakePhotoFileStore())

    private fun viewModel() = ResumenViewModel(
        obtenerPeriodo = ObtenerPeriodo(clock),
        observarResumenDelPeriodo = ObservarResumenDelPeriodo(
            ObservarTicketsDelPeriodo(repository),
            CalcularResumenPeriodo(),
            clock,
        ),
        observarDistribucionDelPeriodo = ObservarDistribucionDelPeriodo(
            ObservarTicketsDelPeriodo(repository),
            CalcularDistribucionCategorias(),
        ),
    )

    private fun ticket(
        emision: LocalDateTime,
        total: Double,
        captura: LocalDateTime = emision,
        categoria: Categoria = Categoria.OTROS,
    ) = Ticket(
        comercio = "T",
        fechaEmision = emision,
        total = total,
        categoria = categoria,
        fechaCaptura = captura,
        motor = Motor.LOCAL,
        rutaFoto = "x.jpg",
    )

    private suspend fun sembrar() {
        repository.save(ticket(hoy.atTime(9, 0), 10.0)) // hoy
        repository.save(ticket(hoy.minusDays(3).atTime(9, 0), 20.0)) // lunes 12: misma semana
        repository.save(ticket(hoy.minusDays(14).atTime(9, 0), 40.0)) // 1 oct: mismo mes
        repository.save(ticket(hoy.minusDays(40).atTime(9, 0), 80.0)) // septiembre
    }

    @Test
    fun arrancaEnMesYMuestraElRangoDelMes() = runTest {
        sembrar()
        viewModel().uiState.test {
            var actual = awaitItem()
            if (actual.cargando) actual = awaitItem()
            assertFalse(actual.cargando)
            assertEquals(PeriodoTipo.MES, actual.tipo)
            assertEquals(LocalDate.of(2026, 10, 1), actual.periodo?.desde)
            assertEquals(LocalDate.of(2026, 10, 31), actual.periodo?.hasta)
            assertEquals(3, actual.resumen?.numTickets)
            assertEquals(70.0, actual.resumen?.total ?: -1.0, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun alternarDiaSemanaMesCambiaPeriodoRecuentoYSuma() = runTest {
        sembrar()
        val vm = viewModel()
        vm.uiState.test {
            awaitItem()
            vm.onSeleccionarPeriodo(PeriodoTipo.DIA)
            var s = esperar(PeriodoTipo.DIA)
            assertEquals(1, s.resumen?.numTickets)
            assertEquals(10.0, s.resumen?.total ?: -1.0, 0.001)

            vm.onSeleccionarPeriodo(PeriodoTipo.SEMANA)
            s = esperar(PeriodoTipo.SEMANA)
            assertEquals(LocalDate.of(2026, 10, 12), s.periodo?.desde)
            assertEquals(LocalDate.of(2026, 10, 18), s.periodo?.hasta)
            assertEquals(2, s.resumen?.numTickets)
            assertEquals(30.0, s.resumen?.total ?: -1.0, 0.001)

            vm.onSeleccionarPeriodo(PeriodoTipo.MES)
            s = esperar(PeriodoTipo.MES)
            assertEquals(3, s.resumen?.numTickets)
            assertEquals(70.0, s.resumen?.total ?: -1.0, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun filtraPorFechaDeEmisionNoDeCaptura() = runTest {
        // Emitido hoy pero capturado en otro mes: entra en Día.
        repository.save(ticket(hoy.atTime(8, 0), 5.0, captura = LocalDateTime.of(2026, 12, 1, 9, 0)))
        // Capturado hoy pero emitido hace 40 días: no entra en Día.
        repository.save(ticket(hoy.minusDays(40).atTime(8, 0), 99.0, captura = hoy.atTime(9, 0)))
        val vm = viewModel()
        vm.onSeleccionarPeriodo(PeriodoTipo.DIA)
        vm.uiState.test {
            val s = esperarSiguiente(PeriodoTipo.DIA)
            assertEquals(1, s.resumen?.numTickets)
            assertEquals(5.0, s.resumen?.total ?: -1.0, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun laCifraSeActualizaSolaAlGuardarUnTicketDelPeriodo() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            var s = awaitItem()
            if (s.cargando) s = awaitItem()
            assertEquals(0, s.resumen?.numTickets)
            repository.save(ticket(hoy.atTime(10, 0), 12.5))
            // combine emite estados intermedios (resumen y distribucion llegan por separado).
            while (s.resumen?.numTickets != 1 || s.distribucion?.porciones?.size != 1) s = awaitItem()
            assertEquals(1, s.resumen?.numTickets)
            assertEquals(12.5, s.resumen?.total ?: -1.0, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun laComparativaYElPromedioSeCalculanContraElPeriodoAnterior() = runTest {
        sembrar()
        val vm = viewModel()
        vm.uiState.test {
            // Mes: 70 vs septiembre 80 (hoy = 15 oct, 15 dias transcurridos).
            var s = esperar(PeriodoTipo.MES)
            assertEquals(-0.125, s.resumen?.variacion ?: 0.0, 1e-9)
            assertEquals(70.0 / 15, s.resumen?.promedioDiario ?: -1.0, 1e-9)

            // Dia: hoy 10 vs ayer sin tickets -> sin referencia.
            vm.onSeleccionarPeriodo(PeriodoTipo.DIA)
            s = esperar(PeriodoTipo.DIA)
            assertEquals(null, s.resumen?.variacion)
            assertEquals(10.0, s.resumen?.promedioDiario ?: -1.0, 1e-9)

            // Semana (12-18 oct): 30 vs semana previa sin tickets -> sin referencia.
            vm.onSeleccionarPeriodo(PeriodoTipo.SEMANA)
            s = esperar(PeriodoTipo.SEMANA)
            assertEquals(null, s.resumen?.variacion)
            assertEquals(30.0 / 4, s.resumen?.promedioDiario ?: -1.0, 1e-9)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun elEstadoInicialDejaDeCargarse() = runTest {
        assertFalse(viewModel().uiState.first { !it.cargando }.cargando)
    }

    @Test
    fun laDistribucionCambiaAlAlternarTipoYEsVaciaSinTickets() = runTest {
        repository.save(ticket(hoy.atTime(9, 0), 10.0, categoria = Categoria.HOGAR))
        repository.save(ticket(hoy.minusDays(14).atTime(9, 0), 30.0, categoria = Categoria.OCIO))
        val vm = viewModel()
        vm.uiState.test {
            var s = esperar(PeriodoTipo.MES)
            assertEquals(listOf(Categoria.OCIO, Categoria.HOGAR), s.distribucion?.porciones?.map { it.categoria })
            assertEquals(4000L, s.distribucion?.totalCentimos)

            vm.onSeleccionarPeriodo(PeriodoTipo.DIA)
            s = esperar(PeriodoTipo.DIA)
            assertEquals(listOf(Categoria.HOGAR), s.distribucion?.porciones?.map { it.categoria })
            assertEquals(100, s.distribucion?.porciones?.single()?.porcentaje)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun sinTicketsLaDistribucionEsVaciaYNoNulaTrasCargar() = runTest {
        val s = viewModel().uiState.first { !it.cargando }
        assertEquals(DistribucionCategorias.VACIA, s.distribucion)
    }

    @Test
    fun laDistribucionSeActualizaSolaAlGuardarUnTicket() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            var s = esperar(PeriodoTipo.MES)
            assertEquals(DistribucionCategorias.VACIA, s.distribucion)
            repository.save(ticket(hoy.atTime(10, 0), 12.5, categoria = Categoria.SALUD))
            while (s.distribucion?.porciones?.map { it.categoria } != listOf(Categoria.SALUD)) s = awaitItem()
            assertEquals(listOf(Categoria.SALUD), s.distribucion?.porciones?.map { it.categoria })
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<ResumenUiState>.esperar(tipo: PeriodoTipo): ResumenUiState =
        esperarSiguiente(tipo)

    private suspend fun app.cash.turbine.ReceiveTurbine<ResumenUiState>.esperarSiguiente(tipo: PeriodoTipo): ResumenUiState {
        var s = awaitItem()
        while (s.cargando || s.tipo != tipo) s = awaitItem()
        return s
    }
}

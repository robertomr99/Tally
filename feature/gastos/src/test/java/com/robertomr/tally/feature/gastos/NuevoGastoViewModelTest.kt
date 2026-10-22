package com.robertomr.tally.feature.gastos

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.robertomr.tally.domain.expenses.data.TicketRepository
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.ErrorGastoManual
import com.robertomr.tally.domain.expenses.entities.FormaPago
import com.robertomr.tally.domain.expenses.entities.Linea
import com.robertomr.tally.domain.expenses.usecases.GuardarGastoManual
import com.robertomr.tally.test.unit.CoroutineTestRule
import com.robertomr.tally.test.unit.FakePhotoFileStore
import com.robertomr.tally.test.unit.FakeTicketDataSource
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Tests JVM de [NuevoGastoViewModel] (spec gasto-manual) con los dobles de
 * `:test:unit`: estado inicial, ediciones, requisito, descartar, guardado,
 * doble toque, fallo con reintento.
 */
class NuevoGastoViewModelTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private val dataSource = FakeTicketDataSource()
    private val repository = TicketRepository(dataSource, FakePhotoFileStore())

    private fun viewModel() = NuevoGastoViewModel(GuardarGastoManual(repository))

    private suspend fun ReceiveTurbine<NuevoGastoUiState>.esperar(
        condicion: (NuevoGastoUiState) -> Boolean,
    ): NuevoGastoUiState {
        while (true) {
            val s = awaitItem()
            if (condicion(s)) return s
        }
    }

    @Test
    fun estadoInicialEsHoyOtrosTotalCeroConRequisitoDeTotal() = runTest {
        viewModel().uiState.test {
            val s = awaitItem()
            assertEquals("", s.datos.comercio)
            assertEquals(LocalDate.now().atStartOfDay(), s.datos.fechaEmision)
            assertEquals(0.0, s.datos.total, 0.0)
            assertEquals(Categoria.OTROS, s.datos.categoria)
            assertEquals(ErrorGastoManual.TOTAL_NO_POSITIVO, s.error)
            assertFalse(s.hayCambios)
            assertFalse(s.guardando)
            assertFalse(s.hecho)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun totalPositivoQuitaElRequisitoYTotalCeroLoVuelvePonerPeroComercioNoBloquea() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            awaitItem()
            vm.onTotalEditado("12,50")
            val ok = esperar { it.error == null }
            assertEquals(12.5, ok.datos.total, 0.0)
            assertTrue(ok.hayCambios)
            assertEquals("", ok.datos.comercio)

            vm.onTotalEditado("0")
            assertEquals(ErrorGastoManual.TOTAL_NO_POSITIVO, esperar { it.error != null }.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun fechaFuturaDaRequisitoYCorregirlaLoQuita() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            awaitItem()
            vm.onTotalEditado("5")
            esperar { it.error == null }

            val manana = LocalDate.now().plusDays(1)
            vm.onFechaEditada("${manana.dayOfMonth}/${manana.monthValue}/${manana.year}")
            assertEquals(ErrorGastoManual.FECHA_FUTURA, esperar { it.error != null }.error)

            val ayer = LocalDate.now().minusDays(1)
            vm.onFechaEditada("${ayer.dayOfMonth}/${ayer.monthValue}/${ayer.year}")
            val ok = esperar { it.error == null }
            // La hora es 00:00 del dia elegido (no se conserva ninguna).
            assertEquals(ayer.atStartOfDay(), ok.datos.fechaEmision)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun textoInvalidoDeFechaOTotalSeIgnora() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            awaitItem()
            vm.onFechaEditada("basura")
            vm.onTotalEditado("xx")
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun descartarRestableceElFormularioInicialSinEscribir() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            awaitItem()
            vm.onComercioEditado("KIOSCO")
            vm.onTotalEditado("9")
            vm.onCategoriaElegida(Categoria.HOGAR)
            esperar { it.hayCambios && it.datos.categoria == Categoria.HOGAR && it.datos.total == 9.0 }

            vm.onDescartar()
            val s = esperar { !it.hayCambios }
            assertEquals("", s.datos.comercio)
            assertEquals(0.0, s.datos.total, 0.0)
            assertEquals(Categoria.OTROS, s.datos.categoria)
            assertNull(repository.getById(1L))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun guardarSinTotalValidoNoHaceNada() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            awaitItem()
            vm.onGuardar()
            expectNoEvents()
            assertNull(repository.getById(1L))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun guardarPersisteUnManualYMarcaHechoUnaSolaVezAunConDobleToque() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            awaitItem()
            vm.onComercioEditado("KIOSCO")
            vm.onTotalEditado("12,5")
            vm.onFormaPagoElegida(FormaPago.TARJETA)
            vm.onLineaAnadida()
            vm.onLineaConceptoEditado(0, "CAFE")
            esperar { it.error == null && it.datos.lineas.firstOrNull()?.concepto == "CAFE" }

            vm.onGuardar()
            vm.onGuardar()
            val hecho = esperar { it.hecho }
            assertTrue(hecho.hecho)

            val t = repository.getById(1L)!!
            assertEquals("KIOSCO", t.comercio)
            assertEquals(12.5, t.total, 0.0)
            assertEquals(FormaPago.TARJETA, t.formaPago)
            assertNull(t.motor)
            assertEquals("", t.rutaFoto)
            assertEquals(listOf(Linea("CAFE", 1, 0.0)), t.lineas)
            assertEquals(LocalDateTime.now().toLocalDate(), t.fechaEmision.toLocalDate())
            assertNull("el doble toque no crea otro", repository.getById(2L))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun guardarFallidoMuestraErrorConservaDatosYPermiteReintentar() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            awaitItem()
            vm.onTotalEditado("7")
            esperar { it.error == null }

            dataSource.saveError = IOException("bd")
            vm.onGuardar()
            val fallo = esperar { it.mensajeError != null }
            assertEquals(NuevoGastoViewModel.MENSAJE_ERROR_GUARDADO, fallo.mensajeError)
            assertEquals(7.0, fallo.datos.total, 0.0)
            assertFalse(fallo.hecho)
            assertFalse(fallo.guardando)

            dataSource.saveError = null
            vm.onGuardar()
            val hecho = esperar { it.hecho }
            assertNull(hecho.mensajeError)
            assertEquals(7.0, repository.getById(1L)!!.total, 0.0)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

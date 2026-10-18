package com.robertomr.tally.feature.ajustes

import app.cash.turbine.test
import com.robertomr.tally.domain.expenses.entities.EstadoModelo
import com.robertomr.tally.domain.expenses.entities.ModeloLocal
import com.robertomr.tally.domain.expenses.entities.MotivoFallo
import com.robertomr.tally.domain.expenses.usecases.CancelarDescargaModelo
import com.robertomr.tally.domain.expenses.usecases.ConfigurarTokenModelo
import com.robertomr.tally.domain.expenses.usecases.DescargarModelo
import com.robertomr.tally.domain.expenses.usecases.EliminarModelo
import com.robertomr.tally.domain.expenses.usecases.ObservarEstadoModelo
import com.robertomr.tally.domain.expenses.usecases.ObservarTokenModelo
import com.robertomr.tally.test.unit.CoroutineTestRule
import com.robertomr.tally.test.unit.FakeDownloadConditions
import com.robertomr.tally.test.unit.FakeModelAccessTokenStore
import com.robertomr.tally.test.unit.FakeModelDownloader
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Tests JVM de [AjustesViewModel] con Turbine, CoroutineTestRule y los dobles de
 * :test:unit (spec descarga-modelo, T6): estados del modelo, avisos de WiFi y
 * espacio, cancelar, eliminar y token.
 */
class AjustesViewModelTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private val downloader = FakeModelDownloader()
    private val conditions = FakeDownloadConditions()
    private val tokenStore = FakeModelAccessTokenStore()

    private fun viewModel() = AjustesViewModel(
        observarEstadoModelo = ObservarEstadoModelo(downloader),
        observarTokenModelo = ObservarTokenModelo(tokenStore),
        descargarModelo = DescargarModelo(downloader, conditions),
        cancelarDescargaModelo = CancelarDescargaModelo(downloader),
        eliminarModelo = EliminarModelo(downloader),
        configurarTokenModelo = ConfigurarTokenModelo(tokenStore),
    )

    @Test
    fun empiezaCargandoYPasaAlEstadoRealDelModelo() = runTest {
        viewModel().uiState.test {
            var actual = awaitItem()
            if (actual.cargando) actual = awaitItem()
            assertFalse(actual.cargando)
            assertEquals(EstadoModelo.NoDescargado, actual.estado)
            assertFalse(actual.hayToken)
            assertNull(actual.aviso)
        }
    }

    @Test
    fun reflejaLosCambiosDeEstadoDelDownloader() = runTest {
        viewModel().uiState.test {
            skipCargando()
            downloader.estadoFlow.value = EstadoModelo.Descargando(1_000, 4_000)
            assertEquals(EstadoModelo.Descargando(1_000, 4_000), awaitItem().estado)
            downloader.estadoFlow.value = EstadoModelo.Descargado(ModeloLocal.TAMANO_ESPERADO_BYTES)
            assertEquals(
                EstadoModelo.Descargado(ModeloLocal.TAMANO_ESPERADO_BYTES),
                awaitItem().estado,
            )
        }
    }

    @Test
    fun conRedMovilMuestraAvisoSoloWifiYNoInicia() = runTest {
        conditions.redSinCoste = false
        val vm = viewModel()
        vm.uiState.test {
            skipCargando()
            vm.onDescargar()
            val ui = awaitItem()
            assertEquals(AvisoDescarga.SoloWifi, ui.aviso)
            assertEquals(EstadoModelo.NoDescargado, ui.estado)
        }
        assertEquals(0, downloader.iniciarCount)
    }

    @Test
    fun sinEspacioMuestraAvisoConNecesariosYLibres() = runTest {
        conditions.espacioLibre = 1_000L
        val vm = viewModel()
        vm.uiState.test {
            skipCargando()
            vm.onDescargar()
            val aviso = awaitItem().aviso as AvisoDescarga.EspacioInsuficiente
            assertEquals(1_000L, aviso.libres)
            assertEquals(
                ModeloLocal.TAMANO_ESPERADO_BYTES + ModeloLocal.MARGEN_ESPACIO_BYTES,
                aviso.necesarios,
            )
        }
        assertEquals(0, downloader.iniciarCount)
    }

    @Test
    fun conWifiYEspacioInicia_yElAvisoPreviosSeLimpia() = runTest {
        conditions.redSinCoste = false
        val vm = viewModel()
        vm.uiState.test {
            skipCargando()
            vm.onDescargar()
            assertEquals(AvisoDescarga.SoloWifi, awaitItem().aviso)

            conditions.redSinCoste = true
            vm.onDescargar()
            assertNull(awaitItem().aviso)
        }
        assertEquals(1, downloader.iniciarCount)
    }

    @Test
    fun elAvisoDesapareceCuandoLaDescargaEstaEnMarcha() = runTest {
        conditions.redSinCoste = false
        val vm = viewModel()
        vm.uiState.test {
            skipCargando()
            vm.onDescargar()
            assertEquals(AvisoDescarga.SoloWifi, awaitItem().aviso)

            downloader.estadoFlow.value = EstadoModelo.Descargando(0, 10)
            val ui = awaitItem()
            assertNull(ui.aviso)
            assertTrue(ui.estado is EstadoModelo.Descargando)
        }
    }

    @Test
    fun onAvisoVistoLimpiaElAviso() = runTest {
        conditions.redSinCoste = false
        val vm = viewModel()
        vm.uiState.test {
            skipCargando()
            vm.onDescargar()
            assertEquals(AvisoDescarga.SoloWifi, awaitItem().aviso)
            vm.onAvisoVisto()
            assertNull(awaitItem().aviso)
        }
    }

    @Test
    fun cancelarYEliminarDelegan() = runTest {
        val vm = viewModel()
        vm.onCancelar()
        vm.onEliminar()
        assertEquals(1, downloader.cancelarCount)
        assertEquals(1, downloader.eliminarCount)
    }

    @Test
    fun elFalloSeExponeConSuMotivo() = runTest {
        downloader.estadoFlow.value = EstadoModelo.Fallo(MotivoFallo.Autorizacion, 0)
        viewModel().uiState.test {
            val primero = awaitItem()
            assertEquals(
                EstadoModelo.Fallo(MotivoFallo.Autorizacion, 0),
                (if (primero.cargando) awaitItem() else primero).estado,
            )
        }
    }

    @Test
    fun guardarYBorrarTokenActualizanHayToken() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            skipCargando()
            vm.onGuardarToken("  hf_abc ")
            assertTrue(awaitItem().hayToken)
            assertEquals("hf_abc", tokenStore.token)
            vm.onBorrarToken()
            assertFalse(awaitItem().hayToken)
        }
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<AjustesUiState>.skipCargando() {
        val primero = awaitItem()
        if (primero.cargando) awaitItem()
    }
}

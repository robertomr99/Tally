package com.robertomr.tally.feature.ajustes

import app.cash.turbine.test
import com.robertomr.tally.domain.expenses.entities.EstadoModelo
import com.robertomr.tally.domain.expenses.entities.ModeloLocal
import com.robertomr.tally.domain.expenses.entities.MotivoFallo
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.OrigenExtraccion
import com.robertomr.tally.domain.expenses.entities.PreferenciaMotor
import com.robertomr.tally.domain.expenses.entities.RegistroExtraccion
import com.robertomr.tally.domain.expenses.entities.ResultadoExtraccion
import com.robertomr.tally.domain.expenses.usecases.BorrarRegistroExtracciones
import com.robertomr.tally.domain.expenses.usecases.ExportarRegistroExtracciones
import com.robertomr.tally.domain.expenses.usecases.ObservarRegistroExtracciones
import com.robertomr.tally.test.unit.FakeRegistroExtracciones
import com.robertomr.tally.domain.expenses.usecases.ConfigurarPreferenciaMotor
import com.robertomr.tally.domain.expenses.usecases.ObservarPreferenciaMotor
import com.robertomr.tally.test.unit.FakePreferenciaMotorStore
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
 * :test:unit (spec descarga-modelo): estados del modelo, avisos de WiFi y
 * espacio, cancelar, eliminar y token.
 */
class AjustesViewModelTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private val downloader = FakeModelDownloader()
    private val conditions = FakeDownloadConditions()
    private val tokenStore = FakeModelAccessTokenStore()
    private val motorStore = FakePreferenciaMotorStore()
    private val registroLog = FakeRegistroExtracciones()

    private fun filaLog(motor: Motor, resultado: ResultadoExtraccion, duracionMs: Long) =
        RegistroExtraccion(
            marcaTiempo = 0L,
            motor = motor,
            origen = OrigenExtraccion.ROUTER,
            resultado = resultado,
            duracionMs = duracionMs,
            longitudTexto = 8,
        )

    private fun viewModel() = AjustesViewModel(
        observarEstadoModelo = ObservarEstadoModelo(downloader),
        observarTokenModelo = ObservarTokenModelo(tokenStore),
        observarPreferenciaMotor = ObservarPreferenciaMotor(motorStore),
        observarRegistroExtracciones = ObservarRegistroExtracciones(registroLog),
        borrarRegistroExtracciones = BorrarRegistroExtracciones(registroLog),
        exportarRegistroExtracciones = ExportarRegistroExtracciones(registroLog),
        configurarPreferenciaMotor = ConfigurarPreferenciaMotor(motorStore),
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

    @Test
    fun laPreferenciaPorDefectoEsAutoSinAvisoLocal() = runTest {
        viewModel().uiState.test {
            var ui = awaitItem()
            if (ui.cargando) ui = awaitItem()
            assertEquals(PreferenciaMotor.AUTO, ui.preferencia)
            assertFalse(ui.avisoLocal)
        }
    }

    @Test
    fun elegirMotorGuardaLaPreferenciaYLaExpone() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            skipCargando()
            vm.onElegirMotor(PreferenciaMotor.NUBE)
            assertEquals(PreferenciaMotor.NUBE, awaitItem().preferencia)
        }
        assertEquals(PreferenciaMotor.NUBE, motorStore.actual())
    }

    @Test
    fun avisoLocalSoloConLocalYModeloNoDescargado() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            skipCargando()
            vm.onElegirMotor(PreferenciaMotor.LOCAL)
            val sinModelo = awaitItem()
            assertEquals(PreferenciaMotor.LOCAL, sinModelo.preferencia)
            assertTrue(sinModelo.avisoLocal)

            downloader.estadoFlow.value = EstadoModelo.Descargado(ModeloLocal.TAMANO_ESPERADO_BYTES)
            assertFalse(awaitItem().avisoLocal)

            vm.onElegirMotor(PreferenciaMotor.NUBE)
            assertFalse(awaitItem().avisoLocal)
        }
    }

    @Test
    fun elDiagnosticoReflejaElLogYSeActualizaAlRegistrar() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            var inicial = awaitItem()
            if (inicial.cargando) inicial = awaitItem()
            assertTrue(inicial.diagnostico.recientes.isEmpty())

            registroLog.registrar(filaLog(Motor.NUBE, ResultadoExtraccion.EXITO, 2_000))
            val conUna = awaitItem().diagnostico
            assertEquals(1, conUna.recientes.size)
            assertEquals(1, conUna.resumen.single().exitos)
            assertEquals(2_000L, conUna.resumen.single().duracionMediaExitoMs)
        }
    }

    @Test
    fun borrarRegistroVaciaElDiagnostico() = runTest {
        registroLog.registrar(filaLog(Motor.LOCAL, ResultadoExtraccion.FALLO, 5))
        val vm = viewModel()
        vm.uiState.test {
            var actual = awaitItem()
            while (actual.cargando || actual.diagnostico.recientes.isEmpty()) actual = awaitItem()
            vm.onBorrarRegistro()
            assertTrue(awaitItem().diagnostico.recientes.isEmpty())
        }
    }

    @Test
    fun copiarCsvEntregaLaCabeceraYLasFilasDelLog() = runTest {
        registroLog.registrar(filaLog(Motor.NUBE, ResultadoExtraccion.EXITO, 321))
        var csv = ""

        viewModel().onCopiarCsv { csv = it }

        val lineas = csv.trimEnd().lines()
        assertEquals(2, lineas.size)
        assertTrue(lineas[0].startsWith("id,instante_iso,motor"))
        assertTrue(lineas[1].contains(",NUBE,ROUTER,EXITO,"))
        assertTrue(lineas[1].endsWith(",321,8"))
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<AjustesUiState>.skipCargando() {
        val primero = awaitItem()
        if (primero.cargando) awaitItem()
    }
}

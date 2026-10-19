package com.robertomr.tally.ajustes

import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.robertomr.tally.MainActivity
import com.robertomr.tally.di.TextEngineEntryPoint
import com.robertomr.tally.domain.expenses.data.ExtractorPorMotor
import com.robertomr.tally.domain.expenses.data.PreferenciaMotorStore
import com.robertomr.tally.domain.expenses.data.RegistroExtracciones
import com.robertomr.tally.domain.expenses.data.TextEngine
import com.robertomr.tally.domain.expenses.entities.DisponibilidadMotor
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.MotorNoDisponibleException
import com.robertomr.tally.domain.expenses.entities.OrigenExtraccion
import com.robertomr.tally.domain.expenses.entities.PreferenciaMotor
import com.robertomr.tally.domain.expenses.entities.RegistroExtraccion
import com.robertomr.tally.domain.expenses.entities.ResultadoExtraccion
import com.robertomr.tally.domain.expenses.entities.TipoFallo
import com.robertomr.tally.domain.expenses.usecases.registrosACsv
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// E2E de la feature log-extracciones (spec, escenarios 1-4, 9) sobre el GRAFO
// REAL de Hilt (con el decorador EngineConRegistro cableado en :framework:ai):
// con la preferencia en LOCAL y sin modelo descargado, una extraccion por el
// router (ROUTER) y otra explicita (REINTENTO) fallan con MotorNoDisponible sin
// invocar ningun motor ni red (determinista). Se comprueba la fila registrada
// (solo metadatos) y que la seccion de diagnostico debug de Ajustes la muestra.
//
// Exito/cancelacion no se ejercen aqui (requieren Gemini o un motor retenible):
// viven en EngineConRegistroTest (JVM) con dobles y reloj fake.
//
// Linea base y limpieza: el log del emulador es persistente; @Before guarda las
// filas y la preferencia de motor, y @After restaura ambas (vacia y reinserta la
// linea base), asi el test no deja basura ni cambia la configuracion.
@RunWith(AndroidJUnit4::class)
class LogExtraccionesFlujoTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val punto: TextEngineEntryPoint by lazy {
        EntryPointAccessors.fromApplication(
            InstrumentationRegistry.getInstrumentation().targetContext,
            TextEngineEntryPoint::class.java,
        )
    }
    private val textEngine: TextEngine get() = punto.textEngine()
    private val extractor: ExtractorPorMotor get() = punto.extractorPorMotor()
    private val registro: RegistroExtracciones get() = punto.registroExtracciones()
    private val preferencias: PreferenciaMotorStore get() = punto.preferenciaMotorStore()

    private lateinit var lineaBase: List<RegistroExtraccion>
    private lateinit var preferenciaInicial: PreferenciaMotor

    @Before
    fun guardarLineaBase() {
        // Si el dispositivo ya tiene modelo local, Local SI funcionaria: no es este escenario.
        assumeTrue(
            "Local disponible en este dispositivo; el escenario de fallo determinista no aplica",
            extractor.disponibilidad(Motor.LOCAL) != DisponibilidadMotor.DISPONIBLE,
        )
        lineaBase = runBlocking { registro.observarRecientes(RegistroExtracciones.MAXIMO_FILAS).first() }
        preferenciaInicial = preferencias.actual()
    }

    @After
    fun restaurar() {
        if (!::lineaBase.isInitialized) return // el assumeTrue salto antes de la linea base
        preferencias.guardar(preferenciaInicial)
        runBlocking {
            registro.borrarTodo()
            lineaBase.asReversed().forEach { registro.registrar(it.copy(id = 0L)) }
        }
    }

    @Test
    fun routerYReintentoFallidosQuedanRegistradosSoloConMetadatosYVisiblesEnAjustes() {
        preferencias.guardar(PreferenciaMotor.LOCAL)
        val textoSecreto = "TEXTO-SECRETO-DEL-TICKET-E2E"

        runBlocking {
            try {
                textEngine.extract(textoSecreto)
                fail("Local forzado sin modelo debia lanzar MotorNoDisponibleException")
            } catch (e: MotorNoDisponibleException) {
                assertEquals(Motor.LOCAL, e.motor)
            }
            try {
                extractor.extract(Motor.LOCAL, "abc")
                fail("El reintento a Local sin modelo debia lanzar MotorNoDisponibleException")
            } catch (_: MotorNoDisponibleException) {
            }
        }

        val nuevas = runBlocking { registro.observarRecientes(RegistroExtracciones.MAXIMO_FILAS).first() }
            .take(2)
        val (reintento, router) = nuevas
        assertEquals(OrigenExtraccion.ROUTER, router.origen)
        assertEquals(Motor.LOCAL, router.motor)
        assertEquals(ResultadoExtraccion.FALLO, router.resultado)
        assertEquals(TipoFallo.MOTOR_NO_DISPONIBLE, router.tipoFallo)
        assertEquals(textoSecreto.length, router.longitudTexto)
        assertEquals(OrigenExtraccion.REINTENTO, reintento.origen)
        assertEquals(Motor.LOCAL, reintento.motor)
        assertEquals(ResultadoExtraccion.FALLO, reintento.resultado)
        assertEquals(3, reintento.longitudTexto)

        // Privacidad: ni el texto ni el mensaje de la excepcion llegan al CSV.
        val csv = registrosACsv(nuevas)
        assertFalse(csv.contains(textoSecreto))
        assertFalse(csv.contains("no esta disponible"))

        abrirAjustes()
        composeRule.onNodeWithTag("diagnostico_extracciones").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("diagnostico_fila_0").performScrollTo()
            .assertTextContains("REI", substring = true)
            .assertTextContains("FALLO", substring = true)
            .assertTextContains("LOCAL", substring = true)
        composeRule.onNodeWithTag("diagnostico_fila_1").performScrollTo()
            .assertTextContains("ROU", substring = true)
            .assertTextContains("MotorNoDisponibleException", substring = true)
        composeRule.onNodeWithTag("diagnostico_resumen_LOCAL").performScrollTo()
            .assertTextContains("ok=0", substring = true)

        composeRule.onNodeWithTag("diagnostico_copiar_csv").performScrollTo().performClick()
        composeRule.waitUntil(10_000) {
            runCatching { composeRule.onNodeWithTag("diagnostico_csv_copiado").assertIsDisplayed() }.isSuccess
        }
        capturarEvidenciaSiSePide(csv)
    }

    /**
     * Opcional: con `-e evidencia <nombre>` deja una captura en
     * `/data/local/tmp/<nombre>.png` y el CSV (solo metadatos) en logcat con el
     * tag `LogExtracciones`, para docs/evidence/log-extracciones/.
     */
    private fun capturarEvidenciaSiSePide(csv: String) {
        val nombre = InstrumentationRegistry.getArguments().getString("evidencia") ?: return
        Log.i("LogExtracciones", "CSV:\n$csv")
        composeRule.waitForIdle()
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("screencap -p /data/local/tmp/$nombre.png")
            .let { ParcelFileDescriptor.AutoCloseInputStream(it).use { s -> s.readBytes() } }
    }

    @Test
    fun borrarRegistroVaciaLaSeccion() {
        preferencias.guardar(PreferenciaMotor.LOCAL)
        runBlocking {
            try {
                textEngine.extract("abc")
            } catch (_: MotorNoDisponibleException) {
            }
        }
        assertTrue(runBlocking { registro.observarRecientes(1).first() }.isNotEmpty())

        abrirAjustes()
        composeRule.onNodeWithTag("diagnostico_borrar").performScrollTo().performClick()
        composeRule.waitUntil(10_000) {
            runCatching { composeRule.onNodeWithTag("diagnostico_vacio").assertIsDisplayed() }.isSuccess
        }
        assertTrue(runBlocking { registro.observarRecientes(1).first() }.isEmpty())
    }

    private fun abrirAjustes() {
        composeRule.onNodeWithTag("boton_ajustes").performClick()
        composeRule.onNodeWithTag("pantalla_ajustes").assertIsDisplayed()
        composeRule.waitUntil(10_000) {
            runCatching { composeRule.onNodeWithTag("seccion_motor").assertIsDisplayed() }.isSuccess
        }
    }
}

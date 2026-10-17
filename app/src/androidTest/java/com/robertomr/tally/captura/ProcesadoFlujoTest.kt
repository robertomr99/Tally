package com.robertomr.tally.captura

import android.content.pm.PackageManager
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.robertomr.tally.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// E2E enfocado de la feature procesado-ui (spec, T8): pantalla 05 ->
// "Continuar" -> procesado (mockup 06) con badge del motor REAL y pasos ->
// resultado de solo lectura con "Revisar ticket" habilitado (spec
// revision-datos), y back del sistema que vuelve a la pantalla 05 con el
// Borrador intacto.
//
// Determinismo (R-PROC3): NO se usa @TestInstallIn para fakes de `TextEngine`
// porque romperia `ExtraccionNubeRealTest` (resuelve el binding real via
// `TextEngineEntryPoint`). Con la camara virtual el OCR suele ser blank ->
// `ExtraerTicket` NO invoca al motor y el resultado llega enseguida con
// "0/5 campos extraidos" (escenario 5). Si el OCR tuvo texto, la extraccion es
// REAL (Gemini) y puede tardar 5-36 s (R2): el test espera a resultado o a
// error honesto; un error del motor real en este entorno se reporta como
// assumption failed (nunca como pass inventado), igual que la camara virtual
// sin frames en CapturaFlujoTest. La determinizacion de pasos y cancelacion en
// vuelo vive en JVM (EscanearViewModelTest con FakeTextEngine.gate) y la
// extraccion real por UI en dispositivo fisico (T9).
//
// El permiso de camara se concede con `pm grant` (misma politica que
// CapturaFlujoTest: `pm revoke` en caliente mata el proceso bajo test).
class ProcesadoFlujoTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val reglas = composeRule

    @Test
    fun procesadoMuestraElBadgeRealYElResultadoSoloLecturaConRevisarHabilitado() {
        capturarFoto()

        // "Continuar" ya esta cableado al procesado (spec procesado-ui).
        composeRule.onNodeWithTag("captura_continuar").assertIsDisplayed()
        composeRule.onNodeWithTag("captura_continuar").assertIsEnabled()
        composeRule.onNodeWithTag("captura_continuar").performClick()

        // Mockup 06: raiz de procesado + badge del motor real + pasos.
        esperarTag("procesado_pantalla")
        composeRule.onNodeWithTag("procesado_pantalla").assertIsDisplayed()
        composeRule.onNodeWithTag("procesado_badge_motor").assertIsDisplayed()
        composeRule.onNodeWithTag("procesado_badge_motor")
            .assertTextContains("Motor Nube", substring = true)
        composeRule.onNodeWithTag("procesado_badge_motor")
            .assertTextContains("Gemini", substring = true)
        composeRule.onNodeWithTag("procesado_pasos").assertIsDisplayed()
        composeRule.onNodeWithTag("procesado_paso_encuadre").assertIsDisplayed()
        composeRule.onNodeWithTag("procesado_paso_ocr").assertIsDisplayed()
        composeRule.onNodeWithTag("procesado_paso_categoria").assertIsDisplayed()

        val terminoEnResultado = esperarFinDeExtraccion()
        if (!terminoEnResultado) {
            // Escenario 4: el motor real fallo (red/cuota/respuesta). La UI
            // muestra el error honesto con sus dos salidas; la extraccion real
            // por UI se cierra en dispositivo fisico (T9).
            composeRule.onNodeWithTag("procesado_error").assertIsDisplayed()
            composeRule.onNodeWithTag("procesado_reintentar").assertIsDisplayed()
            composeRule.onNodeWithTag("procesado_volver_foto").assertIsDisplayed()
            assumeTrue(
                "La extraccion REAL fallo en este entorno (ver mensaje en " +
                    "procesado_error); la verificacion con datos reales es T9.",
                false,
            )
            return
        }

        // Escenario 1/5: resultado de SOLO lectura con completitud real
        // ("n/5 campos extraidos", nunca % de confianza) y "Revisar ticket"
        // habilitado (spec revision-datos: abre la revision editable).
        composeRule.onNodeWithTag("procesado_completitud").assertIsDisplayed()
        composeRule.onNodeWithTag("procesado_completitud")
            .assertTextContains("/5", substring = true)
        composeRule.onNodeWithTag("procesado_completitud")
            .assertTextContains("campos extra\u00eddos", substring = true)
        composeRule.onNodeWithTag("procesado_revisar").assertIsDisplayed()
        composeRule.onNodeWithTag("procesado_revisar").assertIsEnabled()
    }

    @Test
    fun backDesdeElProcesadoVuelveALaFotoConElBorradorIntacto() {
        capturarFoto()

        composeRule.onNodeWithTag("captura_continuar").performClick()
        esperarTag("procesado_pantalla")

        // Back del sistema = Cancelar si la extraccion sigue en vuelo o
        // "Volver a la foto" si ya termino: en ambos casos la pantalla 05
        // reaparece con el Borrador intacto (misma foto, "Continuar"
        // reintentable) y sin datos huerfanos.
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("pantalla_escanear").assertIsDisplayed()
        composeRule.onNodeWithTag("captura_revision").assertIsDisplayed()
        composeRule.onNodeWithTag("captura_continuar").assertIsEnabled()
        assertTrue(
            "la pantalla de procesado no debe seguir visible",
            composeRule.onAllNodesWithTag("procesado_pantalla")
                .fetchSemanticsNodes().isEmpty(),
        )
        composeRule.waitUntil(TIMEOUT_CAPTURA_MS) {
            composeRule.onAllNodesWithTag("captura_foto").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("captura_foto").assertIsDisplayed()
    }

    // --- helpers (misma politica que CapturaFlujoTest) ---

    /** Captura una foto con la camara virtual y espera la pantalla 05. */
    private fun capturarFoto() {
        concederPermisoCamara()
        irAEscanear()
        esperarCamaraLista()
        composeRule.onNodeWithTag("captura_obturador").performClick()
        composeRule.waitUntil(TIMEOUT_CAPTURA_MS) {
            composeRule.onAllNodesWithTag("captura_revision").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithTag("captura_error").fetchSemanticsNodes().isNotEmpty()
        }
        val hayRevision =
            composeRule.onAllNodesWithTag("captura_revision").fetchSemanticsNodes().isNotEmpty()
        assumeTrue(
            "Camara virtual del emulador sin frames: la captura real se cierra en " +
                "dispositivo fisico (verificacion 1 de procesado-ui, T9).",
            hayRevision,
        )
    }

    /** Espera al final de la extraccion; `true` = resultado, `false` = error. */
    private fun esperarFinDeExtraccion(): Boolean {
        composeRule.waitUntil(TIMEOUT_EXTRACCION_MS) {
            composeRule.onAllNodesWithTag("procesado_completitud").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithTag("procesado_error").fetchSemanticsNodes().isNotEmpty()
        }
        return composeRule.onAllNodesWithTag("procesado_completitud")
            .fetchSemanticsNodes().isNotEmpty()
    }

    private fun esperarTag(tag: String) {
        composeRule.waitUntil(TIMEOUT_PROCESADO_MS) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun irAEscanear() {
        composeRule.onNodeWithTag("bottom_bar_escanear").performClick()
        composeRule.waitForIdle()
    }

    /** Espera activa a que la camara este vinculada (obturador habilitado). */
    private fun esperarCamaraLista() {
        composeRule.waitUntil(TIMEOUT_CAMARA_MS) {
            composeRule
                .onAllNodesWithTag("captura_camara_cargando")
                .fetchSemanticsNodes()
                .isEmpty()
        }
    }

    /** `pm grant` de CAMARA (equivalente al adb manual del spec; no mata el proceso). */
    private fun concederPermisoCamara() {
        ejecutarShell("pm grant com.robertomr.tally android.permission.CAMERA")
        composeRule.waitForIdle()
    }

    private fun ejecutarShell(comando: String) {
        val fd = InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .executeShellCommand(comando)
        // Leer hasta EOF asegura que el comando de shell ha terminado.
        ParcelFileDescriptor.AutoCloseInputStream(fd).use { it.readBytes() }
    }

    private companion object {
        const val TIMEOUT_CAMARA_MS = 10_000L
        const val TIMEOUT_CAPTURA_MS = 20_000L
        const val TIMEOUT_PROCESADO_MS = 30_000L
        // La extraccion REAL (Gemini) tarda 5-36 s (R2).
        const val TIMEOUT_EXTRACCION_MS = 90_000L
    }
}

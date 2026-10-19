package com.robertomr.tally.captura

import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.robertomr.tally.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// E2E enfocado de la feature revision-datos (spec, T6): resultado del procesado
// (mockup 06) -> "Revisar ticket" (mockup 07) -> elegir una categoria -> Guardar
// -> pestaña Gastos, y regreso a Escanear con el subflujo reseteado (R-REV3).
//
// Determinismo (R-PROC3): con la camara virtual el OCR suele ser blank, asi que
// `ExtraerTicket` NO invoca al motor (extraccion vacia) y el resultado llega
// enseguida; la revision se rellena a mano. NO se usa @TestInstallIn de
// `TextEngine` (romperia `ExtraccionNubeRealTest`). Si el OCR tuviera texto y la
// extraccion REAL fallara (red/cuota), el test se salta con motivo explicito
// (assumption), nunca como pass inventado. El permiso se concede con `pm grant`.
@RunWith(AndroidJUnit4::class)
class RevisionDatosFlujoTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val reglas = composeRule

    @Test
    fun revisarElegirCategoriaYGuardarVuelveALaPestanaGastos() {
        capturarFoto()
        composeRule.onNodeWithTag("captura_continuar").performClick()

        // Resultado del procesado o error honesto si la extraccion real fallo.
        val hayResultado = esperarFinDeExtraccion()
        if (!hayResultado) {
            assumeTrue(
                "La extraccion REAL fallo en este entorno (red/cuota); la " +
                    "verificacion con datos reales es el paso fisico del orquestador.",
                false,
            )
            return
        }

        // "Revisar ticket" ya esta habilitado (spec revision-datos).
        composeRule.onNodeWithTag("procesado_revisar").assertIsEnabled()
        composeRule.onNodeWithTag("procesado_revisar").performClick()

        // Mockup 07: revision editable con las 6 categorias de la enum fija.
        esperarTag("revision_pantalla")
        composeRule.onNodeWithTag("papel_ticket").assertIsDisplayed()
        composeRule.onNodeWithTag("revision_pantalla").assertIsDisplayed()
        composeRule.onNodeWithTag("revision_badge_motor").assertIsDisplayed()
        CATEGORIAS.forEach { categoria ->
            composeRule.onNodeWithTag("revision_categoria_$categoria").assertIsDisplayed()
        }

        // Elegir una categoria distinta de OTROS y guardar.
        composeRule.onNodeWithTag("revision_categoria_ALIMENTACION").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("revision_guardar").assertIsEnabled()
        composeRule.onNodeWithTag("revision_guardar").performClick()

        // Se persiste y se vuelve al listado (pestaña Gastos, placeholder).
        esperarTag("pantalla_gastos")
        composeRule.onNodeWithTag("pantalla_gastos").assertIsDisplayed()

        // R-REV3: el subflujo de Escanear quedo reseteado a Encuadre (no muestra
        // la revision del ticket ya guardado).
        composeRule.onNodeWithTag("bottom_bar_escanear").performClick()
        composeRule.waitForIdle()
        esperarTag("captura_encuadre")
        composeRule.onNodeWithTag("captura_encuadre").assertIsDisplayed()
        assertTrue(
            "no debe quedar la revision del ticket guardado",
            composeRule.onAllNodesWithTag("revision_pantalla")
                .fetchSemanticsNodes().isEmpty(),
        )
    }

    // E2E enfocado de edicion-lineas (spec, T7): extraccion vacia -> Revisar ->
    // anadir dos lineas -> editarlas -> eliminar una -> el "Total recalculado"
    // sigue la suma y el total del ticket NO cambia -> Guardar cambios -> Gastos.
    // La persistencia exacta de las lineas editadas se verifica en JVM
    // (GuardarTicketTest/EscanearViewModelTest) hasta que exista listado-gastos.
    @Test
    fun anadirEditarYEliminarLineasYGuardarCambiosVuelveALaPestanaGastos() {
        if (!abrirRevisionDatos()) return

        composeRule.onNodeWithTag("revision_lineas").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("revision_lineas_contador").assertTextContains("0 artículos", substring = true)
        composeRule.onNodeWithTag("revision_total_recalculado").assertTextContains("0,00", substring = true)

        // Anadir dos lineas (ids 0 y 1) y rellenarlas.
        composeRule.onNodeWithTag("revision_anadir_linea").performScrollTo().performClick()
        composeRule.onNodeWithTag("revision_anadir_linea").performScrollTo().performClick()
        composeRule.waitForIdle()
        escribir("revision_linea_0_concepto", "PAN")
        escribir("revision_linea_0_cantidad", "3")
        escribir("revision_linea_0_precio", "1,50")
        escribir("revision_linea_1_concepto", "AGUA")
        escribir("revision_linea_1_precio", "2")
        composeRule.onNodeWithTag("revision_lineas_contador").assertTextContains("2 artículos", substring = true)
        composeRule.onNodeWithTag("revision_total_recalculado").assertTextContains("6,50", substring = true)

        // Una cantidad invalida se ignora: la pantalla sigue viva y la suma igual.
        escribir("revision_linea_0_cantidad", "0")
        composeRule.onNodeWithTag("revision_total_recalculado").assertTextContains("6,50", substring = true)
        escribir("revision_linea_0_cantidad", "3")

        // Un precio vacio cuenta como 0 (el total baja) y se puede volver a rellenar.
        escribir("revision_linea_1_precio", "")
        composeRule.onNodeWithTag("revision_total_recalculado").assertTextContains("4,50", substring = true)
        escribir("revision_linea_1_precio", "2")
        composeRule.onNodeWithTag("revision_total_recalculado").assertTextContains("6,50", substring = true)

        // Eliminar la segunda linea.
        composeRule.onNodeWithTag("revision_linea_1_borrar").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(
            "la linea eliminada no debe seguir en pantalla",
            composeRule.onAllNodesWithTag("revision_linea_1").fetchSemanticsNodes().isEmpty(),
        )
        composeRule.onNodeWithTag("revision_lineas_contador").assertTextContains("1 artículo", substring = true)
        composeRule.onNodeWithTag("revision_total_recalculado").assertTextContains("4,50", substring = true)

        // El total del ticket (autoritativo) no se ha tocado al editar lineas.
        composeRule.onNodeWithTag("revision_total").performScrollTo().assertTextContains("0,00", substring = true)

        composeRule.onNodeWithTag("revision_guardar").assertIsEnabled()
        composeRule.onNodeWithTag("revision_guardar").performClick()

        esperarTag("pantalla_gastos")
        composeRule.onNodeWithTag("pantalla_gastos").assertIsDisplayed()
    }

    // Escenario 6 de edicion-lineas: "Descartar" con cambios sin guardar vuelve al
    // resultado del procesado sin escribir (la persistencia nula se verifica en JVM).
    @Test
    fun descartarConLineasEditadasVuelveAlResultadoDelProcesado() {
        if (!abrirRevisionDatos()) return

        composeRule.onNodeWithTag("revision_anadir_linea").performScrollTo().performClick()
        composeRule.waitForIdle()
        escribir("revision_linea_0_concepto", "SIN GUARDAR")

        composeRule.onNodeWithTag("revision_descartar").performClick()
        esperarTag("procesado_completitud")
        composeRule.onNodeWithTag("procesado_completitud").assertIsDisplayed()
        assertTrue(
            "no debe quedar la pantalla de revision",
            composeRule.onAllNodesWithTag("revision_pantalla").fetchSemanticsNodes().isEmpty(),
        )
    }

    // --- helpers (misma politica que ProcesadoFlujoTest) ---

    /**
     * Captura -> Continuar -> Revisar ticket. `false` (con assumption) si la
     * extraccion REAL fallo en el entorno (red/cuota).
     */
    private fun abrirRevisionDatos(): Boolean {
        capturarFoto()
        composeRule.onNodeWithTag("captura_continuar").performClick()
        val hayResultado = esperarFinDeExtraccion()
        assumeTrue(
            "La extraccion REAL fallo en este entorno (red/cuota); la " +
                "verificacion con datos reales es el paso fisico del orquestador.",
            hayResultado,
        )
        composeRule.onNodeWithTag("procesado_revisar").performClick()
        esperarTag("revision_pantalla")
        return true
    }

    private fun escribir(tag: String, texto: String) {
        composeRule.onNodeWithTag(tag).performScrollTo().performTextReplacement(texto)
        composeRule.waitForIdle()
    }

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
            "Camara virtual del emulador sin frames: la captura real se verifica en " +
                "dispositivo fisico (paso manual del orquestador).",
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

    private fun esperarCamaraLista() {
        composeRule.waitUntil(TIMEOUT_CAMARA_MS) {
            composeRule
                .onAllNodesWithTag("captura_camara_cargando")
                .fetchSemanticsNodes()
                .isEmpty()
        }
    }

    /** `pm grant` de CAMARA (equivalente al adb manual; no mata el proceso). */
    private fun concederPermisoCamara() {
        ejecutarShell("pm grant com.robertomr.tally android.permission.CAMERA")
        composeRule.waitForIdle()
    }

    private fun ejecutarShell(comando: String) {
        val fd = InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .executeShellCommand(comando)
        ParcelFileDescriptor.AutoCloseInputStream(fd).use { it.readBytes() }
    }

    private companion object {
        val CATEGORIAS = listOf(
            "ALIMENTACION",
            "HOGAR",
            "TRANSPORTE",
            "OCIO",
            "SALUD",
            "OTROS",
        )
        const val TIMEOUT_CAMARA_MS = 10_000L
        const val TIMEOUT_CAPTURA_MS = 20_000L
        const val TIMEOUT_PROCESADO_MS = 30_000L
        const val TIMEOUT_EXTRACCION_MS = 90_000L
    }
}

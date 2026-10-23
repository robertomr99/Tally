package com.robertomr.tally.captura

import android.content.pm.PackageManager
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.robertomr.tally.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

// E2E enfocado de la feature captura-foto (spec, T11): encuadre guiado
// (mockup 04) con permiso concedido, obturador -> pantalla 05 (revision de la
// foto) y "Repetir foto" -> encuadre; ademas, el estado in-app de permiso
// denegado (escenario 6) sin crash.
//
// El permiso de camara se concede con `pm grant` al inicio de cada test que lo
// necesita (antes de navegar a Escanear, que es donde se evalua el gate). NO
// se usa `pm revoke` en caliente: revocar un permiso runtime mata el proceso
// de la app bajo instrumentacion (comportamiento de Android, reproducido el
// 2026-10-15 en logcat: "Killing ... com.robertomr.tally ... permissions
// revoked") y aborta toda la tanda. Por eso `denegado...` corre la primera
// (@FixMethodOrder) sobre el estado limpio de instalacion, sin grants previos.
//
// La camara del emulador es simulada: si la escena virtual no entrega frames,
// el test valida los estados de UI y deja la captura real a los dispositivos
// fisicos (realme GT 2 / Tab S9+); en ese caso el test queda
// como "assumption failed" (skipped), nunca como pass inventado.
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class CapturaFlujoTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val reglas = composeRule

    @Test
    fun denegadoMuestraEstadoInAppSinCrash() {
        // Escenario 6: permiso denegado -> estado in-app en espanol con boton
        // para reintentar; la raiz pantalla_escanear sigue presente. Se
        // comprueba el estado real del sistema: si otro test ya concedio el
        // permiso (orden/estado sucio), no se puede revocar sin matar el
        // proceso, asi que el test se salta con el motivo explicito.
        val concedido = hayPermisoDeCamara()
        assumeTrue(
            "CAMERA ya concedida: no se puede revocar en caliente (Android mata el " +
                "proceso bajo test). Escenario 6 cubierto con instalacion limpia y " +
                "verificacion manual.",
            !concedido,
        )

        irAEscanear()

        composeRule.onNodeWithTag("pantalla_escanear").assertIsDisplayed()
        composeRule.onNodeWithTag("captura_permiso").assertIsDisplayed()
        composeRule.onNodeWithTag("captura_permitir").assertIsDisplayed()

        val permisoTrasUi = hayPermisoDeCamara()
        assertTrue("el estado in-app no debe conceder permisos", !permisoTrasUi)
    }

    @Test
    fun obturadorAbreLaRevisionDeLaFotoYRepetirVuelveAlEncuadre() {
        concederPermisoCamara()
        irAEscanear()
        esperarCamaraLista()

        composeRule.onNodeWithTag("captura_obturador").performClick()

        // Tras la captura se abre la pantalla 05 (revision) o, si la camara
        // simulada no entrega frames, el estado de error sin crash.
        composeRule.waitUntil(TIMEOUT_CAPTURA_MS) {
            composeRule.onAllNodesWithTag("captura_revision").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithTag("captura_error").fetchSemanticsNodes().isNotEmpty()
        }
        val hayRevision =
            composeRule.onAllNodesWithTag("captura_revision").fetchSemanticsNodes().isNotEmpty()

        if (!hayRevision) {
            // Limitacion conocida del emulador: la camara virtual no entrego
            // frames. Se valida al menos el estado de error y la vuelta al
            // encuadre; la foto real en pantalla 05 se verifica en fisico.
            composeRule.onNodeWithTag("captura_error").assertIsDisplayed()
            composeRule.onNodeWithTag("captura_reintentar").performClick()
            composeRule.onNodeWithTag("captura_encuadre").assertIsDisplayed()
            assumeTrue(
                "Camara virtual del emulador sin frames: la captura real se cierra " +
                    "en dispositivo fisico (verificacion 1 de captura-foto)",
                false,
            )
            return
        }

        // Escenario 5: pantalla 05 con la foto, "Repetir foto" operativo y
        // "Continuar" habilitado (abre el procesado de procesado-ui).
        composeRule.onNodeWithTag("captura_revision").assertIsDisplayed()
        composeRule.onNodeWithTag("captura_foto").assertIsDisplayed()
        composeRule.onNodeWithTag("captura_repetir").assertIsDisplayed()
        composeRule.onNodeWithTag("captura_continuar").assertIsDisplayed()
        composeRule.onNodeWithTag("captura_continuar").assertIsEnabled()

        // Escenario 1/3/4 de ocr-mlkit: el panel de debug del texto crudo esta
        // presente en builds debug (el test instala el build debug) con el
        // texto, "(OCR vacio)" o "OCR no disponible". La espera de arriba
        // (TIMEOUT_CAPTURA_MS) absorbe la latencia del OCR, que corre dentro
        // del estado Procesando antes de abrir la revision. La escena de la
        // camara virtual no contiene un ticket real, asi que solo se comprueba
        // la PRESENCIA del panel (la calidad del texto es verificacion fisica).
        composeRule.onNodeWithTag("captura_texto_crudo").assertIsDisplayed()

        // Escenario 7: "Repetir foto" vuelve al encuadre (el borrado en disco de
        // la foto reemplazada esta cubierto por EscanearViewModelTest).
        composeRule.onNodeWithTag("captura_repetir").performClick()
        composeRule.onNodeWithTag("captura_encuadre").assertIsDisplayed()
    }

    @Test
    fun permisoConcedidoMuestraElEncuadreGuiado() {
        concederPermisoCamara()
        irAEscanear()

        // Raiz estable (regresion de NavigationTest) + mockup 04 visible.
        composeRule.onNodeWithTag("pantalla_escanear").assertIsDisplayed()
        composeRule.onNodeWithTag("captura_encuadre").assertIsDisplayed()
        composeRule.onNodeWithTag("captura_obturador").assertIsDisplayed()
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

    private fun hayPermisoDeCamara(): Boolean {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        return androidx.core.content.ContextCompat.checkSelfPermission(
            contexto,
            android.Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
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
    }
}

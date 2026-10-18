package com.robertomr.tally.ajustes

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.robertomr.tally.MainActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// E2E de la feature ajustes-motor (spec, S1/S3/S6) en emulador: la seccion
// "Motor de extraccion" muestra las tres opciones, elegir una la selecciona y la
// escribe en disco (lo que lee el router y sobrevive al reinicio del proceso), y
// el aviso de Local aparece sin modelo descargado. El reinicio real del proceso
// (am force-stop) se verifica a mano y queda en docs/evidence/ajustes-motor/.
@RunWith(AndroidJUnit4::class)
class AjustesMotorFlujoTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val contexto: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @After
    fun restaurarAuto() {
        runCatching {
            abrirAjustes()
            elegir("motor_opcion_auto")
        }
    }

    @Test
    fun laSeccionMuestraLasTresOpcionesYLaPersistidaEstaSeleccionada() {
        abrirAjustes()
        composeRule.onNodeWithTag("seccion_motor").assertIsDisplayed()
        composeRule.onNodeWithTag("motor_opcion_auto").assertIsDisplayed()
        composeRule.onNodeWithTag("motor_opcion_local").assertIsDisplayed()
        composeRule.onNodeWithTag("motor_opcion_nube").assertIsDisplayed()
        // Sin valor en disco la opcion vigente es Auto; si lo hay, la que diga el disco.
        val guardado = guardadoEnDisco() ?: "AUTO"
        composeRule.onNodeWithTag("motor_opcion_${guardado.lowercase()}").assertIsSelected()
    }

    @Test
    fun elegirNubeLaSeleccionaYLaPersisteEnDisco() {
        abrirAjustes()
        elegir("motor_opcion_nube")
        composeRule.onNodeWithTag("motor_opcion_auto").assertIsNotSelected()
        composeRule.onNodeWithTag("motor_opcion_local").assertIsNotSelected()
        assertEquals("NUBE", guardadoEnDisco())
    }

    @Test
    fun elegirLocalSinModeloMuestraElAvisoAnticipado() {
        abrirAjustes()
        elegir("motor_opcion_local")
        assertEquals("LOCAL", guardadoEnDisco())
        composeRule.onNodeWithTag("motor_aviso_local").performScrollTo().assertIsDisplayed()
        elegir("motor_opcion_nube")
        composeRule.onNodeWithTag("motor_aviso_local").assertDoesNotExist()
    }

    private fun abrirAjustes() {
        composeRule.onNodeWithTag("boton_ajustes").performClick()
        composeRule.onNodeWithTag("pantalla_ajustes").assertIsDisplayed()
        composeRule.waitUntil(10_000) {
            runCatching { composeRule.onNodeWithTag("seccion_motor").assertIsDisplayed() }.isSuccess
        }
    }

    private fun elegir(tag: String) {
        composeRule.onNodeWithTag(tag).performClick()
        composeRule.onNodeWithTag(tag).assertIsSelected()
    }

    private fun guardadoEnDisco(): String? =
        contexto.getSharedPreferences("tally_ajustes", Context.MODE_PRIVATE)
            .getString("preferencia_motor", null)
}

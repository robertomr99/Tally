package com.robertomr.tally.gastos

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.robertomr.tally.MainActivity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// E2E del dock de Nuevo gasto (spec mejoras-visuales-3 escenario 4, actualizado por
// gasto-manual-2): el dock solo tiene Descartar y "Guardar gasto" (el bloque "Total
// recalculado" ya no existe: hay un unico total al final del papel); al anadir una linea
// con total 0 ese total pasa a mostrar la suma. No guarda nada: nunca pulsa Guardar,
// asi que no deja datos en el emulador.
@RunWith(AndroidJUnit4::class)
class NuevoGastoDockFlujoTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun existe(tag: String): Boolean =
        composeRule.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun esperarTag(tag: String) {
        composeRule.waitUntil(10_000) { existe(tag) }
    }

    private fun abrirNuevoGasto() {
        composeRule.onNodeWithTag("bottom_bar_gastos").performClick()
        esperarTag("pantalla_gastos")
        composeRule.onNodeWithTag("gastos_anadir").performClick()
        esperarTag("pantalla_nuevo_gasto")
    }

    @Test
    fun dockSoloTieneDescartarYGuardarGastoSinTotalRecalculado() {
        abrirNuevoGasto()

        composeRule.onNodeWithTag("revision_total").performScrollTo()
        composeRule.onNodeWithTag("revision_total").performTextReplacement("12,5")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("revision_guardar").assertIsEnabled()
        composeRule.onNodeWithText("Guardar gasto").assertExists()
        composeRule.onNodeWithTag("revision_descartar").assertExists()
        assertFalse(existe("revision_total_recalculado"))

        // Con total tecleado, anadir una linea no lo toca y tampoco aparece "Total recalculado".
        composeRule.onNodeWithTag("revision_anadir_linea").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertFalse(existe("revision_total_recalculado"))
        composeRule.onNodeWithText("Guardar gasto").assertExists()
    }

    @Test
    fun alAnadirUnaLineaConTotalCeroElTotalPasaAMostrarLaSuma() {
        abrirNuevoGasto()

        composeRule.onNodeWithTag("revision_anadir_linea").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("revision_linea_0_concepto").performScrollTo()
        composeRule.onNodeWithTag("revision_linea_0_concepto").performTextReplacement("PAN")
        composeRule.onNodeWithTag("revision_linea_0_cantidad").performTextReplacement("3")
        composeRule.onNodeWithTag("revision_linea_0_precio").performTextReplacement("1,50")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("revision_total").performScrollTo().assertTextContains("4,50", substring = true)
        assertTrue(existe("revision_total_manual"))
        assertFalse(existe("revision_total_recalculado"))
        composeRule.onNodeWithTag("revision_guardar").assertIsEnabled()
    }
}

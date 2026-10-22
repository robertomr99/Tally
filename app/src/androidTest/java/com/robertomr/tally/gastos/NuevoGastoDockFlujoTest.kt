package com.robertomr.tally.gastos

import androidx.compose.ui.test.assertIsEnabled
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

// E2E del dock de Nuevo gasto (spec mejoras-visuales-3, escenario 4): con total escrito y sin
// lineas no se muestra el "Total recalculado" (seria 0,00 y confundiria) y el boton se llama
// "Guardar gasto"; al anadir una linea el bloque aparece. No guarda nada: nunca pulsa Guardar,
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

    @Test
    fun sinLineasNoHayTotalRecalculadoYElBotonDiceGuardarGasto() {
        composeRule.onNodeWithTag("bottom_bar_gastos").performClick()
        esperarTag("pantalla_gastos")
        composeRule.onNodeWithTag("gastos_anadir").performClick()
        esperarTag("pantalla_nuevo_gasto")

        composeRule.onNodeWithTag("revision_total").performScrollTo()
        composeRule.onNodeWithTag("revision_total").performTextReplacement("12,5")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("revision_guardar").assertIsEnabled()
        composeRule.onNodeWithText("Guardar gasto").assertExists()
        assertFalse(existe("revision_total_recalculado"))

        composeRule.onNodeWithTag("revision_anadir_linea").performScrollTo().performClick()
        composeRule.waitForIdle()
        esperarTag("revision_total_recalculado")
        assertTrue(existe("revision_total_recalculado"))
        composeRule.onNodeWithText("Guardar gasto").assertExists()
    }
}

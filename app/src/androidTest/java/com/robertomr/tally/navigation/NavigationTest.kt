package com.robertomr.tally.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.robertomr.tally.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// E2E de navegación de la feature navegacion-base (spec): 3 destinos desde la
// bottom bar, pill activo según el destino, sin crash y back del sistema.
@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun startsOnResumenWithActivePill() {
        composeRule.onNodeWithTag("pantalla_resumen").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_bar_resumen").assertIsSelected()
        composeRule.onNodeWithTag("bottom_bar_gastos").assertIsNotSelected()
        composeRule.onNodeWithTag("bottom_bar_escanear").assertIsNotSelected()
    }

    @Test
    fun bottomBarNavigatesAcrossTheThreeDestinations() {
        composeRule.onNodeWithTag("bottom_bar_gastos").performClick()
        composeRule.onNodeWithTag("pantalla_gastos").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_bar_gastos").assertIsSelected()
        composeRule.onNodeWithTag("bottom_bar_resumen").assertIsNotSelected()

        composeRule.onNodeWithTag("bottom_bar_escanear").performClick()
        composeRule.onNodeWithTag("pantalla_escanear").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_bar_escanear").assertIsSelected()
        composeRule.onNodeWithTag("bottom_bar_gastos").assertIsNotSelected()

        composeRule.onNodeWithTag("bottom_bar_resumen").performClick()
        composeRule.onNodeWithTag("pantalla_resumen").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_bar_resumen").assertIsSelected()
        composeRule.onNodeWithTag("bottom_bar_escanear").assertIsNotSelected()
    }

    @Test
    fun systemBackReturnsToResumenAndNextBackExitsTheApp() {
        composeRule.onNodeWithTag("bottom_bar_gastos").performClick()
        composeRule.onNodeWithTag("pantalla_gastos").assertIsDisplayed()

        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pantalla_resumen").assertIsDisplayed()

        // El cierre tras el segundo back no es síncrono (finishAfterTransition /
        // predictive back en API 35+ aplazan finish()), así que se espera activa
        // a que la actividad empiece a cerrarse en vez de leer isFinishing en el
        // mismo runnable (comportamiento real verificado en emulator-5554).
        val activity = composeRule.activity
        composeRule.runOnUiThread {
            activity.onBackPressedDispatcher.onBackPressed()
        }
        var finishingAfterSecondBack = false
        val deadlineMillis = System.currentTimeMillis() + 5_000
        while (!finishingAfterSecondBack && System.currentTimeMillis() < deadlineMillis) {
            finishingAfterSecondBack = activity.isFinishing || activity.isDestroyed
            if (!finishingAfterSecondBack) Thread.sleep(50)
        }
        assertTrue(finishingAfterSecondBack)
    }
}

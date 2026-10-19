package com.robertomr.tally.gastos

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.robertomr.tally.domain.expenses.entities.PeriodoTipo
import com.robertomr.tally.feature.common.theme.TallyTheme
import com.robertomr.tally.feature.gastos.GastosContenido
import com.robertomr.tally.feature.gastos.GastosUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Estados fijos de la pantalla Gastos (spec listado-gastos, escenario 5): el
// vacio no se puede forzar con datos reales (la base del emulador no esta
// vacia), asi que se pinta el contenido sin ViewModel con un estado dado.
@RunWith(AndroidJUnit4::class)
class ListadoGastosVacioTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun estadoVacioMuestraTextoYBotonEscanearQueInvocaElCallback() {
        var escanear = 0
        composeRule.setContent {
            TallyTheme {
                GastosContenido(
                    ui = GastosUiState(cargando = false, tipo = PeriodoTipo.DIA, grupos = emptyList()),
                    onSeleccionarPeriodo = {},
                    onTicketClick = {},
                    onEscanear = { escanear++ },
                )
            }
        }
        composeRule.onNodeWithTag("pantalla_gastos").assertIsDisplayed()
        composeRule.onNodeWithTag("listado_vacio").assertIsDisplayed()
        composeRule.onNodeWithTag("lista_gastos").assertDoesNotExist()
        composeRule.onNodeWithTag("boton_escanear_vacio").assertIsDisplayed().performClick()
        assertEquals(1, escanear)
    }

    @Test
    fun cargandoNoPintaListaNiVacio() {
        composeRule.setContent {
            TallyTheme {
                GastosContenido(
                    ui = GastosUiState(),
                    onSeleccionarPeriodo = {},
                    onTicketClick = {},
                    onEscanear = {},
                )
            }
        }
        composeRule.onNodeWithTag("periodo_mes").assertIsDisplayed()
        composeRule.onNodeWithTag("lista_gastos").assertDoesNotExist()
        composeRule.onNodeWithTag("listado_vacio").assertDoesNotExist()
        composeRule.onNode(hasTestTag("ticket_1")).assertDoesNotExist()
    }
}

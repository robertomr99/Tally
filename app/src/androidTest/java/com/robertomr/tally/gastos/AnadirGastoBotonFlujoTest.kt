package com.robertomr.tally.gastos

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.robertomr.tally.domain.expenses.entities.FiltroFormaPago
import com.robertomr.tally.domain.expenses.entities.PeriodoTipo
import com.robertomr.tally.feature.common.theme.TallyTheme
import com.robertomr.tally.feature.gastos.GastosContenido
import com.robertomr.tally.feature.gastos.GastosUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Boton "Añadir gasto" de Gastos (spec gasto-manual-2, C) a fontScale 1,0 (cabecera) y 2,0 (fila
// propia): visible, >= 48 dp, con texto y que dispara el callback. Estados fijos, sin ViewModel
// ni datos reales: no escribe nada. El flujo completo hasta `pantalla_nuevo_gasto` lo cubren
// GastoManualFlujoTest y NuevoGastoDockFlujoTest, que pulsan este mismo tag.
@RunWith(AndroidJUnit4::class)
class AnadirGastoBotonFlujoTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun gastos(fontScale: Float, filtro: FiltroFormaPago, hayTickets: Boolean, onAnadir: () -> Unit) {
        composeRule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale = fontScale)) {
                TallyTheme {
                    Box(Modifier.width(360.dp).height(720.dp)) {
                        GastosContenido(
                            ui = GastosUiState(
                                cargando = false,
                                tipo = PeriodoTipo.DIA,
                                grupos = emptyList(),
                                filtro = filtro,
                                hayTicketsEnPeriodo = hayTickets,
                            ),
                            onSeleccionarPeriodo = {},
                            onSeleccionarRango = { _, _ -> },
                            onTicketClick = {},
                            onEscanear = {},
                            onAnadirGasto = onAnadir,
                        )
                    }
                }
            }
        }
    }

    private fun comprobarBoton(veces: IntArray) {
        composeRule.onNodeWithTag("gastos_anadir").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
            .assertTextContains("Añadir gasto")
        composeRule.onNodeWithTag("gastos_anadir").performClick()
        assertEquals(1, veces[0])
    }

    @Test
    fun fontScaleNormalMuestraElBotonConTextoYAbre() {
        val veces = intArrayOf(0)
        gastos(1.0f, FiltroFormaPago.TODOS, hayTickets = false) { veces[0]++ }
        comprobarBoton(veces)
        // En el vacio sigue tambien el boton propio del vacio.
        composeRule.onNodeWithTag("gastos_anadir_vacio").assertIsDisplayed()
    }

    @Test
    fun fontScale13EnFilaPropiaTambienVisible() {
        val veces = intArrayOf(0)
        gastos(1.3f, FiltroFormaPago.TODOS, hayTickets = false) { veces[0]++ }
        comprobarBoton(veces)
    }

    @Test
    fun fontScaleDobleEnFilaPropiaVisible() {
        val veces = intArrayOf(0)
        gastos(2.0f, FiltroFormaPago.TODOS, hayTickets = false) { veces[0]++ }
        comprobarBoton(veces)
    }

    @Test
    fun conFiltroSinResultadosElBotonSigueEnLaCabecera() {
        val veces = intArrayOf(0)
        gastos(1.0f, FiltroFormaPago.TARJETA, hayTickets = true) { veces[0]++ }
        comprobarBoton(veces)
    }
}

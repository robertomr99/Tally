package com.robertomr.tally.gastos

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
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

// Estados fijos de Gastos con filtro (spec filtro-forma-pago, escenarios 4, 5 y 8): el
// vacio por filtro no se puede forzar con los datos reales del emulador (hay tickets
// residuales de otros E2E), asi que se pinta GastosContenido sin ViewModel.
@RunWith(AndroidJUnit4::class)
class FiltroFormaPagoVacioTest {

    @get:Rule
    val composeRule = createComposeRule()

    // Escenario 4: hay tickets en el periodo pero ninguno de tarjeta -> vacio especifico.
    @Test
    fun vacioPorFiltroOfreceVerTodosYNoEscanearNiAnadir() {
        val pulsados = mutableListOf<FiltroFormaPago>()
        composeRule.setContent {
            TallyTheme {
                var ui by remember {
                    mutableStateOf(
                        GastosUiState(
                            cargando = false, tipo = PeriodoTipo.DIA, grupos = emptyList(),
                            filtro = FiltroFormaPago.TARJETA, hayTicketsEnPeriodo = true,
                        ),
                    )
                }
                GastosContenido(
                    ui = ui,
                    onSeleccionarPeriodo = {},
                    onSeleccionarRango = { _, _ -> },
                    onTicketClick = {},
                    onEscanear = {},
                    onSeleccionarFiltro = { f ->
                        pulsados += f
                        ui = ui.copy(filtro = f, hayTicketsEnPeriodo = true, grupos = emptyList())
                    },
                )
            }
        }
        composeRule.onNodeWithTag("filtro_pago_tarjeta").assertIsSelected()
        composeRule.onNodeWithTag("listado_vacio_filtro").assertIsDisplayed()
        composeRule.onNodeWithTag("listado_vacio").assertDoesNotExist()
        composeRule.onNodeWithTag("boton_escanear_vacio").assertDoesNotExist()
        composeRule.onNodeWithTag("gastos_anadir_vacio").assertDoesNotExist()
        composeRule.onNodeWithTag("filtro_vaciar").assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(listOf(FiltroFormaPago.TODOS), pulsados)
    }

    // Escenario 5: periodo sin ningun ticket -> vacio original aunque haya filtro.
    @Test
    fun periodoSinTicketsMantieneElVacioOriginalConCualquierFiltro() {
        composeRule.setContent {
            TallyTheme {
                GastosContenido(
                    ui = GastosUiState(
                        cargando = false, tipo = PeriodoTipo.DIA, grupos = emptyList(),
                        filtro = FiltroFormaPago.EFECTIVO, hayTicketsEnPeriodo = false,
                    ),
                    onSeleccionarPeriodo = {},
                    onSeleccionarRango = { _, _ -> },
                    onTicketClick = {},
                    onEscanear = {},
                )
            }
        }
        composeRule.onNodeWithTag("filtro_pago_efectivo").assertIsSelected()
        composeRule.onNodeWithTag("listado_vacio").assertIsDisplayed()
        composeRule.onNodeWithTag("listado_vacio_filtro").assertDoesNotExist()
        composeRule.onNodeWithTag("boton_escanear_vacio").assertIsDisplayed()
    }

    // Escenario 8: 360 dp y letra a 2,0: los tres chips completos, >= 48 dp, sin recortes.
    @Test
    fun aTrescientosSesentaDpYLetraDosLosTresChipsSeVenCompletos() {
        composeRule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale = 2f)) {
                TallyTheme {
                    Box(Modifier.width(360.dp).fillMaxHeight()) {
                        GastosContenido(
                            ui = GastosUiState(cargando = false, tipo = PeriodoTipo.DIA, grupos = emptyList()),
                            onSeleccionarPeriodo = {},
                            onSeleccionarRango = { _, _ -> },
                            onTicketClick = {},
                            onEscanear = {},
                        )
                    }
                }
            }
        }
        listOf("filtro_pago_todos", "filtro_pago_efectivo", "filtro_pago_tarjeta").forEach {
            composeRule.onNodeWithTag(it).assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        }
    }
}

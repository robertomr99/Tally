package com.robertomr.tally.diseno

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.robertomr.tally.domain.expenses.entities.DisponibilidadMotor
import com.robertomr.tally.domain.expenses.entities.FiltroFormaPago
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.OfertaMotor
import com.robertomr.tally.domain.expenses.entities.PeriodoTipo
import com.robertomr.tally.feature.common.DockEdicion
import com.robertomr.tally.feature.common.PeriodoSelector
import com.robertomr.tally.feature.common.theme.TallyTheme
import com.robertomr.tally.feature.escanear.ExtraccionFallidaContent
import com.robertomr.tally.feature.gastos.GastosContenido
import com.robertomr.tally.feature.gastos.GastosUiState
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Fuente al doble en 360 dp (spec mejoras-visuales-3, escenarios 1, 2, 4 y 6): se pintan los
// composables con estados fijos y densidad con fontScale 2,0, sin ViewModel ni datos reales.
// El borde de 2 dp del segmento activo (W01) no es semantica y no tiene aserción: su
// evidencia es la captura de docs/evidence/mejoras-visuales-3/.
@RunWith(AndroidJUnit4::class)
class FuentesGrandesFlujoTest {

    @get:Rule
    val composeRule = createComposeRule()

    /** 360 dp de ancho (movil estrecho) y 720 dp de alto, con letra al doble. */
    private fun contenidoConLetraDoble(contenido: @Composable () -> Unit) {
        composeRule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale = 2f)) {
                TallyTheme {
                    Box(Modifier.width(360.dp).height(720.dp)) { contenido() }
                }
            }
        }
    }

    @Test
    fun selectorDePeriodoApiladoMuestraLosCuatroSegmentosYElActivoSeleccionado() {
        contenidoConLetraDoble {
            PeriodoSelector(
                seleccionado = PeriodoTipo.MES,
                rango = null,
                hoy = LocalDate.of(2026, 10, 22),
                onSeleccionarTipo = {},
                onSeleccionarRango = { _, _ -> },
            )
        }
        listOf("periodo_dia", "periodo_semana", "periodo_mes", "periodo_personalizado").forEach {
            composeRule.onNodeWithTag(it).assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        }
        composeRule.onNodeWithTag("periodo_mes").assertIsSelected()
    }

    @Test
    fun falloDeProcesadoMuestraLosBotonesCompletosYApilados() {
        contenidoConLetraDoble {
            ExtraccionFallidaContent(
                mensaje = "No se pudo conectar con el servicio de extracción. Comprueba tu conexión.",
                motor = Motor.NUBE,
                otro = OfertaMotor(Motor.LOCAL, DisponibilidadMotor.DISPONIBLE),
                hayAnterior = true,
                onReintentar = {},
                onReintentarOtro = {},
                onVolverALaFoto = {},
                onVolverAnterior = {},
            )
        }
        // Reintentar y Volver a la foto son la fila fija inferior; el resto vive en el cuerpo con scroll.
        listOf("procesado_reintentar", "procesado_volver_foto").forEach {
            composeRule.onNodeWithTag(it).assertIsDisplayed().assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(200.dp)
        }
        composeRule.onNodeWithTag("procesado_reintentar_otro").performScrollTo().assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(200.dp)
        composeRule.onNodeWithTag("procesado_volver_anterior").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("procesado_proceso").assertIsDisplayed()
    }

    @Test
    fun gastosVacioAlcanzaLosBotonesConUnSoloScroll() {
        contenidoConLetraDoble {
            GastosContenido(
                ui = GastosUiState(cargando = false, tipo = PeriodoTipo.DIA, grupos = emptyList()),
                onSeleccionarPeriodo = {},
                onSeleccionarRango = { _, _ -> },
                onTicketClick = {},
                onEscanear = {},
            )
        }
        composeRule.onNodeWithTag("periodo_dia").assertIsDisplayed()
        listOf("boton_escanear_vacio", "gastos_anadir_vacio").forEach {
            composeRule.onNodeWithTag(it).performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        }
    }

    @Test
    fun gastosVacioPorFiltroAlcanzaVerTodosConUnSoloScroll() {
        contenidoConLetraDoble {
            GastosContenido(
                ui = GastosUiState(
                    cargando = false, tipo = PeriodoTipo.DIA, grupos = emptyList(),
                    filtro = FiltroFormaPago.TARJETA, hayTicketsEnPeriodo = true,
                ),
                onSeleccionarPeriodo = {},
                onSeleccionarRango = { _, _ -> },
                onTicketClick = {},
                onEscanear = {},
            )
        }
        composeRule.onNodeWithTag("filtro_pago_tarjeta").assertIsSelected()
        composeRule.onNodeWithTag("filtro_vaciar").performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun dockSoloTieneDosBotonesYUsaElTextoDelLlamador() {
        contenidoConLetraDoble {
            DockEdicion(
                onDescartar = {},
                onGuardar = {},
                textoGuardar = "Guardar gasto",
            )
        }
        composeRule.onNodeWithTag("revision_total_recalculado").assertDoesNotExist()
        composeRule.onNodeWithTag("revision_guardar").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("Guardar gasto").assertIsDisplayed()
        composeRule.onNodeWithTag("revision_descartar").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun gastosMuestraAnadirGastoEnFilaPropiaConLetraDoble() {
        contenidoConLetraDoble {
            GastosContenido(
                ui = GastosUiState(cargando = false, tipo = PeriodoTipo.DIA, grupos = emptyList()),
                onSeleccionarPeriodo = {},
                onSeleccionarRango = { _, _ -> },
                onTicketClick = {},
                onEscanear = {},
            )
        }
        composeRule.onNodeWithTag("gastos_anadir").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("Añadir gasto").assertIsDisplayed()
    }
}

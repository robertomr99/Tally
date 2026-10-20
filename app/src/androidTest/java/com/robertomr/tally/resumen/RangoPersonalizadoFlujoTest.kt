package com.robertomr.tally.resumen

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.robertomr.tally.MainActivity
import com.robertomr.tally.di.TicketRepositoryEntryPoint
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.Periodo
import com.robertomr.tally.domain.expenses.entities.Ticket
import com.robertomr.tally.feature.common.etiquetaPeriodo
import com.robertomr.tally.feature.common.formatoImporte
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// E2E de rango-personalizado (spec, escenarios 1, 2 y 6) en emulador. El rango se
// marca a mano en el DateRangePicker real tocando las celdas de los dias (por
// texto del dia, primera coincidencia = mes en curso, que es el que abre el picker):
// del dia 1 del mes en curso a hoy. Se siembran con el repositorio real tickets en
// ambos bordes (dia 1 a las 00:05 y hoy a las 23:50, con captura distinta de la
// emision) y fuera (el dia anterior al rango y manana). Como otros E2E o el uso previo
// pueden haber dejado tickets, las expectativas son "linea base leida del repositorio
// + sembrado"; lo sembrado se borra en @After.
@RunWith(AndroidJUnit4::class)
class RangoPersonalizadoFlujoTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val repositorio = EntryPointAccessors
        .fromApplication(
            InstrumentationRegistry.getInstrumentation().targetContext.applicationContext,
            TicketRepositoryEntryPoint::class.java,
        )
        .ticketRepository()

    private val idsSembrados = mutableListOf<Long>()
    private val hoy: LocalDate = LocalDate.now()
    private val primero: LocalDate = hoy.withDayOfMonth(1)
    private val rango = Periodo.personalizado(primero, hoy)

    @After
    fun limpiarTicketsSembrados() {
        runBlocking { idsSembrados.forEach { repositorio.delete(it) } }
        idsSembrados.clear()
    }

    private fun sembrar(comercio: String, emision: LocalDateTime, total: Double, captura: LocalDate): Ticket {
        val borrador = Ticket(
            comercio = comercio,
            fechaEmision = emision,
            total = total,
            categoria = Categoria.OTROS,
            fechaCaptura = LocalDateTime.of(captura, LocalTime.NOON),
            motor = Motor.LOCAL,
            rutaFoto = "",
        )
        val id = runBlocking { repositorio.save(borrador) }
        idsSembrados += id
        return borrador.copy(id = id)
    }

    private fun existe(tag: String): Boolean =
        composeRule.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun esperarTag(tag: String) {
        composeRule.waitUntil(10_000) { existe(tag) }
    }

    private fun esperarTexto(tag: String, texto: String) {
        composeRule.waitUntil(10_000) {
            runCatching { composeRule.onNodeWithTag(tag, useUnmergedTree = true).assertTextEquals(texto) }.isSuccess
        }
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).assertTextEquals(texto)
    }

    private fun textoTickets(n: Int) = if (n == 1) "1 ticket" else "$n tickets"

    /**
     * Celdas de dia del picker: su texto accesible es "Thursday, October 1, 2026"
     * (depende del idioma del dispositivo), asi que se casa por el numero de dia
     * como token numerico suelto y por el anio, no por el nombre del mes. La
     * primera coincidencia es el mes en curso (el picker abre en el mes de hoy).
     */
    private fun esDiaDelPicker(dia: Int) = SemanticsMatcher("celda del dia $dia") { nodo ->
        val textos = nodo.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }
        val numero = Regex("(^|\\D)$dia(\\D|$)")
        nodo.config.contains(SemanticsActions.OnClick) &&
            textos.any { numero.containsMatchIn(it) && it.contains(hoy.year.toString()) }
    }

    private fun celdaDelDia(dia: Int): SemanticsNodeInteraction = composeRule.onAllNodes(esDiaDelPicker(dia))[0]

    /** Abre el picker desde el segmento y marca primero..hoy (sin confirmar). */
    private fun abrirPickerYMarcarRango() {
        composeRule.onNodeWithTag("periodo_personalizado").performClick()
        esperarTag("rango_dialogo")
        // Sin inicio ni fin, "Aplicar rango" esta deshabilitado.
        composeRule.onNodeWithTag("rango_aplicar").assertIsNotEnabled()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodes(esDiaDelPicker(primero.dayOfMonth)).fetchSemanticsNodes().isNotEmpty()
        }
        celdaDelDia(primero.dayOfMonth).performClick()
        celdaDelDia(hoy.dayOfMonth).performClick()
        composeRule.waitForIdle()
    }

    private fun aplicarRango() {
        composeRule.onNodeWithTag("rango_aplicar").assertIsEnabled().performClick()
        composeRule.waitUntil(10_000) { !existe("rango_dialogo") }
    }

    private fun baseDelRango(): List<Ticket> =
        runBlocking { repositorio.observeByDateRange(rango.desde, rango.hasta).first() }

    // Escenarios 1 y 6: marcar el rango en Resumen y en Gastos, filtra igual y se conserva al volver.
    @Test
    fun elRangoMarcadoAManoSeReflejaEnResumenYGastosYSeConservaAlVolver() {
        val base = baseDelRango()
        val inicio = sembrar("E2E rango inicio", primero.atTime(0, 5), 11.11, captura = hoy.minusDays(40))
        val fin = sembrar("E2E rango fin", hoy.atTime(23, 50), 22.22, captura = hoy.minusDays(40))
        val antes = sembrar("E2E rango antes", primero.minusDays(1).atTime(12, 0), 44.44, captura = hoy)
        val manana = sembrar("E2E rango manana", hoy.plusDays(1).atTime(12, 0), 55.55, captura = hoy)

        // Resumen: Mes activo por defecto; Personalizado abre el picker.
        composeRule.onNodeWithTag("periodo_mes").assertIsSelected()
        abrirPickerYMarcarRango()
        aplicarRango()

        composeRule.onNodeWithTag("periodo_personalizado").assertIsSelected()
        composeRule.onNodeWithTag("periodo_mes").assertIsNotSelected()
        val dentro = 2
        esperarTexto("resumen_rango", etiquetaPeriodo(rango, hoy))
        esperarTexto("resumen_num_tickets", textoTickets(base.size + dentro))
        esperarTexto("resumen_suma", formatoImporte(base.sumOf { it.total } + 11.11 + 22.22))

        // Gastos: seleccion independiente; se marca el mismo rango y se filtra igual.
        composeRule.onNodeWithTag("bottom_bar_gastos").performClick()
        esperarTag("pantalla_gastos")
        composeRule.onNodeWithTag("periodo_mes").assertIsSelected()
        abrirPickerYMarcarRango()
        aplicarRango()
        composeRule.onNodeWithTag("periodo_personalizado").assertIsSelected()
        esperarTexto("gastos_rango", etiquetaPeriodo(rango, hoy))
        esperarTag("lista_gastos")
        listOf(fin, inicio).forEach {
            composeRule.onNodeWithTag("lista_gastos").performScrollToNode(hasTestTag("ticket_${it.id}"))
            esperarTag("ticket_${it.id}")
        }
        assertTrue(!existe("ticket_${antes.id}"))
        assertTrue(!existe("ticket_${manana.id}"))

        // Volver a Resumen conserva el rango (estado en el ViewModel + saveState de la bottom bar).
        composeRule.onNodeWithTag("bottom_bar_resumen").performClick()
        esperarTag("pantalla_resumen")
        composeRule.onNodeWithTag("periodo_personalizado").assertIsSelected()
        esperarTexto("resumen_rango", etiquetaPeriodo(rango, hoy))
        esperarTexto("resumen_num_tickets", textoTickets(base.size + dentro))
    }

    // Escenarios 2 y 6: cancelar no cambia la seleccion; tras cambiar a Dia el picker se reabre precargado.
    @Test
    fun cancelarNoCambiaLaSeleccionYElPickerSeReabrePrecargado() {
        composeRule.onNodeWithTag("periodo_mes").assertIsSelected()
        composeRule.onNodeWithTag("periodo_personalizado").performClick()
        esperarTag("rango_dialogo")
        composeRule.onNodeWithTag("rango_aplicar").assertIsNotEnabled()
        composeRule.onNodeWithTag("rango_cancelar").performClick()
        composeRule.waitUntil(10_000) { !existe("rango_dialogo") }
        composeRule.onNodeWithTag("periodo_mes").assertIsSelected()
        composeRule.onNodeWithTag("periodo_personalizado").assertIsNotSelected()

        abrirPickerYMarcarRango()
        aplicarRango()
        composeRule.onNodeWithTag("periodo_dia").performClick()
        composeRule.onNodeWithTag("periodo_dia").assertIsSelected()
        composeRule.onNodeWithTag("periodo_personalizado").performClick()
        esperarTag("rango_dialogo")
        // Precargado con el ultimo rango: se puede confirmar sin volver a marcar nada.
        composeRule.onNodeWithTag("rango_aplicar").assertIsEnabled().performClick()
        composeRule.waitUntil(10_000) { !existe("rango_dialogo") }
        composeRule.onNodeWithTag("periodo_personalizado").assertIsSelected()
        esperarTexto("resumen_rango", etiquetaPeriodo(rango, hoy))
    }
}

package com.robertomr.tally.gastos

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.robertomr.tally.MainActivity
import com.robertomr.tally.di.TicketRepositoryEntryPoint
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.Periodo
import com.robertomr.tally.domain.expenses.entities.PeriodoTipo
import com.robertomr.tally.domain.expenses.entities.Ticket
import com.robertomr.tally.feature.common.formatoImporte
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// E2E de listado-gastos (spec, escenarios 1-7) en emulador. Se siembran con el
// repositorio real tickets de comercio unico hoy (y uno fuera de periodo); otros
// E2E o el uso previo pueden haber dejado datos, asi que las expectativas se
// calculan como "linea base leida del repositorio + sembrado" y lo sembrado se
// borra en @After.
@RunWith(AndroidJUnit4::class)
class ListadoGastosFlujoTest {

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

    @After
    fun limpiarTicketsSembrados() {
        runBlocking { idsSembrados.forEach { repositorio.delete(it) } }
        idsSembrados.clear()
    }

    private fun baseDe(periodo: Periodo): List<Ticket> =
        runBlocking { repositorio.observeByDateRange(periodo.desde, periodo.hasta).first() }

    private fun sembrar(comercio: String, hora: LocalTime, total: Double, fecha: LocalDate = hoy): Ticket {
        val emision = LocalDateTime.of(fecha, hora)
        val borrador = Ticket(
            comercio = comercio,
            fechaEmision = emision,
            total = total,
            categoria = Categoria.OTROS,
            fechaCaptura = emision,
            motor = Motor.LOCAL,
            rutaFoto = "",
        )
        val id = runBlocking { repositorio.save(borrador) }
        idsSembrados += id
        return borrador.copy(id = id)
    }

    /** Orden esperado calculado aqui: fecha de emision desc y, en empate, id desc. */
    private fun ordenEsperado(tickets: List<Ticket>): List<Ticket> =
        tickets.sortedWith(compareByDescending<Ticket> { it.fechaEmision }.thenByDescending { it.id })

    private fun centimosDelDia(tickets: List<Ticket>, dia: LocalDate): Long =
        tickets.filter { it.fechaEmision.toLocalDate() == dia && it.total.isFinite() && it.total > 0.0 }
            .sumOf { Math.round(it.total * 100) }

    private fun irAGastos() {
        composeRule.onNodeWithTag("bottom_bar_gastos").performClick()
        esperarTag("pantalla_gastos")
        composeRule.waitForIdle()
    }

    private fun esperarTag(tag: String) {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun existe(tag: String): Boolean =
        composeRule.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun esperarAusente(tag: String) {
        composeRule.waitUntil(10_000) { !existe(tag) }
    }

    private fun esperarTexto(tag: String, texto: String, sinFusionar: Boolean = true) {
        composeRule.waitUntil(10_000) {
            runCatching { composeRule.onNodeWithTag(tag, useUnmergedTree = sinFusionar).assertTextEquals(texto) }.isSuccess
        }
    }

    /** Desplaza la lista de Gastos hasta que el ticket este compuesto. */
    private fun mostrarEnGastos(id: Long) {
        composeRule.onNodeWithTag("lista_gastos").performScrollToNode(hasTestTag("ticket_$id"))
        esperarTag("ticket_$id")
    }

    /** Desplaza hasta la cabecera del dia y espera a que su total coincida con [centimos]. */
    private fun esperarTotalDelDia(dia: LocalDate, centimos: Long) {
        composeRule.onNodeWithTag("lista_gastos").performScrollToNode(hasTestTag("grupo_dia_$dia"))
        esperarTexto("grupo_dia_${dia}_total", "Total: " + formatoImporte(centimos / 100.0))
    }

    private fun topDe(tag: String): Float =
        composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.top

    // Escenarios 1-3: solo los tickets del periodo, orden descendente, total del dia y fila completa.
    @Test
    fun gastosListaExactamenteLosTicketsDelDiaEnOrdenYConTotalDelGrupo() {
        val dia = Periodo.de(PeriodoTipo.DIA, hoy)
        val base = baseDe(dia)
        val a = sembrar("E2E listado A", LocalTime.of(9, 15), 10.00)
        val b = sembrar("E2E listado B", LocalTime.of(12, 0), 20.00)
        val c = sembrar("E2E listado C", LocalTime.of(18, 40), 5.50)
        val fuera = sembrar("E2E listado fuera", LocalTime.NOON, 99.0, fecha = hoy.minusDays(40))

        irAGastos()
        composeRule.onNodeWithTag("periodo_dia").performClick()
        val esperados = ordenEsperado(base + listOf(a, b, c))
        esperarTag("grupo_dia_$hoy")

        // Todos los esperados existen (scroll a cada uno) y el de fuera de periodo no.
        esperados.forEach { mostrarEnGastos(it.id) }
        assertTrue(!existe("ticket_${fuera.id}"))

        // Contenido de las filas sembradas.
        listOf(a, b, c).forEach {
            mostrarEnGastos(it.id)
            composeRule.onNodeWithTag("ticket_${it.id}_comercio", useUnmergedTree = true).assertTextEquals(it.comercio)
            composeRule.onNodeWithTag("ticket_${it.id}_total", useUnmergedTree = true).assertTextEquals(formatoImporte(it.total))
            composeRule.onNodeWithTag("ticket_${it.id}_categoria", useUnmergedTree = true).assertTextEquals("Otros")
        }

        // Orden: C (18:40) por encima de B (12:00) por encima de A (09:15), si estan compuestos a la vez.
        composeRule.onNodeWithTag("lista_gastos").performScrollToNode(hasTestTag("ticket_${c.id}"))
        if (existe("ticket_${a.id}") && existe("ticket_${b.id}")) {
            assertTrue(topDe("ticket_${c.id}") < topDe("ticket_${b.id}"))
            assertTrue(topDe("ticket_${b.id}") < topDe("ticket_${a.id}"))
        }

        // Total del grupo del dia = suma independiente de la base mas lo sembrado.
        composeRule.onNodeWithTag("lista_gastos").performScrollToNode(hasTestTag("grupo_dia_$hoy"))
        esperarTexto("grupo_dia_${hoy}_total", "Total: " + formatoImporte(centimosDelDia(base + listOf(a, b, c), hoy) / 100.0))
    }

    // Escenario 4: reactividad al guardar y al alternar periodo.
    @Test
    fun guardarUnTicketLoAnadeSoloYAlternarPeriodoCambiaElContenido() {
        val dia = Periodo.de(PeriodoTipo.DIA, hoy)
        val base = baseDe(dia)
        irAGastos()
        composeRule.onNodeWithTag("periodo_dia").performClick()
        composeRule.waitForIdle()

        val nuevo = sembrar("E2E listado reactivo", LocalTime.of(10, 30), 7.25)
        esperarTag("grupo_dia_$hoy")
        mostrarEnGastos(nuevo.id)
        composeRule.onNodeWithTag("ticket_${nuevo.id}_comercio", useUnmergedTree = true).assertTextEquals(nuevo.comercio)
        esperarTotalDelDia(hoy, centimosDelDia(base + nuevo, hoy))

        // Un ticket de hace 40 dias no esta en Dia ni en Semana; el del dia si esta en Mes.
        val antiguo = sembrar("E2E listado antiguo", LocalTime.NOON, 3.0, fecha = hoy.minusDays(40))
        composeRule.waitForIdle()
        assertTrue(!existe("ticket_${antiguo.id}"))
        composeRule.onNodeWithTag("periodo_semana").performClick()
        composeRule.waitForIdle()
        assertTrue(!existe("ticket_${antiguo.id}"))
        composeRule.onNodeWithTag("periodo_mes").performClick()
        esperarTag("lista_gastos")
        mostrarEnGastos(nuevo.id)
    }

    // D4: un ticket con total <= 0 aparece en la lista pero no suma al total del dia.
    @Test
    fun ticketConTotalCeroApareceEnLaListaPeroNoSumaAlDia() {
        val dia = Periodo.de(PeriodoTipo.DIA, hoy)
        val base = baseDe(dia)
        val cero = sembrar("E2E listado cero", LocalTime.of(8, 5), 0.0)
        val normal = sembrar("E2E listado normal", LocalTime.of(8, 10), 6.0)

        irAGastos()
        composeRule.onNodeWithTag("periodo_dia").performClick()
        mostrarEnGastos(cero.id)
        composeRule.onNodeWithTag("ticket_${cero.id}_total", useUnmergedTree = true).assertTextEquals(formatoImporte(0.0))
        mostrarEnGastos(normal.id)
        esperarTotalDelDia(hoy, centimosDelDia(base + listOf(cero, normal), hoy))
    }

    // Escenario 6: tocar una fila abre ticket/{id} y Atras vuelve (Gastos y Resumen).
    @Test
    fun tocarUnaFilaAbreElDetalleYAtrasVuelve() {
        val t = sembrar("E2E listado tocar", LocalTime.of(23, 59), 4.0)

        // Desde Gastos.
        irAGastos()
        composeRule.onNodeWithTag("periodo_dia").performClick()
        esperarTag("ticket_${t.id}")
        mostrarEnGastos(t.id)
        composeRule.onNodeWithTag("ticket_${t.id}").performClick()
        esperarTag("pantalla_detalle_ticket")
        esperarTag("detalle_ticket_${t.id}")
        volverAtras()
        esperarTag("pantalla_gastos")
        composeRule.onNodeWithTag("pantalla_gastos").assertIsDisplayed()

        // Desde Resumen (vista previa): el mas reciente del dia entra en los 5 primeros.
        composeRule.onNodeWithTag("bottom_bar_resumen").performClick()
        esperarTag("pantalla_resumen")
        composeRule.waitForIdle()
        esperarTag("resumen_ticket_${t.id}")
        composeRule.onNodeWithTag("resumen_ticket_${t.id}").performScrollTo().performClick()
        esperarTag("pantalla_detalle_ticket")
        esperarTag("detalle_ticket_${t.id}")
        volverAtras()
        esperarTag("pantalla_resumen")
        composeRule.onNodeWithTag("pantalla_resumen").assertIsDisplayed()
    }

    // Escenario 7: Resumen limita a 5 filas y "Ver todos (N)" lleva a Gastos.
    @Test
    fun resumenMuestraCincoFilasYVerTodosLlevaAGastos() {
        val mes = Periodo.de(PeriodoTipo.MES, hoy)
        val base = baseDe(mes)
        val sembrados = (1..6).map { sembrar("E2E listado prev $it", LocalTime.of(23, 59, it), it.toDouble()) }
        val total = base.size + sembrados.size

        esperarTag("resumen_recientes")
        esperarTag("boton_ver_todos")
        val filas = SemanticsMatcher("fila de resumen") { nodo ->
            nodo.config.getOrNull(SemanticsProperties.TestTag)?.matches(Regex("resumen_ticket_\\d+")) == true
        }
        composeRule.waitUntil(10_000) { composeRule.onAllNodes(filas).fetchSemanticsNodes().size == 5 }
        assertEquals(5, composeRule.onAllNodes(filas).fetchSemanticsNodes().size)
        esperarTexto("boton_ver_todos", "Ver todos ($total)", sinFusionar = false)
        composeRule.onNodeWithTag("boton_ver_todos").performScrollTo()

        composeRule.onNodeWithTag("boton_ver_todos").performClick()
        esperarTag("pantalla_gastos")
        composeRule.onNodeWithTag("pantalla_gastos").assertIsDisplayed()
    }

    // Escenario 5: estado vacio coherente en Gastos y Resumen (solo si hoy no hay tickets).
    @Test
    fun periodoSinTicketsMuestraElVacioEnGastosYEnResumen() {
        val dia = Periodo.de(PeriodoTipo.DIA, hoy)
        // Si la linea base de hoy no esta vacia el vacio queda cubierto en JVM (GastosViewModelTest).
        assumeTrue(baseDe(dia).isEmpty())

        composeRule.onNodeWithTag("periodo_dia").performClick()
        esperarTag("resumen_recientes_vacio")
        composeRule.onNodeWithTag("resumen_recientes_vacio", useUnmergedTree = true)
            .assertTextEquals("Sin tickets en este periodo.")
        esperarTag("donut_vacio")

        irAGastos()
        composeRule.onNodeWithTag("periodo_dia").performClick()
        esperarTag("listado_vacio")
        composeRule.onNodeWithTag("boton_escanear_vacio").assertIsDisplayed()
        composeRule.onNodeWithTag("boton_escanear_vacio").performClick()
        esperarTag("pantalla_escanear")
    }

    private fun volverAtras() {
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }
}

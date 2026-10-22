package com.robertomr.tally.gastos

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.robertomr.tally.MainActivity
import com.robertomr.tally.di.TicketRepositoryEntryPoint
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.FormaPago
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.Ticket
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// E2E de filtro-forma-pago (spec, escenarios 1, 2, 3, 6, 7 y 8) en emulador. El test
// crea su propia semilla (3 tickets de hoy, comercio unico, uno por forma de pago:
// EFECTIVO, TARJETA y NO_INDICADA) con el repositorio real y la borra en @After.
// Solo se afirma sobre los tags `ticket_<id>` de la semilla: el emulador tiene
// tickets residuales y nunca se afirman totales globales. La semilla va a las 23:58,
// la primera fila del dia de hoy, asi que queda dentro de la ventana de la lista.
// Los vacios por filtro (escenarios 4 y 5) estan en FiltroFormaPagoVacioTest.
@RunWith(AndroidJUnit4::class)
class FiltroFormaPagoFlujoTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext

    private val repositorio = EntryPointAccessors
        .fromApplication(contexto.applicationContext, TicketRepositoryEntryPoint::class.java)
        .ticketRepository()

    private val idsSembrados = mutableListOf<Long>()
    private val hoy: LocalDate = LocalDate.now()
    private val comercio = "E2E filtro ${UUID.randomUUID()}"

    private lateinit var efectivo: Ticket
    private lateinit var tarjeta: Ticket
    private lateinit var sinIndicar: Ticket

    @Before
    fun sembrarSemilla() {
        efectivo = sembrar(FormaPago.EFECTIVO)
        tarjeta = sembrar(FormaPago.TARJETA)
        sinIndicar = sembrar(FormaPago.NO_INDICADA)
    }

    @After
    fun limpiar() {
        runBlocking { idsSembrados.forEach { repositorio.delete(it) } }
        idsSembrados.clear()
    }

    private fun sembrar(forma: FormaPago): Ticket {
        val emision = LocalDateTime.of(hoy, LocalTime.of(23, 58, 7))
        val borrador = Ticket(
            comercio = comercio,
            fechaEmision = emision,
            total = 13.90,
            categoria = Categoria.ALIMENTACION,
            fechaCaptura = emision.minusMinutes(5),
            motor = Motor.NUBE,
            rutaFoto = "fotos/no_existe_e2e_filtro.jpg",
            formaPago = forma,
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

    private fun esperarAusente(tag: String) {
        composeRule.waitUntil(10_000) { !existe(tag) }
    }

    private fun textoDe(tag: String): String =
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()
            .config[SemanticsProperties.Text].joinToString { it.text }

    private fun irAGastos(periodo: String = "periodo_dia") {
        composeRule.onNodeWithTag("bottom_bar_gastos").performClick()
        esperarTag("pantalla_gastos")
        composeRule.onNodeWithTag(periodo).performClick()
        composeRule.waitForIdle()
    }

    private fun filtrar(chip: String) {
        composeRule.onNodeWithTag(chip).performClick()
        composeRule.waitForIdle()
    }

    /** Solo los tickets de la semilla indicados estan en pantalla (tras desplazar a los esperados). */
    private fun assertVisibles(vararg esperados: Ticket) {
        val todos = listOf(efectivo, tarjeta, sinIndicar)
        esperados.forEach { t ->
            composeRule.onNodeWithTag("lista_gastos").performScrollToNode(hasTestTag("ticket_${t.id}"))
            esperarTag("ticket_${t.id}")
        }
        todos.filter { it !in esperados }.forEach { t -> esperarAusente("ticket_${t.id}") }
    }

    private fun assertSeleccionado(chip: String) {
        listOf("filtro_pago_todos", "filtro_pago_efectivo", "filtro_pago_tarjeta").forEach { tag ->
            if (tag == chip) {
                composeRule.onNodeWithTag(tag).assertIsSelected()
            } else {
                composeRule.onNodeWithTag(tag).assertIsNotSelected()
            }
        }
    }

    // Escenarios 1, 2 y 8: tarjeta, efectivo (sin NO_INDICADA) y todos; chips >= 48 dp.
    @Test
    fun filtrarPorTarjetaEfectivoYTodosMuestraLosTicketsEsperados() {
        irAGastos()
        esperarTag("filtro_pago")
        assertSeleccionado("filtro_pago_todos")
        listOf("filtro_pago_todos", "filtro_pago_efectivo", "filtro_pago_tarjeta").forEach {
            composeRule.onNodeWithTag(it).assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        }
        assertVisibles(efectivo, tarjeta, sinIndicar)

        filtrar("filtro_pago_tarjeta")
        assertSeleccionado("filtro_pago_tarjeta")
        assertVisibles(tarjeta)

        filtrar("filtro_pago_efectivo")
        assertSeleccionado("filtro_pago_efectivo")
        assertVisibles(efectivo)

        filtrar("filtro_pago_todos")
        assertSeleccionado("filtro_pago_todos")
        assertVisibles(efectivo, tarjeta, sinIndicar)
    }

    // Escenario 3: cambiar de periodo no resetea el filtro; el listado es la interseccion.
    @Test
    fun elFiltroSeConservaAlCambiarDePeriodo() {
        irAGastos()
        filtrar("filtro_pago_tarjeta")
        assertVisibles(tarjeta)

        for (periodo in listOf("periodo_semana", "periodo_mes", "periodo_dia")) {
            composeRule.onNodeWithTag(periodo).performClick()
            composeRule.waitForIdle()
            assertSeleccionado("filtro_pago_tarjeta")
            assertVisibles(tarjeta)
        }
        filtrar("filtro_pago_todos")
        assertVisibles(efectivo, tarjeta, sinIndicar)
    }

    // Escenario 6: detalle, Nuevo gasto y la barra inferior conservan el filtro.
    @Test
    fun elFiltroSobreviveADetalleNuevoGastoYBarraInferior() {
        irAGastos()
        filtrar("filtro_pago_efectivo")
        assertVisibles(efectivo)

        // Detalle y vuelta con Back del sistema.
        composeRule.onNodeWithTag("ticket_${efectivo.id}").performClick()
        esperarTag("detalle_ticket_${efectivo.id}")
        volverAtras()
        esperarTag("pantalla_gastos")
        assertSeleccionado("filtro_pago_efectivo")
        assertVisibles(efectivo)

        // Nuevo gasto cancelado con Back.
        composeRule.onNodeWithTag("gastos_anadir").performClick()
        esperarTag("pantalla_nuevo_gasto")
        volverAtras()
        esperarTag("pantalla_gastos")
        assertSeleccionado("filtro_pago_efectivo")
        assertVisibles(efectivo)

        // Ida y vuelta por la barra inferior (saveState / restoreState).
        composeRule.onNodeWithTag("bottom_bar_resumen").performClick()
        esperarTag("pantalla_resumen")
        composeRule.onNodeWithTag("bottom_bar_gastos").performClick()
        esperarTag("pantalla_gastos")
        assertSeleccionado("filtro_pago_efectivo")
        assertVisibles(efectivo)
    }

    // Escenario 7: el filtro de Gastos no cambia el total de Resumen.
    @Test
    fun resumenNoSeVeAfectadoPorElFiltro() {
        composeRule.onNodeWithTag("bottom_bar_resumen").performClick()
        esperarTag("resumen_suma")
        composeRule.waitForIdle()
        val antes = textoDe("resumen_suma")

        irAGastos("periodo_mes")
        filtrar("filtro_pago_tarjeta")
        assertVisibles(tarjeta)

        composeRule.onNodeWithTag("bottom_bar_resumen").performClick()
        esperarTag("resumen_suma")
        composeRule.waitForIdle()
        assertEquals(antes, textoDe("resumen_suma"))
    }

    private fun volverAtras() {
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }
}

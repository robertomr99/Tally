package com.robertomr.tally.gastos

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.robertomr.tally.MainActivity
import com.robertomr.tally.di.TicketRepositoryEntryPoint
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.FormaPago
import com.robertomr.tally.domain.expenses.entities.Linea
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.Ticket
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// E2E de gasto-manual-2 (escenarios 1, 2, 4, 5 y 7) en emulador: total unico automatico y
// cambio derivado, ambos de solo lectura; aviso de diferencia y "Usar la suma" en el Detalle de
// un ticket sembrado. Comercio UNICO por test; @After borra por repositorio todo lo que haya
// con ese comercio (sembrado o creado por la UI), aunque el test falle a medias.
@RunWith(AndroidJUnit4::class)
class TotalCambioAutomaticosFlujoTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext

    private val repositorio = EntryPointAccessors
        .fromApplication(contexto.applicationContext, TicketRepositoryEntryPoint::class.java)
        .ticketRepository()

    private val hoy: LocalDate = LocalDate.now()
    private val comercio = "E2E total ${UUID.randomUUID()}"

    @After
    fun limpiar() {
        runBlocking { encontrar().forEach { repositorio.delete(it.id) } }
    }

    private fun encontrar(): List<Ticket> = runBlocking {
        repositorio.observeByDateRange(hoy.minusDays(40), hoy.plusDays(40)).first()
            .filter { it.comercio == comercio }
    }

    private fun sembrar(
        total: Double,
        lineas: List<Linea>,
        forma: FormaPago = FormaPago.NO_INDICADA,
        entregado: Double? = null,
        cambio: Double? = null,
    ): Ticket {
        val emision = LocalDateTime.of(hoy, LocalTime.of(23, 57, 11))
        val borrador = Ticket(
            comercio = comercio,
            fechaEmision = emision,
            total = total,
            categoria = Categoria.ALIMENTACION,
            fechaCaptura = emision.minusMinutes(5),
            motor = Motor.NUBE,
            rutaFoto = "fotos/no_existe_e2e_total.jpg",
            lineas = lineas,
            formaPago = forma,
            entregado = entregado,
            cambio = cambio,
        )
        val id = runBlocking { repositorio.save(borrador) }
        return borrador.copy(id = id)
    }

    private fun existe(tag: String): Boolean =
        composeRule.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun esperarTag(tag: String) {
        composeRule.waitUntil(10_000) { existe(tag) }
    }

    private fun texto(tag: String): String =
        composeRule.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.EditableText].text

    private fun reemplazar(tag: String, valor: String) {
        composeRule.onNodeWithTag(tag).performScrollTo()
        composeRule.onNodeWithTag(tag).performTextReplacement(valor)
    }

    private val soloLectura = SemanticsMatcher.keyNotDefined(SemanticsActions.SetText)
    private val editable = SemanticsMatcher.keyIsDefined(SemanticsActions.SetText)

    private fun irANuevoGasto() {
        composeRule.onNodeWithTag("bottom_bar_gastos").performClick()
        esperarTag("pantalla_gastos")
        composeRule.onNodeWithTag("periodo_dia").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("gastos_anadir").performClick()
        esperarTag("pantalla_nuevo_gasto")
    }

    private fun abrirDetalle(id: Long) {
        composeRule.onNodeWithTag("bottom_bar_gastos").performClick()
        esperarTag("pantalla_gastos")
        composeRule.onNodeWithTag("periodo_dia").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("lista_gastos").performScrollToNode(hasTestTag("ticket_$id"))
        esperarTag("ticket_$id")
        composeRule.onNodeWithTag("ticket_$id").performClick()
        esperarTag("detalle_ticket_$id")
    }

    // Escenarios 4 y 1: dos lineas dan el total solo; el cambio sale de entregado y total; ambos
    // son de solo lectura; guardar persiste el total mostrado y el cambio calculado.
    @Test
    fun nuevoGastoSumaLasLineasYCalculaElCambioSinPoderEditarlos() {
        irANuevoGasto()
        reemplazar("revision_comercio", comercio)

        composeRule.onNodeWithTag("revision_anadir_linea").performScrollTo().performClick()
        composeRule.onNodeWithTag("revision_anadir_linea").performScrollTo().performClick()
        composeRule.waitForIdle()
        reemplazar("revision_linea_0_concepto", "PAN")
        reemplazar("revision_linea_0_cantidad", "3")
        reemplazar("revision_linea_0_precio", "1,50")
        reemplazar("revision_linea_1_concepto", "AGUA")
        reemplazar("revision_linea_1_precio", "2")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("revision_total").performScrollTo()
            .assertTextContains("6,50", substring = true).assert(soloLectura)
        composeRule.onNodeWithTag("revision_total_manual").performScrollTo().assertIsDisplayed()
        assertFalse(existe("revision_aviso_total"))
        assertFalse(existe("revision_total_recalculado"))

        composeRule.onNodeWithTag("revision_forma_pago_efectivo").performScrollTo().performClick()
        esperarTag("revision_entregado")
        reemplazar("revision_entregado", "10")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("revision_cambio").performScrollTo().assert(soloLectura)
        assertEquals("3,50", texto("revision_cambio"))

        // Cambiar una cantidad mueve total y cambio.
        reemplazar("revision_linea_0_cantidad", "2")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("revision_total").performScrollTo().assertTextContains("5,00", substring = true)
        assertEquals("5,00", texto("revision_cambio"))

        composeRule.onNodeWithTag("revision_guardar").performClick()
        esperarTag("pantalla_gastos")
        val guardado = encontrar().single()
        assertEquals(5.0, guardado.total, 0.0)
        assertEquals(FormaPago.EFECTIVO, guardado.formaPago)
        assertEquals(10.0, guardado.entregado!!, 0.0)
        assertEquals(5.0, guardado.cambio!!, 0.0)
        assertEquals(listOf(Linea("PAN", 2, 1.50), Linea("AGUA", 1, 2.0)), guardado.lineas)
    }

    // Escenario 5 (y 6 en el Detalle): total 10 con lineas que suman 3,90 se conserva, editable y
    // con aviso de diferencia; "Usar la suma" lo pasa a automatico y "Editar total a mano" lo
    // devuelve a editable; guardar persiste el total mostrado.
    @Test
    fun detalleConLineasQueNoCuadranAvisaYUsarLaSumaLoPasaAAutomatico() {
        val t = sembrar(10.0, listOf(Linea("A", 1, 3.90)))
        abrirDetalle(t.id)

        composeRule.onNodeWithTag("revision_total").performScrollTo().assert(editable)
        assertEquals("10,00", texto("revision_total"))
        composeRule.onNodeWithTag("revision_aviso_total").performScrollTo().assertIsDisplayed()
            .assertTextContains("diferencia 6,10", substring = true)
        assertFalse(existe("revision_total_manual"))

        composeRule.onNodeWithTag("revision_total_usar_suma").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("revision_total").performScrollTo()
            .assertTextContains("3,90", substring = true).assert(soloLectura)
        assertFalse(existe("revision_aviso_total"))

        composeRule.onNodeWithTag("revision_total_manual").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("revision_total").performScrollTo().assert(editable)
        assertEquals("3,90", texto("revision_total"))

        composeRule.onNodeWithTag("revision_guardar").performClick()
        esperarTag("pantalla_gastos")
        assertEquals(3.90, runBlocking { repositorio.getById(t.id) }!!.total, 0.0)
    }

    // Escenarios 7 y 2: en un ticket cuyo total cuadra, cambiar una cantidad actualiza el total
    // solo; el cambio de un ticket en efectivo se abre calculado aunque lo guardado fuera otro.
    @Test
    fun detalleQueCuadraSigueLasEdicionesYMuestraElCambioCalculado() {
        val t = sembrar(3.90, listOf(Linea("A", 1, 3.90)), FormaPago.EFECTIVO, entregado = 5.0, cambio = 0.50)
        abrirDetalle(t.id)

        composeRule.onNodeWithTag("revision_total").performScrollTo()
            .assertTextContains("3,90", substring = true).assert(soloLectura)
        composeRule.onNodeWithTag("revision_cambio").performScrollTo().assert(soloLectura)
        assertEquals("1,10", texto("revision_cambio"))

        reemplazar("revision_linea_0_cantidad", "2")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("revision_total").performScrollTo().assertTextContains("7,80", substring = true)
        // Entregado 5 no cubre 7,80: cambio vacio y aviso con el faltante.
        assertEquals("", texto("revision_cambio"))
        composeRule.onNodeWithTag("revision_aviso_pago").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("revision_guardar").performClick()
        esperarTag("pantalla_gastos")
        val guardado = runBlocking { repositorio.getById(t.id) }!!
        assertEquals(7.80, guardado.total, 0.0)
        assertNull(guardado.cambio)
        assertTrue(guardado.lineas.single().cantidad == 2)
    }
}

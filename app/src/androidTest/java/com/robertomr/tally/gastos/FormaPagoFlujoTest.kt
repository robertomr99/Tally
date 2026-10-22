package com.robertomr.tally.gastos

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// E2E de forma-pago (spec, escenarios 1, 3, 5 y 6) en emulador: chips del
// listado, selector y campos Entregado/Cambio del detalle, aviso no bloqueante
// y persistencia (la tarjeta limpia entregado y cambio al guardar). Tickets de
// comercio UNICO sembrados con el repositorio real y borrados en @After.
@RunWith(AndroidJUnit4::class)
class FormaPagoFlujoTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext

    private val repositorio = EntryPointAccessors
        .fromApplication(contexto.applicationContext, TicketRepositoryEntryPoint::class.java)
        .ticketRepository()

    private val idsSembrados = mutableListOf<Long>()
    private val hoy: LocalDate = LocalDate.now()

    @After
    fun limpiar() {
        runBlocking { idsSembrados.forEach { repositorio.delete(it) } }
        idsSembrados.clear()
    }

    private fun sembrar(
        forma: FormaPago,
        entregado: Double? = null,
        cambio: Double? = null,
        total: Double = 13.90,
    ): Ticket {
        val emision = LocalDateTime.of(hoy, LocalTime.of(23, 58, 7))
        val borrador = Ticket(
            comercio = "E2E pago ${UUID.randomUUID()}",
            fechaEmision = emision,
            total = total,
            categoria = Categoria.ALIMENTACION,
            fechaCaptura = emision.minusMinutes(5),
            motor = Motor.NUBE,
            rutaFoto = "fotos/no_existe_e2e_pago.jpg",
            formaPago = forma,
            entregado = entregado,
            cambio = cambio,
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

    private fun texto(tag: String): String =
        composeRule.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.EditableText].text

    private fun irAGastosDia() {
        composeRule.onNodeWithTag("bottom_bar_gastos").performClick()
        esperarTag("pantalla_gastos")
        composeRule.onNodeWithTag("periodo_dia").performClick()
        composeRule.waitForIdle()
    }

    private fun abrirDesdeGastos(id: Long) {
        composeRule.onNodeWithTag("lista_gastos").performScrollToNode(hasTestTag("ticket_$id"))
        esperarTag("ticket_$id")
        composeRule.onNodeWithTag("ticket_$id").performClick()
        esperarTag("detalle_ticket_$id")
    }

    private fun reemplazar(tag: String, valor: String) {
        composeRule.onNodeWithTag(tag).performScrollTo()
        composeRule.onNodeWithTag(tag).performTextReplacement(valor)
    }

    private fun elegirForma(sufijo: String) {
        composeRule.onNodeWithTag("revision_forma_pago_$sufijo").performScrollTo().performClick()
        composeRule.waitForIdle()
    }

    // Escenarios 1 y 3: el listado distingue efectivo y tarjeta con chip; no indicada
    // no lo tiene; el importe de la fila sigue siendo el total (nunca entregado).
    @Test
    fun listadoMuestraChipDeEfectivoYTarjetaYNadaSiNoIndicadaConElTotalIntacto() {
        val efectivo = sembrar(FormaPago.EFECTIVO, entregado = 50.0, cambio = 36.10)
        val tarjeta = sembrar(FormaPago.TARJETA)
        val sinDato = sembrar(FormaPago.NO_INDICADA)

        irAGastosDia()
        listOf(efectivo, tarjeta, sinDato).forEach {
            composeRule.onNodeWithTag("lista_gastos").performScrollToNode(hasTestTag("ticket_${it.id}"))
            esperarTag("ticket_${it.id}")
        }
        assertTrue(existe("ticket_${efectivo.id}_forma_pago"))
        assertTrue(existe("ticket_${tarjeta.id}_forma_pago"))
        assertFalse(existe("ticket_${sinDato.id}_forma_pago"))
        val importe = composeRule.onNodeWithTag("ticket_${efectivo.id}_total", useUnmergedTree = true)
            .fetchSemanticsNode().config[SemanticsProperties.Text].joinToString { it.text }
        assertTrue("el importe debe ser el total (13,90 €), no el entregado: $importe", importe.contains("13,90"))
    }

    // Escenarios 5 y 3: el detalle muestra la forma y los campos del efectivo; al elegir
    // tarjeta se ocultan (y reaparecen al volver); guardar tarjeta persiste null.
    @Test
    fun detalleOcultaCamposAlElegirTarjetaYGuardarLosLimpia() {
        val t = sembrar(FormaPago.EFECTIVO, entregado = 50.0, cambio = 36.10)

        irAGastosDia()
        abrirDesdeGastos(t.id)
        esperarTag("detalle_forma_pago")
        composeRule.onNodeWithTag("revision_forma_pago_efectivo").performScrollTo().assertIsSelected()
        assertEquals("50,00", texto("revision_entregado"))
        assertEquals("36,10", texto("revision_cambio"))

        elegirForma("tarjeta")
        composeRule.onNodeWithTag("revision_forma_pago_tarjeta").assertIsSelected()
        assertFalse(existe("revision_entregado"))
        assertFalse(existe("revision_cambio"))

        // Volver a efectivo antes de guardar recupera los valores.
        elegirForma("efectivo")
        assertEquals("50,00", texto("revision_entregado"))
        assertEquals("36,10", texto("revision_cambio"))

        elegirForma("tarjeta")
        composeRule.onNodeWithTag("revision_guardar").performClick()
        esperarTag("pantalla_gastos")
        val guardado = runBlocking { repositorio.getById(t.id) }!!
        assertEquals(FormaPago.TARJETA, guardado.formaPago)
        assertNull(guardado.entregado)
        assertNull(guardado.cambio)
        assertEquals(13.90, guardado.total, 0.0)
    }

    // Escenario 6: entregado calcula el cambio; un cambio que no cuadra avisa pero no bloquea.
    @Test
    fun entregadoCalculaElCambioYElAvisoNoBloqueaElGuardado() {
        val t = sembrar(FormaPago.NO_INDICADA)

        irAGastosDia()
        abrirDesdeGastos(t.id)
        assertFalse(existe("detalle_forma_pago"))
        assertFalse(existe("revision_entregado"))

        elegirForma("efectivo")
        esperarTag("revision_entregado")
        reemplazar("revision_entregado", "50")
        composeRule.waitForIdle()
        assertEquals("36,10", texto("revision_cambio"))
        assertFalse(existe("revision_aviso_pago"))

        // spec gasto-manual-2: el cambio es derivado y de solo lectura (no se puede teclear).
        composeRule.onNodeWithTag("revision_cambio").assert(SemanticsMatcher.keyNotDefined(SemanticsActions.SetText))

        // Entregado menor que el total: cambio vacio y aviso con el faltante (13,90 - 5 = 8,90).
        reemplazar("revision_entregado", "5")
        composeRule.waitForIdle()
        assertEquals("", texto("revision_cambio"))
        composeRule.onNodeWithTag("revision_aviso_pago").performScrollTo().assertIsDisplayed()
            .assertTextContains("faltan 8,90 €", substring = true)

        // El aviso no bloquea: Guardar sigue activo y persiste entregado 5 y cambio null.
        composeRule.onNodeWithTag("revision_guardar").performClick()
        esperarTag("pantalla_gastos")
        val guardado = runBlocking { repositorio.getById(t.id) }!!
        assertEquals(FormaPago.EFECTIVO, guardado.formaPago)
        assertEquals(5.0, guardado.entregado!!, 0.0)
        assertNull(guardado.cambio)
        assertEquals(13.90, guardado.total, 0.0)
    }
}

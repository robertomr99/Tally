package com.robertomr.tally.gastos

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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
import com.robertomr.tally.di.TextEngineEntryPoint
import com.robertomr.tally.di.TicketRepositoryEntryPoint
import com.robertomr.tally.domain.expenses.data.RegistroExtracciones
import com.robertomr.tally.domain.expenses.entities.Ticket
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// E2E de gasto-manual (spec, escenarios 1, 3, 4, 5, 6 y 7) en emulador. El gasto
// se crea POR LA UI con un comercio UNICO (el emulador tiene tickets residuales:
// no se afirman totales globales) y @After borra por repositorio todo gasto con
// ese comercio, aunque el test falle a medias. El log de extracciones se compara
// con su linea base (no se vacia ni se toca).
@RunWith(AndroidJUnit4::class)
class GastoManualFlujoTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext

    private val repositorio = EntryPointAccessors
        .fromApplication(contexto.applicationContext, TicketRepositoryEntryPoint::class.java)
        .ticketRepository()

    private val registro: RegistroExtracciones = EntryPointAccessors
        .fromApplication(contexto.applicationContext, TextEngineEntryPoint::class.java)
        .registroExtracciones()

    private val hoy: LocalDate = LocalDate.now()
    private val formatoFecha = DateTimeFormatter.ofPattern("d/M/yyyy")
    private val comercio = "E2E manual ${UUID.randomUUID()}"
    private var lineaBaseLog = 0

    @Before
    fun medirLog() {
        lineaBaseLog = filasLog()
    }

    @After
    fun limpiar() {
        runBlocking { encontrar().forEach { repositorio.delete(it.id) } }
    }

    private fun filasLog(): Int =
        runBlocking { registro.observarRecientes(RegistroExtracciones.MAXIMO_FILAS).first() }.size

    /** Gastos (en +-40 dias de hoy) con el comercio unico de este test. */
    private fun encontrar(): List<Ticket> = runBlocking {
        repositorio.observeByDateRange(hoy.minusDays(40), hoy.plusDays(40)).first()
            .filter { it.comercio == comercio }
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

    private fun reemplazar(tag: String, valor: String) {
        composeRule.onNodeWithTag(tag).performScrollTo()
        composeRule.onNodeWithTag(tag).performTextReplacement(valor)
    }

    private fun volverAtras() {
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }

    private fun irANuevoGasto() {
        composeRule.onNodeWithTag("bottom_bar_gastos").performClick()
        esperarTag("pantalla_gastos")
        composeRule.onNodeWithTag("periodo_dia").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("gastos_anadir").performClick()
        esperarTag("pantalla_nuevo_gasto")
    }

    // Escenarios 1, 4 y 5: alta minima por la UI, detalle sin foto ni IA, edicion y borrado.
    @Test
    fun altaDetalleEdicionYBorradoDeUnGastoManual() {
        irANuevoGasto()
        // La barra inferior se oculta en Nuevo gasto, como en el detalle.
        assertFalse(existe("bottom_bar_gastos"))
        // Sin foto ni badge de IA en el alta.
        assertFalse(existe("revision_badge_motor"))

        reemplazar("revision_comercio", comercio)
        reemplazar("revision_total", "12,5")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("revision_guardar").assertIsEnabled()
        assertFalse(existe("nuevo_gasto_requisito"))
        composeRule.onNodeWithTag("revision_guardar").performClick()

        // Vuelve a Gastos con un aviso breve que no depende de la lista.
        esperarTag("pantalla_gastos")
        esperarTag("gasto_guardado_aviso")
        assertTrue(existe("bottom_bar_gastos"))
        val guardado = encontrar().single()
        assertEquals(12.5, guardado.total, 0.0)
        assertNull(guardado.motor)
        assertEquals("", guardado.rutaFoto)
        assertEquals(hoy, guardado.fechaEmision.toLocalDate())

        // Aparece en el listado de hoy (periodo Dia activo).
        composeRule.onNodeWithTag("lista_gastos").performScrollToNode(hasTestTag("ticket_${guardado.id}"))
        esperarTag("ticket_${guardado.id}")
        composeRule.onNodeWithTag("ticket_${guardado.id}").performClick()
        esperarTag("detalle_ticket_${guardado.id}")

        // Escenario 4: "Sin foto" y chip "Manual"; nada de IA ni de "Foto no disponible".
        esperarTag("detalle_sin_foto")
        assertTrue(existe("detalle_manual"))
        assertFalse(existe("revision_badge_motor"))
        assertFalse(existe("detalle_foto_ausente"))
        assertFalse(existe("detalle_ver_foto"))

        // Escenario 5: editar el total conserva motor y foto vacios.
        reemplazar("revision_total", "20")
        composeRule.onNodeWithTag("revision_guardar").assertIsEnabled().performClick()
        esperarTag("pantalla_gastos")
        val editado = encontrar().single()
        assertEquals(guardado.id, editado.id)
        assertEquals(20.0, editado.total, 0.0)
        assertNull(editado.motor)
        assertEquals("", editado.rutaFoto)

        // ...y borrar con confirmacion no falla por la ruta de foto vacia.
        composeRule.onNodeWithTag("lista_gastos").performScrollToNode(hasTestTag("ticket_${guardado.id}"))
        composeRule.onNodeWithTag("ticket_${guardado.id}").performClick()
        esperarTag("detalle_ticket_${guardado.id}")
        composeRule.onNodeWithTag("detalle_eliminar").performClick()
        esperarTag("detalle_confirmar_borrado")
        composeRule.onNodeWithTag("detalle_confirmar_eliminar").performClick()
        esperarTag("pantalla_gastos")
        esperarAusente("ticket_${guardado.id}")
        assertTrue(encontrar().isEmpty())

        // Escenario 6: un gasto manual no es una extraccion.
        assertEquals("el log de extracciones no debe cambiar", lineaBaseLog, filasLog())
    }

    // Escenario 3: validacion minima visible (texto, no solo color); comercio vacio no bloquea.
    @Test
    fun guardarSeDeshabilitaConMotivoVisibleSinTotalOConFechaFutura() {
        irANuevoGasto()

        // Valor inicial: total 0 -> requisito visible y Guardar deshabilitado.
        esperarTag("nuevo_gasto_requisito")
        assertEquals("Indica un total mayor que 0", textoDe("nuevo_gasto_requisito"))
        composeRule.onNodeWithTag("revision_guardar").assertIsNotEnabled()
        composeRule.onNodeWithTag("revision_descartar").assertIsNotEnabled()

        // Con total y SIN comercio ya se puede guardar.
        reemplazar("revision_total", "5")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("revision_guardar").assertIsEnabled()
        assertFalse(existe("nuevo_gasto_requisito"))

        // Fecha futura -> otro motivo, tambien visible.
        reemplazar("revision_fecha", hoy.plusDays(1).format(formatoFecha))
        composeRule.waitForIdle()
        esperarTag("nuevo_gasto_requisito")
        assertEquals("La fecha no puede ser futura", textoDe("nuevo_gasto_requisito"))
        composeRule.onNodeWithTag("revision_guardar").assertIsNotEnabled()

        // Descartar restablece el formulario inicial (sin escribir en Room).
        composeRule.onNodeWithTag("revision_descartar").assertIsEnabled().performClick()
        composeRule.waitForIdle()
        esperarTag("nuevo_gasto_requisito")
        assertEquals("Indica un total mayor que 0", textoDe("nuevo_gasto_requisito"))
        composeRule.onNodeWithTag("revision_guardar").assertIsNotEnabled()
    }

    // Escenario 7: atras vuelve a Gastos sin guardar y sin preguntar.
    @Test
    fun atrasDescartaSinGuardarNiPreguntar() {
        irANuevoGasto()
        reemplazar("revision_comercio", comercio)
        reemplazar("revision_total", "9")
        composeRule.waitForIdle()

        volverAtras()
        esperarTag("pantalla_gastos")
        assertFalse(existe("pantalla_nuevo_gasto"))
        assertTrue("no se debe guardar nada", encontrar().isEmpty())
        assertTrue(existe("gastos_anadir"))
        assertEquals(lineaBaseLog, filasLog())
    }
}

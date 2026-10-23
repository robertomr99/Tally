package com.robertomr.tally.ajustes

import androidx.compose.ui.semantics.SemanticsProperties
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
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
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.Ticket
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// E2E de seed-demo (spec, escenarios 1-4 y 7) en la build debug, sobre el repositorio
// real. Aislamiento: @Before guarda los tickets SIN marca de demo (mas uno "real"
// sembrado aqui con el mismo comercio y fecha que D01) y @After comprueba que siguen
// IDENTICOS, borra solo lo marcado y el ticket real propio. Nunca borra por rango.
@RunWith(AndroidJUnit4::class)
class SembrarDemoFlujoTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val repositorio = EntryPointAccessors
        .fromApplication(
            InstrumentationRegistry.getInstrumentation().targetContext.applicationContext,
            TicketRepositoryEntryPoint::class.java,
        )
        .ticketRepository()

    private val hoy: LocalDate = LocalDate.now()
    private var idRealPropio = 0L
    private lateinit var realesAntes: List<Ticket>

    private fun todos(): List<Ticket> = runBlocking {
        repositorio.observeByDateRange(LocalDate.of(2000, 1, 1), LocalDate.of(2100, 12, 31)).first()
    }

    private fun demo(): List<Ticket> = todos().filter { it.nota?.startsWith("tally-demo:v1:") == true }

    private fun reales(): List<Ticket> =
        todos().filter { it.nota?.startsWith("tally-demo:v1:") != true }.sortedBy { it.id }

    @Before
    fun prepararAislamiento() {
        // Estado limpio de demo (solo tickets marcados) y un ticket real que choca con D01.
        runBlocking { demo().forEach { repositorio.delete(it.id) } }
        val emision = LocalDateTime.of(hoy, java.time.LocalTime.of(9, 5))
        idRealPropio = runBlocking {
            repositorio.save(
                Ticket(
                    comercio = "Super Barrio",
                    fechaEmision = emision,
                    total = 7.77,
                    categoria = Categoria.ALIMENTACION,
                    fechaCaptura = emision,
                    motor = Motor.LOCAL,
                    rutaFoto = "",
                ),
            )
        }
        realesAntes = reales()
    }

    @After
    fun limpiarYComprobarAislamiento() {
        val despuesDeLaPrueba = reales()
        runBlocking { demo().forEach { repositorio.delete(it.id) } }
        runBlocking { repositorio.delete(idRealPropio) }
        // Los tickets reales no cambiaron durante la prueba (id, campos y lineas).
        assertEquals(realesAntes, despuesDeLaPrueba)
        assertTrue(demo().isEmpty())
    }

    private fun esperarTag(tag: String) {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun esperarTexto(tag: String, texto: String) {
        composeRule.waitUntil(10_000) {
            runCatching { composeRule.onNodeWithTag(tag).assertTextEquals(texto) }.isSuccess
        }
    }

    private fun abrirAjustes() {
        composeRule.onNodeWithTag("boton_ajustes").performClick()
        composeRule.onNodeWithTag("pantalla_ajustes").assertIsDisplayed()
        esperarTag("seccion_datos_demo")
        composeRule.onNodeWithTag("seccion_datos_demo").performScrollTo().assertIsDisplayed()
    }

    private fun sembrarDesdeAjustes() {
        composeRule.onNodeWithTag("demo_sembrar").performScrollTo().performClick()
        composeRule.onNodeWithTag("demo_confirmar_sembrar").performClick()
    }

    private fun quitarDesdeAjustes() {
        composeRule.onNodeWithTag("demo_quitar").performScrollTo().performClick()
        composeRule.onNodeWithTag("demo_confirmar_quitar").performClick()
    }

    /**
     * Opcional: con `-e evidencia <prefijo>` deja `/data/local/tmp/<prefijo>-<paso>.png`
     * (para docs/evidence/seed-demo/). Sin el argumento no hace nada.
     */
    private fun capturar(paso: String) {
        val prefijo = InstrumentationRegistry.getArguments().getString("evidencia") ?: return
        composeRule.waitForIdle()
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("screencap -p /data/local/tmp/$prefijo-$paso.png")
            .let { ParcelFileDescriptor.AutoCloseInputStream(it).use { s -> s.readBytes() } }
    }

    private fun texto(tag: String): String =
        composeRule.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.EditableText].text

    private fun reemplazar(tag: String, valor: String) {
        composeRule.onNodeWithTag(tag).performScrollTo()
        composeRule.onNodeWithTag(tag).performTextReplacement(valor)
    }

    // Escenarios 1, 2 y 4: sembrar, repetir (idempotente) y Gastos muestra los de demo.
    @Test
    fun sembrarYRepetirEsIdempotenteYGastosMuestraLosTickets() {
        abrirAjustes()
        esperarTexto("demo_contador", "Tickets de demo: 0")

        sembrarDesdeAjustes()
        esperarTexto("demo_resultado", "13 tickets sembrados")
        esperarTexto("demo_contador", "Tickets de demo: 13")
        assertEquals(13, demo().size)
        assertEquals(13, demo().map { it.nota }.toSet().size)

        sembrarDesdeAjustes()
        esperarTexto("demo_resultado", "Ya estaban los 13 tickets de demo")
        esperarTexto("demo_contador", "Tickets de demo: 13")
        assertEquals(13, demo().size)
        composeRule.onNodeWithTag("demo_resultado").performScrollTo()
        capturar("01-ajustes-sembrado")

        // Gastos: el ticket de hoy de la demo (D01) aparece en el periodo Dia.
        val d01 = demo().first { it.nota == "tally-demo:v1:D01" }
        composeRule.onNodeWithTag("ajustes_volver").performClick()
        composeRule.onNodeWithTag("bottom_bar_gastos").performClick()
        esperarTag("pantalla_gastos")
        composeRule.onNodeWithTag("periodo_dia").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("lista_gastos").performScrollToNode(hasTestTag("ticket_${d01.id}"))
        esperarTag("ticket_${d01.id}")
        capturar("02-gastos-dia")
        composeRule.onNodeWithTag("periodo_mes").performClick()
        composeRule.waitForIdle()
        capturar("03-gastos-mes")
        composeRule.onNodeWithTag("bottom_bar_resumen").performClick()
        esperarTag("pantalla_resumen")
        capturar("04-resumen-donut")

        // Escenario 4 (parcial): el real de mismo comercio y fecha sigue ahi, intacto.
        assertEquals(realesAntes, reales())
    }

    // Escenario 3: Quitar borra exactamente los de demo y avisa; despues "No habia datos".
    @Test
    fun quitarBorraSoloLosDeDemoYAvisa() {
        abrirAjustes()
        sembrarDesdeAjustes()
        esperarTexto("demo_resultado", "13 tickets sembrados")
        esperarTexto("demo_contador", "Tickets de demo: 13")

        quitarDesdeAjustes()
        esperarTexto("demo_resultado", "13 tickets de demo quitados")
        esperarTexto("demo_contador", "Tickets de demo: 0")
        assertTrue(demo().isEmpty())
        assertEquals(realesAntes, reales())
        assertNotNull(runBlocking { repositorio.getById(idRealPropio) })

        quitarDesdeAjustes()
        esperarTexto("demo_resultado", "No había datos de demo")
        assertEquals(realesAntes, reales())
    }

    // Escenario 7: el ticket "malo" se abre en Detalle y se corrige con Editar.
    @Test
    fun ticketMaloSeCorrigeDesdeDetalle() {
        // D13 cae en "hoy - 1": con el filtro Mes solo esta visible si no es dia 1.
        assumeTrue(hoy.dayOfMonth > 1)
        abrirAjustes()
        sembrarDesdeAjustes()
        esperarTexto("demo_contador", "Tickets de demo: 13")
        val malo = demo().first { it.nota == "tally-demo:v1:D13" }
        assertEquals("Comercio ilegible", malo.comercio)
        assertEquals(0.0, malo.total, 0.0)

        composeRule.onNodeWithTag("ajustes_volver").performClick()
        composeRule.onNodeWithTag("bottom_bar_gastos").performClick()
        esperarTag("pantalla_gastos")
        composeRule.onNodeWithTag("periodo_mes").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("lista_gastos").performScrollToNode(hasTestTag("ticket_${malo.id}"))
        esperarTag("ticket_${malo.id}")
        composeRule.onNodeWithTag("ticket_${malo.id}").performClick()
        esperarTag("detalle_ticket_${malo.id}")
        assertEquals("Comercio ilegible", texto("revision_comercio"))
        capturar("05-detalle-ticket-malo")

        reemplazar("revision_comercio", "Mercadona corregido")
        reemplazar("revision_total", "18,40")
        composeRule.onNodeWithTag("revision_categoria_ALIMENTACION").performScrollTo().performClick()
        capturar("06-detalle-corregido")
        composeRule.onNodeWithTag("revision_guardar").performClick()
        esperarTag("pantalla_gastos")

        val corregido = runBlocking { repositorio.getById(malo.id) }!!
        assertEquals("Mercadona corregido", corregido.comercio)
        assertEquals(18.40, corregido.total, 0.0)
        assertEquals(Categoria.ALIMENTACION, corregido.categoria)
        // Editar conserva la marca: sigue siendo de demo y "Quitar" lo borrara.
        assertEquals("tally-demo:v1:D13", corregido.nota)
        assertEquals(realesAntes, reales())
    }
}

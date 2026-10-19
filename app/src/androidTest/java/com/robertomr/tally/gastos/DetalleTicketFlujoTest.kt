package com.robertomr.tally.gastos

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
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
import com.robertomr.tally.domain.expenses.entities.Linea
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.Ticket
import dagger.hilt.android.EntryPointAccessors
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// E2E de detalle-ticket (spec, escenarios 1-7) en emulador. Se siembran con el
// repositorio real tickets de comercio UNICO (el emulador tiene tickets
// residuales: no se afirman totales globales) y un JPEG REAL escrito en
// filesDir/fotos desde el targetContext; @After borra por repositorio lo que
// siga existiendo y los ficheros que quedaran. Ningun test salta por assumeTrue.
@RunWith(AndroidJUnit4::class)
class DetalleTicketFlujoTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext

    private val repositorio = EntryPointAccessors
        .fromApplication(contexto.applicationContext, TicketRepositoryEntryPoint::class.java)
        .ticketRepository()

    private val idsSembrados = mutableListOf<Long>()
    private val fotosSembradas = mutableListOf<String>()
    private val hoy: LocalDate = LocalDate.now()
    private val formatoFecha = DateTimeFormatter.ofPattern("d/M/yyyy")

    @After
    fun limpiar() {
        runBlocking { idsSembrados.forEach { repositorio.delete(it) } }
        idsSembrados.clear()
        fotosSembradas.forEach { File(contexto.filesDir, it).delete() }
        fotosSembradas.clear()
    }

    /** Escribe un JPEG real (decodificable) en filesDir/fotos y devuelve su ruta relativa. */
    private fun sembrarFoto(): String {
        val relativa = "fotos/detalle_e2e_${UUID.randomUUID()}.jpg"
        val fichero = File(contexto.filesDir, relativa)
        fichero.parentFile?.mkdirs()
        val bitmap = Bitmap.createBitmap(240, 360, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(240, 236, 225))
        val pincel = Paint().apply { color = Color.rgb(40, 90, 60) }
        canvas.drawRect(20f, 20f, 220f, 60f, pincel)
        FileOutputStream(fichero).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        fotosSembradas += relativa
        return relativa
    }

    private fun sembrar(rutaFoto: String, comercio: String = "E2E detalle ${UUID.randomUUID()}"): Ticket {
        val emision = LocalDateTime.of(hoy, LocalTime.of(23, 59, 41))
        val borrador = Ticket(
            comercio = comercio,
            fechaEmision = emision,
            total = 10.0,
            categoria = Categoria.HOGAR,
            fechaCaptura = emision.minusMinutes(5),
            motor = Motor.NUBE,
            rutaFoto = rutaFoto,
            nota = "nota e2e",
            textoCrudoOcr = "crudo e2e",
            lineas = listOf(Linea("LECHE", 2, 1.5), Linea("PAN", 1, 0.9)),
        )
        val id = runBlocking { repositorio.save(borrador) }
        idsSembrados += id
        return borrador.copy(id = id)
    }

    private fun esperarTag(tag: String) {
        composeRule.waitUntil(10_000) { existe(tag) }
    }

    private fun existe(tag: String): Boolean =
        composeRule.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

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

    private fun volverAtras() {
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }

    private fun reemplazar(tag: String, valor: String) {
        composeRule.onNodeWithTag(tag).performScrollTo()
        composeRule.onNodeWithTag(tag).performTextReplacement(valor)
    }

    // Escenarios 1 y 7: se abre desde Gastos y desde Resumen con foto, campos,
    // lineas y badge reales; sin barra inferior; Atras conserva el periodo.
    @Test
    fun abreDesdeGastosYResumenMostrandoFotoCamposYLineas() {
        val ruta = sembrarFoto()
        val t = sembrar(ruta)

        irAGastosDia()
        abrirDesdeGastos(t.id)
        composeRule.onNodeWithTag("pantalla_detalle_ticket").assertIsDisplayed()
        composeRule.onNodeWithTag("papel_ticket").assertIsDisplayed()
        assertEquals(t.comercio, texto("revision_comercio"))
        assertEquals(hoy.format(formatoFecha), texto("revision_fecha"))
        assertEquals("10,00", texto("revision_total"))
        assertEquals("LECHE", texto("revision_linea_0_concepto"))
        assertEquals("2", texto("revision_linea_0_cantidad"))
        assertEquals("PAN", texto("revision_linea_1_concepto"))
        composeRule.onNodeWithTag("revision_badge_motor", useUnmergedTree = true)
            .assertTextEquals("Motor Nube · Gemini")
        assertFalse("la barra inferior debe ocultarse en el detalle", existe("bottom_bar_gastos"))

        // Foto original: el visor decodifica el JPEG sembrado.
        composeRule.onNodeWithTag("detalle_ver_foto").performClick()
        esperarTag("detalle_foto")
        composeRule.onNodeWithTag("detalle_foto").assertIsDisplayed()
        composeRule.onNodeWithTag("detalle_cerrar_foto").performClick()
        esperarAusente("detalle_foto")

        // Atras: mismo periodo (Dia) y barra inferior de vuelta.
        volverAtras()
        esperarTag("pantalla_gastos")
        composeRule.onNodeWithTag("periodo_dia").assertIsSelected()
        assertTrue(existe("bottom_bar_gastos"))

        // Desde la vista previa de Resumen.
        composeRule.onNodeWithTag("bottom_bar_resumen").performClick()
        esperarTag("pantalla_resumen")
        composeRule.waitForIdle()
        esperarTag("resumen_ticket_${t.id}")
        composeRule.onNodeWithTag("resumen_ticket_${t.id}").performScrollTo().performClick()
        esperarTag("detalle_ticket_${t.id}")
        assertEquals(t.comercio, texto("revision_comercio"))
        volverAtras()
        esperarTag("pantalla_resumen")
    }

    // Escenarios 3 y 4: editar persiste el MISMO ticket y el listado se actualiza solo.
    @Test
    fun editarPersisteElMismoTicketConHoraConservadaYElListadoSeActualiza() {
        val t = sembrar(sembrarFoto())
        val nuevoComercio = "E2E editado ${UUID.randomUUID()}"
        val nuevaFecha = if (hoy.dayOfMonth > 1) hoy.minusDays(1) else hoy.plusDays(1)

        irAGastosDia()
        abrirDesdeGastos(t.id)

        // Sin cambios: Guardar y Descartar deshabilitados.
        composeRule.onNodeWithTag("revision_guardar").assertIsNotEnabled()
        composeRule.onNodeWithTag("revision_descartar").assertIsNotEnabled()

        reemplazar("revision_comercio", nuevoComercio)
        reemplazar("revision_total", "12,5")
        reemplazar("revision_fecha", nuevaFecha.format(formatoFecha))
        composeRule.onNodeWithTag("revision_categoria_OCIO").performScrollTo().performClick()
        composeRule.onNodeWithTag("revision_linea_0_borrar").performScrollTo().performClick()
        composeRule.onNodeWithTag("revision_anadir_linea").performScrollTo().performClick()
        reemplazar("revision_linea_2_concepto", "HUEVOS")
        reemplazar("revision_linea_2_cantidad", "3")
        reemplazar("revision_linea_2_precio", "2,5")

        composeRule.onNodeWithTag("revision_guardar").assertIsEnabled()
        composeRule.onNodeWithTag("revision_guardar").performClick()

        // D8: vuelve a la pantalla de origen y la lista muestra el cambio sola.
        esperarTag("pantalla_gastos")
        val guardado = runBlocking { repositorio.getById(t.id) }
        assertNotNull(guardado)
        guardado!!
        assertEquals(t.id, guardado.id)
        assertEquals(nuevoComercio, guardado.comercio)
        assertEquals(12.5, guardado.total, 0.0)
        assertEquals(Categoria.OCIO, guardado.categoria)
        assertEquals(LocalDateTime.of(nuevaFecha, LocalTime.of(23, 59, 41)), guardado.fechaEmision)
        assertEquals(t.fechaCaptura, guardado.fechaCaptura)
        assertEquals(Motor.NUBE, guardado.motor)
        assertEquals(t.rutaFoto, guardado.rutaFoto)
        assertEquals("crudo e2e", guardado.textoCrudoOcr)
        assertEquals("nota e2e", guardado.nota)
        assertEquals(listOf(Linea("PAN", 1, 0.9), Linea("HUEVOS", 3, 2.5)), guardado.lineas)
        // El periodo Dia seguia activo: el ticket movido a otra fecha sale del listado.
        composeRule.onNodeWithTag("periodo_dia").assertIsSelected()
        esperarAusente("ticket_${t.id}")
        composeRule.onNodeWithTag("periodo_mes").performClick()
        composeRule.onNodeWithTag("lista_gastos").performScrollToNode(hasTestTag("ticket_${t.id}"))
        esperarTag("ticket_${t.id}")
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodes(hasText(nuevoComercio), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    // Descartar restablece el formulario a lo guardado sin escribir en Room; Atras no guarda (D3).
    @Test
    fun descartarRestableceYAtrasNoGuarda() {
        val t = sembrar(sembrarFoto())

        irAGastosDia()
        abrirDesdeGastos(t.id)
        reemplazar("revision_comercio", "CAMBIO NO GUARDADO")
        composeRule.onNodeWithTag("revision_descartar").assertIsEnabled()
        composeRule.onNodeWithTag("revision_descartar").performClick()
        composeRule.waitForIdle()
        assertEquals(t.comercio, texto("revision_comercio"))
        composeRule.onNodeWithTag("revision_guardar").assertIsNotEnabled()

        reemplazar("revision_total", "99")
        volverAtras()
        esperarTag("pantalla_gastos")
        val enRepo = runBlocking { repositorio.getById(t.id) }!!
        assertEquals(t.comercio, enRepo.comercio)
        assertEquals(10.0, enRepo.total, 0.0)
    }

    // Escenarios 5 y 6 (verificacion 1): borrado confirmado elimina fila, lineas y fichero.
    @Test
    fun borrarConConfirmacionEliminaTicketYFicheroDeFoto() {
        val ruta = sembrarFoto()
        val t = sembrar(ruta)
        val fichero = File(contexto.filesDir, ruta)
        assertTrue("la foto sembrada debe existir antes de borrar", fichero.exists())

        irAGastosDia()
        abrirDesdeGastos(t.id)

        // Cancelar no cambia nada.
        composeRule.onNodeWithTag("detalle_eliminar").performClick()
        esperarTag("detalle_confirmar_borrado")
        composeRule.onNodeWithTag("detalle_cancelar_borrado").performClick()
        esperarAusente("detalle_confirmar_borrado")
        assertNotNull(runBlocking { repositorio.getById(t.id) })
        assertTrue(fichero.exists())

        // Confirmar: vuelve al listado y desaparece todo.
        composeRule.onNodeWithTag("detalle_eliminar").performClick()
        esperarTag("detalle_confirmar_borrado")
        composeRule.onNodeWithTag("detalle_confirmar_eliminar").performClick()
        esperarTag("pantalla_gastos")
        esperarAusente("ticket_${t.id}")
        assertNull(runBlocking { repositorio.getById(t.id) })
        assertFalse("la foto debe borrarse de filesDir", fichero.exists())
    }

    // Escenario 2: foto ausente (fichero inexistente) sin crash y el resto funciona.
    @Test
    fun fotoAusenteMuestraTarjetaYLaEdicionSigueFuncionando() {
        val t = sembrar("fotos/detalle_e2e_inexistente_${UUID.randomUUID()}.jpg")

        irAGastosDia()
        abrirDesdeGastos(t.id)
        esperarTag("detalle_foto_ausente")
        assertFalse(existe("detalle_ver_foto"))
        composeRule.onNodeWithTag("detalle_foto_ausente").assertIsDisplayed()

        reemplazar("revision_comercio", "E2E sin foto ${UUID.randomUUID()}")
        composeRule.onNodeWithTag("revision_guardar").assertIsEnabled()
    }

    // Escenario 2: ticket inexistente (borrado entre tocar la fila y abrir el detalle).
    @Test
    fun ticketInexistenteMuestraAvisoYVolverSale() {
        val t = sembrar(sembrarFoto())

        irAGastosDia()
        composeRule.onNodeWithTag("lista_gastos").performScrollToNode(hasTestTag("ticket_${t.id}"))
        esperarTag("ticket_${t.id}")
        // Se captura la accion de la fila, se borra el ticket por repositorio y se
        // dispara la navegacion con la accion capturada (ruta obsoleta).
        val accion = composeRule.onNodeWithTag("ticket_${t.id}").fetchSemanticsNode()
            .config[SemanticsActions.OnClick].action!!
        runBlocking { repositorio.delete(t.id) }
        composeRule.runOnUiThread { accion() }

        esperarTag("detalle_no_existe")
        composeRule.onNodeWithTag("detalle_no_existe").assertIsDisplayed()
        assertFalse(existe("revision_comercio"))
        composeRule.onNodeWithTag("detalle_volver").performClick()
        esperarTag("pantalla_gastos")
    }
}

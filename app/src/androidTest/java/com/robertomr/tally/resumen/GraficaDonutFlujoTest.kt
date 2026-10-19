package com.robertomr.tally.resumen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
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
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// E2E de grafica-donut (spec, escenarios 1, 3, 4, 6 y 7) en emulador. Se siembran
// con el repositorio real tickets de varias categorias hoy; otros E2E pueden
// haber dejado tickets, asi que las expectativas se calculan como "linea base
// leida del repositorio + sembrado" (agrupando por categoria en centimos, de
// forma independiente a la logica de dominio) y lo sembrado se borra en @After.
@RunWith(AndroidJUnit4::class)
class GraficaDonutFlujoTest {

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

    /** Centimos por categoria calculados aqui (suma de `round(total * 100)`), sin usar el dominio. */
    private fun centimosPorCategoria(tickets: List<Ticket>): Map<Categoria, Long> =
        tickets.filter { it.total > 0.0 }
            .groupBy { it.categoria }
            .mapValues { (_, lista) -> lista.sumOf { Math.round(it.total * 100) } }
            .filterValues { it > 0L }

    @Test
    fun donutMuestraLeyendaYDesgloseConImportesYPorcentajesQueSuman100() {
        val mes = Periodo.de(PeriodoTipo.MES, hoy)
        val base = baseDe(mes)
        sembrar(Categoria.ALIMENTACION, 10.00)
        sembrar(Categoria.ALIMENTACION, 28.00)
        sembrar(Categoria.HOGAR, 20.00)
        sembrar(Categoria.TRANSPORTE, 15.00)
        val esperado = centimosPorCategoria(base + tickets(Categoria.ALIMENTACION to 38.0, Categoria.HOGAR to 20.0, Categoria.TRANSPORTE to 15.0))

        verificarDistribucion(esperado)
        composeRule.onNodeWithTag("donut_grafica").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun alternarPeriodoRecalculaLaDistribucion() {
        val dia = Periodo.de(PeriodoTipo.DIA, hoy)
        val baseDia = baseDe(dia)
        sembrar(Categoria.SALUD, 12.34)
        sembrar(Categoria.OCIO, 7.66)

        composeRule.onNodeWithTag("periodo_dia").performClick()
        val esperadoDia = centimosPorCategoria(baseDia + tickets(Categoria.SALUD to 12.34, Categoria.OCIO to 7.66))
        verificarDistribucion(esperadoDia)

        val mes = Periodo.de(PeriodoTipo.MES, hoy)
        composeRule.onNodeWithTag("periodo_mes").performClick()
        val esperadoMes = centimosPorCategoria(baseDe(mes))
        verificarDistribucion(esperadoMes)
    }

    @Test
    fun periodoSinGastoMuestraElEstadoVacio() {
        val dia = Periodo.de(PeriodoTipo.DIA, hoy)
        // Si la linea base de hoy no esta vacia el vacio queda cubierto en JVM (spec, caso C).
        assumeTrue(centimosPorCategoria(baseDe(dia)).isEmpty())

        composeRule.onNodeWithTag("periodo_dia").performClick()
        composeRule.waitUntil(10_000) {
            runCatching { composeRule.onNodeWithTag("donut_vacio").assertExists() }.isSuccess
        }
        composeRule.onNodeWithTag("donut_vacio").assertTextEquals("Aún no hay gastos en este periodo.")
        for (c in Categoria.entries) {
            composeRule.onNodeWithTag("desglose_${c.name}").assertDoesNotExist()
            composeRule.onNodeWithTag("leyenda_${c.name}").assertDoesNotExist()
        }
    }

    @Test
    fun guardarUnTicketActualizaElDonutSolo() {
        val mes = Periodo.de(PeriodoTipo.MES, hoy)
        val base = baseDe(mes)
        verificarDistribucion(centimosPorCategoria(base))

        sembrar(Categoria.OTROS, 33.33)

        verificarDistribucion(centimosPorCategoria(base + tickets(Categoria.OTROS to 33.33)))
    }

    /** Espera a que importes y porcentajes de la UI coincidan con [esperado] y valida las reglas del reparto. */
    private fun verificarDistribucion(esperado: Map<Categoria, Long>) {
        for (c in Categoria.entries) {
            val centimos = esperado[c]
            if (centimos == null) {
                composeRule.waitUntil(10_000) {
                    runCatching { composeRule.onNodeWithTag("desglose_${c.name}").assertDoesNotExist() }.isSuccess
                }
                composeRule.onNodeWithTag("leyenda_${c.name}").assertDoesNotExist()
            } else {
                esperarTexto("desglose_${c.name}_importe", formatoImporte(centimos / 100.0))
                composeRule.onNodeWithTag("leyenda_${c.name}").assertExists()
            }
        }
        // Los porcentajes mostrados suman 100 (cada porcion diminuta "<1 %" cuenta como 0).
        if (esperado.isNotEmpty()) {
            val suma = esperado.keys.sumOf { c -> porcentajeMostrado(c) }
            assertEquals(100, suma)
        }
    }

    private fun porcentajeMostrado(c: Categoria): Int {
        val texto = textoDe("desglose_${c.name}_porcentaje")
        return if (texto.startsWith("<")) 0 else texto.removeSuffix(" %").trim().toInt()
    }

    private fun textoDe(tag: String): String {
        val config = composeRule.onNodeWithTag(tag).fetchSemanticsNode().config
        return config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }.orEmpty()
    }

    private fun esperarTexto(tag: String, texto: String) {
        composeRule.waitUntil(10_000) {
            runCatching { composeRule.onNodeWithTag(tag).assertTextEquals(texto) }.isSuccess
        }
        composeRule.onNodeWithTag(tag).assertTextEquals(texto)
    }

    private fun tickets(vararg pares: Pair<Categoria, Double>): List<Ticket> =
        pares.map { (categoria, total) -> ticket(categoria, total) }

    private fun ticket(categoria: Categoria, total: Double) = Ticket(
        comercio = "E2E donut $total",
        fechaEmision = LocalDateTime.of(hoy, LocalTime.NOON),
        total = total,
        categoria = categoria,
        fechaCaptura = LocalDateTime.of(hoy, LocalTime.NOON),
        motor = Motor.LOCAL,
        rutaFoto = "",
    )

    private fun sembrar(categoria: Categoria, total: Double) {
        val id = runBlocking { repositorio.save(ticket(categoria, total)) }
        idsSembrados += id
    }
}

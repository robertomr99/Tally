package com.robertomr.tally.resumen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.robertomr.tally.MainActivity
import com.robertomr.tally.di.TicketRepositoryEntryPoint
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.Periodo
import com.robertomr.tally.domain.expenses.entities.PeriodoTipo
import com.robertomr.tally.domain.expenses.entities.Ticket
import com.robertomr.tally.feature.resumen.TEXTO_SIN_REFERENCIA
import com.robertomr.tally.feature.common.formatoImporte
import com.robertomr.tally.feature.resumen.formatoPromedio
import com.robertomr.tally.feature.resumen.formatoVariacion
import com.robertomr.tally.feature.resumen.textoTickets
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// E2E de resumen-total (spec, escenarios 1, 5 y 7) en emulador. Se siembran con
// el repositorio real 10,00 y 20,50 hoy y 40,00 en el primer dia del mes anterior.
// Otros E2E pueden haber dejado tickets, asi que las expectativas se calculan
// como "linea base leida del repositorio + sembrado" y los tickets sembrados se
// borran en @After. Las expectativas se derivan aqui de forma independiente
// (dias transcurridos, variacion) y solo se formatean con las funciones de la UI.
@RunWith(AndroidJUnit4::class)
class ResumenTotalFlujoTest {

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

    @Test
    fun tarjetaMuestraTotalPromedioTicketsYComparativaDelMes() {
        val mes = Periodo.de(PeriodoTipo.MES, hoy)
        val anterior = mes.anterior()
        val baseActual = baseDe(mes)
        val baseAnterior = baseDe(anterior)

        sembrar(hoy, 10.00)
        sembrar(hoy, 20.50)
        sembrar(anterior.desde, 40.00)

        val total = baseActual.sumOf { it.total } + 30.50
        val totalAnterior = baseAnterior.sumOf { it.total } + 40.00
        val dias = ChronoUnit.DAYS.between(mes.desde, hoy) + 1 // mes en curso: dias transcurridos
        val variacion = (total - totalAnterior) / totalAnterior

        esperarTexto("resumen_suma", formatoImporte(total))
        esperarTexto("resumen_num_tickets", textoTickets(baseActual.size + 2))
        esperarTexto("resumen_promedio", formatoPromedio(total / dias))
        esperarTexto("resumen_comparativa", "${formatoVariacion(variacion)} vs período anterior")
        composeRule.onNodeWithTag("pantalla_resumen").assertIsDisplayed()
    }

    @Test
    fun alternarPeriodoRecalculaLaTarjetaYAnteriorSinGastoMuestraBadgeNeutro() {
        val dia = Periodo.de(PeriodoTipo.DIA, hoy)
        val baseDia = baseDe(dia)
        val baseAyer = baseDe(dia.anterior())

        sembrar(hoy, 12.34)

        composeRule.onNodeWithTag("periodo_dia").performClick()
        val totalDia = baseDia.sumOf { it.total } + 12.34
        esperarTexto("resumen_suma", formatoImporte(totalDia))
        esperarTexto("resumen_num_tickets", textoTickets(baseDia.size + 1))
        // En DIA el promedio es siempre el total.
        esperarTexto("resumen_promedio", formatoPromedio(totalDia))
        val totalAyer = baseAyer.sumOf { it.total }
        val esperadoBadge = if (totalAyer <= 0.0) {
            TEXTO_SIN_REFERENCIA
        } else {
            "${formatoVariacion((totalDia - totalAyer) / totalAyer)} vs período anterior"
        }
        esperarTexto("resumen_comparativa", esperadoBadge)
    }

    @Test
    fun guardarUnTicketActualizaLaTarjetaSola() {
        val mes = Periodo.de(PeriodoTipo.MES, hoy)
        val base = baseDe(mes)
        esperarTexto("resumen_suma", formatoImporte(base.sumOf { it.total }))

        sembrar(hoy, 33.33)

        esperarTexto("resumen_suma", formatoImporte(base.sumOf { it.total } + 33.33))
        esperarTexto("resumen_num_tickets", textoTickets(base.size + 1))
    }

    private fun esperarTexto(tag: String, texto: String) {
        composeRule.waitUntil(10_000) {
            runCatching { composeRule.onNodeWithTag(tag).assertTextEquals(texto) }.isSuccess
        }
        composeRule.onNodeWithTag(tag).assertTextEquals(texto)
    }

    private fun sembrar(emision: LocalDate, total: Double) {
        val id = runBlocking {
            repositorio.save(
                Ticket(
                    comercio = "E2E resumen $total",
                    fechaEmision = LocalDateTime.of(emision, LocalTime.NOON),
                    total = total,
                    categoria = Categoria.OTROS,
                    fechaCaptura = LocalDateTime.of(emision, LocalTime.NOON),
                    motor = Motor.LOCAL,
                    rutaFoto = "",
                ),
            )
        }
        idsSembrados += id
    }
}

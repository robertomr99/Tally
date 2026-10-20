package com.robertomr.tally.resumen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
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
import com.robertomr.tally.feature.common.etiquetaPeriodo
import com.robertomr.tally.feature.common.formatoImporte
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// E2E de periodo-selector (spec, escenarios 4 y 5) en emulador: se siembran
// tickets con el repositorio real (hoy, hace 3 dias, hace 40 dias; uno emitido
// hoy pero capturado hace 40 dias y otro al reves) y se alterna Dia/Semana/Mes
// comprobando rango, recuento y suma. Como otros E2E pueden haber dejado
// tickets en la base, las expectativas se calculan como "linea base del rango
// leida del repositorio + lo sembrado". Los tickets sembrados se borran en @After.
@RunWith(AndroidJUnit4::class)
class PeriodoSelectorFlujoTest {

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

    private class Siembra(val emision: LocalDate, val total: Double)

    private val siembra = listOf(
        Siembra(hoy, 11.11),
        Siembra(hoy.minusDays(3), 22.22),
        Siembra(hoy.minusDays(40), 44.44),
    )

    @After
    fun limpiarTicketsSembrados() {
        runBlocking { idsSembrados.forEach { repositorio.delete(it) } }
        idsSembrados.clear()
    }

    @Test
    fun alternarDiaSemanaMesCambiaRangoRecuentoYSuma() {
        // Lineas base ANTES de sembrar (puede haber tickets de otros E2E).
        val base = PeriodoTipo.entries.filter { it != PeriodoTipo.PERSONALIZADO }.associateWith { tipo ->
            val p = Periodo.de(tipo, hoy)
            runBlocking { repositorio.observeByDateRange(p.desde, p.hasta).first() }
        }
        sembrar()

        // Escenario 5: Mes activo por defecto.
        composeRule.onNodeWithTag("periodo_mes").assertIsSelected()
        composeRule.onNodeWithTag("periodo_dia").assertIsNotSelected()
        composeRule.onNodeWithTag("periodo_semana").assertIsNotSelected()

        // Orden Mes -> Dia -> Semana -> Mes: el total tiene que cambiar al alternar.
        verificar("periodo_mes", PeriodoTipo.MES, base)
        verificar("periodo_dia", PeriodoTipo.DIA, base)
        verificar("periodo_semana", PeriodoTipo.SEMANA, base)
        verificar("periodo_mes", PeriodoTipo.MES, base)
    }

    @Test
    fun guardarUnTicketDelPeriodoActivoActualizaLaCifraSola() {
        val p = Periodo.de(PeriodoTipo.MES, hoy)
        val base = runBlocking { repositorio.observeByDateRange(p.desde, p.hasta).first() }
        composeRule.onNodeWithTag("periodo_mes").assertIsSelected()
        esperarTexto("resumen_num_tickets", textoTickets(base.size))

        sembrarUno(Siembra(hoy, 33.33))

        val esperado = base.sumOf { it.total } + 33.33
        esperarTexto("resumen_num_tickets", textoTickets(base.size + 1))
        esperarTexto("resumen_suma", formatoImporte(esperado))
    }

    private fun verificar(tagSegmento: String, tipo: PeriodoTipo, base: Map<PeriodoTipo, List<Ticket>>) {
        composeRule.onNodeWithTag(tagSegmento).performClick()
        composeRule.onNodeWithTag(tagSegmento).assertIsSelected()
        val periodo = Periodo.de(tipo, hoy)
        val sembradosEnRango = siembra.filter { it.emision >= periodo.desde && it.emision <= periodo.hasta }
        val numEsperado = base.getValue(tipo).size + sembradosEnRango.size
        val sumaEsperada = base.getValue(tipo).sumOf { it.total } + sembradosEnRango.sumOf { it.total }
        esperarTexto("resumen_rango", etiquetaPeriodo(periodo))
        esperarTexto("resumen_num_tickets", textoTickets(numEsperado))
        esperarTexto("resumen_suma", formatoImporte(sumaEsperada))
        composeRule.onNodeWithTag("pantalla_resumen").assertIsDisplayed()
    }

    private fun textoTickets(n: Int) = if (n == 1) "1 ticket" else "$n tickets"

    private fun esperarTexto(tag: String, texto: String) {
        composeRule.waitUntil(10_000) {
            runCatching { composeRule.onNodeWithTag(tag).assertTextEquals(texto) }.isSuccess
        }
        composeRule.onNodeWithTag(tag).assertTextEquals(texto)
    }

    private fun sembrar() = siembra.forEach { sembrarUno(it) }

    private fun sembrarUno(s: Siembra) {
        val captura = if (s.total == 11.11) hoy.minusDays(40) else if (s.total == 44.44) hoy else s.emision
        val id = runBlocking {
            repositorio.save(
                Ticket(
                    comercio = "E2E periodo ${s.total}",
                    fechaEmision = LocalDateTime.of(s.emision, java.time.LocalTime.NOON),
                    total = s.total,
                    categoria = Categoria.OTROS,
                    fechaCaptura = LocalDateTime.of(captura, java.time.LocalTime.NOON),
                    motor = Motor.LOCAL,
                    rutaFoto = "",
                ),
            )
        }
        idsSembrados += id
    }
}

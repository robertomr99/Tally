package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.Periodo
import com.robertomr.tally.domain.expenses.entities.PeriodoTipo
import com.robertomr.tally.domain.expenses.entities.Ticket
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Métricas con datasets conocidos (spec resumen-total, escenarios 1-5). */
class CalcularResumenPeriodoTest {

    private val calcular = CalcularResumenPeriodo()
    private val eps = 1e-9

    private fun tickets(vararg totales: Double) = totales.map {
        Ticket(
            comercio = "T",
            fechaEmision = LocalDateTime.of(2026, 10, 1, 12, 0),
            total = it,
            categoria = Categoria.OTROS,
            fechaCaptura = LocalDateTime.of(2026, 10, 1, 12, 0),
            motor = Motor.LOCAL,
            rutaFoto = "x.jpg",
        )
    }

    private val octubre = Periodo.de(PeriodoTipo.MES, LocalDate.of(2026, 10, 18))

    @Test
    fun datasetConocidoTotalNumeroYVariacion() {
        val r = calcular(octubre, LocalDate.of(2026, 10, 18), tickets(10.0, 20.5, 30.0), tickets(40.0))
        assertEquals(60.5, r.total, eps)
        assertEquals(3, r.numTickets)
        assertEquals(40.0, r.totalAnterior, eps)
        assertEquals(0.5125, r.variacion!!, eps)
        assertEquals(60.5 / 18, r.promedioDiario, eps)
    }

    @Test
    fun anteriorVacioDaVariacionNulaSinNaNNiInfinito() {
        val r = calcular(octubre, LocalDate.of(2026, 10, 18), tickets(50.0), emptyList())
        assertNull(r.variacion)
        assertEquals(50.0, r.total, eps)
    }

    @Test
    fun ambosVaciosDanCerosYVariacionNula() {
        val r = calcular(octubre, LocalDate.of(2026, 10, 18), emptyList(), emptyList())
        assertEquals(0.0, r.total, eps)
        assertEquals(0.0, r.promedioDiario, eps)
        assertEquals(0, r.numTickets)
        assertNull(r.variacion)
    }

    @Test
    fun actualVacioConAnteriorConGastoEsMenos100() {
        val r = calcular(octubre, LocalDate.of(2026, 10, 18), emptyList(), tickets(25.0))
        assertNotNull(r.variacion)
        assertEquals(-1.0, r.variacion!!, eps)
        assertEquals(0.0, r.total, eps)
    }

    @Test
    fun variacionCeroSiTotalesIguales() {
        val r = calcular(octubre, LocalDate.of(2026, 10, 18), tickets(30.0), tickets(10.0, 20.0))
        assertEquals(0.0, r.variacion!!, eps)
    }

    @Test
    fun mesEnCursoDivideEntreDiasTranscurridos() {
        // 18-oct: 180 / 18 = 10, no / 31.
        assertEquals(10.0, calcular(octubre, LocalDate.of(2026, 10, 18), tickets(180.0), emptyList()).promedioDiario, eps)
    }

    @Test
    fun primerDiaDelMesDivideEntreUno() {
        assertEquals(50.0, calcular(octubre, LocalDate.of(2026, 10, 1), tickets(50.0), emptyList()).promedioDiario, eps)
    }

    @Test
    fun ultimoDiaDelMesDivideEntreDiasTotales() {
        assertEquals(10.0, calcular(octubre, LocalDate.of(2026, 10, 31), tickets(310.0), emptyList()).promedioDiario, eps)
    }

    @Test
    fun periodoTerminadoDivideEntreDiasTotales() {
        assertEquals(10.0, calcular(octubre, LocalDate.of(2026, 11, 5), tickets(310.0), emptyList()).promedioDiario, eps)
    }

    @Test
    fun hoyAnteriorAlInicioNuncaDivideEntreCero() {
        val r = calcular(octubre, LocalDate.of(2026, 9, 1), tickets(50.0), emptyList())
        assertEquals(50.0, r.promedioDiario, eps)
    }

    @Test
    fun semanaEnCursoEnMiercolesSonTresDias() {
        val semana = Periodo.de(PeriodoTipo.SEMANA, LocalDate.of(2026, 10, 14)) // lun 12 - dom 18
        assertEquals(10.0, calcular(semana, LocalDate.of(2026, 10, 14), tickets(30.0), emptyList()).promedioDiario, eps)
    }

    @Test
    fun diaSiempreEsElTotal() {
        val dia = Periodo.de(PeriodoTipo.DIA, LocalDate.of(2026, 10, 18))
        assertEquals(42.0, calcular(dia, LocalDate.of(2026, 10, 18), tickets(42.0), emptyList()).promedioDiario, eps)
        assertEquals(42.0, calcular(dia, LocalDate.of(2026, 10, 25), tickets(42.0), emptyList()).promedioDiario, eps)
    }
}

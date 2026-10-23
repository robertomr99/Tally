package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.DistribucionCategorias
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.Ticket
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Reparto por categoría (spec grafica-donut). */
class CalcularDistribucionCategoriasTest {

    private val calcular = CalcularDistribucionCategorias()

    private fun t(categoria: Categoria, total: Double) = Ticket(
        comercio = "T",
        fechaEmision = LocalDateTime.of(2026, 10, 15, 12, 0),
        total = total,
        categoria = categoria,
        fechaCaptura = LocalDateTime.of(2026, 10, 15, 12, 0),
        motor = Motor.LOCAL,
        rutaFoto = "x.jpg",
    )

    @Test
    fun datasetConocidoOrdenaPorImporteYSuma100() {
        val d = calcular(
            listOf(
                t(Categoria.ALIMENTACION, 10.0), t(Categoria.ALIMENTACION, 28.0),
                t(Categoria.HOGAR, 20.0), t(Categoria.TRANSPORTE, 15.0),
            ),
        )
        assertEquals(7300L, d.totalCentimos)
        assertEquals(
            listOf(Categoria.ALIMENTACION, Categoria.HOGAR, Categoria.TRANSPORTE),
            d.porciones.map { it.categoria },
        )
        assertEquals(listOf(3800L, 2000L, 1500L), d.porciones.map { it.centimos })
        assertEquals(listOf(52, 27, 21), d.porciones.map { it.porcentaje })
        assertEquals(100, d.porciones.sumOf { it.porcentaje })
        assertEquals(3800.0 / 7300, d.porciones[0].fraccion, 1e-12)
    }

    @Test
    fun tresIgualesRepartenRestoPorOrdinal() {
        val d = calcular(listOf(t(Categoria.HOGAR, 1.0), t(Categoria.OCIO, 1.0), t(Categoria.ALIMENTACION, 1.0)))
        assertEquals(
            listOf(Categoria.ALIMENTACION, Categoria.HOGAR, Categoria.OCIO),
            d.porciones.map { it.categoria },
        )
        assertEquals(listOf(34, 33, 33), d.porciones.map { it.porcentaje })
        assertEquals(100, d.porciones.sumOf { it.porcentaje })
    }

    @Test
    fun seisIgualesSuman100() {
        val d = calcular(Categoria.entries.map { t(it, 5.0) })
        assertEquals(6, d.porciones.size)
        assertEquals(100, d.porciones.sumOf { it.porcentaje })
        assertEquals(listOf(17, 17, 17, 17, 16, 16), d.porciones.map { it.porcentaje })
    }

    @Test
    fun repartosDesigualesSiempreSuman100() {
        val importes = listOf(
            listOf(1.0, 2.0, 4.0), listOf(33.33, 33.33, 33.34), listOf(0.01, 0.02, 99.99),
            listOf(7.0, 11.0, 13.0, 17.0, 19.0, 23.0), listOf(1.0, 1.0, 1.0, 1.0, 1.0, 1.0),
            listOf(12.34, 56.78, 90.12, 34.56), listOf(0.07, 0.07, 0.07, 0.07, 0.07, 0.01),
        )
        for (lista in importes) {
            val d = calcular(lista.mapIndexed { i, v -> t(Categoria.entries[i], v) })
            assertEquals("lista=$lista", 100, d.porciones.sumOf { it.porcentaje })
        }
    }

    @Test
    fun empateDeRestosPrefiereMasCentimosYLuegoOrdinal() {
        // 5 / 5 / 2 de 12: bases 41, 41, 16 (suman 98); restos 8/12 8/12 8/12 empatan;
        // los 2 sobrantes van a las de mas centimos (HOGAR antes que OCIO por ordinal).
        val d = calcular(listOf(t(Categoria.OCIO, 0.05), t(Categoria.HOGAR, 0.05), t(Categoria.SALUD, 0.02)))
        assertEquals(listOf(Categoria.HOGAR, Categoria.OCIO, Categoria.SALUD), d.porciones.map { it.categoria })
        assertEquals(listOf(42, 42, 16), d.porciones.map { it.porcentaje })
    }

    @Test
    fun categoriasACeroSeOmitenYIgualesOrdenanPorOrdinal() {
        val d = calcular(listOf(t(Categoria.SALUD, 10.0), t(Categoria.OCIO, 10.0), t(Categoria.HOGAR, 0.0)))
        assertEquals(listOf(Categoria.OCIO, Categoria.SALUD), d.porciones.map { it.categoria })
        assertEquals(listOf(50, 50), d.porciones.map { it.porcentaje })
    }

    @Test
    fun listaVaciaOTodoCeroDevuelveVacia() {
        assertEquals(DistribucionCategorias.VACIA, calcular(emptyList()))
        assertEquals(DistribucionCategorias.VACIA, calcular(listOf(t(Categoria.HOGAR, 0.0))))
        assertTrue(calcular(emptyList()).estaVacia)
    }

    @Test
    fun unaSolaCategoriaEsElCienPorCien() {
        val d = calcular(listOf(t(Categoria.HOGAR, 12.5), t(Categoria.HOGAR, 7.5)))
        assertEquals(1, d.porciones.size)
        assertEquals(100, d.porciones[0].porcentaje)
        assertEquals(1.0, d.porciones[0].fraccion, 0.0)
        assertEquals(2000L, d.totalCentimos)
    }

    @Test
    fun tresDecimalesRedondeanHalfUpSinErrorBinario() {
        assertEquals(101L, calcular(listOf(t(Categoria.OTROS, 1.005))).totalCentimos)
        assertEquals(1429L, calcular(listOf(t(Categoria.OTROS, 14.285))).totalCentimos)
    }

    @Test
    fun negativosCeroNaNEInfinitoSeIgnoran() {
        val d = calcular(
            listOf(
                t(Categoria.OTROS, -5.0), t(Categoria.OTROS, Double.NaN),
                t(Categoria.OTROS, Double.POSITIVE_INFINITY), t(Categoria.OTROS, Double.NEGATIVE_INFINITY),
                t(Categoria.HOGAR, 4.0),
            ),
        )
        assertEquals(listOf(Categoria.HOGAR), d.porciones.map { it.categoria })
        assertEquals(400L, d.totalCentimos)
    }

    @Test
    fun porcionDiminutaConservaPorcentajeCeroYFraccionPositiva() {
        val d = calcular(listOf(t(Categoria.HOGAR, 1000.0), t(Categoria.OCIO, 0.01)))
        val ocio = d.porciones.last()
        assertEquals(Categoria.OCIO, ocio.categoria)
        assertEquals(0, ocio.porcentaje)
        assertTrue(ocio.fraccion > 0.0)
        assertEquals(100, d.porciones.sumOf { it.porcentaje })
    }
}

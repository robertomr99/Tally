package com.robertomr.tally.feature.resumen

import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.PorcionCategoria
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Geometría pura del donut (spec grafica-donut, T4). */
class SegmentosDonutTest {

    private fun porcion(fraccion: Double, categoria: Categoria = Categoria.OTROS) =
        PorcionCategoria(categoria, centimos = (fraccion * 10_000).toLong(), fraccion = fraccion, porcentaje = 0)

    @Test
    fun sinPorcionesNoHaySegmentos() {
        assertTrue(segmentosDonut(emptyList()).isEmpty())
    }

    @Test
    fun unaSolaPorcionEsElAnilloCompletoSinHuecoDesdeLas12() {
        val s = segmentosDonut(listOf(porcion(1.0)))
        assertEquals(listOf(SegmentoDonut(-90f, 360f)), s)
    }

    @Test
    fun barridosMasHuecosSuman360YArrancanEnMenos90() {
        val fracciones = listOf(3800.0 / 7300, 2000.0 / 7300, 1500.0 / 7300)
        val s = segmentosDonut(fracciones.map { porcion(it) }, huecoGrados = 2f)
        assertEquals(-90f, s.first().inicio, 1e-4f)
        assertEquals(360f, s.sumOf { it.barrido.toDouble() }.toFloat() + 2f * s.size, 1e-3f)
    }

    @Test
    fun losBarridosSonProporcionalesALaFraccionYSeguidos() {
        val s = segmentosDonut(listOf(porcion(0.5), porcion(0.25), porcion(0.25)), huecoGrados = 2f)
        assertEquals(178f, s[0].barrido, 1e-4f)
        assertEquals(88f, s[1].barrido, 1e-4f)
        assertEquals(-90f + 180f, s[1].inicio, 1e-4f)
        assertEquals(-90f + 270f, s[2].inicio, 1e-4f)
    }

    @Test
    fun unaPorcionDiminutaTieneBarridoMinimoDeUnGrado() {
        val s = segmentosDonut(listOf(porcion(0.9999), porcion(0.0001)), huecoGrados = 2f)
        assertEquals(1f, s[1].barrido, 0f)
    }
}

package com.robertomr.tally.feature.escanear

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests JVM de la geometria pura del encuadre (spec captura-recorte-encuadre,
 * T1): proporcion/centrado del recuadro y su rect normalizado [0,1] respecto a
 * la vista (el que se recorta de la foto).
 */
class EncuadreGeometriaTest {

    @Test
    fun zonaIsCenteredAndCapsWidthAndHeight() {
        val zona = EncuadreGeometria.zona(
            anchoVista = 1000f,
            altoVista = 2000f,
            margenPx = 10f,
            anchoMaxPx = 300f,
            altoFrac = 0.68f,
            altoMaxPx = 900f,
        )

        // ancho = min(1000 - 20, 300) = 300; alto = min(1360, 900) = 900.
        assertEquals(300f, zona.right - zona.left, 1e-6f)
        assertEquals(900f, zona.bottom - zona.top, 1e-6f)
        assertEquals(350f, zona.left, 1e-6f)
        assertEquals(650f, zona.right, 1e-6f)
        assertEquals(550f, zona.top, 1e-6f)
        assertEquals(1450f, zona.bottom, 1e-6f)
        // Centrado en la vista.
        assertEquals(500f, (zona.left + zona.right) / 2, 1e-6f)
        assertEquals(1000f, (zona.top + zona.bottom) / 2, 1e-6f)
    }

    @Test
    fun zonaUsesMarginsWhenWidthCapDoesNotApply() {
        val zona = EncuadreGeometria.zona(
            anchoVista = 200f,
            altoVista = 300f,
            margenPx = 20f,
            anchoMaxPx = 500f,
            altoFrac = 0.5f,
            altoMaxPx = 500f,
        )

        // ancho = min(160, 500) = 160 (manda el margen); alto = 150 (fraccion).
        assertEquals(160f, zona.right - zona.left, 1e-6f)
        assertEquals(150f, zona.bottom - zona.top, 1e-6f)
        assertEquals(20f, zona.left, 1e-6f)
        assertEquals(75f, zona.top, 1e-6f)
    }

    @Test
    fun rectNormalizadoMapsZonaToUnitCoordinates() {
        val zona = EncuadreGeometria.zona(
            anchoVista = 1000f,
            altoVista = 2000f,
            margenPx = 10f,
            anchoMaxPx = 300f,
            altoFrac = 0.68f,
            altoMaxPx = 900f,
        )

        val rect = EncuadreGeometria.rectNormalizado(zona, anchoVista = 1000f, altoVista = 2000f)

        assertEquals(0.35f, rect.left, 1e-6f)
        assertEquals(0.275f, rect.top, 1e-6f)
        assertEquals(0.65f, rect.right, 1e-6f)
        assertEquals(0.725f, rect.bottom, 1e-6f)
    }
}

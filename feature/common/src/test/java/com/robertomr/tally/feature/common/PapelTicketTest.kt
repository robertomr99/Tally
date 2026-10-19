package com.robertomr.tally.feature.common

import org.junit.Assert.assertEquals
import org.junit.Test

class PapelTicketTest {

    @Test
    fun `el numero de dientes se ajusta al ancho y nunca baja de uno`() {
        assertEquals(1, numeroDeDientes(ancho = 0f, dienteAncho = 12f))
        assertEquals(1, numeroDeDientes(ancho = 5f, dienteAncho = 12f))
        assertEquals(30, numeroDeDientes(ancho = 360f, dienteAncho = 12f))
        assertEquals(29, numeroDeDientes(ancho = 350f, dienteAncho = 12f))
    }

    @Test
    fun `el importe de una fila es cantidad por precio unitario`() {
        assertEquals(5.97, importeDeLinea(3, 1.99), 1e-9)
        assertEquals(4.5, importeDeLinea(1, 4.5), 1e-9)
        assertEquals(0.0, importeDeLinea(2, 0.0), 1e-9)
    }
}

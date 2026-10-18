package com.robertomr.tally.domain.expenses.entities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests JVM de la logica pura de lineas editables (spec edicion-lineas,
 * verificacion 3 / escenario 3): sumas, cantidad >= 1, precio >= 0, filtrado de
 * lineas vacias e ids de fila.
 */
class LineaEditableTest {

    @Test
    fun totalRecalculadoSumaCantidadPorPrecio() {
        val lineas = listOf(
            LineaEditable(0, "LECHE", 2, 1.05),
            LineaEditable(1, "PAN", 1, 0.90),
            LineaEditable(2, "HUEVOS", 3, 2.50),
        )

        assertEquals(2 * 1.05 + 0.90 + 3 * 2.50, totalRecalculado(lineas), 1e-9)
    }

    @Test
    fun totalRecalculadoSinLineasEsCero() {
        assertEquals(0.0, totalRecalculado(emptyList()), 0.0)
    }

    @Test
    fun lineasEditablesInicialesAsignaIdsCorrelativosYConservaValores() {
        val editables = lineasEditablesIniciales(
            listOf(Linea("LECHE", 2, 1.05), Linea("PAN", 1, 0.90)),
        )

        assertEquals(listOf(0L, 1L), editables.map { it.id })
        assertEquals(
            listOf(
                LineaEditable(0, "LECHE", 2, 1.05),
                LineaEditable(1, "PAN", 1, 0.90),
            ),
            editables,
        )
        assertTrue(lineasEditablesIniciales(emptyList()).isEmpty())
    }

    @Test
    fun lineasEditablesInicialesAcotaCantidadYPrecioDefensivamente() {
        val editables = lineasEditablesIniciales(listOf(Linea("X", 0, -1.0)))

        assertEquals(1, editables.single().cantidad)
        assertEquals(0.0, editables.single().precioUnitario, 0.0)
    }

    @Test
    fun nuevaLineaEditableEsFilaVaciaConElIdDado() {
        assertEquals(LineaEditable(0, "", 1, 0.0), nuevaLineaEditable(0))

        val nueva = nuevaLineaEditable(5)
        assertEquals(5L, nueva.id)
        assertEquals("", nueva.concepto)
        assertEquals(1, nueva.cantidad)
        assertEquals(0.0, nueva.precioUnitario, 0.0)
    }

    @Test
    fun parsearCantidadSoloAceptaEnterosMayoresOIgualesAUno() {
        assertEquals(3, parsearCantidad(" 3 "))
        listOf("", "abc", "0", "-3", "2,5", "2.5").forEach {
            assertNull(it, parsearCantidad(it))
        }
    }

    @Test
    fun parsearPrecioVacioEsCeroYLosInvalidosSonNull() {
        assertEquals(0.0, parsearPrecio("")!!, 0.0)
        assertEquals(0.0, parsearPrecio("   ")!!, 0.0)
        assertEquals(0.9, parsearPrecio("0,90")!!, 1e-9)
        assertEquals(1.5, parsearPrecio("1.5")!!, 1e-9)
        listOf("abc", "-1", "NaN", "Infinity").forEach {
            assertNull(it, parsearPrecio(it))
        }
    }

    @Test
    fun aLineasDescartaConceptosEnBlancoYAcotaCantidadYPrecio() {
        val editadas = listOf(
            LineaEditable(0, "LECHE", 2, 1.05),
            LineaEditable(1, "", 1, 3.0),
            LineaEditable(2, "   ", 4, 1.0),
            LineaEditable(3, " CAFE ", 0, -2.0),
        )

        assertEquals(
            listOf(
                Linea("LECHE", 2, 1.05),
                Linea("CAFE", 1, 0.0),
            ),
            editadas.aLineas(),
        )
    }

    @Test
    fun aLineasDeListaVaciaEsVacia() {
        assertTrue(emptyList<LineaEditable>().aLineas().isEmpty())
    }
}

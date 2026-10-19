package com.robertomr.tally.feature.common

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Etiquetas del listado (spec listado-gastos, T4). Hoy = jueves 2026-10-22. */
class EtiquetaDiaTest {

    private val hoy = LocalDate.of(2026, 10, 22)

    @Test
    fun hoyYAyer() {
        assertEquals("Hoy, 22 de octubre", etiquetaDia(hoy, hoy))
        assertEquals("Ayer, 21 de octubre", etiquetaDia(LocalDate.of(2026, 10, 21), hoy))
    }

    @Test
    fun otroDiaLlevaElDiaDeLaSemana() {
        assertEquals("Lunes, 19 de octubre", etiquetaDia(LocalDate.of(2026, 10, 19), hoy))
        assertEquals("Domingo, 4 de octubre", etiquetaDia(LocalDate.of(2026, 10, 4), hoy))
        assertEquals("Jueves, 1 de enero", etiquetaDia(LocalDate.of(2026, 1, 1), hoy))
    }

    @Test
    fun elAnioSoloApareceSiDifiereDelActual() {
        assertEquals("Martes, 30 de diciembre de 2025", etiquetaDia(LocalDate.of(2025, 12, 30), hoy))
        assertEquals("Ayer, 31 de diciembre de 2025", etiquetaDia(LocalDate.of(2025, 12, 31), LocalDate.of(2026, 1, 1)))
    }

    @Test
    fun todosLosMesesEnEspanol() {
        val esperados = listOf(
            "enero", "febrero", "marzo", "abril", "mayo", "junio",
            "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
        )
        esperados.forEachIndexed { i, mes ->
            assertEquals(true, etiquetaDia(LocalDate.of(2026, i + 1, 10), hoy).contains("10 de $mes"))
        }
    }

    @Test
    fun fechaCorta() {
        assertEquals("24 oct", etiquetaFechaCorta(LocalDate.of(2026, 10, 24), hoy))
        assertEquals("3 dic 2025", etiquetaFechaCorta(LocalDate.of(2025, 12, 3), hoy))
    }

    @Test
    fun horaOcultaSoloEnMedianoche() {
        assertEquals("14:32", textoHora(LocalDateTime.of(2026, 10, 24, 14, 32)))
        assertEquals("00:05", textoHora(LocalDateTime.of(2026, 10, 24, 0, 5)))
        assertNull(textoHora(LocalDateTime.of(2026, 10, 24, 0, 0)))
    }
}

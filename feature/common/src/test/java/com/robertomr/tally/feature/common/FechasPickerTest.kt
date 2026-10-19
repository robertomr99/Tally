package com.robertomr.tally.feature.common

import java.time.LocalDate
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/** Conversión LocalDate <-> millis UTC del date picker (spec rango-personalizado, escenario 7). */
class FechasPickerTest {

    private val zonaOriginal = TimeZone.getDefault()

    @After
    fun restauraZona() = TimeZone.setDefault(zonaOriginal)

    private val fechas = listOf(
        LocalDate.of(2026, 10, 18),
        LocalDate.of(2028, 2, 29),
        LocalDate.of(2025, 12, 31),
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 3, 29), // cambio de hora en Madrid
    )

    @Test
    fun medianocheUtcConocida() {
        assertEquals(1_792_281_600_000L, LocalDate.of(2026, 10, 18).aMillisUtc())
        assertEquals(LocalDate.of(1970, 1, 1), 0L.aLocalDateUtc())
    }

    @Test
    fun idaYVueltaEsExactaEnCualquierZonaPorDefecto() {
        listOf("Europe/Madrid", "Pacific/Auckland", "America/Los_Angeles", "UTC").forEach { zona ->
            TimeZone.setDefault(TimeZone.getTimeZone(zona))
            fechas.forEach { assertEquals("$zona $it", it, it.aMillisUtc().aLocalDateUtc()) }
        }
    }

    @Test
    fun cualquierInstanteDelDiaUtcResuelveAEseDia() {
        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Auckland"))
        val ms = LocalDate.of(2026, 10, 18).aMillisUtc()
        assertEquals(LocalDate.of(2026, 10, 18), (ms + 23 * 3_600_000L).aLocalDateUtc())
        assertEquals(LocalDate.of(2026, 10, 17), (ms - 1).aLocalDateUtc())
    }
}

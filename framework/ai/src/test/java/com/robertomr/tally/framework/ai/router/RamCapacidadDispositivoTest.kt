package com.robertomr.tally.framework.ai.router

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RamCapacidadDispositivoTest {
    @Test
    fun fronteraDelUmbral() {
        assertFalse(ramSuficiente(UMBRAL_RAM_BYTES - 1))
        assertTrue(ramSuficiente(UMBRAL_RAM_BYTES))
    }

    @Test
    fun tabS9PlusPasaYUnMovilDe8GbNo() {
        assertTrue(ramSuficiente(11_454_088L * 1024))
        assertFalse(ramSuficiente(8L * 1024 * 1024 * 1024))
    }
}

package com.robertomr.tally

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SplashPoliticaTest {

    private fun mantener(
        primera: Boolean = true,
        sdk: Int = 36,
        escala: Float = 1f,
        ms: Long = 0L,
    ) = SplashPolitica.debeMantener(primera, sdk, escala, ms)

    @Test
    fun `primera creacion con animaciones mantiene el splash`() {
        assertTrue(mantener(ms = 0L))
        assertTrue(mantener(ms = 699L))
    }

    @Test
    fun `al llegar al techo de 700 ms se libera`() {
        assertFalse(mantener(ms = 700L))
        assertFalse(mantener(ms = 5_000L))
    }

    @Test
    fun `escala de animacion cero no retiene`() {
        assertFalse(mantener(escala = 0f))
    }

    @Test
    fun `escalas distintas de cero retienen`() {
        assertTrue(mantener(escala = 0.5f))
        assertTrue(mantener(escala = 2f))
    }

    @Test
    fun `por debajo de Android 12 no retiene`() {
        assertFalse(mantener(sdk = 30))
        assertFalse(mantener(sdk = 26))
        assertTrue(mantener(sdk = 31))
    }

    @Test
    fun `recreacion o proceso vivo no retiene`() {
        assertFalse(mantener(primera = false))
    }
}

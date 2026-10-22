package com.robertomr.tally.feature.gastos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Texto del diálogo de borrado: un gasto manual no menciona la foto (spec mejoras-visuales-3, W07). */
class TextoConfirmarBorradoTest {

    @Test
    fun gastoManualNoMencionaLaFoto() {
        val texto = textoConfirmarBorrado("Bar Pepe", "12,50 €", esManual = true)
        assertFalse(texto.contains("foto"))
        assertEquals(
            "Se eliminará «Bar Pepe» por 12,50 € junto con sus líneas. Esta acción no se puede deshacer.",
            texto,
        )
    }

    @Test
    fun ticketConFotoMencionaLaFoto() {
        val texto = textoConfirmarBorrado("Mercadona", "35,30 €", esManual = false)
        assertTrue(texto.contains("sus líneas y su foto"))
        assertEquals(
            "Se eliminará «Mercadona» por 35,30 € junto con sus líneas y su foto. Esta acción no se puede deshacer.",
            texto,
        )
    }

    @Test
    fun nombreResueltoEImporteAparecenTalCual() {
        val texto = textoConfirmarBorrado("este ticket", "0,00 €", esManual = false)
        assertTrue(texto.contains("«este ticket»"))
        assertTrue(texto.contains("por 0,00 €"))
    }
}

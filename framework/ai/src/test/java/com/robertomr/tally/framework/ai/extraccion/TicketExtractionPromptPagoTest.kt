package com.robertomr.tally.framework.ai.extraccion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests JVM del bloque REGLAS DE PAGO del prompt (spec forma-pago). Los literales
 * previos del prompt siguen cubiertos por [TicketExtractionPromptTest]; aqui solo
 * se anaden los nuevos y se comprueba que la REGLA DEL TOTAL no se ha tocado.
 */
class TicketExtractionPromptPagoTest {

    private val prompt = TicketExtractionPrompt.buildPrompt("TEXTO")

    @Test
    fun promptDeclaraLasClavesYValoresDePago() {
        listOf("forma_pago", "entregado", "cambio").forEach { clave ->
            assertTrue("falta la clave '$clave'", prompt.contains("\"$clave\""))
        }
        listOf("EFECTIVO", "TARJETA", "NO_INDICADA", "datáfono", "A DEVOLVER").forEach { literal ->
            assertTrue("falta '$literal'", prompt.contains(literal))
        }
    }

    @Test
    fun reglasDePagoEstanSeparadasDeLaReglaDelTotal() {
        val iniTotal = prompt.indexOf("REGLA DEL TOTAL")
        val iniPago = prompt.indexOf("REGLAS DE PAGO")
        val iniAdicionales = prompt.indexOf("Reglas adicionales:")
        assertTrue(iniTotal in 0 until iniPago)
        assertTrue(iniPago < iniAdicionales)
        val bloqueTotal = prompt.substring(iniTotal, iniPago)
        // La REGLA DEL TOTAL conserva sus literales y no menciona entregado/cambio.
        assertTrue(bloqueTotal.contains("SIEMPRE el total CON IVA"))
        assertTrue(bloqueTotal.contains("NUNCA uses la base imponible"))
        assertTrue(bloqueTotal.contains("el total es 3.50 (nunca 3.36)"))
        assertTrue(!bloqueTotal.contains("entregado"))
    }

    @Test
    fun elPromptLocalHeredaLasReglasDePago() {
        val local = TicketExtractionPrompt.buildLocalPrompt("TEXTO")
        assertTrue(local.contains("REGLAS DE PAGO"))
        assertTrue(local.startsWith(prompt))
    }

    @Test
    fun responseSchemaSeConstruyeConLosCamposDePago() {
        assertNotNull(TicketExtractionPrompt.responseSchema())
        assertEquals(true, prompt.contains("NO_INDICADA"))
    }
}

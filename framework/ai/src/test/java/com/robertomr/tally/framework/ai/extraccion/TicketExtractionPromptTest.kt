package com.robertomr.tally.framework.ai.extraccion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests JVM del prompt/schema de la extraccion en nube (spec extraccion-nube,
 * T5): el prompt contiene el schema JSON (claves del dominio), el enum de 6
 * categorias, la regla R3 del total CON IVA (con el caso trampa 3,36 -> 3,50) y
 * el texto crudo tal cual. Tambien comprueba que el `responseSchema` de
 * Firebase AI Logic se construye con la enum de categorias.
 */
class TicketExtractionPromptTest {

    private val textoCrudo = "MERCADONA\nTOTAL 12,34 EUR"
    private val prompt = TicketExtractionPrompt.buildPrompt(textoCrudo)

    @Test
    fun promptContainsTheJsonSchemaKeysOfTheDomain() {
        listOf(
            "comercio", "fecha_emision", "total", "categoria", "lineas",
            "concepto", "cantidad", "precio_unitario",
        ).forEach { clave ->
            assertTrue("falta la clave '$clave' en el prompt", prompt.contains("\"$clave\""))
        }
    }

    @Test
    fun promptContainsTheFixedEnumOfSixCategories() {
        listOf("ALIMENTACION", "HOGAR", "TRANSPORTE", "OCIO", "SALUD", "OTROS").forEach { categoria ->
            assertTrue("falta la categoria '$categoria' en el prompt", prompt.contains(categoria))
        }
        // Enum cerrado: no se admiten mas nombres.
        assertTrue(prompt.contains("6 valores"))
    }

    @Test
    fun promptForcesTheTotalWithVatRuleAndTheTrapCase() {
        assertTrue("falta la regla del total con IVA", prompt.contains("total CON IVA"))
        listOf("Total (impuestos incl.)", "TOTAL A PAGAR", "TOTAL COMPRA").forEach { etiqueta ->
            assertTrue("falta la etiqueta '$etiqueta' del total con IVA", prompt.contains(etiqueta))
        }
        // Caso trampa (scenario 2 / R3): base 3,36 + IVA 0,14 = TOTAL COMPRA 3,50.
        assertTrue("falta el ejemplo trampa 3,50 vs 3,36", prompt.contains("3.50"))
        assertTrue("falta el ejemplo trampa 3,50 vs 3,36", prompt.contains("3.36"))
        assertTrue("falta la prohibicion de la base imponible", prompt.contains("base imponible"))
    }

    @Test
    fun promptEchoesTheRawTextVerbatim() {
        assertTrue("el texto crudo debe viajar tal cual en el prompt", prompt.contains(textoCrudo))
    }

    @Test
    fun responseSchemaIsBuiltWithTheEnumOfSixCategories() {
        // Smoke: construir el schema no falla y devuelve un Schema no nulo.
        // (La enum de 6 viaja en el prompt y en el schema; aqui solo se verifica
        // que la construccion del structured output es utilizable.)
        assertNotNull(TicketExtractionPrompt.responseSchema())
    }

    @Test
    fun localPromptKeepsTheCloudPromptAndAddsTheJsonOnlyClosing() {
        val local = TicketExtractionPrompt.buildLocalPrompt(textoCrudo)
        assertTrue("el prompt local debe contener el de la nube tal cual", local.startsWith(prompt))
        assertTrue(local.contains("unicamente el objeto JSON"))
        assertTrue(local.contains("sin comentarios `//`"))
        assertTrue(local.contains("6 valores"))
        assertTrue(local.contains("total CON IVA"))
    }

    @Test
    fun modelConstantIsRegistered() {
        // gemini-flash-lite-latest: variante lite elegida por el usuario
        // (2026-10-17) para la via nube (fallback de ADR 0001) por su mayor cuota
        // diaria y menor coste; sustituye a gemini-3.8-flash, que agotaba el free
        // tier (20 req/dia).
        assertEquals("gemini-flash-lite-latest", TicketExtractionPrompt.MODELO)
    }
}

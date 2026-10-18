package com.robertomr.tally.framework.ai.extraccion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests JVM del detector de objeto JSON completo (spec extraccion-local, scenario 2). */
class JsonObjectStreamDetectorTest {

    @Test
    fun detectsTheTopLevelClosingBraceWithNesting() {
        val d = JsonObjectStreamDetector()
        val json = """{"a": {"b": [1, {"c": 2}]}, "d": 3}"""
        assertFalse(d.accept(json.dropLast(1)))
        assertTrue(d.accept(json + " texto basura"))
        assertEquals(json.length, d.endIndex)
    }

    @Test
    fun ignoresBracesInsideStringsAndEscapedQuotes() {
        val d = JsonObjectStreamDetector()
        val json = """{"concepto": "caja } con \" y { llaves", "n": 1}"""
        assertFalse(d.accept(json.substring(0, json.length - 2)))
        assertTrue(d.accept(json))
        assertEquals(json.length, d.endIndex)
    }

    @Test
    fun ignoresProseBeforeTheObjectAndFences() {
        val d = JsonObjectStreamDetector()
        val texto = "Claro, aqui tienes:\n```json\n{\"x\": 1}\n```"
        assertTrue(d.accept(texto))
        assertEquals("Claro, aqui tienes:\n```json\n{\"x\": 1}", texto.substring(0, d.endIndex))
    }

    @Test
    fun truncatedObjectIsNeverComplete() {
        val d = JsonObjectStreamDetector()
        assertFalse(d.accept("{\"a\": {\"b\": 1}, \"c\": \"sin cerrar"))
        assertFalse(d.isComplete)
        assertEquals(-1, d.endIndex)
    }

    @Test
    fun worksIncrementallyChunkByChunk() {
        val d = JsonObjectStreamDetector()
        val json = """{"a": "}", "b": {"c": 1}}"""
        var acumulado = ""
        var detenidoEn = -1
        json.forEachIndexed { i, c ->
            acumulado += c
            if (detenidoEn < 0 && d.accept(acumulado)) detenidoEn = i
        }
        assertEquals(json.length - 1, detenidoEn)
    }

    @Test
    fun endOfTurnMarkerStopsGeneration() {
        val d = JsonObjectStreamDetector()
        val texto = "{\"a\": 1<end_of_turn>"
        assertTrue(d.accept(texto))
        assertEquals("{\"a\": 1", texto.substring(0, d.endIndex))
    }

    @Test
    fun markerSplitAcrossChunksIsDetected() {
        val d = JsonObjectStreamDetector()
        assertFalse(d.accept("texto <end_of"))
        assertTrue(d.accept("texto <end_of_turn>"))
    }

    @Test
    fun recortarCutsAtFirstCompleteObjectOrReturnsTextUntouched() {
        assertEquals("""{"a":1}""", JsonObjectStreamDetector.recortar("""{"a":1} y {"b":2}"""))
        assertEquals("""{"a":""", JsonObjectStreamDetector.recortar("""{"a":"""))
    }
}

package com.robertomr.tally.domain.expenses.entities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Tests JVM del parser/validador compartido (spec extraccion-nube, T4 =
 * verificacion 2): respuestas validas normalizadas (incluidas tolerancias del
 * contenedor) y respuestas malformadas rechazadas con
 * [InvalidExtractionException] y motivo claro (scenario 4).
 */
class TicketExtraidoParserTest {

    // --- respuestas validas ---------------------------------------------------

    @Test
    fun validResponseParsesIntoANormalizedExtraction() {
        val respuesta = """
            {
              "comercio": "  MERCADONA ",
              "fecha_emision": "2026-10-03T15:32:00",
              "total": 32.09,
              "categoria": "ALIMENTACION",
              "lineas": [
                {"concepto": "AGUA", "cantidad": 2, "precio_unitario": 0.50},
                {"concepto": "CERVEZA", "cantidad": 1, "precio_unitario": 1.50}
              ]
            }
        """.trimIndent()

        val extraido = TicketExtraidoParser.parse(respuesta, Motor.NUBE)

        assertEquals("MERCADONA", extraido.comercio)
        assertEquals(java.time.LocalDateTime.of(2026, 10, 3, 15, 32, 0), extraido.fechaEmision)
        assertEquals(32.09, extraido.total!!, 0.0001)
        assertEquals(Categoria.ALIMENTACION, extraido.categoria)
        assertEquals(Motor.NUBE, extraido.motor)
        assertEquals(2, extraido.lineas.size)
        assertEquals(Linea("AGUA", 2, 0.50), extraido.lineas[0])
        assertEquals(Linea("CERVEZA", 1, 1.50), extraido.lineas[1])
    }

    @Test
    fun fencedJsonWithSurroundingProseIsTolerated() {
        val respuesta = """
            Claro, aqui tienes la extraccion del ticket:
            ```json
            {"comercio": "AEROPUERTO", "fecha_emision": "2026-10-07", "total": 3.50,
             "categoria": "ALIMENTACION", "lineas": []}
            ```
            Espero que sirva.
        """.trimIndent()

        val extraido = TicketExtraidoParser.parse(respuesta, Motor.LOCAL)

        assertEquals("AEROPUERTO", extraido.comercio)
        // Fecha sola ISO = comienzo del dia.
        assertEquals(java.time.LocalDateTime.of(2026, 10, 7, 0, 0, 0), extraido.fechaEmision)
        assertEquals(3.50, extraido.total!!, 0.0001)
        assertEquals(Motor.LOCAL, extraido.motor)
        assertTrue(extraido.lineas.isEmpty())
    }

    @Test
    fun spanishDecimalCommasAndStringNumbersAreNormalized() {
        val respuesta = """
            {"comercio": "OZONE", "fecha_emision": "2026-10-12T19:13:05", "total": "12,64",
             "categoria": "ocio",
             "lineas": [{"concepto": "TACOS DE POLLO", "cantidad": "3", "precio_unitario": "1.234,56"}]}
        """.trimIndent()

        val extraido = TicketExtraidoParser.parse(respuesta, Motor.NUBE)

        assertEquals(12.64, extraido.total!!, 0.0001)
        assertEquals(Categoria.OCIO, extraido.categoria) // match case-insensitive
        assertEquals(Linea("TACOS DE POLLO", 3, 1234.56), extraido.lineas[0])
    }

    @Test
    fun omittedCantidadDefaultsToOneAndUnknownKeysAreIgnored() {
        val respuesta = """
            {"comercio": "GRUPO", "fecha_emision": "2026-10-11", "total": 7.91,
             "categoria": "OTROS", "descuento_aplicado": "no se",
             "lineas": [{"concepto": "LECHE", "precio_unitario": 7.91}]}
        """.trimIndent()

        val extraido = TicketExtraidoParser.parse(respuesta, Motor.NUBE)

        assertEquals(Linea("LECHE", 1, 7.91), extraido.lineas[0])
    }

    @Test
    fun missingOptionalFieldsYieldEmptyValues() {
        val respuesta = """{"categoria": "HOGAR"}"""

        val extraido = TicketExtraidoParser.parse(respuesta, Motor.NUBE)

        assertEquals("", extraido.comercio)
        assertNull(extraido.fechaEmision)
        assertNull(extraido.total)
        assertTrue(extraido.lineas.isEmpty())
        assertEquals(Categoria.HOGAR, extraido.categoria)
    }

    // --- respuestas malformadas -> InvalidExtractionException -----------------

    @Test
    fun responseWithoutAJsonObjectIsRejected() {
        assertInvalid("Lo siento, no puedo extraer los datos de este ticket.", "objeto JSON")
    }

    @Test
    fun malformedJsonIsRejected() {
        assertInvalid("{\"comercio\": \"MERCADONA\", ", "JSON malformado")
    }

    @Test
    fun missingCategoriaIsRejected() {
        assertInvalid(
            """{"comercio": "MERCADONA", "total": 3.50}""",
            "categoria ausente",
        )
    }

    @Test
    fun categoriaOutsideTheFixedEnumIsRejected() {
        assertInvalid(
            """{"comercio": "MERCADONA", "categoria": "SUPERMERCADOS"}""",
            "fuera de la enum",
        )
    }

    @Test
    fun nonIsoFechaEmisionIsRejected() {
        assertInvalid(
            """{"fecha_emision": "07/10/2026", "categoria": "OTROS"}""",
            "no es una fecha ISO",
        )
    }

    @Test
    fun negativeTotalIsRejected() {
        assertInvalid(
            """{"total": -3.50, "categoria": "OTROS"}""",
            "total negativo",
        )
    }

    @Test
    fun nonNumericTotalIsRejected() {
        assertInvalid(
            """{"total": "doce euros", "categoria": "OTROS"}""",
            "total no es numerico",
        )
    }

    @Test
    fun infiniteTotalIsRejected() {
        assertInvalid(
            """{"total": "Infinity", "categoria": "OTROS"}""",
            "total no es finito",
        )
    }

    @Test
    fun lineWithoutConceptoIsRejected() {
        assertInvalid(
            """{"categoria": "OTROS", "lineas": [{"cantidad": 1, "precio_unitario": 1.0}]}""",
            "linea sin concepto",
        )
    }

    @Test
    fun blankConceptoIsRejected() {
        assertInvalid(
            """{"categoria": "OTROS", "lineas": [{"concepto": "   ", "precio_unitario": 1.0}]}""",
            "linea sin concepto",
        )
    }

    @Test
    fun cantidadLessThanOneIsRejected() {
        assertInvalid(
            """{"categoria": "OTROS", "lineas": [{"concepto": "AGUA", "cantidad": 0, "precio_unitario": 1.0}]}""",
            "cantidad < 1",
        )
    }

    @Test
    fun negativePrecioUnitarioIsRejected() {
        assertInvalid(
            """{"categoria": "OTROS", "lineas": [{"concepto": "AGUA", "precio_unitario": -1.0}]}""",
            "precio_unitario negativo",
        )
    }

    @Test
    fun missingPrecioUnitarioIsRejected() {
        // Interpretacion registrada en Implementation Notes del spec: el schema
        // exige precio_unitario en cada linea (Linea.precioUnitario no admite
        // null); su ausencia invalida la respuesta en vez de inventar 0.0.
        assertInvalid(
            """{"categoria": "OTROS", "lineas": [{"concepto": "AGUA", "cantidad": 1}]}""",
            "precio_unitario ausente",
        )
    }

    // --- helpers --------------------------------------------------------------

    private fun assertInvalid(respuesta: String, fragmentoMotivo: String) {
        try {
            TicketExtraidoParser.parse(respuesta, Motor.NUBE)
            fail("esperaba InvalidExtractionException para: $respuesta")
        } catch (e: InvalidExtractionException) {
            assertTrue(
                "el motivo '${e.message}' no contiene '$fragmentoMotivo'",
                e.message.orEmpty().contains(fragmentoMotivo),
            )
        }
    }
}

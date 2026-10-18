package com.robertomr.tally.framework.ai.extraccion

import com.google.firebase.ai.type.Schema

/**
 * Prompt + schema de la extraccion estructurada en nube (spec extraccion-nube,
 * T5): comparte forma con el que usara `extraccion-local` (mismo schema, mismas
 * reglas) para que ambos motores sean comparables (ADR 0002).
 *
 * Regla R3 (docs/domain-model.md, deuda de `ocr-mlkit`): el `total` es SIEMPRE
 * el total CON IVA (Total (impuestos incl.) / TOTAL A PAGAR / TOTAL COMPRA),
 * nunca la base imponible ni la cuota de IVA. El caso trampa es el ticket 2:
 * "3,36 IVA: 0,14" + "TOTAL COMPRA 3,50" -> total 3.50, no 3.36.
 */
internal object TicketExtractionPrompt {

    /**
     * Modelo Gemini de la via nube (U3 del spec). ORIGINAL: `gemini-2.5-flash`.
     * El primer run real de T7 (2026-10-17) recibio
     * `ServerException: This model models/gemini-2.5-flash is no longer
     * available to new users. Please update your code to use
     * models/gemini-3.8-flash` (proyecto nuevo `tally-586d1`), asi que se paso a
     * `gemini-3.8-flash`.
     *
     * DECISION DEL USUARIO (2026-10-17): usar una variante **lite** (via nube).
     * `gemini-3.8-flash` agoto el free tier muy rapido (limite 20 req/dia; una
     * sola corrida de la suite real gasta ~5), lo que hacia inviable verificar
     * sin quedarse sin cuota. Las variantes lite tienen una cuota diaria bastante
     * mayor y menor coste por token. Encaja con el plan de producto: el motor
     * **Local (Gemma on-device)** es el principal y la nube es solo el
     * **fallback** (ADR 0001, `router-automatico`).
     */
    const val MODELO = "gemini-flash-lite-latest"

    /** Claves del schema JSON, en el mismo orden que [responseSchema]. */
    private val CLAVES_SCHEMA = listOf(
        "comercio", "fecha_emision", "total", "categoria", "lineas",
        "concepto", "cantidad", "precio_unitario",
    )

    /** Enum fija de 6 categorias (CONTEXT.md); misma lista que [Categoria]. */
    private val CATEGORIAS = listOf(
        "ALIMENTACION", "HOGAR", "TRANSPORTE", "OCIO", "SALUD", "OTROS",
    )

    /**
     * Construye el prompt en español a partir del texto crudo del OCR: instrucciones,
     * schema JSON, enum de categorias, regla del total con IVA y el texto tal cual.
     */
    fun buildPrompt(textoCrudo: String): String = """
        Eres un extractor de datos de tickets de compra en español. A partir del TEXTO CRUDO
        de un ticket (salida de OCR, con el ruido tipico del papel termico) debes devolver
        UN SOLO objeto JSON valido, sin prosa ni comentarios, con este schema exacto:

        {
          "comercio": string,        // nombre del comercio; "" si no aparece
          "fecha_emision": string,   // emision ISO: "yyyy-MM-dd" o "yyyy-MM-dd'T'HH:mm[:ss]"; omite el campo si no es legible
          "total": number,           // TOTAL CON IVA en euros; omite el campo si no aparece
          "categoria": string,       // uno de: ${CATEGORIAS.joinToString(", ")}
          "lineas": [ {"concepto": string, "cantidad": integer, "precio_unitario": number} ]
        }

        REGLA DEL TOTAL (muy importante): "total" es SIEMPRE el total CON IVA, el importe
        final que se paga ("Total (impuestos incl.)", "TOTAL A PAGAR", "TOTAL COMPRA" o
        "TOTAL"). NUNCA uses la base imponible ni la cuota de IVA. Ejemplo: si el ticket
        muestra "3,36 IVA: 0,14" y "TOTAL COMPRA 3,50", el total es 3.50 (nunca 3.36).

        Reglas adicionales:
        - "categoria" es OBLIGATORIA y debe ser exactamente uno de estos 6 valores:
          ${CATEGORIAS.joinToString(", ")}.
        - Cada linea necesita "concepto" no vacio, "cantidad" entera >= 1 y
          "precio_unitario" numerico >= 0 con IVA incluido.
        - No inventes datos que no esten en el texto; si un dato no aparece, omite el campo.
        - Devuelve siempre un JSON valido con las claves ${CLAVES_SCHEMA.joinToString(", ")}.

        TEXTO CRUDO DEL TICKET:
        <<<
        $textoCrudo
        >>>
    """.trimIndent()

    /**
     * Prompt de la via LOCAL (spec extraccion-local, D4): el mismo de la nube
     * (mismo schema, enum y regla del total con IVA: comparativa honesta, ADR 0002)
     * + cierre explicito, porque Gemma local no tiene structured output.
     */
    fun buildLocalPrompt(textoCrudo: String): String =
        buildPrompt(textoCrudo) + "\n\n" + CIERRE_LOCAL

    private const val CIERRE_LOCAL =
        "Devuelve unicamente el objeto JSON, sin texto antes ni despues y sin comentarios `//`."

    /**
     * `responseSchema` de Firebase AI Logic (structured output): obliga al modelo
     * al JSON del prompt. `fecha_emision`, `total`, `comercio` y `lineas` son
     * opcionales (pueden no aparecer en el ticket); `categoria` es obligatoria y
     * de enum cerrado. Construccion bajo demanda (los tests JVM del prompt no
     * cargan clases de Firebase).
     */
    fun responseSchema(): Schema = Schema.obj(
        properties = mapOf(
            "comercio" to Schema.string(description = "Nombre del comercio; cadena vacia si no aparece"),
            "fecha_emision" to Schema.string(
                description = "Fecha de emision ISO (yyyy-MM-dd o yyyy-MM-dd'T'HH:mm[:ss]); omitida si no es legible",
            ),
            "total" to Schema.double(description = "TOTAL CON IVA en euros (Total (impuestos incl.) / TOTAL A PAGAR / TOTAL COMPRA); nunca la base imponible"),
            "categoria" to Schema.enumeration(
                values = CATEGORIAS,
                description = "Categoria del ticket (enum fija de 6)",
            ),
            "lineas" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "concepto" to Schema.string(description = "Concepto del producto; no vacio"),
                        "cantidad" to Schema.integer(description = "Unidades; entero >= 1", minimum = 1.0),
                        "precio_unitario" to Schema.double(description = "Precio unitario con IVA; >= 0", minimum = 0.0),
                    ),
                ),
                description = "Lineas del ticket (puede ser vacia)",
            ),
        ),
        optionalProperties = listOf("comercio", "fecha_emision", "total", "lineas"),
    )
}

package com.robertomr.tally.domain.expenses.entities

import java.time.LocalDateTime

/**
 * Resultado de la extraccion estructurada del ticket (spec extraccion-nube,
 * etapa 2 del ADR 0002): los datos que un motor de extraccion ([Motor.NUBE] con
 * Gemini o [Motor.LOCAL] con Gemma) deriva del texto crudo del OCR, ya validados
 * por `TicketExtraidoParser` y listos para que la UI deje revisar.
 *
 * Semantica de campos (docs/domain-model.md):
 * - [comercio]: `""` si no aparece en el ticket (la UI lo deja rellenar).
 * - [fechaEmision]: `null` si no es legible/ausente (la UI propondra hoy).
 * - [total]: `null` si no se encuentra; cuando hay valor es SIEMPRE el
 *   **total con IVA** (Total (impuestos incl.) / TOTAL A PAGAR / TOTAL COMPRA),
 *   nunca la base imponible (regla R3).
 * - [lineas]: puede ser vacia (ticket sin detalle extraido).
 * - [categoria]: siempre una de las 6 de [Categoria] (enum fija).
 * - [motor]: quien extrajo los datos; se registra con la extraccion para el
 *   badge/router futuro (ADR 0001).
 */
data class TicketExtraido(
    val comercio: String = "",
    val fechaEmision: LocalDateTime? = null,
    val total: Double? = null,
    val lineas: List<Linea> = emptyList(),
    val categoria: Categoria = Categoria.OTROS,
    val motor: Motor,
)

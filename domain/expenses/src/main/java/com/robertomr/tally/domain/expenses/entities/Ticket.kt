package com.robertomr.tally.domain.expenses.entities

import java.time.LocalDateTime

/**
 * Agregado raíz del dominio (CONTEXT.md, docs/domain-model.md).
 *
 * Solo se persiste el estado Guardado (el Borrador vive en memoria). Un ticket
 * todavía no guardado lleva [id] = 0; al guardarlo se le asigna el id
 * definitivo. El [total] es el dato autoritativo para el seguimiento: puede no
 * coincidir con la suma de [lineas] (OCR imperfecto) y no hay sobrescritura
 * automática.
 */
data class Ticket(
    val id: Long = 0L,
    val comercio: String,
    val fechaEmision: LocalDateTime,
    val total: Double,
    val categoria: Categoria,
    val fechaCaptura: LocalDateTime,
    val motor: Motor,
    val rutaFoto: String,
    val nota: String? = null,
    val textoCrudoOcr: String? = null,
    val lineas: List<Linea> = emptyList(),
)

package com.robertomr.tally.domain.expenses.entities

/**
 * Producto de un ticket (CONTEXT.md, docs/domain-model.md): concepto, cantidad
 * (entero ≥ 1) y precio final unitario (IVA incluido). No lleva categoría
 * propia; pertenece a un Ticket y se guarda y edita con él.
 */
data class Linea(
    val concepto: String,
    val cantidad: Int = 1,
    val precioUnitario: Double,
)

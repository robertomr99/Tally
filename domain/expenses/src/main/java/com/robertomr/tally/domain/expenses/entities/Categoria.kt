package com.robertomr.tally.domain.expenses.entities

/**
 * Categoría del ticket completo. Enum fija de 6 valores (CONTEXT.md,
 * docs/domain-model.md): el modelo elige de esta lista y el usuario corrige
 * entre estas opciones. No es una tabla editable.
 */
enum class Categoria {
    ALIMENTACION,
    HOGAR,
    TRANSPORTE,
    OCIO,
    SALUD,
    OTROS,
}

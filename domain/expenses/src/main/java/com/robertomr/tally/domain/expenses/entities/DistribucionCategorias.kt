package com.robertomr.tally.domain.expenses.entities

/**
 * Parte del gasto del período que corresponde a una [categoria] (spec
 * grafica-donut). [centimos] es exacto (Long); [fraccion] = centimos / total
 * sirve para ángulos y barras; [porcentaje] es el entero ya repartido por resto
 * mayor para que todas las porciones sumen exactamente 100.
 */
data class PorcionCategoria(
    val categoria: Categoria,
    val centimos: Long,
    val fraccion: Double,
    val porcentaje: Int,
)

/**
 * Reparto del gasto por categoría, ordenado por importe descendente. Las
 * categorías sin gasto no aparecen.
 */
data class DistribucionCategorias(
    val porciones: List<PorcionCategoria>,
    val totalCentimos: Long,
) {
    val estaVacia: Boolean get() = porciones.isEmpty()

    companion object {
        val VACIA = DistribucionCategorias(porciones = emptyList(), totalCentimos = 0L)
    }
}

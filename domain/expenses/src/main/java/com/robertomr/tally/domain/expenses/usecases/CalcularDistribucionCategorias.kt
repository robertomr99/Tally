package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.DistribucionCategorias
import com.robertomr.tally.domain.expenses.entities.PorcionCategoria
import com.robertomr.tally.domain.expenses.entities.Ticket
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

/**
 * Reparte el gasto de los tickets por categoría (función pura, spec
 * grafica-donut). Trabaja en céntimos `Long` derivados del `Double` del ticket
 * (redondeo HALF_UP sobre la representación decimal, para que 1,005 sea 101 y
 * no 100 por error binario). Los porcentajes enteros se reparten por el método
 * del resto mayor: siempre suman 100.
 */
class CalcularDistribucionCategorias @Inject constructor() {

    operator fun invoke(tickets: List<Ticket>): DistribucionCategorias {
        val porCategoria = mutableMapOf<Categoria, Long>()
        for (ticket in tickets) {
            val centimos = aCentimos(ticket.total) ?: continue
            if (centimos <= 0L) continue
            porCategoria[ticket.categoria] = (porCategoria[ticket.categoria] ?: 0L) + centimos
        }
        val total = porCategoria.values.sum()
        if (total <= 0L) return DistribucionCategorias.VACIA

        // Orden estricto por importe desc; empate por orden de declaración.
        val ordenadas = porCategoria.entries.sortedWith(
            compareByDescending<Map.Entry<Categoria, Long>> { it.value }.thenBy { it.key.ordinal },
        )

        val bases = ordenadas.map { it.value * 100 / total }
        val restos = ordenadas.map { it.value * 100 % total }
        var sobrantes = (100 - bases.sum()).toInt()
        // Las porciones con mayor resto reciben +1 (empate: más céntimos, luego ordinal).
        val indicesPorResto = ordenadas.indices.sortedWith(
            compareByDescending<Int> { restos[it] }
                .thenByDescending { ordenadas[it].value }
                .thenBy { ordenadas[it].key.ordinal },
        )
        val extra = IntArray(ordenadas.size)
        for (i in indicesPorResto) {
            if (sobrantes <= 0) break
            extra[i] = 1
            sobrantes--
        }

        val porciones = ordenadas.mapIndexed { i, entrada ->
            PorcionCategoria(
                categoria = entrada.key,
                centimos = entrada.value,
                fraccion = entrada.value.toDouble() / total,
                porcentaje = bases[i].toInt() + extra[i],
            )
        }
        return DistribucionCategorias(porciones = porciones, totalCentimos = total)
    }
}

/**
 * Céntimos del importe, o `null` si no es finito. Conversión compartida por la
 * distribución por categoría y el total diario del listado (HALF_UP sobre la
 * representación decimal, para que 1,005 sea 101).
 */
internal fun aCentimos(total: Double): Long? {
    if (!total.isFinite()) return null
    return BigDecimal.valueOf(total).movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
}

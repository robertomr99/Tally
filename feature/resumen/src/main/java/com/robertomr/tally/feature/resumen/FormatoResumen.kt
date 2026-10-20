package com.robertomr.tally.feature.resumen

import com.robertomr.tally.feature.common.formatoImporte
import java.util.Locale

/** Texto del badge cuando el período anterior no tiene gasto (variación `null`). */
const val TEXTO_SIN_REFERENCIA = "Sin datos del periodo anterior"


private val ES = Locale.forLanguageTag("es-ES")

/** Promedio diario, p. ej. `46,08 € / día`. */
fun formatoPromedio(valor: Double): String = "${formatoImporte(valor)} / día"

/**
 * Variación como porcentaje con 1 decimal, coma y signo explícito en positivos:
 * `+51,3 %`, `-4,2 %`. Si redondea a cero se muestra `0,0 %` (sin `+0,0`/`-0,0`).
 * `null` (sin referencia) devuelve [TEXTO_SIN_REFERENCIA].
 */
fun formatoVariacion(fraccion: Double?): String {
    if (fraccion == null) return TEXTO_SIN_REFERENCIA
    val porcentaje = Math.round(fraccion * 1000) / 10.0
    if (porcentaje == 0.0) return "0,0 %"
    return String.format(ES, "%+.1f %%", porcentaje)
}

/** `1 ticket` / `N tickets`. */
fun textoTickets(n: Int): String = if (n == 1) "1 ticket" else "$n tickets"

/** Porcentaje entero: `52 %`; una porción diminuta (0 %) se muestra `<1 %`. */
fun textoPorcentaje(porcentaje: Int): String = if (porcentaje <= 0) "<1 %" else "$porcentaje %"

/** `1 categoría con gasto` / `N categorías con gasto`. */
fun textoCategoriasConGasto(n: Int): String = if (n == 1) "1 categoría con gasto" else "$n categorías con gasto"

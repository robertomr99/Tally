package com.robertomr.tally.feature.resumen

import androidx.compose.ui.text.TextStyle
import java.util.Locale

/** Texto del badge cuando el período anterior no tiene gasto (variación `null`). */
const val TEXTO_SIN_REFERENCIA = "Sin datos del período anterior"

/** Cifras tabulares (DESIGN.md > regla-importe) para todo número de la tarjeta. */
val EstiloTabular = TextStyle(fontFeatureSettings = "tnum")

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

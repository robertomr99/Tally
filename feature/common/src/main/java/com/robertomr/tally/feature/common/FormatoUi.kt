package com.robertomr.tally.feature.common

import androidx.compose.ui.text.TextStyle
import java.util.Locale

/** Cifras tabulares (DESIGN.md > regla-importe) para todo número de importe. */
val EstiloTabular = TextStyle(fontFeatureSettings = "tnum")

/** Importe en español, p. ej. `1.428,50 €`. Las cifras tabulares se aplican en el composable (`EstiloTabular`). */
fun formatoImporte(valor: Double): String = String.format(Locale.forLanguageTag("es-ES"), "%,.2f €", valor)

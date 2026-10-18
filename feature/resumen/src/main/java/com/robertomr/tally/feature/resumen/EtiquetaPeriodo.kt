package com.robertomr.tally.feature.resumen

import com.robertomr.tally.domain.expenses.entities.Periodo
import java.time.LocalDate
import java.util.Locale

private val MESES = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")

private fun LocalDate.corta() = "$dayOfMonth ${MESES[monthValue - 1]}"

/**
 * Etiqueta del rango en español: `18 Oct` (un día) o `1 Oct – 31 Oct`. Los
 * meses van tabulados a mano para no depender del `Locale` del dispositivo.
 */
fun etiquetaPeriodo(periodo: Periodo): String =
    if (periodo.desde == periodo.hasta) periodo.desde.corta()
    else "${periodo.desde.corta()} – ${periodo.hasta.corta()}"

/** Importe en español, p. ej. `1.428,50 €`. Las cifras tabulares se aplican en el composable (`EstiloTabular`). */
fun formatoImporte(valor: Double): String = String.format(Locale.forLanguageTag("es-ES"), "%,.2f €", valor)

package com.robertomr.tally.feature.common

import com.robertomr.tally.domain.expenses.entities.Periodo
import com.robertomr.tally.domain.expenses.entities.PeriodoTipo
import java.time.LocalDate

private val MESES = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")

private fun LocalDate.corta() = "$dayOfMonth ${MESES[monthValue - 1]}"

private fun LocalDate.conAnio() = "${corta()} $year"

/**
 * Etiqueta del rango en español: `18 Oct` (un día) o `1 Oct – 31 Oct`. Los
 * meses van tabulados a mano para no depender del `Locale` del dispositivo.
 * Un rango PERSONALIZADO añade el año (`28 Dic 2025 – 4 Ene 2026`) si cruza
 * año o si no es el año de [hoy]; sin [hoy] solo si cruza año. Día/Semana/Mes
 * no cambian.
 */
fun etiquetaPeriodo(periodo: Periodo, hoy: LocalDate? = null): String {
    val conAnio = periodo.tipo == PeriodoTipo.PERSONALIZADO &&
        (periodo.desde.year != periodo.hasta.year || (hoy != null && periodo.desde.year != hoy.year))
    val formato: (LocalDate) -> String = if (conAnio) LocalDate::conAnio else LocalDate::corta
    return if (periodo.desde == periodo.hasta) formato(periodo.desde)
    else "${formato(periodo.desde)} – ${formato(periodo.hasta)}"
}

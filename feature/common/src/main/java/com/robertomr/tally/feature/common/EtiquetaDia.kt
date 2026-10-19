package com.robertomr.tally.feature.common

import java.time.LocalDate
import java.time.LocalDateTime

private val MESES_LARGOS = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
)

private val MESES_CORTOS = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

private val DIAS_SEMANA = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

/**
 * Cabecera de día del listado (spec listado-gastos): `Hoy, 24 de octubre`,
 * `Ayer, 23 de octubre` o `Lunes, 21 de octubre`. El año solo aparece si difiere
 * del de [hoy]. Tabulada a mano para no depender del `Locale` del dispositivo.
 */
fun etiquetaDia(fecha: LocalDate, hoy: LocalDate): String {
    val prefijo = when (fecha) {
        hoy -> "Hoy"
        hoy.minusDays(1) -> "Ayer"
        else -> DIAS_SEMANA[fecha.dayOfWeek.value - 1]
    }
    val anio = if (fecha.year != hoy.year) " de ${fecha.year}" else ""
    return "$prefijo, ${fecha.dayOfMonth} de ${MESES_LARGOS[fecha.monthValue - 1]}$anio"
}

/** Fecha corta `24 oct` (con año si difiere del de [hoy]) para filas sin cabecera de día. */
fun etiquetaFechaCorta(fecha: LocalDate, hoy: LocalDate): String {
    val anio = if (fecha.year != hoy.year) " ${fecha.year}" else ""
    return "${fecha.dayOfMonth} ${MESES_CORTOS[fecha.monthValue - 1]}$anio"
}

/** `HH:mm`, o `null` si es 00:00 (se asume fecha sin hora, spec listado-gastos D5). */
fun textoHora(fechaEmision: LocalDateTime): String? =
    if (fechaEmision.hour == 0 && fechaEmision.minute == 0) null
    else "%02d:%02d".format(fechaEmision.hour, fechaEmision.minute)

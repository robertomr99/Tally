package com.robertomr.tally.domain.expenses.entities

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Reductores puros de la edicion de [DatosRevision] (spec detalle-ticket, D1):
 * los comparten la revision del ticket recien digitalizado (`:feature:escanear`)
 * y el detalle del ticket guardado (`:feature:gastos`) sin que ambos features se
 * conozcan. Kotlin puro, testeable en JVM.
 *
 * Convencion: los reductores con texto devuelven `null` cuando el texto no es
 * valido; el llamador lo ignora (el campo conserva su ultimo valor valido).
 */

private val FORMATO_FECHA_EDICION: DateTimeFormatter = DateTimeFormatter.ofPattern("d/M/yyyy")

/**
 * [DatosRevision] editable de un [Ticket] ya guardado: cabecera tal cual y lineas
 * convertidas con [lineasEditablesIniciales] (ids correlativos `0..n-1`).
 */
fun datosRevisionDe(ticket: Ticket): DatosRevision = DatosRevision(
    comercio = ticket.comercio,
    fechaEmision = ticket.fechaEmision,
    total = ticket.total,
    categoria = ticket.categoria,
    lineas = lineasEditablesIniciales(ticket.lineas),
)

/**
 * Cambia la fecha desde un texto `d/M/yyyy`; `null` si no es parseable.
 *
 * @param conservarHora `true` mantiene la hora de emision actual (detalle de un
 * ticket guardado, D7); `false` la fija a las 00:00 (comportamiento de la
 * revision tras el escaneo).
 */
fun DatosRevision.conFechaTexto(texto: String, conservarHora: Boolean): DatosRevision? {
    val fecha = try {
        LocalDate.parse(texto.trim(), FORMATO_FECHA_EDICION)
    } catch (e: DateTimeParseException) {
        return null
    }
    val nueva: LocalDateTime =
        if (conservarHora) fecha.atTime(fechaEmision.toLocalTime()) else fecha.atStartOfDay()
    return copy(fechaEmision = nueva)
}

/** Cambia el total desde un texto (coma o punto decimal); `null` si no es parseable. */
fun DatosRevision.conTotalTexto(texto: String): DatosRevision? {
    val total = texto.trim().replace(',', '.').toDoubleOrNull() ?: return null
    return copy(total = total)
}

/** Anade una fila vacia con el [id] de cliente dado; nunca toca el total. */
fun DatosRevision.conLineaAnadida(id: Long): DatosRevision =
    copy(lineas = lineas + nuevaLineaEditable(id))

/** Cambia el concepto de la linea [id] (las demas quedan igual). */
fun DatosRevision.conLineaConcepto(id: Long, concepto: String): DatosRevision =
    conLineaEditada(id) { it.copy(concepto = concepto) }

/** Cantidad entera >= 1 desde texto; `null` si no es valida. */
fun DatosRevision.conLineaCantidad(id: Long, texto: String): DatosRevision? {
    val cantidad = parsearCantidad(texto) ?: return null
    return conLineaEditada(id) { it.copy(cantidad = cantidad) }
}

/** Precio >= 0 desde texto (coma o punto); `null` si no es valido. */
fun DatosRevision.conLineaPrecio(id: Long, texto: String): DatosRevision? {
    val precio = parsearPrecio(texto) ?: return null
    return conLineaEditada(id) { it.copy(precioUnitario = precio) }
}

/** Elimina la linea [id]; el total no cambia. */
fun DatosRevision.sinLinea(id: Long): DatosRevision =
    copy(lineas = lineas.filterNot { it.id == id })

private fun DatosRevision.conLineaEditada(
    id: Long,
    transformacion: (LineaEditable) -> LineaEditable,
): DatosRevision = copy(
    lineas = lineas.map { linea -> if (linea.id == id) transformacion(linea) else linea },
)

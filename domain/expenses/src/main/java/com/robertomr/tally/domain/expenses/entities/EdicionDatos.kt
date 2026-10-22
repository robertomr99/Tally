package com.robertomr.tally.domain.expenses.entities

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.math.abs

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
    formaPago = ticket.formaPago,
    entregado = ticket.entregado,
    cambio = ticket.cambio,
).conModoTotalInicial().conPagoCoherente()

/**
 * Modo inicial del total al abrir un ticket (spec gasto-manual-2, B3): sin
 * lineas TECLEADO; con lineas y total ausente/<= 0 o que cuadra con la suma
 * (+-0,01) AUTOMATICO con `total = suma`; con lineas y total que NO cuadra
 * TECLEADO conservando el total del papel/guardado (nunca se pierde).
 */
internal fun DatosRevision.conModoTotalInicial(): DatosRevision {
    if (lineas.isEmpty()) return copy(totalAutomatico = false)
    val suma = totalDeLineas(lineas)
    return if (total <= 0.0 || abs(suma - total) <= TOLERANCIA_PAGO) {
        copy(total = suma, totalAutomatico = true)
    } else {
        copy(totalAutomatico = false)
    }
}

/**
 * Lleva el total a su estado coherente tras una edicion de lineas (B4): sin
 * lineas TECLEADO (la cifra se conserva); en AUTOMATICO `total = suma`; en
 * TECLEADO el total no se toca, salvo si es la [primeraLinea] con total 0 o si
 * tras editar la suma cuadra con el total (+-0,01): pasa a AUTOMATICO. Termina
 * con [conPagoCoherente] porque el cambio depende del total.
 */
private fun DatosRevision.conTotalSincronizado(primeraLinea: Boolean = false): DatosRevision {
    if (lineas.isEmpty()) return copy(totalAutomatico = false).conPagoCoherente()
    val suma = totalDeLineas(lineas)
    val resultado = when {
        totalAutomatico -> copy(total = suma)
        primeraLinea && total == 0.0 -> copy(total = suma, totalAutomatico = true)
        abs(suma - total) <= TOLERANCIA_PAGO -> copy(total = suma, totalAutomatico = true)
        else -> this
    }
    return resultado.conPagoCoherente()
}

/**
 * Transicion explicita del usuario (B5): `true` "Usar la suma de las lineas"
 * (TECLEADO -> AUTOMATICO, `total = suma`; sin lineas no hace nada); `false`
 * "Editar total a mano" (AUTOMATICO -> TECLEADO, el total queda igual).
 */
fun DatosRevision.conTotalAutomatico(automatico: Boolean): DatosRevision =
    if (automatico) {
        if (lineas.isEmpty()) this else copy(total = totalDeLineas(lineas), totalAutomatico = true).conPagoCoherente()
    } else {
        copy(totalAutomatico = false)
    }

/**
 * Deja entregado y cambio coherentes con el total (spec gasto-manual-2, A3);
 * solo actua con [FormaPago.EFECTIVO] y total > 0:
 * - con entregado: `cambio = entregado - total` redondeado a centimos, o `null`
 *   si lo entregado no cubre el total (nunca negativo);
 * - solo cambio (caso extraido): `entregado = total + cambio`.
 * Con tarjeta / no indicada deja los valores ocultos como estan.
 */
fun DatosRevision.conPagoCoherente(): DatosRevision {
    if (formaPago != FormaPago.EFECTIVO || total <= 0.0) return this
    val entregado = entregado
    if (entregado != null) {
        val diferencia = redondearCentimos(entregado - total) + 0.0
        return copy(cambio = if (diferencia >= 0.0) diferencia else null)
    }
    val cambio = cambio ?: return this
    return copy(entregado = redondearCentimos(total + cambio))
}

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

/**
 * Cambia el total desde un texto (coma o punto decimal); `null` si no es
 * parseable o si el total es AUTOMATICO (solo lectura: se ignora).
 */
fun DatosRevision.conTotalTexto(texto: String): DatosRevision? {
    if (totalAutomatico) return null
    val total = texto.trim().replace(',', '.').toDoubleOrNull() ?: return null
    return copy(total = total).conPagoCoherente()
}

/**
 * Cambia la forma de pago (spec forma-pago). CONSERVA entregado y cambio en el
 * estado de edicion aunque la forma deje de ser efectivo (la UI los oculta y
 * reaparecen si se vuelve a efectivo); [normalizada] los limpia al guardar.
 */
fun DatosRevision.conFormaPago(forma: FormaPago): DatosRevision =
    copy(formaPago = forma).conPagoCoherente()

/**
 * Cambia lo entregado desde un texto (coma o punto). Texto en blanco = sin
 * entregado Y sin cambio (`null` en ambos); texto no numerico o negativo =
 * `null` (se ignora). El cambio se deriva (`entregado - total`, solo lectura).
 */
fun DatosRevision.conEntregadoTexto(texto: String): DatosRevision? {
    if (texto.isBlank()) return copy(entregado = null, cambio = null)
    val valor = texto.trim().replace(',', '.').toDoubleOrNull()
        ?.takeIf { it.isFinite() && it >= 0.0 } ?: return null
    return copy(entregado = valor).conPagoCoherente()
}

/**
 * Anade una fila vacia con el [id] de cliente dado; la primera fila con total 0
 * pasa el total a AUTOMATICO (ver [conTotalSincronizado]).
 */
fun DatosRevision.conLineaAnadida(id: Long): DatosRevision =
    copy(lineas = lineas + nuevaLineaEditable(id))
        .conTotalSincronizado(primeraLinea = lineas.isEmpty())

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

/** Elimina la linea [id]; borrar la ultima conserva la cifra del total. */
fun DatosRevision.sinLinea(id: Long): DatosRevision =
    copy(lineas = lineas.filterNot { it.id == id }).conTotalSincronizado()

private fun DatosRevision.conLineaEditada(
    id: Long,
    transformacion: (LineaEditable) -> LineaEditable,
): DatosRevision = copy(
    lineas = lineas.map { linea -> if (linea.id == id) transformacion(linea) else linea },
).conTotalSincronizado()

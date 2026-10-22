package com.robertomr.tally.domain.expenses.entities

import java.time.LocalDate

/**
 * Motivo por el que un gasto manual todavia no se puede guardar (spec
 * gasto-manual): el total debe ser mayor que 0 y la fecha no puede ser futura.
 * El comercio vacio NO bloquea (el listado muestra "Sin comercio").
 */
enum class ErrorGastoManual { TOTAL_NO_POSITIVO, FECHA_FUTURA }

/**
 * Validacion minima del alta manual (funcion pura). El total tiene prioridad
 * sobre la fecha cuando fallan los dos.
 *
 * @param hoy inyectable para tests deterministas.
 */
fun DatosRevision.errorGastoManual(hoy: LocalDate = LocalDate.now()): ErrorGastoManual? = when {
    !total.isFinite() || total <= 0.0 -> ErrorGastoManual.TOTAL_NO_POSITIVO
    fechaEmision.toLocalDate().isAfter(hoy) -> ErrorGastoManual.FECHA_FUTURA
    else -> null
}

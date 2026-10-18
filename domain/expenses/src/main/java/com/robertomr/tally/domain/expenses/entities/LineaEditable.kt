package com.robertomr.tally.domain.expenses.entities

/**
 * Linea del ticket mientras el usuario la edita en la revision (spec
 * edicion-lineas, mockup 08): concepto, cantidad y precio final unitario.
 *
 * [id] es un correlativo de cliente que solo sirve para identificar la fila
 * durante la edicion (clave estable en Compose y direccionamiento de
 * ediciones/borrado). NO se persiste: al guardar se mapea a [Linea] con
 * [aLineas].
 */
data class LineaEditable(
    val id: Long,
    val concepto: String,
    val cantidad: Int,
    val precioUnitario: Double,
)

/**
 * Lineas editables iniciales a partir de las extraidas por el motor, con ids
 * correlativos `0..n-1` (funcion pura).
 */
fun lineasEditablesIniciales(lineas: List<Linea>): List<LineaEditable> =
    lineas.mapIndexed { indice, linea ->
        LineaEditable(
            id = indice.toLong(),
            concepto = linea.concepto,
            cantidad = linea.cantidad.coerceAtLeast(1),
            precioUnitario = linea.precioUnitario.coerceAtLeast(0.0),
        )
    }

/**
 * Fila nueva lista para escribir ("Añadir producto o linea"): concepto vacio,
 * cantidad 1 y precio 0. Su id es `max(id) + 1` (0 si no hay lineas), asi que
 * nunca colisiona con una fila existente.
 */
fun nuevaLineaEditable(existentes: List<LineaEditable>): LineaEditable =
    LineaEditable(
        id = (existentes.maxOfOrNull { it.id } ?: -1L) + 1L,
        concepto = "",
        cantidad = 1,
        precioUnitario = 0.0,
    )

/**
 * "Total recalculado" (footer del mockup 08): suma de `cantidad x precio` de
 * las lineas; 0 lineas -> `0.0`.
 *
 * Es solo una REFERENCIA (docs/domain-model.md, "Total vs lineas"): el total
 * del ticket es el dato autoritativo y nunca se sobrescribe con este valor.
 */
fun totalRecalculado(lineas: List<LineaEditable>): Double =
    lineas.sumOf { it.cantidad * it.precioUnitario }

/**
 * Mapea las lineas editadas a [Linea] para persistirlas: descarta las filas sin
 * concepto (en blanco) y acota defensivamente cantidad >= 1 y precio >= 0
 * (docs/domain-model.md: cantidad entera >= 1).
 */
fun List<LineaEditable>.aLineas(): List<Linea> =
    filter { it.concepto.isNotBlank() }
        .map { editable ->
            Linea(
                concepto = editable.concepto.trim(),
                cantidad = editable.cantidad.coerceAtLeast(1),
                precioUnitario = editable.precioUnitario.coerceAtLeast(0.0),
            )
        }

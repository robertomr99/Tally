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
 * cantidad 1 y precio 0. El [id] lo asigna quien edita con un contador que solo
 * crece, asi que borrar la ultima fila y anadir otra nunca reutiliza un id.
 */
fun nuevaLineaEditable(id: Long): LineaEditable =
    LineaEditable(
        id = id,
        concepto = "",
        cantidad = 1,
        precioUnitario = 0.0,
    )

/**
 * Cantidad de linea tecleada: entero >= 1 (docs/domain-model.md); si no, `null`
 * (el editor ignora el texto y la UI lo marca en error).
 */
fun parsearCantidad(texto: String): Int? =
    texto.trim().toIntOrNull()?.takeIf { it >= 1 }

/**
 * Precio unitario tecleado: campo vacio = 0 (como una linea nueva); si no,
 * numero finito >= 0 con coma o punto decimal; un texto no valido devuelve
 * `null` (el editor lo ignora y la UI lo marca en error).
 */
fun parsearPrecio(texto: String): Double? {
    val limpio = texto.trim()
    if (limpio.isEmpty()) return 0.0
    return limpio.replace(',', '.').toDoubleOrNull()
        ?.takeIf { it.isFinite() && it >= 0.0 }
}

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

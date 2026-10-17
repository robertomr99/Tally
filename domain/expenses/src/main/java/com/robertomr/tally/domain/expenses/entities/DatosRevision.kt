package com.robertomr.tally.domain.expenses.entities

import java.time.LocalDateTime

/**
 * Cabecera corregida a mano en la revision del ticket digitalizado (spec
 * revision-datos, mockup 07): comercio, fecha de emision, total y categoria.
 *
 * Solo cubre la cabecera; las lineas se muestran en solo lectura (su edicion es
 * `edicion-lineas`, otra feature). Los campos son no nulos: los ausentes en la
 * extraccion ya se han resuelto con los valores por defecto de
 * [datosRevisionInicial].
 */
data class DatosRevision(
    val comercio: String,
    val fechaEmision: LocalDateTime,
    val total: Double,
    val categoria: Categoria,
)

/**
 * Estado editable inicial del ticket digitalizado a partir de la extraccion del
 * motor (funcion pura, testeable en JVM).
 *
 * Defaults de campos ausentes (docs/domain-model.md, R-REV2): comercio vacio se
 * mantiene (la UI lo deja rellenar), fecha de emision nula -> "hoy", total nulo
 * -> `0.0`, categoria ya es una de las 6 de [Categoria]. Ningun campo bloquea el
 * guardado.
 *
 * @param hoy fecha propuesta cuando el ticket no trae una legible; inyectable
 * para tests deterministas.
 */
fun datosRevisionInicial(
    extraido: TicketExtraido,
    hoy: LocalDateTime = LocalDateTime.now(),
): DatosRevision = DatosRevision(
    comercio = extraido.comercio,
    fechaEmision = extraido.fechaEmision ?: hoy,
    total = extraido.total ?: 0.0,
    categoria = extraido.categoria,
)

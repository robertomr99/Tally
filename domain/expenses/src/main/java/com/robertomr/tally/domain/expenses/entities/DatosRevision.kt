package com.robertomr.tally.domain.expenses.entities

import java.time.LocalDateTime

/**
 * Lo que el usuario esta editando en la revision del ticket digitalizado (spec
 * revision-datos, mockup 07; spec edicion-lineas, mockup 08): la cabecera
 * corregida (comercio, fecha de emision, total y categoria) y las lineas
 * editables ([lineas]).
 *
 * El [total] es el dato autoritativo del ticket y solo cambia por edicion
 * explicita del usuario; NUNCA se deriva de [lineas] (docs/domain-model.md,
 * "Total vs lineas": la suma de lineas es solo referencia, ver
 * [totalRecalculado]). Los campos son no nulos: los ausentes en la extraccion
 * ya se han resuelto con los valores por defecto de [datosRevisionInicial].
 */
data class DatosRevision(
    val comercio: String,
    val fechaEmision: LocalDateTime,
    val total: Double,
    val categoria: Categoria,
    val lineas: List<LineaEditable>,
)

/**
 * Estado editable inicial del ticket digitalizado a partir de la extraccion del
 * motor (funcion pura, testeable en JVM).
 *
 * Defaults de campos ausentes (docs/domain-model.md, R-REV2): comercio vacio se
 * mantiene (la UI lo deja rellenar), fecha de emision nula -> "hoy", total nulo
 * -> `0.0`, categoria ya es una de las 6 de [Categoria]. Las lineas extraidas se
 * convierten en editables con [lineasEditablesIniciales]. Ningun campo bloquea
 * el guardado.
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
    lineas = lineasEditablesIniciales(extraido.lineas),
)

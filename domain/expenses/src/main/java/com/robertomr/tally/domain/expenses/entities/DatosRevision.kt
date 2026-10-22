package com.robertomr.tally.domain.expenses.entities

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Lo que el usuario esta editando en la revision del ticket digitalizado (spec
 * revision-datos, mockup 07; spec edicion-lineas, mockup 08): la cabecera
 * corregida (comercio, fecha de emision, total y categoria) y las lineas
 * editables ([lineas]).
 *
 * El [total] tiene dos modos (spec gasto-manual-2, docs/domain-model.md,
 * "Total vs lineas"):
 * - AUTOMATICO ([totalAutomatico] = `true`): el total es la suma de las lineas
 *   ([totalDeLineas]) y sigue a cada edicion; solo lectura en la UI.
 * - TECLEADO (`false`): el total es el que escribio el usuario o el del papel
 *   y NUNCA se sobrescribe; si hay lineas que no cuadran, la UI avisa. Sin
 *   lineas siempre es TECLEADO.
 *
 * [totalAutomatico] no se persiste: se deriva al abrir ([datosRevisionInicial],
 * [datosRevisionDe]). Los campos son no nulos: los ausentes en la extraccion
 * ya se han resuelto con los valores por defecto de [datosRevisionInicial].
 */
data class DatosRevision(
    val comercio: String,
    val fechaEmision: LocalDateTime,
    val total: Double,
    val categoria: Categoria,
    val lineas: List<LineaEditable>,
    val formaPago: FormaPago = FormaPago.NO_INDICADA,
    val entregado: Double? = null,
    val cambio: Double? = null,
    val totalAutomatico: Boolean = false,
)

/**
 * Estado editable inicial del ticket digitalizado a partir de la extraccion del
 * motor (funcion pura, testeable en JVM).
 *
 * Defaults de campos ausentes (docs/domain-model.md, R-REV2): comercio vacio se
 * mantiene (la UI lo deja rellenar), fecha de emision nula -> "hoy", total nulo
 * -> `0.0`, categoria ya es una de las 6 de [Categoria]. Las lineas extraidas se
 * convierten en editables con [lineasEditablesIniciales]. Ningun campo bloquea
 * el guardado. El modo del total sale de [conModoTotalInicial] y el pago se
 * deja coherente con [conPagoCoherente].
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
    formaPago = extraido.formaPago,
    entregado = extraido.entregado,
    cambio = extraido.cambio,
).conModoTotalInicial().conPagoCoherente()

/**
 * Estado inicial del alta de un gasto manual (spec gasto-manual): comercio
 * vacio, fecha de hoy a las 00:00 (el listado oculta la hora 00:00), total 0
 * (TECLEADO), categoria [Categoria.OTROS], sin lineas y pago sin indicar.
 *
 * @param hoy inyectable para tests deterministas.
 */
fun datosGastoManualInicial(hoy: LocalDate = LocalDate.now()): DatosRevision = DatosRevision(
    comercio = "",
    fechaEmision = hoy.atStartOfDay(),
    total = 0.0,
    categoria = Categoria.OTROS,
    lineas = emptyList(),
)

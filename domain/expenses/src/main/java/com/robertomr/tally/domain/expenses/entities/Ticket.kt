package com.robertomr.tally.domain.expenses.entities

import java.time.LocalDateTime

/**
 * Agregado raíz del dominio (CONTEXT.md, docs/domain-model.md).
 *
 * Solo se persiste el estado Guardado (el Borrador vive en memoria). Un ticket
 * todavía no guardado lleva [id] = 0; al guardarlo se le asigna el id
 * definitivo. El [total] es el dato autoritativo para el seguimiento: puede no
 * coincidir con la suma de [lineas] (OCR imperfecto) y no hay sobrescritura
 * automática.
 *
 * [formaPago], [entregado] y [cambio] (spec forma-pago) son solo informativos:
 * el [total] sigue siendo lo que cuesta. [entregado]/[cambio] solo son no nulos
 * si [formaPago] es [FormaPago.EFECTIVO] (ver [normalizarPago]).
 *
 * [motor] nulo significa "sin extraccion": es un gasto manual (spec
 * gasto-manual), que ademas no tiene foto ([rutaFoto] vacia). [Motor] sigue
 * siendo "quien extrajo" (ADR 0001/0002); por eso no hay un `Motor.MANUAL`.
 */
data class Ticket(
    val id: Long = 0L,
    val comercio: String,
    val fechaEmision: LocalDateTime,
    val total: Double,
    val categoria: Categoria,
    val fechaCaptura: LocalDateTime,
    val motor: Motor? = null,
    val rutaFoto: String,
    val nota: String? = null,
    val textoCrudoOcr: String? = null,
    val lineas: List<Linea> = emptyList(),
    val formaPago: FormaPago = FormaPago.NO_INDICADA,
    val entregado: Double? = null,
    val cambio: Double? = null,
)

/** Un ticket sin motor es un gasto añadido a mano (sin foto ni extraccion). */
val Ticket.esManual: Boolean get() = motor == null

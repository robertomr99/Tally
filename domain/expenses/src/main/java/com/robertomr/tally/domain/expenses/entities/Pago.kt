package com.robertomr.tally.domain.expenses.entities

import kotlin.math.abs
import kotlin.math.round

/**
 * Reglas puras de la forma de pago (spec forma-pago, docs/domain-model.md).
 * Kotlin puro, testeable en JVM.
 *
 * Invariante persistida: [Ticket.entregado] y [Ticket.cambio] solo son no
 * nulos si la forma es [FormaPago.EFECTIVO]; nunca son negativos ni no finitos.
 * Ninguno de los dos es el total: el total (con IVA) es lo que cuesta.
 */

/** Tolerancia (en euros) de la comprobacion entregado - total = cambio. */
const val TOLERANCIA_PAGO = 0.01

/** Resultado de [normalizarPago]. */
data class PagoNormalizado(
    val formaPago: FormaPago,
    val entregado: Double?,
    val cambio: Double?,
)

/** Aviso NO bloqueante de coherencia del pago en efectivo. */
enum class AvisoPago {
    /** Lo entregado es menor que el total. */
    ENTREGADO_MENOR_QUE_TOTAL,

    /** `entregado - total` no coincide con el cambio (mas de 0,01). */
    CAMBIO_NO_CUADRA,
}

/** Redondeo a centimos. */
internal fun redondearCentimos(valor: Double): Double = round(valor * 100.0) / 100.0

/**
 * Aplica la invariante de pago:
 * 1. entregado o cambio presentes con forma NO_INDICADA -> EFECTIVO (tener
 *    entregado o cambio implica efectivo);
 * 2. forma distinta de EFECTIVO -> entregado = cambio = null;
 * 3. EFECTIVO con total > 0: solo entregado -> cambio = entregado - total (si
 *    >= 0); solo cambio -> entregado = total + cambio;
 * 4. valores negativos o no finitos -> null.
 *
 * Con TARJETA no se infiere nada de entregado/cambio (se descartan).
 */
fun normalizarPago(
    formaPago: FormaPago,
    total: Double?,
    entregado: Double?,
    cambio: Double?,
): PagoNormalizado {
    var entregadoValido = entregado?.takeIf { it.isFinite() && it >= 0.0 }
    var cambioValido = cambio?.takeIf { it.isFinite() && it >= 0.0 }
    val forma = if (formaPago == FormaPago.NO_INDICADA &&
        (entregadoValido != null || cambioValido != null)
    ) {
        FormaPago.EFECTIVO
    } else {
        formaPago
    }
    if (forma != FormaPago.EFECTIVO) {
        return PagoNormalizado(forma, null, null)
    }
    val totalValido = total?.takeIf { it.isFinite() && it > 0.0 }
    if (totalValido != null) {
        if (entregadoValido != null && cambioValido == null && entregadoValido >= totalValido) {
            cambioValido = redondearCentimos(entregadoValido - totalValido)
        } else if (cambioValido != null && entregadoValido == null) {
            entregadoValido = redondearCentimos(totalValido + cambioValido)
        }
    }
    return PagoNormalizado(forma, entregadoValido, cambioValido)
}

/** Version con la invariante de pago aplicada; se usa al guardar (ver [normalizarPago]). */
fun DatosRevision.normalizada(): DatosRevision {
    val pago = normalizarPago(formaPago, total, entregado, cambio)
    return copy(formaPago = pago.formaPago, entregado = pago.entregado, cambio = pago.cambio)
}

/**
 * Aviso no bloqueante de coherencia (solo con EFECTIVO y entregado rellenado):
 * entregado menor que el total, o cambio que no cuadra con entregado - total.
 */
fun avisoPago(datos: DatosRevision): AvisoPago? {
    if (datos.formaPago != FormaPago.EFECTIVO) return null
    val entregado = datos.entregado ?: return null
    if (entregado < datos.total - TOLERANCIA_PAGO) return AvisoPago.ENTREGADO_MENOR_QUE_TOTAL
    val cambio = datos.cambio ?: return null
    return if (abs(entregado - datos.total - cambio) > TOLERANCIA_PAGO) {
        AvisoPago.CAMBIO_NO_CUADRA
    } else {
        null
    }
}

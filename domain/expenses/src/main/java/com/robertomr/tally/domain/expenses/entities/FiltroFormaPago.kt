package com.robertomr.tally.domain.expenses.entities

/**
 * Filtro del listado de Gastos por forma de pago (spec filtro-forma-pago).
 * [TODOS] admite cualquier forma, incluida [FormaPago.NO_INDICADA]; [EFECTIVO] y
 * [TARJETA] solo admiten su propia forma (NO_INDICADA solo aparece con [TODOS]).
 */
enum class FiltroFormaPago {
    TODOS,
    EFECTIVO,
    TARJETA,
    ;

    fun admite(forma: FormaPago): Boolean = when (this) {
        TODOS -> true
        EFECTIVO -> forma == FormaPago.EFECTIVO
        TARJETA -> forma == FormaPago.TARJETA
    }
}

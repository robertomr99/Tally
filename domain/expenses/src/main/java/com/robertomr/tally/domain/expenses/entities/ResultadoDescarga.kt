package com.robertomr.tally.domain.expenses.entities

/** Resultado de pedir la descarga del modelo (spec descarga-modelo). */
sealed interface ResultadoDescarga {
    /** La descarga quedo encolada. */
    data object Iniciada : ResultadoDescarga

    /** Red movil o medida: no se encola nada ni se abre conexion. */
    data object RequiereWifi : ResultadoDescarga

    /** No hay espacio para el resto del modelo mas el margen. */
    data class EspacioInsuficiente(val necesarios: Long, val libres: Long) : ResultadoDescarga
}

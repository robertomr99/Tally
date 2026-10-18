package com.robertomr.tally.domain.expenses.entities

/**
 * El motor local no puede extraer porque el modelo no esta descargado
 * (spec extraccion-local, scenario 4). Es un estado recuperable (el usuario
 * descarga el modelo en Ajustes), no un fallo del modelo ni del schema.
 */
class ModeloLocalNoDisponibleException(
    mensaje: String = "El modelo local no esta descargado: descarga el modelo en Ajustes",
) : IllegalStateException(mensaje)

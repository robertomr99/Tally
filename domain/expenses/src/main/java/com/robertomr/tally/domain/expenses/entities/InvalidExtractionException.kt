package com.robertomr.tally.domain.expenses.entities

/**
 * La respuesta del motor de extraccion no cumple el schema compartido de
 * `TicketExtraido` (spec extraccion-nube, scenario 4): JSON roto o sin objeto,
 * categoria ausente o fuera de la enum, fecha no ISO, total no numerico/
 * negativo, linea sin concepto, cantidad < 1 o precio invalido.
 *
 * El [motivo] es explicito para que la presentacion pueda mostrarlo (via
 * manual / reintento de otras features). No es un error transitorio de red:
 * esos se propagan tal cual desde el motor.
 */
class InvalidExtractionException(motivo: String) : Exception(motivo)

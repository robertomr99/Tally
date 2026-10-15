package com.robertomr.tally.framework.database.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Ticket con su conjunto de líneas (relación 1-N cargada por Room).
 */
data class TicketWithLineas(
    @Embedded val ticket: TicketEntity,
    @Relation(parentColumn = "id", entityColumn = "ticket_id")
    val lineas: List<LineaEntity>,
)

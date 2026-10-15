package com.robertomr.tally.framework.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Fila de la tabla `lineas` (spec persistencia-room). La FK con borrado en
 * cascada garantiza que no quedan líneas huérfanas al borrar su ticket; el
 * índice en `ticket_id` cubre la carga por relación y el borrado.
 */
@Entity(
    tableName = "lineas",
    foreignKeys = [
        ForeignKey(
            entity = TicketEntity::class,
            parentColumns = ["id"],
            childColumns = ["ticket_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("ticket_id")],
)
data class LineaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    // 0 = sin ticket asignado aǧn; `TicketDao.upsert` fija el id real al insertar.
    @ColumnInfo(name = "ticket_id") val ticketId: Long = 0L,
    val concepto: String,
    val cantidad: Int,
    @ColumnInfo(name = "precio_unitario") val precioUnitario: Double,
)

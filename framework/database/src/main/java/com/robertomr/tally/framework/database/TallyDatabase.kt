package com.robertomr.tally.framework.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.robertomr.tally.framework.database.dao.TicketDao
import com.robertomr.tally.framework.database.entity.LineaEntity
import com.robertomr.tally.framework.database.entity.TicketEntity

/**
 * Base de datos local de Tally (Room es la única fuente de verdad; AGENTS.md).
 * v1: `tickets` y `lineas` con borrado en cascada de líneas. El esquema se
 * exporta a `schemas/` para migraciones futuras.
 */
@Database(
    entities = [TicketEntity::class, LineaEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class TallyDatabase : RoomDatabase() {

    abstract fun ticketDao(): TicketDao
}

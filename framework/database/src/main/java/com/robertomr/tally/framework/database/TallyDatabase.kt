package com.robertomr.tally.framework.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.robertomr.tally.framework.database.dao.ExtraccionLogDao
import com.robertomr.tally.framework.database.dao.TicketDao
import com.robertomr.tally.framework.database.entity.ExtraccionLogEntity
import com.robertomr.tally.framework.database.entity.LineaEntity
import com.robertomr.tally.framework.database.entity.TicketEntity

/**
 * Base de datos local de Tally (Room es la única fuente de verdad; AGENTS.md).
 * v1: `tickets` y `lineas` con borrado en cascada de líneas.
 * v2 (log-extracciones): añade `extracciones_log` con [MIGRATION_1_2].
 * v3 (forma-pago): `tickets` gana `forma_pago`, `entregado` y `cambio` con [MIGRATION_2_3].
 * El esquema se exporta a `schemas/` para migraciones y su test.
 */
@Database(
    entities = [TicketEntity::class, LineaEntity::class, ExtraccionLogEntity::class],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class TallyDatabase : RoomDatabase() {

    abstract fun ticketDao(): TicketDao

    abstract fun extraccionLogDao(): ExtraccionLogDao
}

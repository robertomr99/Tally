package com.robertomr.tally.framework.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migración 1 -> 2 (spec log-extracciones, D1): solo AÑADE la tabla
 * `extracciones_log` y su índice. No toca `tickets` ni `lineas`, así que los
 * datos del usuario se conservan. El SQL replica exactamente el `createSql` de
 * `schemas/.../2.json` (Room valida el esquema al abrir la BD).
 *
 * Prohibido `fallbackToDestructiveMigration`: borraría los tickets.
 */
internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `extracciones_log` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`marca_tiempo` INTEGER NOT NULL, " +
                "`motor` TEXT NOT NULL, " +
                "`origen` TEXT NOT NULL, " +
                "`resultado` TEXT NOT NULL, " +
                "`tipo_fallo` TEXT, " +
                "`clase_error` TEXT, " +
                "`duracion_ms` INTEGER NOT NULL, " +
                "`longitud_texto` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_extracciones_log_marca_tiempo` " +
                "ON `extracciones_log` (`marca_tiempo`)"
        )
    }
}

package com.robertomr.tally.framework.database

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Conversores de tipos de Room.
 *
 * `LocalDateTime` se codifica como epoch millis interpretando la fecha-hora
 * local en UTC. Es solo una codificación estable (no un instante real): el
 * round-trip es exacto y no depende de la zona del dispositivo ni de cambios
 * de hora, así las consultas por rango son deterministas.
 */
class Converters {

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): Long? =
        value?.toInstant(ZoneOffset.UTC)?.toEpochMilli()

    @TypeConverter
    fun toLocalDateTime(value: Long?): LocalDateTime? =
        value?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDateTime() }
}

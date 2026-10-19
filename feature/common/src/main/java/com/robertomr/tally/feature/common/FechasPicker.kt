package com.robertomr.tally.feature.common

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Conversión entre `LocalDate` y los epoch millis que usa el date picker de
 * Material 3, que SIEMPRE son medianoche UTC del día elegido. Se usa
 * `ZoneOffset.UTC` y nunca la zona del dispositivo: en Madrid (UTC+1/+2) la
 * conversión local daría un día de error en el borde.
 */
fun LocalDate.aMillisUtc(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

/** Inversa de [aMillisUtc]; cualquier instante del día UTC resuelve a ese día. */
fun Long.aLocalDateUtc(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

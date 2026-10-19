package com.robertomr.tally.framework.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Fila de la tabla `extracciones_log` (spec log-extracciones, BD v2). Solo
 * metadatos (D3): jamás texto crudo, datos del ticket ni mensajes de excepción.
 * Los enums del dominio se guardan como texto (`name`) para no acoplar el
 * esquema al orden de los valores. El índice por `marca_tiempo` cubre la
 * consulta de recientes y el recorte por retención.
 */
@Entity(
    tableName = "extracciones_log",
    indices = [Index("marca_tiempo")],
)
data class ExtraccionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "marca_tiempo") val marcaTiempo: Long,
    val motor: String,
    val origen: String,
    val resultado: String,
    @ColumnInfo(name = "tipo_fallo") val tipoFallo: String?,
    @ColumnInfo(name = "clase_error") val claseError: String?,
    @ColumnInfo(name = "duracion_ms") val duracionMs: Long,
    @ColumnInfo(name = "longitud_texto") val longitudTexto: Int,
)

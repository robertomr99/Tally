package com.robertomr.tally.framework.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Fila de la tabla `tickets` (spec persistencia-room). El mapping con el
 * agregado de dominio vive en `source/TicketLocalDataSource`.
 *
 * `fecha_emision` y `fecha_captura` se codifican con el TypeConverter de
 * `Converters` (LocalDateTime ↔ epoch millis).
 */
@Entity(tableName = "tickets")
data class TicketEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val comercio: String,
    @ColumnInfo(name = "fecha_emision") val fechaEmision: LocalDateTime,
    val total: Double,
    val categoria: String,
    val nota: String?,
    @ColumnInfo(name = "fecha_captura") val fechaCaptura: LocalDateTime,
    val motor: String,
    @ColumnInfo(name = "ruta_foto") val rutaFoto: String,
    @ColumnInfo(name = "texto_crudo_ocr") val textoCrudoOcr: String?,
    // Forma de pago (spec forma-pago, v3). El defaultValue DEBE coincidir con el
    // DEFAULT de MIGRATION_2_3 y con el createSql de schemas/.../3.json: Room
    // valida el esquema al abrir y un desajuste tumba la app.
    @ColumnInfo(name = "forma_pago", defaultValue = "NO_INDICADA")
    val formaPago: String = "NO_INDICADA",
    @ColumnInfo(name = "entregado") val entregado: Double? = null,
    @ColumnInfo(name = "cambio") val cambio: Double? = null,
)

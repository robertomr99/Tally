package com.robertomr.tally.framework.database.source

import com.robertomr.tally.domain.expenses.data.TicketDataSource
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.FormaPago
import com.robertomr.tally.domain.expenses.entities.Linea
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.Ticket
import com.robertomr.tally.framework.database.dao.TicketDao
import com.robertomr.tally.framework.database.entity.LineaEntity
import com.robertomr.tally.framework.database.entity.TicketEntity
import com.robertomr.tally.framework.database.entity.TicketWithLineas
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Valor guardado en la columna `motor` (TEXT NOT NULL) para un gasto manual, que
 * en dominio es `Ticket.motor == null` (spec gasto-manual; sin migracion).
 */
internal const val MOTOR_MANUAL = "MANUAL"

/**
 * Lectura tolerante de la columna `motor`: "MANUAL" o un valor desconocido
 * (p. ej. de una version futura) es `null` (se ve como gasto manual). Nunca lanza.
 */
internal fun motorDesde(valor: String): Motor? =
    Motor.entries.firstOrNull { it.name == valor }

/**
 * Implementación Room de [TicketDataSource] (interna de `:framework:database`;
 * AGENTS.md). El mapping entidad↔dominio vive aquí.
 */
internal class TicketLocalDataSource(
    private val ticketDao: TicketDao,
) : TicketDataSource {

    override suspend fun save(ticket: Ticket): Long =
        ticketDao.upsert(
            ticket = ticket.toEntity(),
            lineas = ticket.lineas.map { it.toEntity() },
        )

    override suspend fun delete(id: Long) {
        // Las líneas se borran en cascada por la FK de la tabla `lineas`.
        ticketDao.deleteById(id)
    }

    override suspend fun getById(id: Long): Ticket? =
        ticketDao.getById(id)?.toDomain()

    override fun observeByDateRange(from: LocalDate, to: LocalDate): Flow<List<Ticket>> {
        // Rango inclusivo por fecha de emisión: se codifica como
        // [desde el inicio de `from`, hasta el inicio del día siguiente a `to`).
        val fromMillis = from.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val toMillisExclusive = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        return ticketDao.observeByDateRange(fromMillis, toMillisExclusive)
            .map { rows -> rows.map { it.toDomain() } }
    }
}

private fun Ticket.toEntity(): TicketEntity = TicketEntity(
    id = id,
    comercio = comercio,
    fechaEmision = fechaEmision,
    total = total,
    categoria = categoria.name,
    nota = nota,
    fechaCaptura = fechaCaptura,
    motor = motor?.name ?: MOTOR_MANUAL,
    rutaFoto = rutaFoto,
    textoCrudoOcr = textoCrudoOcr,
    formaPago = formaPago.name,
    entregado = entregado,
    cambio = cambio,
)

private fun Linea.toEntity(): LineaEntity = LineaEntity(
    concepto = concepto,
    cantidad = cantidad,
    precioUnitario = precioUnitario,
)

private fun TicketWithLineas.toDomain(): Ticket = Ticket(
    id = ticket.id,
    comercio = ticket.comercio,
    fechaEmision = ticket.fechaEmision,
    total = ticket.total,
    categoria = Categoria.valueOf(ticket.categoria),
    fechaCaptura = ticket.fechaCaptura,
    motor = motorDesde(ticket.motor),
    rutaFoto = ticket.rutaFoto,
    nota = ticket.nota,
    textoCrudoOcr = ticket.textoCrudoOcr,
    // Tolerante: un valor desconocido en BD (p. ej. de una version futura) es NO_INDICADA.
    formaPago = FormaPago.desde(ticket.formaPago),
    entregado = ticket.entregado,
    cambio = ticket.cambio,
    lineas = lineas.map { it.toDomain() },
)

private fun LineaEntity.toDomain(): Linea = Linea(
    concepto = concepto,
    cantidad = cantidad,
    precioUnitario = precioUnitario,
)

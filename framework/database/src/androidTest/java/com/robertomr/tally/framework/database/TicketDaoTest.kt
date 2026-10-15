package com.robertomr.tally.framework.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.robertomr.tally.framework.database.dao.TicketDao
import com.robertomr.tally.framework.database.entity.LineaEntity
import com.robertomr.tally.framework.database.entity.TicketEntity
import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests instrumentados de [TicketDao] con Room en memoria (spec
 * persistencia-room, escenarios 1-3): guardado transaccional del agregado,
 * reemplazo del conjunto de líneas, borrado en cascada y consulta por rango de
 * fechas de emisión vía Flow.
 */
@RunWith(AndroidJUnit4::class)
class TicketDaoTest {

    private lateinit var database: TallyDatabase
    private lateinit var dao: TicketDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TallyDatabase::class.java).build()
        dao = database.ticketDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertSavesTicketWithLinesAsAggregate() = runBlocking {
        val id = dao.upsert(
            ticket = ticket(),
            lineas = listOf(linea("Pan"), linea("Leche"), linea("Huevos")),
        )

        val guardado = dao.getById(id)
        assertNotNull(guardado)
        assertEquals("Mercadona", guardado?.ticket?.comercio)
        assertEquals(3, guardado?.lineas?.size)
        assertEquals(setOf("Pan", "Leche", "Huevos"), guardado?.lineas?.map { it.concepto }?.toSet())
    }

    @Test
    fun upsertReplacesTheWholeLineSetOnResave() = runBlocking {
        val id = dao.upsert(
            ticket = ticket(),
            lineas = listOf(linea("Pan"), linea("Leche")),
        )

        dao.upsert(
            ticket = ticket(id = id, comercio = "Carrefour"),
            lineas = listOf(linea("Huevos")),
        )

        val guardado = dao.getById(id)
        assertEquals("Carrefour", guardado?.ticket?.comercio)
        assertEquals(1, guardado?.lineas?.size)
        assertEquals("Huevos", guardado?.lineas?.single()?.concepto)
    }

    @Test
    fun deleteByIdRemovesTicketAndLinesInCascade() = runBlocking {
        val id = dao.upsert(
            ticket = ticket(),
            lineas = listOf(linea("Pan"), linea("Leche")),
        )

        val borradas = dao.deleteById(id)

        assertEquals(1, borradas)
        assertNull(dao.getById(id))
        // La cascada es por la FK de `lineas`: no quedan líneas huérfanas.
        assertEquals(0, countLineasOf(id))
    }

    @Test
    fun deleteByIdIsNoOpForUnknownId() = runBlocking {
        val borradas = dao.deleteById(999L)

        assertEquals(0, borradas)
    }

    @Test
    fun observeByDateRangeFiltersByEmissionDateAndEmitsOnSave() = runBlocking {
        val fromMillis = millisOf(2026, 10, 1, 0, 0)
        val toMillisExclusive = millisOf(2026, 11, 1, 0, 0)

        dao.observeByDateRange(fromMillis, toMillisExclusive).test {
            assertTrue(awaitItem().isEmpty())

            val idDentro = dao.upsert(
                ticket = ticket(fechaEmision = LocalDateTime.of(2026, 10, 15, 12, 0)),
                lineas = emptyList(),
            )
            assertEquals(listOf(idDentro), awaitItem().map { it.ticket.id })

            val idFuera = dao.upsert(
                ticket = ticket(fechaEmision = LocalDateTime.of(2026, 11, 20, 12, 0)),
                lineas = emptyList(),
            )
            // Fuera del rango: el Flow no debe emitir una lista nueva.
            expectNoEvents()

            // Dentro del rango: emisión actualizada sin volver a consultar.
            val idTambienDentro = dao.upsert(
                ticket = ticket(fechaEmision = LocalDateTime.of(2026, 10, 1, 23, 59)),
                lineas = emptyList(),
            )
            val emitido = awaitItem()
            assertEquals(setOf(idDentro, idTambienDentro), emitido.map { it.ticket.id }.toSet())
            assertTrue(idFuera !in emitido.map { it.ticket.id })
        }
    }

    private suspend fun countLineasOf(ticketId: Long): Int {
        var count = -1
        database.query("SELECT COUNT(*) FROM lineas WHERE ticket_id = ?", arrayOf(ticketId)).use { cursor ->
            if (cursor.moveToFirst()) count = cursor.getInt(0)
        }
        return count
    }

    private fun millisOf(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute)
            .toInstant(java.time.ZoneOffset.UTC)
            .toEpochMilli()

    private fun ticket(
        id: Long = 0L,
        comercio: String = "Mercadona",
        fechaEmision: LocalDateTime = LocalDateTime.of(2026, 10, 15, 12, 0),
    ): TicketEntity = TicketEntity(
        id = id,
        comercio = comercio,
        fechaEmision = fechaEmision,
        total = 10.0,
        categoria = "ALIMENTACION",
        nota = null,
        fechaCaptura = LocalDateTime.of(2026, 10, 15, 13, 0),
        motor = "LOCAL",
        rutaFoto = "/data/fotos/ticket.jpg",
        textoCrudoOcr = null,
    )

    private fun linea(concepto: String): LineaEntity = LineaEntity(
        concepto = concepto,
        cantidad = 1,
        precioUnitario = 1.5,
    )
}

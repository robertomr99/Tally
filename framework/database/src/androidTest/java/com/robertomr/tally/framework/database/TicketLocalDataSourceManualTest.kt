package com.robertomr.tally.framework.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.FormaPago
import com.robertomr.tally.domain.expenses.entities.Linea
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.Ticket
import com.robertomr.tally.domain.expenses.entities.esManual
import com.robertomr.tally.framework.database.entity.TicketEntity
import com.robertomr.tally.framework.database.source.MOTOR_MANUAL
import com.robertomr.tally.framework.database.source.TicketLocalDataSource
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
 * Ida y vuelta de un gasto manual por `TicketLocalDataSource` con Room en memoria
 * (spec gasto-manual): `motor = null` se guarda como "MANUAL" en la columna
 * `TEXT NOT NULL` con `ruta_foto = ""`, y un valor desconocido de `motor` se lee
 * como manual sin lanzar.
 */
@RunWith(AndroidJUnit4::class)
class TicketLocalDataSourceManualTest {

    private lateinit var database: TallyDatabase
    private lateinit var source: TicketLocalDataSource

    private val emision = LocalDateTime.of(2026, 10, 15, 0, 0)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TallyDatabase::class.java).build()
        source = TicketLocalDataSource(database.ticketDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun manual() = Ticket(
        comercio = "",
        fechaEmision = emision,
        total = 12.5,
        categoria = Categoria.OTROS,
        fechaCaptura = LocalDateTime.of(2026, 10, 20, 10, 0),
        motor = null,
        rutaFoto = "",
        lineas = listOf(Linea("CAFE", 2, 1.5)),
        formaPago = FormaPago.TARJETA,
    )

    @Test
    fun manualSeGuardaComoMANUALYSeLeeNulo() = runBlocking {
        val id = source.save(manual())

        val crudo = database.ticketDao().getById(id)!!.ticket
        assertEquals(MOTOR_MANUAL, crudo.motor)
        assertEquals("", crudo.rutaFoto)

        val leido = source.getById(id)!!
        assertNull(leido.motor)
        assertTrue(leido.esManual)
        assertEquals("", leido.rutaFoto)
        assertEquals(12.5, leido.total, 0.0)
        assertEquals(listOf(Linea("CAFE", 2, 1.5)), leido.lineas)
        assertEquals(FormaPago.TARJETA, leido.formaPago)
    }

    @Test
    fun escaneadoSigueConservandoSuMotor() = runBlocking {
        val id = source.save(manual().copy(motor = Motor.NUBE, rutaFoto = "fotos/x.jpg"))

        assertEquals("NUBE", database.ticketDao().getById(id)!!.ticket.motor)
        val leido = source.getById(id)!!
        assertEquals(Motor.NUBE, leido.motor)
        assertEquals("fotos/x.jpg", leido.rutaFoto)
    }

    @Test
    fun motorDesconocidoEnBdSeLeeComoManualSinLanzar() = runBlocking {
        val id = source.save(manual())
        val crudo = database.ticketDao().getById(id)!!.ticket
        val raro: TicketEntity = crudo.copy(motor = "FUTURO")
        database.ticketDao().upsert(raro, emptyList())

        val leido = source.getById(id)
        assertNotNull(leido)
        assertNull(leido!!.motor)
    }

    @Test
    fun borrarUnManualFuncionaYDejaElListadoVacio() = runBlocking {
        val id = source.save(manual())
        source.delete(id)
        assertNull(source.getById(id))
    }
}

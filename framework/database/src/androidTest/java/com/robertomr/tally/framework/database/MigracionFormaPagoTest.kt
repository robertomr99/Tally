package com.robertomr.tally.framework.database

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.FormaPago
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.Ticket
import com.robertomr.tally.framework.database.source.TicketLocalDataSource
import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test de migración 2 -> 3 (spec forma-pago, escenario 7) con
 * [MigrationTestHelper]: crea la BD en v2 con tickets y líneas, migra con
 * [MIGRATION_2_3], valida contra `schemas/.../3.json` (incluye el DEFAULT de
 * `forma_pago`) y comprueba que los datos se conservan con
 * `forma_pago = 'NO_INDICADA'` y `entregado`/`cambio` NULL.
 */
@RunWith(AndroidJUnit4::class)
class MigracionFormaPagoTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TallyDatabase::class.java,
    )

    @After
    fun tearDown() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(NOMBRE_BD)
        context.deleteDatabase(NOMBRE_BD_APP)
    }

    private fun sembrarV2(nombre: String) {
        helper.createDatabase(nombre, 2).apply {
            execSQL(
                "INSERT INTO tickets (id, comercio, fecha_emision, total, categoria, nota, " +
                    "fecha_captura, motor, ruta_foto, texto_crudo_ocr) VALUES " +
                    "(1, 'Mercadona', 1760000000000, 35.30, 'ALIMENTACION', 'nota uno', " +
                    "1760000001000, 'NUBE', 'fotos/1.jpg', 'TEXTO OCR 1'), " +
                    "(2, 'Repsol', 1760100000000, 62.0, 'TRANSPORTE', NULL, " +
                    "1760100001000, 'LOCAL', 'fotos/2.jpg', NULL)"
            )
            execSQL(
                "INSERT INTO lineas (id, ticket_id, concepto, cantidad, precio_unitario) VALUES " +
                    "(1, 1, 'Pan', 2, 1.25), (2, 1, 'Leche', 1, 0.95), (3, 2, 'Gasolina', 1, 62.0)"
            )
            close()
        }
    }

    @Test
    fun migrar2a3ConservaTicketsYLineasYAplicaLosValoresPorDefecto() {
        sembrarV2(NOMBRE_BD)

        val migrada = helper.runMigrationsAndValidate(NOMBRE_BD, 3, true, MIGRATION_2_3)

        migrada.query(
            "SELECT id, comercio, total, categoria, nota, motor, ruta_foto, texto_crudo_ocr, " +
                "forma_pago, entregado, cambio FROM tickets ORDER BY id",
        ).use { c ->
            assertEquals(2, c.count)
            c.moveToFirst()
            assertEquals("Mercadona", c.getString(1))
            assertEquals(35.30, c.getDouble(2), 0.0001)
            assertEquals("ALIMENTACION", c.getString(3))
            assertEquals("nota uno", c.getString(4))
            assertEquals("NUBE", c.getString(5))
            assertEquals("TEXTO OCR 1", c.getString(7))
            assertEquals("NO_INDICADA", c.getString(8))
            assertTrue(c.isNull(9))
            assertTrue(c.isNull(10))
            c.moveToNext()
            assertEquals("Repsol", c.getString(1))
            assertEquals(62.0, c.getDouble(2), 0.0001)
            assertEquals("NO_INDICADA", c.getString(8))
            assertTrue(c.isNull(9))
            assertTrue(c.isNull(10))
        }
        migrada.query("SELECT ticket_id, concepto, cantidad, precio_unitario FROM lineas ORDER BY id")
            .use { c ->
                assertEquals(3, c.count)
                c.moveToFirst()
                assertEquals(1L, c.getLong(0))
                assertEquals("Pan", c.getString(1))
                assertEquals(2, c.getInt(2))
                assertEquals(1.25, c.getDouble(3), 0.0001)
            }
    }

    @Test
    fun migrarDesdeV1HastaV3ValidaLaCadenaCompleta() {
        helper.createDatabase(NOMBRE_BD, 1).apply {
            execSQL(
                "INSERT INTO tickets (id, comercio, fecha_emision, total, categoria, nota, " +
                    "fecha_captura, motor, ruta_foto, texto_crudo_ocr) VALUES " +
                    "(5, 'Zara', 1760000000000, 19.99, 'OTROS', NULL, 1760000001000, 'LOCAL', 'f.jpg', NULL)"
            )
            close()
        }
        val migrada = helper.runMigrationsAndValidate(NOMBRE_BD, 3, true, MIGRATION_1_2, MIGRATION_2_3)
        migrada.query("SELECT comercio, forma_pago FROM tickets WHERE id = 5").use { c ->
            assertEquals(1, c.count)
            c.moveToFirst()
            assertEquals("Zara", c.getString(0))
            assertEquals("NO_INDICADA", c.getString(1))
        }
    }

    @Test
    fun roomRealAbreLaBdMigradaYElMapperManejaValoresConocidosYDesconocidos() = runBlocking {
        sembrarV2(NOMBRE_BD_APP)

        // Misma construcción que DatabaseModule: sin fallback destructivo.
        val room = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            TallyDatabase::class.java,
            NOMBRE_BD_APP,
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
        try {
            val fuente = TicketLocalDataSource(room.ticketDao())

            // Ticket antiguo: NO_INDICADA, sin entregado/cambio, líneas intactas.
            val antiguo = fuente.getById(1)!!
            assertEquals("Mercadona", antiguo.comercio)
            assertEquals(35.30, antiguo.total, 0.0001)
            assertEquals(FormaPago.NO_INDICADA, antiguo.formaPago)
            assertNull(antiguo.entregado)
            assertNull(antiguo.cambio)
            assertEquals(2, antiguo.lineas.size)

            // Ida y vuelta de un ticket en efectivo.
            val id = fuente.save(
                Ticket(
                    comercio = "Super",
                    fechaEmision = LocalDateTime.of(2026, 10, 20, 12, 0),
                    total = 13.90,
                    categoria = Categoria.ALIMENTACION,
                    fechaCaptura = LocalDateTime.of(2026, 10, 20, 12, 1),
                    motor = Motor.NUBE,
                    rutaFoto = "fotos/3.jpg",
                    formaPago = FormaPago.EFECTIVO,
                    entregado = 50.0,
                    cambio = 36.10,
                ),
            )
            val efectivo = fuente.getById(id)!!
            assertEquals(FormaPago.EFECTIVO, efectivo.formaPago)
            assertEquals(50.0, efectivo.entregado!!, 0.0)
            assertEquals(36.10, efectivo.cambio!!, 0.0)
            assertEquals(13.90, efectivo.total, 0.0)

            // Valor desconocido en BD (p. ej. de una versión futura): no revienta.
            room.openHelper.writableDatabase.execSQL(
                "UPDATE tickets SET forma_pago = 'BIZUM' WHERE id = 2",
            )
            assertEquals(FormaPago.NO_INDICADA, fuente.getById(2)!!.formaPago)
        } finally {
            room.close()
        }
    }

    private companion object {
        const val NOMBRE_BD = "migracion-pago-test.db"
        const val NOMBRE_BD_APP = "migracion-pago-test-room.db"
    }
}

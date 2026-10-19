package com.robertomr.tally.framework.database

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.robertomr.tally.framework.database.entity.ExtraccionLogEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test de migración 1 -> 2 (spec log-extracciones, D1/escenario 10) con
 * [MigrationTestHelper]: crea la BD en v1 con tickets y líneas, migra con
 * [MIGRATION_1_2] y comprueba que (a) el esquema resultante coincide con
 * `schemas/.../2.json`, (b) los tickets y líneas se conservan intactos y (c) la
 * tabla `extracciones_log` existe vacía y es utilizable.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TallyDatabase::class.java,
    )

    @After
    fun tearDown() {
        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(NOMBRE_BD_APP)
    }

    @Test
    fun migrar1a2ConservaTicketsYLineasYCreaLaTablaVaciaDelLog() {
        helper.createDatabase(NOMBRE_BD, 1).apply {
            execSQL(
                "INSERT INTO tickets (id, comercio, fecha_emision, total, categoria, nota, " +
                    "fecha_captura, motor, ruta_foto, texto_crudo_ocr) VALUES " +
                    "(1, 'Mercadona', 1760000000000, 35.30, 'COMIDA', 'nota uno', " +
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

        val migrada = helper.runMigrationsAndValidate(NOMBRE_BD, 2, true, MIGRATION_1_2)

        migrada.query("SELECT id, comercio, fecha_emision, total, categoria, nota, fecha_captura, motor, ruta_foto, texto_crudo_ocr FROM tickets ORDER BY id")
            .use { c ->
                assertEquals(2, c.count)
                c.moveToFirst()
                assertEquals("Mercadona", c.getString(1))
                assertEquals(1760000000000L, c.getLong(2))
                assertEquals(35.30, c.getDouble(3), 0.0001)
                assertEquals("COMIDA", c.getString(4))
                assertEquals("nota uno", c.getString(5))
                assertEquals(1760000001000L, c.getLong(6))
                assertEquals("NUBE", c.getString(7))
                assertEquals("fotos/1.jpg", c.getString(8))
                assertEquals("TEXTO OCR 1", c.getString(9))
                c.moveToNext()
                assertEquals("Repsol", c.getString(1))
                assertTrue(c.isNull(5))
                assertTrue(c.isNull(9))
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
        migrada.query("SELECT COUNT(*) FROM extracciones_log").use { c ->
            c.moveToFirst()
            assertEquals(0, c.getInt(0))
        }
    }

    @Test
    fun laBdMigradaSeAbreConRoomRealYElLogFunciona() = runBlocking {
        helper.createDatabase(NOMBRE_BD_APP, 1).apply {
            execSQL(
                "INSERT INTO tickets (id, comercio, fecha_emision, total, categoria, nota, " +
                    "fecha_captura, motor, ruta_foto, texto_crudo_ocr) VALUES " +
                    "(7, 'Zara', 1760000000000, 19.99, 'OTROS', NULL, 1760000001000, 'LOCAL', 'f.jpg', NULL)"
            )
            close()
        }

        // Misma construcción que DatabaseModule: solo MIGRATION_1_2, sin fallback destructivo.
        val room = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            TallyDatabase::class.java,
            NOMBRE_BD_APP,
        ).addMigrations(MIGRATION_1_2).build()
        try {
            val ticket = room.ticketDao().getById(7)
            assertEquals("Zara", ticket?.ticket?.comercio)

            room.extraccionLogDao().insertarYRecortar(
                ExtraccionLogEntity(
                    marcaTiempo = 1L, motor = "NUBE", origen = "ROUTER", resultado = "EXITO",
                    tipoFallo = null, claseError = null, duracionMs = 10, longitudTexto = 5,
                ),
                maximo = 500,
            )
            assertEquals(1, room.extraccionLogDao().observarRecientes(10).first().size)
            // El ticket sigue ahi tras escribir en el log.
            assertEquals("Zara", room.ticketDao().getById(7)?.ticket?.comercio)
        } finally {
            room.close()
        }
    }

    private companion object {
        /** BD temporal del helper (no es la BD real de la app). */
        const val NOMBRE_BD = "migracion-test.db"
        const val NOMBRE_BD_APP = "migracion-test-room.db"
    }
}

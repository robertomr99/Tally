package com.robertomr.tally.framework.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.robertomr.tally.domain.expenses.data.RegistroExtracciones
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.OrigenExtraccion
import com.robertomr.tally.domain.expenses.entities.RegistroExtraccion
import com.robertomr.tally.domain.expenses.entities.ResultadoExtraccion
import com.robertomr.tally.domain.expenses.entities.TipoFallo
import com.robertomr.tally.framework.database.source.RegistroExtraccionesLocalDataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests instrumentados del log de extracciones con Room en memoria (spec
 * log-extracciones, escenario 8): retención de 500 filas, orden descendente,
 * round-trip de campos y borrado total.
 */
@RunWith(AndroidJUnit4::class)
class ExtraccionLogDaoTest {

    private lateinit var database: TallyDatabase
    private lateinit var registro: RegistroExtracciones

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TallyDatabase::class.java).build()
        registro = RegistroExtraccionesLocalDataSource(database.extraccionLogDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun fila(marca: Long) = RegistroExtraccion(
        marcaTiempo = marca,
        motor = Motor.NUBE,
        origen = OrigenExtraccion.ROUTER,
        resultado = ResultadoExtraccion.EXITO,
        duracionMs = 10,
        longitudTexto = 3,
    )

    @Test
    fun tras510InsercionesSoloQuedanLas500MasRecientes() = runBlocking {
        repeat(510) { registro.registrar(fila(marca = it.toLong())) }

        val todas = registro.observarRecientes(1_000).first()

        assertEquals(RegistroExtracciones.MAXIMO_FILAS, todas.size)
        assertEquals(509L, todas.first().marcaTiempo)
        assertEquals(10L, todas.last().marcaTiempo) // las marcas 0..9 se recortaron
    }

    @Test
    fun recientesVienenDelMasNuevoAlMasAntiguoYRespetanElLimite() = runBlocking {
        listOf(30L, 10L, 20L).forEach { registro.registrar(fila(it)) }

        val recientes = registro.observarRecientes(2).first()

        assertEquals(listOf(30L, 20L), recientes.map { it.marcaTiempo })
    }

    @Test
    fun conservaTodosLosCamposIncluidoElFallo() = runBlocking {
        registro.registrar(
            RegistroExtraccion(
                marcaTiempo = 42,
                motor = Motor.LOCAL,
                origen = OrigenExtraccion.REINTENTO,
                resultado = ResultadoExtraccion.FALLO,
                tipoFallo = TipoFallo.RESPUESTA_INVALIDA,
                claseError = "InvalidExtractionException",
                duracionMs = 1_234,
                longitudTexto = 987,
            ),
        )
        registro.registrar(fila(43))

        val (nueva, antigua) = registro.observarRecientes(10).first()

        assertEquals(43L, nueva.marcaTiempo)
        assertNull(nueva.tipoFallo)
        assertEquals(Motor.LOCAL, antigua.motor)
        assertEquals(OrigenExtraccion.REINTENTO, antigua.origen)
        assertEquals(ResultadoExtraccion.FALLO, antigua.resultado)
        assertEquals(TipoFallo.RESPUESTA_INVALIDA, antigua.tipoFallo)
        assertEquals("InvalidExtractionException", antigua.claseError)
        assertEquals(1_234L, antigua.duracionMs)
        assertEquals(987, antigua.longitudTexto)
    }

    @Test
    fun borrarTodoVaciaElLog() = runBlocking {
        repeat(5) { registro.registrar(fila(it.toLong())) }

        registro.borrarTodo()

        assertTrue(registro.observarRecientes(10).first().isEmpty())
    }
}

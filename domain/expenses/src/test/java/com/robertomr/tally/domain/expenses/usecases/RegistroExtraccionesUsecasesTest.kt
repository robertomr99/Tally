package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.data.RegistroExtracciones
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.OrigenExtraccion
import com.robertomr.tally.domain.expenses.entities.RegistroExtraccion
import com.robertomr.tally.domain.expenses.entities.ResultadoExtraccion
import com.robertomr.tally.domain.expenses.entities.TipoFallo
import com.robertomr.tally.test.unit.FakeRegistroExtracciones
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests JVM de los casos de uso del log: resumen, ultimas 20, CSV y borrado. */
class RegistroExtraccionesUsecasesTest {

    private val registro = FakeRegistroExtracciones()

    private fun fila(
        motor: Motor = Motor.NUBE,
        resultado: ResultadoExtraccion = ResultadoExtraccion.EXITO,
        duracionMs: Long = 100,
        marca: Long = 0,
    ) = RegistroExtraccion(
        marcaTiempo = marca,
        motor = motor,
        origen = OrigenExtraccion.ROUTER,
        resultado = resultado,
        duracionMs = duracionMs,
        longitudTexto = 10,
    )

    @Test
    fun resumenPorMotorCuentaTotalExitosYMediaDeLosExitos() = runTest {
        registro.registrar(fila(Motor.NUBE, duracionMs = 1_000))
        registro.registrar(fila(Motor.NUBE, duracionMs = 3_000))
        registro.registrar(fila(Motor.NUBE, ResultadoExtraccion.FALLO, duracionMs = 99_000))
        registro.registrar(fila(Motor.LOCAL, ResultadoExtraccion.CANCELADA, duracionMs = 5))

        val resumen = ObservarRegistroExtracciones(registro)().first().resumen

        assertEquals(2, resumen.size)
        val local = resumen.first { it.motor == Motor.LOCAL }
        assertEquals(1, local.total)
        assertEquals(0, local.exitos)
        assertNull(local.duracionMediaExitoMs)
        val nube = resumen.first { it.motor == Motor.NUBE }
        assertEquals(3, nube.total)
        assertEquals(2, nube.exitos)
        assertEquals(2_000L, nube.duracionMediaExitoMs)
    }

    @Test
    fun muestraSoloLasUltimas20PeroResumeTodo() = runTest {
        repeat(25) { registro.registrar(fila(marca = it.toLong())) }

        val diagnostico = ObservarRegistroExtracciones(registro)().first()

        assertEquals(20, diagnostico.recientes.size)
        assertEquals(25, diagnostico.resumen.single().total)
        assertEquals(25L, diagnostico.recientes.first().id) // la mas reciente primero
    }

    @Test
    fun borrarVaciaElRegistro() = runTest {
        registro.registrar(fila())

        BorrarRegistroExtracciones(registro)()

        assertTrue(registro.registrados.isEmpty())
    }

    @Test
    fun csvLlevaCabeceraOrdenYSoloMetadatos() = runTest {
        registro.registrar(fila(marca = 0, duracionMs = 12))
        registro.registrar(
            fila(Motor.LOCAL, ResultadoExtraccion.FALLO, 7, marca = 1_000).copy(
                tipoFallo = TipoFallo.OTRO,
                claseError = "Raro,\"Error\"",
            ),
        )

        val lineas = ExportarRegistroExtracciones(registro)().trimEnd().lines()

        assertEquals(
            "id,instante_iso,motor,origen,resultado,tipo_fallo,clase_error,duracion_ms,longitud_texto",
            lineas[0],
        )
        // Mas reciente primero (mismo orden que observarRecientes).
        assertEquals("2,1970-01-01T00:00:01Z,LOCAL,ROUTER,FALLO,OTRO,\"Raro,\"\"Error\"\"\",7,10", lineas[1])
        assertEquals("1,1970-01-01T00:00:00Z,NUBE,ROUTER,EXITO,,,12,10", lineas[2])
        assertEquals(3, lineas.size)
    }

    @Test
    fun csvDeRegistroVacioSoloTieneLaCabecera() = runTest {
        assertEquals(CABECERA_CSV + "\n", ExportarRegistroExtracciones(registro)())
    }

    @Test
    fun laRetencionDelContratoEs500() {
        assertEquals(500, RegistroExtracciones.MAXIMO_FILAS)
    }
}

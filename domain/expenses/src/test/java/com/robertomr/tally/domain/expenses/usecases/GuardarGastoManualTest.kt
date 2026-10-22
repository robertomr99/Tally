package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.data.TicketRepository
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.ErrorGastoManual
import com.robertomr.tally.domain.expenses.entities.FormaPago
import com.robertomr.tally.domain.expenses.entities.Linea
import com.robertomr.tally.domain.expenses.entities.LineaEditable
import com.robertomr.tally.domain.expenses.entities.datosGastoManualInicial
import com.robertomr.tally.domain.expenses.entities.errorGastoManual
import com.robertomr.tally.domain.expenses.entities.esManual
import com.robertomr.tally.test.unit.CoroutineTestRule
import com.robertomr.tally.test.unit.FakePhotoFileStore
import com.robertomr.tally.test.unit.FakeTicketDataSource
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Tests JVM de la validacion minima y de [GuardarGastoManual] (spec gasto-manual):
 * el ticket sale sin motor ni foto, el pago se normaliza, el total no se deriva de
 * las lineas y los datos invalidos no se persisten. Incluye que editar y borrar un
 * manual conserva `motor = null` / `rutaFoto = ""`.
 */
class GuardarGastoManualTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private val dataSource = FakeTicketDataSource()
    private val photoFileStore = FakePhotoFileStore()
    private val repository = TicketRepository(dataSource, photoFileStore)
    private val guardar = GuardarGastoManual(repository)

    private val hoy = LocalDate.of(2026, 10, 20)

    private fun valido(total: Double = 12.5) =
        datosGastoManualInicial(LocalDate.now()).copy(total = total)

    // --- datos iniciales y validacion ---

    @Test
    fun datosInicialesSonHoyALas0000OtrosYTotalCero() {
        val d = datosGastoManualInicial(hoy)
        assertEquals("", d.comercio)
        assertEquals(LocalDateTime.of(2026, 10, 20, 0, 0), d.fechaEmision)
        assertEquals(0.0, d.total, 0.0)
        assertEquals(Categoria.OTROS, d.categoria)
        assertTrue(d.lineas.isEmpty())
        assertEquals(FormaPago.NO_INDICADA, d.formaPago)
    }

    @Test
    fun totalCeroNegativoONoFinitoEsTotalNoPositivo() {
        val base = datosGastoManualInicial(hoy)
        for (total in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertEquals(
                "total=$total",
                ErrorGastoManual.TOTAL_NO_POSITIVO,
                base.copy(total = total).errorGastoManual(hoy),
            )
        }
    }

    @Test
    fun fechaFuturaEsError_hoyYAyerNo() {
        val base = datosGastoManualInicial(hoy).copy(total = 5.0)
        assertEquals(
            ErrorGastoManual.FECHA_FUTURA,
            base.copy(fechaEmision = hoy.plusDays(1).atStartOfDay()).errorGastoManual(hoy),
        )
        assertNull(base.copy(fechaEmision = hoy.atStartOfDay()).errorGastoManual(hoy))
        assertNull(base.copy(fechaEmision = hoy.minusDays(1).atStartOfDay()).errorGastoManual(hoy))
        // Hoy con hora tardia tampoco es futura (se compara por fecha).
        assertNull(base.copy(fechaEmision = hoy.atTime(23, 59)).errorGastoManual(hoy))
    }

    @Test
    fun totalTienePrioridadSobreFechaYComercioVacioNoBloquea() {
        val ambos = datosGastoManualInicial(hoy).copy(fechaEmision = hoy.plusDays(3).atStartOfDay())
        assertEquals(ErrorGastoManual.TOTAL_NO_POSITIVO, ambos.errorGastoManual(hoy))
        assertNull(datosGastoManualInicial(hoy).copy(total = 1.0, comercio = "").errorGastoManual(hoy))
    }

    // --- GuardarGastoManual ---

    @Test
    fun guardaTicketSinMotorNiFotoConFechaDeCapturaAhora() = runTest {
        val antes = LocalDateTime.now()
        val id = guardar(valido(12.5).copy(comercio = "KIOSCO"))

        val t = repository.getById(id)!!
        assertTrue(id > 0L)
        assertEquals("KIOSCO", t.comercio)
        assertEquals(12.5, t.total, 0.0)
        assertEquals(Categoria.OTROS, t.categoria)
        assertNull(t.motor)
        assertTrue(t.esManual)
        assertEquals("", t.rutaFoto)
        assertNull(t.textoCrudoOcr)
        assertNull(t.nota)
        assertFalse("fechaCaptura = ahora", t.fechaCaptura.isBefore(antes))
    }

    @Test
    fun comercioVacioSePersiste() = runTest {
        val id = guardar(valido())
        assertEquals("", repository.getById(id)!!.comercio)
    }

    @Test
    fun totalEscritoNoSeDerivaDeLasLineasYSeFiltranLasVacias() = runTest {
        val datos = valido(10.0).copy(
            lineas = listOf(
                LineaEditable(0, "CAFE", 2, 1.5),
                LineaEditable(1, "   ", 1, 99.0),
            ),
        )
        val t = repository.getById(guardar(datos))!!
        assertEquals(10.0, t.total, 0.0)
        assertEquals(listOf(Linea("CAFE", 2, 1.5)), t.lineas)
    }

    @Test
    fun pagoSeNormalizaComoEnLaRevision() = runTest {
        // Efectivo con entregado y cambio coherentes se conserva...
        val efectivo = valido(8.0).copy(formaPago = FormaPago.EFECTIVO, entregado = 10.0, cambio = 2.0)
        val te = repository.getById(guardar(efectivo))!!
        assertEquals(FormaPago.EFECTIVO, te.formaPago)
        assertEquals(10.0, te.entregado!!, 0.0)
        assertEquals(2.0, te.cambio!!, 0.0)
        // ...y con tarjeta entregado/cambio se descartan.
        val tarjeta = valido(8.0).copy(formaPago = FormaPago.TARJETA, entregado = 10.0, cambio = 2.0)
        val tt = repository.getById(guardar(tarjeta))!!
        assertEquals(FormaPago.TARJETA, tt.formaPago)
        assertNull(tt.entregado)
        assertNull(tt.cambio)
    }

    @Test
    fun datosInvalidosLanzanYNoSePersiste() = runTest {
        val futura = valido().copy(fechaEmision = LocalDate.now().plusDays(1).atStartOfDay())
        for (invalido in listOf(datosGastoManualInicial(), futura)) {
            val fallo = runCatching { guardar(invalido) }.exceptionOrNull()
            assertTrue(fallo is IllegalArgumentException)
        }
        assertNull(repository.getById(1L))
    }

    // --- editar y borrar un manual ---

    @Test
    fun actualizarConservaMotorNuloYRutaVacia() = runTest {
        val id = guardar(valido(12.5))
        val original = repository.getById(id)!!

        ActualizarTicket(repository)(original, valido(20.0).copy(comercio = "NUEVO"))

        val t = repository.getById(id)!!
        assertEquals(20.0, t.total, 0.0)
        assertEquals("NUEVO", t.comercio)
        assertNull(t.motor)
        assertEquals("", t.rutaFoto)
    }

    @Test
    fun borrarUnManualNoFallaYSoloIntentaBorrarRutaVacia() = runTest {
        val id = guardar(valido(12.5))

        BorrarTicket(repository)(id)

        assertNull(repository.getById(id))
        assertEquals(listOf(""), photoFileStore.deletedPaths)
    }
}

package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.data.TicketRepository
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.DatosRevision
import com.robertomr.tally.domain.expenses.entities.Linea
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.TicketExtraido
import com.robertomr.tally.domain.expenses.entities.Ticket
import com.robertomr.tally.domain.expenses.entities.datosRevisionInicial
import com.robertomr.tally.test.unit.CoroutineTestRule
import com.robertomr.tally.test.unit.FakePhotoFileStore
import com.robertomr.tally.test.unit.FakeTicketDataSource
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Tests JVM de [GuardarTicket] (spec revision-datos, T1/T5 y escenarios 1, 2 y
 * 4): el mapeo `TicketExtraido` + `DatosRevision` -> `Ticket` y la persistencia
 * por `TicketRepository.save` con los dobles de :test:unit.
 */
class GuardarTicketTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private val dataSource = FakeTicketDataSource()
    private val repository = TicketRepository(dataSource, FakePhotoFileStore())
    private val guardarTicket = GuardarTicket(repository)

    private val fechaExtraida = LocalDateTime.of(2026, 10, 15, 12, 0)
    private val extraido = TicketExtraido(
        comercio = "MERCADONA",
        fechaEmision = fechaExtraida,
        total = 35.30,
        lineas = listOf(
            Linea("LECHE", 2, 1.05),
            Linea("PAN", 1, 0.90),
        ),
        categoria = Categoria.ALIMENTACION,
        motor = Motor.NUBE,
    )

    @Test
    fun guardarConLineasFotoYTextoPersisteElAgregadoCompleto() = runTest {
        val antes = LocalDateTime.now()
        val datos = DatosRevision(
            comercio = "MERCADONA CORREGIDO",
            fechaEmision = LocalDateTime.of(2026, 10, 14, 20, 30),
            total = 35.30,
            categoria = Categoria.HOGAR,
        )

        val id = guardarTicket(
            extraido = extraido,
            rutaFoto = "fotos/captura_test.jpg",
            textoCrudo = "MERCADONA\nTOTAL 35,30",
            datos = datos,
        )

        val ticket = repository.getById(id)
        assertTrue("el ticket guardado debe existir", ticket != null)
        ticket as Ticket
        assertTrue("el id debe asignarse", id > 0L)
        assertEquals(id, ticket.id)
        // Cabecera corregida.
        assertEquals("MERCADONA CORREGIDO", ticket.comercio)
        assertEquals(datos.fechaEmision, ticket.fechaEmision)
        assertEquals(35.30, ticket.total, 0.0)
        assertEquals(Categoria.HOGAR, ticket.categoria)
        // Del resultado de la extraccion.
        assertEquals(extraido.motor, ticket.motor)
        assertEquals(extraido.lineas, ticket.lineas)
        // Del Borrador / mapeo.
        assertEquals("fotos/captura_test.jpg", ticket.rutaFoto)
        assertEquals("MERCADONA\nTOTAL 35,30", ticket.textoCrudoOcr)
        assertNull(ticket.nota)
        assertTrue(
            "fechaCaptura = ahora, no antes de la llamada",
            !ticket.fechaCaptura.isBefore(antes),
        )
    }

    @Test
    fun extraccionVaciaConDefaultsSePersisteSinBloquear() = runTest {
        val hoy = LocalDateTime.of(2026, 10, 17, 9, 0)
        val vacio = TicketExtraido(motor = Motor.NUBE)

        // Escenario 2: sin datos, los defaults no bloquean (comercio "",
        // fecha hoy, total 0.0, categoria OTROS).
        val id = guardarTicket(
            extraido = vacio,
            rutaFoto = "fotos/captura_vacia.jpg",
            textoCrudo = "",
            datos = datosRevisionInicial(vacio, hoy),
        )

        val ticket = repository.getById(id)!!
        assertEquals("", ticket.comercio)
        assertEquals(hoy, ticket.fechaEmision)
        assertEquals(0.0, ticket.total, 0.0)
        assertEquals(Categoria.OTROS, ticket.categoria)
        assertTrue(ticket.lineas.isEmpty())
    }

    @Test
    fun observeByDateRangeEmiteElTicketEnLaFechaCorregida() = runTest {
        val datos = DatosRevision(
            comercio = "TIENDA",
            fechaEmision = LocalDateTime.of(2026, 9, 5, 18, 0),
            total = 12.0,
            categoria = Categoria.OCIO,
        )

        val id = guardarTicket(extraido, "fotos/captura_test.jpg", "texto", datos)

        // Escenario 1 (R-REV1): el ticket guardado es observable por fecha de
        // emision corregida, no por la fecha de captura.
        val emitidos = repository
            .observeByDateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))
            .first()
        assertEquals(listOf(id), emitidos.map { it.id })

        val fueraDeRango = repository
            .observeByDateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))
            .first()
        assertTrue(fueraDeRango.isEmpty())
    }

    @Test
    fun datosRevisionInicialRellenaSoloLosCamposAusentes() = runTest {
        val hoy = LocalDateTime.of(2026, 10, 17, 9, 0)

        val parcial = datosRevisionInicial(extraido, hoy)
        assertEquals("MERCADONA", parcial.comercio)
        assertEquals(fechaExtraida, parcial.fechaEmision)
        assertEquals(35.30, parcial.total, 0.0)
        assertEquals(Categoria.ALIMENTACION, parcial.categoria)

        val vacio = datosRevisionInicial(TicketExtraido(motor = Motor.LOCAL), hoy)
        assertEquals("", vacio.comercio)
        assertEquals(hoy, vacio.fechaEmision)
        assertEquals(0.0, vacio.total, 0.0)
        assertEquals(Categoria.OTROS, vacio.categoria)
    }

    @Test
    fun cancellationIsNeverSwallowed() {
        dataSource.saveError = CancellationException("guardado cancelado")

        val error = assertThrows(CancellationException::class.java) {
            kotlinx.coroutines.runBlocking {
                guardarTicket(
                    extraido,
                    "fotos/captura_test.jpg",
                    "texto",
                    datosRevisionInicial(extraido),
                )
            }
        }

        assertEquals("guardado cancelado", error.message)
    }
}

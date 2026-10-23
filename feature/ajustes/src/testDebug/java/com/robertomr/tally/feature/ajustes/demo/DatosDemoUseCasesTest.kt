package com.robertomr.tally.feature.ajustes.demo

import com.robertomr.tally.domain.expenses.data.TicketRepository
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.FormaPago
import com.robertomr.tally.domain.expenses.entities.Linea
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.Ticket
import com.robertomr.tally.test.unit.FakePhotoFileStore
import com.robertomr.tally.test.unit.FakeTicketDataSource
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests JVM de sembrar/quitar con repositorio sobre fuente en memoria (spec seed-demo, tarea 3). */
class DatosDemoUseCasesTest {

    private val hoy = LocalDate.of(2026, 10, 22)
    private val fuente = FakeTicketDataSource()
    private val fotos = FakePhotoFileStore()
    private val repo = TicketRepository(fuente, fotos)
    private val sembrar = SembrarDatosDemo(repo)
    private val quitar = QuitarDatosDemo(repo)
    private val observar = ObservarDatosDemo(repo)

    private suspend fun todos(): List<Ticket> =
        repo.observeByDateRange(LocalDate.of(2000, 1, 1), LocalDate.of(2100, 12, 31)).first()

    /** Ticket real: mismo comercio y fecha que uno de demo, sin marca. */
    private fun real(
        comercio: String = "Super Barrio",
        nota: String? = null,
        ruta: String = "fotos/real.jpg",
    ) = Ticket(
        comercio = comercio,
        fechaEmision = hoy.atTime(9, 5),
        total = 12.30,
        categoria = Categoria.ALIMENTACION,
        fechaCaptura = LocalDateTime.of(2026, 10, 22, 9, 6),
        motor = Motor.NUBE,
        rutaFoto = ruta,
        nota = nota,
        textoCrudoOcr = "texto real",
        lineas = listOf(Linea("Pan", 1, 1.0)),
        formaPago = FormaPago.TARJETA,
    )

    @Test
    fun sembrarInsertaTodoElConjunto() = runTest {
        val r = sembrar(hoy)
        assertEquals(ResultadoSembrado(13, 0), r)
        assertEquals(13, todos().size)
        assertEquals(13, observar().first().size)
        assertEquals("13 tickets sembrados", textoSembrado(r))
    }

    @Test
    fun repetirNoDuplica() = runTest {
        sembrar(hoy)
        val r = sembrar(hoy)
        assertEquals(ResultadoSembrado(0, 13), r)
        assertEquals(13, todos().size)
        assertEquals("Ya estaban los 13 tickets de demo", textoSembrado(r))
    }

    @Test
    fun repetirOtroDiaNoDuplicaPorqueLaClaveNoEsLaFecha() = runTest {
        sembrar(hoy)
        val r = sembrar(hoy.plusDays(9))
        assertEquals(ResultadoSembrado(0, 13), r)
        assertEquals(13, todos().size)
    }

    @Test
    fun siFaltaUnoSeRepoeSoloEse() = runTest {
        sembrar(hoy)
        val borrado = todos().first { it.nota == marcaDemo("D04") }
        repo.delete(borrado.id)
        val r = sembrar(hoy)
        assertEquals(ResultadoSembrado(1, 12), r)
        assertEquals(13, todos().size)
        assertEquals("1 sembrado, resto ya estaban",textoSembrado(r))
    }

    @Test
    fun quitarBorraExactamenteLosDeDemo() = runTest {
        sembrar(hoy)
        assertEquals(13, quitar())
        assertTrue(todos().isEmpty())
        assertEquals("13 tickets de demo quitados", textoQuitado(13))
    }

    @Test
    fun quitarSinDatosDemoDiceQueNoHabia() = runTest {
        assertEquals(0, quitar())
        assertEquals("No había datos de demo", textoQuitado(0))
    }

    @Test
    fun aislamientoNiSembrarNiQuitarTocanTicketsReales() = runTest {
        // Mismo comercio y fecha que D01, y otro con nota propia que NO es de demo.
        val idReal = repo.save(real())
        val idConNota = repo.save(real(comercio = "Otro", nota = "mi nota"))
        val idNotaParecida = repo.save(real(comercio = "Parecida", nota = "tally-demo:v0:D01"))
        val antes = todos().filter { it.id in listOf(idReal, idConNota, idNotaParecida) }.sortedBy { it.id }

        sembrar(hoy)
        sembrar(hoy)
        assertEquals(13, quitar())

        val despues = todos()
        assertEquals(3, despues.size)
        assertEquals(antes, despues.sortedBy { it.id })
        assertTrue(fotos.deletedPaths.none { it == "fotos/real.jpg" })
    }

    @Test
    fun quitarNoBorraTicketsRealesAunqueNoHayaDemo() = runTest {
        repo.save(real())
        assertEquals(0, quitar())
        assertEquals(1, todos().size)
    }

    @Test
    fun unTicketDeDemoEditadoSigueSiendoDeDemo() = runTest {
        sembrar(hoy)
        val d13 = todos().first { it.nota == marcaDemo("D13") }
        repo.save(d13.copy(comercio = "Corregido", total = 9.5)) // como ActualizarTicket: conserva nota
        assertEquals(ResultadoSembrado(0, 13), sembrar(hoy))
        assertEquals(13, quitar())
    }

    @Test
    fun losDeDemoNoTienenFotoYBorrarlosLlamaAlAlmacenConRutaVacia() = runTest {
        sembrar(hoy)
        quitar()
        assertTrue(fotos.deletedPaths.all { it.isEmpty() })
    }
}

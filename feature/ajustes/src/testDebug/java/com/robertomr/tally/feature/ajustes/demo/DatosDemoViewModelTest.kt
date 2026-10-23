package com.robertomr.tally.feature.ajustes.demo

import app.cash.turbine.test
import com.robertomr.tally.domain.expenses.data.TicketDataSource
import com.robertomr.tally.domain.expenses.data.TicketRepository
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.Ticket
import com.robertomr.tally.test.unit.CoroutineTestRule
import com.robertomr.tally.test.unit.FakePhotoFileStore
import com.robertomr.tally.test.unit.FakeTicketDataSource
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Tests JVM de [DatosDemoViewModel] (spec seed-demo, tarea 4). */
class DatosDemoViewModelTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private val hoy = LocalDate.of(2026, 10, 22)
    private val fuente = FakeTicketDataSource()

    /** Fuente que retiene `save` hasta que el test lo libera (para el doble toque). */
    private class FuenteRetenida(
        private val delegada: FakeTicketDataSource,
        val puerta: CompletableDeferred<Unit>,
    ) : TicketDataSource by delegada {
        var guardados = 0
        override suspend fun save(ticket: Ticket): Long {
            puerta.await()
            guardados++
            return delegada.save(ticket)
        }
    }

    private fun crear(source: TicketDataSource = fuente): DatosDemoViewModel {
        val repo = TicketRepository(source, FakePhotoFileStore())
        return DatosDemoViewModel(ObservarDatosDemo(repo), SembrarDatosDemo(repo), QuitarDatosDemo(repo))
    }

    private fun real() = Ticket(
        comercio = "Super Barrio",
        fechaEmision = hoy.atTime(9, 5),
        total = 3.0,
        categoria = Categoria.HOGAR,
        fechaCaptura = hoy.atTime(9, 6),
        rutaFoto = "",
    )

    @Test
    fun estadoInicialVacio() = runTest {
        crear().uiState.test {
            assertEquals(DatosDemoUiState(0, false, null), awaitItem())
        }
    }

    @Test
    fun sembrarActualizaContadorYResultado() = runTest {
        val vm = crear()
        vm.uiState.test {
            awaitItem() // inicial
            vm.onSembrar(hoy)
            var ultimo = awaitItem()
            while (ultimo.resultado == null || ultimo.contador != 13 || ultimo.ocupado) ultimo = awaitItem()
            assertEquals("13 tickets sembrados", ultimo.resultado)
            assertEquals(13, ultimo.contador)
        }
    }

    @Test
    fun repetirDiceYaEstaban() = runTest {
        val vm = crear()
        vm.uiState.test {
            vm.onSembrar(hoy)
            var s = awaitItem()
            while (s.contador != 13 || s.ocupado || s.resultado == null) s = awaitItem()
            vm.onSembrar(hoy)
            s = awaitItem()
            while (s.resultado != "Ya estaban los 13 tickets de demo") s = awaitItem()
            assertEquals(13, s.contador)
        }
    }

    @Test
    fun quitarVaciaElContadorSinTocarLoReal() = runTest {
        fuente.save(real())
        val vm = crear()
        vm.uiState.test {
            vm.onSembrar(hoy)
            var s = awaitItem()
            while (s.contador != 13 || s.ocupado || s.resultado == null) s = awaitItem()
            vm.onQuitar()
            s = awaitItem()
            while (s.resultado != "13 tickets de demo quitados") s = awaitItem()
            assertEquals(0, s.contador)
        }
        assertEquals(1, fuente.observeByDateRange(LocalDate.of(2000, 1, 1), LocalDate.of(2100, 1, 1)).first().size)
    }

    @Test
    fun quitarSinDatosDiceNoHabia() = runTest {
        val vm = crear()
        vm.uiState.test {
            awaitItem()
            vm.onQuitar()
            var s = awaitItem()
            while (s.resultado != "No había datos de demo") s = awaitItem()
            assertFalse(s.ocupado)
        }
    }

    @Test
    fun dobleToqueNoDuplica() = runTest {
        val puerta = CompletableDeferred<Unit>()
        val retenida = FuenteRetenida(fuente, puerta)
        val vm = crear(retenida)
        vm.uiState.test {
            awaitItem()
            vm.onSembrar(hoy)
            var s = awaitItem()
            while (!s.ocupado) s = awaitItem()
            vm.onSembrar(hoy) // segundo toque mientras esta ocupado: se descarta
            puerta.complete(Unit)
            while (s.ocupado || s.resultado == null || s.contador != 13) s = awaitItem()
            assertEquals("13 tickets sembrados", s.resultado)
            assertEquals(13, retenida.guardados)
            assertEquals(13, s.contador)
        }
    }

    @Test
    fun unFalloDaMensajeYLiberaElBloqueo() = runTest {
        fuente.saveError = IllegalStateException("fallo de BD")
        val vm = crear()
        vm.uiState.test {
            awaitItem()
            vm.onSembrar(hoy)
            var s = awaitItem()
            while (s.resultado == null || s.ocupado) s = awaitItem()
            assertEquals("No se pudieron sembrar los datos de demo", s.resultado)
            assertTrue(s.contador == 0)
        }
    }
}

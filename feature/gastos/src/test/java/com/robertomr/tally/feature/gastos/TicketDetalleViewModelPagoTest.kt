package com.robertomr.tally.feature.gastos

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.robertomr.tally.domain.expenses.data.TicketRepository
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.FormaPago
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.Ticket
import com.robertomr.tally.domain.expenses.usecases.ActualizarTicket
import com.robertomr.tally.domain.expenses.usecases.BorrarTicket
import com.robertomr.tally.domain.expenses.usecases.ObtenerTicket
import com.robertomr.tally.test.unit.CoroutineTestRule
import com.robertomr.tally.test.unit.FakePhotoFileStore
import com.robertomr.tally.test.unit.FakeTicketDataSource
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Tests JVM de la forma de pago en [TicketDetalleViewModel] (spec forma-pago):
 * elegir forma, entregado con cambio calculado, conservar ocultos al pasar a
 * tarjeta y limpiarlos al guardar; el total no cambia nunca.
 */
class TicketDetalleViewModelPagoTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private val repository = TicketRepository(FakeTicketDataSource(), FakePhotoFileStore())

    private suspend fun sembrar(
        forma: FormaPago = FormaPago.NO_INDICADA,
        entregado: Double? = null,
        cambio: Double? = null,
    ): Long = repository.save(
        Ticket(
            comercio = "SUPER",
            fechaEmision = LocalDateTime.of(2026, 10, 15, 13, 45),
            total = 13.90,
            categoria = Categoria.ALIMENTACION,
            fechaCaptura = LocalDateTime.of(2026, 10, 15, 14, 0),
            motor = Motor.NUBE,
            rutaFoto = "fotos/x.jpg",
            formaPago = forma,
            entregado = entregado,
            cambio = cambio,
        ),
    )

    private fun viewModel(id: Long) = TicketDetalleViewModel(
        savedStateHandle = SavedStateHandle(mapOf(TicketDetalleViewModel.ARG_ID to id)),
        obtenerTicket = ObtenerTicket(repository),
        actualizarTicket = ActualizarTicket(repository),
        borrarTicket = BorrarTicket(repository),
    )

    private suspend fun ReceiveTurbine<TicketDetalleUiState>.esperarDatos(
        condicion: (TicketDetalleUiState.Datos) -> Boolean = { true },
    ): TicketDetalleUiState.Datos {
        while (true) {
            val s = awaitItem()
            if (s is TicketDetalleUiState.Datos && condicion(s)) return s
        }
    }

    @Test
    fun ticketAntiguoSeAbreComoNoIndicadaSinCambios() = runTest {
        val vm = viewModel(sembrar())
        vm.uiState.test {
            val s = esperarDatos()
            assertEquals(FormaPago.NO_INDICADA, s.datos.formaPago)
            assertNull(s.datos.entregado)
            assertFalse(s.hayCambios)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun efectivoConEntregadoCalculaCambioYGuardaSinTocarElTotal() = runTest {
        val id = sembrar()
        val vm = viewModel(id)
        vm.uiState.test {
            esperarDatos()
            vm.onFormaPagoElegida(FormaPago.EFECTIVO)
            vm.onEntregadoEditado("50,00")
            val s = esperarDatos { it.datos.cambio != null }
            assertEquals(36.10, s.datos.cambio!!, 0.0)
            assertTrue(s.hayCambios)

            vm.onGuardar()
            esperarDatos { it.hecho }
            val guardado = repository.getById(id)!!
            assertEquals(FormaPago.EFECTIVO, guardado.formaPago)
            assertEquals(50.0, guardado.entregado!!, 0.0)
            assertEquals(36.10, guardado.cambio!!, 0.0)
            assertEquals(13.90, guardado.total, 0.0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun pasarATarjetaConservaOcultosYGuardarLosLimpia() = runTest {
        val id = sembrar(FormaPago.EFECTIVO, 50.0, 36.10)
        val vm = viewModel(id)
        vm.uiState.test {
            esperarDatos()
            vm.onFormaPagoElegida(FormaPago.TARJETA)
            val tarjeta = esperarDatos { it.datos.formaPago == FormaPago.TARJETA }
            assertEquals(50.0, tarjeta.datos.entregado!!, 0.0)

            vm.onFormaPagoElegida(FormaPago.EFECTIVO)
            val vuelta = esperarDatos { it.datos.formaPago == FormaPago.EFECTIVO }
            assertEquals(36.10, vuelta.datos.cambio!!, 0.0)
            assertFalse("volver al valor original no es un cambio", vuelta.hayCambios)

            vm.onFormaPagoElegida(FormaPago.TARJETA)
            esperarDatos { it.datos.formaPago == FormaPago.TARJETA }
            vm.onGuardar()
            esperarDatos { it.hecho }
            val guardado = repository.getById(id)!!
            assertEquals(FormaPago.TARJETA, guardado.formaPago)
            assertNull(guardado.entregado)
            assertNull(guardado.cambio)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun textoInvalidoDeEntregadoSeIgnora() = runTest {
        val vm = viewModel(sembrar(FormaPago.EFECTIVO, 50.0, 36.10))
        vm.uiState.test {
            esperarDatos()
            vm.onEntregadoEditado("abc")
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- cambio derivado y total automatico (spec gasto-manual-2) ---------------

    @Test
    fun cambiarElTotalRecalculaElCambioYGuardaElCalculado() = runTest {
        val id = sembrar(FormaPago.EFECTIVO, 50.0, 36.10)
        val vm = viewModel(id)
        vm.uiState.test {
            esperarDatos()
            vm.onTotalEditado("20")
            val s = esperarDatos { it.datos.total == 20.0 }
            assertEquals(30.0, s.datos.cambio!!, 0.0)
            vm.onGuardar()
            esperarDatos { it.hecho }
            assertEquals(30.0, repository.getById(id)!!.cambio!!, 0.0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun ticketAntiguoConCambioQueNoCuadraSeAbreConElCalculadoYAlGuardarSePersisteEse() = runTest {
        val id = sembrar(FormaPago.EFECTIVO, 50.0, 30.0)
        val vm = viewModel(id)
        vm.uiState.test {
            val s = esperarDatos()
            assertEquals(36.10, s.datos.cambio!!, 0.0)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

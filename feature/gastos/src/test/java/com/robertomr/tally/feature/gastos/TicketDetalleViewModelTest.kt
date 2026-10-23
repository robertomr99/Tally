package com.robertomr.tally.feature.gastos

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.robertomr.tally.domain.expenses.data.TicketRepository
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.Linea
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.Ticket
import com.robertomr.tally.domain.expenses.usecases.ActualizarTicket
import com.robertomr.tally.domain.expenses.usecases.BorrarTicket
import com.robertomr.tally.domain.expenses.usecases.ObtenerTicket
import com.robertomr.tally.test.unit.CoroutineTestRule
import com.robertomr.tally.test.unit.FakePhotoFileStore
import com.robertomr.tally.test.unit.FakeTicketDataSource
import java.io.IOException
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Tests JVM de [TicketDetalleViewModel] (spec detalle-ticket) con los
 * dobles de `:test:unit`: carga, inexistente, edicion, descartar, guardar,
 * borrar y sus fallos, y el doble toque.
 */
class TicketDetalleViewModelTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private val dataSource = FakeTicketDataSource()
    private val photoFileStore = FakePhotoFileStore()
    private val repository = TicketRepository(dataSource, photoFileStore)

    private val emision = LocalDateTime.of(2026, 10, 15, 13, 45)
    private val captura = LocalDateTime.of(2026, 10, 15, 14, 0)

    private suspend fun sembrar(): Long = repository.save(
        Ticket(
            comercio = "MERCADONA",
            fechaEmision = emision,
            total = 10.0,
            categoria = Categoria.ALIMENTACION,
            fechaCaptura = captura,
            motor = Motor.NUBE,
            rutaFoto = "fotos/x.jpg",
            nota = "n",
            textoCrudoOcr = "crudo",
            lineas = listOf(Linea("LECHE", 2, 1.0), Linea("PAN", 1, 3.0)),
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
    fun cargandoLuegoDatosDelTicket() = runTest {
        val id = sembrar()
        val vm = viewModel(id)
        assertEquals(TicketDetalleUiState.Cargando, vm.uiState.value)
        vm.uiState.test {
            val s = esperarDatos()
            assertEquals("MERCADONA", s.datos.comercio)
            assertEquals(2, s.datos.lineas.size)
            assertEquals(Motor.NUBE, s.ticket.motor)
            assertFalse(s.hayCambios)
            assertNull(s.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun ticketInexistenteMuestraNoExiste() = runTest {
        val vm = viewModel(404L)
        vm.uiState.test {
            var s = awaitItem()
            while (s == TicketDetalleUiState.Cargando) s = awaitItem()
            assertEquals(TicketDetalleUiState.NoExiste, s)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun fallarLaLecturaSeTrataComoNoExiste() = runTest {
        val id = sembrar()
        dataSource.getByIdError = IOException("bd")
        viewModel(id).uiState.test {
            var s = awaitItem()
            while (s == TicketDetalleUiState.Cargando) s = awaitItem()
            assertEquals(TicketDetalleUiState.NoExiste, s)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun editarMarcaHayCambiosYRevertirLoDesmarca() = runTest {
        val id = sembrar()
        val vm = viewModel(id)
        vm.uiState.test {
            esperarDatos()
            vm.onComercioEditado("CARREFOUR")
            val editado = esperarDatos { it.hayCambios }
            assertEquals("CARREFOUR", editado.datos.comercio)

            vm.onComercioEditado("MERCADONA")
            assertFalse(esperarDatos { !it.hayCambios }.hayCambios)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun fechaConservaLaHoraYTextoInvalidoSeIgnora() = runTest {
        val id = sembrar()
        val vm = viewModel(id)
        vm.uiState.test {
            esperarDatos()
            vm.onFechaEditada("3/11/2026")
            val s = esperarDatos { it.hayCambios }
            assertEquals(LocalDateTime.of(2026, 11, 3, 13, 45), s.datos.fechaEmision)

            vm.onFechaEditada("basura")
            vm.onTotalEditado("xx")
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun descartarRestableceLoGuardadoSinEscribir() = runTest {
        val id = sembrar()
        val vm = viewModel(id)
        vm.uiState.test {
            esperarDatos()
            vm.onTotalEditado("99,5")
            esperarDatos { it.hayCambios }

            vm.onDescartar()
            val s = esperarDatos { !it.hayCambios }
            assertEquals(10.0, s.datos.total, 0.0)
            assertEquals(10.0, repository.getById(id)!!.total, 0.0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun guardarPersisteElMismoTicketYMarcaHecho() = runTest {
        val id = sembrar()
        val vm = viewModel(id)
        vm.uiState.test {
            esperarDatos()
            vm.onComercioEditado("CARREFOUR")
            vm.onTotalEditado("12,5")
            vm.onFechaEditada("3/11/2026")
            vm.onLineaConceptoEditado(0, "LECHE ENTERA")
            vm.onLineaEliminada(1)
            vm.onLineaAnadida()
            vm.onLineaConceptoEditado(2, "HUEVOS")
            esperarDatos { it.datos.lineas.size == 2 && it.datos.lineas.last().concepto == "HUEVOS" }

            vm.onGuardar()
            val hecho = esperarDatos { it.hecho }
            assertTrue(hecho.hecho)

            val guardado = repository.getById(id)!!
            assertEquals(id, guardado.id)
            assertEquals("CARREFOUR", guardado.comercio)
            assertEquals(12.5, guardado.total, 0.0)
            assertEquals(LocalDateTime.of(2026, 11, 3, 13, 45), guardado.fechaEmision)
            assertEquals(captura, guardado.fechaCaptura)
            assertEquals(Motor.NUBE, guardado.motor)
            assertEquals("fotos/x.jpg", guardado.rutaFoto)
            assertEquals("crudo", guardado.textoCrudoOcr)
            assertEquals("n", guardado.nota)
            assertEquals(
                listOf(Linea("LECHE ENTERA", 2, 1.0), Linea("HUEVOS", 1, 0.0)),
                guardado.lineas,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun guardarSinCambiosNoHaceNada() = runTest {
        val id = sembrar()
        val vm = viewModel(id)
        vm.uiState.test {
            esperarDatos()
            vm.onGuardar()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun guardarFallidoMuestraErrorConservaDatosYPermiteReintentar() = runTest {
        val id = sembrar()
        val vm = viewModel(id)
        vm.uiState.test {
            esperarDatos()
            vm.onComercioEditado("CARREFOUR")
            esperarDatos { it.hayCambios }

            dataSource.saveError = IOException("bd")
            vm.onGuardar()
            val fallo = esperarDatos { it.error != null }
            assertEquals(TicketDetalleViewModel.MENSAJE_ERROR_GUARDADO, fallo.error)
            assertEquals("CARREFOUR", fallo.datos.comercio)
            assertFalse(fallo.hecho)
            assertEquals("MERCADONA", repository.getById(id)!!.comercio)

            dataSource.saveError = null
            vm.onGuardar()
            esperarDatos { it.hecho }
            assertEquals("CARREFOUR", repository.getById(id)!!.comercio)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun borrarOkMarcaHechoSinPasarPorNoExisteYBorraLaFoto() = runTest {
        val id = sembrar()
        val vm = viewModel(id)
        vm.uiState.test {
            esperarDatos()
            vm.onConfirmarBorrado()
            val vistos = mutableListOf<TicketDetalleUiState>()
            var s = awaitItem()
            vistos += s
            while (!(s is TicketDetalleUiState.Datos && s.hecho)) {
                s = awaitItem()
                vistos += s
            }
            assertFalse(vistos.any { it == TicketDetalleUiState.NoExiste })
            assertNull(repository.getById(id))
            assertEquals(listOf("fotos/x.jpg"), photoFileStore.deletedPaths)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun borrarConFalloDeBdMuestraErrorYElTicketSigueIntacto() = runTest {
        val id = sembrar()
        val vm = viewModel(id)
        vm.uiState.test {
            esperarDatos()
            dataSource.deleteError = IOException("bd")
            vm.onConfirmarBorrado()
            val fallo = esperarDatos { it.error != null }
            assertEquals(TicketDetalleViewModel.MENSAJE_ERROR_BORRADO, fallo.error)
            assertFalse(fallo.hecho)
            assertNotNull(repository.getById(id))
            assertTrue(photoFileStore.deletedPaths.isEmpty())

            // Reintentar tras arreglar el fallo.
            dataSource.deleteError = null
            vm.onConfirmarBorrado()
            esperarDatos { it.hecho }
            assertNull(repository.getById(id))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun borrarConFotoQueLanzaEsExitoParaElUsuario() = runTest {
        val id = sembrar()
        photoFileStore.deleteError = IOException("disco")
        val vm = viewModel(id)
        vm.uiState.test {
            esperarDatos()
            vm.onConfirmarBorrado()
            val s = esperarDatos { it.hecho || it.error != null }
            assertTrue(s.hecho)
            assertNull(s.error)
            assertNull(repository.getById(id))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun dobleBorradoSoloBorraUnaVez() = runTest {
        val id = sembrar()
        val vm = viewModel(id)
        vm.uiState.test {
            esperarDatos()
            vm.onConfirmarBorrado()
            vm.onConfirmarBorrado()
            esperarDatos { it.hecho }
            assertEquals(1, photoFileStore.deletedPaths.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun lasEdicionesTrasHechoSeIgnoran() = runTest {
        val id = sembrar()
        val vm = viewModel(id)
        vm.uiState.test {
            esperarDatos()
            vm.onConfirmarBorrado()
            esperarDatos { it.hecho }
            vm.onComercioEditado("OTRO")
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- total unico (spec gasto-manual-2) -------------------------------------

    @Test
    fun ticketConLineasQueNoSumanSuTotalSeAbreTecleadoYConservaElTotal() = runTest {
        val vm = viewModel(sembrar())
        vm.uiState.test {
            val s = esperarDatos()
            assertFalse(s.datos.totalAutomatico)
            assertEquals(10.0, s.datos.total, 0.0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun usarLaSumaYEditarAManoNoHabilitanGuardarPeroCambiarElTotalSi() = runTest {
        val vm = viewModel(sembrar())
        vm.uiState.test {
            esperarDatos()
            vm.onTotalAutomaticoElegido(true)
            val auto = esperarDatos { it.datos.totalAutomatico }
            assertEquals(5.0, auto.datos.total, 0.0)
            assertTrue(auto.hayCambios)

            vm.onTotalAutomaticoElegido(false)
            val manual = esperarDatos { !it.datos.totalAutomatico }
            assertEquals(5.0, manual.datos.total, 0.0)
            assertTrue(manual.hayCambios)

            vm.onTotalEditado("10")
            assertFalse(esperarDatos { !it.hayCambios }.hayCambios)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun alternarElModoSinCambiarValoresNoCuentaComoCambio() = runTest {
        // Total 5 = suma de las lineas: se abre en automatico.
        val id = repository.save(
            Ticket(
                comercio = "MERCADONA",
                fechaEmision = emision,
                total = 5.0,
                categoria = Categoria.ALIMENTACION,
                fechaCaptura = captura,
                motor = Motor.NUBE,
                rutaFoto = "fotos/x.jpg",
                lineas = listOf(Linea("LECHE", 2, 1.0), Linea("PAN", 1, 3.0)),
            ),
        )
        val vm = viewModel(id)
        vm.uiState.test {
            val abierto = esperarDatos()
            assertTrue(abierto.datos.totalAutomatico)
            vm.onTotalAutomaticoElegido(false)
            val manual = esperarDatos { !it.datos.totalAutomatico }
            assertEquals(5.0, manual.datos.total, 0.0)
            assertFalse("mismos valores que lo guardado: sin cambios", manual.hayCambios)
            vm.onTotalAutomaticoElegido(true)
            assertFalse(esperarDatos { it.datos.totalAutomatico }.hayCambios)
            cancelAndIgnoreRemainingEvents()
        }
    }
    @Test
    fun enAutomaticoEditarUnaCantidadActualizaElTotalYGuardaLoMostrado() = runTest {
        val id = sembrar()
        val vm = viewModel(id)
        vm.uiState.test {
            esperarDatos()
            vm.onTotalAutomaticoElegido(true)
            esperarDatos { it.datos.totalAutomatico }
            vm.onLineaCantidadEditada(0, "4")
            val s = esperarDatos { it.datos.total == 7.0 }
            vm.onGuardar()
            esperarDatos { it.hecho }
            assertEquals(7.0, repository.getById(id)!!.total, 0.0)
            assertEquals(7.0, s.datos.total, 0.0)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
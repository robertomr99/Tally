package com.robertomr.tally.feature.gastos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.FormaPago
import com.robertomr.tally.domain.expenses.entities.DatosRevision
import com.robertomr.tally.domain.expenses.entities.Ticket
import com.robertomr.tally.domain.expenses.entities.conFechaTexto
import com.robertomr.tally.domain.expenses.entities.conLineaAnadida
import com.robertomr.tally.domain.expenses.entities.conLineaCantidad
import com.robertomr.tally.domain.expenses.entities.conLineaConcepto
import com.robertomr.tally.domain.expenses.entities.conLineaPrecio
import com.robertomr.tally.domain.expenses.entities.conCambioTexto
import com.robertomr.tally.domain.expenses.entities.conEntregadoTexto
import com.robertomr.tally.domain.expenses.entities.conFormaPago
import com.robertomr.tally.domain.expenses.entities.conTotalTexto
import com.robertomr.tally.domain.expenses.entities.datosRevisionDe
import com.robertomr.tally.domain.expenses.entities.sinLinea
import com.robertomr.tally.domain.expenses.usecases.ActualizarTicket
import com.robertomr.tally.domain.expenses.usecases.BorrarTicket
import com.robertomr.tally.domain.expenses.usecases.ObtenerTicket
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/**
 * Estado del detalle de un ticket guardado (spec detalle-ticket). Mientras no
 * responde la lectura es [Cargando] (nunca un formulario vacio editable); si el
 * ticket ya no existe, [NoExiste].
 */
sealed interface TicketDetalleUiState {

    data object Cargando : TicketDetalleUiState

    data object NoExiste : TicketDetalleUiState

    /**
     * @property ticket el ticket tal cual se leyo (fuente de `motor`, `rutaFoto`...).
     * @property datos lo que se muestra/edita (cabecera + lineas editables).
     * @property hayCambios `datos` difiere de lo guardado.
     * @property error mensaje visible de un guardado/borrado fallido, o `null`.
     * @property hecho la operacion terminada bien: la pantalla sale por `onBack` una vez.
     */
    data class Datos(
        val ticket: Ticket,
        val datos: DatosRevision,
        val hayCambios: Boolean,
        val guardando: Boolean = false,
        val borrando: Boolean = false,
        val error: String? = null,
        val hecho: Boolean = false,
    ) : TicketDetalleUiState
}

/**
 * ViewModel del detalle (spec detalle-ticket). El id llega por el argumento de
 * la ruta (`SavedStateHandle`, clave [ARG_ID]). El estado es una cadena de
 * operadores sobre entradas `MutableStateFlow` (ticket leido una vez -> D6,
 * datos editados y orden de Guardar/Borrar); sin `load()`.
 *
 * Las operaciones de escritura corren con `runCatchingLanzandoCancelacion`: un
 * fallo se muestra como error y se puede reintentar; la `CancellationException`
 * siempre se relanza. Un borrado/guardado correcto marca `hecho` (la pantalla
 * vuelve con `onBack`) y NUNCA pasa por [TicketDetalleUiState.NoExiste].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TicketDetalleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val obtenerTicket: ObtenerTicket,
    private val actualizarTicket: ActualizarTicket,
    private val borrarTicket: BorrarTicket,
) : ViewModel() {

    private val ticketId: Long = savedStateHandle[ARG_ID] ?: 0L

    /** Lectura unica del ticket (D6): no se re-lee, el formulario no se reescribe. */
    private val lectura: StateFlow<Lectura> = flow {
        // Un fallo de lectura se trata como "no existe" (sin formulario ni crash).
        val ticket = runCatchingLanzandoCancelacion { obtenerTicket(ticketId) }.getOrNull()
        emit(if (ticket == null) Lectura.NoExiste else Lectura.Existe(ticket))
    }.stateIn(viewModelScope, SharingStarted.Lazily, Lectura.Cargando)

    /** Datos editados por el usuario; `null` = sin tocar (se muestran los guardados). */
    private val edicion = MutableStateFlow<DatosRevision?>(null)

    /** Orden de escritura con un numero de intento: reintentar tras un fallo la relanza. */
    private val orden = MutableStateFlow<Orden>(Orden.Ninguna)

    /** Guarda contra doble toque: mientras hay una operacion en curso (o terminada) se ignora otra. */
    private val ocupado = AtomicBoolean(false)
    private var intento = 0
    private var proximoIdLinea = -1L

    private val ejecucion: Flow<Ejecucion> = orden.flatMapLatest { actual ->
        when (actual) {
            Orden.Ninguna -> flowOf<Ejecucion>(Ejecucion.Reposo)
            is Orden.Guardar -> flow<Ejecucion> {
                emit(Ejecucion.Guardando)
                emit(guardar())
            }
            is Orden.Borrar -> flow<Ejecucion> {
                emit(Ejecucion.Borrando)
                emit(borrar())
            }
        }
    }

    val uiState: StateFlow<TicketDetalleUiState> = lectura
        .flatMapLatest { carga ->
            when (carga) {
                Lectura.Cargando -> flowOf(TicketDetalleUiState.Cargando)
                Lectura.NoExiste -> flowOf(TicketDetalleUiState.NoExiste)
                is Lectura.Existe -> {
                    val inicial = datosRevisionDe(carga.ticket)
                    combine(edicion, ejecucion) { editados, ejec ->
                        val datos = editados ?: inicial
                        TicketDetalleUiState.Datos(
                            ticket = carga.ticket,
                            datos = datos,
                            hayCambios = datos != inicial,
                            guardando = ejec == Ejecucion.Guardando,
                            borrando = ejec == Ejecucion.Borrando,
                            error = (ejec as? Ejecucion.Fallo)?.mensaje,
                            hecho = ejec == Ejecucion.Hecho,
                        )
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TicketDetalleUiState.Cargando)

    fun onComercioEditado(comercio: String) {
        editar { it.copy(comercio = comercio) }
    }

    /** Fecha `d/M/yyyy`; la hora de emision se conserva (D7). Texto invalido: se ignora. */
    fun onFechaEditada(texto: String) {
        editar { it.conFechaTexto(texto, conservarHora = true) }
    }

    fun onTotalEditado(texto: String) {
        editar { it.conTotalTexto(texto) }
    }

    /** Forma de pago (spec forma-pago): conserva entregado/cambio ocultos hasta guardar. */
    fun onFormaPagoElegida(forma: FormaPago) {
        editar { it.conFormaPago(forma) }
    }

    fun onEntregadoEditado(texto: String) {
        editar { it.conEntregadoTexto(texto) }
    }

    fun onCambioEditado(texto: String) {
        editar { it.conCambioTexto(texto) }
    }

    fun onCategoriaElegida(categoria: Categoria) {
        editar { it.copy(categoria = categoria) }
    }

    fun onLineaAnadida() {
        val id = siguienteIdLinea() ?: return
        editar { it.conLineaAnadida(id) }
    }

    fun onLineaConceptoEditado(id: Long, concepto: String) {
        editar { it.conLineaConcepto(id, concepto) }
    }

    fun onLineaCantidadEditada(id: Long, texto: String) {
        editar { it.conLineaCantidad(id, texto) }
    }

    fun onLineaPrecioEditado(id: Long, texto: String) {
        editar { it.conLineaPrecio(id, texto) }
    }

    fun onLineaEliminada(id: Long) {
        editar { it.sinLinea(id) }
    }

    /** "Descartar": restablece lo guardado sin escribir en Room. */
    fun onDescartar() {
        if (ocupado.get()) return
        edicion.value = null
        orden.value = Orden.Ninguna
    }

    /** "Guardar cambios": sin cambios no hace nada; una segunda pulsacion en curso se ignora. */
    fun onGuardar() {
        val estado = uiState.value as? TicketDetalleUiState.Datos ?: return
        if (!estado.hayCambios) return
        if (!ocupado.compareAndSet(false, true)) return
        orden.value = Orden.Guardar(++intento)
    }

    /** Confirmacion del dialogo "Eliminar ticket"; un segundo toque en curso se ignora. */
    fun onConfirmarBorrado() {
        if (lectura.value !is Lectura.Existe) return
        if (!ocupado.compareAndSet(false, true)) return
        orden.value = Orden.Borrar(++intento)
    }

    private fun editar(reductor: (DatosRevision) -> DatosRevision?) {
        if (ocupado.get()) return
        val existe = lectura.value as? Lectura.Existe ?: return
        val base = edicion.value ?: datosRevisionDe(existe.ticket)
        val nuevo = reductor(base) ?: return
        edicion.value = nuevo
        // Una edicion deja el detalle limpio (borra un posible error previo).
        orden.value = Orden.Ninguna
    }

    /** Ids de linea nuevos: el contador solo crece, asi que nunca reutiliza un id borrado. */
    private fun siguienteIdLinea(): Long? {
        val existe = lectura.value as? Lectura.Existe ?: return null
        if (proximoIdLinea < 0) proximoIdLinea = existe.ticket.lineas.size.toLong()
        return proximoIdLinea++
    }

    private suspend fun guardar(): Ejecucion {
        val existe = lectura.value as? Lectura.Existe ?: return liberar(Ejecucion.Fallo(MENSAJE_ERROR_GUARDADO))
        val datos = edicion.value ?: datosRevisionDe(existe.ticket)
        return try {
            actualizarTicket(existe.ticket, datos)
            Ejecucion.Hecho
        } catch (e: CancellationException) {
            ocupado.set(false)
            throw e
        } catch (e: Throwable) {
            liberar(Ejecucion.Fallo(MENSAJE_ERROR_GUARDADO))
        }
    }

    private suspend fun borrar(): Ejecucion = try {
        borrarTicket(ticketId)
        Ejecucion.Hecho
    } catch (e: CancellationException) {
        ocupado.set(false)
        throw e
    } catch (e: Throwable) {
        liberar(Ejecucion.Fallo(MENSAJE_ERROR_BORRADO))
    }

    /** Tras un fallo se vuelve a permitir reintentar. */
    private fun liberar(resultado: Ejecucion): Ejecucion {
        ocupado.set(false)
        return resultado
    }

    private sealed interface Lectura {
        data object Cargando : Lectura
        data object NoExiste : Lectura
        data class Existe(val ticket: Ticket) : Lectura
    }

    private sealed interface Orden {
        data object Ninguna : Orden
        data class Guardar(val intento: Int) : Orden
        data class Borrar(val intento: Int) : Orden
    }

    private sealed interface Ejecucion {
        data object Reposo : Ejecucion
        data object Guardando : Ejecucion
        data object Borrando : Ejecucion
        data object Hecho : Ejecucion
        data class Fallo(val mensaje: String) : Ejecucion
    }

    companion object {
        /** Clave del argumento de la ruta `ticket/{ticketId}` (la define `:app`). */
        const val ARG_ID = "ticketId"

        const val MENSAJE_ERROR_GUARDADO = "No se pudieron guardar los cambios"
        const val MENSAJE_ERROR_BORRADO = "No se pudo eliminar el ticket"
    }
}

/** Como `runCatching`, pero re-lanza SIEMPRE la [CancellationException]. */
private inline fun <T> runCatchingLanzandoCancelacion(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }

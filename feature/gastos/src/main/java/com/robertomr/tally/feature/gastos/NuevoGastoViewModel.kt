package com.robertomr.tally.feature.gastos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.DatosRevision
import com.robertomr.tally.domain.expenses.entities.ErrorGastoManual
import com.robertomr.tally.domain.expenses.entities.FormaPago
import com.robertomr.tally.domain.expenses.entities.conCambioTexto
import com.robertomr.tally.domain.expenses.entities.conEntregadoTexto
import com.robertomr.tally.domain.expenses.entities.conFechaTexto
import com.robertomr.tally.domain.expenses.entities.conFormaPago
import com.robertomr.tally.domain.expenses.entities.conLineaAnadida
import com.robertomr.tally.domain.expenses.entities.conLineaCantidad
import com.robertomr.tally.domain.expenses.entities.conLineaConcepto
import com.robertomr.tally.domain.expenses.entities.conLineaPrecio
import com.robertomr.tally.domain.expenses.entities.conTotalTexto
import com.robertomr.tally.domain.expenses.entities.datosGastoManualInicial
import com.robertomr.tally.domain.expenses.entities.errorGastoManual
import com.robertomr.tally.domain.expenses.entities.sinLinea
import com.robertomr.tally.domain.expenses.usecases.GuardarGastoManual
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
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
 * Estado de la pantalla "Nuevo gasto" (spec gasto-manual).
 *
 * @property datos lo que se esta editando (mismo modelo que la revision).
 * @property error por que todavia no se puede guardar, o `null` si es valido.
 * @property hayCambios `datos` difiere del formulario inicial.
 * @property mensajeError fallo del ultimo guardado (se puede reintentar), o `null`.
 * @property hecho guardado terminado bien: la pantalla sale por `onGastoGuardado` una vez.
 */
data class NuevoGastoUiState(
    val datos: DatosRevision,
    val error: ErrorGastoManual?,
    val hayCambios: Boolean,
    val guardando: Boolean = false,
    val mensajeError: String? = null,
    val hecho: Boolean = false,
)

/**
 * ViewModel de "Nuevo gasto" (spec gasto-manual). Estado como cadena de
 * operadores sobre `MutableStateFlow` de entrada (datos editados y orden de
 * guardar); sin `load()`. Las ediciones usan los mismos reductores puros que el
 * detalle; la fecha se edita sin conservar hora (00:00 del dia elegido).
 *
 * Un fallo de guardado se muestra y se puede reintentar; la
 * `CancellationException` siempre se relanza. Un guardado correcto marca
 * `hecho` y no se acepta otra edicion ni otro guardado.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NuevoGastoViewModel @Inject constructor(
    private val guardarGastoManual: GuardarGastoManual,
) : ViewModel() {

    private val inicial: DatosRevision = datosGastoManualInicial()

    private val edicion = MutableStateFlow(inicial)

    /** Numero de intento de guardado (0 = ninguno); reintentar tras un fallo lo relanza. */
    private val orden = MutableStateFlow(0)

    /** Guarda contra doble toque: mientras hay un guardado en curso (o terminado) se ignora otro. */
    private val ocupado = AtomicBoolean(false)
    private var intento = 0
    private var proximoIdLinea = 0L

    private val ejecucion: Flow<Ejecucion> = orden.flatMapLatest { actual ->
        if (actual == 0) {
            flowOf<Ejecucion>(Ejecucion.Reposo)
        } else {
            flow<Ejecucion> {
                emit(Ejecucion.Guardando)
                emit(guardar())
            }
        }
    }

    val uiState: StateFlow<NuevoGastoUiState> = combine(edicion, ejecucion) { datos, ejec ->
        NuevoGastoUiState(
            datos = datos,
            error = datos.errorGastoManual(LocalDate.now()),
            hayCambios = datos != inicial,
            guardando = ejec == Ejecucion.Guardando,
            mensajeError = (ejec as? Ejecucion.Fallo)?.mensaje,
            hecho = ejec == Ejecucion.Hecho,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        NuevoGastoUiState(datos = inicial, error = inicial.errorGastoManual(), hayCambios = false),
    )

    fun onComercioEditado(comercio: String) {
        editar { it.copy(comercio = comercio) }
    }

    /** Fecha `d/M/yyyy`; la hora es 00:00 del dia elegido. Texto invalido: se ignora. */
    fun onFechaEditada(texto: String) {
        editar { it.conFechaTexto(texto, conservarHora = false) }
    }

    fun onTotalEditado(texto: String) {
        editar { it.conTotalTexto(texto) }
    }

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
        val id = proximoIdLinea
        editar { it.conLineaAnadida(id) }?.let { proximoIdLinea++ }
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

    /** "Descartar": restablece el formulario inicial sin escribir en Room. */
    fun onDescartar() {
        if (ocupado.get()) return
        edicion.value = inicial
        orden.value = 0
    }

    /** "Guardar": solo con datos validos; una segunda pulsacion en curso se ignora. */
    fun onGuardar() {
        val estado = uiState.value
        if (estado.error != null) return
        if (!ocupado.compareAndSet(false, true)) return
        orden.value = ++intento
    }

    /** Devuelve algo no nulo si la edicion se aplico (para avanzar el contador de lineas). */
    private fun editar(reductor: (DatosRevision) -> DatosRevision?): Unit? {
        if (ocupado.get()) return null
        val nuevo = reductor(edicion.value) ?: return null
        edicion.value = nuevo
        // Una edicion limpia un posible error de guardado previo.
        orden.value = 0
        return Unit
    }

    private suspend fun guardar(): Ejecucion {
        val datos = edicion.value
        return try {
            guardarGastoManual(datos)
            Ejecucion.Hecho
        } catch (e: CancellationException) {
            ocupado.set(false)
            throw e
        } catch (e: Throwable) {
            ocupado.set(false)
            Ejecucion.Fallo(MENSAJE_ERROR_GUARDADO)
        }
    }

    private sealed interface Ejecucion {
        data object Reposo : Ejecucion
        data object Guardando : Ejecucion
        data object Hecho : Ejecucion
        data class Fallo(val mensaje: String) : Ejecucion
    }

    companion object {
        const val MENSAJE_ERROR_GUARDADO = "No se pudo guardar el gasto"
    }
}

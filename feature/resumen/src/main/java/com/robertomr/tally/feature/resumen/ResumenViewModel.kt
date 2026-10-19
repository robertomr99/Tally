package com.robertomr.tally.feature.resumen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robertomr.tally.domain.expenses.entities.DistribucionCategorias
import com.robertomr.tally.domain.expenses.entities.Periodo
import com.robertomr.tally.domain.expenses.entities.PeriodoTipo
import com.robertomr.tally.domain.expenses.entities.ResumenPeriodo
import com.robertomr.tally.domain.expenses.entities.Ticket
import com.robertomr.tally.domain.expenses.usecases.ObservarListadoDelPeriodo
import com.robertomr.tally.domain.expenses.usecases.ObservarDistribucionDelPeriodo
import com.robertomr.tally.domain.expenses.usecases.ObservarResumenDelPeriodo
import com.robertomr.tally.domain.expenses.usecases.ObtenerPeriodo
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** Filas de la vista previa de tickets en Resumen (spec listado-gastos, D6). */
const val MAX_RECIENTES = 5

/**
 * Estado del panel (specs periodo-selector y resumen-total). [resumen] lleva las
 * métricas de la tarjeta de total y [distribucion] el reparto por categoría del
 * donut (spec grafica-donut); ambos son `null` mientras carga. [recientes] son
 * los últimos tickets del período (máx. [MAX_RECIENTES]) y [totalTickets] su cuenta
 * total (spec listado-gastos).
 */
data class ResumenUiState(
    val cargando: Boolean = true,
    val tipo: PeriodoTipo = PeriodoTipo.MES,
    val periodo: Periodo? = null,
    val resumen: ResumenPeriodo? = null,
    val distribucion: DistribucionCategorias? = null,
    val recientes: List<Ticket>? = null,
    val totalTickets: Int = 0,
    val hoy: LocalDate = LocalDate.now(),
    /** Último rango personalizado elegido (precarga el picker); `null` si nunca hubo. */
    val rango: Periodo? = null,
)

/**
 * ViewModel de Resumen. El estado es una cadena de operadores sobre el tipo de
 * período seleccionado (`flatMapLatest` hacia el Flow de Room); sin funciones
 * `load()`. La selección vive aquí (sobrevive a rotación) y arranca en Mes.
 *
 * Desviación conocida: AGENTS.md cita `stateAsResultIn`/`AcScaffold`, que no
 * existen en el repo; se sigue el patrón de Ajustes (`stateIn` directo).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ResumenViewModel @Inject constructor(
    private val obtenerPeriodo: ObtenerPeriodo,
    private val observarResumenDelPeriodo: ObservarResumenDelPeriodo,
    private val observarDistribucionDelPeriodo: ObservarDistribucionDelPeriodo,
    private val observarListadoDelPeriodo: ObservarListadoDelPeriodo,
) : ViewModel() {

    private val seleccion = MutableStateFlow(Seleccion())

    val uiState: StateFlow<ResumenUiState> = seleccion
        .flatMapLatest { (tipo, rango) ->
            val periodo = obtenerPeriodo(tipo, rango)
            val hoy = obtenerPeriodo(PeriodoTipo.DIA).desde
            combine(
                observarResumenDelPeriodo(periodo),
                observarDistribucionDelPeriodo(periodo),
                observarListadoDelPeriodo(periodo),
            ) { resumen, distribucion, grupos ->
                // Vista previa: los más recientes en el orden del dominio, sin agrupar.
                val tickets = grupos.flatMap { it.tickets }
                ResumenUiState(
                    cargando = false,
                    tipo = tipo,
                    periodo = periodo,
                    resumen = resumen,
                    distribucion = distribucion,
                    recientes = tickets.take(MAX_RECIENTES),
                    totalTickets = tickets.size,
                    hoy = hoy,
                    rango = rango,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ResumenUiState())

    /**
     * Día/Semana/Mes, o PERSONALIZADO reaplicando el último rango (sin rango
     * previo se ignora: el selector abre antes el picker).
     */
    fun onSeleccionarPeriodo(tipo: PeriodoTipo) {
        seleccion.update { actual ->
            if (tipo == PeriodoTipo.PERSONALIZADO && actual.rango == null) actual else actual.copy(tipo = tipo)
        }
    }

    /** Rango inclusivo elegido en el picker; un rango invertido (desde > hasta) se ignora. */
    fun onSeleccionarRango(desde: LocalDate, hasta: LocalDate) {
        if (desde.isAfter(hasta)) return
        seleccion.value = Seleccion(PeriodoTipo.PERSONALIZADO, Periodo.personalizado(desde, hasta))
    }
}

/** Entrada del estado: tipo activo y último rango personalizado elegido (se conserva al cambiar a Día/Semana/Mes). */
private data class Seleccion(val tipo: PeriodoTipo = PeriodoTipo.MES, val rango: Periodo? = null)

package com.robertomr.tally.feature.gastos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robertomr.tally.domain.expenses.entities.FiltroFormaPago
import com.robertomr.tally.domain.expenses.entities.GrupoDiaTickets
import com.robertomr.tally.domain.expenses.entities.Periodo
import com.robertomr.tally.domain.expenses.entities.PeriodoTipo
import com.robertomr.tally.domain.expenses.usecases.AgruparTicketsPorDia
import com.robertomr.tally.domain.expenses.usecases.FiltrarTicketsPorFormaPago
import com.robertomr.tally.domain.expenses.usecases.ObservarTicketsDelPeriodo
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Estado del listado (spec listado-gastos). [grupos] es `null` mientras carga y
 * vacío si el período no tiene tickets. [hoy] sirve para etiquetar los días
 * ("Hoy", "Ayer") sin inyectar el reloj en la pantalla.
 */
data class GastosUiState(
    val cargando: Boolean = true,
    val tipo: PeriodoTipo = PeriodoTipo.MES,
    val periodo: Periodo? = null,
    val grupos: List<GrupoDiaTickets>? = null,
    val hoy: LocalDate = LocalDate.now(),
    /** Último rango personalizado elegido (precarga el picker); `null` si nunca hubo. */
    val rango: Periodo? = null,
    /** Filtro de forma de pago activo (solo afecta al listado, no a Resumen). */
    val filtro: FiltroFormaPago = FiltroFormaPago.TODOS,
    /** El período tiene tickets antes de filtrar: distingue el vacío real del vacío por filtro. */
    val hayTicketsEnPeriodo: Boolean = false,
)

/**
 * ViewModel de Gastos. Cadena de operadores sobre el tipo de período
 * (`flatMapLatest` hacia el Flow de Room); sin `load()`. Arranca en Mes.
 *
 * Desviación conocida (igual que Resumen): se usa `stateIn` directo porque
 * `stateAsResultIn`/`AcScaffold` no existen en el repo.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GastosViewModel @Inject constructor(
    private val obtenerPeriodo: ObtenerPeriodo,
    private val observarTicketsDelPeriodo: ObservarTicketsDelPeriodo,
    private val filtrarPorFormaPago: FiltrarTicketsPorFormaPago,
    private val agruparPorDia: AgruparTicketsPorDia,
) : ViewModel() {

    private val seleccion = MutableStateFlow(Seleccion())

    /** Vive en el ViewModel (no en el estado visual): sobrevive a detalle, Nuevo gasto y rotación. */
    private val filtro = MutableStateFlow(FiltroFormaPago.TODOS)

    val uiState: StateFlow<GastosUiState> = combine(seleccion, filtro) { sel, f -> sel to f }
        .flatMapLatest { (sel, filtroActivo) ->
            val (tipo, rango) = sel
            val periodo = obtenerPeriodo(tipo, rango)
            // El período Día es el de hoy: su `desde` es la fecha del reloj inyectado.
            val hoy = obtenerPeriodo(PeriodoTipo.DIA).desde
            observarTicketsDelPeriodo(periodo).map { tickets ->
                GastosUiState(
                    cargando = false,
                    tipo = tipo,
                    periodo = periodo,
                    grupos = agruparPorDia(filtrarPorFormaPago(tickets, filtroActivo)),
                    hoy = hoy,
                    rango = rango,
                    filtro = filtroActivo,
                    hayTicketsEnPeriodo = tickets.isNotEmpty(),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GastosUiState())

    /**
     * Día/Semana/Mes, o PERSONALIZADO reaplicando el último rango (sin rango
     * previo se ignora: el selector abre antes el picker).
     */
    fun onSeleccionarPeriodo(tipo: PeriodoTipo) {
        seleccion.update { actual ->
            if (tipo == PeriodoTipo.PERSONALIZADO && actual.rango == null) actual else actual.copy(tipo = tipo)
        }
    }

    /** Todos / Efectivo / Tarjeta; no se reinicia al cambiar de período o rango. */
    fun onSeleccionarFiltro(nuevo: FiltroFormaPago) {
        filtro.value = nuevo
    }

    /** Rango inclusivo elegido en el picker; un rango invertido (desde > hasta) se ignora. */
    fun onSeleccionarRango(desde: LocalDate, hasta: LocalDate) {
        if (desde.isAfter(hasta)) return
        seleccion.value = Seleccion(PeriodoTipo.PERSONALIZADO, Periodo.personalizado(desde, hasta))
    }
}

/** Entrada del estado: tipo activo y último rango personalizado elegido (se conserva al cambiar a Día/Semana/Mes). */
private data class Seleccion(val tipo: PeriodoTipo = PeriodoTipo.MES, val rango: Periodo? = null)

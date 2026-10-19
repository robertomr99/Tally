package com.robertomr.tally.feature.gastos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robertomr.tally.domain.expenses.entities.GrupoDiaTickets
import com.robertomr.tally.domain.expenses.entities.Periodo
import com.robertomr.tally.domain.expenses.entities.PeriodoTipo
import com.robertomr.tally.domain.expenses.usecases.ObservarListadoDelPeriodo
import com.robertomr.tally.domain.expenses.usecases.ObtenerPeriodo
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

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
    private val observarListadoDelPeriodo: ObservarListadoDelPeriodo,
) : ViewModel() {

    private val tipoSeleccionado = MutableStateFlow(PeriodoTipo.MES)

    val uiState: StateFlow<GastosUiState> = tipoSeleccionado
        .flatMapLatest { tipo ->
            val periodo = obtenerPeriodo(tipo)
            // El período Día es el de hoy: su `desde` es la fecha del reloj inyectado.
            val hoy = obtenerPeriodo(PeriodoTipo.DIA).desde
            observarListadoDelPeriodo(periodo).map { grupos ->
                GastosUiState(cargando = false, tipo = tipo, periodo = periodo, grupos = grupos, hoy = hoy)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GastosUiState())

    fun onSeleccionarPeriodo(tipo: PeriodoTipo) {
        tipoSeleccionado.value = tipo
    }
}

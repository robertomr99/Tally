package com.robertomr.tally.feature.resumen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robertomr.tally.domain.expenses.entities.Periodo
import com.robertomr.tally.domain.expenses.entities.PeriodoTipo
import com.robertomr.tally.domain.expenses.entities.ResumenPeriodo
import com.robertomr.tally.domain.expenses.usecases.ObservarResumenDelPeriodo
import com.robertomr.tally.domain.expenses.usecases.ObtenerPeriodo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Estado del panel (specs periodo-selector y resumen-total). [resumen] lleva las
 * métricas de la tarjeta de total y es `null` mientras carga.
 */
data class ResumenUiState(
    val cargando: Boolean = true,
    val tipo: PeriodoTipo = PeriodoTipo.MES,
    val periodo: Periodo? = null,
    val resumen: ResumenPeriodo? = null,
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
) : ViewModel() {

    private val tipoSeleccionado = MutableStateFlow(PeriodoTipo.MES)

    val uiState: StateFlow<ResumenUiState> = tipoSeleccionado
        .flatMapLatest { tipo ->
            val periodo = obtenerPeriodo(tipo)
            observarResumenDelPeriodo(periodo).map { resumen ->
                ResumenUiState(
                    cargando = false,
                    tipo = tipo,
                    periodo = periodo,
                    resumen = resumen,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ResumenUiState())

    fun onSeleccionarPeriodo(tipo: PeriodoTipo) {
        tipoSeleccionado.value = tipo
    }
}

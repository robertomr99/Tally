package com.robertomr.tally.feature.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robertomr.tally.domain.expenses.entities.EstadoModelo
import com.robertomr.tally.domain.expenses.entities.PreferenciaMotor
import com.robertomr.tally.domain.expenses.entities.ResultadoDescarga
import com.robertomr.tally.domain.expenses.usecases.CancelarDescargaModelo
import com.robertomr.tally.domain.expenses.usecases.ConfigurarPreferenciaMotor
import com.robertomr.tally.domain.expenses.usecases.ConfigurarTokenModelo
import com.robertomr.tally.domain.expenses.usecases.DescargarModelo
import com.robertomr.tally.domain.expenses.usecases.EliminarModelo
import com.robertomr.tally.domain.expenses.usecases.ObservarEstadoModelo
import com.robertomr.tally.domain.expenses.usecases.ObservarPreferenciaMotor
import com.robertomr.tally.domain.expenses.usecases.ObservarTokenModelo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Avisos que bloquean el inicio de la descarga (spec descarga-modelo, escenarios 3 y 4). */
sealed interface AvisoDescarga {
    /** Red movil o medida: solo se descarga con WiFi. */
    data object SoloWifi : AvisoDescarga

    data class EspacioInsuficiente(val necesarios: Long, val libres: Long) : AvisoDescarga
}

/** Estado de la seccion "Modelo local" de Ajustes. */
data class AjustesUiState(
    val cargando: Boolean = true,
    val estado: EstadoModelo = EstadoModelo.NoDescargado,
    val hayToken: Boolean = false,
    val aviso: AvisoDescarga? = null,
    /** Motor forzado (spec ajustes-motor); AUTO por defecto. */
    val preferencia: PreferenciaMotor = PreferenciaMotor.AUTO,
    /** Local elegido pero sin modelo descargado: la extraccion fallara (S6). */
    val avisoLocal: Boolean = false,
)

/**
 * ViewModel de Ajustes (spec descarga-modelo). El estado es una cadena de
 * operadores: `combine` del estado del modelo, la existencia de token y el flujo
 * de avisos de entrada; sin funciones `load()` imperativas. No carga el modelo.
 */
@HiltViewModel
class AjustesViewModel @Inject constructor(
    observarEstadoModelo: ObservarEstadoModelo,
    observarTokenModelo: ObservarTokenModelo,
    observarPreferenciaMotor: ObservarPreferenciaMotor,
    private val configurarPreferenciaMotor: ConfigurarPreferenciaMotor,
    private val descargarModelo: DescargarModelo,
    private val cancelarDescargaModelo: CancelarDescargaModelo,
    private val eliminarModelo: EliminarModelo,
    private val configurarTokenModelo: ConfigurarTokenModelo,
) : ViewModel() {

    private val aviso = MutableStateFlow<AvisoDescarga?>(null)

    val uiState: StateFlow<AjustesUiState> = combine(
        observarEstadoModelo(),
        observarTokenModelo(),
        observarPreferenciaMotor(),
        aviso,
    ) { estado, hayToken, preferencia, avisoActual ->
        // El aviso solo tiene sentido mientras no hay descarga en marcha.
        val visible = avisoActual.takeIf {
            estado is EstadoModelo.NoDescargado || estado is EstadoModelo.Fallo
        }
        AjustesUiState(
            cargando = false,
            estado = estado,
            hayToken = hayToken,
            aviso = visible,
            preferencia = preferencia,
            avisoLocal = preferencia == PreferenciaMotor.LOCAL && estado !is EstadoModelo.Descargado,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AjustesUiState())

    fun onDescargar() {
        viewModelScope.launch {
            aviso.value = when (val r = descargarModelo()) {
                ResultadoDescarga.Iniciada -> null
                ResultadoDescarga.RequiereWifi -> AvisoDescarga.SoloWifi
                is ResultadoDescarga.EspacioInsuficiente ->
                    AvisoDescarga.EspacioInsuficiente(r.necesarios, r.libres)
            }
        }
    }

    fun onCancelar() {
        viewModelScope.launch {
            aviso.value = null
            cancelarDescargaModelo()
        }
    }

    fun onEliminar() {
        viewModelScope.launch {
            aviso.value = null
            eliminarModelo()
        }
    }

    fun onElegirMotor(preferencia: PreferenciaMotor) = configurarPreferenciaMotor(preferencia)

    fun onGuardarToken(token: String) = configurarTokenModelo(token)

    fun onBorrarToken() = configurarTokenModelo("")

    fun onAvisoVisto() {
        aviso.value = null
    }
}

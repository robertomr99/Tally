package com.robertomr.tally.feature.ajustes.demo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de la seccion "Datos de demo" (spec seed-demo, D7). */
internal data class DatosDemoUiState(
    val contador: Int = 0,
    val ocupado: Boolean = false,
    val resultado: String? = null,
)

/**
 * ViewModel de la seccion de datos de demo (SOLO debug). El contador es un Flow
 * derivado de la BD; `ocupado` y `resultado` son entradas del `combine`. Un
 * doble toque no duplica: mientras hay una operacion en curso se ignora otra.
 */
@HiltViewModel
internal class DatosDemoViewModel @Inject constructor(
    observarDatosDemo: ObservarDatosDemo,
    private val sembrarDatosDemo: SembrarDatosDemo,
    private val quitarDatosDemo: QuitarDatosDemo,
) : ViewModel() {

    private val ocupado = MutableStateFlow(false)
    private val resultado = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DatosDemoUiState> = combine(
        observarDatosDemo().map { it.size },
        ocupado,
        resultado,
    ) { contador, ocupado, resultado -> DatosDemoUiState(contador, ocupado, resultado) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DatosDemoUiState())

    fun onSembrar(hoy: LocalDate = LocalDate.now()) = ejecutar("No se pudieron sembrar los datos de demo") {
        textoSembrado(sembrarDatosDemo(hoy))
    }

    fun onQuitar() = ejecutar("No se pudieron quitar los datos de demo") {
        textoQuitado(quitarDatosDemo())
    }

    private fun ejecutar(textoError: String, accion: suspend () -> String) {
        // update atomico: el segundo toque ve ocupado=true y se descarta.
        var yaOcupado = false
        ocupado.update { actual -> yaOcupado = actual; true }
        if (yaOcupado) return
        resultado.value = null
        viewModelScope.launch {
            try {
                resultado.value = accion()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                resultado.value = textoError
            } finally {
                ocupado.value = false
            }
        }
    }
}

internal fun textoSembrado(r: ResultadoSembrado): String = when {
    r.sembrados == 0 -> "Ya estaban los ${r.yaEstaban} tickets de demo"
    r.yaEstaban == 0 -> "${r.sembrados} tickets sembrados"
    r.sembrados == 1 -> "1 sembrado, resto ya estaban"
    else -> "${r.sembrados} sembrados, resto ya estaban"
}

internal fun textoQuitado(n: Int): String = when (n) {
    0 -> "No había datos de demo"
    1 -> "1 ticket de demo quitado"
    else -> "$n tickets de demo quitados"
}

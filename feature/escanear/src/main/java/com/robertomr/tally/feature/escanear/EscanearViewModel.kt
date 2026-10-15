package com.robertomr.tally.feature.escanear

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robertomr.tally.domain.expenses.data.PhotoFileStore
import com.robertomr.tally.domain.expenses.usecases.GuardarFotoCapturada
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.launch

/**
 * Estado visible del subflujo de captura dentro de Escanear (spec captura-foto):
 * encuadre (mockup 04) -> procesado de la foto -> revision (mockup 05). La
 * pantalla 05 es un estado interno de esta feature; sin rutas nuevas en :app.
 */
sealed interface CapturaUiState {
    data object PermisoPendiente : CapturaUiState
    data object Encuadre : CapturaUiState
    data object Procesando : CapturaUiState
    data class Revision(val rutaFoto: String) : CapturaUiState
    data class Error(val mensaje: String) : CapturaUiState
}

/**
 * ViewModel de Escanear (AGENTS.md): recibe casos de uso por constructor y
 * expone un [StateFlow] derivado con operadores sobre flujos de entrada
 * (sin funciones `load()` imperativas que muten estado).
 *
 * La foto se procesa y se guarda EN EL MOMENTO DE LA CAPTURA con
 * [GuardarFotoCapturada]; la ruta relativa vive solo en memoria (el Borrador no
 * se persiste hasta revision-datos).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EscanearViewModel @Inject constructor(
    private val guardarFotoCapturada: GuardarFotoCapturada,
    private val photoFileStore: PhotoFileStore,
) : ViewModel() {

    private val permisoConcedido = MutableStateFlow(false)
    private val entradaCaptura = MutableStateFlow<EntradaCaptura>(EntradaCaptura.Ninguna)

    val estado: StateFlow<CapturaUiState> = combine(
        permisoConcedido,
        entradaCaptura.flatMapLatest { resultadoDeCaptura(it) },
    ) { permiso, captura ->
        when {
            !permiso -> CapturaUiState.PermisoPendiente
            captura is CapturaEnProgreso.Ninguna -> CapturaUiState.Encuadre
            captura is CapturaEnProgreso.Procesando -> CapturaUiState.Procesando
            captura is CapturaEnProgreso.Guardada -> CapturaUiState.Revision(captura.rutaFoto)
            else -> CapturaUiState.Error((captura as CapturaEnProgreso.Fallida).mensaje)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = CapturaUiState.PermisoPendiente,
    )

    /** El gate de permiso de la pantalla informa del resultado real del sistema. */
    fun onPermisoCambiado(concedido: Boolean) {
        permisoConcedido.value = concedido
    }

    /** Bytes JPEG crudos de CameraX (la pantalla solo hace de binding). */
    fun onImageCapturado(rawJpeg: ByteArray) {
        entradaCaptura.value = EntradaCaptura.Cruda(rawJpeg)
    }

    /** La camara no pudo capturar (sin frames, error de dispositivo...). */
    fun onCapturaFallida(mensaje: String) {
        entradaCaptura.value = EntradaCaptura.Fallida(mensaje)
    }

    /**
     * "Repetir foto" (y volver al encuadre desde Error): descarta la foto del
     * borrador reemplazado, borrando su fichero en disco (best-effort; escenario
     * 7 del spec). La foto de un ticket Guardado solo se borra en cascada.
     */
    fun onRepetirFoto() {
        val actual = estado.value
        if (actual is CapturaUiState.Revision) {
            val ruta = actual.rutaFoto
            viewModelScope.launch(Dispatchers.IO) {
                runCatching { photoFileStore.delete(ruta) }
            }
        }
        entradaCaptura.value = EntradaCaptura.Ninguna
    }

    private fun resultadoDeCaptura(entrada: EntradaCaptura): Flow<CapturaEnProgreso> =
        when (entrada) {
            EntradaCaptura.Ninguna -> flowOf(CapturaEnProgreso.Ninguna)
            is EntradaCaptura.Fallida -> flowOf(CapturaEnProgreso.Fallida(entrada.mensaje))
            is EntradaCaptura.Cruda -> flow {
                emit(CapturaEnProgreso.Procesando)
                emit(
                    runCatching { guardarFotoCapturada(entrada.bytes) }
                        .fold(
                            onSuccess = { CapturaEnProgreso.Guardada(it) },
                            onFailure = {
                                CapturaEnProgreso.Fallida(it.message ?: "No se pudo guardar la foto")
                            },
                        )
                )
            }
        }

    /** Entradas del flujo: bytes de captura o fallo de la camara. */
    private sealed interface EntradaCaptura {
        data object Ninguna : EntradaCaptura
        class Cruda(val bytes: ByteArray) : EntradaCaptura
        data class Fallida(val mensaje: String) : EntradaCaptura
    }

    /** Resultado derivado del procesado/guardado de la captura actual. */
    private sealed interface CapturaEnProgreso {
        data object Ninguna : CapturaEnProgreso
        data object Procesando : CapturaEnProgreso
        data class Guardada(val rutaFoto: String) : CapturaEnProgreso
        data class Fallida(val mensaje: String) : CapturaEnProgreso
    }
}

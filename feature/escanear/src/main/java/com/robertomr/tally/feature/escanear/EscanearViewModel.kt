package com.robertomr.tally.feature.escanear

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robertomr.tally.domain.expenses.data.PhotoFileStore
import com.robertomr.tally.domain.expenses.usecases.GuardarFotoCapturada
import com.robertomr.tally.domain.expenses.usecases.ReconocerTextoCrudo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
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
 *
 * [CapturaUiState.Revision] lleva el par del Borrador en memoria
 * (spec ocr-mlkit): `rutaFoto` + `textoCrudo` del OCR (etapa 1 del ADR 0002).
 */
sealed interface CapturaUiState {
    data object PermisoPendiente : CapturaUiState
    data object Encuadre : CapturaUiState
    data object Procesando : CapturaUiState

    /**
     * Pantalla 05 con la foto ya guardada y el texto crudo del OCR registrado
     * con el Borrador. [ocrNoDisponible] marca el fallo del motor (escenario 4:
     * la captura NO se convierte en error); el texto vacio sin fallo es "OCR
     * vacio" (escenario 3).
     */
    data class Revision(
        val rutaFoto: String,
        val textoCrudo: String,
        val ocrNoDisponible: Boolean = false,
    ) : CapturaUiState

    data class Error(val mensaje: String) : CapturaUiState
}

/**
 * ViewModel de Escanear (AGENTS.md): recibe casos de uso por constructor y
 * expone un [StateFlow] derivado con operadores sobre flujos de entrada
 * (sin funciones `load()` imperativas que muten estado).
 *
 * La foto se procesa y se guarda EN EL MOMENTO DE LA CAPTURA con
 * [GuardarFotoCapturada] y, a continuacion (spec ocr-mlkit), se reconoce su
 * texto crudo con [ReconocerTextoCrudo] DENTRO del estado `Procesando`. El par
 * (rutaFoto, textoCrudo) vive solo en memoria (el Borrador no se persiste hasta
 * revision-datos). Politica: el OCR vacio o el fallo del motor NUNCA bloquean
 * la captura (la foto ya esta en disco).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EscanearViewModel @Inject constructor(
    private val guardarFotoCapturada: GuardarFotoCapturada,
    private val reconocerTextoCrudo: ReconocerTextoCrudo,
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
            captura is CapturaEnProgreso.Guardada -> CapturaUiState.Revision(
                rutaFoto = captura.rutaFoto,
                textoCrudo = captura.textoCrudo,
                ocrNoDisponible = captura.ocrNoDisponible,
            )
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
     * "Repetir foto" (y volver al encuadre desde Error): descarta la foto y el
     * texto crudo del borrador reemplazados; la foto se borra en disco
     * (best-effort; escenario 7 del spec captura-foto) y el texto desaparece
     * (escenario 5 del spec ocr-mlkit). La foto de un ticket Guardado solo se
     * borra en cascada.
     */
    fun onRepetirFoto() {
        val actual = estado.value
        if (actual is CapturaUiState.Revision) {
            val ruta = actual.rutaFoto
            viewModelScope.launch(Dispatchers.IO) {
                runCatchingLanzandoCancelacion { photoFileStore.delete(ruta) }
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
                // La foto se guarda primero, SIEMPRE (evidencia inmutable).
                val ruta = runCatchingLanzandoCancelacion { guardarFotoCapturada(entrada.bytes) }
                    .getOrElse {
                        emit(CapturaEnProgreso.Fallida(it.message ?: "No se pudo guardar la foto"))
                        return@flow
                    }
                // El OCR nunca bloquea la captura: vacio o fallo se marcan y
                // se llega igualmente a la pantalla 05 (escenarios 3 y 4).
                val textoCrudo = runCatchingLanzandoCancelacion { reconocerTextoCrudo(ruta) }
                emit(
                    textoCrudo.fold(
                        onSuccess = { CapturaEnProgreso.Guardada(ruta, it, ocrNoDisponible = false) },
                        onFailure = { CapturaEnProgreso.Guardada(ruta, "", ocrNoDisponible = true) },
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

        /** Foto ya en disco + texto crudo del OCR registrado con el Borrador. */
        data class Guardada(
            val rutaFoto: String,
            val textoCrudo: String,
            val ocrNoDisponible: Boolean,
        ) : CapturaEnProgreso

        data class Fallida(val mensaje: String) : CapturaEnProgreso
    }
}

/**
 * Como `runCatching`, pero re-lanza SIEMPRE la [CancellationException]
 * (finding Low sesion 014 / R-OCR4): cancelar no es un fallo de negocio y no
 * debe convertirse en resultado.
 */
private inline fun <T> runCatchingLanzandoCancelacion(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }

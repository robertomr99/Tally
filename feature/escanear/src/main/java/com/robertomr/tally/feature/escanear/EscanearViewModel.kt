package com.robertomr.tally.feature.escanear

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robertomr.tally.domain.expenses.data.CropRegion
import com.robertomr.tally.domain.expenses.data.PhotoFileStore
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.TicketExtraido
import com.robertomr.tally.domain.expenses.usecases.ExtraerTicket
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Estado visible del subflujo de captura dentro de Escanear (spec captura-foto):
 * encuadre (mockup 04) -> procesado de la foto -> revision (mockup 05). La
 * pantalla 05 es un estado interno de esta feature; sin rutas nuevas en :app.
 *
 * [CapturaUiState.Revision] lleva el par del Borrador en memoria
 * (spec ocr-mlkit): `rutaFoto` + `textoCrudo` del OCR (etapa 1 del ADR 0002).
 *
 * Spec procesado-ui (mockup 06): sobre el Borrador vivo, "Continuar" abre el
 * procesado de la extraccion ([CapturaUiState.ProcesandoExtraccion]) y de el
 * salen el resultado de solo lectura ([CapturaUiState.ExtraccionLista]) o el
 * error honesto ([CapturaUiState.ExtraccionFallida]); cancelar/volver deja el
 * Borrador intacto en la pantalla 05.
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

    /**
     * Procesado en curso (mockup 06): `ExtraerTicket` corriendo sobre el texto
     * crudo del Borrador. La foto ya esta en disco y NO se toca.
     */
    data class ProcesandoExtraccion(
        val rutaFoto: String,
        val textoCrudo: String,
    ) : CapturaUiState

    /**
     * Resultado de solo lectura (spec procesado-ui T4): el [TicketExtraido]
     * validado a la espera de `revision-datos` (edicion/guardado son de otra
     * feature). El motor del resultado es `extraido.motor`.
     */
    data class ExtraccionLista(
        val rutaFoto: String,
        val textoCrudo: String,
        val extraido: TicketExtraido,
    ) : CapturaUiState

    /**
     * Error de extraccion/red (spec procesado-ui T5): mensaje honesto del
     * fallo, sin datos inventados ni fallback a otro motor (ADR 0001). El
     * Borrador y la foto se conservan.
     */
    data class ExtraccionFallida(
        val rutaFoto: String,
        val textoCrudo: String,
        val mensaje: String,
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
 *
 * Spec procesado-ui: la extraccion la dispara el usuario ("Continuar") con
 * [ExtraerTicket] sobre el texto crudo del Borrador. La orden de extraccion es
 * otra entrada del flujo: `flatMapLatest` cancela la corrutina activa cuando
 * llega una orden nueva (Continuar/Reintentar/Cancelar/Volver) y una
 * [CancellationException] se re-lanza SIEMPRE (nunca se convierte en estado de
 * error ni en resultado; R-PROC4). No se toca `TicketRepository`: esta feature
 * no persiste nada (non-goal; cero datos huerfanos).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EscanearViewModel @Inject constructor(
    private val guardarFotoCapturada: GuardarFotoCapturada,
    private val reconocerTextoCrudo: ReconocerTextoCrudo,
    private val extraerTicket: ExtraerTicket,
    private val photoFileStore: PhotoFileStore,
) : ViewModel() {

    private val permisoConcedido = MutableStateFlow(false)
    private val entradaCaptura = MutableStateFlow<EntradaCaptura>(EntradaCaptura.Ninguna)
    private val ordenExtraccion = MutableStateFlow<OrdenExtraccion>(OrdenExtraccion.Ninguna)
    private var intentoExtraccion = 0

    /**
     * Motor que procesara la extraccion (spec procesado-ui T1/T3): passthrough
     * de `ExtraerTicket.motor`, disponible ANTES de llamar al motor para que el
     * badge sea honesto desde el primer instante del procesado.
     */
    val motor: Motor get() = extraerTicket.motor

    val estado: StateFlow<CapturaUiState> = combine(
        permisoConcedido,
        entradaCaptura.flatMapLatest { entrada ->
            // La captura se procesa UNA sola vez por entrada (el pipeline de
            // foto/OCR no se duplica aunque haya varios colectores).
            combine(resultadoDeCaptura(entrada), ordenExtraccion) { captura, orden ->
                captura to orden
            }.flatMapLatest { (captura, orden) ->
                extraccionDe(captura, orden).map { extraccion -> captura to extraccion }
            }
        },
    ) { permiso, (captura, extraccion) ->
        estadoDe(permiso, captura, extraccion)
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
    fun onImageCapturado(rawJpeg: ByteArray, crop: CropRegion) {
        entradaCaptura.value = EntradaCaptura.Cruda(rawJpeg, crop)
    }

    /** La camara no pudo capturar (sin frames, error de dispositivo...). */
    fun onCapturaFallida(mensaje: String) {
        entradaCaptura.value = EntradaCaptura.Fallida(mensaje)
    }

    /**
     * "Continuar" (pantalla 05): dispara la extraccion del texto crudo del
     * Borrador. El numero de intento hace que pulsaciones repetidas vuelvan a
     * emitir (StateFlow deduplica valores iguales).
     */
    fun onContinuar() {
        intentoExtraccion += 1
        ordenExtraccion.value = OrdenExtraccion.Continuar(intentoExtraccion)
    }

    /**
     * "Reintentar" (estado de error): nueva extraccion con el MISMO motor
     * (ADR 0001: sin fallback silencioso a otro motor).
     */
    fun onReintentarExtraccion() {
        intentoExtraccion += 1
        ordenExtraccion.value = OrdenExtraccion.Reintentar(intentoExtraccion)
    }

    /**
     * "Cancelar" (o back del sistema durante el procesado): aborta la extraccion
     * en curso y vuelve a la pantalla 05 con el Borrador intacto (misma
     * `rutaFoto` y `textoCrudo`; la foto NO se borra y nada se persiste).
     */
    fun onCancelarProcesado() {
        ordenExtraccion.value = OrdenExtraccion.Cancelar
    }

    /**
     * "Volver a la foto" (error; y back del sistema desde el resultado): vuelve
     * a la pantalla 05 con el Borrador intacto, sin borrar la foto.
     */
    fun onVolverALaFoto() {
        ordenExtraccion.value = OrdenExtraccion.Volver
    }

    /**
     * "Repetir foto" (y volver al encuadre desde Error): descarta la foto y el
     * texto crudo del borrador reemplazados; la foto se borra en disco
     * (best-effort; escenario 7 del spec captura-foto) y el texto desaparece
     * (escenario 5 del spec ocr-mlkit). La foto de un ticket Guardado solo se
     * borra en cascada. Cambiar la entrada de captura cancela tambien cualquier
     * extraccion en curso (cancelacion estructurada).
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
                val ruta = runCatchingLanzandoCancelacion {
                    guardarFotoCapturada(entrada.bytes, entrada.crop)
                }
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

    /**
     * Extraccion del Borrador segun la orden del usuario (spec procesado-ui):
     * solo "Continuar"/"Reintentar" sobre un Borrador vivo invocan a
     * [ExtraerTicket]; cualquier otra orden deja la extraccion en `Ninguna`.
     * Cuando `flatMapLatest` cancela este flujo, la [CancellationException]
     * sale por arriba sin convertirse en `Fallida` (R-PROC4).
     */
    private fun extraccionDe(
        captura: CapturaEnProgreso,
        orden: OrdenExtraccion,
    ): Flow<ExtraccionEnProgreso> {
        val borrador = captura as? CapturaEnProgreso.Guardada
            ?: return flowOf(ExtraccionEnProgreso.Ninguna)
        return when (orden) {
            is OrdenExtraccion.Continuar, is OrdenExtraccion.Reintentar -> flow {
                emit(ExtraccionEnProgreso.EnProgreso)
                val resultado = runCatchingLanzandoCancelacion { extraerTicket(borrador.textoCrudo) }
                emit(
                    resultado.fold(
                        onSuccess = { ExtraccionEnProgreso.Lista(it) },
                        onFailure = {
                            ExtraccionEnProgreso.Fallida(it.message ?: MENSAJE_ERROR_EXTRACCION)
                        },
                    )
                )
            }
            else -> flowOf(ExtraccionEnProgreso.Ninguna)
        }
    }

    private fun estadoDe(
        permiso: Boolean,
        captura: CapturaEnProgreso,
        extraccion: ExtraccionEnProgreso,
    ): CapturaUiState = when {
        !permiso -> CapturaUiState.PermisoPendiente
        captura is CapturaEnProgreso.Ninguna -> CapturaUiState.Encuadre
        captura is CapturaEnProgreso.Procesando -> CapturaUiState.Procesando
        captura is CapturaEnProgreso.Fallida -> CapturaUiState.Error(captura.mensaje)
        captura is CapturaEnProgreso.Guardada -> when (extraccion) {
            ExtraccionEnProgreso.Ninguna -> CapturaUiState.Revision(
                rutaFoto = captura.rutaFoto,
                textoCrudo = captura.textoCrudo,
                ocrNoDisponible = captura.ocrNoDisponible,
            )
            ExtraccionEnProgreso.EnProgreso -> CapturaUiState.ProcesandoExtraccion(
                rutaFoto = captura.rutaFoto,
                textoCrudo = captura.textoCrudo,
            )
            is ExtraccionEnProgreso.Lista -> CapturaUiState.ExtraccionLista(
                rutaFoto = captura.rutaFoto,
                textoCrudo = captura.textoCrudo,
                extraido = extraccion.extraido,
            )
            is ExtraccionEnProgreso.Fallida -> CapturaUiState.ExtraccionFallida(
                rutaFoto = captura.rutaFoto,
                textoCrudo = captura.textoCrudo,
                mensaje = extraccion.mensaje,
            )
        }
        else -> CapturaUiState.Error("Estado de captura inesperado")
    }

    /** Entradas del flujo: bytes de captura o fallo de la camara. */
    private sealed interface EntradaCaptura {
        data object Ninguna : EntradaCaptura
        class Cruda(val bytes: ByteArray, val crop: CropRegion) : EntradaCaptura
        data class Fallida(val mensaje: String) : EntradaCaptura
    }

    /**
     * Orden del usuario sobre la extraccion (spec procesado-ui), otra entrada
     * del flujo. [Continuar] y [Reintentar] llevan numero de intento para que
     * pulsaciones repetidas vuelvan a emitir. La llegada de cualquier orden
     * nueva cancela la extraccion activa (`flatMapLatest`).
     */
    private sealed interface OrdenExtraccion {
        data object Ninguna : OrdenExtraccion
        data class Continuar(val intento: Int) : OrdenExtraccion
        data class Reintentar(val intento: Int) : OrdenExtraccion
        data object Cancelar : OrdenExtraccion
        data object Volver : OrdenExtraccion
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

    /** Resultado derivado de la extraccion del Borrador actual. */
    private sealed interface ExtraccionEnProgreso {
        data object Ninguna : ExtraccionEnProgreso
        data object EnProgreso : ExtraccionEnProgreso
        data class Lista(val extraido: TicketExtraido) : ExtraccionEnProgreso
        data class Fallida(val mensaje: String) : ExtraccionEnProgreso
    }

    private companion object {
        const val MENSAJE_ERROR_EXTRACCION = "No se pudieron extraer los datos del ticket"
    }
}

/**
 * Como `runCatching`, pero re-lanza SIEMPRE la [CancellationException]
 * (finding Low sesion 014 / R-OCR4 / R-PROC4): cancelar no es un fallo de
 * negocio y no debe convertirse en resultado.
 */
private inline fun <T> runCatchingLanzandoCancelacion(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }

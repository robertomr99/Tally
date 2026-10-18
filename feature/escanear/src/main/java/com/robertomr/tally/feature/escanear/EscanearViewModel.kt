package com.robertomr.tally.feature.escanear

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robertomr.tally.domain.expenses.data.CropRegion
import com.robertomr.tally.domain.expenses.data.PhotoFileStore
import com.robertomr.tally.domain.expenses.entities.Categoria
import com.robertomr.tally.domain.expenses.entities.DatosRevision
import com.robertomr.tally.domain.expenses.entities.DisponibilidadMotor
import com.robertomr.tally.domain.expenses.entities.MotorNoDisponibleException
import com.robertomr.tally.domain.expenses.entities.OfertaMotor
import com.robertomr.tally.domain.expenses.entities.LineaEditable
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.TicketExtraido
import com.robertomr.tally.domain.expenses.entities.datosRevisionInicial
import com.robertomr.tally.domain.expenses.entities.nuevaLineaEditable
import com.robertomr.tally.domain.expenses.entities.parsearCantidad
import com.robertomr.tally.domain.expenses.entities.parsearPrecio
import com.robertomr.tally.domain.expenses.usecases.ExtraerTicket
import com.robertomr.tally.domain.expenses.usecases.ExtraerTicketConMotor
import com.robertomr.tally.domain.expenses.usecases.ObtenerOtroMotor
import com.robertomr.tally.domain.expenses.usecases.GuardarTicket
import com.robertomr.tally.domain.expenses.usecases.GuardarFotoCapturada
import com.robertomr.tally.domain.expenses.usecases.ReconocerTextoCrudo
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
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
 *
 * Spec revision-datos (mockup 07): desde el resultado, "Revisar ticket" abre la
 * revision editable ([CapturaUiState.RevisionDatos]); "Guardar" persiste el
 * ticket ([CapturaUiState.Guardando] -> [CapturaUiState.Guardado]) y la pantalla
 * emite el evento de vuelta al listado. Spec edicion-lineas (mockup 08): en la
 * misma revision se anaden, eliminan y editan las lineas; el total del ticket no
 * se deriva de ellas.
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
        val motor: Motor,
        val anterior: TicketExtraido? = null,
    ) : CapturaUiState

    /**
     * Resultado de solo lectura (spec procesado-ui T4): el [TicketExtraido]
     * validado con completitud de campos, a la espera de "Revisar ticket".
     * [otro] es el motor alternativo ofrecido para reintentar (spec
     * reintento-otro-motor).
     */
    data class ExtraccionLista(
        val rutaFoto: String,
        val textoCrudo: String,
        val extraido: TicketExtraido,
        val otro: OfertaMotor,
    ) : CapturaUiState

    /**
     * Error de extraccion/red (spec procesado-ui T5): mensaje honesto del
     * fallo, sin datos inventados ni fallback a otro motor (ADR 0001). El
     * Borrador y la foto se conservan. [motor] es el que fallo; [anterior] el
     * resultado previo conservado durante un reintento (spec
     * reintento-otro-motor, D4); [otro] el motor alternativo.
     */
    data class ExtraccionFallida(
        val rutaFoto: String,
        val textoCrudo: String,
        val mensaje: String,
        val motor: Motor,
        val anterior: TicketExtraido? = null,
        val otro: OfertaMotor,
    ) : CapturaUiState

    /**
     * Ticket digitalizado editable (spec revision-datos, mockup 07): la cabecera
     * corregida y las lineas editables ([datos]) sobre la extraccion tal cual
     * ([extraido]). [errorGuardado] lleva el motivo real si el guardado fallo
     * (la UI lo pinta con la paleta `error`); null en la revision normal.
     */
    data class RevisionDatos(
        val rutaFoto: String,
        val textoCrudo: String,
        val extraido: TicketExtraido,
        val datos: DatosRevision,
        val errorGuardado: String? = null,
    ) : CapturaUiState

    /** Guardado en curso (breve): el ticket se esta persistiendo en Room. */
    data object Guardando : CapturaUiState

    /** Ticket persistido; la pantalla emite el evento de vuelta al listado. */
    data class Guardado(val id: Long) : CapturaUiState

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
 * pulsar Guardar). Politica: el OCR vacio o el fallo del motor NUNCA bloquean
 * la captura (la foto ya esta en disco).
 *
 * Spec procesado-ui: la extraccion la dispara el usuario ("Continuar") con
 * [ExtraerTicket] sobre el texto crudo del Borrador. La orden de extraccion es
 * otra entrada del flujo: `flatMapLatest` cancela la corrutina activa cuando
 * llega una orden nueva (Continuar/Reintentar/Cancelar/Volver) y una
 * [CancellationException] se re-lanza SIEMPRE (nunca se convierte en estado de
 * error ni en resultado; R-PROC4).
 *
 * Spec revision-datos: la edicion de la cabecera vive en [datosRevision]
 * (entrada del flujo) y "Guardar" es otra orden ([ordenGuardado]); el guardado
 * corre [GuardarTicket] (dominio) que persiste el ticket con
 * `TicketRepository.save` (la unica escritura de todo el subflujo). El mapeo
 * vive en dominio; aqui solo se orquesta. Una [CancellationException] del
 * guardado tambien se re-lanza (R-REV4).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EscanearViewModel @Inject constructor(
    private val guardarFotoCapturada: GuardarFotoCapturada,
    private val reconocerTextoCrudo: ReconocerTextoCrudo,
    private val extraerTicket: ExtraerTicket,
    private val extraerTicketConMotor: ExtraerTicketConMotor,
    private val obtenerOtroMotor: ObtenerOtroMotor,
    private val guardarTicket: GuardarTicket,
    private val photoFileStore: PhotoFileStore,
) : ViewModel() {

    private val permisoConcedido = MutableStateFlow(false)
    private val entradaCaptura = MutableStateFlow<EntradaCaptura>(EntradaCaptura.Ninguna)
    private val ordenExtraccion = MutableStateFlow<OrdenExtraccion>(OrdenExtraccion.Ninguna)

    /**
     * Cabecera corregida en la revision (null = no se esta revisando). Es una
     * entrada reactiva: cada edicion recompone el estado visible.
     */
    private val datosRevision = MutableStateFlow<DatosRevision?>(null)
    private val ordenGuardado = MutableStateFlow<OrdenGuardado>(OrdenGuardado.Ninguna)
    private var intentoExtraccion = 0
    // Contador del intento de guardado: "Guardar" es un data class con el
    // intento, de modo que reintentar tras un fallo (sin editar ningun campo)
    // vuelve a emitir y re-ejecuta el guardado (MutableStateFlow conflaciona
    // valores iguales).
    private var intentoGuardado = 0
    // Siguiente id de cliente para una linea nueva: solo crece durante la
    // revision, asi que borrar la ultima fila y anadir otra no reutiliza su id.
    private var proximoIdLinea = 0L

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
                extraccionDe(captura, orden).flatMapLatest { extraccion ->
                    flujoDeRevision(captura, extraccion)
                }
            }
        },
    ) { permiso, flujo ->
        estadoDe(permiso, flujo)
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
     * "Reintentar" (estado de error): nueva extraccion con el MISMO motor que
     * fallo, guardado en el estado (ADR 0001: sin fallback silencioso; spec
     * reintento-otro-motor, S5), no el que el router elegiria ahora.
     */
    fun onReintentarExtraccion() {
        val fallida = estado.value as? CapturaUiState.ExtraccionFallida ?: return
        intentoExtraccion += 1
        ordenExtraccion.value = OrdenExtraccion.ReintentarCon(
            intento = intentoExtraccion,
            motor = fallida.motor,
            anterior = fallida.anterior,
        )
    }

    /**
     * "Probar con {otro}" (resultado o error, spec reintento-otro-motor): nueva
     * extraccion con el otro motor sobre el MISMO texto crudo del Borrador (sin
     * OCR ni foto nuevos). Desde el resultado se conserva ese resultado como
     * `anterior` mientras dura el reintento (D4); desde un error se conserva el
     * `anterior` que ya hubiera. Ignorado si el otro motor no esta disponible
     * o si el estado no es resultado/error (p. ej. doble pulsacion).
     */
    fun onReintentarConOtroMotor() {
        val actual = estado.value
        val (otro, anterior) = when (actual) {
            is CapturaUiState.ExtraccionLista -> actual.otro to actual.extraido
            is CapturaUiState.ExtraccionFallida -> actual.otro to actual.anterior
            else -> return
        }
        if (otro.disponibilidad != DisponibilidadMotor.DISPONIBLE) return
        intentoExtraccion += 1
        ordenExtraccion.value = OrdenExtraccion.ReintentarCon(
            intento = intentoExtraccion,
            motor = otro.motor,
            anterior = anterior,
        )
    }

    /**
     * "Volver al resultado anterior" (error de un reintento): restaura el
     * resultado conservado sin llamar a ningun motor.
     */
    fun onVolverAlResultadoAnterior() {
        val anterior = (estado.value as? CapturaUiState.ExtraccionFallida)?.anterior ?: return
        ordenExtraccion.value = OrdenExtraccion.Restaurar(anterior)
    }

    /**
     * "Cancelar" (o back del sistema durante el procesado): aborta la extraccion
     * en curso y vuelve a la pantalla 05 con el Borrador intacto (misma
     * `rutaFoto` y `textoCrudo`; la foto NO se borra y nada se persiste). Si el
     * procesado es un reintento con resultado `anterior`, vuelve a ese
     * resultado (spec reintento-otro-motor, S6).
     */
    fun onCancelarProcesado() {
        val anterior = (estado.value as? CapturaUiState.ProcesandoExtraccion)?.anterior
        ordenExtraccion.value = if (anterior != null) {
            OrdenExtraccion.Restaurar(anterior)
        } else {
            OrdenExtraccion.Cancelar
        }
    }

    /**
     * "Volver a la foto" (error; y back del sistema desde el resultado): vuelve
     * a la pantalla 05 con el Borrador intacto, sin borrar la foto.
     */
    fun onVolverALaFoto() {
        ordenExtraccion.value = OrdenExtraccion.Volver
    }

    /**
     * "Revisar ticket" (resultado, spec revision-datos): abre la revision
     * editable con la cabecera y las lineas prefijadas por [datosRevisionInicial]
     * (las lineas se editan con los eventos `onLinea*`, spec edicion-lineas).
     */
    fun onRevisar() {
        val actual = estado.value
        if (actual !is CapturaUiState.ExtraccionLista) return
        val datos = datosRevisionInicial(actual.extraido)
        proximoIdLinea = datos.lineas.size.toLong()
        datosRevision.value = datos
        ordenGuardado.value = OrdenGuardado.Revisar
    }

    fun onComercioEditado(comercio: String) {
        actualizarDatos { it.copy(comercio = comercio) }
    }

    /** Fecha como texto `d/M/yyyy`; un texto no parseable se ignora. */
    fun onFechaEditada(texto: String) {
        val fecha = parsearFecha(texto) ?: return
        actualizarDatos { it.copy(fechaEmision = fecha) }
    }

    /** Total como texto; coma o punto decimal; un texto no parseable se ignora. */
    fun onTotalEditado(texto: String) {
        val total = parsearTotal(texto) ?: return
        actualizarDatos { it.copy(total = total) }
    }

    /** Chips de categoria: solo las 6 de la enum fija (CONTEXT.md). */
    fun onCategoriaElegida(categoria: Categoria) {
        actualizarDatos { it.copy(categoria = categoria) }
    }

    /**
     * "Añadir producto o linea" (spec edicion-lineas, mockup 08): anade una
     * fila vacia (concepto "", cantidad 1, precio 0) con id de cliente nuevo.
     * Nunca toca el total del ticket.
     */
    fun onLineaAnadida() {
        val nueva = nuevaLineaEditable(proximoIdLinea++)
        actualizarDatos { it.copy(lineas = it.lineas + nueva) }
    }

    fun onLineaConceptoEditado(id: Long, concepto: String) {
        actualizarLinea(id) { it.copy(concepto = concepto) }
    }

    /** Cantidad entera >= 1; un texto no parseable, decimal o < 1 se ignora. */
    fun onLineaCantidadEditada(id: Long, texto: String) {
        val cantidad = parsearCantidad(texto) ?: return
        actualizarLinea(id) { it.copy(cantidad = cantidad) }
    }

    /** Precio >= 0 con coma o punto decimal; no parseable o negativo se ignora. */
    fun onLineaPrecioEditado(id: Long, texto: String) {
        val precio = parsearPrecio(texto) ?: return
        actualizarLinea(id) { it.copy(precioUnitario = precio) }
    }

    /** Boton borrar de la fila: elimina la linea por id (el total no cambia). */
    fun onLineaEliminada(id: Long) {
        actualizarDatos { datos -> datos.copy(lineas = datos.lineas.filterNot { it.id == id }) }
    }

    /**
     * "Guardar" (spec revision-datos): persiste el ticket con [GuardarTicket]
     * (Room via `TicketRepository.save`) y deja el subflujo en [CapturaUiState.Guardando]
     * -> [CapturaUiState.Guardado]. Los campos vacios NO bloquean (R-REV2).
     */
    fun onGuardar() {
        if (datosRevision.value == null) return
        ordenGuardado.value = OrdenGuardado.Guardar(++intentoGuardado)
    }

    /**
     * Back del sistema (o "Volver") desde la revision: no escribe nada en Room y
     * vuelve al resultado del procesado con el Borrador intacto (escenario 6).
     */
    fun onVolverAlResultado() {
        datosRevision.value = null
        ordenGuardado.value = OrdenGuardado.Ninguna
    }

    /**
     * Cierre del guardado (la pantalla ya emitio el evento de vuelta al
     * listado): resetea el subflujo a [CapturaUiState.Encuadre] para que volver a
     * Escanear muestre la camara limpia y no la revision del ticket guardado
     * (R-REV3).
     */
    fun onGuardadoVisto() {
        entradaCaptura.value = EntradaCaptura.Ninguna
        ordenExtraccion.value = OrdenExtraccion.Ninguna
        datosRevision.value = null
        ordenGuardado.value = OrdenGuardado.Ninguna
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

    private fun actualizarDatos(transformacion: (DatosRevision) -> DatosRevision) {
        val actual = datosRevision.value ?: return
        datosRevision.value = transformacion(actual)
        // Una edicion deja la revision limpia (borra un posible error de guardado).
        ordenGuardado.value = OrdenGuardado.Revisar
    }

    /** Edita SOLO la linea [id] (direccionada por id de cliente, R-EDI2). */
    private fun actualizarLinea(id: Long, transformacion: (LineaEditable) -> LineaEditable) {
        actualizarDatos { datos ->
            datos.copy(
                lineas = datos.lineas.map { linea ->
                    if (linea.id == id) transformacion(linea) else linea
                },
            )
        }
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
            // Primer intento: el motor se resuelve UNA vez al empezar (badge
            // honesto, spec reintento-otro-motor S8).
            is OrdenExtraccion.Continuar -> flow {
                val motor = extraerTicket.motor
                emitAll(extraer(motor, anterior = null) { extraerTicket(borrador.textoCrudo) })
            }
            // Reintento explicito con un motor concreto sobre el MISMO texto
            // crudo del Borrador (sin OCR ni foto nuevos).
            is OrdenExtraccion.ReintentarCon -> extraer(orden.motor, orden.anterior) {
                extraerTicketConMotor(borrador.textoCrudo, orden.motor)
            }
            is OrdenExtraccion.Restaurar -> flowOf(
                ExtraccionEnProgreso.Lista(orden.anterior, obtenerOtroMotor(orden.anterior.motor))
            )
            else -> flowOf(ExtraccionEnProgreso.Ninguna)
        }
    }

    /**
     * Una extraccion con [motor]: `EnProgreso` -> `Lista` o `Fallida`. La
     * [CancellationException] se re-lanza (nunca es error ni resultado).
     */
    private fun extraer(
        motor: Motor,
        anterior: TicketExtraido?,
        bloque: suspend () -> TicketExtraido,
    ): Flow<ExtraccionEnProgreso> = flow {
        emit(ExtraccionEnProgreso.EnProgreso(motor, anterior))
        val resultado = runCatchingLanzandoCancelacion { bloque() }
        emit(
            resultado.fold(
                onSuccess = { ExtraccionEnProgreso.Lista(it, obtenerOtroMotor(it.motor)) },
                onFailure = {
                    ExtraccionEnProgreso.Fallida(
                        mensaje = mensajeDeError(it),
                        motor = motor,
                        anterior = anterior,
                        otro = obtenerOtroMotor(motor),
                    )
                },
            )
        )
    }

    private fun mensajeDeError(error: Throwable): String =
        if (error is MotorNoDisponibleException) {
            motivoNoDisponible(error.disponibilidad) ?: MENSAJE_ERROR_EXTRACCION
        } else {
            error.message ?: MENSAJE_ERROR_EXTRACCION
        }

    /**
     * Etapa de revision/guardado (spec revision-datos): solo actua cuando la
     * extraccion ya termino en [ExtraccionEnProgreso.Lista]; combina la
     * cabecera editada ([datosRevision]) con la orden ([ordenGuardado]). Un
     * cambio de entrada cancela el guardado en curso (cancelacion estructurada).
     */
    private fun flujoDeRevision(
        captura: CapturaEnProgreso,
        extraccion: ExtraccionEnProgreso,
    ): Flow<FlujoInterno> {
        if (extraccion !is ExtraccionEnProgreso.Lista) {
            return flowOf(
                FlujoInterno(
                    captura = captura,
                    extraccion = extraccion,
                    datos = null,
                    guardado = GuardadoEnProgreso.Ninguna,
                )
            )
        }
        return combine(datosRevision, ordenGuardado) { datos, orden -> datos to orden }
            .flatMapLatest { (datos, orden) ->
                guardadoDe(captura, extraccion, orden, datos).map { guardado ->
                    FlujoInterno(
                        captura = captura,
                        extraccion = extraccion,
                        datos = datos,
                        guardado = guardado,
                    )
                }
            }
    }

    /**
     * Guardado del ticket (spec revision-datos): solo la orden `Guardar` sobre
     * un Borrador vivo escribe en Room, con [GuardarTicket]. Exito -> `Hecho(id)`
     * (la pantalla navega al listado); fallo -> `Fallido(mensaje)` (la revision
     * lo muestra con la paleta `error`). La [CancellationException] se re-lanza
     * SIEMPRE: cancelar el guardado no es un fallo de negocio (R-REV4).
     */
    private fun guardadoDe(
        captura: CapturaEnProgreso,
        extraccion: ExtraccionEnProgreso.Lista,
        orden: OrdenGuardado,
        datos: DatosRevision?,
    ): Flow<GuardadoEnProgreso> {
        if (orden !is OrdenGuardado.Guardar) return flowOf(GuardadoEnProgreso.Ninguna)
        val borrador = captura as? CapturaEnProgreso.Guardada
            ?: return flowOf(GuardadoEnProgreso.Ninguna)
        return flow {
            emit(GuardadoEnProgreso.EnProgreso)
            val resultado = runCatchingLanzandoCancelacion {
                guardarTicket(
                    extraido = extraccion.extraido,
                    rutaFoto = borrador.rutaFoto,
                    textoCrudo = borrador.textoCrudo,
                    datos = datos ?: datosRevisionInicial(extraccion.extraido),
                )
            }
            emit(
                resultado.fold(
                    onSuccess = { GuardadoEnProgreso.Hecho(it) },
                    onFailure = { GuardadoEnProgreso.Fallido(it.message ?: MENSAJE_ERROR_GUARDADO) },
                )
            )
        }
    }

    private fun estadoDe(permiso: Boolean, flujo: FlujoInterno): CapturaUiState = when {
        !permiso -> CapturaUiState.PermisoPendiente
        flujo.captura is CapturaEnProgreso.Ninguna -> CapturaUiState.Encuadre
        flujo.captura is CapturaEnProgreso.Procesando -> CapturaUiState.Procesando
        flujo.captura is CapturaEnProgreso.Fallida ->
            CapturaUiState.Error(flujo.captura.mensaje)
        flujo.captura is CapturaEnProgreso.Guardada -> estadoDeBorrador(flujo)
        else -> CapturaUiState.Error("Estado de captura inesperado")
    }

    private fun estadoDeBorrador(flujo: FlujoInterno): CapturaUiState {
        val captura = flujo.captura as CapturaEnProgreso.Guardada
        return when (val extraccion = flujo.extraccion) {
            ExtraccionEnProgreso.Ninguna -> CapturaUiState.Revision(
                rutaFoto = captura.rutaFoto,
                textoCrudo = captura.textoCrudo,
                ocrNoDisponible = captura.ocrNoDisponible,
            )
            is ExtraccionEnProgreso.EnProgreso -> CapturaUiState.ProcesandoExtraccion(
                rutaFoto = captura.rutaFoto,
                textoCrudo = captura.textoCrudo,
                motor = extraccion.motor,
                anterior = extraccion.anterior,
            )
            is ExtraccionEnProgreso.Fallida -> CapturaUiState.ExtraccionFallida(
                rutaFoto = captura.rutaFoto,
                textoCrudo = captura.textoCrudo,
                mensaje = extraccion.mensaje,
                motor = extraccion.motor,
                anterior = extraccion.anterior,
                otro = extraccion.otro,
            )
            is ExtraccionEnProgreso.Lista -> estadoDeRevision(
                captura = captura,
                extraido = extraccion.extraido,
                otro = extraccion.otro,
                datos = flujo.datos,
                guardado = flujo.guardado,
            )
        }
    }

    private fun estadoDeRevision(
        captura: CapturaEnProgreso.Guardada,
        extraido: TicketExtraido,
        otro: OfertaMotor,
        datos: DatosRevision?,
        guardado: GuardadoEnProgreso,
    ): CapturaUiState = when {
        guardado is GuardadoEnProgreso.EnProgreso -> CapturaUiState.Guardando
        guardado is GuardadoEnProgreso.Hecho -> CapturaUiState.Guardado(guardado.id)
        datos == null -> CapturaUiState.ExtraccionLista(
            rutaFoto = captura.rutaFoto,
            textoCrudo = captura.textoCrudo,
            extraido = extraido,
            otro = otro,
        )
        guardado is GuardadoEnProgreso.Fallido -> CapturaUiState.RevisionDatos(
            rutaFoto = captura.rutaFoto,
            textoCrudo = captura.textoCrudo,
            extraido = extraido,
            datos = datos,
            errorGuardado = guardado.mensaje,
        )
        else -> CapturaUiState.RevisionDatos(
            rutaFoto = captura.rutaFoto,
            textoCrudo = captura.textoCrudo,
            extraido = extraido,
            datos = datos,
        )
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
        data class ReintentarCon(
            val intento: Int,
            val motor: Motor,
            val anterior: TicketExtraido?,
        ) : OrdenExtraccion
        data class Restaurar(val anterior: TicketExtraido) : OrdenExtraccion
        data object Cancelar : OrdenExtraccion
        data object Volver : OrdenExtraccion
    }

    /**
     * Orden del usuario sobre el guardado (spec revision-datos): `Revisar` abre
     * la pantalla 07 y `Guardar` persiste. `Ninguna` = fuera de la revision.
     * `Guardar` lleva el numero de intento para poder re-dispararse aunque el
     * usuario reintente sin editar (conflacion de `MutableStateFlow`).
     */
    private sealed interface OrdenGuardado {
        data object Ninguna : OrdenGuardado
        data object Revisar : OrdenGuardado
        data class Guardar(val intento: Int) : OrdenGuardado
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
        data class EnProgreso(
            val motor: Motor,
            val anterior: TicketExtraido?,
        ) : ExtraccionEnProgreso
        data class Lista(val extraido: TicketExtraido, val otro: OfertaMotor) : ExtraccionEnProgreso
        data class Fallida(
            val mensaje: String,
            val motor: Motor,
            val anterior: TicketExtraido?,
            val otro: OfertaMotor,
        ) : ExtraccionEnProgreso
    }

    /** Resultado derivado del guardado del ticket revisado. */
    private sealed interface GuardadoEnProgreso {
        data object Ninguna : GuardadoEnProgreso
        data object EnProgreso : GuardadoEnProgreso
        data class Hecho(val id: Long) : GuardadoEnProgreso
        data class Fallido(val mensaje: String) : GuardadoEnProgreso
    }

    /**
     * Instantanea del subflujo: captura + extraccion + cabecera editada +
     * guardado. Es lo que [estadoDe] traduce al estado visible.
     */
    private data class FlujoInterno(
        val captura: CapturaEnProgreso,
        val extraccion: ExtraccionEnProgreso,
        val datos: DatosRevision?,
        val guardado: GuardadoEnProgreso,
    )

    private companion object {
        const val MENSAJE_ERROR_EXTRACCION = "No se pudieron extraer los datos del ticket"
        const val MENSAJE_ERROR_GUARDADO = "No se pudo guardar el ticket"

        val FORMATO_FECHA_REVISION: DateTimeFormatter =
            DateTimeFormatter.ofPattern("d/M/yyyy")

        fun parsearFecha(texto: String): LocalDateTime? = try {
            LocalDate.parse(texto.trim(), FORMATO_FECHA_REVISION).atStartOfDay()
        } catch (e: DateTimeParseException) {
            null
        }

        fun parsearTotal(texto: String): Double? =
            texto.trim().replace(',', '.').toDoubleOrNull()
    }
}

/**
 * Como `runCatching`, pero re-lanza SIEMPRE la [CancellationException]
 * (finding Low sesion 014 / R-OCR4 / R-PROC4 / R-REV4): cancelar no es un fallo
 * de negocio y no debe convertirse en resultado.
 */
private inline fun <T> runCatchingLanzandoCancelacion(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }

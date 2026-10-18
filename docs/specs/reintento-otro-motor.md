# Feature Implementation Spec: Reintentar la extracción con el otro motor

## Source Feature

- `id`: reintento-otro-motor
- `area`: revision
- `title`: Reintentar extracción con el otro motor
- `user_visible_behavior`: Con un resultado inservible, el usuario reintenta cambiando de motor (Local↔Nube) sobre el mismo texto crudo; el nuevo motor queda registrado.
- `depends_on`: ["router-automatico"] (`accepted`, sesión 035)
- `status`: not_started (planificada 2026-10-18)
- `verification` (fuente): (1) en directo, extracción nube mala → reintento local (y viceversa) en la Tab S9+; (2) el texto crudo se reutiliza: no se repite el OCR.
- `notes` (fuente): decisión de producto del descubrimiento (reintento + revisión manual como última vía). ADR 0001: sin fallback silencioso; ADR 0002: ambos motores consumen exactamente el mismo texto crudo.

## Goal

Desde el **resultado** (`ExtraccionLista`) y desde el **error** (`ExtraccionFallida`) de procesado, el usuario puede lanzar una nueva extracción con el **otro** motor sobre el **mismo `textoCrudo` del Borrador** (sin repetir OCR ni foto). El motor que produjo cada resultado/error es siempre visible (badge) y el que queda en el ticket guardado (`Ticket.motor`) es el del resultado aceptado. Sin fallback automático.

## Non-Goals

- `ajustes-motor`: selector Auto/Local/Nube, persistencia, pantalla. `PreferenciaMotorSource` sigue fija en AUTO y **no interviene** en el reintento explícito.
- Reintento desde la revisión editable (mockup 07/`RevisionDatos`): se perderían las ediciones; fuera de alcance (R-RO4). Solo desde resultado y error.
- Fallback automático, comparador lado a lado, historial de intentos persistido, log de extracciones (`log-extracciones`), mejora de calidad del motor local, cambios de OCR, Room/`schemas/`, `init.sh`, `build-logic/`, módulos Gradle nuevos, Retrofit/OkHttp, claves en el APK.
- Detección de red como condición de disponibilidad (misma decisión D2 de router-automatico).

## Job Story

When la extracción de un motor me da un resultado inservible (totales mal, error de red o de carga del modelo),
I want reintentar yo mismo con el otro motor sin repetir la foto ni el OCR,
so I can llegar a un resultado útil sabiendo en todo momento qué motor produjo cada cosa.

## Acceptance Scenarios

### S1: acción en el resultado
Given `ExtraccionLista` producida por el motor M
Then, junto a "Revisar ticket", se ve un botón secundario "Probar con {otro}" (tag `procesado_reintentar_otro`) donde otro = ¬M y la etiqueta usa `etiquetaMotor`-corto ("Probar con Nube · Gemini" / "Probar con Local · Gemma 3n").
When lo pulsa y el otro motor está `DISPONIBLE`
Then pasa a `ProcesandoExtraccion` con badge del **otro** motor (estado procesando), `ExtraerTicketConMotor` corre sobre el `textoCrudo` del Borrador, y al terminar muestra `ExtraccionLista` con `extraido.motor` = otro y el badge de ese motor.

### S2: acción en el error
Given `ExtraccionFallida` tras el motor M (badge FALLIDO de M)
Then hay tres salidas: "Volver a la foto", "Reintentar" (mismo M, ver S5) y "Probar con {otro}" (tag `procesado_reintentar_otro`). Pulsar la última lanza la extracción con ¬M como en S1.

### S3: disponibilidad del otro motor (sin ocultar la razón)
Given el otro motor es Nube → siempre `DISPONIBLE` (si no hay red/cuota, falla honestamente tras el intento, sin pre-detección).
Given el otro motor es Local → `DISPONIBLE` solo si `capaz && modeloDisponible`; si no, el botón se **muestra deshabilitado** con motivo (tag `procesado_reintentar_otro_motivo`): `SIN_MODELO` → "Descarga el modelo local en Ajustes para usarlo"; `NO_CAPAZ` → "Este dispositivo no puede ejecutar el motor local". Nunca se invoca un motor no disponible. (Prioridad si fallan ambas: `NO_CAPAZ`.)

### S4: mismo texto crudo, sin OCR
Then en todo reintento `ReconocerTextoCrudo` y `GuardarFotoCapturada` **no** se invocan; el motor recibe exactamente `CapturaUiState.*.textoCrudo` del Borrador (verificable en JVM con `FakeTextEngine.received`).

### S5: "Reintentar" (mismo motor) pasa a ser explícito
Given un error del motor M
When pulsa "Reintentar"
Then se reintenta con **M** (el que falló, guardado en el estado), no con el que el router elegiría ahora. Cambio interno respecto a procesado-ui (que re-evaluaba la política); mismo comportamiento visible mientras el entorno no cambie.

### S6: qué pasa con el resultado anterior
Given un reintento lanzado desde `ExtraccionLista` (resultado R1 del motor M)
Then R1 se **conserva en memoria** (`anterior`) durante el reintento y solo se reemplaza cuando el nuevo intento **tiene éxito** (R2 pasa a ser el único resultado vivo; `Guardar` persiste R2 y su `motor`).
When cancela durante el reintento (botón Cancelar o back) → vuelve a `ExtraccionLista` con R1 intacto (no a la pantalla 05).
When el reintento falla → `ExtraccionFallida` (badge FALLIDO del motor nuevo) con un botón extra "Volver al resultado anterior" (tag `procesado_volver_anterior`, solo si hay `anterior`) que restaura R1. "Volver a la foto"/back desde ese error descarta R1 y vuelve a la pantalla 05 (como hoy).
Given un reintento lanzado desde `ExtraccionFallida`: no hay `anterior`; cancelar vuelve a la pantalla 05 como hoy.

### S7: cancelación y concurrencia
Then cancelar/volver/nuevo intento cancela la corrutina activa (`flatMapLatest`), `CancellationException` nunca se convierte en error ni resultado (R-PROC4), y nunca hay dos extracciones simultáneas (la anterior se cancela antes de empezar la nueva; `GemmaLocalEngine` ya serializa con `Mutex`). Pulsar el botón dos veces seguidas cuenta como un solo intento efectivo.

### S8: badge honesto (cierra D4 de router-automatico)
Then el badge durante el procesado sale del `motor` **del estado** (`ProcesandoExtraccion.motor`), no de `viewModel.motor` recomputado en recomposición; igual en `ExtraccionFallida.motor`. En el primer intento ("Continuar") ese motor se resuelve una vez al empezar la extracción (`ExtraerTicket.motor`).

### S9: no regresión
Given el flujo sin reintento (Continuar → resultado → Revisar → Guardar), cancelación, back, "Reintentar" mismo motor
Then se comporta igual que antes: tags existentes (`procesado_pantalla`, `procesado_badge_motor`, `procesado_revisar`, `procesado_reintentar`, `procesado_volver_foto`, `procesado_cancelar`, `procesado_error`, `procesado_completitud`) intactos; `ProcesadoFlujoTest`, `CapturaFlujoTest`, `NavigationTest` y el resto de E2E `:app` siguen en verde; revisión/guardado sin cambios.

### S10: arquitectura
`domain/expenses` sin imports de Android/MediaPipe/Firebase; `AiModule` (único `di.kt` de `framework/ai`) sigue `internal`; sin módulos Gradle nuevos; sin versiones literales; sin claves; `schemas/` e `init.sh` sin diff.

## Repository Research

### Files Inspected
`AGENTS.md`; `PROGRESS.md` (cabecera y sesiones 034-035); `feature_list.json` (entrada); `docs/specs/{router-automatico,procesado-ui}.md` (completo el primero; procesado-ui por grep de motor/reintento/toggle); `docs/adr/0001`; `docs/domain-model.md` (transiciones: "Reintento de extracción… puede cambiar de motor; se actualiza el motor registrado"); `docs/design/stitch/06-procesando-ticket/code.html` (toggle "Cambiar a Nube", solo grep) y `07-ticket-generado/code.html` (botones "Reintentar"/"Editar", grep); `domain/expenses/.../{data/EngineRouter,data/TextEngine,data/ModelDownloader,usecases/ExtraerTicket,usecases/GuardarTicket (motor=extraido.motor)}.kt`; `framework/ai/.../di/di.kt`, `GemmaLocalEngine` (Mutex), `MediaPipeLocalLlm` (grep); `feature/escanear/.../{EscanearViewModel,EscanearScreen,ProcesadoContent}.kt`; `app/.../captura/ProcesadoFlujoTest.kt`; `test/unit/.../FakeTextEngine.kt`. **No inspeccionado**: `docs/specs/{extraccion-local,extraccion-nube,revision-datos}.md` ni `risks-and-open-questions.md` completos (solo greps); `EscanearState.kt`; cuerpo completo de `EscanearViewModelTest`; `FakeModelDownloader`/`FakeCapacidadDispositivo` (existen; comprobar firma antes de usar).

### Hechos del código actual
- `EscanearViewModel.estado` = cadena `combine(permiso, entradaCaptura.flatMapLatest{ combine(resultadoDeCaptura, ordenExtraccion).flatMapLatest{ extraccionDe → flujoDeRevision }})`. `OrdenExtraccion` = Ninguna/Continuar(n)/Reintentar(n)/Cancelar/Volver; `Reintentar` y `Continuar` ejecutan el mismo `extraerTicket(borrador.textoCrudo)`. El Borrador (`CapturaEnProgreso.Guardada(rutaFoto, textoCrudo, ocrNoDisponible)`) **ya conserva el texto crudo** y no se re-ejecuta el OCR al extraer: el reintento con otro motor no necesita tocar la captura.
- `CapturaUiState.ProcesandoExtraccion(rutaFoto, textoCrudo)` y `ExtraccionFallida(rutaFoto, textoCrudo, mensaje)` **no llevan el motor**; `EscanearScreen` usa `viewModel.motor` (= `extraerTicket.motor` = router re-evaluado). Con un reintento forzado ese valor sería incorrecto → hay que llevar el motor en el estado.
- `ExtraerTicket(textEngine)` solo conoce el `TextEngine` del router; `EngineRouter` tiene `local`/`nube` privados y no ofrece "extraer con X". `ModelDownloader.rutaModelo()` y `CapacidadDispositivo.puedeEjecutarModeloLocal()` ya existen y son baratos y síncronos.
- `GuardarTicket` persiste `motor = extraido.motor`: el motor registrado del reintento sale gratis; no hay cambio de Room.
- Mockup 06 tiene un toggle "Cambiar a Nube / En dispositivo" (copy ficticia de privacidad prohibida por DESIGN.md); mockup 07 tiene un "Reintentar" outlined con icono refrescar junto a "Editar". Se adopta el patrón del 07 (botón outlined secundario) sin la copy ficticia.
- Constructores a ajustar si cambian: `EscanearViewModel(...)` se instancia en `EscanearViewModelTest` (líneas ~73 y ~431); `ExtraerTicket(...)` NO cambia.

## Technical Approach

1. **Dominio (puro)**, sin tocar `TextEngine` ni `ExtraerTicket`:
   - `entities/DisponibilidadMotor.kt`: `enum { DISPONIBLE, SIN_MODELO, NO_CAPAZ }`; `entities/OfertaMotor.kt`: `data class OfertaMotor(val motor: Motor, val disponibilidad: DisponibilidadMotor)`.
   - `data/ExtractorPorMotor.kt`: `interface ExtractorPorMotor { fun disponibilidad(motor: Motor): DisponibilidadMotor; suspend fun extract(motor: Motor, textoCrudo: String): TicketExtraido }`.
   - `data/EngineRouter.kt`: implementa además `ExtractorPorMotor` (la política `elegirMotor` **no cambia**). Función pura `disponibilidadMotor(motor, capaz, modeloDisponible)`: NUBE→DISPONIBLE; LOCAL→NO_CAPAZ si !capaz, SIN_MODELO si !modelo, si no DISPONIBLE. `extract(motor, texto)`: **no consulta la preferencia**; si `disponibilidad(motor) != DISPONIBLE` lanza `MotorNoDisponibleException(motor, disponibilidad)` (nueva, en `entities/`) sin invocar el engine; si no, delega en `local`/`nube`. Sin `runCatching`, sin fallback.
   - `usecases/ExtraerTicketConMotor.kt`: `@Inject constructor(extractor: ExtractorPorMotor)`, `suspend operator fun invoke(textoCrudo, motor)`: texto blank → `TicketExtraido` vacío con ese `motor` (misma regla que `ExtraerTicket`, sin llamar al motor); si no, `extractor.extract(motor, texto)`. No captura excepciones.
   - `usecases/ObtenerOtroMotor.kt`: `@Inject constructor(extractor)`, `operator fun invoke(actual: Motor): OfertaMotor` (otro = ¬actual; disponibilidad = `extractor.disponibilidad(otro)`).
2. **DI** (`framework/ai/.../di/di.kt`, único): `@Provides @Singleton fun provideEngineRouter(...): EngineRouter` y `provideTextEngine(router): TextEngine = router` + `provideExtractorPorMotor(router): ExtractorPorMotor = router` (misma instancia). `TextEngineEntryPoint`/tests reales no cambian (siguen resolviendo `TextEngine`).
3. **ViewModel** (`EscanearViewModel.kt`, sigue siendo cadena de operadores; sin `load()`):
   - Constructor gana `extraerTicketConMotor: ExtraerTicketConMotor` y `obtenerOtroMotor: ObtenerOtroMotor`.
   - `OrdenExtraccion` gana `ReintentarCon(intento, motor, anterior: TicketExtraido?)` y `Restaurar(anterior)` (emite `Lista(anterior, …)`). `Reintentar(n)` desaparece: `onReintentarExtraccion()` lee `estado.value as? ExtraccionFallida` y emite `ReintentarCon(n, motor = fallida.motor, anterior = fallida.anterior)`.
   - `ExtraccionEnProgreso`: `EnProgreso(motor, anterior)`, `Lista(extraido, otro: OfertaMotor)`, `Fallida(mensaje, motor, anterior, otro: OfertaMotor)`. `Continuar` resuelve `val motor = extraerTicket.motor` una vez al iniciar el flow y usa `extraerTicket(texto)`; `ReintentarCon` usa `extraerTicketConMotor(texto, motor)`. El `otro` se calcula al emitir `Lista`/`Fallida` con `obtenerOtroMotor(motor)` (resultado: `extraido.motor`).
   - `CapturaUiState`: `ProcesandoExtraccion(rutaFoto, textoCrudo, motor, anterior)`, `ExtraccionLista(rutaFoto, textoCrudo, extraido, otro)`, `ExtraccionFallida(rutaFoto, textoCrudo, mensaje, motor, anterior, otro)`. `RevisionDatos` sin cambios.
   - Eventos nuevos: `onReintentarConOtroMotor()` (lee `estado.value`: de `ExtraccionLista` con `anterior=extraido`, motor=`otro.motor`; de `ExtraccionFallida` con `anterior=null`, motor=`otro.motor`; ignora si `otro.disponibilidad != DISPONIBLE` o si el estado no es resultado/error) y `onVolverAlResultadoAnterior()` (de `ExtraccionFallida.anterior`, emite `Restaurar`). `onCancelarProcesado()`: si `ProcesandoExtraccion.anterior != null` → `Restaurar(anterior)`, si no → `Cancelar` (como hoy). `viewModel.motor` se mantiene solo para compatibilidad de tests; la pantalla deja de usarlo.
   - El `anterior` viaja en la orden (entrada), no en estado mutable oculto; `onRepetirFoto`/`onGuardadoVisto` resetean `ordenExtraccion` como hoy.
4. **UI** (`ProcesadoContent.kt`/`EscanearScreen.kt`):
   - `ExtraccionListaContent(extraido, otro, onRevisar, onReintentarOtro)`: `OutlinedButton` (icono `Refresh`, pill, 48 dp) sobre "Revisar ticket"; `enabled = otro.disponibilidad == DISPONIBLE`; si no, `Text` `bodySmall` `onSurfaceVariant` con el motivo debajo.
   - `ExtraccionFallidaContent(mensaje, motor, anterior!=null, otro, onReintentar, onReintentarOtro, onVolverALaFoto, onVolverAnterior)`: añade el outlined "Probar con …" (+motivo) y, si hay `anterior`, `TextButton` "Volver al resultado anterior".
   - `ProcesandoExtraccionContent(motor = actual.motor, …)`; `ExtraccionFallidaContent(motor = actual.motor)`. Back: `ProcesandoExtraccion` sigue = `onCancelarProcesado`; resultado/error sigue = `onVolverALaFoto`.
   - `etiquetaMotorCorta(Motor)`: "Nube · Gemini" / "Local · Gemma 3n" (reutilizar de `etiquetaMotor` quitando el prefijo "Motor "). Sin colores/componentes nuevos de DESIGN.md.
5. **Textos de copy** (español): botones "Probar con Nube · Gemini"/"Probar con Local · Gemma 3n"; motivos S3; "Volver al resultado anterior".

## Expected File Changes

- `domain/expenses/.../entities/{DisponibilidadMotor,OfertaMotor,MotorNoDisponibleException}.kt` — create.
- `domain/expenses/.../data/{ExtractorPorMotor.kt (create), EngineRouter.kt (modify)}`; `.../usecases/{ExtraerTicketConMotor,ObtenerOtroMotor}.kt` — create.
- `framework/ai/.../di/di.kt` — modify (router singleton + 2 bindings).
- `feature/escanear/.../{EscanearViewModel,EscanearScreen,ProcesadoContent}.kt` — modify. `EscanearState.kt` sin cambios.
- Tests: `domain/expenses/src/test/.../data/EngineRouterTest.kt` (modify: `disponibilidadMotor` tabla, `extract(motor,…)` solo invoca el pedido, ignora preferencia, `MotorNoDisponible` sin llamar al engine, errores/cancelación propagados), `.../usecases/{ExtraerTicketConMotorTest,ObtenerOtroMotorTest}.kt` (create), `feature/escanear/src/test/.../EscanearViewModelTest.kt` (modify: constructores con un `EngineRouter` real sobre `FakeTextEngine` LOCAL/NUBE + `FakeCapacidadDispositivo` + `FakeModelDownloader`; escenarios S1-S8), `app/src/androidTest/.../captura/ProcesadoFlujoTest.kt` (modify, ver Verification).
- Docs: `docs/architecture.md` (flujo de reintento y `ExtractorPorMotor`), `docs/specs/reintento-otro-motor.md` (esta), `docs/evidence/reintento-otro-motor/` — create. `domain-model.md`/ADRs/`AGENTS.md`/`DESIGN.md`: not needed (la transición y el ADR 0001 ya lo describen; no se contradice ningún ADR → sin ADR nuevo). `CONTEXT.md`: not needed salvo que el implementer introduzca un término nuevo de dominio visible ("Motor alternativo").
- `PROGRESS.md`, `feature_list.json`: los actualiza el orquestador/implementer con evidencia.

## Visual Design Impact
Mockup 07 aporta el botón outlined con icono de refrescar; el toggle del 06 se reinterpreta como esta misma acción. Sin copy de privacidad ficticia (DESIGN.md). Reutiliza badge y paleta existentes (botón deshabilitado y motivo con `onSurfaceVariant`; error con `error`). Revisión visual humana de capturas (resultado con botón, error con tres salidas, deshabilitado con motivo) en `docs/evidence/reintento-otro-motor/`.

## Implementation Plan / Tasks
- [ ] T1 Dominio: entidades, `ExtractorPorMotor`, `disponibilidadMotor`, `EngineRouter.extract(motor,…)`, use cases + tests JVM (S3, S4, S7). *Done: JUnit XML verde.*
- [ ] T2 DI (`AiModule`) y compilación de `:app`. *Done: `assembleDebug` verde; 0 imports Android/Firebase/MediaPipe en `domain/**`.*
- [ ] T3 VM: estado con `motor`/`anterior`/`otro`, `ReintentarCon`/`Restaurar`, eventos, `onReintentarExtraccion` con el motor del error; actualizar `EscanearViewModelTest` y añadir casos: reintento desde resultado (motor nuevo, `received` = mismo texto, OCR/foto sin llamadas extra), desde error, `otro` deshabilitado no dispara, cancelar restaura R1, fallo con `anterior` + restaurar, doble pulsación, `CancellationException` no es error, guardar tras reintento persiste `motor` del R2. *Done: VM tests verdes (aislar el test inestable ajeno conocido de sesión 035).*
- [ ] T4 UI: botones, motivos y badge desde estado; tags nuevos. *Done: compila.*
- [ ] T5 Regresión: `gradlew.bat assembleDebug testDebugUnitTest :test:unit:test --rerun`; E2E `:app` en `emulator-5554`; ampliar `ProcesadoFlujoTest` (si OCR blank la extracción es vacía y rápida: assert del botón `procesado_reintentar_otro` en el resultado y que, al pulsarlo, el badge cambia al otro motor o el botón está deshabilitado con motivo según el modelo/RAM del emulador — RAM <10 GB ⇒ `NO_CAPAZ`; no depender de red). Búsquedas: 0 versiones literales, 0 `AIza|api_key|buildConfigField`, `schemas/`/`init.sh` sin diff.
- [ ] T6 **Verificación física** en Tab S9+ (abajo); evidencia en `docs/evidence/reintento-otro-motor/`.
- [ ] T7 Docs, `feature_list.json` y `PROGRESS.md`.

## Verification Plan
- JVM: gates de T5. Justificación E2E: la extracción real depende de red/cuota/modelo; los caminos de reintento se prueban deterministas en JVM (`FakeTextEngine.gate/received`), y el E2E del emulador cubre presencia/estado del botón y no regresión.
- **Física Tab S9+** (modelo descargado, RAM 11,7 GB ⇒ Local `DISPONIBLE`; Nube requiere cuota de Gemini, agotada desde 2026-10-17: si sigue agotada, documentarlo y probar al menos Local→Nube con error honesto y Nube→Local desde el error por cuota):
  1. Ticket real (preferiblemente uno de los que el local falla: tickets 1 o 3): Continuar → Local (26-41 s) → resultado con total incorrecto → "Probar con Nube · Gemini" → badge Nube, total correcto, sin nueva captura/OCR (logcat sin `ReconocerTexto`, mismo `texto_crudo`). Guardar y comprobar `motor=NUBE` en el ticket (`run-as`/listado).
  2. Sentido inverso: resultado Nube → "Probar con Local" → badge Local procesando ~30 s → resultado; **cancelar a mitad** y comprobar que vuelve al resultado anterior sin colgar la app ni dejar el modelo cargado (aprovecha la cancelación física pendiente de procesado-ui).
  3. Error: forzar fallo de nube (sin red/cuota) → error con badge Nube FALLIDO → "Probar con Local" funciona; "Reintentar" repite con Nube.
  4. Disabled: con modelo eliminado (Ajustes) desde un resultado Nube, el botón se ve deshabilitado con "Descarga el modelo local…". (En realme GT 2, si hay cuota: Local deshabilitado por `NO_CAPAZ`/`SIN_MODELO`; opcional.)

## Evidence To Capture
JUnit XML (router, use cases, VM); `am instrument` de `ProcesadoFlujoTest`; capturas (resultado con botón, error con tres salidas, deshabilitado con motivo, badge tras reintento) y logcat de la prueba física con motor, `received`/tiempos y ausencia de OCR; `motor` del ticket guardado tras reintento.

## Decisiones con DEFAULT (no bloqueantes)
- **D1 Qué es "otro motor"**: ¬(motor que produjo el resultado/error visible), no ¬(política del router). Evita ambigüedad con `ajustes-motor`.
- **D2 Disponibilidad Local = `capaz && modelo`** (misma que Auto), aunque un usuario podría forzar Local sin RAM; el forzado explícito pertenece a `ajustes-motor`. Evita OOM al pulsar un botón. Nube siempre disponible (D2 de router: la red no es entrada).
- **D3 Botón deshabilitado con motivo** en vez de oculto: explica por qué no hay alternativa (descubribilidad del modelo local).
- **D4 Se conserva el resultado anterior solo mientras dura el reintento** (cancelar/fallar vuelve a él); el éxito lo reemplaza; no hay comparador ni historial. Razón: reintentar local cuesta 26-41 s/cuota; perder un buen resultado por un reintento cancelado sería hostil.
- **D5 "Reintentar" mismo motor** pasa a usar el motor del error (no la política), coherente con ADR 0001.
- **D6 Sin reintento desde la revisión editable** (perder ediciones).
- **D7 `anterior` viaja en la orden de extracción** (no en estado mutable paralelo) para mantener la cadena de operadores.

**Decisión de producto visible (confirmar; no bloquea):** D2 (Local solo si capaz+modelo; el usuario no puede forzarlo desde aquí), D3 (botón deshabilitado visible con motivo) y D4 (conservar R1 durante el reintento, con "Volver al resultado anterior" en el error). Si se prefiere simplificar, D4 puede recortarse a "el reintento reemplaza el resultado y cancelar vuelve a la pantalla 05" eliminando `anterior`, `Restaurar` y el botón extra. **No se crea módulo Gradle ni dependencia.** Cambios de API internos: constructor de `EscanearViewModel` y campos de `CapturaUiState` (solo tests lo usan).

## Riesgos y preguntas abiertas
- **R-RO1** Cuota de Gemini agotada puede impedir la mitad de la prueba física (Nube); documentar sin inventar resultados.
- **R-RO2** Carga/cancelación del modelo local al encadenar Local cancelado → Nube/Local inmediato: `Mutex` serializa, pero verificar físicamente liberación de memoria.
- **R-RO3** Cambio de comportamiento de "Reintentar" (D5) y del origen del badge (S8): cubrir con tests VM; el badge del primer intento puede diferir del `extraido.motor` final solo si cambia el entorno durante la extracción (el badge final es siempre `extraido.motor`).
- **R-RO4** Sin reintento desde `RevisionDatos`: el usuario que descubre un mal resultado ya en la revisión debe volver al resultado (back) y reintentar; se pierden ediciones por diseño.
- **R-RO5** Fiabilidad de `NO_CAPAZ` (RAM ≥10 GB, R-RT2 abierto): el realme GT 2 sigue sin medir.
- **R-RO6** Test inestable ajeno `EscanearViewModelTest.capturingProcessesAndSavesThePhotoAtCaptureTime`; no confundir con regresiones.

## Validator Checklist
- [ ] Alcance: solo reintento explícito; sin fallback, sin ajustes, sin cambios de Room/OCR/revisión.
- [ ] Acción presente en resultado y error; disponibilidad y motivo correctos (S3); Local no se invoca si no es `DISPONIBLE`.
- [ ] El motor recibe el mismo `textoCrudo`; ningún OCR/foto nuevo (JVM y logcat físico).
- [ ] Badge procesando/fallido/final = motor real del intento (desde el estado); `Ticket.motor` guardado = motor del resultado aceptado.
- [ ] Cancelación: restaura R1 en reintento desde resultado, pantalla 05 en los demás; `CancellationException` jamás es error; sin dos extracciones simultáneas.
- [ ] `EscanearViewModelTest`, dominio, `:test:unit` y E2E `:app` en verde; tags previos intactos; arquitectura (domain puro, framework `internal`, un `di.kt`, sin módulos nuevos/claves/versiones literales).
- [ ] Evidencia física real en Tab S9+ (ambos sentidos o limitación de cuota documentada) y capturas revisadas.

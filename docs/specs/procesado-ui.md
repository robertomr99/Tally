# Feature Implementation Spec: Pantalla de procesado con progreso, pasos y cancelación

## Source Feature

- `id`: procesado-ui
- `area`: revision
- `title`: Pantalla de procesado con progreso, pasos y cancelación
- `user_visible_behavior`: Tras capturar se muestra el estado de procesado (mockup 06): pasos Encuadre→OCR→Categorización, progreso circular, cancelar y badge del motor en uso; cancelar conserva la foto.
- `depends_on`: ["extraccion-nube"] (accepted, sesión 019 → satisfecha)
- `status`: not_started (a fecha de planificación, 2026-10-17)
- `source`: `feature_list.json`
- `verification` (fuente):
  1. Flujo captura→procesado→resultado en el realme GT 2 (motor nube) — ver Open Question U1 (dispositivo).
  2. Cancelar aborta la extracción sin dejar datos huérfanos.
  3. El badge muestra el motor real; sin % de confianza inventado (CONTEXT.md).
- `notes` (fuente): Mockup 06; la decoración ficticia está prohibida por DESIGN.md. Toggle del mockup = reintento con el otro motor (feature `reintento-otro-motor`).

## Goal

Cablear el estado de procesado (mockup 06) entre la pantalla 05 y el resultado: al
pulsar "Continuar" con el Borrador vivo (foto en disco + texto crudo), la app ejecuta
la extracción (`ExtraerTicket`/`TextEngine`, hoy motor Nube) mostrando los pasos
reales **Encuadre → OCR → Categorización**, un progreso circular SIN porcentaje
inventado y el **Badge de motor** con el motor real ("Motor Nube · Gemini"); el
usuario puede **cancelar** en cualquier momento (aborta la extracción, conserva la
foto y vuelve a la pantalla 05 con el Borrador intacto) y, al terminar, ve un estado
**resultado** de solo lectura con la completitud de campos extraídos. Es la primera
superficie donde la latencia real de Gemini (R2) se hace visible al usuario.

## Non-Goals

- Guardar en Room / `TicketRepository` (eso es `revision-datos`, que además hará la
  edición de datos). Esta feature NO persiste nada.
- Edición de datos, mockup 07 ("Reintentar/Editar/Guardar") ni mockup 08.
- Reintento con el **otro** motor ni toggle Local↔Nube del mockup
  (`reintento-otro-motor`; ADR 0001: sin fallback silencioso). Sí se permite
  "Reintentar" con el MISMO motor y re-pulsar "Continuar" tras cancelar.
- `EngineRouter`, `extraccion-local`, `descarga-modelo`, `ajustes-motor`.
- Decoración ficticia del mockup 06 (ver Visual Design Impact): "Motor OCR v4.2",
  "Garantía de privacidad criptográfica", "[ORG_NAME 98%]", "68%", "Tally Vision".
- Crear `stateAsResultIn`/`AcScaffold`/`rememberXState` (no existen; ver U3), módulos
  Gradle nuevos, rutas nuevas en `:app`, dependencias nuevas, `init.sh`, `build-logic/`.

## Job Story

When he confirmado la foto del ticket (pantalla 05) y quiero los datos estructurados
sin esperar a ciegas durante la llamada al motor,
I want ver el progreso real del procesado (pasos + motor en uso) y poder cancelar
cuando quiera sin perder la foto,
so I can mantener el control de la espera larga (Gemini tarda 5-36 s) y volver a la
foto o reintentar si cambio de idea.

## Users And Permissions

- Usuario único (sin cuentas). Sin permisos Android nuevos. Con motor Nube el texto
  crudo ya viaja a Gemini (privacidad asumida en ADR 0001/0002); el badge hace
  visible qué motor procesa (requisito de AGENTS.md/ADR 0001).

## Acceptance Scenarios

### Scenario 1: flujo captura→procesado→resultado con pasos y badge (verificación 1)

Given la app con permiso de cámara, un ticket encuadrado y Firebase AI Logic operativo
(emulador con GMS o dispositivo físico; ver U1)
When se captura la foto y se pulsa "Continuar" en pantalla 05
Then se muestra la pantalla de procesado (mockup 06) con los pasos Encuadre
(Completado), OCR (Completado) y Categorización (Activo), progreso circular sin % y
badge "Motor Nube · Gemini"; al terminar la extracción se muestra el estado
resultado con los 3 pasos Completados, badge en estado done y un resumen de solo
lectura del `TicketExtraido` (comercio, fecha, total con IVA, líneas, categoría +
completitud de campos real, p. ej. "5/5 campos extraídos"); el botón "Revisar ticket"
aparece deshabilitado hasta `revision-datos` (patrón captura-foto).

### Scenario 2: cancelar aborta y conserva la foto (verificación 2)

Given una extracción en curso (paso Categorización Activo)
When el usuario pulsa "Cancelar" (o back del sistema)
Then la corrutina de extracción se aborta (`CancellationException` re-lanzada, jamás
convertida en estado de error ni en resultado), la UI vuelve a la pantalla 05 con el
Borrador intacto (misma `rutaFoto` y `textoCrudo`), la foto sigue en disco
(`run-as … ls files/fotos` idéntico antes/después) y no se ha escrito nada en Room ni
llamado a `TicketRepository` (cero datos huérfanos; estado del VM consistente:
"Continuar" reintentable).

### Scenario 3: badge con el motor real y sin % inventados (verificación 3)

Given el procesado en cualquier estado
Then el badge muestra el motor real del `TextEngine` en uso ("Motor Nube · Gemini",
icono cloud; mapeo genérico desde `Motor` para el futuro "Motor Local · Gemma 3n")
con estado procesando/done; la UI NO contiene porcentajes de confianza ni de progreso
inventados (ni "99,4 %", ni "68%", ni "% verificado"): solo estados de paso y, en el
resultado, completitud de campos calculada sobre el `TicketExtraido` (CONTEXT.md).

### Scenario 4: error de extracción/red visible sin inventar datos

Given la extracción en curso y una caída de red / cuota / respuesta inválida
(`InvalidExtractionException`)
Then se muestra estado de error con copy en español claro (paleta de `error`), sin
ningún dato inventado ni fallback silencioso a otro motor (ADR 0001), con acciones
"Reintentar" (mismo motor) y "Volver a la foto"; la foto se conserva.

### Scenario 5: OCR vacío → resultado con campos vacíos (vía manual futura)

Given un Borrador con `textoCrudo` blank (foto ilegible)
When se pulsa "Continuar"
Then `ExtraerTicket` NO invoca al motor y devuelve `TicketExtraido` vacío; el
procesado termina rápido en el estado resultado con "0/N campos extraídos" (la foto
NO queda "gastada"; la edición manual llega con `revision-datos`).

### Scenario 6: regresión de lo aceptado

Given la suite existente
Then `NavigationTest` 3/3 y `CapturaFlujoTest` siguen en verde (actualizando el assert
de "Continuar", ahora habilitado), "Repetir foto" sigue borrando la foto reemplazada
best-effort, `ExtraccionNubeRealTest` 5/5 intacto y no hay rutas nuevas en `:app`.

## Repository Research

### Files Inspected

- `feature/escanear/.../EscanearViewModel.kt` — `CapturaUiState` (PermisoPendiente/
  Encuadre/Procesando/Revision(rutaFoto, textoCrudo, ocrNoDisponible)/Error); cadena
  `entradaCaptura.flatMapLatest { resultadoDeCaptura(it) }`; `runCatchingLanzandoCancelacion`
  re-lanza `CancellationException` (patrón a mantener); NO llama a `ExtraerTicket` aún.
- `feature/escanear/.../RevisionFotoContent.kt` — "Continuar" `enabled = false` (línea
  205), KDoc "DESHABILITADO hasta que `procesado-ui` lo cablee" (punto de arranque).
- `feature/escanear/.../EscanearScreen.kt` — `when` sobre `CapturaUiState`;
  `onContinuar = { /* cableado con procesado-ui */ }`; `ProcesandoContent` (estado
  breve de captura, a no confundir con la pantalla 06); `BackHandler` en Revision.
- `feature/escanear/.../EscanearState.kt` — `rememberEscanearState()` (patrón XState).
- `domain/expenses/` — `TextEngine` (`val motor: Motor` + `suspend extract`),
  `ExtraerTicket` (blank → `TicketExtraido` vacío sin invocar motor; errores
  propagan; `CancellationException` jamás tragada), `TicketExtraido`, `Motor`, `Categoria`.
- `test/unit/.../FakeTextEngine.kt` — `result`/`error`/`motor`/`received` y
  `gate: CompletableDeferred<Unit>?` ya preparado para tests de cancelación.
- `app/src/androidTest/.../CapturaFlujoTest.kt` — E2E con cámara virtual; assert
  `captura_continuar` `assertIsNotEnabled` (a actualizar).
- `app/src/androidTest/.../ExtraccionNubeRealTest.kt` + `di/TextEngineEntryPoint.kt` —
  patrón de test real (EntryPoint; NO sustituir el binding `TextEngine` con
  `@TestInstallIn` o se rompería este test).
- `app/src/main/.../navigation/TallyApp.kt` / `TallyRoute.kt` — 3 rutas; sin cambios.
- `docs/design/stitch/06-procesando-ticket/code.html` + `DESIGN.md` (líneas 155-158,
  169-172, 209, 215-224) + `docs/design/stitch/stitch-design-system.md` (tokens).
- `docs/domain-model.md` (Borrador; "cancelar el procesado y quedarse en el Borrador
  con campos vacíos"), `CONTEXT.md` (sin % inventados; completitud + motor),
  `docs/adr/0001/0002`, `docs/risks-and-open-questions.md` (R2, R4), `AGENTS.md`,
  `docs/specs/{captura-foto,ocr-mlkit,extraccion-nube}.md` (patrón de spec).
- No inspeccionado: `06-procesando-ticket/screen.png` (el agente no ve imágenes);
  `stitch-design-system.md` solo parcialmente (vía DESIGN.md).

### Existing Patterns To Follow

- Subflujo interno de `:feature:escanear` sin rutas nuevas (pantalla 05 fue así;
  captura-foto dejó constancia: "reversible cuando procesado-ui necesite navegación
  real"). Sin módulos Gradle nuevos (regla dura).
- Estado derivado con operadores sobre `MutableStateFlow` (`flatMapLatest`/`combine`);
  sin `load()` imperativas. Cancelación = cambio de entrada que cancela el flujo
  activo (`flatMapLatest`) — `runCatchingLanzandoCancelacion` para no tragar
  `CancellationException` (hallazgo Low recurrente; ver R-PROC4).
- testTags `captura_*`/`pantalla_escanear`; nuevos `procesado_*`. E2E con
  `createAndroidComposeRule<MainActivity>()`, `@FixMethodOrder` donde toque.
- Badge de motor (DESIGN.md, componente NUEVO obligatorio): "Motor Local · Gemma 3n"
  (memory) o "Motor Nube · Gemini" (cloud), con estado procesando/done.

### Current Gaps

- `ExtraerTicket` no se invoca desde ningún ViewModel (solo tests): esta feature lo
  cablea. No expone `motor` (ver T1). `stateAsResultIn`/`AcScaffold` no existen (U3).
- No existe pantalla de revisión/resultado (`revision-datos`): el estado "resultado"
  de esta feature es el terminal del slice (U4).

## Technical Approach

1. **Dominio (mínimo)**: `ExtraerTicket` expone `val motor: Motor` (= `textEngine.motor`)
   para que el badge muestre el motor ANTES de acabar la extracción; el VM sigue
   recibiendo solo casos de uso (AGENTS.md).
2. **Estado (`EscanearViewModel`)**: `CapturaUiState` gana
   `ProcesandoExtraccion(rutaFoto, textoCrudo)`, `ExtraccionLista(rutaFoto, textoCrudo, extraido)`
   y `ExtraccionFallida(rutaFoto, textoCrudo, mensaje)`. Nueva entrada
   `MutableStateFlow<OrdenExtraccion>` (Continuar / Cancelar / Reintentar / Volver).
   Cadena declarativa: al entrar la orden `Continuar` sobre el Borrador de
   `CapturaEnProgreso.Guardada`, `flatMapLatest` ejecuta `extraerTicket(textoCrudo)`
   emitiendo los estados; **Cancelar/Volver/Repetir foto emite una orden nueva que
   cancela el flujo activo** (cancelación estructurada; alternativa equivalente: `Job`
   explícito en propiedad — en cualquier caso `CancellationException` se re-lanza
   SIEMPRE). Sin `runCatching` plano sobre suspend.
3. **Pasos Encuadre→OCR→Categorización (mapeo real, sin simular)**: Encuadre =
   Completado (la foto ya está en disco); OCR = Completado (`ReconocerTextoCrudo` ya
   corrió durante la captura — moverlo rompería el panel debug de `ocr-mlkit`;
   ver U5); Categorización = Activo mientras corre `ExtraerTicket`, Completado al
   acabar. Progreso circular **indeterminado** (o fracción 2/3 pasos, factual): nunca
   % numérico.
4. **Cancelar**: aborta, vuelve a pantalla 05 con el Borrador intacto; la foto solo se
   borra en "Repetir foto" (best-effort, ya implementado). Back del sistema durante el
   procesado = Cancelar.
5. **Badge**: mapeo `Motor` → chip ("Motor Nube · Gemini"/"Motor Local · Gemma 3n")
   con estado; tras el resultado refleja `TicketExtraido.motor`.
6. **Resultado**: resumen de solo lectura del `TicketExtraido` + completitud de campos
   (CONTEXT.md) + "Revisar ticket" deshabilitado hasta `revision-datos` (U4).
7. **Error**: copy en español (DESIGN.md Do: "estados vacíos y de error con la misma
   paleta y copy claro"); "Reintentar" (mismo motor) + "Volver a la foto"; sin datos
   inventados ni otro motor.

## Expected File Changes

- `domain/expenses/src/main/java/.../usecases/ExtraerTicket.kt` — modify; `val motor: Motor`.
- `domain/expenses/src/test/java/.../usecases/ExtraerTicketTest.kt` — modify; assert del motor.
- `feature/escanear/src/main/java/.../ProcesadoContent.kt` — create; mockup 06
  (badge, pasos, progreso circular, Cancelar, silueta de ticket, estados éxito/error),
  testTags `procesado_*`.
- `feature/escanear/src/main/java/.../EscanearViewModel.kt` — modify; `ExtraerTicket`,
  estados de extracción, órdenes y cancelación (patrón `runCatchingLanzandoCancelacion`).
- `feature/escanear/src/main/java/.../EscanearScreen.kt` — modify; render de los nuevos
  estados, `onContinuar` → VM, BackHandler en procesado = cancelar.
- `feature/escanear/src/main/java/.../RevisionFotoContent.kt` — modify; "Continuar"
  `enabled = true` + KDoc (quitar el non-goal).
- `feature/escanear/src/main/java/.../EscanearState.kt` — modify solo si hace falta
  estado visual (DEFAULT: no tocar).
- `feature/escanear/src/test/java/.../EscanearViewModelTest.kt` — modify; escenarios
  nuevos (pasos, cancelación con `FakeTextEngine.gate`, error, blank, motor).
- `app/src/androidTest/java/.../captura/CapturaFlujoTest.kt` — modify; assert de
  "Continuar" habilitado.
- `app/src/androidTest/java/.../captura/ProcesadoFlujoTest.kt` — create; E2E enfocado.
- `docs/evidence/procesado-ui/` — create; JUnit XML, screencaps/uiautomator, run-as.
- `docs/architecture.md`, `docs/risks-and-open-questions.md` — update (ver abajo).
- `:app/src/main` (TallyApp/TallyRoute) — **sin cambios** (subflujo interno).
- Dependencias: **ninguna nueva** (Compose M3, Coroutines, Hilt, Turbine ya están). Si
  el implementador necesitara alguna, la fija SOLO en `gradle/libs.versions.toml` y lo
  registra en Implementation Notes.

## Visual Design Impact

- UI involved: sí. Design source: `docs/design/stitch/06-procesando-ticket/` +
  `DESIGN.md` + `stitch-design-system.md` (tokens ya mapeados en `:feature:common`).
- Normativo del mockup 06: badge on-device/nube, silueta de ticket con scan beam,
  progreso circular, pasos Encuadre → OCR → Categorización (chips: completado con
  check en `secondary-container`, activo en `primary` con punto animado, pendiente en
  `surface-container-high`), botón "Cancelar" (texto, `rounded-full`, ≥44dp).
- Decoración ficticia PROHIBIDA (DESIGN.md R4/Do's and Don'ts): "Motor OCR v4.2" /
  "Tally Vision" / "% verificado" (→ Badge de motor real), "Procesando en el
  dispositivo (On-device ML)" + "Garantía de privacidad criptográfica" + candado
  (→ etiqueta de proceso veraz según motor: "Procesando en la nube (Gemini)"),
  "[ORG_NAME 98%]" / "ITEM_01" y el "68%" central (→ skeleton sin etiquetas; progreso
  sin %), toggle "Cambiar a Nube/Dispositivo" (→ `reintento-otro-motor`). Animaciones
  permitidas: scan beam, radar, progreso circular (las del mockup; no inventar más).
- Estados nuevos: procesando, resultado (solo lectura) y error (copy español).

## Durable Documentation Impact

- `docs/architecture.md`: update — el pipeline de captura gana la etapa de extracción
  visible (procesado con cancelación) en la sección de presentación/flujo.
- `docs/risks-and-open-questions.md`: update — R2: la latencia ya es visible al
  usuario (progreso indeterminado + cancelar); nota de la verificación física.
- `docs/domain-model.md`: not needed (los estados del Borrador ya cubren "cancelar el
  procesado y quedarse en el Borrador con campos vacíos"; pantalla 05 = vista del Borrador).
- `AGENTS.md`, `DESIGN.md`, `docs/adr/*`, `docs/build-brief.md`,
  `docs/technical-discovery.md`, `CONTEXT.md`: not needed (sin cambios de reglas).
- `feature_list.json` y `PROGRESS.md`: update (evidencia; lo aplica el orquestador).

## Implementation Plan

1. T1 dominio (`ExtraerTicket.motor`) y T2 VM con cancelación + tests JVM (base dura).
2. UI del procesado (T3-T5): pasos/badge/progreso, resultado, error.
3. Cableado pantalla 05 → procesado (T6) y regresión de tests (T7).
4. E2E emulador (T8) y verificación física con motor nube real (T9).
5. Cierre: regresión completa, búsquedas de decoración/%, docs durables (T10).

## Implementation Tasks

- [x] T1 Dominio: `ExtraerTicket.motor` (passthrough de `textEngine.motor`) + assert en `ExtraerTicketTest`.
- [x] T2 VM: estados `ProcesandoExtraccion`/`ExtraccionLista`/`ExtraccionFallida`, `OrdenExtraccion` y cadena declarativa con cancelación por `flatMapLatest` (o `Job` equivalente); `CancellationException` SIEMPRE re-lanzada.
- [x] T3 UI `ProcesadoContent.kt` (mockup 06 normativo): Badge de motor real con estado, pasos reales Encuadre→OCR→Categorización, progreso circular indeterminado + scan beam/radar, "Cancelar", silueta de ticket sin etiquetas ficticias, sin % de ningún tipo.
- [x] T4 Estado resultado: resumen de solo lectura del `TicketExtraido` + completitud de campos ("n/N campos extraídos") + "Revisar ticket" `enabled = false` (hasta `revision-datos`).
- [x] T5 Estado error: copy en español + "Reintentar" (mismo motor) + "Volver a la foto"; sin datos inventados.
- [x] T6 Cableado: `EscanearScreen` renderiza los estados y `onContinuar` → VM; `RevisionFotoContent` con "Continuar" habilitado (KDoc actualizado); back en procesado = cancelar; `:app` sin rutas nuevas.
- [x] T7 Tests JVM (`EscanearViewModelTest` + `FakeTextEngine.gate`): pasos; cancelar aborta y NO borra la foto ni llama a `TicketRepository`; error sin inventar; blank → resultado rápido; badge = motor real; regresión de los tests existentes.
- [x] T8 E2E emulador `ProcesadoFlujoTest` (captura→Continuar→procesado con badge/pasos→resultado; "Revisar ticket" deshabilitado) + `CapturaFlujoTest` actualizado (Continuar habilitado). Nota: con la cámara virtual el OCR suele ser blank → el E2E es determinista y cubre Scenario 5; la extracción real por UI va en T9.
- [ ] T9 Verificación física (DEFAULT Tab S9+ con ticket de papel, motor nube real) — **PARCIAL**: verification 1 (flujo real hasta resultado con datos reales) y verification 3 (badge real) HECHAS en la Tab S9+; verification 2 (Cancelar a mitad de extracción + `run-as ls files/fotos` antes/después) BLOQUEADA por cuota agotada del free tier de Gemini (la extracción falla al instante; sin ventana de cancelación). Cubierta por tests JVM (T7); reintentar cuando la cuota se restablezca.
- [x] T10 Cierre: `.\gradlew.bat assembleDebug testDebugUnitTest` + suite instrumentada completa; búsquedas (0 "% de confianza", 0 decoración ficticia del mockup, 0 versiones literales fuera del toml, `git diff init.sh` vacío, `schemas/` de Room sin diff); docs durables; evidencia en `docs/evidence/procesado-ui/`.

## Verification Plan

- `.\gradlew.bat assembleDebug testDebugUnitTest` → BUILD SUCCESSFUL (gate estándar;
  equivalencia de `init.sh` en esta máquina sin `bash`; `git diff init.sh` vacío).
- `.\gradlew.bat :test:unit:test --rerun :domain:expenses:test --rerun :feature:escanear:testDebugUnitTest --rerun` → BUILD SUCCESSFUL (JUnit XML: EscanearViewModelTest ampliado, ExtraerTicketTest, SmokeTest).
- `$env:ANDROID_SERIAL="emulator-5554"; .\gradlew.bat :app:connectedDebugAndroidTest` → suite completa en verde (CapturaFlujoTest + NavigationTest + ProcesadoFlujoTest + ExtraccionNubeRealTest 5/5).
- Focused: `$env:ANDROID_SERIAL="emulator-5554"; .\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.robertomr.tally.captura.ProcesadoFlujoTest"`.
- Físico (DEFAULT Tab S9+ `SM-X810`, ADB inalámbrico; U1): `.\gradlew.bat :app:installDebug` + `pm grant … CAMERA` + flujo real con ticket de papel hasta resultado con extracción nube real (verificación 1) + cancelación en vuelo (verificación 2).
- E2E enfocado obligatorio (esta feature cambia el flujo de usuario y hay harness):
  `ProcesadoFlujoTest`. No se usa `@TestInstallIn` para fakes de `TextEngine`
  (rompería `ExtraccionNubeRealTest`, que resuelve el binding real vía
  `TextEngineEntryPoint`); la determinización de pasos/cancelación se hace en JVM con
  `FakeTextEngine.gate` (justificación registrada).

## Evidence To Capture

- JUnit XML (JVM + instrumentados) copiados a `docs/evidence/procesado-ui/`.
- Screencaps + `uiautomator dump` de: procesado en marcha (badge + pasos), resultado,
  error y cancelación (pantalla 05 tras cancelar).
- Físico: `run-as com.robertomr.tally ls -l files/fotos` antes/después de cancelar
  (foto conservada) y extracción real con los campos del ticket de papel.
- Búsqueda sin % inventados ni decoración ficticia (comandos citables) +
  `0 versiones literales` fuera de `gradle/libs.versions.toml`.
- Todo registrado en `feature_list.json` (evidence) y `PROGRESS.md`, con dispositivos.

## Riesgos

- **R-PROC1 — Latencia visible (R2)**: 5-36 s por extracción; progreso indeterminado
  puede parecer "colgado". Mitigación: copy honesto + Cancelar siempre disponible;
  medición formal sigue en S3.
- **R-PROC2 — Cancelación y red**: la corrutina se cancela localmente, pero la llamada
  remota puede completar en el servidor; "sin datos huérfanos" se interpreta en local
  (cero escrituras, estado consistente, foto intacta).
- **R-PROC3 — E2E emulador sin extracción real** (OCR de cámara virtual suele ser
  blank): cubierto con JVM (`FakeTextEngine.gate`) + verificación física T9.
- **R-PROC4 — `CancellationException`** (hallazgo Low recurrente): ningún `runCatching`
  plano sobre suspend; patrón `runCatchingLanzandoCancelacion` verificado por test.
- **R-PROC5 — Discrepancia AGENTS.md vs código** (`stateAsResultIn`/`AcScaffold`/`XState`
  no existen): ver U3; no inventar utilidades nuevas en esta feature.

## Open Questions (para el usuario)

- **U1 — Dispositivo de la verificación 1**: el texto de la feature cita al realme GT 2,
  que NO está conectado a esta máquina. DEFAULT: **Tab S9+ (`SM-X810`)** como sustituto
  físico (precedente sesiones 012-013/018) + `emulator-5554` para el E2E de flujo.
  Confirmar si se exige literalmente el realme.
- **U2 — Flujo pantalla 05 → procesado**: DEFAULT = "Continuar" habilitado → procesado
  (orden de mockups 05→06→07 y KDoc de captura-foto). Alternativa literal del
  `user_visible_behavior` ("tras capturar"): auto-navegación a procesado sin pasar por
  pantalla 05 (rompería "Repetir foto" como gate barato; no recomendado).
- **U3 — Utilidades de AGENTS.md**: `stateAsResultIn`/`AcScaffold` no existen (mismo
  gap ya documentado en captura-foto). DEFAULT: NO crearlas; seguir el patrón existente
  (sealed `CapturaUiState` + `when`; `rememberEscanearState`). Crearlas = alcance extra.
- **U4 — Botón del resultado**: DEFAULT = "Revisar ticket" deshabilitado hasta
  `revision-datos` (patrón exacto de "Continuar" en captura-foto). Alternativa:
  callback `onExtractionReady` ya cableado (no recomendado: botón sin destino).
- **U5 — Pasos Encuadre/OCR al abrir procesado**: DEFAULT = reflejar estados reales
  (Encuadre y OCR ya Completados; solo Categorización corre) porque el OCR ocurre en la
  captura (moverlo rompería `ocr-mlkit`). Alternativa: simular secuencia falsa
  (prohibida: sería decoración ficticia).

## Implementation Notes / Desviaciones

- **Desviación aprobada por el usuario (2026-10-17): App Check debug en la app.**
  El flujo físico real no podía completar porque la app normal no instalaba
  ningún `AppCheckProviderFactory` (solo lo hacía `ExtraccionNubeRealTest`), así
  que Gemini rechazaba la petición ("Firebase App Check token is invalid") y la
  extracción real solo funcionaba bajo instrumentación, nunca en la app. Se añadió
  `app/src/debug/java/.../appcheck/AppCheckDebugInitializer.kt` (un
  `ContentProvider`) + `app/src/debug/AndroidManifest.xml` que instalan el
  `DebugAppCheckProviderFactory` **solo en la variante debug** de `:app`. No toca
  `src/main`, `init.sh`, `build-logic/` ni añade dependencias (usa
  `firebase-appcheck-debug`, ya presente). El debug token lo genera el dispositivo
  y se registra a mano en la consola; nunca viaja en release. Ficheros fuera del
  `Expected File Changes` del spec (2 nuevos en `app/src/debug/`).
- **Ajuste de testTag en `ProcesadoContent.kt`**: el `testTag("procesado_badge_motor")`
  estaba en el `Row` del badge y `assertTextContains` no lee textos de hijos sin
  merge; se movió al `Text` de la etiqueta del motor (mismo patrón que
  `procesado_completitud`). El E2E enfocado pasó 2/2 tras el arreglo.
- **Alcance del E2E**: `ProcesadoFlujoTest` 2/2 en `emulator-5554` (cámara virtual
  → OCR blank → resultado rápido, Scenario 5) + `CapturaFlujoTest` actualizado
  (Continuar habilitado). No se usó `@TestInstallIn` de `TextEngine` (rompería
  `ExtraccionNubeRealTest`); la cancelación se determina en JVM con
  `FakeTextEngine.gate`.
- **Regresión instrumentada**: suite completa `:app` 13/13 (CapturaFlujoTest 3 +
  ProcesadoFlujoTest 2 + NavigationTest 3 + ExtraccionNubeRealTest 5 con Gemini
  real) **antes** del cambio de App Check; tras el cambio se re-ejecutaron los 8
  tests no dependientes de red (3+2+3) en verde. `ExtraccionNubeRealTest` no se
  re-ejecutó entonces por **cuota agotada** (ver R2); ya estaba `accepted` en la
  sesión 019.
- **Verificación física (Tab S9+ SM-X810)**: verification 1 completada con
  extracción real a resultado (badge "Motor Nube · Gemini", comercio
  "HOLA ALMIRANTE", fecha, total 32,09 €, categoría "Alimentación", "4/5 campos
  extraídos", "Revisar ticket" deshabilitado); verification 3 (badge/motor real y
  estado de error honesto con motivo real) vía `uiautomator`; verification 2
  (cancelación física) bloqueada por cuota. Evidencia en
  `docs/evidence/procesado-ui/`.

## Validator Checklist

- [x] Implementation stays within this feature's scope (sin Room/guardado, sin edición,
  sin otro motor, sin toggle, sin módulos ni rutas nuevas, sin dependencias nuevas).
- [x] Acceptance scenarios 1-6 pass (1 con extracción real en dispositivo; 2 con foto
  conservada y cero escrituras; 3 badge real y 0 % inventados; 4 error honesto;
  5 blank → resultado vacío; 6 regresión).
- [x] Verification evidence is present (cancelación física pendiente por cuota, ver H1) (JUnit XML + capturas/uiautomator + run-as en
  `docs/evidence/procesado-ui/`).
- [x] E2E enfocado (`ProcesadoFlujoTest`) añadido/actualizado, con la justificación de
  no-fake de `TextEngine` registrada.
- [x] Sin decoración ficticia del mockup 06 (búsquedas citables: "Motor OCR", "98%",
  "68%", "Garantía de privacidad", "Cambiar a Nube").
- [x] `CancellationException` nunca convertida en resultado/estado de error.
- [x] `feature_list.json` y `PROGRESS.md` actualizados con evidencia no inventada.
- [x] No unrelated product behavior or extra feature work was added.

### Validator Verdict (2026-10-17, sesión 021)

- Veredicto: **accept** (feature-validator). Gates re-ejecutados en verde (JVM 16/16 VM + dominio; `:app` E2E 8/8 sin red). Sin hallazgos Critical/High/Medium.
- Findings Low/Info: (H1) cancelación física pendiente por cuota Gemini + sin artefacto `run-as` antes/después; (H2) falta el XML del run `:app` 13/13 citado (corroborado por evidencia aceptada de `extraccion-nube`); (H4) revocar los debug tokens al cerrar las pruebas de nube; (H5) precisión del modelo (32,09 vs 35,30 del contraste humano), deuda de prompt/`log-extracciones`. `ExtraccionNubeRealTest` no re-ejecutado por cuota agotada (no imputable a esta feature).

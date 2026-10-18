# Feature Implementation Spec: Router automático de motor con indicador visible

## Source Feature

- `id`: router-automatico
- `area`: ia
- `title`: Router automático de motor con indicador visible
- `user_visible_behavior`: La app elige motor sola (Local si el dispositivo puede y el modelo está descargado; si no Nube); el motor queda registrado con la extracción y visible (badge en procesado y ticket).
- `depends_on`: ["extraccion-local", "extraccion-nube"] (ambas `accepted`; `descarga-modelo` también `accepted` y aporta `rutaModelo()`)
- `status`: not_started (planificada 2026-10-18)
- `verification` (fuente): (1) en directo, Tab S9+ usa Local y realme GT 2 usa Nube; (2) unit tests del router con combinaciones capacidad/modelo/override; (3) el badge refleja el motor realmente usado en cada extracción.
- `notes` (fuente): ADR 0001. Fallback automático silencioso no existe: el reintento es acción explícita del usuario.

## Goal

Introducir `EngineRouter` (dominio, Kotlin puro) como el `TextEngine` **sin calificar** del grafo Hilt: en cada extracción elige entre `GemmaLocalEngine` y `GeminiCloudEngine` con una política pura y testeada, y delega. El motor elegido queda en `TicketExtraido.motor` (ya se persiste en `Ticket.motor` y ya lo pintan los badges de procesado y revisión). **Sin cambios de UI ni de VM**: `ExtraerTicket.motor` ya es passthrough de `TextEngine.motor`.

## Non-Goals

- **`reintento-otro-motor`**: ni botón, ni `ExtraerTicket(texto, motor)`, ni reuso explícito del texto crudo con otro motor. El "Reintentar" de procesado-ui sigue siendo "mismo motor" (re-evalúa la política; con el mismo entorno elige el mismo).
- **`ajustes-motor`**: ni selector Auto/Local/Nube, ni persistencia (DataStore/Room), ni pantalla. Aquí solo existe el **puerto** de preferencia con valor fijo `AUTO`.
- Fallback automático ante fallo (ADR 0001): si el motor elegido falla, el error se propaga tal cual (`ExtraccionFallida`), nunca se prueba el otro motor.
- Detección de red como entrada de la política (ver D2), caché del modelo cargado (D2 de extraccion-local), mejora de calidad del motor local, módulos Gradle nuevos, Retrofit/OkHttp, claves en el APK, cambios en `AGENTS.md`/ADRs/`init.sh`/`build-logic/`/esquema Room.

## Job Story

When pulso Continuar sobre un ticket ya capturado,
I want que la app use sola el motor adecuado (on-device si mi dispositivo lo soporta y descargué el modelo; si no, nube) y me lo muestre,
so I can saber dónde se procesó mi ticket sin configurar nada.

## Acceptance Scenarios

### Scenario 1: política Auto (verificación 2)
Given `PreferenciaMotor.AUTO`
Then `elegirMotor(AUTO, capaz, modeloDisponible)` = LOCAL **solo** si `capaz && modeloDisponible`; en cualquier otro caso (no capaz; capaz sin modelo; ni lo uno ni lo otro) = NUBE. Tabla de 4 combinaciones cubierta en JVM.

### Scenario 2: override (puerto para `ajustes-motor`)
Given `PreferenciaMotor.NUBE` → siempre NUBE (aunque sea capaz y tenga modelo).
Given `PreferenciaMotor.LOCAL` → siempre LOCAL, **sin comprobar capacidad ni modelo**: si falta el modelo, `GemmaLocalEngine` lanza `ModeloLocalNoDisponibleException` y se propaga (no se cae a Nube). Las 3 preferencias × 4 combinaciones están en el test.

### Scenario 3: delegación y registro del motor (verificación 3)
Given el router con dos `FakeTextEngine`
When `extract(texto)` con política→LOCAL (o NUBE)
Then se invoca **solo** el motor elegido, el `TicketExtraido` devuelto lleva su `motor`, y `router.motor` (consultado antes) coincide con el elegido si el entorno no cambia. Errores y `CancellationException` del motor elegido se propagan sin tocar el otro (no hay fallback).

### Scenario 4: el resto de la app no cambia
Given el grafo Hilt de `:app`
Then `ExtraerTicket`, `EscanearViewModel`, `ProcesadoContent`, `RevisionDatosContent` y `GuardarTicket` compilan **sin modificar**; sus tests (`ExtraerTicketTest`, `EscanearViewModelTest` 23/23) y el E2E `:app` siguen en verde; el badge de procesado muestra el motor elegido antes de llamar y `extraido.motor` al terminar.

### Scenario 5: en directo (verificación 1)
Given Tab S9+ con el modelo descargado y realme GT 2 sin modelo
When procesan un ticket real con la preferencia por defecto
Then la Tab muestra "Motor Local · Gemma 3n" y el realme "Motor Nube · Gemini"; el ticket guardado en cada uno lleva `motor` LOCAL / NUBE respectivamente.

### Scenario 6: arquitectura
`domain/expenses` sin imports de Android/MediaPipe/Firebase; implementaciones del puerto de capacidad `internal` en `framework/ai`; un solo `di.kt` por módulo (`AiModule` ampliado); 0 versiones literales; sin claves ni `BuildConfig`; sin módulos Gradle nuevos; `schemas/` e `init.sh` sin diff.

## Repository Research

### Files Inspected
`AGENTS.md`; `PROGRESS.md` (líneas 1-679: estado y sesiones 001-011; el resto del log no leído, el estado vigente está en la cabecera); `feature_list.json` (entradas `router-automatico`, `reintento-otro-motor`, `ajustes-motor`); `docs/adr/0001-doble-motor-con-router.md`; `docs/specs/{extraccion-local,descarga-modelo,procesado-ui}.md` (parcial para procesado-ui); `docs/risks-and-open-questions.md` (grep de router/motor); `domain/expenses/.../{data/TextEngine,data/ModelDownloader,usecases/ExtraerTicket}.kt`; `framework/ai/.../di/di.kt`, `modelo/WorkManagerModelDownloader.kt` (`rutaModelo()` = `isFile && length>0`), `extraccion/{GemmaLocalEngine,GeminiCloudEngine}.kt` (existencia); `app/.../di/TextEngineEntryPoint.kt`; `app/src/androidTest/.../extraccion/ExtraccionLocalRealTest.kt`; `feature/escanear/.../{EscanearViewModel,EscanearScreen,ProcesadoContent,RevisionDatosContent}.kt` (grep de `motor`). No inspeccionado: `docs/domain-model.md`/`CONTEXT.md` en detalle (el glosario `Motor` ya existe en código), `ExtraccionNubeRealTest` completo.

### Existing Patterns To Follow
- Hoy `AiModule` provee `TextEngine` = `GeminiCloudEngine()` (sin calificar) y `@Named("local") TextEngine` = `GemmaLocalEngine` (`@Singleton`). `ExtraerTicket @Inject(textEngine: TextEngine)` y el VM leen `extraerTicket.motor` **antes** de extraer (badge honesto durante 26-41 s de local) y usan `extraido.motor` al terminar.
- Puertos de dominio implementados en `framework/ai` e `internal` (`DownloadConditions` ← `AndroidDownloadConditions`): mismo patrón para capacidad.
- Dobles en `:test:unit` (`FakeTextEngine(motor=...)`, `FakeModelDownloader`).

### Current Gaps
- No hay `EngineRouter`, `PreferenciaMotor` ni noción de capacidad del dispositivo. El badge hoy siempre dice Nube en la app real.
- `TextEngineEntryPoint.textEngine()` y `ExtraccionNubeRealTest` asumen que el `TextEngine` sin calificar es la nube; `ExtraccionLocalRealTest.theInjectedEngine...` aserta `ep.textEngine().motor == NUBE`. Hay que recalificar (ver Technical Approach 5).
- Datos de comparativa (sesiones 030-033): nube 4/4 totales correctos y rápida; local 2/4 (tickets 1 y 3 fallan por ambigüedad del OCR) y 26-41 s/ticket en Tab S9+; local sin red y el ticket no sale del dispositivo.

## Technical Approach

1. **Dominio (`domain/expenses`, puro)**:
   - `entities/PreferenciaMotor.kt`: `enum class PreferenciaMotor { AUTO, LOCAL, NUBE }`.
   - `data/CapacidadDispositivo.kt`: `interface CapacidadDispositivo { fun puedeEjecutarModeloLocal(): Boolean }`.
   - `data/PreferenciaMotorSource.kt`: `fun interface PreferenciaMotorSource { fun actual(): PreferenciaMotor }` (síncrona, barata; `ajustes-motor` la reimplementará leyendo su almacén).
   - `data/EngineRouter.kt`: `class EngineRouter(local: TextEngine, nube: TextEngine, capacidad, modelDownloader, preferencia) : TextEngine`, más la **función pura** `elegirMotor(preferencia, capaz, modeloDisponible): Motor` (top-level, fuente única de la política). `override val motor get() = elegir()`; `extract` llama `elegir()` y delega. `modeloDisponible = modelDownloader.rutaModelo() != null`. Sin `runCatching`, sin captura de excepciones.
2. **Política (D1, D2)**: Auto = Local si `capaz && modeloDisponible`, si no Nube. La red **no** es entrada: con modelo presente Local no la necesita; sin modelo, Nube es la única opción y, si no hay red, falla honestamente (sin red no hay motor posible; copy de error ya existente de procesado-ui).
3. **Capacidad (D3)**: `framework/ai/.../router/RamCapacidadDispositivo.kt` (`internal`): `ActivityManager.MemoryInfo.totalMem >= UMBRAL_RAM_BYTES` con `UMBRAL_RAM_BYTES = 10_000_000_000L` (Tab S9+ = 11.454.088 kB ≈ 11,7 GB pasa). Es una heurística de memoria (el modelo ocupa ~3,6 GB residentes, R-LOC2), no un benchmark. El umbral sale como constante documentada y testeable en JVM mediante una función pura `ramSuficiente(totalMemBytes)`.
4. **DI (`AiModule`, único `di.kt`)**: `GeminiCloudEngine` pasa a `@Provides @Named("nube")`; `@Named("local")` se mantiene; el `TextEngine` **sin calificar** pasa a `@Provides @Singleton fun provideTextEngine(@Named("local") local, @Named("nube") nube, capacidad, downloader, preferencia): TextEngine = EngineRouter(...)`. Nuevos `@Provides`: `CapacidadDispositivo` (`RamCapacidadDispositivo(context)`) y `PreferenciaMotorSource { PreferenciaMotor.AUTO }` (valor fijo; **es el único punto que `ajustes-motor` sustituirá**).
5. **Compatibilidad con tests existentes**: `TextEngineEntryPoint` gana `@Named("nube") fun textEngineNube()`; `textEngine()` devuelve ahora el router. `ExtraccionNubeRealTest` pasa a usar `textEngineNube()`; `ExtraccionLocalRealTest.theInjectedEngineIsTheLocalOneAndTheDefaultStaysCloud` se reescribe (renombrar) para afirmar `local=LOCAL`, `nube=NUBE` y que `textEngine()` es el router (su `motor` depende del entorno: no asertar valor fijo).
6. **UI/VM**: sin cambios. `EscanearScreen` lee `viewModel.motor` en cada recomposición (ahora `File.isFile` + `ActivityManager`: coste trivial). Limitación aceptada (D4): si el modelo termina de descargarse *durante* una extracción, el badge de "procesando" podría diferir un instante del `extraido.motor` final; el badge final y el ticket siempre usan `extraido.motor` (verdad registrada).
7. **Seams para features posteriores (no implementar)**: `reintento-otro-motor` podrá añadir `EngineRouter.extractCon(motor, texto)` (o parámetro en `ExtraerTicket`) usando los mismos engines inyectados; `ajustes-motor` implementará `PreferenciaMotorSource` y la UI. Ninguno exige cambiar `elegirMotor`.

## Expected File Changes

- `domain/expenses/.../entities/PreferenciaMotor.kt`, `.../data/{CapacidadDispositivo,PreferenciaMotorSource,EngineRouter}.kt` — create.
- `domain/expenses/src/test/.../data/EngineRouterTest.kt` — create (tabla política + delegación + propagación de errores + no-fallback + cancelación).
- `test/unit/src/main/kotlin/.../{FakeCapacidadDispositivo,FakePreferenciaMotorSource}.kt` — create (o lambdas locales si basta; `FakeTextEngine`/`FakeModelDownloader` se reutilizan, comprobar antes de crear).
- `framework/ai/.../router/RamCapacidadDispositivo.kt` — create (`internal`); `framework/ai/src/test/.../router/RamCapacidadDispositivoTest.kt` — create (función pura del umbral).
- `framework/ai/.../di/di.kt` — modify (según Approach 4).
- `app/src/main/.../di/TextEngineEntryPoint.kt` — modify (+ `textEngineNube()`).
- `app/src/androidTest/.../extraccion/{ExtraccionNubeRealTest,ExtraccionLocalRealTest}.kt` — modify (Approach 5). Opcional: assert en un E2E existente de que el badge de procesado muestra el motor esperado (emulador sin modelo → "Motor Nube").
- `docs/architecture.md` (sección TextEngine: router, política, calificadores), `docs/risks-and-open-questions.md` (decisión de política con los datos de la comparativa), `docs/evidence/router-automatico/` — create/update.
- `PROGRESS.md`, `feature_list.json` — los actualiza el orquestador/implementer al verificar (no este spec). Se sugiere que el orquestador añada en `notes` de `router-automatico`: "verificación 1 usa Tab S9+ y realme GT 2; registrar MemTotal de ambos".

## Visual Design Impact
Sin diseño nuevo. Se reutilizan los badges existentes (`ProcesadoContent`: "Motor Local · Gemma 3n" / "Motor Nube · Gemini"; `BadgeMotorRevision`). `DESIGN.md`/Stitch: no tocar.

## Durable Documentation Impact
- `ARCHITECTURE.md`/`CONSTRAINTS.md` raíz: not needed (no existen; equivalente `docs/architecture.md`).
- `AGENTS.md`: not needed (ya describe `EngineRouter` y el motor en la UI). `docs/adr/0001`: not needed (esta spec lo implementa tal cual; **no** hay ADR nuevo porque no se contradice).
- `docs/architecture.md`: update. `docs/risks-and-open-questions.md`: update. `docs/domain-model.md`/`technical-discovery.md`: not needed.

## Implementation Plan
1. Dominio + tests JVM (política, router) — verificable sin dispositivo.
2. Capacidad por RAM + DI + EntryPoint; ajustar los dos tests reales; `assembleDebug`.
3. Regresión JVM + E2E `:app` en `emulator-5554`.
4. Verificación física (Tab S9+ y realme GT 2), evidencias, docs.

## Implementation Tasks
- [ ] T1 `PreferenciaMotor`, puertos, `elegirMotor` + `EngineRouter` + `EngineRouterTest` (3 preferencias × 4 combinaciones; solo el elegido se invoca; error/cancelación propagados sin fallback; `motor` coherente). *Done: JUnit XML verde en `domain/expenses/build/test-results/`.*
- [ ] T2 `RamCapacidadDispositivo` + `ramSuficiente` + test de frontera del umbral. *Done: test verde.*
- [ ] T3 `AiModule` (nube calificado, router sin calificar, capacidad, preferencia fija AUTO) + `TextEngineEntryPoint`. *Done: `assembleDebug` verde; búsqueda: 0 imports de MediaPipe/Firebase en `domain/**`; `EngineRouter` y puertos sin Android.*
- [ ] T4 Ajustar `ExtraccionNubeRealTest`/`ExtraccionLocalRealTest` (Approach 5). *Done: compilan y los tests de identidad pasan en `emulator-5554`.*
- [ ] T5 Regresión: `assembleDebug testDebugUnitTest :test:unit:test --rerun :domain:expenses:test --rerun :framework:ai:testDebugUnitTest`; `ANDROID_SERIAL=emulator-5554 :app:connectedDebugAndroidTest` **sin modelo en el emulador** (si hay modelo, el badge E2E sería Local; ver riesgo R-RT3). Búsquedas: 0 versiones literales, 0 `AIza|api_key|buildConfigField`, `schemas/` e `init.sh` sin diff.
- [ ] T6 **Verificación física** (ver abajo). Exportar evidencia a `docs/evidence/router-automatico/`.
- [ ] T7 Docs durables, `feature_list.json` (evidencia) y sesión en `PROGRESS.md`.

## Verification Plan
- JVM (Windows `gradlew.bat`): gates de T5. E2E persistente: no se añade pantalla ni flujo; se justifica no crear E2E de UI nuevo (el comportamiento visible es el badge ya cubierto); se mantiene la regresión E2E y opcionalmente un assert de badge "Nube" en `ProcesadoFlujoTest`.
- **Física, Tab S9+ (modelo descargado)**: capturar ticket real → Continuar → badge "Motor Local · Gemma 3n" visible durante el procesado y en revisión; guardar y confirmar `motor=LOCAL` (screenshot + `adb shell run-as`/logcat `ExtraccionLocal`; ver evidencia). **Aprovechar los 26-41 s de local para completar la cancelación física pendiente de procesado-ui** (la cuota de Gemini sigue agotada): cancelar a mitad y comprobar que conserva la foto y no deja la app colgada ni el modelo sin liberar.
- **Física, realme GT 2 (sin modelo)**: mismo flujo → "Motor Nube · Gemini" y `motor=NUBE`. Registrar `MemTotal` de ambos (`adb shell cat /proc/meminfo`) para validar el umbral D3. Si el realme tuviera ≥10 GB, la verificación 1 se cumple igualmente porque **no tiene el modelo**: no descargarlo ahí salvo prueba deliberada.
- Contraste de la comparativa: no se re-mide aquí; se cita la de `extraccion-local`/`extraccion-nube`.

## Evidence To Capture
JUnit XML (router, capacidad); capturas del badge en Tab S9+ y realme; `MemTotal` de ambos; logs de las extracciones físicas con el motor; salida de búsquedas de arquitectura/seguridad; nota de la política y su justificación en `docs/risks-and-open-questions.md`.

## Decisiones con DEFAULT (no bloqueantes)
- **D1 Preferencia por defecto = Local-first con modelo descargado** (ADR 0001 y `user_visible_behavior`). Razones: descargar 3,4 GB es una acción explícita del usuario que expresa intención de usar on-device; offline-first y privacidad (el ticket no sale del dispositivo) son el argumento central del proyecto; la calidad menor (2/4 totales, ambigüedad del OCR) se mitiga con revisión manual y, después, `reintento-otro-motor`. Coste aceptado: 26-41 s por ticket y peor acierto que la nube. La política es una sola función pura: invertirla (Nube-first y Local solo si offline/forzado) es un cambio de una línea + tests.
- **D2 La red no es entrada de la política.** Si no hay modelo, Nube aunque no haya red (error honesto, sin fallback). Evita un puerto de red y una decisión "silenciosa" que contradiría el ADR.
- **D3 Capacidad = RAM total ≥ 10 GB** (heurística, constante documentada). Alternativas (benchmark, lista de dispositivos) descartadas por alcance.
- **D4 El router decide por extracción**, sin estado pegajoso; el badge final es siempre `extraido.motor`.
- **D5 Sin cambios de VM/UI/Room**; `Ticket.motor` ya se persiste desde `persistencia-room`.
- **D6 Preferencia fija `AUTO`** como puerto vacío; la persistencia y la UI son de `ajustes-motor`.

**Decisión de producto a confirmar (no bloquea implementar):** D1. Si el usuario prefiere Nube-first en Auto (más precisa y rápida, pero el ticket sale del dispositivo y la verificación "Tab S9+ usa Local" dejaría de cumplirse sin forzar), hay que cambiar `user_visible_behavior`/verificación 1 y ADR 0001 antes de implementar. **Ninguna decisión bloqueante de módulos**: no se crea módulo Gradle ni dependencia.

## Riesgos y preguntas abiertas
- **R-RT1 Calidad**: Auto en la Tab producirá resultados peores que la nube (2/4); la mitigación real llega con `reintento-otro-motor`. Documentar en el informe de validación como compromiso consciente.
- **R-RT2 Umbral de RAM**: se desconoce la RAM del realme GT 2 del usuario (variantes 8/12 GB); el ADR afirma que no puede mover el modelo pero el spike solo se midió en la Tab. Medir y registrar en T6.
- **R-RT3 E2E dependientes del motor**: si el emulador tiene el modelo (p. ej. tras `adb push` de extraccion-local T6), el router elegirá Local y cualquier E2E que asuma "Nube" cambiaría. Ejecutar T5 sin modelo o forzar la preferencia en tests.
- **R-RT4 Badge transitorio** (D4): ventana mínima de discrepancia si cambia el entorno durante una extracción.
- **R-RT5 Local colgado/OOM**: sin fallback automático, un fallo de carga en dispositivos justos se ve como error; el usuario no puede cambiar de motor hasta `reintento-otro-motor`/`ajustes-motor`. Aceptado por el orden del roadmap.
- **Pregunta**: ¿se confirma D1 (Local-first)? DEFAULT: sí, por ADR 0001.

## Validator Checklist
- [ ] Alcance: solo router, puertos y DI; sin UI, reintento, ajustes ni persistencia de preferencia; sin fallback automático.
- [ ] `elegirMotor` cubre la tabla completa (3 prefs × 4 combinaciones) y el router solo invoca el motor elegido.
- [ ] `domain` puro; `RamCapacidadDispositivo` `internal`; un solo `di.kt` por módulo; sin módulos nuevos ni versiones literales; sin claves.
- [ ] `ExtraerTicket`/VM/Screens sin diff de producto; tests existentes y E2E en verde.
- [ ] Evidencia física real: Tab S9+ = Local, realme GT 2 = Nube, `motor` registrado en el ticket guardado, `MemTotal` de ambos; sin resultados inventados.
- [ ] D1 y la justificación con los datos de la comparativa registrados; `feature_list.json`/`PROGRESS.md` con evidencia.

# Feature Implementation Spec: Texto crudo del ticket con ML Kit

## Source Feature

- `id`: ocr-mlkit
- `area`: ia
- `title`: Texto crudo del ticket con ML Kit
- `user_visible_behavior`: De la foto capturada se obtiene el texto crudo (ML Kit Text Recognition v2, latin) y queda guardado con el borrador para extracción y reintentos.
- `depends_on`: ["captura-foto"] (accepted → satisfecha)
- `status`: not_started (a fecha de planificación, 2026-10-15)
- `source`: `feature_list.json`
- `verification` (fuente):
  1. Con 4 fotos reales de tickets (desviación acordada 2026-10-16 respecto a 5), el texto crudo coincide razonablemente con el papel.
  2. El texto crudo queda registrado con el borrador (visible en debug).
- `notes` (fuente): ADR 0002 (OCR en dos etapas). On-device, sin clave de API.

## Goal

Implementar la **etapa 1 del ADR 0002**: foto persistida → texto crudo con ML Kit
Text Recognition v2 (latin, on-device, sin red ni clave de API). El texto crudo
se obtiene automáticamente de la foto ya guardada por `captura-foto`, queda
**registrado con el Borrador** (en memoria, junto a `rutaFoto`) y es visible en
una superficie de debug de la pantalla 05. El contrato de dominio (`OcrEngine`)
queda listo para que `extraccion-nube`/`extraccion-local` consuman exactamente
este texto y para que `reintento-otro-motor` lo reutilice sin repetir el OCR.

## Non-Goals

- Extracción estructurada de ningún tipo: `TicketExtraido`, schema, comercio/
  fecha/total/líneas/categoría (`extraccion-nube`, `extraccion-local`).
- `TextEngine`, `EngineRouter`, badge de motor y registro de motor usado.
- Pantalla de procesado (mockup 06, `procesado-ui`): "Continuar" sigue
  **deshabilitado** en pantalla 05.
- Escrituras en Room ni uso de `TicketRepository`: el texto crudo se persiste
  con el ticket cuando `revision-datos` guarde (el campo y la columna ya
  existen; ver Contrato de dominio).
- Persistencia del Borrador tras crash (sigue en "Later / Not MVP" de
  `docs/risks-and-open-questions.md`); sin ficheros sidecar de texto (P2).
- Reintento con otro motor (`reintento-otro-motor`) y Ajustes.
- Decoración ficticia de `DESIGN.md` (R4): chips "OCR 99,4 %", "3 conceptos y
  total identificados", porcentajes de confianza. El panel de texto crudo es
  una superficie **solo debug**, no diseño.
- Módulos Gradle nuevos, migraciones de Room, cambios de esquema.
- Red, claves de API o variantes de ML Kit que dependan de Play Services.

## Job Story

When he capturado la foto del ticket y quiero que la extracción parta de su texto,
I want que la app lea el texto del ticket en el dispositivo (OCR) y lo guarde con mi borrador,
so I can extraer los datos con cualquier motor y reintentar sin repetir el OCR ni volver a fotografiar.

## Users And Permissions

- Usuario único (sin cuentas; la posesión del dispositivo es la autenticación,
  `docs/build-brief.md`): captura la foto y el texto crudo se obtiene solo.
- Sin permisos nuevos: ML Kit bundled (modelo dentro del APK) no pide permisos,
  ni red, ni cuenta de Google. La app sigue funcionando offline.
- El texto crudo vive en almacenamiento privado de la app (memoria del
  Borrador + columna de Room al guardar); nada sale del dispositivo.

## Acceptance Scenarios

### Scenario 1: flujo feliz — de la foto al texto crudo

Given la app en Escanear con permiso concedido y una foto capturada (ya guardada en `filesDir/fotos/`)
When termina el procesado de la captura
Then la pantalla 05 muestra la foto y, en el panel de debug (builds debug), el texto crudo del
ticket; el texto queda registrado con el Borrador (`rutaFoto` + `textoCrudo` en el ViewModel).

### Scenario 2: el texto crudo coincide con 4 fotos reales

Given 4 fotos reales de tickets (fixtures reales; desviación acordada 2026-10-16 respecto a 5)
When corre el OCR sobre cada una (test instrumentado) y se contrasta con el papel
Then el texto crudo contiene las palabras clave razonables de cada ticket (comercio y total
presentes, salvo legibilidad real del papel) y el contraste humano "coincide razonablemente con
el papel" queda registrado como evidencia.

### Scenario 3: OCR vacío (foto ilegible o sin texto)

Given una captura sin texto legible
When termina el procesado
Then la pantalla 05 muestra la foto con indicación explícita de "(OCR vacío)" en el panel de
debug; no hay crash; "Repetir foto" sigue operativo y "Continuar" deshabilitado; la foto NO se
considera gastada.

### Scenario 4: fallo del motor OCR

Given que el motor OCR lanza un error durante el procesado
When termina la captura
Then la foto ya guardada se conserva y la pantalla 05 se muestra igualmente con indicación de
"OCR no disponible" en debug; el fallo NO convierte la captura en error ni bloquea el flujo.

### Scenario 5: "Repetir foto" descarta el texto crudo del borrador

Given pantalla 05 con foto y texto crudo ya obtenidos
When el usuario pulsa "Repetir foto"
Then se vuelve al encuadre, la foto del borrador se borra (best-effort, regla de `captura-foto`)
y el texto crudo del borrador descartado desaparece; la captura siguiente genera su propio texto.

### Scenario 6: el texto se reutiliza sin repetir OCR (contrato para reintentos)

Given un Borrador con texto crudo ya obtenido
When se consulta el estado (una y otra vez) sin nueva captura
Then no se invoca de nuevo el motor OCR: el texto del Borrador se recupera tal cual
(unit test del ViewModel: el doble `FakeOcrEngine` registra una sola invocación).

### Scenario 7: regresión del flujo aceptado

Given la feature implementada
When corren los E2E y la revisión de código
Then `NavigationTest` sigue 3/3, `CapturaFlujoTest` en verde (con el panel de debug), "Continuar"
sigue deshabilitado, `:domain:expenses` sigue sin imports de Android/ML Kit y el esquema de Room
no cambia.

## Repository Research

### Files Inspected

- `AGENTS.md` — capa de IA (`TextEngine` en `domain/expenses/data/`, implementaciones `internal` en `framework/ai`), reglas de módulos, testing, "qué no hacer" (sin módulos Gradle nuevos).
- `PROGRESS.md` — `captura-foto` aceptada (sesión 014); finding Low: `runCatching` que traga `CancellationException` (a no repetir); criterio de dispositivos (emulador para gates, Tab S9+ para captura física).
- `feature_list.json` — alcance, verificaciones y nota ADR 0002 de esta feature; continuidad con `extraccion-nube`/`extraccion-local` ("los 5 textos crudos reales") y `reintento-otro-motor` ("el texto crudo se reutiliza: no se repite el OCR").
- `docs/adr/0002-ocr-en-dos-etapas-con-ml-kit.md` — **obligatorio**: etapa 1 = foto → texto crudo (ML Kit Text Recognition v2, latin, on-device, sin clave); el texto se guarda con el ticket como evidencia y entrada de reintentos; ambos motores consumen el mismo texto.
- `docs/adr/0001-doble-motor-con-router.md` — sin impacto (no hay motor de extracción aquí); no se contradice.
- `docs/domain-model.md` — Ticket incluye "texto crudo del OCR"; Borrador no persistido (vive en memoria; solo la foto se guarda al capturar); "OCR vacío" no gasta la foto.
- `docs/technical-discovery.md` — OCR = ML Kit Text Recognition v2 (`text-recognition` latin), on-device, gratuito, sin clave. **Discrepancia detectada**: narra "fotos y texto crudo como ficheros en `filesDir`", pero el esquema aceptado (`persistencia-room`) ya modela el texto crudo como columna `texto_crudo_ocr` de `tickets` (ver Contrato de dominio y P2).
- `docs/risks-and-open-questions.md` — R3 (calidad de OCR en tickets reales); research task "ML Kit Text Recognition v2 → texto crudo de 5 tickets reales" (se cierra con esta feature); "Persistencia de borradores tras crash" = Later / Not MVP.
- `CONTEXT.md` — "OCR" y "Texto crudo" (salida textual tal cual, antes de estructurar; se guarda junto al ticket como evidencia).
- `docs/build-brief.md` — MVP slice 3: "OCR (ML Kit) → extracción"; esta feature es solo la primera flecha.
- `docs/architecture.md` — diagrama de módulos y regla de dependencias; `:framework:ai` ya existe (spike Gemma).
- `docs/specs/captura-foto.md` (referencia de formato) + `persistencia-room.md` (Open Questions) + `spike-gemma-mediapipe.md` — estructura y nivel de detalle del repo.
- `domain/expenses/.../entities/Ticket.kt` — **`textoCrudoOcr: String?` ya existe** en el agregado.
- `domain/expenses/.../data/{PhotoFileStore,PhotoProcessor,TicketRepository}.kt` y `usecases/GuardarFotoCapturada.kt` — contratos actuales: `PhotoFileStore` solo tiene `save`/`delete` (sin lectura); patrón de caso de uso (`@Inject constructor` + `operator fun invoke`).
- `framework/database/.../entity/TicketEntity.kt` + `schemas/.../1.json` — columna `texto_crudo_ocr` ya en el esquema v1 (sin migración que hacer).
- `framework/database/.../source/{PhotoFileStoreImpl,TicketLocalDataSource}.kt` — `delete`/`save` sobre `filesDir/fotos`; el mapping Ticket ↔ TicketEntity ya incluye `textoCrudoOcr` en ambos sentidos.
- `framework/ai/` (`di/di.kt`, `spike/GemmaMediaPipeSpike.kt`, `build.gradle.kts`) — patrón `di.kt` (`internal object AiModule`, aún sin providers); `:framework:ai` hoy NO depende de `:domain:expenses` (solo MediaPipe + coroutines).
- `feature/escanear/` (`EscanearScreen/ViewModel/State`, `RevisionFotoContent`, `EscanearViewModelTest`) — pipeline de captura: `CapturaUiState` (PermisoPendiente/Encuadre/Procesando/Revision(rutaFoto)/Error); "Continuar" `enabled = false`; pantalla 05 con testTags `captura_*`; el VM inyecta `GuardarFotoCapturada` + `PhotoFileStore`.
- `app/src/androidTest/.../{NavigationTest,CapturaFlujoTest}.kt` — E2E persistente del repo; `CapturaFlujoTest` ya cubre obturador → pantalla 05 → "Repetir foto".
- `test/unit/src/main/kotlin/` — `FakePhotoFileStore` (solo save/delete), `FakePhotoProcessor`, `FakeTicketDataSource`, `CoroutineTestRule`.
- `gradle/libs.versions.toml` — sin ML Kit; patrón de versiones centralizadas.

### Existing Patterns To Follow

- Contratos en `domain/<área>/data/` (inglés: `PhotoProcessor`, `PhotoFileStore`, `TextEngine`), implementaciones `internal` en `framework/<área>/`, bindings en un `di.kt` por módulo (`AiModule` de `:framework:ai` se amplía; no se crea otro).
- Casos de uso en `domain/<área>/usecases/` con nombre en español (`GuardarFotoCapturada` → `ReconocerTextoCrudo`) y `operator fun invoke`.
- ViewModel con estado derivado por operadores sobre `MutableStateFlow` (cadena `flatMapLatest`; sin `load()` imperativas); tests con Turbine + `CoroutineTestRule` + dobles de `:test:unit`.
- Tests instrumentados de APIs Android en `framework/*/src/androidTest/` (precedentes `CapturedPhotoProcessorTest`, `PhotoFileStoreTest`, `GemmaMediaPipeSpikeTest`), fixtures de imagen en `src/androidTest/assets/`.
- Versiones solo en `gradle/libs.versions.toml`; decisión de versión registrada (patrón de sesiones previas).

### Current Gaps

- No existe interfaz de OCR en dominio ni implementación ML Kit: hay que crearlas.
- `PhotoFileStore` no puede leer ficheros (solo `save`/`delete`): el OCR necesita
  la foto ya persistida → ampliar el contrato con `read`.
- `:framework:ai` no depende de `:domain:expenses` todavía (hoy solo el spike).
- No hay fixtures de tickets reales ni textos crudos de referencia para las
  features posteriores (`extraccion-nube`/`extraccion-local` esperan "los 5
  textos crudos reales").
- `runCatching` sin re-lanzar `CancellationException` en `EscanearViewModel`
  (finding Low sesión 014): no repetir en código nuevo y corregir si se toca.

## Technical Approach

1. **Dominio (`:domain:expenses`)** — nueva interfaz `data/OcrEngine.kt`
   (`suspend fun recognize(image: ByteArray): String`): recibe bytes JPEG de la
   foto ya procesada por `captura-foto` (EXIF horneada, ≤1280 px) y devuelve el
   **texto crudo** (cadena tal cual, líneas separadas por `\n`; `""` si no hay
   texto). Cero imports de Android/ML Kit. Ampliar `PhotoFileStore` con
   `suspend fun read(path: String): ByteArray?` (relativo bajo `filesDir`, misma
   semántica de rutas que `delete`; `null` si no existe). Caso de uso
   `usecases/ReconocerTextoCrudo` (`@Inject constructor(PhotoFileStore,
   OcrEngine)`, `operator fun invoke(rutaFoto: String): String` = `read` →
   `recognize`; fichero ausente → `""`; errores del motor se propagan y
   `CancellationException` **nunca se traga**).
2. **Framework IA (`:framework:ai`)** — `ocr/MlKitOcrEngine.kt` (`internal`,
   implementa `OcrEngine`): ML Kit Text Recognition v2 latin **bundled**
   (`com.google.mlkit:text-recognition`, modelo en el APK, sin Play Services ni
   red), `Dispatchers.IO`, `BitmapFactory.decodeByteArray` →
   `InputImage.fromBitmap(bitmap, 0)` (la foto ya viene con la orientación
   horneada) → `Text.text` como texto crudo. Task de ML Kit envuelta en
   `suspendCancellableCoroutine` (sin dependencia extra de coroutines-play-services).
   Binding `@Provides OcrEngine` en el `di.kt` existente de `:framework:ai`, que
   gana dependencia de `:domain:expenses`. `PhotoFileStoreImpl.read` en
   `:framework:database` (junto a `save`/`delete`).
3. **Feature (`:feature:escanear`)** — la cadena de captura pasa a ser:
   `onImageCapturado` → `GuardarFotoCapturada` (la foto se guarda primero,
   siempre) → `ReconocerTextoCrudo(rutaFoto)` dentro del estado `Procesando` →
   `CapturaUiState.Revision(rutaFoto, textoCrudo)`. Semántica: **OCR vacío o
   fallo del motor nunca bloquean la captura** (la foto ya está en disco; se
   llega a pantalla 05 con `textoCrudo = ""` y una marca de estado para el panel
   debug). "Repetir foto" descarta Borrador y texto. Panel de debug en
   `RevisionFotoContent`: solo en builds debug
   (`ApplicationInfo.FLAG_DEBUGGABLE`), testTag `captura_texto_crudo`, muestra el
   texto crudo con scroll o "(OCR vacío)" / "OCR no disponible".
4. **Build** — `gradle/libs.versions.toml` con `mlkitTextRecognition`
   (`com.google.mlkit:text-recognition`, línea 16.x; el implementer fija la
   última estable y la registra como en features previas). Variante bundled
   elegida sobre `play-services-mlkit-text-recognition` (unbundled) por
   offline-first, determinismo y funcionamiento en el emulador; coste: ~+6 MB de
   APK (anotado en riesgos).

## Contrato de dominio (texto crudo)

1. **Producción** — `OcrEngine` en `domain/expenses/data/`:
   entrada = bytes JPEG de la foto ya persistida; salida = texto crudo (`""` si
   no se detecta texto). Implementación única: `MlKitOcrEngine` (internal,
   `:framework:ai`). Es la etapa 1 del ADR 0002; la etapa 2 (`TextEngine`) es de
   otras features.
2. **Orquestación** — `ReconocerTextoCrudo(rutaFoto): String` en
   `domain/expenses/usecases/`: lee la foto con `PhotoFileStore.read` y corre el
   OCR. Fichero ausente → `""` (sin crash). Errores del motor se propagan al
   llamador (la política de "no bloquear" vive en la presentación).
3. **Vida del texto ("guardado con el borrador")** — **sin entidad nueva ni
   tabla nueva**: el campo `Ticket.textoCrudoOcr` y la columna
   `tickets.texto_crudo_ocr` ya existen (esquema v1, mapping ya en
   `TicketLocalDataSource`). En esta feature el Borrador (in-memory, estado del
   `EscanearViewModel`) lleva el par `(rutaFoto, textoCrudo)`. Cuando
   `revision-datos` persista vía `TicketRepository.save`, el texto viajará en
   `Ticket.textoCrudoOcr`. Recuperación para `reintento-otro-motor`: desde el
   Borrador en memoria, **sin repetir el OCR**. Si la app muere, solo sobrevive
   la foto (regla del dominio; el OCR puede re-correrse desde ella).
4. **Visible en debug** — panel en pantalla 05 solo debug (testTag
   `captura_texto_crudo`); en release no se muestra nada (el texto viaja igual
   con el Borrador/Ticket).

## Expected File Changes

- `gradle/libs.versions.toml` — modify; + `mlkitTextRecognition` (`com.google.mlkit:text-recognition`, latin bundled).
- `framework/ai/build.gradle.kts` — modify; + `implementation(project(":domain:expenses"))`, + `implementation(libs.mlkit.text.recognition)`.
- `framework/ai/src/main/java/com/robertomr/tally/framework/ai/ocr/MlKitOcrEngine.kt` — create; implementación ML Kit (`internal`).
- `framework/ai/src/main/java/com/robertomr/tally/framework/ai/di/di.kt` — modify; + `@Provides OcrEngine`.
- `framework/ai/src/androidTest/java/com/robertomr/tally/framework/ai/ocr/MlKitOcrEngineTest.kt` — create; test instrumentado del OCR (imagen renderizada + fixtures reales).
- `framework/ai/src/androidTest/assets/ticket_real_1.jpg` … `ticket_real_5.jpg` — create; fixtures reales (ver P3).
- `framework/database/src/main/java/com/robertomr/tally/framework/database/source/PhotoFileStoreImpl.kt` — modify; + `read`.
- `framework/database/src/androidTest/java/com/robertomr/tally/framework/database/PhotoFileStoreTest.kt` — modify; + tests de `read` (relativo/ausente).
- `domain/expenses/src/main/java/com/robertomr/tally/domain/expenses/data/OcrEngine.kt` — create; interfaz.
- `domain/expenses/src/main/java/com/robertomr/tally/domain/expenses/data/PhotoFileStore.kt` — modify; + `read` con contrato de ruta relativa.
- `domain/expenses/src/main/java/com/robertomr/tally/domain/expenses/usecases/ReconocerTextoCrudo.kt` — create; caso de uso.
- `domain/expenses/src/test/java/com/robertomr/tally/domain/expenses/usecases/ReconocerTextoCrudoTest.kt` — create; unit test (JVM).
- `test/unit/src/main/kotlin/com/robertomr/tally/test/unit/FakeOcrEngine.kt` — create; doble (registra invocaciones, resultado/error configurables).
- `test/unit/src/main/kotlin/com/robertomr/tally/test/unit/FakePhotoFileStore.kt` — modify; + `read` (payloads por ruta configurables).
- `feature/escanear/src/main/java/com/robertomr/tally/feature/escanear/EscanearViewModel.kt` — modify; cadena `GuardarFotoCapturada` → `ReconocerTextoCrudo`; `Revision(rutaFoto, textoCrudo)`; semántica vacío/fallo; **re-lanzar `CancellationException`** en los `runCatching` (corregir también el existente — finding Low sesión 014 — al tocar el fichero).
- `feature/escanear/src/main/java/com/robertomr/tally/feature/escanear/EscanearScreen.kt` — modify; pasa `textoCrudo` a la revisión.
- `feature/escanear/src/main/java/com/robertomr/tally/feature/escanear/RevisionFotoContent.kt` — modify; panel de debug (FLAG_DEBUGGABLE, testTag `captura_texto_crudo`).
- `feature/escanear/src/test/java/com/robertomr/tally/feature/escanear/EscanearViewModelTest.kt` — modify; casos OCR (texto registrado, vacío, fallo no bloquea, sin re-OCR, repetir descarta).
- `app/src/androidTest/java/com/robertomr/tally/captura/CapturaFlujoTest.kt` — modify; espera del OCR + panel debug presente + "Continuar" deshabilitado.
- `docs/evidence/ocr-mlkit/` — create; los 4 textos crudos reales (`.txt`; desviación acordada de 5 a 4) + capturas/uiautomator del panel debug (inputs de `extraccion-nube`/`extraccion-local`).
- `docs/specs/ocr-mlkit.md` — create (este documento).
- `docs/architecture.md` — update; nueva sección "OCR (texto crudo)" (etapa 1 ADR 0002).
- `docs/risks-and-open-questions.md` — update; research task "ML Kit Text Recognition v2" completada con resultados reales; nota sobre R3.
- `docs/technical-discovery.md` — update (aclaración): el texto crudo se persiste como columna `texto_crudo_ocr` de `tickets` y vive con el Borrador en memoria; no como fichero sidecar (salvo que P2 se resuelva en contra del default).
- `docs/domain-model.md` — update mínimo: el Borrador lleva el texto crudo desde el OCR (una línea en States and Lifecycles).
- `PROGRESS.md` y `feature_list.json` — update **al terminar implementación y verificación** (evidencia); no aprobados por este spec.

## Visual Design Impact

- UI involved: mínima. Design source: `docs/design/stitch/05-revision-foto/` sin cambios normativos.
- Pantallas/estados afectados: solo pantalla 05, con un panel **exclusivamente debug**
  (no forma parte del mockup; no es decoración de diseño, es una superficie de
  diagnóstico similar a la que pide `docs/technical-discovery.md` en Observability).
- El resto queda exactamente como en `captura-foto`: "Repetir foto" operativo,
  "Continuar" deshabilitado, sin chips ni porcentajes inventados (CONTEXT.md R4).
- New design artifact required: no.

## Durable Documentation Impact

- `ARCHITECTURE.md` (raíz): not needed — no existe; el equivalente es `docs/architecture.md`.
- `CONSTRAINTS.md` (raíz): not needed — no existe; la restricción crítica (dominio sin Android/ML Kit; OCR on-device sin clave) queda en `AGENTS.md`, `docs/architecture.md` y este spec.
- `AGENTS.md`: not needed — la capa de IA ya está documentada; no cambian las reglas.
- `docs/architecture.md`: update — sección "OCR (texto crudo)": `OcrEngine` en domain, `MlKitOcrEngine` en `:framework:ai`, foto → texto crudo (etapa 1 ADR 0002).
- `docs/domain-model.md`: update mínimo — el Borrador lleva el texto crudo del OCR.
- `docs/risks-and-open-questions.md`: update — research task ML Kit cerrada con datos reales; R3 anotada.
- `docs/technical-discovery.md`: update de aclaración (columna vs fichero; ver Contrato de dominio).
- `PROGRESS.md` + `feature_list.json`: update al finalizar (evidencia), no ahora.

## Implementation Plan

1. Catálogo + `framework/ai/build.gradle.kts` (ML Kit + `:domain:expenses`): base compila.
2. Dominio: `OcrEngine`, `PhotoFileStore.read`, `ReconocerTextoCrudo` + dobles + tests JVM.
3. Framework: `MlKitOcrEngine` + binding `di.kt` + `PhotoFileStoreImpl.read` + tests instrumentados (render + fixtures).
4. Feature: cadena OCR en el ViewModel + panel debug en pantalla 05 + tests del VM.
5. E2E + verificación física (4 tickets reales; desviación acordada) + docs durables + evidencia.

## Implementation Tasks

- [x] T1 `gradle/libs.versions.toml` con `mlkitTextRecognition` (com.google.mlkit:text-recognition, latin bundled) + `framework/ai/build.gradle.kts` (+`:domain:expenses`, +ML Kit); 0 versiones literales en `**/*.gradle.kts`. *Criterio: `assembleDebug` en verde; decisión de versión registrada.* → versión fijada `16.0.1` (última estable de la línea 16.x, 2024-08; bundled por offline-first/determinismo), `assembleDebug` BUILD SUCCESSFUL.
- [x] T2 Dominio: `data/OcrEngine.kt`, `PhotoFileStore.read` (contrato ruta relativa), `usecases/ReconocerTextoCrudo.kt`. *Criterio: `:domain:expenses` sin imports de Android/ML Kit (búsqueda).* → 0 coincidencias.
- [x] T3 `:test:unit`: `FakeOcrEngine.kt`, `FakePhotoFileStore.read` (con round-trip `save`→`read`).
- [x] T4 `ReconocerTextoCrudoTest` (JVM): read→recognize en orden; devuelve el texto del motor; fichero ausente → `""`; error del motor se propaga; `CancellationException` re-lanzada (no convertida en resultado). *Criterio: tests en verde con Turbine/fakes.* → 5/5 en verde.
- [x] T5 Framework: `MlKitOcrEngine.kt` (internal, Dispatchers.IO, `InputImage.fromBitmap`) + `@Provides OcrEngine` en `framework/ai/di/di.kt` + `PhotoFileStoreImpl.read`.
- [x] T6 `MlKitOcrEngineTest` (androidTest `:framework:ai`): **(a)** imagen renderizada en runtime (Canvas: texto grande y claro) con asserts de tokens exactos; **(b)** 4 fixtures reales `ticket_real_1..4.jpg` + `ticket_real_1..4.tokens.txt` con su tabla de tokens (comercio/total) → **7 tests, 0 fallos, 0 skipped** en `emulator-5554` y en la Tab S9+ (JUnit XML 2026-10-16). **Desviación registrada**: el spec planificaba 5 tickets, se aportaron 4 fotos reales (ver Implementation Notes). Verificación 1 de la feature = 4 fotos reales (desviación acordada 2026-10-16).
- [x] T7 Feature: `CapturaUiState.Revision(rutaFoto, textoCrudo)`, cadena del VM (foto primero → OCR), semántica vacío/fallo no bloquea, panel debug en `RevisionFotoContent` (FLAG_DEBUGGABLE, `captura_texto_crudo`), `EscanearScreen` pasa el texto; `EscanearViewModelTest` ampliado (incluye una sola invocación de OCR y descarte en "Repetir foto"). *Criterio: tests JVM en verde; `runCatching` sin tragar `CancellationException`.* → 11/11 en verde (`runCatchingLanzandoCancelacion`).
- [x] T8 `CapturaFlujoTest` ampliado (pantalla 05 con panel debug presente y "Continuar" deshabilitado, con esperas que absorban la latencia del OCR) + regresión `NavigationTest` 3/3. → 6/6 en `emulator-5554`, 0 skipped.
- [x] T9 Verificación física Tab S9+: contraste humano "coincide razonablemente con el papel" para los tickets reales. → **CONFIRMADO por el usuario (2026-10-16)**: los 4 textos crudos reales (`docs/evidence/ocr-mlkit/texto_crudo_1..4.txt`, OCR de ML Kit sobre las fotos aportadas) contienen comercio, fecha, productos y totales; el usuario confirma que coinciden razonablemente con el papel (ruido típico de tickets térmicos: `SANTOMÁS`, `D'ALACANT`, `ROSQUILLA`). Observación registrada por el usuario: el precio relevante para el usuario es el **total con IVA**; el texto crudo incluye base, cuota y total por separado (p. ej. ticket 2 `3,36 IVA: 0,14`), de modo que elegir el total con IVA es responsabilidad de la etapa 2 (`extraccion-nube`/`extraccion-local`, ADR 0002), no del OCR. Captura en vivo por el flujo completo en la Tab S9+ con panel debug (`docs/evidence/ocr-mlkit-panel-debug-tab.png` + `ocr-mlkit-uiautomator-tab.xml`, escenario 3 "(OCR vacío)" en hardware). Verificación 1 = 4 fotos reales (desviación acordada).
- [x] T10 Regresión completa (gates JVM + instrumentados), docs durables (`architecture.md`, `domain-model.md`, `risks-and-open-questions.md`, `technical-discovery.md`) y evidencia en `feature_list.json`/`PROGRESS.md`.

## Implementation Notes / Desviaciones (sesión 015)

- **Fixtures reales (T6b/T9) bloqueados por dependencia humana.** No hay 5
  tickets de papel ni quien los fotografíe en esta sesión. En lugar de inventar
  imágenes o textos, se implementó el harness parametrizado que lee
  `ticket_real_<n>.jpg` + `ticket_real_<n>.tokens.txt` y se salta con
  `assumeTrue` (motivo explícito) si faltan. La verificación 1 del spec (5 fotos
  reales + contraste humano) NO se ejecutó entonces y por eso la feature no
  quedó `passing` en esa sesión. **SUPERADO en la sesión 016** (ver más abajo):
  se aportaron 4 fixtures reales, el harness quedó en 4 métodos sin skips y el
  usuario confirmó el contraste humano.
- **`FakePhotoFileStore` con round-trip.** El doble compartido no devolvía por
  `read` lo escrito por `save`; el OCR nunca llegaba a invocar el motor y 4
  tests del ViewModel fallaban. Corregido el doble (mapa `savedByName`) sin
  tocar producto.
- **`ReconocerTextoCrudoTest`**: `engineErrorPropagatesToTheCaller` devolvía
  `String` (última expresión de `runBlocking`) y JUnit exige `void`; se declara
  `: Unit`.
- **Corrupción de encoding corregida**: la KDoc de `PhotoFileStore.delete` había
  perdido los acentos (U+FFFD) en el árbol parcial; restaurada.
- **Gate `:framework:ai:connectedDebugAndroidTest` en emulador**: el módulo
  también contiene `GemmaMediaPipeSpikeTest`, que exige el modelo `.litertlm`
  (3,4 GB, solo Tab S9+) y falla por diseño en el emulador. El gate del OCR se
  ejecuta filtrado a `MlKitOcrEngineTest`
  (`-Pandroid.testInstrumentationRunnerArguments.class=...`) en emulador y Tab;
  el test del spike es de otro feature y ya `accepted`.
- **`docs/technical-discovery.md`**: la narración "texto crudo como fichero en
  `filesDir`" era una discrepancia; aclarada a columna `texto_crudo_ocr` +
  Borrador en memoria (default P2).

### Sesión 016 (2026-10-16): fixtures reales aportados y desviación a 4

- El usuario aportó **4 fotos reales de tickets** (`ticket_real_1..4.jpeg`),
  commiteadas como `framework/ai/src/androidTest/assets/ticket_real_1..4.jpg`
  **con su tabla de tokens** `ticket_real_<n>.tokens.txt` (un token "razonable"
  por línea: comercio/total del ticket). **Desviación acordada**: el spec y la
  verificación planificaban 5 fotos; se acepta **4** (confirmado por el usuario
  2026-10-16). El harness se ajustó de 5 a 4 métodos (sin skip permanente).
- Tokens por ticket (presentes en la salida real de ML Kit, corroborados con un
  OCR independiente — Windows.Media.Ocr — para no ser tautológicos):
  1) `HOLA`/`ALMIRANTE`/`PROFORMA`/`32,09`; 2) `AEROPUERTO`/`TOTAL`/`SEVILLA`/`3,36`;
  3) `OZONE`/`TACOS`/`POLLO`/`12,64`; 4) `GRUPO`/`LECHE`/`PISTACHO`/`7,91`.
- Gate `:framework:ai:connectedDebugAndroidTest` (filtrado a `MlKitOcrEngineTest`):
  **7 tests, 0 fallos, 0 skipped** en `emulator-5554` y en la Tab S9+ (SM-X810).
- Textos crudos reales exportados a `docs/evidence/ocr-mlkit/texto_crudo_1..4.txt`
  (UTF-8, con `€` y acentos) como insumo de `extraccion-nube`/`extraccion-local`.
- Corrección de harness: el `id` de la feature en `feature_list.json` había
  quedado como `"  "` (corrupción); restaurado a `"ocr-mlkit"`.
- Pendiente para cerrar T9: **contraste humano** del usuario sobre cada
  `texto_crudo_<n>.txt` vs el papel ("coincide razonablemente").

## Verification Plan

Criterio de dispositivo (explícito, precedentes `captura-foto`/`spike-gemma-mediapipe`):

- **Emulador `emulator-5554`**: gates JVM, tests instrumentados del OCR (ML Kit
  bundled funciona sin Play Services) y E2E (`CapturaFlujoTest`,
  `NavigationTest`). La escena de la cámara virtual no contiene un ticket real,
  por lo que el E2E comprueba la **presencia** del panel debug y la semántica
  de estados, no la calidad del texto.
- **Tab S9+ (SM-X810, físico)**: obligatorio para la verificación 1 de la
  feature (4 fotos reales de tickets de papel — desviación acordada — + contraste
  humano "coincide razonablemente con el papel") y para la captura en vivo por el flujo completo.
  Los instrumentados del OCR se re-ejecutan también en ella (precedente del
  spike). El realme GT 2 no se exige aquí (se reserva para las features de motor
  nube).

Automatizada (Windows: `gradlew.bat`):

- `gradlew.bat assembleDebug` → BUILD SUCCESSFUL (regresión base).
- `gradlew.bat testDebugUnitTest` → BUILD SUCCESSFUL (incluye `ReconocerTextoCrudoTest` y `EscanearViewModelTest`).
- `gradlew.bat :test:unit:test --rerun`, `:domain:expenses:test --rerun`, `:framework:database:testDebugUnitTest` → verdes; revisar JUnit XML.
- `ANDROID_SERIAL=emulator-5554 gradlew.bat :framework:ai:connectedDebugAndroidTest` → `MlKitOcrEngineTest` (tokens del render + 4 fixtures reales) en verde.
- `ANDROID_SERIAL=<serial-SM-X810> gradlew.bat :framework:ai:connectedDebugAndroidTest` → mismo contenido en la Tab S9+ (corroboración).
- `ANDROID_SERIAL=emulator-5554 gradlew.bat :framework:database:connectedDebugAndroidTest` → regresión `PhotoFileStoreTest` (con `read`) + `CapturedPhotoProcessorTest` + `TicketDaoTest`.
- `ANDROID_SERIAL=emulator-5554 gradlew.bat :app:connectedDebugAndroidTest` → `CapturaFlujoTest` (panel debug presente, "Continuar" deshabilitado) + `NavigationTest` 3/3.
- `bash init.sh` (o equivalente): `assembleDebug` + `testDebugUnitTest` en verde; **sin cambios** en el script.
- Búsquedas de regresión: 0 versiones literales fuera del toml; 0 imports de Android/ML Kit en `domain/**`; esquema Room sin cambios (`schemas/` sin diff); "Continuar" `enabled = false` intacto.

Manual / físico (Tab S9+):

- Capturar en vivo ≥1 ticket de papel real por el flujo completo (encuadre → obturador → pantalla 05): el panel debug muestra el texto crudo (`uiautomator dump` + screenshot).
- Los 4 tickets reales (desviación acordada; el spec planificaba 5): OCR de cada uno (fixtures del test instrumentado), texto crudo exportado a `docs/evidence/ocr-mlkit/texto_crudo_1..4.txt`; el usuario/autor contrasta cada texto con el papel y confirma "coincide razonablemente" (la comparación con papel exige juicio humano: el agente no ve imágenes ni el ticket físico).
- Emulador (opcional complementario): captura de la escena virtual → panel debug presente (con texto o "(OCR vacío)"), sin crash.

E2E persistente: ver `:app:connectedDebugAndroidTest` arriba (no hay `test:e2e` web/JS en este repo). La feature cambia comportamiento visible en pantalla 05, por lo que se amplía `CapturaFlujoTest` (justificación de cobertura) sin tocar el flujo aceptado.

## Evidence To Capture

- JUnit XML de todos los tests (rutas `*/build/test-results/` y `*/build/outputs/androidTest-results/`): nombres y conteos de `ReconocerTextoCrudoTest`, `EscanearViewModelTest`, `MlKitOcrEngineTest` (emulador y Tab), `PhotoFileStoreTest`, `CapturaFlujoTest`, `NavigationTest`.
- `docs/evidence/ocr-mlkit/texto_crudo_1..4.txt` con los 4 textos crudos reales (insumo declarado para `extraccion-nube`/`extraccion-local`; desviación acordada de 5 a 4) + nota del contraste humano vs papel.
- Captura/screenshot del panel debug en pantalla 05 (`docs/evidence/ocr-mlkit-panel-debug-*.png`) + fragmento de `uiautomator dump` con el testTag `captura_texto_crudo`.
- Decisión de versión de ML Kit (catálogo) + confirmación de 0 versiones literales + confirmación de esquema Room sin cambios.
- Resultado de la regresión E2E (`CapturaFlujoTest` + `NavigationTest` 3/3).
- Todo lo anterior registrado en `feature_list.json` (evidence) y `PROGRESS.md` (sesión), con dispositivos citados explícitamente.

## Riesgos

- **R-OCR1 — Calidad en tickets reales** (R3 del repo): térmicos descoloridos,
  arrugas, fuentes pequeñas. Mitigación: fixtures reales con tokens esperados
  "razonables" (no exactos), flujo no bloqueante (OCR vacío → edición manual
  posterior) y contraste humano registrado.
- **R-OCR2 — Latencia del OCR en la captura**: el estado `Procesando` absorbe el
  tiempo del OCR (decenas de ms–segundos según dispositivo). Riesgo sobre los
  E2E existentes (esperas); mitigación: esperas amplias en `CapturaFlujoTest`.
  Si la latencia resultara molesta, el cambio a OCR en segundo plano (pantalla 05
  inmediata, panel que se rellena) es reversible sin tocar el contrato de dominio.
- **R-OCR3 — Tamaño de APK**: la variante bundled añade ~+6 MB. Aceptado por
  offline-first y determinismo (decisión registrada; alternativa unbundled =
  Play Services, descartada por depender de descarga/Play en el emulador).
- **R-OCR4 — `CancellationException`** (finding Low sesión 014): todo
  `runCatching` nuevo sobre suspend functions debe re-lanzarla; se corrige el
  existente al tocar `EscanearViewModel`.

## Open Questions (para el usuario)

Recomendaciones por defecto claramente marcadas como tales (**DEFAULT**). Las no
bloqueantes no frenan a T1; el implementador puede seguir con el default si el
usuario no responde antes de la tarea señalada.

1. **¿Cuándo se ejecuta el OCR?** — **DEFAULT: automático tras guardar la
   foto**, dentro del estado `Procesando` de la captura (coincide con
   `docs/domain-model.md`: "Foto capturada → Borrador (se lanza OCR…)" y con el
   comportamiento de la feature). Alternativa: botón "Ejecutar OCR" bajo demanda
   en pantalla 05 (más perezoso, pero retrasa la disponibilidad del texto para
   `procesado-ui`). *No bloquea T1–T6; afecta a T7.*
2. **¿El texto crudo persiste más allá del Borrador?** — **DEFAULT: solo con el
   Borrador en memoria + la columna existente `texto_crudo_ocr` al guardar el
   ticket** (`revision-datos`), sin ficheros sidecar. Es lo que ya modelan
   dominio y Room (esquema v1 sin migración) y es coherente con "los borradores
   no se persisten". Alternativa (lo narra `docs/technical-discovery.md`):
   fichero sidecar junto a la foto (`fotos/captura_<uuid>.txt`) que sobreviva a
   la muerte de la app sin re-OCR; exige ampliar el store y crea dos fuentes de
   verdad. *No bloquea T1.*
3. **¿Origen de las fotos reales de tickets?** — **DEFAULT (resuelto): fixtures
   reales** capturadas/aportadas por el autor, commiteadas con tabla de tokens
   esperados, más la captura en vivo de ≥1 ticket por el flujo completo como
   corroboración. Justificación: es re-ejecutable por el validador (precedente
   `CapturedPhotoProcessorTest` con fixture), y `extraccion-nube`/`extraccion-local`
   necesitan conservar los textos crudos reales como insumo.
   **Desviación acordada 2026-10-16**: se aportaron **4** fotos reales
   (`ticket_real_1..4.jpg` + `.tokens.txt`) en vez de 5 (verificación 1 = 4
   fotos).
4. **Superficie de "visible en debug"** (menor) — **DEFAULT: panel en pantalla 05
   solo en builds debug** (testTag `captura_texto_crudo`), verificable con
   `uiautomator`. Alternativa: solo `logcat`. *No bloquea.*

## Validator Checklist

- [x] Implementation stays within this feature's scope (texto crudo; sin extracción estructurada, sin `TextEngine`/router/procesado-ui, sin escrituras en Room).
- [x] Acceptance scenarios 1-7 pass.
- [x] Verification 1 (4 fotos reales; desviación acordada 2026-10-16 respecto a 5) con evidencia del contraste humano vs papel y verificación 2 (registrado con el borrador, visible en debug) con evidencia de UI/tests.
- [x] `OcrEngine` en dominio sin imports de Android/ML Kit; `MlKitOcrEngine` `internal` en `:framework:ai`; un solo `di.kt` por módulo; sin módulos Gradle nuevos.
- [x] Esquema Room sin cambios (`schemas/` sin diff); el texto crudo viaja por el campo/ columna ya existentes.
- [x] "Continuar" sigue deshabilitado; `NavigationTest` 3/3; `CapturaFlujoTest` en verde; `pantalla_escanear` intacto.
- [x] OCR vacío y fallo del motor no bloquean la captura (la foto se conserva); `CancellationException` re-lanzada en todo `runCatching` nuevo.
- [x] Versiones solo en `gradle/libs.versions.toml`; `init.sh` sin cambios y en verde.
- [x] Docs durables (`docs/architecture.md`, `docs/domain-model.md`, `docs/risks-and-open-questions.md`, `docs/technical-discovery.md`) actualizadas; `feature_list.json` y `PROGRESS.md` con evidencia no inventada (los 4 textos crudos reales en `docs/evidence/ocr-mlkit/`).
- [x] Persistent E2E coverage updated (`CapturaFlujoTest`) o justificado.
- [x] No unrelated product behavior or extra feature work was added.

Marcado en la validación independiente del 2026-10-16 (sesión 016): veredicto inicial `revise` por 2 findings Medium + 2 Low, reparados por el orquestador (evidencia de `captura-foto` restaurada desde HEAD; research task de ML Kit actualizada; tamaños de evidencia corregidos; spec alineado a 4 fotos; mensaje de skip del harness). Tras las reparaciones no hay cambios de producto y se persiste `accepted`.

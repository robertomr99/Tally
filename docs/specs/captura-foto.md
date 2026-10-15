# Feature Implementation Spec: Captura con encuadre guiado y foto guardada al instante

## Source Feature

- `id`: captura-foto
- `area`: captura
- `title`: Captura con encuadre guiado y foto guardada al instante
- `user_visible_behavior`: El usuario encuadra con la retícula guiada (mockup 04) y al capturar la foto (orientación corregida, comprimida ~1280 px) queda ya en filesDir aunque la app muera después.
- `depends_on`: ["navegacion-base"] (accepted → satisfecha)
- `status`: not_started (a fecha de planificación, 2026-10-15)
- `source`: `feature_list.json`
- `verification` (fuente):
  1. Foto de prueba visible en la pantalla 05 con orientación correcta.
  2. Unit test del pipeline de compresión y orientación EXIF con imagen de recursos.
- `notes` (fuente): Regla de dominio: la foto se persiste en el momento de la captura (el Borrador no se persiste). Mockups 04-05.

## Goal

Sustituir el placeholder de `:feature:escanear` por el flujo de captura real: encuadre guiado
(mockup 04, `docs/design/stitch/04-camara-encuadre/`) sobre preview de cámara en vivo, y
pantalla de revisión de la foto (mockup 05, `docs/design/stitch/05-revision-foto/`) que muestra
la foto ya procesada. Al pulsar el obturador, la foto se procesa (orientación EXIF horneada en
píxeles + escala a ~1280 px lado largo + JPEG ~85) y se escribe **en el momento de la captura**
en `filesDir/fotos/` con **nombre relativo**, de modo que sobrevive a la muerte de la app. El
Borrador (ruta relativa de la foto) vive solo en memoria hasta `revision-datos`.

## Non-Goals

- OCR, texto crudo ni extracción con IA (`ocr-mlkit`, `extraccion-nube`, `extraccion-local`).
- Pantalla de procesado (mockup 06, `procesado-ui`): el botón "Continuar" existe en pantalla 05
  pero queda **deshabilitado** hasta que esa feature lo cablee.
- Persistencia de Borrador ni escrituras en Room (`TicketRepository` no se toca).
- Listados, detalle ni guardado del ticket (`listado-gastos`, `detalle-ticket`, `revision-datos`).
- Decoración ficticia del mockup 04/05 prohibida por `DESIGN.md` (R4): chips "OCR 99,4 %",
  "3 conceptos y total identificados", badge "Foto nítida / Recorte automático", modos
  Auto/Manual, detector de luz, botón Galería, linterna/flash, ajuste de esquinas (crop),
  zoom/rotar. La retícula guiada (scrim + esquinas + hint) sí es normativa.
- Crear módulos Gradle nuevos (`:framework:camera` u otros): prohibido por AGENTS.md sin
  aprobación explícita del usuario.
- Migraciones de Room ni cambios de esquema.

## Job Story

When estoy delante del ticket de papel y quiero guardarlo como evidencia,
I want to encuadrarlo con la retícula guiada y capturarlo de un toque,
so I can tener la foto ya guardada y bien orientada aunque la app se cierre después.

## Users And Permissions

- Usuario único (sin cuentas; la posesión del dispositivo es la autenticación,
  `docs/technical-discovery.md`): puede abrir Escanear, dar permiso de cámara, capturar,
  revisar la foto y repetir la captura.
- Permiso `android.permission.CAMERA` en tiempo de ejecución. Si se denega, la pantalla muestra
  estado in-app en español con botón para volver a pedirlo (sin bucles ni crashes).
- Las fotos viven solo en almacenamiento privado (`filesDir/fotos/`); nada en MediaStore.

## Acceptance Scenarios

### Scenario 1: encuadre guiado y captura

Given la app abierta en la pestaña Escanear con permiso de cámara concedido
When el usuario alinea el ticket en la retícula (mockup 04) y pulsa el obturador
Then la foto se captura, se procesa (EXIF + ~1280 px) y se muestra la pantalla 05 con la foto.

### Scenario 2: la foto se persiste al instante (aunque la app muera)

Given una captura recién hecha
When se fuerza el cierre de la app (`adb shell am force-stop`) y se lista `filesDir/fotos/`
Then el fichero de la foto sigue existiendo en disco (con nombre relativo, sin ruta absoluta).

### Scenario 3: orientación EXIF corregida

Given la imagen de recursos con EXIF Orientation=6 (girada 90°)
When corre el pipeline sobre ella
Then la salida tiene los píxeles ya rotados (dimensiones intercambiadas) y sin tag de
orientación EXIF; la foto se ve derecha en pantalla 05.

### Scenario 4: compresión ~1280 px

Given una imagen de entrada mayor que 1280 px de lado largo
When corre el pipeline
Then la salida es JPEG con lado largo ≤ 1280 px (proporción conservada, calidad ~85) y el
original de cámara NO se guarda.

### Scenario 5: foto visible en pantalla 05

Given una captura completada
When la pantalla 05 (revisión de foto) está visible
Then se ve la foto capturada con orientación correcta, con "Repetir foto" operativo y
"Continuar" deshabilitado (testTags: `captura_revision`, `captura_foto`, `captura_repetir`,
`captura_continuar`).

### Scenario 6: permiso de cámara denegado

Given el permiso de cámara denegado (o revocado)
When el usuario abre Escanear
Then se muestra estado in-app con botón para reintentar el permiso; no hay crash y el testTag
`pantalla_escanear` sigue presente (regresión de `NavigationTest`).

### Scenario 7: repetir foto

Given la pantalla 05 con una foto ya capturada
When el usuario pulsa "Repetir foto"
Then se vuelve al encuadre, la foto del borrador reemplazado se borra en disco (best-effort) y
puede capturarse una nueva (quedando solo la nueva en `filesDir/fotos/`).

## Repository Research

### Files Inspected

- `AGENTS.md` — módulos, capa de presentación (Screen/ViewModel/State), navegación, testing, "qué no hacer" (sin módulos Gradle nuevos).
- `PROGRESS.md` — siguiente feature = `captura-foto`; los móviles físicos se reservan para captura-foto y Gemma.
- `feature_list.json` — alcance, verificación y nota de regla de dominio de esta feature.
- `docs/architecture.md` — diagrama de módulos y regla de dependencias.
- `docs/domain-model.md` — estados Borrador/Guardado; "la foto sí se guarda al capturar, el Borrador no se persiste"; foto en cascada al borrar; corrección EXIF.
- `CONTEXT.md` — "Foto original" = evidencia inmutable del ticket.
- `docs/build-brief.md` — MVP: "Captura: cámara con encuadre guiado y foto original guardada".
- `docs/technical-discovery.md` — fotos en `filesDir` (nada en MediaStore); compresión ~1280 px lado largo, JPEG ~85 tras corregir EXIF; la foto se persiste en la captura.
- `docs/risks-and-open-questions.md` — pregunta abierta "EXIF/rotación y compresión: decidir pipeline exacto al implementar" (se cierra aquí); R4 scope creep de UI.
- `docs/adr/0001` y `0002` — sin impacto directo (IA); no se contradicen.
- `DESIGN.md` + `docs/design/stitch/04-camara-encuadre/`, `05-revision-foto/` y `stitch-design-system.md` — normativo vs decoración ficticia (sección Do's and Don'ts).
- `framework/database/src/main/java/com/robertomr/tally/framework/database/source/PhotoFileStoreImpl.kt` — `delete` resuelve rutas relativas bajo `filesDir` y **acepta absolutas** (nota teórica de persistencia-room): al guardar se DEBEN usar nombres relativos.
- `domain/expenses/src/main/java/com/robertomr/tally/domain/expenses/data/PhotoFileStore.kt` — interfaz actual: solo `delete`.
- `feature/escanear/src/main/java/com/robertomr/tally/feature/escanear/EscanearScreen.kt` — placeholder con `testTag("pantalla_escanear")` que `NavigationTest` exige.
- `app/src/main/java/com/robertomr/tally/navigation/{TallyRoute,TallyApp}.kt` + `app/src/androidTest/.../NavigationTest.kt` — shell de 3 destinos; la bottom bar debe seguir visible en Escanear para no romper el E2E aceptado.
- `feature/common/.../theme/{Color,Type,Theme}.kt` y `TallyBottomBar.kt` — tokens Forest Precision; patrón de composables compartidos.
- `feature/{resumen,gastos}/...` — patrón de feature (Screen placeholder; este será el primer módulo feature con ViewModel real).
- `gradle/libs.versions.toml` + `build-logic/` — sin CameraX, sin exifinterface, sin `androidx.hilt:hilt-navigation-compose` (necesario para `hiltViewModel()`), sin `androidx.test:rules`; convention plugins `tally.android.library/compose/hilt`.
- `test/unit/src/main/kotlin/` — `FakeTicketDataSource`, `FakePhotoFileStore` (solo `delete`); no existe `CoroutineTestRule` pese a citarse en AGENTS.md.
- `framework/database/build.gradle.kts`, `domain/expenses/build.gradle.kts`, `settings.gradle.kts`, `app/build.gradle.kts`, `init.sh` — patrones de build y gates (`assembleDebug` + `testDebugUnitTest`; sin `connectedAndroidTest`).

### Existing Patterns To Follow

- Contratos en `domain/<área>/data/`, implementaciones `internal` en `framework/<área>/`, bindings en un `di.kt` por módulo (`framework/database/di/di.kt` ya provee `PhotoFileStore`).
- Casos de uso en `domain/<área>/usecases/` con `operator fun invoke` + `@Inject constructor` (será el primero del repo).
- Feature module = Screen + ViewModel (`@HiltViewModel`, `StateFlow`, sin Compose) + State (`rememberXState()`), flujo unidireccional, estado del VM derivado con operadores sobre `MutableStateFlow`.
- Tests: JVM (`src/test`) para lógica pura/con fakes de `:test:unit` + Turbine; instrumentados (`connectedDebugAndroidTest`) para APIs Android (precedente `PhotoFileStoreTest`).
- Versiones solo en `gradle/libs.versions.toml`; decisiones de versión nuevas documentadas en `PROGRESS.md` (patrón navigationCompose/Espresso).

### Current Gaps

- No hay CameraX ni `hiltViewModel()` disponibles en el catálogo: hay que añadirlos.
- `stateAsResultIn`, `AcScaffold` y `rememberXState` (AGENTS.md) **no existen aún en `:feature:common`**. Esta feature no tiene Flows de dominio (no aplica `Result`/`AcScaffold`); se aplica el patrón XState y se deja constancia en Unknowns.
- `CoroutineTestRule` citado en AGENTS.md no existe: se crea en `:test:unit` (necesario para tests de ViewModel con `Dispatchers.Main`).
- No hay harness E2E genérico; el E2E persistente del repo son los `connectedDebugAndroidTest` de `:app` (`NavigationTest`).

## Technical Approach

1. **Dominio (`:domain:expenses`)** — `data/PhotoProcessor.kt` (interfaz: `suspend fun process(rawJpeg: ByteArray): ByteArray`) y ampliación de `PhotoFileStore` con `suspend fun save(jpeg: ByteArray): String` que devuelve **nombre relativo bajo `filesDir`** (p.ej. `fotos/captura_<uuid>.jpg`; contrato: nunca absoluto — restricción de seguridad heredada de `PhotoFileStoreImpl`). Caso de uso `usecases/GuardarFotoCapturada` (`@Inject constructor(PhotoProcessor, PhotoFileStore)`, `operator fun invoke(rawJpeg): String` = process → save). **No se toca Room** ni `TicketRepository`.
2. **Framework (`:framework:database`)** — `source/CapturedPhotoProcessor.kt` (`internal`, implementa `PhotoProcessor`; en `Dispatchers.IO`): lee EXIF (`androidx.exifinterface`), rota píxeles con Matrix, escala a ≤1280 px lado largo, recomprime JPEG ~85 sin tag de orientación. Matemática pura (dimensiones objetivo + mapeo EXIF→grados) en objeto interno testeable desde JVM. `PhotoFileStoreImpl.save` escribe bajo `filesDir/fotos/` (creando dir) y devuelve la ruta relativa. Bindings en `di/di.kt`. Se mantiene aquí (junto al `PhotoFileStoreImpl` existente) porque crear `:framework:camera` exige aprobación del usuario; la costura CameraX queda en la capa de UI.
3. **Feature (`:feature:escanear`)** — `EscanearViewModel` (`@HiltViewModel`, recibe `GuardarFotoCapturada`): estado `StateFlow<CapturaUiState>` (sealed: PermisoPendiente / Encuadre / Procesando / Revision(rutaFoto) / Error) derivado con `flatMapLatest`/`combine` sobre `MutableStateFlow` de entrada (los bytes JPEG entran por `onImageCapturado(bytes)`; sin funciones `load()` imperativas). `EscanearScreen` conserva `testTag("pantalla_escanear")` en la raíz (regresión `NavigationTest`), gate de permiso in-app (sin diálogo del sistema al entrar: botón "Permitir cámara" que lanza el launcher), subflujo interno Encuadre↔Revisión con `BackHandler` (**sin rutas nuevas en `:app`**: la pantalla 05 es un estado de `:feature:escanear`; cuando llegue `procesado-ui` se decidirá si convierte el flujo en ruta). `CameraEncuadre.kt` (mockup 04): `AndroidView(PreviewView)` + CameraX `ImageCapture`, retícula = scrim 65 % + esquinas + hint "Alinea los bordes del ticket dentro del recuadro", obturador y "Cerrar". `RevisionFotoContent.kt` (mockup 05): foto decodificada con `BitmapFactory` → `ImageBitmap` (sin librería de imágenes), dock "Repetir foto" (borra la foto del borrador reemplazado, best-effort, vía `PhotoFileStore.delete`) + "Continuar" deshabilitado.
4. **Permisos y build** — `uses-permission CAMERA` en `feature/escanear/src/main/AndroidManifest.xml` (se mergea en `:app`); CameraX (`camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`) solo en `:feature:escanear`; `hilt-navigation-compose` para `hiltViewModel()`; `activity-compose` para el launcher de permisos. Versiones nuevas fijadas en el catálogo (ver Unknowns).
5. **Restricción de seguridad (explícita)** — todo lo que se guarde en `filesDir` usa **nombres relativos**; el pipeline nunca escribe rutas absolutas en la salida del caso de uso. Verificable por test (`save` devuelve relativo) y por revisión de código.

## Expected File Changes

- `gradle/libs.versions.toml` — modify; + `cameraX` (camera-core/camera-camera2/camera-lifecycle/camera-view), + `exifinterface`, + `hiltNavigationCompose` (`androidx.hilt:hilt-navigation-compose` para `hiltViewModel()`), + `androidxTestRules` (`androidx.test:rules`).
- `framework/database/build.gradle.kts` — modify; + `exifinterface`; + `testImplementation(libs.junit)` si falta.
- `framework/database/src/main/java/com/robertomr/tally/framework/database/source/PhotoFileStoreImpl.kt` — modify; + `save` bajo `filesDir/fotos/` con nombre relativo.
- `framework/database/src/main/java/com/robertomr/tally/framework/database/source/CapturedPhotoProcessor.kt` — create; pipeline EXIF+escala+JPEG (`internal`).
- `framework/database/src/main/java/com/robertomr/tally/framework/database/photo/PhotoPipelineMath.kt` — create; matemática pura del pipeline (JVM-testable).
- `framework/database/src/main/java/com/robertomr/tally/framework/database/di/di.kt` — modify; + `@Provides PhotoProcessor`.
- `framework/database/src/test/java/com/robertomr/tally/framework/database/photo/PhotoPipelineMathTest.kt` — create; unit test JVM.
- `framework/database/src/androidTest/java/com/robertomr/tally/framework/database/CapturedPhotoProcessorTest.kt` — create; test instrumentado con imagen de recursos (verificación 2).
- `framework/database/src/androidTest/assets/ticket_exif_rotado.jpg` — create; fixture <100 KB con EXIF Orientation=6 (generar una vez y commitear).
- `framework/database/src/androidTest/java/com/robertomr/tally/framework/database/PhotoFileStoreTest.kt` — modify; + tests de `save` (nombre relativo, fichero en `filesDir`, roundtrip save→delete).
- `domain/expenses/src/main/java/com/robertomr/tally/domain/expenses/data/PhotoFileStore.kt` — modify; + `save` con contrato de ruta relativa.
- `domain/expenses/src/main/java/com/robertomr/tally/domain/expenses/data/PhotoProcessor.kt` — create; interfaz del pipeline.
- `domain/expenses/src/main/java/com/robertomr/tally/domain/expenses/usecases/GuardarFotoCapturada.kt` — create; caso de uso (primero del repo en `usecases/`).
- `domain/expenses/src/test/java/com/robertomr/tally/domain/expenses/usecases/GuardarFotoCapturadaTest.kt` — create; unit test con fakes.
- `test/unit/src/main/kotlin/com/robertomr/tally/test/unit/FakePhotoFileStore.kt` — modify; + `save` (registra bytes, devuelve nombre configurable).
- `test/unit/src/main/kotlin/com/robertomr/tally/test/unit/FakePhotoProcessor.kt` — create.
- `test/unit/src/main/kotlin/com/robertomr/tally/test/unit/CoroutineTestRule.kt` — create; rule de `Dispatchers.Main` (AGENTS.md testing).
- `feature/escanear/build.gradle.kts` — modify; + `id("tally.hilt")` y deps (`:domain:expenses`, `hilt-navigation-compose`, `activity-compose`, CameraX ×4, `material-icons-extended`; test: `:test:unit`, junit, turbine, coroutines-test).
- `feature/escanear/src/main/AndroidManifest.xml` — modify; + `uses-permission android.permission.CAMERA`.
- `feature/escanear/src/main/java/com/robertomr/tally/feature/escanear/EscanearScreen.kt` — modify; raíz con `testTag("pantalla_escanear")`, gate de permiso, subflujo Encuadre|Revisión.
- `feature/escanear/src/main/java/com/robertomr/tally/feature/escanear/EscanearViewModel.kt` — create.
- `feature/escanear/src/main/java/com/robertomr/tally/feature/escanear/EscanearState.kt` — create; `rememberEscanearState()`.
- `feature/escanear/src/main/java/com/robertomr/tally/feature/escanear/CameraEncuadre.kt` — create; mockup 04 + binding CameraX.
- `feature/escanear/src/main/java/com/robertomr/tally/feature/escanear/RevisionFotoContent.kt` — create; mockup 05.
- `feature/escanear/src/test/java/com/robertomr/tally/feature/escanear/EscanearViewModelTest.kt` — create; JVM con Turbine + fakes.
- `app/build.gradle.kts` — modify; + `androidTestImplementation(libs.androidx.test.rules)`.
- `app/src/androidTest/java/com/robertomr/tally/captura/CapturaFlujoTest.kt` — create; E2E enfocado (ver Verification Plan).
- `docs/specs/captura-foto.md` — create (este documento).
- `docs/architecture.md` — update; sección de captura: pipeline foto → `filesDir/fotos/` con nombre relativo; CameraX vive en `:feature:escanear`.
- `docs/domain-model.md` — update; "Repetir foto" borra la foto del borrador reemplazado (best-effort); la foto de un ticket Guardado solo se borra en cascada.
- `docs/risks-and-open-questions.md` — update; cerrar la pregunta "EXIF/rotación y compresión" con el pipeline decidido (1280 px, JPEG ~85, rotación horneada).
- `PROGRESS.md` y `feature_list.json` — update **al terminar implementación y verificación** (evidencia); no aprobados por este spec.

## Visual Design Impact

- UI involved: sí. Design source: `DESIGN.md` + `docs/design/stitch/04-camara-encuadre/` y `05-revision-foto/` (normativo estructura/proporciones; el HTML/Tailwind se traduce a Material 3 Compose).
- Pantallas/estados: encuadre de cámara (mockup 04) y revisión de foto (mockup 05); estados de permiso denegado y de error de captura (copy en español claro, paleta Forest de `feature/common/theme`).
- Normativo a respetar: scrim ~65 % fuera de la zona de captura, marcas de esquina del encuadre, hint "Alinea los bordes del ticket dentro del recuadro", obturador circular 80dp, dock inferior "Repetir foto" (outlined) + "Continuar" (primary), ≥44dp táctil, Inter/tokens del tema `TallyTheme`.
- Decisiones explícitas: la bottom bar permanece visible (los mockups 04/05 la ocultan, pero ocultarla rompería `NavigationTest` de navegacion-base y el shell aceptado); el dock de pantalla 05 se coloca por encima de la bottom bar. Decoración ficticia NO implementada (ver Non-Goals).
- New design artifact required: no.

## Durable Documentation Impact

- `ARCHITECTURE.md` (raíz): not needed — no existe; el equivalente es `docs/architecture.md`.
- `CONSTRAINTS.md` (raíz): not needed — no existe; la restricción crítica (rutas relativas en `filesDir`) queda en el contrato de `PhotoFileStore`, en `docs/architecture.md` y en este spec.
- `AGENTS.md`: not needed — no cambian el flujo de trabajo ni las reglas de módulos.
- `docs/architecture.md`: update — nueva sección de captura/pipeline de foto.
- `docs/domain-model.md`: update — regla de "Repetir foto" sobre la foto del borrador.
- `docs/risks-and-open-questions.md`: update — pregunta EXIF/compresión cerrada con decisión.
- `PROGRESS.md` + `feature_list.json`: update al finalizar (evidencia), no ahora.

## Implementation Plan

1. Catálogo + build files + permiso de cámara (base compila).
2. Dominio: `PhotoProcessor`, `PhotoFileStore.save`, `GuardarFotoCapturada` + tests JVM con fakes.
3. Framework: `PhotoPipelineMath` + `CapturedPhotoProcessor` + `PhotoFileStoreImpl.save` + `di.kt` + tests (JVM + instrumentado con fixture).
4. Feature: ViewModel/State + UI encuadre (CameraX) + pantalla 05 + test del ViewModel.
5. E2E enfocado + verificación en emulador y dispositivos físicos + docs + evidencia.

## Implementation Tasks

- [x] T1 `gradle/libs.versions.toml` con cameraX (core/camera2/lifecycle/view), exifinterface, hiltNavigationCompose, androidxTestRules; 0 versiones literales en `**/*.gradle.kts`; decisión de versiones registrada.
- [x] T2 `framework/database/build.gradle.kts` (+exifinterface), `feature/escanear/build.gradle.kts` (+tally.hilt + deps), `app/build.gradle.kts` (+androidx-test-rules), `feature/escanear/src/main/AndroidManifest.xml` (+CAMERA).
- [x] T3 Dominio: `PhotoProcessor.kt`, `PhotoFileStore.save`, `usecases/GuardarFotoCapturada.kt`.
- [x] T4 `:test:unit`: `FakePhotoProcessor.kt`, `CoroutineTestRule.kt`, `FakePhotoFileStore.save`.
- [x] T5 `GuardarFotoCapturadaTest` (JVM): process→save en orden, devuelve nombre relativo, errores del processor se propagan.
- [x] T6 Framework: `PhotoPipelineMath.kt` + `CapturedPhotoProcessor.kt` + `PhotoFileStoreImpl.save` + binding en `di/di.kt`.
- [x] T7 `PhotoPipelineMathTest` (JVM): tablas de escalado (≤1280 no escala, >1280 escala conservando proporción) y mapeo EXIF→grados.
- [x] T8 Fixture `ticket_exif_rotado.jpg` (EXIF Orientation=6) + `CapturedPhotoProcessorTest` (androidTest): píxeles rotados, lado largo ≤1280, sin tag de orientación; `PhotoFileStoreTest` ampliado (save→relativo→existe→delete).
- [x] T9 Feature: `EscanearState.kt`, `EscanearViewModel.kt` (estado derivado con operadores; `onImageCapturado` → `GuardarFotoCapturada`) + `EscanearViewModelTest` (Turbine + CoroutineTestRule: permiso, procesando, revisión, repetir borra la foto anterior).
- [x] T10 Feature UI: `CameraEncuadre.kt` (mockup 04 + CameraX), `RevisionFotoContent.kt` (mockup 05), `EscanearScreen.kt` (raíz con `pantalla_escanear`, gate de permiso, BackHandler, dock con Continuar deshabilitado).
- [x] T11 `CapturaFlujoTest` (E2E en `:app/src/androidTest`) y regresión `NavigationTest` 3/3 en verde.
- [x] T12 Verificación manual (emulador + realme GT 2 / Tab S9+), docs durables actualizadas, evidencia en `feature_list.json`/`PROGRESS.md`. — EMULADOR + docs + evidencia COMPLETADOS 2026-10-15 (escenarios 2/5/6 con screenshots y `run-as`/`force-stop`); FÍSICO PENDIENTE de revisión humana: la Tab S9+ (SM-X810) está conectada por ADB pero con pantalla bloqueada que no despierta vía adb (lockscreen seguro, batería al 8% sin cargar) — en ella sí pasaron los 17 tests instrumentados del pipeline/almacenamiento — y el realme GT 2 no está conectado a esta máquina. Pasos para la revisión humana: encender/desbloquear el dispositivo, `adb install -r`, `pm grant ... CAMERA`, capturar un ticket de papel real (vertical y girado), comprobar pantalla 05 derecha + persistencia tras `force-stop`, guardar screenshots `captura-foto-05-realme.png` / `captura-foto-05-tab.png`.

## Verification Plan

Automatizada (Windows: `gradlew.bat`; emulador `emulator-5554`):

- `gradlew.bat assembleDebug` → BUILD SUCCESSFUL (regresión base).
- `gradlew.bat testDebugUnitTest` → BUILD SUCCESSFUL (incluye `PhotoPipelineMathTest`, `GuardarFotoCapturadaTest`, `EscanearViewModelTest`).
- `gradlew.bat :test:unit:test --rerun`, `gradlew.bat :domain:expenses:test --rerun`, `gradlew.bat :framework:database:testDebugUnitTest` → verdes; revisar JUnit XML.
- `ANDROID_SERIAL=emulator-5554 gradlew.bat :framework:database:connectedDebugAndroidTest` → `CapturedPhotoProcessorTest` (imagen de recursos: verificación 2 de la feature) + `PhotoFileStoreTest` en verde.
- `ANDROID_SERIAL=emulator-5554 gradlew.bat :app:connectedDebugAndroidTest` → `NavigationTest` 3/3 (regresión: `pantalla_escanear` intacto) + `CapturaFlujoTest` (E2E enfocado: permiso con `GrantPermissionRule` o `adb shell pm grant com.robertomr.tally android.permission.CAMERA` previo, encuadre visible, obturador → pantalla 05 con foto, "Repetir foto" → encuadre). Si la cámara virtual del emulador no entrega frames (limitación conocida), el test valida los estados de UI y la captura real se acepta con evidencia en dispositivo físico (criterio de PROGRESS: Tab S9+ y realme GT 2 reservados a captura-foto).
- `bash init.sh` (o equivalente): `assembleDebug` + `testDebugUnitTest` en verde; **no** debe cambiar ni ejecutar `connectedAndroidTest` ni arrancar procesos largos.
- Búsqueda de regresión de catálogo: 0 versiones literales en `**/*.gradle.kts`; 0 rutas absolutas devueltas por `save` (test + revisión).

Manual / dispositivos:

- Emulador: `adb install -r`; conceder y denegar permiso (estados de Scenario 6); captura de escena virtual → screenshot de pantalla 05 (`adb exec-out screencap -p > docs/evidence/captura-foto-05-emulador.png`); `adb shell run-as com.robertomr.tally ls files/fotos` → fichero presente; `adb shell am force-stop com.robertomr.tally` + relistar → el fichero persiste (Scenario 2).
- **Físico (obligatorio para aceptación: realme GT 2 y/o Tab S9+)**: capturar un ticket de papel real en vertical y en horizontal/girado; comprobar en pantalla 05 que la foto sale derecha (Scenario 3 end-to-end); repetir el check de persistencia tras force-stop; screenshots + salidas de `adb` como evidencia.
- E2E persistente: ver `:app:connectedDebugAndroidTest` arriba. No hay `test:e2e` web/JS en este repo; el E2E de Android es la vía persistente y esta feature cambia comportamiento visible, por lo que se añade `CapturaFlujoTest` (justificación de cobertura) sin tocar el flujo de navegación aceptado.

## Evidence To Capture

- JUnit XML de todos los tests (rutas `*/build/test-results/` y `*/build/outputs/androidTest-results/`): nombres de test y conteos.
- Salida de `CapturedPhotoProcessorTest` con dims de entrada/salida y assert de EXIF (verificación 2).
- `docs/evidence/captura-foto-05-emulador.png` y capturas en físico (`captura-foto-05-realme.png`, `captura-foto-05-tab.png`): foto en pantalla 05 con orientación correcta (verificación 1; revisión visual humana, el agente no inspecciona imágenes).
- Salida de `adb shell run-as ... ls files/fotos` antes/después de `force-stop` (Scenario 2) y nota de ruta relativa.
- Resultado de `CapturaFlujoTest` y `NavigationTest` 3/3 (regresión).
- Decisiones de versiones del catálogo (cameraX, exifinterface, hilt-navigation-compose, test-rules) + confirmación de 0 versiones literales.
- Todo lo anterior registrado en `feature_list.json` (evidence) y `PROGRESS.md` (sesión), con mención explícita a los dispositivos físicos usados.

## Validator Checklist

- [x] Alcance respetado: sin OCR/IA, sin Room/Borrador persistido, sin decoración ficticia, "Continuar" deshabilitado, sin módulos Gradle nuevos.
- [x] Escenarios 1-7 cumplidos; verificación 1 (foto en pantalla 05 con orientación correcta) con evidencia en **dispositivo físico** (o justificación explícita si falta) y verificación 2 (pipeline con imagen de recursos) automatizada en verde.
- [x] `save` devuelve siempre ruta relativa bajo `filesDir` (restricción de seguridad); ningún fichero escrito fuera de `filesDir`.
- [x] `NavigationTest` sigue 3/3 y `pantalla_escanear` sigue presente en todos los estados de `EscanearScreen`.
- [x] Tests del pipeline cubren EXIF (fixture girada) y escala (~1280 px); `GuardarFotoCapturadaTest` prueba persistencia en la captura; `EscanearViewModelTest` con fakes de `:test:unit`.
- [x] Versiones solo en `gradle/libs.versions.toml`; decisiones registradas; `init.sh` sin cambios y en verde.
- [x] Docs durables (`docs/architecture.md`, `docs/domain-model.md`, `docs/risks-and-open-questions.md`) actualizadas; `feature_list.json` y `PROGRESS.md` con evidencia no inventada.
- [x] Sin trabajo de features ajenas.

Marcado por el feature-validator el 2026-10-15 (sesión 014 de PROGRESS.md): veredicto **accept**; 4 findings Low no bloqueantes (revisión visual humana pendiente; deriva del estado final de iles/fotos en la Tab S9+; unCatching que traga CancellationException en EscanearViewModel; nits de estilo AGENTS.md en EscanearState/VM). Gates re-ejecutados por el validador en verde.

## Unknowns (explícitos)

- Versiones exactas a fijar para CameraX, exifinterface, `androidx.hilt:hilt-navigation-compose` y `androidx.test:rules`: el implementer elige la última estable compatible con compileSdk 36 y lo registra (patrón navigationCompose). Orientación: líneas 1.4.x/1.5.x (CameraX), 1.3.x/1.4.x (exifinterface), 1.2.x (hilt-navigation-compose).
- `stateAsResultIn`/`AcScaffold` no existen en `:feature:common`: esta feature no los necesita (sin Flows de dominio) y no debe inventarlos; quedan para la primera feature con streams de dominio (p.ej. `listado-gastos`).
- La cámara del emulador es simulada: la captura E2E puede requerir dispositivo físico para cerrar verificación 1 (criterio ya reservado en PROGRESS para esta feature).
- Si en el futuro se quiere CameraX en un `:framework:camera`, requiere aprobación del usuario (AGENTS.md); este spec deja el seam `PhotoProcessor`/`PhotoFileStore` para facilitar esa extracción.
- Subflujo Encuadre↔Revisión sin ruta propia en `:app`: reversible cuando `procesado-ui` necesite navegación real entre pantallas.

## Implementation Notes (sesión 012, 2026-10-15)

Desviaciones y hallazgos al implementar/verificar (para el feature-validator):

1. **`CapturaFlujoTest` sin `GrantPermissionRule`**: revocar un permiso runtime
   en caliente (`pm revoke`) mata el proceso de la app bajo instrumentación
   (comportamiento de Android; reproducido: logcat `Killing 5629:com.robertomr.tally
   (adj 0): permissions revoked` 2026-10-15 18:07:02 → "Process crashed" y la
   tanda entera se aborta). Diseño final: `@FixMethodOrder(NAME_ASCENDING)` con
   `denegadoMuestraEstadoInAppSinCrash` primero (sobre el estado limpio de
   instalación, sin grants previos) y `pm grant` (seguro, no mata el proceso)
   en los tests que necesitan permiso, antes de navegar a Escanear (el gate se
   evalúa al componer `EscanearScreen`, no al arrancar la actividad). El test
   de denegado usa `assumeTrue` como guarda si otro test concedió ya el permiso.
   La dependencia `androidx.test:rules` se mantiene en el catálogo (T2) aunque
   el test final no use `GrantPermissionRule`.
2. **Corrección de assert en `CapturedPhotoProcessorTest`**: el píxel opuesto
   del fixture es blanco (fondo); el assert "no debería ser rojo" comparaba
   `Color.red < 120` y confundía blanco con rojo. Corregido a "no
   predominantemente rojo" (mismo criterio que el assert positivo). La
   orientación y las dimensiones ya salían correctas.
3. **`EscanearViewModelTest.withoutPermissionStateStaysPending`**: `estado` es
   `StateFlow` (conflación por igualdad) y sin permiso el valor no cambia, así
   que no hay emisión nueva que consumir; el test espera a que la captura
   termine y comprueba `estado.value` + `expectNoEvents()`.
4. **La cámara virtual del emulador SÍ entrega frames**: el E2E completo
   (obturador → pantalla 05 con foto, "Continuar" deshabilitado, "Repetir foto"
   → encuadre) pasa sin assumption-skip en `emulator-5554`; la captura real
   quedó como `filesDir/fotos/captura_<uuid>.jpg` de 960×1280 (lado largo 1280).
5. **Verificación física pendiente de revisión humana** (ver T12): Tab S9+
   conectada pero con pantalla bloqueada que no despierta vía adb (los 17 tests
   instrumentados no-UI del pipeline pasan en ella); realme GT 2 no conectado. -> RESUELTO en sesion 013 (2026-10-15):
6. **Verificacion fisica completada en Tab S9+ (SM-X810, Android 16/API 36)**:
   instalacion `gradlew.bat :app:installDebug` + `pm grant ... CAMERA` + arranque
   COLD (895 ms); captura vertical de ticket de papel real -> pantalla 05
   (`captura-foto-05-tab.png`), foto `fotos/captura_79d34873-...jpg` (363972 b,
   JPEG 960x1280, 0 segmentos EXIF APP1); persistencia tras `force-stop` (mismo
   fichero); captura con rotacion forzada (`user_rotation 1`) -> pantalla 05
   (`captura-foto-05-tab-girada.png`), foto `captura_7e753987-...jpg` (262304 b,
   JPEG 1280x960, dimensiones intercambiadas, 0 EXIF) = Scenario 3 end-to-end;
   "Repetir foto" borra el borrador reemplazado (Scenario 7) y la captura
   posterior solo deja la nueva. Rotacion automatica restaurada. Detalle y
   comandos exactos en `feature_list.json` (evidencia 2026-10-15 T12) y
   PROGRESS.md sesion 013. Pendiente solo revision visual de los PNG/JPG por
   mano humana (el agente no puede inspeccionar imagenes).

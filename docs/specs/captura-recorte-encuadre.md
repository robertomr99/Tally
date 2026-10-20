# Feature Implementation Spec: Recorte de la foto al área del encuadre guiado

## Source Feature

- `id`: captura-recorte-encuadre
- `area`: captura
- `title`: Recorte de la foto al área del encuadre guiado
- `user_visible_behavior`: Al capturar, la foto guardada corresponde al área
  interior del recuadro guiado (no a todo el fotograma): el ticket ocupa más
  píxeles y conserva detalle en la pantalla 05 y en la extracción posterior.
- `depends_on`: ["captura-foto"] (accepted, sesión 014 → satisfecha)
- `status`: not_started (a fecha de planificación, 2026-10-17)
- `source`: `feature_list.json`
- `verification` (fuente):
  1. La foto resultante (pantalla 05) contiene el área del recuadro y el ticket ocupa una proporción claramente mayor que hoy.
  2. El recorte respeta la orientación EXIF horneada y el lado largo de 1280 px.
  3. Unit test del mapeo recuadro→imagen (escala/proporción).
  4. E2E de captura sigue en verde (encuadre → pantalla 05 con foto) y regresión `NavigationTest`.
- `notes` (fuente): el recuadro del mockup 04 hoy NO recorta; el crop era Non-Goal explícito de `captura-foto` (spec, línea 36). Feature nueva, no bug.

## Goal

Que la foto guardada al capturar corresponda al **área interior del recuadro
guiado** (mockup 04), no al fotograma completo. Hoy `CameraEncuadre.capturar()`
llama a `ImageCapture.takePicture` sin `ViewPort` ni recorte y
`CapturedPhotoProcessor` reescala el fotograma entero a ≤1280 px de lado largo:
el ticket ocupa una fracción pequeña de la imagen y pierde detalle, y el hint
"Alinea los bordes del ticket dentro del recuadro" no tiene efecto real. Al
recortar al recuadro, el ticket ocupa más píxeles y mejora tanto la pantalla 05
como la calidad del texto que entra en OCR y en la extracción.

La retícula debe seguir pintándose **a pantalla completa** (preview del fotograma
entero + scrim + esquinas), no un viewfinder que ya venga recortado: el usuario
debe seguir viendo el ticket completo y el encuadre alrededor.

## Non-Goals

- Cambiar el pipeline EXIF/compresión (sigue: hornear orientación → ≤1280 px lado
  largo → JPEG ~85, sin tag de orientación).
- Detección automática de bordes/esquinas del ticket, enderezado (deskew), ajuste
  manual de esquinas, filtros de luz, flash, linterna (decoración ficticia
  del mockup 04, prohibida por `DESIGN.md`). La galería ya no es non-goal: ver
  `galeria-escaneo` (foto completa vía `CropRegion.Completo`, sin recorte).
- Cambiar el contrato `PhotoFileStore.save` (sigue devolviendo `fotos/captura_<uuid>.jpg`
  relativo bajo `filesDir`).
- Convertir la preview en un viewfinder recortado/zoomeado: el encuadre sigue
  mostrando el fotograma completo con scrim (mockup 04).
- Rutas nuevas en `:app`, módulos Gradle nuevos, dependencias nuevas.
- OCR, extracción, Room, navegación.

## Job Story

When enfoco el ticket con la retícula del mockup 04,
I want que la foto que se guarda sea exactamente lo que veo dentro del recuadro,
so I can que el ticket salga nítido y legible sin tener que llenar yo la pantalla
ni acercarme más de la cuenta.

## Users And Permissions

- Usuario único (sin cuentas). Sin permisos Android nuevos (CAMERA ya está).

## Acceptance Scenarios

### Scenario 1: la foto corresponde al recuadro (happy path)

Given la app con permiso de cámara y el encuadre guiado en pantalla
When el ticket está alineado dentro del recuadro y el usuario pulsa el obturador
Then la imagen procesada que se muestra en la pantalla 05 contiene el área
interior del recuadro (mismo encuadre que se ve en la preview) y el ticket ocupa
una proporción claramente mayor que antes del cambio.

### Scenario 2: orientación y tamaño correctos

Given una captura con el dispositivo girado (p. ej. `user_rotation 1`)
When se procesa la foto recortada
Then el JPEG resultante tiene la orientación horneada correcta, lado largo = 1280 px
(proporción del recuadro conservada) y 0 segmentos EXIF APP1.

### Scenario 3: sin frames / captura fallida

Given la cámara virtual del emulador sin frames o un error de captura
When se pulsa el obturador
Then el flujo muestra el estado de error de captura de `captura-foto` (sin crash),
igual que hoy.

### Scenario 4: regresión

Given la suite existente
Then `CapturaFlujoTest` (encuadre → pantalla 05 con foto, "Repetir foto" → encuadre)
y `NavigationTest` siguen en verde; `ProcesadoFlujoTest` sigue en verde.

### Scenario 5: la preview no se recorta ni se zoomea

Given la pantalla de encuadre tras el cambio
When la cámara está lista
Then la preview sigue mostrando el fotograma completo a pantalla completa con el
scrim ~65 % y las esquinas (mockup 04), es decir, el recuadro NO se convierte en
un viewfinder que ocupe toda la vista ni la preview se amplía al área del
recuadro; el comportamiento observable del encuadre es el de hoy salvo por el
recorte de la foto resultante.

### Scenario 6: vía de recorte determinista si la API de ViewPort no sirve

Given que la API real de CameraX 1.6.2 no permite fijar un sub-rect en el
`ViewPort` sin recortar/zoomear la preview (ver Gate de API)
When se implementa el recorte
Then se aplica el recorte en el pipeline de la foto (`CapturedPhotoProcessor`)
con la geometría pura del recuadro, de modo que el resultado observable
(escenarios 1-3) es el mismo y la preview no cambia (escenario 5).

## Repository Research

### Files Inspected

- `feature/escanear/.../CameraEncuadre.kt` — preview CameraX (`PreviewView`,
  `FILL_CENTER`), `ImageCapture` (`CAPTURE_MODE_MINIMIZE_LATENCY`), binding con
  `provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)`
  (SIN `UseCaseGroup` ni `ViewPort`); `capturar()` dispone `takePicture` a un
  `ByteArrayOutputStream` y entrega bytes crudos; la retícula y
  `calcularZonaCaptura(size, density)` (líneas ~252-259) definen el rectángulo del
  recuadro (margen 24dp, ancho ≤320dp, alto ≤68vh, centrado) **solo para pintar el
  scrim**.
- `feature/escanear/.../EscanearScreen.kt` — `CapturaUiState.Encuadre ->
  CameraEncuadre(onCapturado, onCapturaFallida, onCerrar)`; el `NavController` no
  entra; subflujo interno.
- `feature/escanear/.../EscanearViewModel.kt` — `onImageCapturado(bytes)` →
  `GuardarFotoCapturada(bytes)` → estado `Revision(rutaFoto, textoCrudo)`.
- `domain/expenses/.../data/PhotoProcessor.kt` — `interface PhotoProcessor { suspend fun process(rawJpeg: ByteArray): ByteArray }`.
- `domain/expenses/.../usecases/GuardarFotoCapturada.kt` — `process(rawJpeg)` →
  `photoFileStore.save(bytes)`; devuelve ruta relativa.
- `framework/database/.../source/CapturedPhotoProcessor.kt` — EXIF →
  `PhotoPipelineMath.gradosParaOrientacion` → `dimensionesObjetivo` → decode
  escalado → `Matrix().postRotate` → JPEG 85.
- `framework/database/.../photo/PhotoPipelineMath.kt` — `MAX_LADO_LARGO = 1280`,
  `CALIDAD_JPEG = 85`, `gradosParaOrientacion`, `dimensionesObjetivo`,
  `inSampleSize` (Kotlin puro, testeado JVM).
- `app/src/androidTest/.../captura/CapturaFlujoTest.kt` — E2E de captura (cámara
  virtual; `pm grant`; encuadre → pantalla 05).
- `docs/specs/captura-foto.md` — Non-Goal "ajuste de esquinas (crop)" (línea 36);
  pipeline y contrato.
- `DESIGN.md` + `docs/design/stitch/04-camara-encuadre/` — retícula normativa
  (scrim ~65 %, esquinas, hint, obturador 80dp).
- No inspeccionado visualmente: `04-camara-encuadre/screen.png` (el agente no ve imágenes).
- CameraX **1.6.2** (`gradle/libs.versions.toml`): `ViewPort` recorta `ImageCapture`
  (documentado: "for on-disk ImageCapture, the image is cropped before saving"),
  pero el `ViewPort.Builder` reciente se construye con el aspect ratio +
  `setScaleType`/`setLayoutDirection` (crop derivado) y **no** con un `setRect`
  arbitrario, y la `Preview` del mismo `UseCaseGroup` comparte el crop (puede
  zoomear la preview). Fuente: código de `androidx.camera.core.ViewPort`
  (androidx-main) consultado 2026-10-17; la superficie exacta de **1.6.2** se
  confirma en el Gate de API (T0).

### Existing Patterns To Follow

- Subflujo interno de `:feature:escanear` (sin rutas nuevas).
- CameraX ya vive en la capa de UI (`CameraEncuadre`); no existe `:framework:camera`
  (crearlo exige aprobación explícita).
- Pipeline EXIF/escala puro y testeable (`PhotoPipelineMath`), con tests JVM y
  `CapturedPhotoProcessorTest` (androidTest, fixture `ticket_exif_rotado.jpg`).

### Current Gaps

- `bindToLifecycle` no usa `UseCaseGroup`/`ViewPort`: la captura no se recorta.
- `calcularZonaCaptura` es `private` dentro de un composable y depende de
  `Size`/`Density` de Compose: su matemática no es testeable desde JVM hoy.
- No está confirmado que CameraX 1.6.2 permita fijar el rect del recuadro en el
  `ViewPort` sin recortar/zoomear la `Preview` (ver Gate de API en Technical
  Approach); el plan debe ser robusto a esa incertidumbre.
- No existe ninguna función pura que mapee el rect del recuadro (coordenadas de
  vista) a la imagen capturada teniendo en cuenta el `FILL_CENTER` de la
  `PreviewView`.

## Technical Approach

Objetivo: que el área interior del recuadro (la que pinta `calcularZonaCaptura`)
sea la que se guarda, sin cambiar el pipeline EXIF/≤1280 ni el contrato
`PhotoFileStore.save`, y sin convertir la preview en un viewfinder recortado.

### Gate de API (T0, obligatorio antes de codificar)

El enfoque `ViewPort` es version-dependiente y hay que resolverlo contra la API
**real de CameraX 1.6.2** (la fijada en `gradle/libs.versions.toml`):

- `ViewPort` **sí** recorta la salida de `ImageCapture` (documentado: "for on-disk
  ImageCapture, the image is cropped before saving").
- El `ViewPort.Builder` reciente se construye con `(aspectRatio, rotation)` +
  `setScaleType`/`setLayoutDirection`, y el crop rect se **deriva del aspect ratio
  y el scale type**; hay que confirmar si 1.6.2 aún expone `setRect(RectF)`
  (histórico, deprecado) o solo el aspect ratio.
- La `Preview` del mismo `UseCaseGroup` comparte el `ViewPort`: si su aspecto ≠ el
  de la vista, la preview se **recorta/zoomea** al área del ViewPort y deja de
  verse el fotograma completo, rompiendo el mockup 04 (preview fullscreen + scrim).

Decisión: si 1.6.2 permite un crop sub-rect sin zoomear la preview → **Enfoque A**;
en caso contrario (o si la verificación física muestra desviación) → **Enfoque B**,
determinista y testeable en JVM. El Enfoque B es el fallback por defecto ante
cualquier duda de la API.

### Enfoque A (si el gate lo permite): `ViewPort` de CameraX

1. Construir un `UseCaseGroup` con `Preview` + `ImageCapture` y un `ViewPort`
   cuyo crop coincida con el recuadro (aspecto del recuadro × `display.rotation`,
   y el rect del recuadro si la API lo permite), reutilizando la geometría pura.
   El pipeline EXIF/≤1280 no cambia.
2. Preferir la captura **en memoria** (`takePicture(Executor,
   OnImageCapturedCallback)`): el propio doc de `ViewPort` avisa de que la captura
   a fichero/stream ("on-disk") con crop **añade latencia**; `ImageProxy.getCropRect()`
   expone el recorte para aplicarlo explícitamente si hiciera falta.
3. Mantener la retícula visual exactamente como está (scrim/squinas/hint) y
   comprobar que "lo que se ve en la preview" = "lo que se captura" (Scenario 5).

### Enfoque B (DEFAULT si el gate falla): recorte en el pipeline

Recortar en `CapturedPhotoProcessor` tras hornear la orientación EXIF y antes del
escalado a ≤1280, con la geometría pura compartida con la retícula:

1. Extraer la matemática de `calcularZonaCaptura` a una función pura testeable
   desde JVM (misma fórmula: margen 24dp, ancho ≤320dp, alto ≤68vh, centrada),
   p. ej. `EncuadreGeometria.zona(anchoVista, altoVista, margenPx, anchoMaxPx,
   altoFrac): Rect` y `rectNormalizado(zona, anchoVista, altoVista): RectF`
   (coordenadas [0,1]).
2. Mapear ese rect a la imagen capturada **después de hornear la rotación**,
   corrigiendo el factor de escala del `FILL_CENTER` de la `PreviewView` (la
   preview recorta el fotograma al aspecto de la vista; como el recuadro está
   centrado, el mapeo se reduce a escala + centro). Mecanismo recomendado si está
   disponible: `PreviewView.getOutputTransform()` (requiere
   `ImplementationMode.COMPATIBLE`) para pasar vista→buffer; si no, derivar el
   factor de FILL_CENTER a partir del aspecto de la vista y del tamaño de la
   imagen (determinista y testeable).
3. Ampliar el contrato `PhotoProcessor.process(rawJpeg, crop)` (dominio +
   framework + `GuardarFotoCapturada` + dobles/tests), pasando el rect normalizado.

Ambos enfoques conservan: contrato de `PhotoFileStore.save`, `MAX_LADO_LARGO`
= 1280, JPEG 85, 0 EXIF de salida, y el estado `Revision(rutaFoto, textoCrudo)`.
El Enfoque B garantiza WYSIWYG por construcción (la misma función pura dibuja la
retícula y calcula el crop) y se prueba en JVM.

## Expected File Changes

- `feature/escanear/src/main/java/.../EncuadreGeometria.kt` — create; geometría
  pura del recuadro (`zona(...)` + `rectNormalizado(...)`), sin Compose → testeable
  en JVM. `calcularZonaCaptura` (en `CameraEncuadre.kt`) pasa a delegar en ella.
- `feature/escanear/src/test/java/.../EncuadreGeometriaTest.kt` — create; test JVM
  de escala/proporción/centrado del recuadro y del rect normalizado (regla 3 de la
  verificación).
- `feature/escanear/src/main/java/.../CameraEncuadre.kt` — modify; **Enfoque A**:
  `UseCaseGroup` + `ViewPort` en `bindToLifecycle` (y captura en memoria si aplica);
  **Enfoque B**: sin cambios de binding, solo delega la geometría.
- `feature/escanear/src/main/java/.../EscanearScreen.kt` /
  `EscanearViewModel.kt` — modify solo si el Enfoque B necesita propagar el rect
  del recuadro hasta `GuardarFotoCapturada` (la pantalla ya tiene el tamaño de la
  vista en el `Canvas` de la retícula; evaluar de dónde se toma el rect real).
- `app/src/androidTest/.../captura/CapturaFlujoTest.kt` — modify solo si hace
  falta ajustar aserciones del E2E (p. ej. dimensiones del resultado).
- `framework/database/src/main/java/.../source/CapturedPhotoProcessor.kt` —
  modify **solo Enfoque B**; recorte tras hornear EXIF y antes del escalado.
- `framework/database/src/main/java/.../photo/PhotoPipelineMath.kt` — modify
  **solo Enfoque B**; matemática pura del mapeo rect normalizado → píxeles.
- `framework/database/src/androidTest/.../CapturedPhotoProcessorTest.kt` — modify
  **solo Enfoque B** (caso con crop).
- `domain/expenses/.../data/PhotoProcessor.kt` + `usecases/GuardarFotoCapturada.kt`
  — modify **solo Enfoque B**; contrato `process(rawJpeg, crop)`.
- `test/unit/.../FakePhotoProcessor.kt` (y su uso en tests) — modify **solo
  Enfoque B**.
- `docs/architecture.md` — update; sección Captura de foto (el recuadro recorta).
- `docs/specs/captura-recorte-encuadre.md` — este documento.
- Dependencias: **ninguna nueva** (`camera-core`/`camera-view` ya están en el catálogo).

Nota: la lista de ficheros de Enfoque B está condicionada al Gate de API (T0); la
del Enfoque A es la que aplica si el gate lo permite. El implementador declara en
Implementation Notes qué enfoque tomó y por qué.

## Visual Design Impact

- UI involved: sí. Design source: `DESIGN.md` + `docs/design/stitch/04-camara-encuadre/`.
- La retícula (scrim ~65 %, esquinas `secondary-fixed`, hint, obturador 80dp) NO
  cambia visualmente; cambia su **semántica** (ahora delimita el recorte real).
- Sin decoración ficticia nueva.

## Durable Documentation Impact

- `docs/architecture.md`: update — sección "Captura de foto": la región del
  recuadro es la que se captura (ViewPort o crop en pipeline), no el fotograma
  completo.
- `ARCHITECTURE.md` (raíz): not needed — no existe; el equivalente es
  `docs/architecture.md`.
- `CONSTRAINTS.md` (raíz): not needed — no existe y no se introduce un MUST/MUST
  NOT durable nuevo (el contrato de ruta relativa sigue en `PhotoFileStore`).
- `AGENTS.md`: not needed — sin cambios de reglas de trabajo.
- `docs/adr/*`: not needed — no cambia la decisión de motores ni de pipeline.
- `docs/specs/captura-foto.md`: not needed (la feature nueva se documenta aquí);
  el Non-Goal de crop se cita como origen.
- `feature_list.json` y `PROGRESS.md`: update (evidencia; lo aplica el orquestador).

## Implementation Plan

1. **T0 Gate de API**: confirmar la superficie de crop de CameraX 1.6.2
   (`ViewPort.Builder`/`setRect`/`setScaleType`, `ImageCapture` en memoria vs
   fichero, `PreviewView.getOutputTransform`) y decidir Enfoque A o B.
2. Extraer la geometría del recuadro a una función pura + test JVM.
3. Implementar el enfoque elegido (A: `UseCaseGroup`+`ViewPort`; B: crop en
   pipeline con el contrato ampliado).
4. Verificar en emulador (E2E) y en físico (Tab S9+ con ticket de papel) que el
   ticket ocupa más proporción, que la preview no se recorta (Scenario 5) y que
   EXIF/tamaño/regresión siguen bien.
5. Si el Enfoque A se eligió pero la verificación física muestra desviación o
   zoom, revertir a Enfoque B y repetir.
6. Cierre: gates, búsquedas, docs durables, evidencia.

## Implementation Tasks

- [x] T0 Gate de API: documentado en Implementation Notes; **Enfoque B elegido** (recorte en el pipeline). Ver nota.
- [x] T1 Geometría pura: `EncuadreGeometria.zona(...)` + `rectNormalizado(...)` testeables desde JVM + `EncuadreGeometriaTest` (3/3).
- [x] T2 (Enfoque A) `ViewPort` — NO aplica (se eligió Enfoque B).
- [x] T3 (Enfoque B) Crop en `CapturedPhotoProcessor` + `PhotoPipelineMath.recorteEnImagen` (mapeo FILL_CENTER→imagen) + contrato `PhotoProcessor.process(rawJpeg, crop: CropRegion)` (dominio + `GuardarFotoCapturada` + `FakePhotoProcessor` + tests).
- [x] T4 Recorte real verificado en dispositivo: `CapturedPhotoProcessorTest.processCropsTheGuidedRegionInsteadOfTheWholeFrame` (2000x1000 con recuadro al 50% → 1000x500, 0 fallos) + `EncuadreGeometriaTest` JVM.
- [x] T5 Regresión: pipeline EXIF/≤1280/JPEG 85 intacto; `CapturaFlujoTest`, `NavigationTest` y `ProcesadoFlujoTest` 8/8 en `emulator-5554`; `:framework:database:connectedDebugAndroidTest` 22/22.
- [x] T6 Verificación física Tab S9+ (hecha 2026-10-17): foto recortada **819x1280 (aspecto 0,64)** vs **960x1280 (0,75)** del fotograma completo previo; 0 EXIF; preview a pantalla completa sin zoom (Scenario 5); usuario la validó visualmente; con el recorte la extracción nube dio el total correcto 35,30 € (antes 32,09 €). Evidencia en `docs/evidence/captura-recorte-encuadre/`.
- [x] T7 Cierre: `assembleDebug testDebugUnitTest` + `:test:unit:test`/`:domain:expenses:test`/`:feature:escanear:testDebugUnitTest`/`:framework:database:testDebugUnitTest`/`:framework:database:connectedDebugAndroidTest` en verde; `git diff init.sh` vacío; `schemas/` sin diff; docs durables; evidencia en `docs/evidence/captura-recorte-encuadre/`.

## Verification Plan

- Gate de API (T0): cita textual de la API de CameraX 1.6.2 usada (método exacto
  de `ViewPort.Builder`/`ImageCapture`) en Implementation Notes; sin inventar.
- `.\gradlew.bat assembleDebug testDebugUnitTest` → BUILD SUCCESSFUL.
- `.\gradlew.bat :feature:escanear:testDebugUnitTest --rerun` (`EncuadreGeometriaTest`,
  escala/proporción/centrado + rect normalizado) → verde; JUnit XML inspeccionado.
- Enfoque B: `.\gradlew.bat :framework:database:testDebugUnitTest --rerun`
  (`PhotoPipelineMathTest` con el mapeo FILL_CENTER→imagen) + `:domain:expenses:test --rerun`
  (`GuardarFotoCapturadaTest` con crop) → verde.
- `$env:ANDROID_SERIAL="emulator-5554"; .\gradlew.bat :app:connectedDebugAndroidTest`
  → `CapturaFlujoTest` + `ProcesadoFlujoTest` + `NavigationTest` en verde.
  - Focused: `-Pandroid.testInstrumentationRunnerArguments.class=com.robertomr.tally.captura.CapturaFlujoTest`.
- Físico (DEFAULT Tab S9+ `SM-X810`, ADB inalámbrico; el realme GT 2 no está
  conectado): `:app:installDebug` + `pm grant … CAMERA` + captura real (vertical y
  girada); `uiautomator`/`screencap` de la pantalla 05; análisis del JPEG
  (dimensiones + 0 EXIF) como en sesiones 012-013.
  - Medida de la proporción del ticket (p. ej. altura del ticket / altura de la
    imagen) antes/después con el mismo ticket; debe aumentar de forma clara.
  - Scenario 5: captura de pantalla del encuadre que muestre el fotograma completo
    con scrim (no un viewfinder recortado).
- Enfoque A: comprobar en el JSON/captura que la imagen contiene el recuadro y no
  el fotograma completo (no basta con que compile).
- E2E obligatorio: `CapturaFlujoTest` (esta feature cambia la captura observable).
- `init.sh` no cambia (`git diff init.sh` vacío; equivalencia con los gates
  anteriores en esta máquina sin `bash`; `init.sh` no arranca procesos largos).

## Evidence To Capture

- Cita textual de la API de CameraX 1.6.2 usada (Gate de API T0): métodos exactos
  de `ViewPort.Builder`/`ImageCapture` y enfoque elegido, en Implementation Notes.
- JUnit XML (JVM del test de geometría + instrumentados) en `docs/evidence/captura-recorte-encuadre/`.
- Capturas `uiautomator`/`screencap` de la pantalla 05 con el ticket tras el recorte + dimensiones del JPEG (lado largo 1280, 0 EXIF).
- Comparación citable de la proporción del ticket en la pantalla 05 antes/después.
- Registro en `feature_list.json` (evidence) y `PROGRESS.md`, con dispositivos.

## Open Questions (para el usuario)

- **U1 — Enfoque**: DEFAULT = Enfoque A (`ViewPort` de CameraX, recorte con
  resolución del sensor) **si el Gate de API confirma que 1.6.2 puede fijar el
  rect del recuadro sin zoomear la preview**; si no, Enfoque B (recortar en el
  pipeline, determinista y testeable en JVM). Confirmar que se acepta que el
  implementador elija A o B según el gate y lo documente.
- **U2 — Aspecto del recuadro**: mantener el recuadro actual (≤320dp × 68vh) como
  región de recorte. Alternativa: forzar un aspecto tipo ticket (p. ej. 3:5).
  DEFAULT: mantener el del mockup 04.
- **U3 — Dispositivo**: Tab S9+ (`SM-X810`) como sustituto físico (realme GT 2 no
  conectado; precedente sesiones 012-013/018).
- **U4 — Tope 1280 px**: se mantiene. Tras recortar, el ticket ocupa más píxeles
  dentro del mismo tope (mejora el detalle sin subir el peso).
- **U5 — Preview fullscreen**: la retícula sigue a pantalla completa con scrim; no
  se acepta un viewfinder recortado/zoomeado. Confirmar que, si `ViewPort` implica
  zoom de la preview, se prefiere el Enfoque B (crop en pipeline) antes que cambiar
  el mockup.

## Implementation Notes / Desviaciones

- **Gate de API (T0) → ENFOQUE B elegido (recorte en el pipeline).** Motivo: el
  recuadro del mockup 04 es un sub-rect arbitrario (≤320dp × 68vh) más pequeño que
  el máximo centrado que define un `ViewPort` por aspecto; además el `ViewPort`
  comparte la región visible con la `Preview`, de modo que si su aspecto no coincide
  con el de la vista la preview se recortaría/zoomearía y rompería el mockup 04 a
  pantalla completa (Scenario 5/U5). El Enfoque B es determinista, testeable en JVM
  y no toca el diseño visual. NO se afirma un detalle de API de CameraX 1.6.2 que no
  se haya podido verificar en esta máquina.
- **Implementación (Enfoque B)**:
  - `EncuadreGeometria` (nueva, `:feature:escanear`): `zona(...)` y
    `rectNormalizado(...)` puros (sin Compose) — la MISMA geometría pinta la
    retícula y calcula el recorte (WYSIWYG). `EncuadreGeometriaTest` 3/3.
  - `CropRegion` (nueva) en `:domain:expenses/data`: rect normalizado [0,1]
    respecto a la VISTA + `viewAspectRatio`. Contrato
    `PhotoProcessor.process(rawJpeg, crop)`.
  - `PhotoPipelineMath.recorteEnImagen(...)`: mapea el rect de la vista a la imagen
    orientada modelando el `FILL_CENTER` de la `PreviewView` (factor de escala y
    centro 0.5). `PhotoPipelineMathTest` 10/10 (identidad, recorte horizontal y
    vertical por FILL_CENTER).
  - `CapturedPhotoProcessor`: EXIF → recorte de la región → escala a ≤1280 → JPEG 85
    sin EXIF. `CapturedPhotoProcessorTest.processCropsTheGuidedRegionInsteadOfTheWholeFrame`
    (2000x1000 con recuadro al 50% → **1000x500**): recorta de verdad, no el
    fotograma.
  - `CameraEncuadre`: `onCapturado(bytes, crop)` con el rect de la vista
    (`onSizeChanged`); la retícula usa la misma función; la preview sigue a pantalla
    completa (FILL_CENTER), sin cambios visuales.
  - `EscanearViewModel`/`GuardarFotoCapturada`/`FakePhotoProcessor`: propagan el
    `crop`. `GuardarFotoCapturadaTest` 4/4, `EscanearViewModelTest` 16/16.
- **Corrección de un test del implementer**: `PhotoPipelineMathTest.centeredRectKeepsRatioWhenAspectsMatch`
  esperaba `right=0.5`; con aspectos coincidentes el recorte es identidad
  (`right=0.75`). Corregida la expectativa (el código era correcto).
- **Finding Low (robustez, no bloqueante)**: con el recorte en pipeline, la
  decodificación se muestrea para preservar el detalle del recuadro, así que en
  fotos de sensor grandes (p. ej. 4000x3000) puede decodificar a resolución alta
  (más memoria transitoria que el pipeline previo, que bajaba a ~1280 antes de
  recortar). A vigilar en dispositivos de gama baja; se puede acotar el
  `inSampleSize` si molesta.
- **T6 (físico) HECHA** (Tab S9+ `SM-X810`, 2026-10-17): recorte confirmado
  (819x1280, aspecto 0,64 = aspecto del recuadro, vs 960x1280/0,75 del fotograma
  completo; 0 EXIF); preview a pantalla completa sin zoom (Scenario 5); el usuario
  validó visualmente. Bonus: la extracción nube con la foto recortada dio el total
  correcto 35,30 € (con el fotograma completo daba 32,09 €). Ficheros en
  `docs/evidence/captura-recorte-encuadre/` (`fisico-ticket-recortado.jpg`,
  `scenario5-encuadre-preview.png`, `fisico-resultado-recorte.png`, `fisico-resultado-uia.xml`,
  `run-as-fotos.txt`).

## Validator Checklist

- [x] Implementation stays within this feature's scope (sin detección de bordes/deskew/ajuste de esquinas, sin tocar OCR/extracción/Room, sin dependencias ni módulos ni rutas nuevas).
- [x] Se declara en Implementation Notes el resultado del Gate de API y el enfoque elegido (A o B). (Se eligió B y se documentó el motivo; NO se cita un detalle de API de CameraX 1.6.2 que no se pudo verificar en esta máquina.)
- [x] Acceptance scenarios 1-6 pass (1-3 con verificación física en Tab S9+; 5 comprueba que la preview no se recorta/zoomea; 6 = Enfoque B, el fallback elegido).
- [x] Verification evidence is present (JUnit XML de `EncuadreGeometriaTest` + instrumentados + capturas/uiautomator + dimensiones del JPEG en `docs/evidence/captura-recorte-encuadre/`).
- [x] `CapturaFlujoTest` sigue en verde (E2E enfocado) y `NavigationTest`/`ProcesadoFlujoTest` sin regresión.
- [x] Pipeline EXIF/≤1280/JPEG 85 intacto y contrato `PhotoFileStore.save` intacto (ruta relativa).
- [x] La proporción del ticket en la pantalla 05 es claramente mayor que antes (comparación citable) y el encuadre no cambió visualmente de forma observable más allá de la semántica del recuadro.
- [x] `feature_list.json` y `PROGRESS.md` actualizados con evidencia no inventada.
- [x] No unrelated product behavior or extra feature work was added.

### Validator Verdict (2026-10-17, sesión 023)

- Veredicto: **accept** (feature-validator). Gates re-ejecutados en verde (JVM `EncuadreGeometriaTest` 3/3, `PhotoPipelineMathTest` 10/10, `GuardarFotoCapturadaTest` 4/4, `EscanearViewModelTest` 16/16; `:framework:database:connectedDebugAndroidTest` 22/22; `:app` E2E 8/8). Sin hallazgos Critical/High/Medium.
- Verificó que el código de `recorteEnImagen` es correcto (la expectativa corregida era la errónea) y corroboró la evidencia física 819x1280 (0,64) **al píxel** con la matemática del recuadro; 0 EXIF.
- 5 findings Low no bloqueantes: (1)(2) texto obsoleto "T6 pendiente" en `notes`/`PROGRESS` (corregido); (3) revisión visual humana de PNG pendiente; (4) nit de orientación/encoding de la evidencia; (5) supuesto aspecto preview==ImageCapture (válido en el dispositivo probado). Finding Low ya declarado: memoria transitoria en sensores grandes.

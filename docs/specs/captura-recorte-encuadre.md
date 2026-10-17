# Feature Implementation Spec: Recorte de la foto al área del encuadre guiado

## Source Feature

- `id`: captura-recorte-encuadre
- `area`: captura
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

## Non-Goals

- Cambiar el pipeline EXIF/compresión (sigue: hornear orientación → ≤1280 px lado
  largo → JPEG ~85, sin tag de orientación).
- Detección automática de bordes/esquinas del ticket, enderezado (deskew), ajuste
  manual de esquinas, filtros de luz, flash, linterna, galería (decoración ficticia
  del mockup 04, prohibida por `DESIGN.md`).
- Cambiar el contrato `PhotoFileStore.save` (sigue devolviendo `fotos/captura_<uuid>.jpg`
  relativo bajo `filesDir`).
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

## Technical Approach

Enfoque recomendado (DEFAULT): recorte en la propia captura con **CameraX
`ViewPort`**.

1. Construir un `UseCaseGroup` con `Preview` + `ImageCapture` y
   `ViewPort.Builder(Rational(ancho, alto), display.rotation).setRect(rectF).build()`,
   donde `rectF` es el rectángulo del recuadro en coordenadas de la `PreviewView`
   (el mismo que calcula `calcularZonaCaptura`) y el `Rational` es su proporción.
   Así CameraX entrega directamente una imagen cuyo contenido coincide con lo que
   hay dentro del recuadro, **con resolución del sensor** (mejor que recortar
   después). El pipeline EXIF/≤1280 no cambia.
2. Extraer la matemática del recuadro a una función pura (p. ej. un objeto
   `EncuadreGeometria` con `fun zona(width, height, margenPx, anchoMaxPx, altoFrac): Rect`)
   para poder testearla desde JVM (regla 3 de la verificación).
3. Mantener la retícula visual exactamente como está (scrim/squinas/hint), de modo
   que "lo que se ve en la preview" = "lo que se captura".

Plan B (si `ViewPort` no recorta `ImageCapture` en esta versión de CameraX, o si
la verificación física muestra desviación): pasar el rectángulo del recuadro al
pipeline y **recortar en `CapturedPhotoProcessor`** tras hornear la orientación y
antes del escalado a ≤1280, mapeando las coordenadas de la preview a la imagen
(teniendo en cuenta el `FILL_CENTER` de la `PreviewView` y la orientación). Este
plan B implica ampliar el contrato `PhotoProcessor.process(rawJpeg, crop)`
(cambio en dominio + framework + `GuardarFotoCapturada` + dobles/tests). Se elige
solo si el DEFAULT falla en la verificación física.

Cualquiera de las dos vías conserva: contrato de `PhotoFileStore.save`, `MAX_LADO_LARGO`
= 1280, JPEG 85, 0 EXIF de salida, y el estado `Revision(rutaFoto, textoCrudo)`.

## Expected File Changes

- `feature/escanear/src/main/java/.../CameraEncuadre.kt` — modify; `UseCaseGroup`
  + `ViewPort` a partir de la zona del recuadro; extraer la matemática de la zona
  a una función pura.
- `feature/escanear/src/test/java/.../` — create; test JVM de la geometría del
  recuadro (escala/proporción) si la función pura vive en `:feature:escanear`
  (alternativa: moverla a un sitio testeable sin Compose).
- `app/src/androidTest/.../captura/CapturaFlujoTest.kt` — modify solo si hace
  falta ajustar aserciones del E2E (p. ej. dimensiones del resultado).
- `framework/database/src/androidTest/.../CapturedPhotoProcessorTest.kt` — modify
  solo si se toma el Plan B (crop en pipeline).
- `domain/expenses/.../data/PhotoProcessor.kt` + `usecases/GuardarFotoCapturada.kt`
  — modify **solo Plan B**.
- `docs/architecture.md` — update; sección Captura de foto (el recuadro recorta).
- `docs/specs/captura-recorte-encuadre.md` — este documento.
- Dependencias: **ninguna nueva** (`camera-core`/`camera-view` ya están en el catálogo).

## Visual Design Impact

- UI involved: sí. Design source: `DESIGN.md` + `docs/design/stitch/04-camara-encuadre/`.
- La retícula (scrim ~65 %, esquinas `secondary-fixed`, hint, obturador 80dp) NO
  cambia visualmente; cambia su **semántica** (ahora delimita el recorte real).
- Sin decoración ficticia nueva.

## Durable Documentation Impact

- `docs/architecture.md`: update — sección "Captura de foto": la región del
  recuadro es la que se captura (ViewPort), no el fotograma completo.
- `AGENTS.md`: not needed — sin cambios de reglas de trabajo.
- `docs/adr/*`: not needed — no cambia la decisión de motores ni de pipeline.
- `docs/specs/captura-foto.md`: not needed (la feature nueva se documenta aquí);
  el Non-Goal de crop se cita como origen.
- `feature_list.json` y `PROGRESS.md`: update (evidencia; lo aplica el orquestador).

## Implementation Plan

1. Extraer la geometría del recuadro a una función pura + test JVM.
2. Cablear `UseCaseGroup` + `ViewPort` en `CameraEncuadre` reutilizando esa geometría.
3. Verificar en emulador (E2E) y en físico (Tab S9+ con ticket de papel) que el
   ticket ocupa más proporción y que EXIF/tamaño/regresión siguen bien.
4. Si el DEFAULT no recorta, aplicar el Plan B (crop en pipeline) y repetir la verificación.
5. Cierre: gates, búsquedas, docs durables, evidencia.

## Implementation Tasks

- [ ] T1 Geometría pura: extraer `zona(width, height, …) -> Rect` testeable desde JVM + test (escala/proporción).
- [ ] T2 `CameraEncuadre`: `UseCaseGroup` + `ViewPort` (rect del recuadro + `Rational` + `display.rotation`) en `bindToLifecycle`; preview `FILL_CENTER` coherente.
- [ ] T3 Verificar que la imagen capturada coincide con el recuadro (no el fotograma); confirmar con `ViewPort` que `ImageCapture` respeta el crop.
- [ ] T4 Regresión: pipeline EXIF/≤1280 intacto; `CapturaFlujoTest` y `NavigationTest` en verde.
- [ ] T5 Verificación física Tab S9+: ticket real (vertical y girado); comparar proporción del ticket en la pantalla 05 frente a la evidencia anterior (`docs/evidence/captura-foto-*`); JPEG con orientación correcta y lado largo 1280.
- [ ] T6 Plan B (solo si T3/T5 muestran que `ViewPort` no recorta): crop en `CapturedPhotoProcessor` + contrato `process(rawJpeg, crop)` + tests.
- [ ] T7 Cierre: `assembleDebug testDebugUnitTest` + suite instrumentada; 0 versiones literales fuera del toml; `git diff init.sh` vacío; `schemas/` sin diff; docs durables; evidencia en `docs/evidence/captura-recorte-encuadre/`.

## Verification Plan

- `.\gradlew.bat assembleDebug testDebugUnitTest` → BUILD SUCCESSFUL.
- `.\gradlew.bat :feature:escanear:testDebugUnitTest --rerun` (+ `:framework:database:testDebugUnitTest` si Plan B) → verde.
- `$env:ANDROID_SERIAL="emulator-5554"; .\gradlew.bat :app:connectedDebugAndroidTest` → `CapturaFlujoTest` + `ProcesadoFlujoTest` + `NavigationTest` en verde.
  - Focused: `-Pandroid.testInstrumentationRunnerArguments.class=com.robertomr.tally.captura.CapturaFlujoTest`.
- Físico (DEFAULT Tab S9+ `SM-X810`, ADB inalámbrico; el realme GT 2 no está conectado): `:app:installDebug` + `pm grant … CAMERA` + captura real (vertical y girada); `uiautomator`/`screencap` de la pantalla 05; análisis del JPEG (dimensiones + 0 EXIF) como en sesiones 012-013.
- E2E obligatorio: `CapturaFlujoTest` (esta feature cambia la captura observable).
- `init.sh` no cambia (`git diff init.sh` vacío; equivalencia con los gates anteriores en esta máquina sin `bash`).

## Evidence To Capture

- JUnit XML (JVM del test de geometría + instrumentados) en `docs/evidence/captura-recorte-encuadre/`.
- Capturas `uiautomator`/`screencap` de la pantalla 05 con el ticket tras el recorte + dimensiones del JPEG (lado largo 1280, 0 EXIF).
- Comparación citable de la proporción del ticket en la pantalla 05 antes/después.
- Registro en `feature_list.json` (evidence) y `PROGRESS.md`, con dispositivos.

## Open Questions (para el usuario)

- **U1 — Enfoque**: DEFAULT = `ViewPort` de CameraX (recorte con resolución del sensor). Alternativa = recortar los bytes tras capturar (Plan B). Confirmar si se prefiere el DEFAULT.
- **U2 — Aspecto del recuadro**: mantener el recuadro actual (≤320dp × 68vh) como región de recorte. Alternativa: forzar un aspecto tipo ticket (p. ej. 3:5). DEFAULT: mantener el del mockup 04.
- **U3 — Dispositivo**: Tab S9+ (`SM-X810`) como sustituto físico (realme GT 2 no conectado; precedente sesiones 012-013/018).
- **U4 — Tope 1280 px**: se mantiene. Tras recortar, el ticket ocupa más píxeles dentro del mismo tope (mejora el detalle sin subir el peso).

## Validator Checklist

- [ ] Implementation stays within this feature's scope (sin detección de bordes/deskew/ajuste de esquinas, sin tocar OCR/extracción/Room, sin dependencias ni módulos ni rutas nuevas).
- [ ] Acceptance scenarios 1-4 pass (1 y 2 con verificación física en Tab S9+).
- [ ] Verification evidence is present (JUnit XML + capturas/uiautomator + dimensiones del JPEG en `docs/evidence/captura-recorte-encuadre/`).
- [ ] `CapturaFlujoTest` sigue en verde (E2E enfocado) y `NavigationTest`/`ProcesadoFlujoTest` sin regresión.
- [ ] Pipeline EXIF/≤1280/JPEG 85 intacto y contrato `PhotoFileStore.save` intacto.
- [ ] `feature_list.json` y `PROGRESS.md` actualizados con evidencia no inventada.
- [ ] No unrelated product behavior or extra feature work was added.

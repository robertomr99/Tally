# Feature Implementation Spec: Spike — Gemma 3n corre en la Tab S9+ con MediaPipe

## Source Feature

- `id`: spike-gemma-mediapipe
- `area`: ia
- `depends_on`: ["bootstrap-gradle"] (status `accepted` 2026-10-06)
- `status`: accepted (verificado 2026-10-12: harness verde en la Tab S9+ con el
  modelo cargado; R1 resuelta con decisión GO; validación independiente
  `accept` 2026-10-13)
- `source`: `feature_list.json`

## Goal

Spike técnico sin UI que elimina la mayor incertidumbre del proyecto (riesgo R1):
confirmar que un modelo Gemma 3n se carga con MediaPipe GenAI/LLM Inference en la
Galaxy Tab S9+ (12 GB), **fuera del hilo principal**, y responde a un prompt de
prueba en un tiempo aceptable. Se registra la latencia y se toma una decisión
go/no-go con plan B (Gemma 2 o modelo menor). No se construye el motor de
extracción ni su contrato: solo se valida la viabilidad de la tecnología.

## Non-Goals

- **No** definir ni implementar el contrato `TextEngine`, `TicketExtraido`,
  parser, validador, schema ni enum de categorías (features `extraccion-local` y
  `extraccion-nube`). Este spike no fija ese diseño.
- **No** UI de ningún tipo (procesado, badge de motor, Ajustes, placeholders).
- **No** UX de descarga del modelo con progreso/WiFi/espacio (`descarga-modelo`).
- **No** motor en nube, Firebase, Gemini ni Firebase AI Logic.
- **No** `EngineRouter`, forzar motor en Ajustes ni override de motor.
- **No** claves de API en APK, `BuildConfig` ni en el repo.
- **No** empaquetar el modelo en el APK (viene de Hugging Face, ~2–3 GB).
- **No** Room, persistencia ni integración con `:app`/navegación.

## Job Story

Cuando necesito desriesgar la incertidumbre técnica que bloquea la semana 1
(modelo local con MediaPipe),
quiero cargar Gemma 3n fuera del hilo principal en la Tab S9+ y obtener una
respuesta a un prompt de prueba,
para poder decidir go/no-go y elegir modelo/artefacto antes de comprometerme con
la feature del motor local.

## Users And Permissions

- Autor/desarrollador: único actor. No hay actor de usuario final. El harness se
  ejecuta manualmente por `adb`/test instrumentado en la **Tab S9+ física**
  (12 GB). El emulador `emulator-5554` no vale para este spike (RAM y soporte de
  MediaPipe insuficientes).

## Acceptance Scenarios

### Scenario 1: el modelo carga off-main-thread y responde

Given la Tab S9+ conectada con el modelo copiado a su almacenamiento externo
privado y el test instrumentado instalado
When se ejecuta el harness que carga el modelo y lanza un prompt de prueba
Then la carga no ocurre en el hilo principal y se obtiene una respuesta no vacía
del modelo, sin crash de la app.

### Scenario 2: latencia registrada

Given que el modelo ha respondido
When el harness mide tiempo de carga y de generación
Then los números reales (ms) quedan registrados en `PROGRESS.md`.

### Scenario 3: decisión go/no-go y plan B documentados

Given el resultado del spike (éxito o fallo)
When se cierra la sesión
Then `docs/risks-and-open-questions.md` contiene la resolución de R1 con la
decisión (go/no-go), el modelo/artefacto validado y el plan B concreto si aplica.

### Scenario 4: el camino estándar sigue intacto

Given el spike implementado
When ejecuto `bash init.sh`
Then ejecuta la verificación base (`assembleDebug` + `testDebugUnitTest`) sin
arrancar procesos largos y **sin** ejecutar `connectedAndroidTest`, y termina en
verde.

### Scenario 5: fallo controlado sin bloquear el proyecto

Given que el artefacto/modelo elegido no funciona en Android o el dispositivo
And When el harness no puede cargar el modelo
Then el fallo se captura con el error real, se documenta el no-go y el plan B, y
no se rompe el build estándar (`assembleDebug`/`testDebugUnitTest` verdes).

## Repository Research

### Files Inspected

- `AGENTS.md` — módulos, `di.kt` por módulo, impls `internal`, stack, cero
  versiones literales, capa IA en `framework/ai`, sin clave de API.
- `PROGRESS.md` — sesión 004; siguiente feature = esta; R1 sin validar.
- `feature_list.json` — criterios de verificación y `notes` de esta feature.
- `docs/specs/bootstrap-gradle.md` — nivel de detalle de spec aceptada.
- `docs/risks-and-open-questions.md` — R1 (Gemma 3n + MediaPipe), plan B
  (Gemma 2 / modelo menor), latencia local puede ser de decenas de segundos.
- `docs/technical-discovery.md` — Gemma 3n + MediaPipe GenAI en `framework/ai`,
  modelo fuera del APK, entorno Windows a validar en S1.
- `CONTEXT.md` — Motor Local = Gemma 3n con MediaPipe; nada de % inventados.
- `docs/adr/0001-doble-motor-con-router.md` — motor Local en `framework/ai`,
  `internal`; contrato `TextEngine` (no se crea en este spike).
- `docs/architecture.md` — `framework:ai` ya listado como capa técnica MediaPipe.
- `gradle/libs.versions.toml` — catálogo único (AGP 9.2.1, Kotlin 2.2.10, KSP,
  minSdk 26, compileSdk 36); sin entrada de MediaPipe ni de `androidx.test`.
- `framework/ai/build.gradle.kts` — `tally.android.library` + `tally.hilt`, sin
  dependencias ni `testOptions`.
- `framework/ai/src/main/java/com/robertomr/tally/framework/ai/di/di.kt` —
  `internal object AiModule` vacío.
- `app/build.gradle.kts` — `:app` ya depende de `:framework:ai`.
- `build-logic/.../AndroidLibraryConventionPlugin.kt` — `minSdk`/`compileSdk` del
  catálogo; no configura `testInstrumentationRunner`.
- `init.sh` — ejecuta `assembleDebug testDebugUnitTest`; no debe cambiar.

### Existing Patterns To Follow

- Versiones **solo** en `gradle/libs.versions.toml`; build files usan `libs.*`.
- `framework/ai` es android library `internal`; implementaciones `internal`.
- Un `di.kt` por módulo con Hilt.
- `init.sh` no arranca procesos largos (no `connectedAndroidTest`).
- Docs/comentarios en español; identificadores de código en inglés.
- Windows: comandos con `gradlew.bat`; `adb` en el SDK del usuario.

### Current Gaps

- No existe dependencia de MediaPipe (GenAI/LLM Inference) en el catálogo ni en
  `framework/ai`; falta confirmar artefacto y versión que soporta Gemma 3n.
- Sin formato de modelo decidido (`.task` vs `.litertlm`) ni fuente exacta en
  Hugging Face; tamaño esperado 2–3 GB, fuera del APK.
- **No hay infraestructura instrumentada** en ningún módulo: sin
  `testInstrumentationRunner`, sin `androidTest` source sets, sin deps
  `androidx.test` en el catálogo. Hay que añadirlas.
- `framework/ai` no tiene implementación; `AiModule` está vacío.
- No hay contrato `TextEngine` (deliberadamente fuera de este spike).

## Technical Approach

- **Artefacto MediaPipe**: investigar y fijar el artefacto GenAI/LLM Inference
  (familia `com.google.mediapipe:tasks-genai`) y la versión que soporta el modelo
  elegido. Toda versión va en `libs.versions.toml` (`[versions] mediapipeGenai` +
  `[libraries]`); ninguna versión literal en build files.
- **Modelo**: obtener de Hugging Face un Gemma 3n compatible (o, si no lo hay,
  caer a Gemma 2 / modelo menor según plan B) y decidir el formato. El fichero
  **nunca** entra en el APK ni en el repo.
- **Wrapper interno off-main-thread** en `framework/ai`: clase `internal`
  (p. ej. `GemmaLocalSpike`) que recibe un `File` del modelo, expone un método
  `suspend` que carga y genera, y devuelve texto + métricas (ms de carga y de
  generación). La carga e inferencia usan `withContext(...)` en un dispatcher de
  background (IO o single-thread dedicado); **nunca** main. Sin Compose, sin
  `domain`, sin interfaz de dominio: no es `TextEngine`.
- **Harness sin UI (preferido)**: test instrumentado en
  `framework/ai/src/androidTest/...` que: (1) resuelve el directorio con
  `InstrumentationRegistry...targetContext.getExternalFilesDir("models")` y
  **loguea la ruta absoluta**; (2) falla con mensaje claro si falta el modelo;
  (3) carga off-main-thread y lanza un prompt corto; (4) asevera respuesta no
  vacía y loguea latencias. Requiere `testInstrumentationRunner =
  "androidx.test.runner.AndroidJUnitRunner"` y deps `androidx.test` en catálogo +
  `androidTestImplementation`. Si el path del test APK de la library resulta
  incómodo, alternativa documentada: mover el harness a `:app` androidTest.
- **Push del modelo**: `adb push <modelo> /sdcard/Android/data/<paquete>/files/models/`
  con el paquete que loguee el test (`targetContext`). No se sube al repo; se
  documenta fuente y nombre.
- **Ejecución manual** en Tab S9+ física:
  `gradlew.bat :framework:ai:connectedDebugAndroidTest` (o `:app:...`). **No** se
  añade a `init.sh`.
- **Go/no-go**: con los números reales se decide; si falla, aplicar y documentar
  plan B. `init.sh` no cambia.

## Expected File Changes

- `gradle/libs.versions.toml` — modify; `[versions]`/`[libraries]` de MediaPipe
  GenAI y de `androidx.test` (runner/rules/core) si hacen falta.
- `framework/ai/build.gradle.kts` — modify; dependencia MediaPipe,
  `testInstrumentationRunner` y `androidTestImplementation` (vía catálogo).
- `framework/ai/src/main/java/.../framework/ai/spike/<Wrapper>.kt` — create;
  wrapper `internal` off-main-thread (nombre provisional).
- `framework/ai/src/main/java/.../framework/ai/di/di.kt` — modify solo si el
  wrapper necesita binding; mantener `AiModule` válido.
- `framework/ai/src/androidTest/java/.../framework/ai/<Spike>Test.kt` — create;
  harness instrumentado sin UI.
- `PROGRESS.md` — update; latencia, dispositivo, decisión y siguiente paso.
- `docs/risks-and-open-questions.md` — update; resolución de R1 y plan B.
- `feature_list.json` — update; status y `evidence`.
- `init.sh` — **sin cambios** (regresión obligatoria).
- Rutas provisionales: nombres de clase/fichero del wrapper y ubicación final del
  harness (`framework:ai` vs `:app`) se fijan al implementar.

## Visual Design Impact

- UI involved: no.
- Design source: not applicable.
- Screens or states affected: ninguna.
- New design artifact required: no — el spike no tiene superficie visual.

## Durable Documentation Impact

- `ARCHITECTURE.md` (raíz): not needed — no establece fronteras nuevas; las
  reglas viven en `AGENTS.md` y `docs/architecture.md`.
- `CONSTRAINTS.md`: not needed — las reglas MUST/MUST NOT duras ya están en
  `AGENTS.md` ("Qué no hacer") y no cambian aquí.
- `AGENTS.md`: not needed — no cambia el workflow ni la ruta de arranque
  (`init.sh` intacto).
- Other docs: `docs/risks-and-open-questions.md` — update obligatorio (R1 + plan
  B); `docs/technical-discovery.md` — update ligero solo si el modelo final
  difiere de "Gemma 3n" (p. ej. plan B), si no not needed; `PROGRESS.md` y
  `feature_list.json` — update obligatorio con evidencia.

## Implementation Plan

1. Investigar artefacto MediaPipe GenAI + versión compatible con Gemma 3n y
   modelo exacto en Hugging Face (formato y tamaño); registrar la elección.
2. Añadir entradas al catálogo (`libs.versions.toml`); ninguna versión literal.
3. Configurar `framework/ai/build.gradle.kts` (dependencia + runner + androidTest).
4. Implementar el wrapper `internal` off-main-thread con métricas de latencia.
5. Implementar el test instrumentado (ruta, prompt, asserts, logs).
6. Instalar en la Tab S9+, resolver ruta del modelo, `adb push` y ejecutar el test.
7. Medir latencia de carga y generación; registrar números crudos.
8. Decidir go/no-go y escribir el plan B concreto si aplica.
9. Actualizar `PROGRESS.md`, `docs/risks-and-open-questions.md` y
   `feature_list.json` con evidencia.
10. Verificar que `init.sh` sigue sin cambios y en verde (sin `connectedAndroidTest`).

## Implementation Tasks

- [x] T1 Artefacto MediaPipe + versión y modelo Hugging Face (formato/tamaño) investigados y anotados.
- [x] T2 Entradas de MediaPipe GenAI (y `androidx.test` si procede) solo en `gradle/libs.versions.toml`.
- [x] T3 `framework/ai/build.gradle.kts` con dependencia, `testInstrumentationRunner` y deps androidTest.
- [x] T4 Wrapper `internal` off-main-thread creado (carga + generate + latencia).
- [x] T5 Test instrumentado sin UI con resolución de ruta de modelo, prompt y asserts.
- [x] T6 Modelo copiado por `adb push` a la ruta correcta en la Tab S9+; test ejecutado manualmente.
      (2026-10-12: `gemma-3n-E2B-it-int4.litertlm` (3.655.827.456 bytes) subido a
      `/storage/emulated/0/Android/data/com.robertomr.tally.framework.ai.test/files/models/`
      vía ADB inalámbrico (9.7 MB/s, 361 s); test verde de verdad en la Tab S9+:
      respuesta no vacía y `ranOnMainThread=false`.)
- [x] T7 Latencia de carga y generación medida y anotada.
      (2026-10-12: `loadMillis=6517`, `generationMillis=53208`,
      `promptTokens=17`, `responseTokens=492` (~9,2 tok/s), respuesta
      "2 + 2 es igual a 4." + cola degenerada de `<end_of_turn>`/`<eos>` hasta
      `maxTokens=512`. Muestra corroborante en logcat:
      `loadMillis=6933`/`generationMillis=53985`. Ver PROGRESS.md sesión 006.)
- [x] T8 Decisión go/no-go y plan B (si aplica) en `docs/risks-and-open-questions.md`.
      (2026-10-12: DECISIÓN GO escrita en R1 con justificación numérica y plan B
      concreto: Gemma 2 2B-it `.task`, E2B sin visión, E4B-it int4 o nube.)
- [x] T9 `PROGRESS.md` y `feature_list.json` actualizados con evidencia (sesión 006;
      status `passing` tras el verde con el modelo cargado).
- [x] T10 `bash init.sh` verificado: sin cambios, verde, sin procesos largos.

## Verification Plan

- Manual en Tab S9+ (física): `gradlew.bat :framework:ai:connectedDebugAndroidTest`
  (o `:app:...`) con el modelo ya copiado. Esperado: test verde, respuesta no
  vacía y latencias logueadas. Comando **manual y nunca** en `init.sh`.
- `bash init.sh` → ejecuta `assembleDebug` + `testDebugUnitTest` y termina en
  verde; **no** arranca procesos largos y **no** ejecuta `connectedAndroidTest`.
  Debe permanecer sin cambios respecto al estado aceptado.
- Regresión de catálogo: `rg -n "mediapipe|androidx.test" --glob "**/*.gradle.kts"`
  → las dependencias resuelven por `libs.*`, sin versiones literales.
- Comprobación de hilo: revisar que el wrapper usa `withContext`/dispatcher de
  background y que no se invoca desde main (revisión de código).
- E2E persistente: no existe harness E2E en el repo y esta feature no cambia
  comportamiento de usuario, auth, rutas ni API, así que no procede cobertura
  E2E; la verificación es el test instrumentado manual (justificado).

## Evidence To Capture

- Salida del test instrumentado: modelo cargado, prompt usado, respuesta no vacía
  y latencias (ms de carga y de generación).
- Ficha del dispositivo (modelo Tab S9+, RAM, versión de Android) y artefacto +
  versión de MediaPipe.
- Nombre, formato, tamaño y fuente Hugging Face del modelo (sin subirlo al repo).
- Ruta absoluta resuelta del modelo y comando `adb push` usado.
- Texto de la decisión go/no-go y del plan B en
  `docs/risks-and-open-questions.md`.
- Salida de `bash init.sh` sin cambios y en verde.
- Entradas actualizadas en `feature_list.json` (`evidence`) y `PROGRESS.md`
  (sesión 005).

## Validator Checklist

- [ ] El alcance se limita al spike: sin UI, sin contrato `TextEngine`/
      `TicketExtraido`/parser, sin nube/Firebase, sin UX de descarga, sin router.
- [ ] Los escenarios de aceptación pasan (carga off-main-thread + respuesta,
      latencia registrada, go/no-go + plan B, `init.sh` intacto).
- [ ] La evidencia está presente (latencia, dispositivo, artefacto/versión,
      modelo, decisión).
- [ ] Cubrimiento E2E no aplicable y justificado; verificación instrumentada
      manual.
- [ ] `init.sh` no cambió, no arranca procesos largos ni ejecuta
      `connectedAndroidTest`.
- [ ] Versiones solo en `gradle/libs.versions.toml`; ninguna versión literal.
- [ ] Wrapper `internal` en `framework/ai`; ninguna dependencia de Android en
      `domain`.
- [ ] `feature_list.json` y `PROGRESS.md` actualizados correctamente.
- [ ] Sin comportamiento de producto no relacionado ni trabajo extra de otras
      features.

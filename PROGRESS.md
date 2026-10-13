# Progress Log

## Current Verified State

- Repository root: `E:\AI-Expert\Tally`
- Standard startup path: `bash init.sh`
- Standard verification path: `gradlew.bat assembleDebug` + `gradlew.bat testDebugUnitTest`
  (+ `gradlew.bat :test:unit:test` para los tests puros de `:test:unit`)
- Emulador de referencia: `emulator-5554` (sdk_gphone64_x86_64; adb en
  `C:/Users/Roberto/AppData/Local/Android/Sdk/platform-tools`). Tab S9+ y
  realme GT 2 se reservan para `captura-foto` y `spike-gemma-mediapipe`.
- Current next ready feature: `navegacion-base` (primera de `feature_list.json`
  con dependencias `accepted`; también quedan desbloqueadas `persistencia-room`,
  `descarga-modelo` y, parcialmente, `extraccion-local`)
- Current blocker: ninguno. R1 resuelta 2026-10-12 con decisión **GO**
  (Gemma 3n E2B-it int4 + MediaPipe `tasks-genai:0.10.35` validado en la Tab S9+).
- Last verified at: 2026-10-13 (sesión 007: validación independiente de
  `spike-gemma-mediapipe` → veredicto `accept`; gates re-ejecutados en verde)

## Session Log

### Session 001

- Date: 2026-09-19
- Goal: Descubrimiento de producto + andamiaje mínimo del repositorio.
- Completed:
  - Descubrimiento completo (interview build-brief) y documentos creados:
    `CONTEXT.md`, `docs/build-brief.md`, `docs/domain-model.md`,
    `docs/technical-discovery.md`, `docs/risks-and-open-questions.md`,
    `DESIGN.md`, `docs/adr/0001-doble-motor-con-router.md`,
    `docs/adr/0002-ocr-en-dos-etapas-con-ml-kit.md`.
  - Assets de diseño de Stitch reorganizados en `docs/design/stitch/`
    (9 pantallas + `stitch-design-system.md`).
  - Andamiaje: `AGENTS.md` actualizado con enrutado (sin tocar reglas),
    `feature_list.json` (23 features), `init.sh` pre-bootstrap, `PROGRESS.md`.
- Verification run: JSON de `feature_list.json` validado (ConvertFrom-Json); listado de `docs/design/stitch/` comprobado tras el movimiento.
- Evidence captured: ver campo `evidence` de cada feature (vacío: no hay features ejecutadas aún).
- Files or artifacts updated: los listados arriba + `init.sh`.
- Known risk or unresolved issue:
  - R1: soporte real de Gemma 3n en MediaPipe (Android) sin validar → spike primera sesión.
  - Cuota/latencia de Gemini (Firebase AI Logic) sin verificar.
  - Open Design Question #1 (colores de categoría) se decide en `grafica-donut`.
- Next best step: sesión 002 = feature `bootstrap-gradle` (crear proyecto Gradle multi-módulo y verificar build vacío en ambos móviles); en paralelo, si hay dispositivo, preparar el spike `spike-gemma-mediapipe`.

### Session 002

- Date: 2026-09-30
- Goal: Verificar y cerrar `bootstrap-gradle` usando el emulador de Android Studio.
- Completed:
  - Criterio de verificación enmendado (decisión del usuario): el emulador
    sustituye a la instalación en ambos móviles en esta feature; los móviles
    quedan para features de cámara y Gemma.
  - `gradlew.bat assembleDebug` → BUILD SUCCESSFUL.
  - `gradlew.bat testDebugUnitTest` → BUILD SUCCESSFUL.
  - `gradlew.bat :test:unit:test` → BUILD SUCCESSFUL (SmokeTest pasa).
  - APK instalado en `emulator-5554`; MainActivity arranca COLD sin crash.
- Verification run: los cuatro comandos anteriores en Windows (gradlew.bat y
  adb del Android SDK del usuario).
- Evidence captured: campo `evidence` de `bootstrap-gradle` en
  `feature_list.json`; screenshot en `docs/evidence/bootstrap-gradle-emulator.png`.
- Files or artifacts updated: `feature_list.json` (criterio, status `passing`,
  evidence), `PROGRESS.md`, `docs/evidence/bootstrap-gradle-emulator.png`.
- Known risk or unresolved issue:
  - Revisión visual del placeholder/tema Forest Precision pendiente a mano:
    el agente no puede inspeccionar imágenes (solo verifica arranque sin crash).
  - `bootstrap-gradle` queda en `passing`; `accepted` exige validación
    independiente (feature-validator), que además desbloquea dependencias
    bajo el flujo estricto.
  - R1: soporte real de Gemma 3n en MediaPipe (Android) sin validar todavía.
- Next best step: sesión 003 = feature `spike-gemma-mediapipe` (bloqueante de
  la semana 1); si se prefiere el flujo completo, primero validación
  independiente de `bootstrap-gradle`.

### Session 003

- Date: 2026-10-06
- Goal: Validación independiente de `bootstrap-gradle` + aplicación del
  Repair Brief (a petición del usuario).
- Completed:
  - Validación con feature-validator: veredicto `revise` (3 findings: `init.sh`
    pre-bootstrap, `docs/architecture.md` ausente, `passing` prematuro).
  - Repair aplicado: `init.sh` reescrito a variante bootstrapped (ejecuta
    `assembleDebug` + `testDebugUnitTest`, falla si algo rompe, imprime
    `:app:installDebug` tras pasar); `docs/architecture.md` creado (diagrama
    de módulos + regla de dependencias).
  - Re-verificación: `bash init.sh` → BUILD SUCCESSFUL (190 tasks).
- Verification run: `bash init.sh`, `gradlew.bat assembleDebug`,
  `gradlew.bat testDebugUnitTest`, `gradlew.bat :test:unit:test`.
- Evidence captured: campo `evidence` de `bootstrap-gradle` en
  `feature_list.json` (2 entradas nuevas del 2026-10-06).
- Files or artifacts updated: `init.sh`, `docs/architecture.md`,
  `feature_list.json`, `PROGRESS.md`.
- Known risk or unresolved issue:
  - `bootstrap-gradle` queda en `passing` hasta que el orquestador persista la
    aceptación como `accepted` (había quedado al margen de la validación).
  - Revisión visual del placeholder/tema Forest Precision pendiente a mano
    (el agente no puede inspeccionar imágenes).
  - R1: soporte real de Gemma 3n en MediaPipe (Android) sin validar todavía.
- Next best step: marcar `bootstrap-gradle` como `accepted` y ejecutar la
  feature `spike-gemma-mediapipe` (sesión 004, bloqueante de la semana 1).

### Session 004

- Date: 2026-10-06
- Goal: Persistir aceptación independiente de `bootstrap-gradle` y hacer commit inicial.
- Completed:
  - Validación independiente con feature-validator: veredicto `accept`
    (1 finding Low: módulos `feature` sin plugin `tally.hilt` aún; no bloquea).
  - Re-verificación: `gradlew.bat assembleDebug testDebugUnitTest` → BUILD
    SUCCESSFUL (190 tasks); `gradlew.bat :test:unit:test` → BUILD SUCCESSFUL;
    `:domain:expenses:dependencies` → solo `kotlin-stdlib` (puro Kotlin).
  - Status de `bootstrap-gradle` → `accepted` en `feature_list.json`.
- Verification run: `gradlew.bat assembleDebug testDebugUnitTest`,
  `gradlew.bat :test:unit:test`, `gradlew.bat :domain:expenses:dependencies`.
- Evidence captured: hallazgos de la validación registrados en `feature_list.json`
  (campo `notes`).
- Files or artifacts updated: `feature_list.json` (status `accepted`, notes),
  `PROGRESS.md`.
- Known risk or unresolved issue:
  - Finding Low: `feature:common` y `feature:resumen` aún sin plugin `tally.hilt`
    (el spec lo pedía; se añadirá cuando haya ViewModel).
  - Revisión visual del placeholder/tema Forest Precision pendiente a mano.
  - R1: soporte real de Gemma 3n en MediaPipe (Android) sin validar todavía.
- Next best step: sesión 005 = feature `spike-gemma-mediapipe` (bloqueante de
  la semana 1, riesgo R1).

### Session 005

- Date: 2026-10-11
- Goal: Continuar y cerrar `spike-gemma-mediapipe` (tareas T6–T10 del plan del
  spec): ejecución en la Tab S9+ física, latencia medida, decisión go/no-go,
  documentación duradera y regresión.
- Completed:
  - Dispositivo verificado y usable: Galaxy Tab S9+ (`SM-X810`, `gts9pwifi`)
    conectada por ADB inalámbrico, Android 16 (API 36), 11.454.088 kB de RAM
    (~11,4 GB / "12 GB"), 356 GB libres en `/storage/emulated`. Con varios
    dispositivos conectados, `ANDROID_SERIAL=<serial> gradlew.bat ...` limita
    `connectedDebugAndroidTest` a uno (verificado: solo corrió en `SM-X810`).
  - T6 parcial: `gradlew.bat :framework:ai:connectedDebugAndroidTest` ejecutado
    de verdad en la Tab S9+. El test APK se instaló y el harness corrió sin UI;
    **fallo controlado** (Scenario 5 del spec) con mensaje claro porque aún no
    hay modelo: "No hay modelo (.litertlm/.task) en
    /storage/emulated/0/Android/data/com.robertomr.tally.framework.ai.test/files/models".
    Ruta absoluta resuelta y paquete del test confirmados en logcat (`GemmaSpike`).
  - Comprobación real del `adb push` en esa ruta (scoped storage de Android 16):
    funciona directamente (fichero probe de 17 bytes subido y borrado después).
  - Modelo Gemma 3n NO disponible en el entorno: búsqueda de `*.litertlm`/`*.task`
    en las unidades locales y en `/sdcard` de la Tab sin resultados; Hugging Face
    (`google/gemma-3n-E2B-it-litertlm`) devuelve 401 (gated: exige aceptar la
    licencia y login). No se han inventado resultados de latencia.
  - T10: regresión completa en verde y `init.sh` sin cambios.
- Verification run:
  - `.\gradlew.bat assembleDebug testDebugUnitTest` → BUILD SUCCESSFUL (190 tasks).
  - `.\gradlew.bat :test:unit:test` → BUILD SUCCESSFUL.
  - `ANDROID_SERIAL=<serial-SM-X810> .\gradlew.bat :framework:ai:connectedDebugAndroidTest`
    → ejecutado en la Tab S9+ (resultados en
    `framework/ai/build/outputs/androidTest-results/connected/debug/`); fallo
    controlado esperado por modelo ausente (assert `fail` del harness).
  - `adb push` probe a `/storage/emulated/0/Android/data/com.robertomr.tally.framework.ai.test/files/models/`
    → "1 file pushed" (borrado tras la prueba).
  - Búsqueda con Select-String de versiones literales en `**/*.gradle.kts` → sin
    resultados; dependencias MediaPipe/androidx.test resueltas por `libs.*`.
  - `init.sh`: sin cambios (`git diff` vacío). Nota: esta máquina no tiene `bash`,
    así que no se invoca `bash init.sh` directamente; su contenido exacto
    (`assembleDebug` + `testDebugUnitTest`) se ejecutó como los dos comandos
    anteriores y ambos en verde. `connectedAndroidTest` sigue fuera de `init.sh`.
- Evidence captured: campo `evidence` de `spike-gemma-mediapipe` en
  `feature_list.json`; salidas de logcat `GemmaSpike` con la ruta resuelta y el
  comando de push; informe del test en
  `framework/ai/build/reports/androidTests/connected/debug/`.
- Files or artifacts updated: `PROGRESS.md`, `feature_list.json` (evidence; el
  status se mantiene `in_progress`), `docs/specs/spike-gemma-mediapipe.md`
  (checkboxes del plan). `docs/risks-and-open-questions.md` **sin cambios**: R1
  solo se resuelve con resultado real del spike.
- Known risk or unresolved issue:
  - **R1 sigue abierta**: sin el modelo no hay latencia ni decisión go/no-go
    (T6 completo, T7 y T8 pendientes). No hay evidencia inventada.
  - Paso manual pendiente (bloqueante): descargar el modelo de Hugging Face
    (gated; aceptar licencia + login con token HF):
    `google/gemma-3n-E2B-it-litertlm` (recomendado, el más ligero) o
    `google/gemma-3n-E4B-it-litertlm`, tomar el `.litertlm` (varios GB, fuera
    del APK y del repo) y ejecutar:
    `adb push <modelo>.litertlm /storage/emulated/0/Android/data/com.robertomr.tally.framework.ai.test/files/models/`
    (el directorio ya existe tras la ejecución de esta sesión; si se desinstala
    el test APK, ejecutar una vez el test para recrearlo o `adb shell mkdir -p`).
    Después: `ANDROID_SERIAL=<serial> gradlew.bat :framework:ai:connectedDebugAndroidTest`
    → registrar ms de carga y generación (T7) y cerrar go/no-go + plan B (T8).
  - `spike-gemma-mediapipe` queda en `in_progress` (no `passing`): la verificación
    en dispositivo con el modelo cargado es requisito del spec y aún no se ha
    ejecutado.
- Next best step: ejecutar el paso manual de arriba (descarga del `.litertlm` +
  `adb push` + `connectedDebugAndroidTest` en la Tab S9+), registrar latencias en
  T7, cerrar T8 con la decisión go/no-go y el plan B en
  `docs/risks-and-open-questions.md`, y solo entonces pasar la feature a
  `passing` para validación independiente.

### Session 006

- Date: 2026-10-12
- Goal: Cerrar `spike-gemma-mediapipe` (T6–T8 del plan del spec) con el modelo ya
  disponible: `adb push` del `.litertlm`, ejecución real del harness en la Tab S9+
  con el modelo cargado, medición de latencias y decisión go/no-go en R1.
- Completed:
  - T6 completo: modelo `gemma-3n-E2B-it-int4.litertlm` (Gemma 3n E2B-it
    cuantizado int4, formato LiteRT-LM; 3.655.827.456 bytes ≈ 3,4 GB; origen
    Hugging Face `google/gemma-3n-E2B-it-litertlm`, descargado a mano por el
    usuario a `E:\AI-Expert\Modelos\`, fuera del repo y del APK) subido con
    `adb push` por ADB inalámbrico a
    `/storage/emulated/0/Android/data/com.robertomr.tally.framework.ai.test/files/models/`
    (9,7 MB/s, 361 s). `adb shell ls -l` confirma 3.655.827.456 bytes
    (md5 en dispositivo `44260a251019aa81594be263681f0928`). El test APK no
    estaba instalado (se había desinstalado desde la sesión 005); el directorio
    se recreó con `adb shell mkdir -p` antes del push.
  - Ejecución real del harness SOLO en la Tab S9+:
    `ANDROID_SERIAL=<serial-SM-X810> .\gradlew.bat :framework:ai:connectedDebugAndroidTest`
    → BUILD SUCCESSFUL, 1 test, 0 fallos (JUnit XML de
    `framework/ai/build/outputs/androidTest-results/connected/debug/`:
    `gemmaLoadsOffMainThreadAndResponds`, time=60,117 s, failures=0, errors=0).
    Respuesta no vacía del modelo y `ranOnMainThread=false` (carga e inferencia
    fuera del hilo principal). **Escenarios 1, 2 y 3 del spec cumplidos.**
  - T7 latencias crudas (logcat tag `GemmaSpike`, ejecución de los artefactos
    oficiales): `ranOnMainThread=false`, `loadMillis=6517`,
    `generationMillis=53208`, `promptTokens=17`, `responseTokens=492`
    (~9,2 tokens/s). Respuesta del modelo: "2 + 2 es igual a 4." seguida de una
    cola degenerada de tokens especiales (`<end_of_turn>`/`<eos>`) hasta el tope
    `maxTokens=512`. Muestra corroborante adicional en el propio logcat (segunda
    ejecución previa la misma tarde, PID 8711, invocación no registrada en esta
    sesión): `loadMillis=6933`, `generationMillis=53985`, mismos tokens y misma
    respuesta. Ficha: Samsung Galaxy Tab S9+ (`SM-X810`, `gts9pwifi`),
    Android 16 (API 36), MemTotal 11.454.088 kB (~11,4 GB);
    MediaPipe `com.google.mediapipe:tasks-genai:0.10.35` (`LlmInference` +
    `LlmInferenceSession`); modelo/peso/origen arriba.
  - T8: decisión **GO** escrita en `docs/risks-and-open-questions.md` (R1
    resuelta) con justificación numérica y plan B concreto (Gemma 2 2B-it
    `.task` → Gemma 3n E2B sin visión → E4B-it int4 → nube según ADR 0001).
  - T10: regresión completa en verde, `init.sh` sin cambios, 0 versiones
    literales en `**/*.gradle.kts`.
- Verification run:
  - `ANDROID_SERIAL=<serial-SM-X810> .\gradlew.bat :framework:ai:connectedDebugAndroidTest`
    → BUILD SUCCESSFUL (1 test, 0 fallos) en la Tab S9+; evidencia en
    `framework/ai/build/outputs/androidTest-results/connected/debug/` y
    `framework/ai/build/reports/androidTests/connected/debug/`.
  - `adb -s <serial> logcat -d -s GemmaSpike` → latencias crudas y respuesta
    (fragmentos anteriores).
  - `.\gradlew.bat assembleDebug testDebugUnitTest` → BUILD SUCCESSFUL (190 tasks).
  - `.\gradlew.bat :test:unit:test` → BUILD SUCCESSFUL.
  - `git diff init.sh` → vacío (sin cambios; esta máquina no tiene `bash`, se
    verificó el diff + la equivalencia de su contenido con los dos comandos
    anteriores; `connectedAndroidTest` sigue fuera de `init.sh`).
  - Select-String de versiones literales (`x.y.z`, `version="0-9`, GAV literal)
    en los 12 `**/*.gradle.kts` → 0 coincidencias.
  - `git status --ignored` + búsqueda de `*.litertlm`/`*.task` en el repo → 0
    ficheros (modelo fuera del repo; `.gitignore` excluye ambos formatos).
- Evidence captured: campo `evidence` de `spike-gemma-mediapipe` en
  `feature_list.json`; informe JUnit/HTML y logcat per-test en
  `framework/ai/build/outputs/androidTest-results/connected/debug/` y
  `framework/ai/build/reports/androidTests/connected/debug/`; resolución de R1
  con números reales en `docs/risks-and-open-questions.md`.
- Files or artifacts updated: `PROGRESS.md`, `feature_list.json` (evidence +
  status `passing`), `docs/specs/spike-gemma-mediapipe.md` (T6–T8 `[x]`),
  `docs/risks-and-open-questions.md` (R1 resuelta + plan B + tarea de research).
- Known risk or unresolved issue:
  - La generación sin secuencia de parada degenera en tokens especiales hasta
    `maxTokens` (los 53 s medidos incluyen ~480 tokens degenerados tras una
    respuesta correcta de una frase; la respuesta útil llega en el primer
    segundo). El motor definitivo (`extraccion-local`) **debe** usar stop
    token/`<end_of_turn>` y re-medir la latencia con prompts de extracción
    largos (prefill de decenas de líneas de ticket). Esto queda registrado en
    R1 y en el spec como requisito derivado, no como fallo del spike.
  - `spike-gemma-mediapipe` queda en `passing` (implementado y auto-verificado);
    `accepted` exige validación independiente (feature-validator) que el
    orquestador debe persistir.
- Next best step: validación independiente de `spike-gemma-mediapipe` con
  feature-validator y persistencia de `accepted`; después, siguiente feature de
  `feature_list.json` cuyas dependencias estén aceptadas (p. ej. `ocr-mlkit` o
  las que dependan del spike).

### Session 007

- Date: 2026-10-13
- Goal: Validación independiente de `spike-gemma-mediapipe` con el subagente
  `validator` (feature-validator) y persistencia de la aceptación.
- Completed:
  - Validación independiente con feature-validator (subagente `validator`):
    veredicto **`accept`** sin hallazgos Critical/High/Medium.
  - Gates re-ejecutados por el validador en verde: `gradlew.bat assembleDebug
    testDebugUnitTest` (BUILD SUCCESSFUL, 190 tasks), `gradlew.bat
    :test:unit:test` (BUILD SUCCESSFUL), 0 versiones literales en los 12
    `**/*.gradle.kts`, 0 ficheros `*.litertlm`/`*.task` en el repo
    (`.gitignore` verificado con `git check-ignore`), 0 patrones de
    secretos/credenciales en el repo, diff de `init.sh`/`AGENTS.md`/
    `docs/architecture.md`/ADRs/`DESIGN.md` vacío.
  - Evidencia de dispositivo re-inspeccionada de verdad (sin re-ejecutar el
    test físico): JUnit XML `TEST-SM-X810 - 16-_framework_ai-.xml`
    (`tests=1 failures=0 errors=0`, time=60,117 s, device SM-X810),
    `test-result.textproto` (PASSED, ADB inalámbrico coherente con lo
    narrado), `device-info.pb` (SM-X810, arm64-v8a, API 36), `meminfo`
    (MemTotal 11.454.088 kB) y logcat per-test con `ranOnMainThread=false
    loadMillis=6517 generationMillis=53208 promptTokens=17 responseTokens=492`,
    respuesta `"2 + 2 es igual a 4."` + cola degenerada, ruta del modelo y
    carga nativa real (`LitertLm/litert_lm/LlmInference`, XNNPack cache con
    `gemma-3n-E2B-it-int4.litertlm`); 0 FATAL/ANR.
  - Validación contra el spec: escenarios 1–5 cumplidos; non-goals respetados
    (sin UI, sin TextEngine/parser/nube/router/Room); arquitectura correcta
    (`internal` en `framework/ai`, carga en `Dispatchers.IO`, `domain`
    intacto, dependencias vía catálogo).
  - Seguridad (checklist feature-scoped): sin hallazgos relevantes ni
    bloqueantes (sin secretos ni token HF, modelo fuera del repo y del APK,
    dependencias justificadas, sin superficie de ataque nueva).
  - Persistida la aceptación: `feature_list.json` status de
    `spike-gemma-mediapipe` → `accepted` (nota con fecha y alcance de la
    validación).
- Verification run: ver "Completed" (gates re-ejecutados por el validador;
  inspección de artefactos en
  `framework/ai/build/outputs/androidTest-results/connected/debug/`).
- Evidence captured: informe de validación (veredicto `accept`) registrado en
  las notas de `feature_list.json` y en esta sesión; artefactos de dispositivo
  re-inspeccionados en `framework/ai/build/outputs/`.
- Files or artifacts updated: `feature_list.json` (status `accepted` + nota),
  `PROGRESS.md`.
- Known risk or unresolved issue:
  - 2 findings Low no bloqueantes: (1) la muestra corroborante PID 8711
    (`loadMillis=6933`) se cita sin artefacto preservado — matizar o preservar;
    (2) la doc duradera aún estima "~2–3 GB" para el modelo; el validado pesa
    ~3,4 GB — actualizar al planificar `descarga-modelo`.
  - Requisito derivado de R1 pendiente de arrastrar a `extraccion-local`: stop
    token/`<end_of_turn>` y re-medición de latencia con prompts de extracción
    largos.
  - Ficheros sin trackear del spike (`docs/specs/spike-gemma-mediapipe.md`,
    `framework/ai/src/androidTest/`,
    `framework/ai/src/main/java/com/robertomr/tally/framework/ai/spike/`) +
    los 6 modificados, pendientes de commit.
- Next best step: commit de los cambios del spike (si el usuario lo pide) y
  siguiente feature con dependencias en verde (`navegacion-base` por orden de
  `feature_list.json`).

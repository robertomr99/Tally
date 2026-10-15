# Progress Log

## Current Verified State

- Repository root: `E:\AI-Expert\Tally`
- Standard startup path: `bash init.sh`
- Standard verification path: `gradlew.bat assembleDebug` + `gradlew.bat testDebugUnitTest`
  (+ `gradlew.bat :test:unit:test` para los tests puros de `:test:unit`)
- Emulador de referencia: `emulator-5554` (sdk_gphone64_x86_64; adb en
  `C:/Users/Roberto/AppData/Local/Android/Sdk/platform-tools`). Tab S9+ y
  realme GT 2 se reservan para `captura-foto` y `spike-gemma-mediapipe`.
- Current next ready feature: `captura-foto` está `accepted` (validación independiente 2026-10-15, sesión 014). Siguientes por orden de `feature_list.json`: `ocr-mlkit` (desbloqueada al aceptarse) y `periodo-selector` (desbloqueada por `persistencia-room`).
- Current blocker: ninguno. Pendientes operativos menores: revisión visual humana de las capturas de `docs/evidence/captura-foto-*` (el agente no inspecciona imágenes) y commit de la feature si el usuario lo pide.
- Last verified at: 2026-10-15 (sesión 014: `captura-foto` validada y aceptada por el feature-validator — gates JVM 23/23, instrumentados 17/17 en emulador `emulator-5554` y Tab S9+, y E2E 6/6 con instalación limpia, todo re-ejecutado en verde; check S2 de persistencia re-verificado en la Tab S9+)

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

### Session 008

- Date: 2026-10-13
- Goal: Implementar y cerrar `navegacion-base` (T1–T9 del plan del spec):
  shell de navegación con bottom bar de 3 destinos, verificación completa y
  documentación duradera.
- Completed:
  - T1 decisiones (confirmadas por el usuario antes de la implementación):
    **Decisión A** = crear `:feature:gastos` y `:feature:escanear` (patrón de
    `:feature:resumen`; registrados en `settings.gradle.kts` y `:app`), y
    criterio de dispositivo **enmendado a emulador** `emulator-5554` (mismo
    precedente que `bootstrap-gradle`, decisión del usuario 2026-09-30; los
    móviles físicos se reservan a `captura-foto` y features de Gemma).
  - T2 catálogo (`gradle/libs.versions.toml`): `navigationCompose = "2.9.8"`
    (última estable compatible con compileSdk 36; la 2.10.x exige compileSdk 37
    vía `lifecycle 2.11.0`), `androidxTestEspresso = "3.7.0"` (fuerza sobre el
    `3.5.0` transitivo de `ui-test-junit4`, que falla al inyectar eventos en
    API 35+: `NoSuchMethodException InputManager.getInstance` — las dos
    decisiones quedan justificadas en esta sesión), `material-icons-extended`
    y `ui-test-junit4`/`ui-test-manifest` gestionados por el Compose BOM.
  - T3 placeholders `GastosScreen`/`EscanearScreen` (módulos nuevos) +
    `testTag` en `ResumenScreen` (`pantalla_resumen`, `pantalla_gastos`,
    `pantalla_escanear`); sin ViewModel ni `XState` (regla del spec).
  - T4 `TallyRoute` (sealed class Resumen/Gastos/Escanear) + `TallyApp`
    (Scaffold + NavHost, start = Resumen) en `:app`; `MainActivity` compone
    `TallyTheme { TallyApp() }`. El `NavController` solo vive en `:app`.
  - T5 `TallyBottomBar` + `BottomBarDestino` en `:feature:common` (mockup 02:
    fila 80dp sobre `surface-container-low`, pill activo
    `secondary-container`/`on-secondary-container` + label semibold,
    inactivos `on-surface-variant`, área táctil ≥44dp); solo callbacks, sin
    `NavController`. Iconos `insights`/`receipt_long`/`document_scanner`.
  - T6 navegación bottom bar estándar (`launchSingleTop`, `restoreState`,
    `popUpTo(start) { saveState = true }`); back del sistema vuelve a Resumen
    y el segundo back sale de la app (escenario 4).
  - T7 `NavigationTest` en `:app/src/androidTest/` con
    `createAndroidComposeRule<MainActivity>()` (3 tests: arranque en Resumen +
    pill, navegación por los 3 destinos + pill, back doble). `:app` gana
    `testInstrumentationRunner` + `androidTestImplementation` (platform del
    Compose BOM declarado explícitamente porque `androidTestImplementation`
    no hereda el platform del convention plugin).
  - T8 verificación en verde (salidas reales abajo) + manual en `emulator-5554`
    + revisión de código. Hallazgo corregido: el assert de cierre del
    `NavigationTest` leía `isFinishing` síncrono y fallaba (tras el segundo
    back, `finishAfterTransition`/predictive back en API 35+ aplazan `finish()`);
    el comportamiento real se verificó a mano con `adb` (correcto) y el test
    pasó a espera activa al cierre. Navegación manual con taps reales verificada
    vía `uiautomator dump` (placeholder correcto en cada destino, pill activo
    por atributo `selected`).
  - T9 `docs/architecture.md` ya actualizado con `:feature:gastos` y
    `:feature:escanear`; `PROGRESS.md`, `feature_list.json` y spec actualizados.
- Verification run (todo 2026-10-13, Windows/`gradlew.bat`):
  - `.\gradlew.bat assembleDebug` → BUILD SUCCESSFUL (233 actionable tasks).
  - `.\gradlew.bat testDebugUnitTest` → BUILD SUCCESSFUL (regresión).
  - `.\gradlew.bat :test:unit:test --rerun` → BUILD SUCCESSFUL (re-ejecutado de
    verdad; JUnit XML `SmokeTest`: tests=1 failures=0 errors=0,
    `dummyTestPasses`).
  - `ANDROID_SERIAL=emulator-5554 .\gradlew.bat :app:connectedDebugAndroidTest`
    → BUILD SUCCESSFUL; `com.robertomr.tally.navigation.NavigationTest`
    tests=3 failures=0 errors=0 (5,161 s) en
    `Medium_Phone_API_36.1(AVD) - 16` (= `emulator-5554`):
    `startsOnResumenWithActivePill` (2,303 s),
    `bottomBarNavigatesAcrossTheThreeDestinations` (1,556 s),
    `systemBackReturnsToResumenAndNextBackExitsTheApp` (1,302 s). Primera
    ejecución: 2/3 en verde y fallo del assert de cierre (hallazgo arriba;
    corregido en el test, sin tocar producto).
  - Manual en `emulator-5554`: `adb install -r` Success; `am start -W` COLD
    1412 ms; taps en la bottom bar (Gastos → Escanear → back → back) con
    `uiautomator dump` confirmando placeholder y pill por destinos; back del
    sistema: back → Resumen (`selected="true"` en el item), segundo back →
    `mCurrentFocus` = launcher (actividad terminada; el proceso queda cacheado,
    comportamiento normal de Android).
  - Capturas: `docs/evidence/navegacion-base-resumen.png`,
    `navegacion-base-gastos.png`, `navegacion-base-escanear.png` (PNG válidos,
    36–42 KB, `screencap` + `adb pull`; revisión visual del pill a mano, como
    en `bootstrap-gradle`).
  - Revisión de código (escenario 3): `TallyRoute` como `sealed class` en
    `:app`; `NavHost` solo en `:app`; 0 usos de `NavController` en `:feature:*`
    (solo una cita en un comentario de `TallyBottomBar`); 0 declaraciones con
    versión literal en los 14 `**/*.gradle.kts` (solo un comentario en español
    cita "3.7.0/3.5.0"); `git diff init.sh` vacío (esta máquina no invoca
    `bash`; su contenido equivalente se ejecutó como los gates anteriores);
    `AGENTS.md`, ADRs y docs de descubrimiento sin diffs.
- Evidence captured: campo `evidence` de `navegacion-base` en
  `feature_list.json`; JUnit XML en
  `app/build/outputs/androidTest-results/connected/debug/` (informe HTML en
  `app/build/reports/androidTests/connected/debug/`); capturas en
  `docs/evidence/navegacion-base-*.png`.
- Files or artifacts updated: `gradle/libs.versions.toml`, `app/build.gradle.kts`,
  `settings.gradle.kts`, `feature/common/build.gradle.kts`,
  `feature/common/.../TallyBottomBar.kt`, `feature/gastos/`,
  `feature/escanear/`, `feature/resumen/.../ResumenScreen.kt`,
  `app/.../MainActivity.kt`, `app/.../navigation/{TallyRoute,TallyApp}.kt`,
  `app/src/androidTest/.../NavigationTest.kt`, `docs/architecture.md`,
  `docs/specs/navegacion-base.md`, `docs/evidence/navegacion-base-*.png`,
  `PROGRESS.md`, `feature_list.json`.
- Known risk or unresolved issue:
  - Revisión visual del pill/tema de la bottom bar: el agente no puede
    inspeccionar imágenes; la captura queda para revisión humana (mismo
    criterio que `bootstrap-gradle`). El estado activo del pill sí se verificó
    programáticamente (semántica `selected` en el E2E y atributo
    `selected="true"` en `uiautomator dump`).
  - Los módulos `:feature:gastos` y `:feature:escanear` no llevan `di.kt`:
    correcto por ahora (placeholders sin Hilt; `tally.hilt` llega con el primer
    ViewModel, hallazgo Low de la validación de `bootstrap-gradle`).
  - `navegacion-base` queda en `passing` (implementada y auto-verificada);
    `accepted` exige validación independiente (feature-validator) que el
    orquestador debe persistir.
- Next best step: validación independiente de `navegacion-base` con
  feature-validator; después, siguiente feature con dependencias aceptadas
  (`persistencia-room` por orden de `feature_list.json`).

### Session 009

- Date: 2026-10-13
- Goal: Validación independiente de `navegacion-base` con el subagente
  `validator` (feature-validator) y persistencia de la aceptación.
- Completed:
  - Validación independiente con feature-validator (subagente `validator`):
    veredicto **`accept`** sin hallazgos Critical/High/Medium.
  - Gates re-ejecutados por el validador en verde (2026-10-13):
    `.\gradlew.bat assembleDebug` (BUILD SUCCESSFUL, 233 actionable tasks),
    `.\gradlew.bat testDebugUnitTest` (BUILD SUCCESSFUL),
    `.\gradlew.bat :test:unit:test --rerun` (BUILD SUCCESSFUL; JUnit XML
    `SmokeTest` tests=1 failures=0 errors=0) y
    `$env:ANDROID_SERIAL="emulator-5554"; .\gradlew.bat :app:connectedDebugAndroidTest`
    (BUILD SUCCESSFUL; JUnit XML fresco `NavigationTest` tests=3 failures=0
    errors=0 en `Medium_Phone_API_36.1(AVD) - 16`, borrando los resultados
    previos antes de ejecutar: `startsOnResumenWithActivePill` 2,42 s,
    `bottomBarNavigatesAcrossTheThreeDestinations` 1,70 s,
    `systemBackReturnsToResumenAndNextBackExitsTheApp` 1,60 s).
  - Escenarios 1-4 del spec cumplidos: navegación entre los 3 destinos,
    pill activo (tokens de `TallyBottomBar.kt` contrastados con el mockup 02 y
    `DESIGN.md`; semántica `selected` verificada por E2E), rutas `sealed class`
    en `:app` sin `NavController` en `:feature:*` (grep repo-wide) y back del
    sistema correcto (back → Resumen; segundo back → la actividad termina).
  - Hallazgo declarado en la sesión 008 (assert de cierre con `isFinishing`
    síncrono vs. predictive back en API 35+) **confirmado** como corrección
    legítima solo de test: cero cambios de producto en el manejo de back,
    el assert falla igual si la app no sale, y el test corregido pasa en
    API 36.1.
  - Arquitectura y reglas verificadas: alcance solo de navegación (sin FAB,
    ViewModels, Room, cámara, OCR ni IA), `init.sh`/`AGENTS.md`/`build-logic/`/
    ADRs/docs de descubrimiento sin diffs, módulos nuevos siguiendo el patrón
    de `:feature:resumen`, sin secretos ni credenciales, versiones solo en el
    catálogo (única coincidencia literal: un comentario en español en
    `app/build.gradle.kts`).
  - Documentación duradera contrastada con artefactos: JUnit XML citado en la
    evidencia coincide al detalle con lo registrado; `docs/architecture.md`,
    spec y `feature_list.json` coherentes.
  - Persistida la aceptación: `feature_list.json` status de `navegacion-base`
    → `accepted` (nota con fecha, alcance de la validación y findings Low).
- Verification run: ver "Completed" (gates re-ejecutados por el validador;
  inspección de JUnit XML en
  `app/build/outputs/androidTest-results/connected/debug/`).
- Evidence captured: informe de validación (veredicto `accept`) registrado en
  las notas de `feature_list.json` y en esta sesión; 2 findings Low
  no bloqueantes anotados.
- Files or artifacts updated: `feature_list.json` (status `accepted` + nota),
  `PROGRESS.md`.
- Known risk or unresolved issue:
  - 2 findings Low no bloqueantes: (1) el label de items inactivos de
    `TallyBottomBar.kt` usa `FontWeight.Medium` mientras que el mockup 02 no
    fija peso para inactivos (desviación cosmética; `DESIGN.md` solo exige
    semibold en el activo); (2) las 3 capturas de `docs/evidence/navegacion-base-*.png`
    no han sido revisadas visualmente por ningún agente (no soportan imagen) —
    queda revisión humana residual, aunque el pill está verificado
    semánticamente (E2E `assertIsSelected` + atributo `selected` en
    `uiautomator`).
  - Ficheros sin trackear de la feature (`docs/specs/navegacion-base.md`,
    `feature/gastos/`, `feature/escanear/`,
    `feature/common/.../TallyBottomBar.kt`,
    `app/src/main/java/com/robertomr/tally/navigation/`,
    `app/src/androidTest/`, capturas) + los 8 modificados, pendientes de commit.
- Next best step: commit de los cambios de `navegacion-base` (si el usuario lo
  pide) y siguiente feature con dependencias aceptadas (`persistencia-room`
  por orden de `feature_list.json`; `captura-foto` también queda desbloqueada
  al depender de `navegacion-base`).

### Session 010

- Date: 2026-10-13
- Goal: Implementar y cerrar `persistencia-room` (T1–T9 del plan del spec):
  capa de persistencia local con Room (`tickets`/`lineas`, borrado en cascada),
  repositorio en dominio, verificación completa (con tests instrumentados en
  `emulator-5554`) y documentación duradera.
- Completed:
  - Revisión del código ya presente en el árbol de trabajo contra el spec
    (escenarios 1–4) **sin desviaciones que corregir**: upsert transaccional con
    reemplazo del conjunto de líneas, borrado en cascada por FK + foto
    best-effort con orden DB-antes-foto y no-op para id inexistente,
    `observeByDateRange` como `Flow` con rango inclusivo por fecha de emisión
    (codificación epoch-millis UTC coherente entre `Converters` y la consulta),
    dominio sin imports de Android/Room, `Flow` en DAO, `@Transaction` en la
    escritura multi-tabla, implementaciones `internal` en `framework` y un solo
    `di.kt` por módulo. Se completó/verificó lo existente sin duplicar trabajo
    (0 ficheros de producto reescritos).
  - T1 catálogo (`gradle/libs.versions.toml`): `javaxInject = 1`
    (`javax.inject`) + `dagger` y `dagger-compiler` (comparten la línea de
    versión `hilt` 2.60.1; `com.google.dagger` publica las tres juntas).
    `:domain:expenses` gana `javax.inject` + plugin ksp + `ksp(libs.dagger.compiler)`
    + `implementation(libs.dagger)` para las fábricas generadas del
    `@Inject constructor` de `TicketRepository` (no hizo falta la contingencia
    `@Provides` en `DatabaseModule`).
  - T2–T3 dominio `:domain:expenses`: entidades `Ticket`/`Linea`/`Categoria`/
    `Motor` (nombres de dominio según `CONTEXT.md`) en `entities/` y
    `TicketDataSource`/`PhotoFileStore`/`TicketRepository` (`@Inject constructor`)
    en `data/`; `delete(id)` = `getById` → `dataSource.delete` (cascada) →
    `photoFileStore.delete(rutaFoto)` best-effort (DB-antes-foto).
  - T4–T5 Room `:framework:database`: `TicketEntity`/`LineaEntity` (FK
    `ON DELETE CASCADE` + índice en `ticket_id`, columnas snake_case),
    `TicketDao` (`@Transaction suspend fun upsert` con delete/insert del conjunto
    de líneas; lecturas `Flow` con `distinctUntilChanged`), `TallyDatabase` v1
    (`exportSchema = true`, esquema en `schemas/.../1.json`), `Converters`
    (`LocalDateTime` ↔ epoch millis UTC), `TicketWithLineas` (relación 1-N),
    `TicketLocalDataSource`/`PhotoFileStoreImpl` `internal` y bindings
    `@Provides` en `di/di.kt` (patrón de `framework/ai`).
  - T6–T7 dobles `FakeTicketDataSource`/`FakePhotoFileStore` en
    `test/unit/src/main/kotlin/` (`:test:unit` con `api(:domain:expenses)`) y
    `TicketRepositoryTest` (6 tests con Turbine; sin ciclo de dependencias:
    `:test:unit` solo es `testImplementation` en `:domain:expenses`).
  - T8 `TicketDaoTest` (5 tests, Room in-memory) + `PhotoFileStoreTest`
    (4 tests, fichero real en `filesDir`) en `framework/database/src/androidTest/`.
  - T9 verificación completa en verde (abajo) + revisión de código (escenario 4) +
    `docs/architecture.md` (sección Persistencia: tablas `tickets`/`lineas` y
    "Room es la única fuente de verdad") + `PROGRESS.md` + `feature_list.json`.
- Verification run (todo 2026-10-13, Windows/`gradlew.bat`):
  - `.\gradlew.bat assembleDebug` → BUILD SUCCESSFUL (236 actionable tasks).
  - `.\gradlew.bat testDebugUnitTest` → BUILD SUCCESSFUL (regresión).
  - `.\gradlew.bat :test:unit:test --rerun` → BUILD SUCCESSFUL (JUnit XML
    `SmokeTest`: tests=1 failures=0 errors=0, `dummyTestPasses`, 0,056 s).
  - `.\gradlew.bat :domain:expenses:test --rerun` → BUILD SUCCESSFUL (JUnit XML
    `TicketRepositoryTest`: tests=6 failures=0 errors=0, 0,083 s):
    `saveAssignsIdAndReturnsAggregateFromGetById`,
    `saveWithExistingIdUpdatesTicketInPlace`,
    `deleteRemovesTicketAndDeletesPhotoFile`,
    `deleteWithUnknownIdIsNoOpAndDoesNotTouchPhotoStore`,
    `deleteRemovesTicketEvenIfPhotoDeletionFails`,
    `observeByDateRangeEmitsInRangeTicketsIncludingBoundariesAndUpdatesOnSave`.
  - `$env:ANDROID_SERIAL="emulator-5554"; .\gradlew.bat :framework:database:connectedDebugAndroidTest`
    → BUILD SUCCESSFUL (9 tests, 0 fallos) en `Medium_Phone_API_36.1(AVD) - 16`
    (= `emulator-5554`): `TicketDaoTest` tests=5 failures=0 errors=0
    (`upsertSavesTicketWithLinesAsAggregate`, `upsertReplacesTheWholeLineSetOnResave`,
    `deleteByIdRemovesTicketAndLinesInCascade`, `deleteByIdIsNoOpForUnknownId`,
    `observeByDateRangeFiltersByEmissionDateAndEmitsOnSave`) + `PhotoFileStoreTest`
    tests=4 failures=0 errors=0 (`deleteRemovesExistingFileGivenRelativeName`,
    `deleteRemovesExistingFileGivenAbsolutePath`,
    `deleteReturnsFalseForMissingFileWithoutCrashing`,
    `deleteReturnsFalseForBlankPath`).
  - Revisión de código (escenario 4): 0 imports de Android/Room en `domain/**`;
    DAO con `Flow` y `@Transaction` en la escritura multi-tabla;
    `TicketLocalDataSource`/`PhotoFileStoreImpl`/`DatabaseModule` `internal` en
    `framework`; un solo `di.kt` por módulo; 0 declaraciones con versión literal
    en los `**/*.gradle.kts` (única coincidencia del patrón de búsqueda: un
    comentario en español en `app/build.gradle.kts`, sin declaración); `git diff`
    de `init.sh`/`AGENTS.md`/`build-logic/`/ADRs/docs de descubrimiento vacío
    (esta máquina no invoca `bash`; equivalente = los gates anteriores).
- Evidence captured: campo `evidence` de `persistencia-room` en
  `feature_list.json`; JUnit XML en
  `domain/expenses/build/test-results/test/`,
  `test/unit/build/test-results/test/` y
  `framework/database/build/outputs/androidTest-results/connected/debug/`
  (informe HTML en `framework/database/build/reports/androidTests/connected/debug/`).
- Files or artifacts updated: fuentes de la feature (dominio, Room, dobles y
  tests ya en el árbol; 0 correcciones necesarias), `docs/specs/persistencia-room.md`
  (T1–T9 `[x]`), `docs/architecture.md` (sección Persistencia), `PROGRESS.md`,
  `feature_list.json` (status `passing` + evidence).
- Known risk or unresolved issue:
  - `persistencia-room` queda en `passing` (implementada y auto-verificada);
    `accepted` exige validación independiente (feature-validator) que el
    orquestador debe persistir.
  - El wiring de `di/di.kt` (Hilt en runtime) no tiene test instrumentado
    propio: se verifica por compilación y agregación Hilt en `assembleDebug`
    (mismo criterio que features anteriores); los tests de DAO construyen la
    base con `Room.inMemoryDatabaseBuilder` directamente.
  - Borradores (estado Borrador del domain-model) no se persisten: fuera de
    scope del spec (riesgo conocido, llega con `procesado-ui`).
- Next best step: validación independiente de `persistencia-room` con
  feature-validator; después, siguiente feature con dependencias aceptadas
  (`captura-foto` por orden de `feature_list.json`; `periodo-selector` queda
  desbloqueada al aceptarse esta).

### Session 011

- Date: 2026-10-13
- Goal: Validación independiente de `persistencia-room` con el subagente
  `validator` (feature-validator) y persistencia de la aceptación.
- Completed:
  - Validación independiente con feature-validator (subagente `validator`):
    veredicto **`accept`** sin hallazgos Critical/High/Medium.
  - Gates re-ejecutados por el validador en verde (2026-10-13, borrando antes
    resultados de tests previos para asegurar frescura):
    `.\gradlew.bat assembleDebug` (BUILD SUCCESSFUL, 236 actionable tasks),
    `.\gradlew.bat testDebugUnitTest` (BUILD SUCCESSFUL),
    `.\gradlew.bat :test:unit:test --rerun` (BUILD SUCCESSFUL; JUnit XML
    `SmokeTest` tests=1 failures=0 errors=0),
    `.\gradlew.bat :domain:expenses:test --rerun` (BUILD SUCCESSFUL; JUnit XML
    `TicketRepositoryTest` tests=6 failures=0 errors=0) y
    `$env:ANDROID_SERIAL="emulator-5554"; .\gradlew.bat :framework:database:connectedDebugAndroidTest`
    (BUILD SUCCESSFUL; JUnit XML fresco `TicketDaoTest` tests=5 failures=0
    errors=0 + `PhotoFileStoreTest` tests=4 failures=0 errors=0 en
    `Medium_Phone_API_36.1(AVD) - 16`, timestamp 2026-10-13T20:52:51). Nota: el
    validador detectó un dispositivo físico conectado además del emulador y fijó
    `ANDROID_SERIAL` para garantizar el dispositivo de referencia.
  - Escenarios 1-4 del spec cumplidos y contrastados con los asserts leídos
    (no solo con nombres): upsert `@Transaction` con reemplazo del conjunto de
    líneas, cascada FK `ON DELETE CASCADE` confirmada en
    `schemas/.../1.json` + borrado best-effort de la foto (orden DB-antes-foto,
    no-op para id inexistente, robusto si falla la foto), `observeByDateRange`
    por `Flow` con rango inclusivo por fecha de emisión (bordes 01-10 00:00 y
    31-10 23:59) y `distinctUntilChanged`, y límites de módulos verificados
    (0 imports de Android/Room en `domain/**`, DAOs con `Flow`, `@Transaction`,
    implementaciones `internal`, un `di.kt` por módulo).
  - Evidencia contrastada: las 9 entradas de `evidence` de `feature_list.json`
    y la sesión 010 coinciden 1:1 con los JUnit XML reales (nombres de test,
    counts, dispositivo; única divergencia: tiempos de ejecución, deriva
    normal entre corridas).
  - Arquitectura, alcance y documentación verificados: diff igual a
    `Expected File Changes` del spec (sin UI, usecases/ViewModels, captura,
    OCR/IA, migraciones ni seeds), 0 versiones literales fuera del toml
    (única coincidencia: comentario en español en `app/build.gradle.kts`),
    `init.sh`/`AGENTS.md`/`build-logic/`/ADRs/docs de descubrimiento sin diffs,
    `docs/architecture.md` con la sección Persistencia correcta, sin secretos
    ni credenciales.
  - Cobertura E2E persistente "no aplica" verificada como justificación
    legítima y registrada (feature sin UI ni flujos de usuario; superficie
    cubierta por tests instrumentados de DAO y por las features consumidoras).
  - Persistida la aceptación: `feature_list.json` status de
    `persistencia-room` → `accepted` (nota con fecha, alcance de la
    validación y findings Low); Validator Checklist del spec completado.
- Verification run: ver "Completed" (gates re-ejecutados por el validador;
  inspección de JUnit XML en
  `framework/database/build/outputs/androidTest-results/connected/debug/`,
  `domain/expenses/build/test-results/test/` y `test/unit/build/test-results/test/`).
- Evidence captured: informe de validación (veredicto `accept`) registrado en
  las notas de `feature_list.json` y en esta sesión; 2 findings Low
  no bloqueantes y 1 nota teórica anotados.
- Files or artifacts updated: `feature_list.json` (status `accepted` + nota),
  `PROGRESS.md`, `docs/specs/persistencia-room.md` (Validator Checklist).
- Known risk or unresolved issue:
  - 2 findings Low no bloqueantes: (1) `TicketRepositoryTest` usa `runTest`
    directo en vez del `CoroutineTestRule` que pide `AGENTS.md` (sin impacto
    funcional: nada bajo prueba toca `Dispatchers.Main`; si se quiere pureza,
    añadir la regla compartida en `:test:unit` cuando lleguen los ViewModels);
    (2) `Expected File Changes` del spec omite `entity/TicketWithLineas.kt` y
    la ruta de tests del plan (`src/test/kotlin/`) no coincide con la
    implementación (`src/test/java/`) — nit de documentación a corregir al
    cerrar la spec.
  - Nota teórica de seguridad: `PhotoFileStoreImpl` acepta y borra rutas
    absolutas (intencional y testeado; la ruta viene de la BD privada). Cuando
    se implemente `captura-foto`, guardar nombres relativos bajo `filesDir`
    (el código ya lo soporta).
  - Desviación ya auto-declarada y confirmada: el wiring Hilt de `di/di.kt`
    se verifica solo por compilación/agregación Hilt (los tests DAO usan
    `Room.inMemoryDatabaseBuilder` directamente); mismo criterio que features
    anteriores.
  - Ficheros sin trackear de la feature (`docs/specs/persistencia-room.md`,
    `domain/expenses/src/`, `framework/database/schemas/`,
    `framework/database/src/androidTest/`, fuentes nuevas de
    `framework/database/` y `test/unit/src/main/`) + los 5 modificados,
    pendientes de commit.
- Next best step: commit de los cambios de `persistencia-room` (si el usuario
  lo pide) y siguiente feature con dependencias aceptadas (`captura-foto` por
  orden de `feature_list.json`; `periodo-selector` también queda desbloqueada).

### Session 012

- Date: 2026-10-15
- Goal: Terminar `captura-foto` (tercera reanudación): validar el trabajo
  previo sin commit del árbol, arreglar los gates que fallaban, ejecutar la
  verificación en emulador y dispositivo físico, actualizar docs durables y
  cerrar la feature con evidencia real.
- Completed:
  - Validación del árbol previo frente a `Expected File Changes` del spec:
    todos los ficheros previstos estaban presentes (dominio `PhotoProcessor` /
    `PhotoFileStore.save` / `GuardarFotoCapturada`, framework
    `PhotoPipelineMath` + `CapturedPhotoProcessor` + `PhotoFileStoreImpl.save` +
    binding en `di/di.kt`, feature `EscanearState`/`EscanearViewModel` /
    `CameraEncuadre` / `RevisionFotoContent` / `EscanearScreen`, fixture
    `ticket_exif_rotado.jpg`, tests JVM/instrumentados/E2E).
  - Arreglo 1 (`EscanearViewModelTest.withoutPermissionStateStaysPending`):
    el test pedía una emisión nueva a un `StateFlow` cuyo valor no cambia
    (conflación por igualdad; sin permiso el estado sigue en `PermisoPendiente`).
    Corregido el test (espera a que la captura termine, `expectNoEvents()` +
    comprobación de `estado.value`); el producto no cambió.
  - Arreglo 2 (`CapturedPhotoProcessorTest.processBakesExif...`): el assert
    "pixel opuesto no debería ser rojo" comparaba `Color.red < 120` y confundía
    el blanco del fixture (red=255) con rojo; corregido a "no predominantemente
    rojo". El pipeline ya salía correcto (720x1280, marcador arriba-izquierda).
  - Arreglo 3 (`CapturaFlujoTest`): el diseño con `pm revoke` en caliente era
    inviable — Android mata el proceso de la app bajo instrumentación al
    revocar un permiso runtime (reproducido: logcat `Killing 5629:com.robertomr.tally
    (adj 0): permissions revoked`, "Process crashed", la tanda entera se abortaba
    y `NavigationTest` no llegaba a ejecutarse). Diseño final: sin
    `GrantPermissionRule`, `@FixMethodOrder(NAME_ASCENDING)` con
    `denegadoMuestraEstadoInAppSinCrash` primero (estado limpio de instalación)
    y `pm grant` (seguro) en los tests con permiso, antes de navegar a Escanear
    (el gate se evalúa al componer `EscanearScreen`).
  - Gates JVM en verde: `assembleDebug`, `testDebugUnitTest`,
    `:test:unit:test --rerun`, `:domain:expenses:test --rerun`,
    `:framework:database:testDebugUnitTest` (23 tests JVM, 0 fallos).
  - Gates instrumentados en verde en `emulator-5554`:
    `:framework:database:connectedDebugAndroidTest` 17/17 y
    `:app:connectedDebugAndroidTest` 6/6 (CapturaFlujoTest 3/3 +
    NavigationTest 3/3). La cámara virtual del emulador SÍ entregó frames: el
    E2E completo (obturador → pantalla 05 con foto, "Continuar" deshabilitado,
    "Repetir foto" → encuadre) pasó sin assumption-skip.
  - Verificación manual en emulador (escenarios 2, 5 y 6): captura real con
    taps, pantalla 05, persistencia del fichero tras `force-stop`
    (`files/fotos/captura_6818f724-….jpg`, 104625 bytes, nombre relativo) y
    foto procesada real de 960x1280 (lado largo 1280); permiso denegado con
    estado in-app y copy "Permiso denegado…" tras negar el diálogo del sistema,
    sin crash.
  - Tests del pipeline en físico: `:framework:database:connectedDebugAndroidTest`
    17/17 también en la Tab S9+ (SM-X810).
  - Docs durables actualizadas: `docs/architecture.md` (sección Captura de
    foto: pipeline → `filesDir/fotos/` con nombre relativo; CameraX en
    `:feature:escanear`), `docs/domain-model.md` ("Repetir foto" borra la foto
    del borrador reemplazado best-effort),
    `docs/risks-and-open-questions.md` (pregunta EXIF/compresión cerrada:
    EXIF horneada en píxeles, ≤1280 px lado largo, JPEG ~85) y
    `docs/specs/captura-foto.md` (T1-T11 [x], T12 con estado parcial +
    Implementation Notes con las desviaciones).
- Verification run:
  - `gradlew.bat assembleDebug` → BUILD SUCCESSFUL.
  - `gradlew.bat testDebugUnitTest` → BUILD SUCCESSFUL.
  - `gradlew.bat :test:unit:test --rerun` → BUILD SUCCESSFUL (SmokeTest 1/1).
  - `gradlew.bat :domain:expenses:test --rerun` → BUILD SUCCESSFUL
    (GuardarFotoCapturadaTest 4/4 + TicketRepositoryTest 6/6).
  - `gradlew.bat :framework:database:testDebugUnitTest` → BUILD SUCCESSFUL
    (PhotoPipelineMathTest 6/6).
  - `ANDROID_SERIAL=emulator-5554 gradlew.bat :framework:database:connectedDebugAndroidTest`
    → BUILD SUCCESSFUL, 17 tests, 0 fallos.
  - `ANDROID_SERIAL=emulator-5554 gradlew.bat :app:connectedDebugAndroidTest`
    → BUILD SUCCESSFUL, 6 tests, 0 fallos, 0 skipped.
  - `ANDROID_SERIAL=<Tab-SM-X810> gradlew.bat :framework:database:connectedDebugAndroidTest`
    → BUILD SUCCESSFUL, 17 tests, 0 fallos.
  - Manual en emulador: install/grant/captura/run-as/force-stop (abajo).
- Evidence captured:
  - JUnit XML: `*/build/test-results/` (23 tests JVM, 0 failures),
    `framework/database/build/outputs/androidTest-results/connected/debug/TEST-SM-X810 - 16-_framework_database-.xml`
    (17 tests, 0 failures; run del emulador con el mismo contenido registrado en
    la evidencia de `feature_list.json`),
    `app/build/outputs/androidTest-results/connected/debug/TEST-Medium_Phone_API_36.1(AVD) - 16-_app-.xml`
    (6 tests, 0 failures, 0 skipped).
  - Screenshots: `docs/evidence/captura-foto-05-emulador.png` (898941 bytes,
    pantalla 05 tras captura real),
    `docs/evidence/captura-foto-permiso-emulador.png` (66778 bytes, gate de
    permiso), `docs/evidence/captura-foto-permiso-denegado-emulador.png`
    (83319 bytes, copy "Permiso denegado…"); revisión visual de las imágenes
    pendiente de mano humana (el agente no inspecciona imágenes).
  - Persistencia (escenario 2): `run-as com.robertomr.tally ls -l files/fotos`
    antes y después de `am force-stop` → mismo fichero
    `captura_6818f724-9ce9-4d95-a644-0086b7ce2b3a.jpg` (104625 bytes); foto
    extraída con `exec-out`: JPEG 960x1280.
  - `uiautomator dump` de los estados de pantalla 05, encuadre, permiso y
    diálogo de denegación del sistema (textos citados en la evidencia de
    `feature_list.json`).
  - Todo registrado con fecha 2026-10-15 en `feature_list.json` (9 entradas de
    `evidence`, sin inventar nada).
- Files or artifacts updated:
  - Producto: `feature/escanear/src/test/.../EscanearViewModelTest.kt` (arreglo
    de carrera), `framework/database/src/androidTest/.../CapturedPhotoProcessorTest.kt`
    (arreglo de assert), `app/src/androidTest/.../CapturaFlujoTest.kt`
    (rediseño del E2E de permisos).
  - Docs: `docs/architecture.md`, `docs/domain-model.md`,
    `docs/risks-and-open-questions.md`, `docs/specs/captura-foto.md`.
  - Harness: `feature_list.json` (`captura-foto` → `passing` + evidencia),
    `PROGRESS.md` (esta sesión).
  - Evidencia: `docs/evidence/captura-foto-*.png` (3 ficheros nuevos).
- Known risk or unresolved issue:
  - Verificación física PENDIENTE de revisión humana (T12): la Tab S9+
    (SM-X810) está conectada por ADB inalámbrico y acepta install/grant/start,
    pero su pantalla está bloqueada y no despierta vía adb
    (`mWakefulness=Dozing`, `mState=OFF`, `locksettings get-disabled=false`,
    batería al 8% sin cargar) → sin captura de cámara física de papel ni
    `captura-foto-05-tab.png`; el realme GT 2 no está conectado a esta máquina.
    Los tests instrumentados no-UI sí pasaron en la Tab (17/17). Los E2E de UI
    en la Tab fallan con "No compose hierarchies found in the app" por la
    pantalla apagada (limitación ambiental, no bug de producto). Pasos para la
    revisión humana en el T12 del spec.
  - Desviación auto-declarada: `androidx.test:rules` queda en el catálogo (T2
    del spec) aunque `CapturaFlujoTest` final no use `GrantPermissionRule` (el
    `pm revoke` en caliente mata el proceso bajo test; ver Implementation Notes
    del spec).
  - Revisión visual de los 3 screenshots pendiente de mano humana (patrón del
    repo: el agente no inspecciona imágenes).
  - `EscanearViewModelTest` usa esperas activas acotadas (Thread.sleep con
    timeout) para el borrado best-effort en `Dispatchers.IO`; determinista pero
    no ideal si algún día hay muchos tests.
  - Todo el árbol de la feature sigue sin commit (decisión de este flujo).
- Next best step: ejecutar `feature-validator` sobre `captura-foto` (paso de
  aceptación independiente) y, en paralelo, la revisión humana de la
  verificación física (encender/desbloquear Tab S9+, capturar ticket real,
  screenshots `captura-foto-05-tab.png` / `captura-foto-05-realme.png`).

## Session 013 (2026-10-15) — Verificación física T12 de captura-foto en Tab S9+

- Goal: completar la verificación física pendiente (T12) de `captura-foto` en la
  Tab S9+ (SM-X810) con ticket de papel real; sin tocar código de producto.
- Completed:
  - Estado del dispositivo: desbloqueada, `mWakefulness=Awake`, batería 14 % y
    subiendo por AC (antes 8 % en la sesión 012), Android 16/API 36, 1752x2800.
  - `gradlew.bat :app:installDebug` con `ANDROID_SERIAL=adb-R52W60AFAPZ-...`
    → instalada; `pm grant ... CAMERA`; arranque COLD 895 ms; pestaña Escanear
    → encuadre mockup 04 con hint y "Cerrar".
  - Escenario 1/5 (captura vertical): obturador → pantalla 05 ("Repetir foto" +
    "Continuar"), `docs/evidence/captura-foto-05-tab.png`; foto
    `fotos/captura_79d34873-e0c0-4d3f-afea-c52ba395b10e.jpg` (363972 bytes),
    JPEG 960x1280 (lado largo 1280) con 0 segmentos EXIF APP1 (parser JPEG
    propio sobre la cabecera).
  - Escenario 2 (persistencia): tras `am force-stop`, `run-as ... ls -l
    files/fotos` muestra el mismo fichero con el mismo tamaño.
  - Escenario 3 end-to-end (girada): `user_rotation 1` (actividad recreada;
    primer tap de obturador no capturó, reintento OK) → pantalla 05,
    `docs/evidence/captura-foto-05-tab-girada.png`; foto
    `captura_7e753987-...jpg` (262304 bytes), JPEG 1280x960 (dimensiones
    intercambiadas, lado largo 1280) con 0 EXIF. Rotación automática restaurada.
  - Escenario 7 ("Repetir foto"): el borrador reemplazado se borra de disco
    (best-effort) y la captura siguiente solo deja la nueva
    (`captura_41d02c1c-...jpg`, 153345 bytes).
- Verification run: comandos adb exactos en `feature_list.json` (evidencia
  "2026-10-15 T12 ..."); análisis JPEG (dimensiones + segmentos EXIF) con
  script PowerShell inline; sin cambios de código → no se reejecutó
  `connectedDebugAndroidTest` (regresión NavigationTest 3/3 de la sesión 012
  sigue válida).
- Evidence: `docs/evidence/captura-foto-05-tab.png` (5413375 bytes),
  `captura-foto-05-tab-foto.jpg` (363972), `captura-foto-05-tab-girada.png`
  (2890852), `captura-foto-05-tab-girada-foto.jpg` (262304),
  `captura-foto-preview-tab.png` (5413656, encuadre con la escena del ticket).
  Cuidado operativo: `adb exec-out screencap -p > file` en PowerShell 5.1
  corrompe el binario (UTF-16); usar `adb shell screencap` + `adb pull`.
- Files updated: `docs/specs/captura-foto.md` (T12 [x] + nota 6),
  `feature_list.json` (5 evidencias T12 + nota), `PROGRESS.md` (esta sesión).
  Cero cambios en código de producto.
- Known risks: revisión visual de los PNG/JPG pendiente de mano humana (este
  agente no puede inspeccionar imágenes; la verificación "foto derecha" se hizo
  por dimensiones intercambiadas + ausencia de EXIF + pantalla 05 en uiautomator);
  la huérfana `captura_79d34873-...jpg` queda en `files/fotos` de la tablet como
  resto de la prueba de persistencia; realme GT 2 sigue sin conectar.
- Next best step: `feature-validator` sobre `captura-foto` (aceptación
  independiente) y revisión humana de los screenshots nuevos.

### Session 014

- Date: 2026-10-15
- Goal: Validación independiente de `captura-foto` con el subagente `validator`
  (feature-validator) y persistencia de la aceptación.
- Completed:
  - Validación independiente con feature-validator (subagente `validator`):
    veredicto **`accept`** sin hallazgos Critical/High/Medium; 4 findings Low
    no bloqueantes (detalle en "Known risk").
  - Gates re-ejecutados por el validador en verde (2026-10-15, sin fiarse de la
    evidencia del implementer):
    - `.\gradlew.bat assembleDebug` → BUILD SUCCESSFUL.
    - `.\gradlew.bat testDebugUnitTest --rerun` → BUILD SUCCESSFUL.
    - `.\gradlew.bat :test:unit:test --rerun` → BUILD SUCCESSFUL (JUnit XML
      `SmokeTest` tests=1 failures=0 errors=0).
    - `.\gradlew.bat :domain:expenses:test --rerun` → BUILD SUCCESSFUL (JUnit
      XML `GuardarFotoCapturadaTest` tests=4 + `TicketRepositoryTest` tests=6,
      0 failures).
    - `.\gradlew.bat :framework:database:testDebugUnitTest --rerun` → BUILD
      SUCCESSFUL (JUnit XML `PhotoPipelineMathTest` tests=6 failures=0).
    - JUnit XML frescos (19:30) de `*/build/test-results/`: 23 tests JVM,
      0 failures — coincide 1:1 con la evidencia declarada (4+6+6+6+1).
    - `$env:ANDROID_SERIAL="emulator-5554"; .\gradlew.bat
      :framework:database:connectedDebugAndroidTest` → BUILD SUCCESSFUL,
      "Starting 17 tests / Finished 17 tests" en `Medium_Phone_API_36.1(AVD) - 16`.
    - `$env:ANDROID_SERIAL="emulator-5554"; .\gradlew.bat
      :app:connectedDebugAndroidTest` → BUILD SUCCESSFUL, 6 tests, 0 failures,
      0 skipped con INSTALACIÓN LIMPIA (la app no estaba instalada en el
      emulador, así que `denegadoMuestraEstadoInAppSinCrash` se ejecutó de
      verdad y no saltó). JUnit XML fresco (19:35): `CapturaFlujoTest` 3/3 +
      `NavigationTest` 3/3 (regresión de navegacion-base intacta).
    - `$env:ANDROID_SERIAL="adb-R52W60AFAPZ-izULbK._adb-tls-connect._tcp";
      .\gradlew.bat :framework:database:connectedDebugAndroidTest` (Tab S9+)
      → BUILD SUCCESSFUL; JUnit XML fresco (19:36:37)
      `TEST-SM-X810 - 16-_framework_database-.xml`: `CapturedPhotoProcessorTest`
      4/4 + `PhotoFileStoreTest` 8/8 + `TicketDaoTest` 5/5 = 17/17.
    - Check S2 de persistencia re-verificado de forma independiente en la Tab
      S9+: `run-as com.robertomr.tally ls -l files/fotos` idéntico antes y
      después de `am force-stop` (mismo `captura_79d34873-…jpg` 363972 bytes +
      `captura_dd72dd37-…jpg` 320403 bytes; nombres relativos bajo
      `files/fotos`; `pidof` vacío tras force-stop).
    - `bash init.sh`: `git diff init.sh` vacío (bash no disponible en esta
      máquina; gates equivalentes re-ejecutados directamente — patrón de las
      sesiones 007/009/011).
  - Evidencia de imágenes verificada de forma independiente por metadatos
    (System.Drawing, sin inspección visual): fixture `ticket_exif_rotado.jpg`
    (62021 bytes, 1600x900, EXIF Orientation=6 — coherente con T8),
    `captura-foto-05-tab-foto.jpg` (960x1280, sin EXIF, 363972 bytes — coherente
    con la evidencia T12 y con el fichero aún presente en el dispositivo),
    `captura-foto-05-tab-girada-foto.jpg` (1280x960, sin EXIF, 262304 bytes —
    dimensiones intercambiadas = escenario 3 end-to-end) y screenshots con
    resolución coherente (Tab 1752x2800 / 2800x1752 girada; emulador
    1080x2400) y tamaños idénticos a los citados en `feature_list.json`.
  - Escenarios 1-7 contrastados con los asserts leídos (no solo nombres):
    encuadre guiado → pantalla 05 (E2E + manual emulador/Tab), persistencia al
    instante con nombre relativo (tests + `run-as`), EXIF horneada (fixture
    Orientation=6 → 720x1280 + captura girada 1280x960 sin tags), compresión
    ≤1280 px lado largo JPEG 85 sin guardar el original (CameraX escribe en
    `ByteArrayOutputStream`, sin fichero temporal), pantalla 05 con testTags
    `captura_revision`/`captura_foto`/`captura_repetir`/`captura_continuar` y
    "Continuar" deshabilitado (assertIsNotEnabled), permiso denegado in-app en
    español sin crash con `pantalla_escanear` presente, y "Repetir foto" que
    borra el borrador reemplazado (VM test + huellas en la Tab).
  - Alcance, arquitectura y seguridad verificados: diff igual a `Expected File
    Changes` del spec; sin OCR/IA ni MediaStore (búsqueda), sin Room/
    `TicketRepository`/schemas tocados, sin decoración ficticia del mockup 04/05
    (solo comentarios que la declaran NO implementada), sin módulos Gradle
    nuevos (`settings.gradle.kts` sin diff) ni rutas nuevas en `:app`, contratos
    `PhotoProcessor`/`PhotoFileStore.save` en `domain/expenses/data` con
    implementaciones `internal` en `framework/database`, caso de uso con
    `operator fun invoke` + `@Inject constructor`, VM sin imports de Compose y
    estado derivado con `combine`/`flatMapLatest`, 0 versiones literales en
    `**/*.gradle.kts` (única coincidencia: comentario en español de Espresso),
    `save` devuelve SIEMPRE `fotos/captura_<uuid>.jpg` relativo bajo `filesDir`
    (contrato + tests + `run-as`), fuentes UTF-8 válidas (UI sin mojibake).
  - Docs durables coherentes con el código: `docs/architecture.md` (sección
    Captura de foto), `docs/domain-model.md` ("Repetir foto" best-effort),
    `docs/risks-and-open-questions.md` (pregunta EXIF/compresión cerrada).
  - Persistida la aceptación: `feature_list.json` status de `captura-foto` →
    `accepted` (evidencia de validación + nota con fecha); Validator Checklist
    del spec completado.
- Verification run: ver "Completed" (gates re-ejecutados por el validador;
  inspección de JUnit XML en `*/build/test-results/` y
  `*/build/outputs/androidTest-results/connected/debug/`; metadatos de imagen
  con System.Drawing; `run-as`/`force-stop` en Tab S9+).
- Evidence captured: informe de validación (veredicto `accept`) registrado en
  las notas/evidencia de `feature_list.json` y en esta sesión; 4 findings Low
  no bloqueantes anotados.
- Files or artifacts updated: `feature_list.json` (status `accepted` +
  evidencia de validación), `PROGRESS.md` (Current Verified State + esta
  sesión), `docs/specs/captura-foto.md` (Validator Checklist). Cero cambios en
  código de producto.
- Known risk or unresolved issue:
  - 4 findings Low no bloqueantes:
    (1) revisión visual de los PNG/JPG de `docs/evidence/captura-foto-*`
    pendiente de mano humana (patrón del repo; precedentes sesiones 009/011 —
    la verificación "foto derecha" queda apoyada por dimensiones intercambiadas
    + 0 EXIF + `uiautomator`, pero el contenido visual no lo ha visto nadie);
    (2) deriva del estado final de `files/fotos` en la Tab S9+: la evidencia
    declara que tras "Repetir foto" quedaban `captura_41d02c1c` (153345 bytes) +
    la huérfana `captura_79d34873`, pero hoy figuran `captura_79d34873` +
    `captura_dd72dd37` (320403 bytes, 19:24) — hubo capturas posteriores al
    registrar la evidencia (probablemente para `captura-foto-preview-tab.png`);
    los artefactos extraídos son coherentes; recomendación: congelar el estado
    del dispositivo tras capturar evidencia y citar el `ls` final;
    (3) `EscanearViewModel.resultadoDeCaptura` envuelve el guardado en
    `runCatching`, que también captura `CancellationException` y lo convertiría
    en estado `Error` (teórico: durante `Procesando` la UI no puede cambiar la
    entrada; recomendable re-lanzar `CancellationException`);
    (4) nits de estilo AGENTS.md: `EscanearState.onPermisoDenegado()` es setter
    público (la regla pide setters privados) y el VM inyecta `PhotoFileStore`
    directamente para el borrado de "Repetir foto" (un caso de uso
    `DescartarFoto` encajaría mejor con "casos de uso, uno por acción"; el spec
    lo sanciona como "vía PhotoFileStore.delete").
  - Nota teórica reiterada (sesión 011): `PhotoFileStoreImpl.delete` acepta
    rutas absolutas (pre-existente, intencional y testeado); `save` SIEMPRE
    devuelve relativo bajo `filesDir/fotos`.
  - realme GT 2 sigue sin conectar; no bloquea: el spec admite "realme GT 2
    y/o Tab S9+" y la verificación física se cerró en la Tab.
  - Todo el árbol de la feature sigue sin commit (decisión de este flujo).
- Next best step: commit de los cambios de `captura-foto` (si el usuario lo
  pide) y siguiente feature con dependencias aceptadas: `ocr-mlkit` (por orden
  de `feature_list.json`) o `periodo-selector` (desbloqueada por
  `persistencia-room`).
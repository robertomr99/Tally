# Progress Log

## Current Verified State

- Repository root: `E:\AI-Expert\Tally`
- Standard startup path: `bash init.sh`
- Standard verification path: `gradlew.bat assembleDebug` + `gradlew.bat testDebugUnitTest`
  (+ `gradlew.bat :test:unit:test` para los tests puros de `:test:unit`)
- Emulador de referencia: `emulator-5554` (sdk_gphone64_x86_64; adb en
  `C:/Users/Roberto/AppData/Local/Android/Sdk/platform-tools`). Tab S9+ y
  realme GT 2 se reservan para `captura-foto` y `spike-gemma-mediapipe`.
- Current next ready feature: `captura-recorte-encuadre` (spec creada en la
  sesión 022: `docs/specs/captura-recorte-encuadre.md`, T1-T7; deps `captura-foto`
  accepted). El usuario pidió arreglar el encuadre ANTES de seguir. Después,
  `revision-datos` (deps aceptadas) y `descarga-modelo`/`extraccion-local`.
  Desviación registrada 2026-10-16: 4 fotos reales en vez de 5. Pendiente
  operativo nuevo: reintentar la cancelación FÍSICA de `procesado-ui` (verificación
  2) cuando se restablezca la cuota free tier de Gemini (agotada 2026-10-17).
- Current blocker: ninguno bloqueante. `procesado-ui` en `accepted` con la
  cancelación física pendiente por cuota Gemini agotada (reintentar al
  restablecerse). Pendientes operativos menores: revisión visual humana de
  capturas (`docs/evidence/captura-foto-*`, `ocr-mlkit-panel-debug-tab.png`,
  `docs/evidence/procesado-ui/*.png`); al cerrar las pruebas de nube, revocar los
  debug tokens de App Check (`983ba4d7-…` de los tests y `b1b0ab76-…` de la app
  debug) en Firebase Console y borrar `app/appcheck-debug-token.txt` (finding Low
  sesión 019); 2 findings Low de la validación de `extraccion-nube` (envoltorio de
  errores de `GeminiCloudEngine`) como mantenimiento no bloqueante.
- Last verified at: 2026-10-17 (sesión 022: modelo de nube cambiado a
  `gemini-flash-lite-latest` (lite) — `ExtraccionNubeRealTest` **5/5** en
  `emulator-5554` con los 4 totales correctos y en 9,2 s (~9× más rápido) —
  enmienda registrada en docs/spec/`feature_list.json`; hallazgo de UX del
  encuadre documentado (el recuadro no recorta; propuesta `captura-recorte-encuadre`);
  ver sesión 022)
- Previous verified at: 2026-10-17 (sesión 021: `procesado-ui` **accepted** por
  feature-validator — veredicto `accept`; gates re-ejecutados en verde (JVM VM
  16/16 + dominio; `:app` E2E 8/8 sin red); verificación física real en Tab S9+
  hasta resultado; cancelación física pendiente por cuota; App Check debug cableado
  en la variante debug de `:app`; solo findings Low/Info)
- Earlier verified at: 2026-10-17 (sesión 019: `extraccion-nube` **accepted**
  por feature-validator — veredicto `accept`; gates re-ejecutados por el validador
  en verde, `:app:connectedDebugAndroidTest` 11/11 con llamadas reales a Gemini,
  degradación sin `google-services.json` verificada; 2 findings Low no
  bloqueantes; ver sesión 019)
- Earlier verified at: 2026-10-17 (sesión 018, cierre: `extraccion-nube`
  verificación 1 REAL en `emulator-5554` — `ExtraccionNubeRealTest` tests=5
  failures=0 skipped=0 con App Check válido y modelo `gemini-3.8-flash`;
  contraste humano de los 4 totales vs papel 4/4 (35,30 / 3,50 / 13,90 / 7,91);
  expectativas del test corregidas por contraste (32,09→35,30; ticket 3
  12,64→13,90); extracciones reales exportadas; regresión E2E 11/11
  (`CapturaFlujoTest` 3/3 + `NavigationTest` 3/3 + real 5/5) + gates JVM en
  verde; ver sesión 018)


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

### Session 015

- Date: 2026-10-16
- Goal: Continuar y cerrar la implementación de `ocr-mlkit` (etapa 1 del
  ADR 0002) desde el árbol parcial sin commit: verificar lo hecho, implementar
  el harness de fixtures reales, ejecutar todos los gates, actualizar docs
  durables y registrar evidencia; sin inventar fixtures ni resultados.
- Completed:
  - Validación del árbol parcial frente a `Expected File Changes` del spec: todo
    estaba presente (dominio `OcrEngine`/`PhotoFileStore.read`/
    `ReconocerTextoCrudo` + test JVM, `:framework:ai` `MlKitOcrEngine` + `di.kt`
    + ML Kit, `PhotoFileStoreImpl.read` + tests, dobles `FakeOcrEngine`/
    `FakePhotoFileStore.read`, feature `Revision(rutaFoto, textoCrudo)` +
    panel debug, `CapturaFlujoTest` ampliado, catálogo con
    `mlkitTextRecognition`, `MlKitOcrEngineTest`). No se rehizo trabajo.
  - T6b **harness de fixtures implementado**: 5 métodos en `MlKitOcrEngineTest`
    que leen `ticket_real_1..5.jpg` + `ticket_real_1..5.tokens.txt` de
    `androidTest/assets` y se SALTAN con `assumeTrue` y motivo explícito si
    faltan. **No había (ni hay) 5 tickets de papel** en la sesión → los 5 tests
    aparecen `skipped`; no se inventaron fixtures ni tokens. T6b/T9 quedan
    bloqueados por aportación humana.
  - Bug corregido en el árbol parcial (bloqueaba 4 tests): `FakePhotoFileStore`
    no devolvía por `read` lo escrito por `save`, así que `ReconocerTextoCrudo`
    veía `null` y nunca invocaba el motor → `EscanearViewModelTest` fallaba en
    los 5 casos de OCR. Arreglado con round-trip (`savedByName`) sin tocar
    producto. También: `ReconocerTextoCrudoTest.engineErrorPropagatesToTheCaller`
    devolvía `String` de `runBlocking` y JUnit exige `void` (`: Unit`), y se
    restauraron los acentos perdidos (U+FFFD) en la KDoc de
    `PhotoFileStore.delete` (regresión de encoding del árbol parcial).
  - Gates JVM en verde: `assembleDebug` BUILD SUCCESSFUL; `testDebugUnitTest`
    BUILD SUCCESSFUL; `:test:unit:test --rerun` (SmokeTest 1/1);
    `:domain:expenses:test --rerun` (GuardarFotoCapturadaTest 4 +
    TicketRepositoryTest 6 + **ReconocerTextoCrudoTest 5/5**);
    `:framework:database:testDebugUnitTest` (PhotoPipelineMathTest 6/6);
    **EscanearViewModelTest 11/11**.
  - Gates instrumentados (emulador `emulator-5554` arrancado en esta sesión):
    `:framework:ai:connectedDebugAndroidTest` filtrado a `MlKitOcrEngineTest`
    → 8 tests, **0 fallos, 5 skipped** (render con tokens exactos + sin texto +
    bytes indecodificables en verde); `:framework:database:connectedDebugAndroidTest`
    → **21/21** (PhotoFileStoreTest 12/12 con los 4 de `read`); `:app:connectedDebugAndroidTest`
    → **6/6, 0 skipped** (`CapturaFlujoTest` 3/3 con el panel `captura_texto_crudo`
    presente y `captura_continuar` `assertIsNotEnabled` + `NavigationTest` 3/3).
  - Gates en la **Tab S9+ (SM-X810)**: `MlKitOcrEngineTest` filtrado → 8 tests
    0 fallos 5 skipped; `:framework:database:connectedDebugAndroidTest` → 21/21.
  - T9 parcial en la Tab S9+ (física, despierta): `:app:installDebug` +
    `pm grant` + captura en vivo por el flujo completo (obturador → pantalla 05)
    con el panel de debug visible y **"(OCR vacío)"** (la escena no era un
    ticket): evidencia `docs/evidence/ocr-mlkit-panel-debug-tab.png` +
    `ocr-mlkit-uiautomator-tab.xml`. Escenario 3 verificado en hardware. Los 5
    tickets reales + contraste humano siguen pendientes (bloqueo humano).
  - Búsquedas de regresión: 0 imports de Android/ML Kit/AndroidX en `domain/**`;
    `schemas/` sin diff; `init.sh` sin diff; `Continuar` `enabled = false`
    intacto (RevisionFotoContent.kt:205); 0 versiones literales fuera de
    `gradle/libs.versions.toml` (única coincidencia: comentario en
    `app/build.gradle.kts`); ML Kit solo vía `libs.mlkit.text.recognition`.
  - Docs durables actualizadas: `docs/architecture.md` (sección "OCR (texto
    crudo)"), `docs/domain-model.md` (el Borrador lleva el texto crudo),
    `docs/technical-discovery.md` (aclaración columna `texto_crudo_ocr` vs
    fichero sidecar), `docs/risks-and-open-questions.md` (research task ML Kit
    = integración verificada + 5 tickets pendientes; R3 anotada como abierta),
    `docs/specs/ocr-mlkit.md` (T1–T5/T7/T8/T10 `[x]`; T6a `[x]`, T6b/T9
    pendientes con Implementation Notes/Desviaciones).
- Verification run: ver "Completed" (JUnit XML en
  `domain/expenses/build/test-results/test/`,
  `feature/escanear/build/test-results/testDebugUnitTest/`,
  `framework/ai/build/outputs/androidTest-results/connected/debug/`,
  `framework/database/build/outputs/androidTest-results/connected/debug/` y
  `app/build/outputs/androidTest-results/connected/debug/`).
- Evidence captured: `feature_list.json` (`ocr-mlkit` con 11 entradas de
  `evidence`, sin inventar nada); `docs/evidence/ocr-mlkit-panel-debug-tab.png`
  (1663169 bytes) y `docs/evidence/ocr-mlkit-uiautomator-tab.xml` (11825 bytes).
- Files or artifacts updated: `framework/ai/src/androidTest/.../MlKitOcrEngineTest.kt`
  (harness de fixtures), `test/unit/src/main/kotlin/.../FakePhotoFileStore.kt`
  (round-trip save→read), `domain/expenses/src/test/.../ReconocerTextoCrudoTest.kt`
  (`: Unit`), `domain/expenses/src/main/.../PhotoFileStore.kt` (acentos
  restaurados), `docs/architecture.md`, `docs/domain-model.md`,
  `docs/technical-discovery.md`, `docs/risks-and-open-questions.md`,
  `docs/specs/ocr-mlkit.md`, `feature_list.json`, `PROGRESS.md`. Cero cambios
  de producto (el árbol ya traía la implementación).
- Known risk or unresolved issue:
  - **`ocr-mlkit` queda en `in_progress` (NO `passing`)**: la implementación y
    todos los gates ejecutables están en verde, pero la verificación 1 del spec
    (5 fotos reales de tickets con contraste humano "coincide razonablemente con
    el papel") NO se ha ejecutado. Sin esos datos, marcar `passing` sería falso.
  - Paso manual pendiente (bloqueante): aportar 5 tickets de papel → guardarlos
    como `framework/ai/src/androidTest/assets/ticket_real_1..5.jpg` con su tabla
    `ticket_real_1..5.tokens.txt` (un token de comercio/total por línea) →
    re-ejecutar el gate del OCR en emulador y Tab S9+; y capturar en vivo por el
    flujo completo para registrar los 5 textos crudos + veredicto humano (T9).
  - Gate `:framework:ai:connectedDebugAndroidTest` sin filtro en emulador falla
    por `GemmaMediaPipeSpikeTest` (exige el `.litertlm` de 3,4 GB, solo Tab);
    se ejecuta filtrado a `MlKitOcrEngineTest`. Es un test de otra feature
    (`spike-gemma-mediapipe`, `accepted`), no un fallo de `ocr-mlkit`.
  - El mismo fichero JUnit XML por módulo se regenera en cada `connected...`
    (Gradle limpia el directorio): los counts del run del emulador se citan con
    su timestamp en `feature_list.json`.
  - Revisión visual de `ocr-mlkit-panel-debug-tab.png` pendiente de mano humana
    (el agente no inspecciona imágenes); el texto del panel sí se verificó con
    `uiautomator` ("Texto crudo (debug)" + "(OCR vacío)").
  - Todo el árbol de la feature sigue sin commit (decisión de este flujo).
- Next best step: aportar los 5 tickets reales (harness + tokens) y ejecutar
  T6b/T9 para poder marcar la verificación 1; después, validación independiente
  con feature-validator. Mientras, `periodo-selector` es la siguiente feature
  con dependencias aceptadas.

### Session 016

- Date: 2026-10-16
- Goal: cerrar la parte pendiente de `ocr-mlkit` (T6b) con fixtures reales:
  el usuario aportó 4 fotos de tickets y delegó la generación de los tokens al
  agente; añadir los assets, ajustar el harness, re-ejecutar gates y registrar
  la desviación (4 fotos en vez de 5) sin inventar datos.
- Completed:
  - El usuario aportó 4 fotos reales (`ticket_real_1..4.jpeg` en el escritorio).
    Copiadas a `framework/ai/src/androidTest/assets/ticket_real_1..4.jpg`
    (355703/364343/362735/421481 bytes) con su tabla de tokens
    `ticket_real_1..4.tokens.txt` (un token "razonable" por línea: comercio/total).
  - Tokens derivados de la salida real de ML Kit (logcat tag `MlKitOcr`,
    `texto_crudo_real_<n>`) y **corroborados con un OCR independiente** del de la
    app (Windows.Media.Ocr vía WinRT) para que el test no sea tautológico:
    `HOLA`/`ALMIRANTE`/`PROFORMA`/`32,09` · `AEROPUERTO`/`TOTAL`/`SEVILLA`/`3,36`
    · `OZONE`/`TACOS`/`POLLO`/`12,64` · `GRUPO`/`LECHE`/`PISTACHO`/`7,91`.
  - **Desviación acordada** (decisión del usuario 2026-10-16): el spec y la
    verificación planificaban 5 fotos reales; se aceptan **4**. El harness pasó
    de 5 a 4 métodos para no dejar un `skipped` permanente.
  - T6 instrumentado con fixtures en verde: `MlKitOcrEngineTest` **7 tests,
    0 fallos, 0 skipped** en `emulator-5554` (JUnit XML
    `TEST-Medium_Phone_API_36.1(AVD) - 16-_framework_ai-.xml`) y en la Tab S9+
    (`TEST-SM-X810 - 16-_framework_ai-.xml`), cada `realTicket<1..4>` con sus
    tokens reales.
  - Textos crudos reales exportados a `docs/evidence/ocr-mlkit/texto_crudo_1..4.txt`
    (UTF-8 con `€` y acentos; 592/610/422/885 bytes) como insumo declarado de
    `extraccion-nube`/`extraccion-local`.
  - Bug del harness corregido: el `id` de `ocr-mlkit` en `feature_list.json`
    había quedado corrupto como `"  "`; restaurado a `"ocr-mlkit"` (JSON validado
    con `ConvertFrom-Json`, 23 entradas).
- Verification run: `ANDROID_SERIAL=emulator-5554` y `ANDROID_SERIAL=<SM-X810>`
  `gradlew.bat :framework:ai:connectedDebugAndroidTest
  -Pandroid.testInstrumentationRunnerArguments.class=<...MlKitOcrEngineTest>` →
  BUILD SUCCESSFUL, "Starting/Finished 7 tests", 0 skipped. (Los gates JVM,
  database y E2E ya quedaron en verde en la sesión 015; no se reejecutaron porque
  no hubo cambios de producto.)
- Evidence captured: `feature_list.json` (evidencia T6b/T6 con dispositivos y
  conteos); `docs/evidence/ocr-mlkit/texto_crudo_1..4.txt`; fixtures y tokens
  en `framework/ai/src/androidTest/assets/`.
- Files or artifacts updated: fixtures + tokens
  (`framework/ai/src/androidTest/assets/ticket_real_1..4.jpg` / `.tokens.txt`),
  `framework/ai/src/androidTest/.../MlKitOcrEngineTest.kt` (harness 4 fixtures +
  KDoc), `docs/specs/ocr-mlkit.md` (T6 `[x]`, T9 pendiente de contraste humano,
  notas/evidencia 5→4), `feature_list.json` (id restaurado, verification 5→4,
  evidencia T6b + nota de desviación), `PROGRESS.md` (esta sesión + estado).
- Contraste humano (T9) **CONFIRMADO por el usuario** (2026-10-16): los 4 textos
  crudos contienen comercio, fecha, productos y totales y "coinciden
  razonablemente con el papel". Observación registrada como deuda de la etapa 2
  (`extraccion-nube`/`extraccion-local`): el precio relevante es el **total con
  IVA**, no la base; el texto crudo trae base/cuota/total por separado
  (documentado en `docs/domain-model.md` y `docs/risks-and-open-questions.md`).
- `ocr-mlkit` → **`passing`** en `feature_list.json` (T6 y T9 cerrados).
- Validación independiente con `feature-validator` (subagente `validator`):
  veredicto inicial **`revise`** por **2 findings Medium** + **2 Low**:
  1. (Medium) La evidencia de `captura-foto` en `feature_list.json` quedó
     corrupta en una edición previa: se borró la entrada del "escenario 6" y se
     truncó el prefijo de la de "seguridad y regresión" (`2026-10-15 revisión` →
     `202   `). **Reparado**: restauradas ambas líneas desde `HEAD` (evidencia
     `captura-foto` de vuelta a 15 entradas; `git diff feature_list.json` ya solo
     toca `ocr-mlkit`).
  2. (Medium) La research task "ML Kit Text Recognition v2" de
     `docs/risks-and-open-questions.md` seguía diciendo "5 tickets reales
     pendiente de aportación humana", contradiciendo R3. **Reparado** (4
     fixtures + contraste humano confirmado + desviación 5→4).
  3. (Low) Tamaños de `texto_crudo_1..4.txt` declarados mal. **Reparado**:
     570/601/391/871 bytes.
  4. (Low) Restos de "5 fotos/tickets" en el spec y mensaje de skip del harness.
     **Reparado** (spec alineado a 4; mensaje del harness actualizado).
  Gates re-ejecutados por el validador en verde (JVM 33; `MlKitOcrEngineTest`
  7/7 en emulador y Tab; `database` 21/21; `:app` 6/6) y sanity del OCR 7/7 tras
  las reparaciones. Sin cambios de producto.
- Persistida la aceptación: `feature_list.json` `ocr-mlkit` → **`accepted`** con
  nota; Validator Checklist del spec marcado.
- Known risk or unresolved issue:
  - Verificación 1 con desviación: 4 fotos reales en vez de 5 (registrada en
    `feature_list.json` verification[0] y en el spec).
  - Revisión visual de `ocr-mlkit-panel-debug-tab.png` pendiente de mano humana.
  - Todo el árbol de la feature sigue sin commit (decisión de este flujo).
- Next best step: commit de los cambios de `ocr-mlkit` (si el usuario lo pide) y
  siguiente feature con dependencias aceptadas (`periodo-selector` por orden de
  `feature_list.json`; `extraccion-nube`/`extraccion-local` quedan desbloqueadas
  al aceptarse `ocr-mlkit`).

### Session 017

- Date: 2026-10-16
- Goal: Planificar `extraccion-nube` (siguiente feature por orden de
  `feature_list.json` con `depends_on` aceptados) con el subagente `planner`
  (skill feature-spec): spec de implementación lista para feature-implementer.
- Completed:
  - Selección de `extraccion-nube` (deps: `ocr-mlkit` = accepted) por la regla de
    AGENTS.md (primera de `feature_list.json` con dependencias listas). Corrección:
    la sesión 016 mencionaba `periodo-selector` como "siguiente por orden de
    `feature_list.json`", inconsistente con el orden real del fichero
    (`extraccion-nube` aparece antes); corregido en "Current next ready feature".
  - Spec creada: `docs/specs/extraccion-nube.md` (patrón de `ocr-mlkit`/
    `captura-foto`) con tareas T1-T10, Expected File Changes, sección de
    prerrequisito humano, Implementation Notes y Validator Checklist.
  - Decisiones técnicas de la spec: `TextEngine`, `TicketExtraido`,
    `TicketExtraidoParser`/`InvalidExtractionException` y caso de uso
    `ExtraerTicket` en `:domain:expenses` (parser/validador compartido para
    `extraccion-local`; `CancellationException` re-lanzada); `GeminiCloudEngine`
    `internal` en `framework/ai/extraccion/` vía Firebase AI Logic (`firebase-ai`
    + BoM, `gemini-2.5-flash`, `responseSchema`, prompt con la regla R3 "total
    con IVA", test trampa: ticket 2 -> 3.50); plugin `google-services` condicional
    (build verde sin credenciales); test real en `:app` androidTest con Hilt y
    `assumeTrue` sin configuración (patrón `MlKitOcrEngineTest`); única
    dependencia nueva en dominio `kotlinx-serialization-json`.
  - Desviación registrada: `verification[0]` de `extraccion-nube` ajustada de 5 a
    4 textos crudos reales (misma desviación acordada 2026-10-16 que `ocr-mlkit`).
  - Prerrequisito humano U1 documentado en la spec (bloquea T7 y la verificación
    1): proyecto Firebase, registrar `com.robertomr.tally`, habilitar Gemini y
    aportar `app/google-services.json` (fuera de VCS por defecto, U2). Preguntas
    abiertas U2-U5 con DEFAULTs en la spec (modelo `gemini-2.5-flash`, contraste
    humano del total del ticket 3 (OZONE 12,64 vs 13,90), backend `googleAI`).
  - Nota operativa: el subagente `planner` solo tenía permisos de escritura bajo
    `docs/specs/*`; la actualización de `feature_list.json` y `PROGRESS.md` la
    aplicó el orquestador con el contenido preparado por el planner.
- Verification run: planificación (sin gates de producto; cero cambios de código).
- Evidence captured: `docs/specs/extraccion-nube.md` (spec completa).
- Files or artifacts updated: `docs/specs/extraccion-nube.md` (nuevo),
  `feature_list.json` (`extraccion-nube`: verification[0] 5->4 + nota de
  planificación), `PROGRESS.md` (esta sesión + "Current next ready feature").
- Known risk or unresolved issue:
  - T7 y la verificación 1 quedan bloqueadas hasta que el usuario aporte el
    `google-services.json` de un proyecto Firebase real (U1); T1-T6 son
    ejecutables sin ello (plugin condicional + test real con `assumeTrue`).
  - La verificación 3 (sin claves en APK/BuildConfig) exige revisión al
    implementar; `google-services.json` fuera de VCS por defecto (U2).
- Next best step: `feature-implementer` sobre `docs/specs/extraccion-nube.md`
  (T1-T6 sin bloqueo) y, en paralelo, aportación humana del proyecto Firebase
  para T7.

### Session 018

- Date: 2026-10-17
- Goal: Continuar e implementar la feature planificada `extraccion-nube`
  (`docs/specs/extraccion-nube.md`, T1-T10), retomando el trabajo T1-T6 a medias
  de la sesión anterior, ejecutar la extracción real en nube (T7/verificación 1)
  y dejar el repo listo para `feature-validator`.
- Completed:
  - Prerrequisito humano U1 YA cumplido por el usuario: `app/google-services.json`
    existe (gitignoreado; `git check-ignore` → `.gitignore:18`). La sesión anterior
    había dejado T1-T6 implementados sin cerrar (dominio
    `TicketExtraido`/`TicketExtraidoParser`/`InvalidExtractionException`/
    `TextEngine`/`ExtraerTicket` + `FakeTextEngine`; `GeminiCloudEngine`/
    `TicketExtractionPrompt`/binding en `AiModule`; `ExtraccionNubeRealTest` +
    assets `texto_crudo_1..4.txt` + `TextEngineEntryPoint`; build con plugin
    `google-services` condicional); todo verificado aquí.
  - T7 ejecución REAL en nube (sin skips): `ExtraccionNubeRealTest` corrió con
    config sobre los 4 textos crudos reales en la Tab S9+ (SM-X810, ADB
    inalámbrico, WiFi + ping a `generativelanguage.googleapis.com` 16-33 ms) y
    en `emulator-5554` (`Medium_Phone_API_36.1`, imagen CON GMS verificada con
    `pm list packages`). Resultado real: `theInjectedEngineIsTheCloudOne` pasa
    (`motor == NUBE`) y los 4 tickets fallan con
    `com.google.firebase.ai.type.ServerException: Firebase App Check token is
    invalid` — Firebase AI Logic exige **App Check enforced**. NO se han inventado
    extracciones: no hay `extraccion_1..4.json` hasta que la ejecución pase.
  - Dos defectos del harness de App Check encontrados y corregidos (sin tocar
    alcance de producto): (1) `firebase-appcheck-debug` 20.0.0 debe ir también
    en `debugImplementation` de `:app` (su `DebugAppCheckProviderFactory.create()`
    resuelve el provider vía `FirebaseApp.get(DebugAppCheckProvider.class)` y el
    descubrimiento de componentes solo lee el paquete de la app; con la librería
    solo en androidTest devolvía `null` → "No AppCheckProvider installed"); (2)
    AGP desinstala la app al terminar cada `connectedDebugAndroidTest` (probado
    con `pm list packages` antes/después) y el token generado por instalación no
    sobrevive (3 ejecuciones → 3 tokens distintos), así que el test fija el debug
    token vía `testInstrumentationRunnerArguments` desde
    `app/appcheck-debug-token.txt` (local, gitignoreado — mismo patrón que
    `google-services.json`); verificado en logcat que el mismo UUID se usa en
    ejecuciones consecutivas y en ambos dispositivos.
  - T8 verificación 3 (seguridad): informe citable en
    `docs/evidence/extraccion-nube/verificacion-3-seguridad.md` —
    `AIza|api[_-]?key|buildConfigField|GEMINI_API` = 0 en fuentes/recursos,
    0 `buildConfigField`, único material de config = recursos estándar del plugin
    `google-services` desde `google-services.json` fuera de VCS, token de App
    Check fuera de VCS y sin viajar en el APK, 0 imports de Android/Firebase en
    `domain/**`.
  - T9 regresión: gates JVM en verde (`assembleDebug` + `testDebugUnitTest` =
    equivalencia de `init.sh` en esta máquina sin `bash`; `git diff init.sh`
    vacío) y suite instrumentada en `emulator-5554` con `CapturaFlujoTest` 3/3 +
    `NavigationTest` 3/3 (sin cambios de UI; "Continuar" `enabled=false` intacto);
    0 versiones literales fuera del toml; `schemas/` de Room sin cambios.
  - T10 cierre: Implementation Notes de la spec con las desviaciones
    (EntryPointAccessors/`AndroidJUnitRunner` en vez de `HiltTestRunner`
    (R-NUBE6), App Check debug provider + `debugImplementation` + token fijo,
    versiones fijadas 1.11.0/4.5.0/35.0.2), docs durables actualizados y
    evidencia registrada.
  - **CIERRE de la sesión 018 (2026-10-17, continuación)**: T7/T9 cerradas y
    verificación 1 CUMPLIDA con el test real **5/5 failures=0 skipped=0** en
    `emulator-5554` (App Check válido tras el registro manual del token por el
    usuario; modelo final `gemini-3.8-flash` — U3 resuelta: el servidor deprecó
    `gemini-2.5-flash` para proyectos nuevos, evidencia en
    `junit-app-emulador-5554-run-modelo-deprecado.xml`).
  - CONTRASTE HUMANO CON EL PAPEL (decisión del usuario 2026-10-17, R3/U4 — el
    papel manda sobre el test): ticket 1 (HOLA ALMIRANTE) = **35,30** (la
    expectativa 32,09 del test estaba MAL derivada del OCR: la foto trae dos
    bloques y el texto crudo ambos importes); ticket 2 (SANTOMÁS) = **3,50**
    con IVA (caso trampa: nunca la base 3,36); ticket 3 (OZONE) = **13,90**
    (50,00 − 36,10 de cambio; el "Total (impuestos incl.) 12,64" del OCR era
    engañoso — U4 resuelta); ticket 4 (GRUPO DIA) = **7,91**. Modelo vs papel:
    4/4 correctos (tolerancia 0,01).
  - Corrección de expectativas en `ExtraccionNubeRealTest.kt` (evidencia del
    papel > test): `ticket1ExtractionIsValidWithTotal3209` →
    `ticket1ExtractionIsValidWithTotal3530` (32,09→35,30) y
    `ticket3ExtractionIsLoggedOnlyBecauseOfTheOcrAmbiguity` →
    `ticket3ExtractionIsValidWithTotal1390` (de "solo registrado" a assert
    13,90 con tolerancia 0,01); KDoc actualizado con la decisión humana y su
    fecha. El modelo devolvió 35,30 y 13,90 en el run final SIN endurecer el
    prompt (no hizo falta; `TicketExtractionPromptTest` sigue 6/6).
  - Exportación de evidencia real: `extraccion_1..4.txt` (contenido EXACTO del
    logcat tag `ExtraccionNube`, toString de `TicketExtraido` → `.txt`, sin
    reformular) + JUnit XML frescos + tabla de contraste humano de los 4
    tickets en `docs/evidence/extraccion-nube/verificacion-1-estado.md`
    (estado "completada").
- Verification run (Windows: `gradlew.bat`, sin `bash`):
  - `.\gradlew.bat assembleDebug testDebugUnitTest` → BUILD SUCCESSFUL.
  - `.\gradlew.bat :test:unit:test --rerun :domain:expenses:test --rerun
    :framework:ai:testDebugUnitTest --rerun` → BUILD SUCCESSFUL; JUnit XML:
    SmokeTest 1/1, TicketRepositoryTest 6/6, GuardarFotoCapturadaTest 4/4,
    ReconocerTextoCrudoTest 5/5, TicketExtraidoParserTest 18/18 (verificación 2),
    ExtraerTicketTest 4/4, TicketExtractionPromptTest 6/6.
  - `$env:ANDROID_SERIAL="adb-R52W60AFAPZ-izULbK._adb-tls-connect._tcp";
    .\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.robertomr.tally.extraccion.ExtraccionNubeRealTest`
    → ExtraccionNubeRealTest tests=5 failures=4 skipped=0 (pasa engine==NUBE;
    los 4 tickets: App Check token invalid).
  - `$env:ANDROID_SERIAL="emulator-5554"; .\gradlew.bat :app:connectedDebugAndroidTest`
    → CapturaFlujoTest 3/3 + NavigationTest 3/3 + ExtraccionNubeRealTest
    tests=5 failures=4 skipped=0 (mismo error App Check).
  - **Cierre 2026-10-17** — `$env:ANDROID_SERIAL="emulator-5554";
    .\gradlew.bat :app:connectedDebugAndroidTest
    "-Pandroid.testInstrumentationRunnerArguments.class=com.robertomr.tally.extraccion.ExtraccionNubeRealTest"`
    → BUILD SUCCESSFUL; JUnit XML: `ExtraccionNubeRealTest` **tests=5
    failures=0 errors=0 skipped=0** (los 4 tickets extraídos con los totales del
    contraste humano: 35,30 / 3,50 / 13,90 / 7,91).
  - **Cierre 2026-10-17** — `$env:ANDROID_SERIAL="emulator-5554";
    .\gradlew.bat :app:connectedDebugAndroidTest` (suite completa) →
    **tests=11 failures=0 errors=0 skipped=0** = CapturaFlujoTest 3/3 +
    NavigationTest 3/3 + ExtraccionNubeRealTest 5/5 (regresión E2E + real).
  - **Cierre 2026-10-17** — `.\gradlew.bat assembleDebug testDebugUnitTest` →
    BUILD SUCCESSFUL; `.\gradlew.bat :domain:expenses:test --rerun` →
    TicketExtraidoParserTest 18/18 + ExtraerTicketTest 4/4 (+ TicketRepositoryTest
    6/6, GuardarFotoCapturadaTest 4/4, ReconocerTextoCrudoTest 5/5);
    `.\gradlew.bat :framework:ai:testDebugUnitTest --rerun` →
    TicketExtractionPromptTest 6/6; `git diff init.sh` → vacío.
- Evidence captured: `docs/evidence/extraccion-nube/junit-app-tab-sm-x810.xml`,
  `docs/evidence/extraccion-nube/junit-app-emulador-5554.xml` (JUnit XML crudo),
  `docs/evidence/extraccion-nube/verificacion-1-estado.md` (comandos, error
  exacto, logcat del debug provider y paso manual) y
  `docs/evidence/extraccion-nube/verificacion-3-seguridad.md`. **Cierre
  2026-10-17**: `docs/evidence/extraccion-nube/extraccion_1..4.txt`
  (extracciones REALES, contenido exacto del logcat tag `ExtraccionNube`),
  `junit-app-emulador-5554-run-final.xml` (real 5/5),
  `junit-app-emulador-5554-suite-completa.xml` (11/11 con E2E),
  `junit-app-emulador-5554-run-modelo-deprecado.xml` (histórico U3: modelo
  `gemini-2.5-flash` deprecado por el servidor) y `verificacion-1-estado.md`
  reescrito a estado "completada" con la **tabla de contraste humano** de los 4
  tickets (comercio / total extraído / total real del papel / veredicto).
- Files or artifacts updated: `app/build.gradle.kts` (runner arguments del token
  fijo + `debugImplementation` de `firebase-appcheck-debug` con su BoM),
  `gradle/libs.versions.toml` (comentario App Check actualizado),
  `app/src/androidTest/.../ExtraccionNubeRealTest.kt` (instalación del provider
  con token fijo), `.gitignore` (+ `app/appcheck-debug-token.txt`),
  `docs/specs/extraccion-nube.md` (T1-T8 marcadas según estado real + notas),
  `docs/architecture.md` (sección "Extracción estructurada (TextEngine)"),
  `docs/technical-discovery.md` (Integrations), `docs/risks-and-open-questions.md`
  (research task Firebase con resultados reales + R3), `feature_list.json`
  (evidence + status `blocked` + notes) y este `PROGRESS.md`. **Cierre
  2026-10-17**: `app/src/androidTest/.../ExtraccionNubeRealTest.kt`
  (expectativas corregidas 32,09→35,30 y 12,64→13,90 + KDoc con la decisión
  humana y fecha), `docs/specs/extraccion-nube.md` (T7/T9 `[x]`, U3/U4
  resueltas, Implementation Notes con las correcciones de expectativa y el
  modelo `gemini-3.8-flash`), `feature_list.json` (status `passing` + evidencia
  de cierre), `docs/evidence/extraccion-nube/` (extracciones, JUnit XML frescos,
  `verificacion-1-estado.md` "completada") y este `PROGRESS.md`.
- Known risk or unresolved issue:
  - **BLOQUEO OPERATIVO RESUELTO 2026-10-17 (cierre)**: el usuario registró el
    debug token de App Check (el de `app/appcheck-debug-token.txt`) en
    **Firebase Console > proyecto `tally-586d1` > Security > App Check > Apps >
    Manage debug tokens** y el test real quedó 5/5 sin skips (verificación 1
    cumplida). Ya no hay pasos manuales pendientes para `extraccion-nube`.
  - R2 (cuota/latencia Gemini): parcialmente observada en los runs reales
    (~5-36 s por extracción con `gemini-3.8-flash` en WiFi doméstica; ver
    tiempos por testcase en los JUnit XML) pero sin medición formal — sigue
    como tarea propia de la feature de medición (S3).
  - U4 RESUELTA (contraste humano 2026-10-17): el total del ticket 3 (OZONE) es
    13,90 (papel); el "12,64" del OCR era engañoso. Expectativa corregida.
  - Sin provider de App Check de producción (Play Integrity): pendiente futuro.
- Next best step: validación independiente de `extraccion-nube` con
  `feature-validator` (status `passing`, evidencia en `feature_list.json` y
  `docs/evidence/extraccion-nube/`); tras `accept`, siguiente feature
  `procesado-ui`.

### Session 019

- Date: 2026-10-17
- Goal: Validación independiente de `extraccion-nube` con el subagente
  `validator` (feature-validator) y persistencia de la aceptación.
- Completed:
  - Validación independiente con feature-validator (subagente `validator`):
    veredicto **`accept`** sin hallazgos Critical/High/Medium; 2 findings Low
    no bloqueantes (detalle en "Known risk").
  - Gates re-ejecutados por el validador en verde (2026-10-17, sobre el árbol
    de trabajo, sin fiarse del resumen del implementer):
    `.\gradlew.bat assembleDebug` (BUILD SUCCESSFUL, 240 tasks),
    `.\gradlew.bat testDebugUnitTest` (BUILD SUCCESSFUL),
    `.\gradlew.bat :test:unit:test --rerun :domain:expenses:test --rerun
    :framework:ai:testDebugUnitTest --rerun` (JUnit XML fresco: SmokeTest 1/1,
    TicketExtraidoParserTest 18/18, ExtraerTicketTest 4/4,
    TicketRepositoryTest 6/6, GuardarFotoCapturadaTest 4/4,
    ReconocerTextoCrudoTest 5/5, TicketExtractionPromptTest 6/6) y
    `$env:ANDROID_SERIAL="emulator-5554"; .\gradlew.bat
    :app:connectedDebugAndroidTest` (suite completa, **con llamadas reales a
    Gemini**): tests=11 failures=0 skipped=0 en
    `Medium_Phone_API_36.1(AVD) - 16` — `ExtraccionNubeRealTest` 5/5 +
    `CapturaFlujoTest` 3/3 + `NavigationTest` 3/3.
  - Degradación sin credenciales (T1) verificada de verdad por el validador:
    renombró `app/google-services.json` → `assembleDebug` BUILD SUCCESSFUL sin
    el fichero (12 tasks re-ejecutadas) → fichero restaurado.
  - Evidencia contrastada con artefactos: la **ejecución real propia** volvió a
    dar 5/5 (el modelo devolvió de nuevo 35.3 / 3.5 / 13.9 / 7.91 → las
    expectativas corregidas no están "ajustadas a una tirada afortunada");
    `extraccion_1..4.txt` íntegros (UTF-8 verificado por bytes) y coherentes
    1:1 con la tabla de contraste humano; assets
    `app/src/androidTest/assets/texto_crudo_1..4.txt` **byte-idénticos**
    (SHA-256) a `docs/evidence/ocr-mlkit/texto_crudo_1..4.txt`; JUnit XML
    citados 1:1 con los ficheros reales, incluidos los runs fallidos
    históricos (App Check inválido, modelo deprecado — mensaje del servidor
    *"use models/gemini-3.8-flash"*) coherentes con `verificacion-1-estado.md`;
    los importes corregidos (35,30 y 13,90 = 50,00−36,10) aparecen
    literalmente en los textos crudos → expectativas ancladas en datos reales +
    decisión humana del usuario (2026-10-17).
  - Escenarios 1-7 cumplidos; verificación 3 sin claves (búsquedas
    `AIza|api[_-]?key|buildConfigField|GEMINI_API` = 0 en código;
    `google-services.json` y `appcheck-debug-token.txt` gitignoreados y fuera
    de `git status`; 0 `buildConfigField`); arquitectura correcta (`TextEngine`
    en `domain/expenses/data/` sin Android/Firebase, parser compartido en
    dominio, `GeminiCloudEngine`/`TicketExtractionPrompt` `internal`,
    `AiModule` único, `TicketExtraido.motor = Motor.NUBE`; `TextEngineEntryPoint`
    en `:app` evaluado como compliant-in-spirit con "un `di.kt` por módulo": no
    es `@Module` y debe procesarse en compilación principal — desviación R-NUBE6
    bien justificada); 0 versiones literales en `**/*.gradle.kts`; `schemas/`
    de Room sin diff; "Continuar" `enabled=false` intacto; `init.sh` sin diffs.
  - Persistida la aceptación: `feature_list.json` status de `extraccion-nube`
    → `accepted` (nota con fecha, alcance y findings Low); Validator Checklist
    de `docs/specs/extraccion-nube.md` completado.
- Verification run: ver "Completed" (gates re-ejecutados por el validador;
  inspección de JUnit XML en `docs/evidence/extraccion-nube/` y
  `app/build/outputs/androidTest-results/connected/debug/`; SHA-256 de assets;
  búsquedas de seguridad; prueba de degradación sin credenciales).
- Evidence captured: informe de validación (veredicto `accept`) registrado en
  las notas de `feature_list.json` y en esta sesión; 2 findings Low
  no bloqueantes anotados.
- Files or artifacts updated: `feature_list.json` (status `accepted` + nota),
  `PROGRESS.md` (Current Verified State + esta sesión),
  `docs/specs/extraccion-nube.md` (Validator Checklist). Cero cambios en
  código de producto.
- Known risk or unresolved issue:
  - 2 findings Low no bloqueantes:
    (1) higiene del debug token de App Check sin paso de revocación
    documentado: al cerrar las pruebas de nube hay que revocar el token
    (`983ba4d7-…`) en Firebase Console > proyecto `tally-586d1` > Security >
    App Check > Manage debug tokens y borrar `app/appcheck-debug-token.txt`
    (`firebase-appcheck-debug` viaja en la variante debug; superficie de abuso
    de cuota Spark si el token filtra);
    (2) envoltorio de errores demasiado amplio en `GeminiCloudEngine`
    (`catch (Exception)` en el lazy y `catch (IllegalStateException)` en
    `extract` → "Firebase AI no configurado" enmascara fallos no-config;
    acotar a las excepciones de config reales; afectará al diagnóstico de
    `log-extracciones`/`reintento-otro-motor`).
  - R2 (cuota/latencia Gemini): parcialmente observada (~5-36 s por extracción
    con `gemini-3.8-flash`) pero sin medición formal — tarea S3.
  - Sin App Check de producción (Play Integrity): pendiente futuro.
  - Todo el árbol de la feature queda pendiente de commit hasta este cierre
    (commit + push a petición del usuario 2026-10-17).
- Next best step: commit y push del árbol completo de `extraccion-nube`; después
  siguiente feature por orden de `feature_list.json` con dependencias
  aceptadas: `procesado-ui` (deps: `extraccion-nube` accepted; también
  desbloqueadas `descarga-modelo` y `extraccion-local`).

### Session 020

- Date: 2026-10-17
- Goal: Planificar `procesado-ui` (siguiente feature por orden de
  `feature_list.json` con `depends_on` aceptados) con el subagente `planner`
  (skill feature-spec): spec de implementación lista para feature-implementer.
- Completed:
  - Selección de `procesado-ui` (deps: `extraccion-nube` = accepted, sesión 019)
    por la regla de AGENTS.md. También quedan desbloqueadas `descarga-modelo` y
    `extraccion-local` por orden posterior.
  - Spec creada: `docs/specs/procesado-ui.md` (patrón de `captura-foto`/
    `extraccion-nube`) con tareas T1-T10, Expected File Changes, Implementation
    Notes vacías y Validator Checklist.
  - Decisiones técnicas de la spec: procesado como subflujo interno de
    `:feature:escanear` (sin rutas nuevas ni módulos Gradle, patrón pantalla 05);
    flujo pantalla 05 → "Continuar" (habilitado por fin, RevisionFotoContent.kt:205)
    → procesado (mockup 06) → resultado de solo lectura con "Revisar ticket"
    deshabilitado hasta `revision-datos`; pasos Encuadre→OCR→Categorización con
    estados REALES (Encuadre/OCR ya completados porque save+OCR ocurren en la
    captura — moverlo rompería ocr-mlkit); progreso circular indeterminado SIN %
    inventado (CONTEXT.md); Badge de motor real ("Motor Nube · Gemini") con
    `ExtraerTicket.motor` como passthrough; cancelar = cancelación estructurada
    (`flatMapLatest`/`Job`) con `CancellationException` siempre re-lanzada
    (hallazgo Low recurrente), conserva foto + Borrador y vuelve a pantalla 05,
    sin escrituras en Room (sin datos huérfanos); error visible sin inventar datos
    ni fallback silencioso (ADR 0001) con "Reintentar" mismo motor; toggle del
    mockup NO implementado (= `reintento-otro-motor`).
  - E2E: `ProcesadoFlujoTest` + actualización de `CapturaFlujoTest` (Continuar
    habilitado); justificado NO usar `@TestInstallIn` de `TextEngine` (rompería
    `ExtraccionNubeRealTest` vía `TextEngineEntryPoint`): la determinización de
    pasos/cancelación es JVM con `FakeTextEngine.gate` (ya existente) y la
    extracción real por UI se verifica en físico con ticket de papel.
  - Preguntas abiertas U1-U5 con DEFAULTs en la spec (U1: dispositivo = Tab S9+
    `SM-X810` en lugar del realme GT 2 citado en verification[0], no conectado a
    esta máquina, precedente sesiones 012-013/018; U3: no crear
    `stateAsResultIn`/`AcScaffold` — no existen, mismo gap que documentó
    captura-foto; U4: botón de resultado deshabilitado hasta `revision-datos`).
  - Nota operativa: el subagente planner no pudo ejecutar la herramienta de
    escritura (despacho atascado en `grep`); la spec se entregó en el informe y el
    orquestador la persistió en `docs/specs/procesado-ui.md` (ver Files).
- Verification run: planificación (sin gates de producto; cero cambios de código).
- Evidence captured: `docs/specs/procesado-ui.md` (spec completa).
- Files or artifacts updated: `docs/specs/procesado-ui.md` (nuevo),
  `feature_list.json` (`procesado-ui`: nota de planificación), `PROGRESS.md`
  (esta sesión + "Current next ready feature").
- Known risk or unresolved issue:
  - U1 sin confirmar: la verificación 1 cita el realme GT 2 y se propone la Tab S9+
    como DEFAULT (revisar con el usuario).
  - R2 (latencia Gemini 5-36 s) se hace visible al usuario en esta pantalla:
    progreso indeterminado puede parecer colgado; mitigación = copy honesto +
    Cancelar disponible; medición formal sigue en S3.
  - El E2E en emulador (cámara virtual, OCR blank) no ejercita extracción real por
    UI: la verificación 1 real exige dispositivo físico con ticket de papel.
- Next best step: `feature-implementer` sobre `docs/specs/procesado-ui.md`
  (T1-T8 ejecutables sin más; T9 con la Tab S9+ conectada y confirmación de U1).

### Session 021

- Date: 2026-10-17
- Goal: Cerrar la implementación de `procesado-ui` (trabajo previo sin commit en
  el árbol): verificar contra el spec, ejecutar todos los gates (JVM, E2E emulador
  y verificación física real en la Tab S9+), actualizar docs durables/evidencia y
  dejar la feature lista para `feature-validator`.
- Completed:
  - Revisión del árbol frente al `Expected File Changes` del spec: todo estaba
    presente (dominio `ExtraerTicket.motor`; VM con `OrdenExtraccion`
    Continuar/Reintentar/Cancelar/Volver + `flatMapLatest` +
    `runCatchingLanzandoCancelacion`; `ProcesadoContent.kt`; cableado en
    `EscanearScreen`; `RevisionFotoContent` con "Continuar" habilitado;
    `ProcesadoFlujoTest`; `CapturaFlujoTest` actualizado). El VM NO toca
    `TicketRepository` (verificado por grep y por test).
  - **Bug corregido (bloqueaba el E2E)**: el `testTag("procesado_badge_motor")`
    estaba en el `Row` del badge y `assertTextContains` no lee el texto de los
    hijos sin merge → `ProcesadoFlujoTest` fallaba en la línea 62. Movido al
    `Text` de la etiqueta del motor (patrón de `procesado_completitud`). E2E
    enfocado pasó 2/2 después.
  - Verificación JVM en verde: `assembleDebug testDebugUnitTest` BUILD
    SUCCESSFUL; `:test:unit:test` 1/1; `:domain:expenses:test` (ExtraerTicketTest
    5/5, TicketExtraidoParserTest 18/18, TicketRepositoryTest 6/6,
    GuardarFotoCapturadaTest 4/4, ReconocerTextoCrudoTest 5/5);
    `:feature:escanear:testDebugUnitTest` **EscanearViewModelTest 16/16**
    (continuar→resultado con completitud, cancelar sin borrar la foto ni tocar el
    repositorio, error sin inventar y reintento con el mismo motor, texto blank →
    resultado rápido sin invocar el motor, badge = motor real).
  - E2E emulador `emulator-5554`: focused `ProcesadoFlujoTest` 2/2; suite `:app`
    `connectedDebugAndroidTest` **13/13** (0 skipped, 0 failed) = CapturaFlujoTest
    3/3 + ProcesadoFlujoTest 2/2 + NavigationTest 3/3 + ExtraccionNubeRealTest 5/5
    (Gemini real: 35,3 / 3,5 / 13,9 / 7,91).
  - **DESVIACIÓN APROBADA POR EL USUARIO (2026-10-17)**: la app normal no
    instalaba ningún `AppCheckProviderFactory` (solo `ExtraccionNubeRealTest`), así
    que la extracción real por UI no podía completar ("Firebase App Check token is
    invalid"). Se añadió `app/src/debug/java/.../appcheck/AppCheckDebugInitializer.kt`
    (un `ContentProvider`) + `app/src/debug/AndroidManifest.xml` que instalan el
    `DebugAppCheckProviderFactory` **solo en debug**; sin tocar `src/main`, sin
    dependencias nuevas, sin secretos en el APK. Tras el cambio se re-ejecutaron
    los 8 tests E2E no-red en verde (la cuota Gemini ya estaba agotada para el
    test real).
  - **Verificación física en la Tab S9+ (SM-X810)**: instalación debug + `pm grant`
    CAMERA + captura de un ticket real (OCR real: "HOLA ALMIRANTE… Sevilla…").
    verification 1 CUMPLIDA: extracción REAL a resultado (badge "Motor Nube ·
    Gemini", comercio HOLA ALMIRANTE, fecha 3/10/2026 15:32, total (con IVA)
    32,09 €, categoría Alimentación, "4/5 campos extraídos", "Revisar ticket"
    deshabilitado). verification 3 CUMPLIDA (badge/motor real y estado de error
    honesto por `uiautomator`, con el motivo real del servidor). verification 2
    (cancelación en vuelo física) **BLOQUEADA por cuota**: se agotó el free tier de
    Gemini (`gemini-3.8-flash`, 20 req/día) por las pruebas reales acumuladas y la
    petición falla al instante (sin ventana de cancelación); cubierta por JVM.
  - Docs durables: `docs/architecture.md` (sección nueva "Procesado de la
    extracción (pantalla 06)" + App Check debug en la variante debug de `:app` +
    "Continuar" habilitado en OCR), `docs/risks-and-open-questions.md` (R2: latencia
    visible + cuota agotada), `docs/specs/procesado-ui.md` (T1-T8/T10 `[x]`, T9
    parcial, Implementation Notes/Desviaciones).
- Verification run (Windows/gradlew.bat, sin `bash`; `git diff init.sh` vacío):
  - `.\gradlew.bat assembleDebug testDebugUnitTest` → BUILD SUCCESSFUL.
  - `.\gradlew.bat :test:unit:test --rerun :domain:expenses:test --rerun :feature:escanear:testDebugUnitTest --rerun` → BUILD SUCCESSFUL (conteos arriba).
  - `$env:ANDROID_SERIAL="emulator-5554"; .\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.robertomr.tally.captura.ProcesadoFlujoTest"` → 2/2.
  - `$env:ANDROID_SERIAL="emulator-5554"; .\gradlew.bat :app:connectedDebugAndroidTest` → 13/13 (0 skipped) con Gemini real.
  - `$env:ANDROID_SERIAL="emulator-5554"; .\gradlew.bat :app:connectedDebugAndroidTest "-P...class=CapturaFlujoTest,ProcesadoFlujoTest,NavigationTest"` → 8/8 (tras el cambio de App Check).
  - Físico Tab S9+ (`adb-R52W60AFAPZ-…`): `:app:installDebug`, `pm grant`, captura real, `Continuar`, resultado real; `uiautomator dump` + `screencap`; `run-as com.robertomr.tally ls -l files/fotos` (baseline). Logcat `DebugAppCheckProvider` con el token generado (`b1b0ab76-…`, registrado a mano por el usuario).
- Evidence captured: `docs/evidence/procesado-ui/` con
  `procesado-fisico-05.png`, `procesado-fisico-06-procesando.png`,
  `procesado-fisico-07-resultado.png`, `procesado-fisico-08-error-cuota.png`,
  `fisico-pantalla05-uia.xml`, `fisico-resultado-uia.xml`,
  `fisico-resultado-error-uia.xml`, `fisico-error-cuota-uia.xml`,
  `run-as-fotos-antes.txt`, `run-as-fotos-cancel-antes.txt`; JUnit XML citados en
  `feature_list.json`; evidencia registrada en `feature_list.json` (status
  `passing`).
- Files or artifacts updated: `feature/escanear/.../ProcesadoContent.kt` (testTag
  del badge movido al `Text`), `app/src/debug/java/.../appcheck/AppCheckDebugInitializer.kt`
  (nuevo), `app/src/debug/AndroidManifest.xml` (nuevo), `docs/architecture.md`,
  `docs/risks-and-open-questions.md`, `docs/specs/procesado-ui.md`,
  `feature_list.json`, `docs/evidence/procesado-ui/`, `PROGRESS.md`.
- Known risk or unresolved issue:
  - `procesado-ui` → **`accepted`**: feature-validator devolvió veredicto
    `accept` (re-ejecutó los gates en verde y no halló Critical/High/Medium). La
    aceptación la persistió el orquestador en `feature_list.json`.
  - Cancelación física (verification 2) pendiente por cuota Gemini; reintentar al
    restablecerse (cubierta por JVM mientras).
  - Degradación de calidad de extracción: en la captura física real el modelo
    devolvió 32,09 € para un ticket cuyo contraste humano previo (sesión 018) era
    35,30: es un problema de precisión del modelo sobre un OCR ruidoso (dos bloques
    de importes), no del cableado; anotado para `log-extracciones`/prompt.
  - App Check debug ahora viaja en la variante debug de `:app` (token en allow-list
    `b1b0ab76-…`): revocar al cerrar las pruebas de nube; en release no hay provider
    (Play Integrity, pendiente futuro, ya conocido).
  - Revisión visual humana de los PNG de `docs/evidence/procesado-ui/` pendiente
    (el agente no inspecciona imágenes).
- Next best step: planificar con `feature-spec` la siguiente feature por orden de
  `feature_list.json` con dependencias aceptadas (`descarga-modelo` o
  `extraccion-local`). Mantenimiento pendiente (no bloqueante): reintentar la
  cancelación física de `procesado-ui` al restablecerse la cuota de Gemini y
  anexar el `run-as` antes/después; revocar los debug tokens de App Check
  (`983ba4d7-…`, `b1b0ab76-…`) al cerrar las pruebas de nube.

### Session 022

- Date: 2026-10-17
- Goal: Mantenimiento pedido por el usuario — (1) dejar la nube como *fallback*
  (ADR 0001) y (2) cambiar el modelo de nube a una variante **lite** para aliviar
  el problema de cuota; además, diagnosticar la queja del encuadre de captura.
- Completed:
  - **Cambio de modelo de nube (lite)**: `TicketExtractionPrompt.MODELO` de
    `gemini-3.8-flash` a **`gemini-flash-lite-latest`** (el flash agotaba el free
    tier: 20 req/día y ~5 por corrida de suite). Actualizado el KDoc y
    `TicketExtractionPromptTest.modelConstantIsRegistered`.
  - **Verificación real del lite**: `:framework:ai:testDebugUnitTest --rerun` en
    verde (TicketExtractionPromptTest 6/6); `ExtraccionNubeRealTest` en
    `emulator-5554` → **tests=5 failures=0 errors=0 skipped=0** con los 4 totales
    correctos (35,30 / 3,50 / 13,90 / 7,91) y en **9,2 s totales** (~9× más
    rápido que el flash, que tardó 82 s solo en el ticket 1). Evidencia:
    `docs/evidence/extraccion-nube/junit-app-emulador-5554-lite.xml`.
  - Docs/enmienda: `docs/architecture.md`, `docs/technical-discovery.md`,
    `docs/risks-and-open-questions.md` (R2 + research task), y la spec
    `docs/specs/extraccion-nube.md` (Implementation Notes) actualizadas al modelo
    lite; `feature_list.json` (`extraccion-nube`: evidence + notes de enmienda).
  - **Hallazgo de UX de captura (reportado por el usuario)**: el recuadro del
    mockup 04 es **solo un hint visual**; `CameraEncuadre.capturar()` llama a
    `ImageCapture.takePicture` **sin recorte ni `ViewPort`** y el pipeline
    reescala a ≤1280 el fotograma completo, así que el ticket pierde detalle y
    "alinear en el recuadro" no tiene efecto. Confirmado que el **crop estaba
    como Non-Goal explícito** de `captura-foto` (spec, línea 36): no es un bug,
    es una mejora nueva. Por petición del usuario se prioriza **antes de la
    siguiente feature**: se añadió `captura-recorte-encuadre` a `feature_list.json`
    (índice 8, primera lista por orden; deps `captura-foto` accepted) y se planificó
    con `feature-spec` → `docs/specs/captura-recorte-encuadre.md` (T1-T7; DEFAULT
    `ViewPort` de CameraX, Plan B crop en pipeline). NO implementada en esta sesión.
- Verification run: `:framework:ai:testDebugUnitTest --rerun` → BUILD SUCCESSFUL;
  `ANDROID_SERIAL=emulator-5554 gradlew.bat :app:connectedDebugAndroidTest
  "-P...class=...ExtraccionNubeRealTest"` → 5/5, 0 fallos.
- Evidence captured: `docs/evidence/extraccion-nube/junit-app-emulador-5554-lite.xml`;
  enmiendas en los docs citados y en `feature_list.json`.
- Files or artifacts updated: `framework/ai/.../extraccion/TicketExtractionPrompt.kt`,
  `framework/ai/.../extraccion/TicketExtractionPromptTest.kt`,
  `docs/architecture.md`, `docs/technical-discovery.md`,
  `docs/risks-and-open-questions.md`, `docs/specs/extraccion-nube.md`,
  `feature_list.json`, `PROGRESS.md`.
- Known risk or unresolved issue:
  - La cuota real del lite sigue sin medir (el techo diario existe; R2 abierta),
    pero deja de agotarse en ~4 corridas.
  - `procesado-ui` sigue `accepted`; la cancelación física (verificación 2) queda
    pendiente por cuota y ahora es más viable con el lite.
  - UX del encuadre: pendiente de planificar `captura-recorte-encuadre` para que el
    recuadro recorte de verdad y no se degraden los tickets.
- Next best step: implementar `captura-recorte-encuadre` (spec T1-T7; DEFAULT
  `ViewPort`) con `feature-implementer`, verificarla (E2E + físico Tab S9+) y
  pasarla a `feature-validator`. Después, `extraccion-local`/validar lo acumulado.

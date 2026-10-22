# Progress Log

## Current Verified State

- Repository root: `E:\AI-Expert\Tally`
- Standard startup path: `bash init.sh`
- Standard verification path: `gradlew.bat assembleDebug` + `gradlew.bat testDebugUnitTest`
  (+ `gradlew.bat :test:unit:test` para los tests puros de `:test:unit`)
- Emulador de referencia: `emulator-5554` (sdk_gphone64_x86_64; adb en
  `C:/Users/Roberto/AppData/Local/Android/Sdk/platform-tools`). Tab S9+ y
  realme GT 2 se reservan para `captura-foto` y `spike-gemma-mediapipe`.
- Current next ready feature: `gasto-manual-2` en `passing` (sesión 072) a la espera de validator; `seed-demo` queda para el final.
- Deuda abierta fuera de features: los E2E existentes dejan tickets vacíos en el emulador; `EscanearViewModelTest` intermitente; revisión editable aún con aspecto de formulario.
  `edicion-lineas` quedó **`accepted`** en la sesión 028 (validator independiente).
  `revision-datos` quedó **`accepted`** en la sesión 026. `captura-recorte-encuadre` quedó `accepted`
  en la sesión 023. Desviación registrada 2026-10-16: 4 fotos reales en vez de 5.
  Pendiente operativo: reintentar la cancelación FÍSICA de `procesado-ui`
  (verificación 2) cuando se restablezca la cuota free tier de Gemini (agotada
  2026-10-17).
- Current blocker: ninguno bloqueante. `procesado-ui` en `accepted` con la
  cancelación física pendiente por cuota Gemini agotada (reintentar al
  restablecerse). Pendientes operativos menores: revisión visual humana de
  capturas (`docs/evidence/captura-foto-*`, `ocr-mlkit-panel-debug-tab.png`,
  `docs/evidence/procesado-ui/*.png`); al cerrar las pruebas de nube, revocar los
  debug tokens de App Check (`983ba4d7-…` de los tests y `b1b0ab76-…` de la app
  debug) en Firebase Console y borrar `app/appcheck-debug-token.txt` (finding Low
  sesión 019); 2 findings Low de la validación de `extraccion-nube` (envoltorio de
  errores de `GeminiCloudEngine`) como mantenimiento no bloqueante.
- Last verified at: 2026-10-18 (sesión 028: `edicion-lineas` **passing** — JVM en
  verde, `:app` E2E 16/16 y `:framework:database` 22/22 en emulator-5554,
  evidencia visual 01-05; ver sesión 028)
- Prior verified at: 2026-10-17 (sesión 026: `revision-datos` **accepted** por
  feature-validator (ronda 2, veredicto `accept`) tras validación 1 `revise` por
  **M1** — repair aplicado (reintento de Guardar ya persiste: `Guardar` con nº de
  intento) + L1/L2/L3; `EscanearViewModelTest` 23/23; E2E `:app` 9/9; evidencia
  visual del mockup 07; el emulador se redimensionó a 12G; ver sesión 026)
- Previous verified at: 2026-10-17 (sesión 023: `captura-recorte-encuadre`
  **accepted** por feature-validator — veredicto `accept`; implementada con
  **Enfoque B** (recorte al recuadro en el pipeline); JVM (EncuadreGeometriaTest
  3/3, PhotoPipelineMathTest 10/10, GuardarFotoCapturadaTest 4/4, EscanearViewModelTest
  16/16) + instrumentados (`CapturedPhotoProcessorTest` recorta 2000x1000→1000x500;
  :app E2E 8/8) en verde; T6 físico HECHO en la Tab S9+ (foto 819x1280/0,64 vs
  960x1280/0,75; 0 EXIF; extracción nube correcta 35,30 €); 5 findings Low no
  bloqueantes; ver sesión 023)
- Earlier verified at: 2026-10-17 (sesión 022: modelo de nube cambiado a
  `gemini-flash-lite-latest` (lite) — `ExtraccionNubeRealTest` **5/5** en
  `emulator-5554` con los 4 totales correctos y en 9,2 s (~9× más rápido);
  hallazgo de UX del encuadre documentado)
- Earlier verified at: 2026-10-17 (sesión 021: `procesado-ui` **accepted** por
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

### Session 023

- Date: 2026-10-17
- Goal: Implementar `captura-recorte-encuadre` (spec T0-T7) con el flujo
  `planner` → `implementer`. El planner refino la spec (gate de API T0) y el
  implementer se interrumpio a mitad; la sesion la continuo el orquestador.
- Completed:
  - Gate de API (T0) → **Enfoque B** (recorte en el pipeline), no `ViewPort`: el
    recuadro es un sub-rect arbitrario y el `ViewPort` comparte region con la
    `Preview` (riesgo de recortar/zoomear el mockup 04 a pantalla completa).
    Determinista, testeable en JVM, sin cambiar el diseño visual.
  - Codigo: `EncuadreGeometria` (nueva, pura, `:feature:escanear`) que pinta la
    reticula y calcula el rect normalizado; `CropRegion` (nueva,
    `:domain:expenses/data`) + contrato `PhotoProcessor.process(rawJpeg, crop)`;
    `PhotoPipelineMath.recorteEnImagen` (mapeo `FILL_CENTER`→imagen);
    `CapturedPhotoProcessor` recorta antes de escalar;
    `CameraEncuadre`/`EscanearViewModel`/`GuardarFotoCapturada`/`FakePhotoProcessor`
    propagan el `crop`. Preview intacta (pantalla completa).
  - Bug del implementer corregido: `PhotoPipelineMathTest.centeredRectKeepsRatioWhenAspectsMatch`
    esperaba `right=0.5`; con aspectos coincidentes es identidad (`0.75`). El
    codigo era correcto.
  - Gates en verde: `assembleDebug testDebugUnitTest`; `EncuadreGeometriaTest`
    3/3; `PhotoPipelineMathTest` 10/10; `GuardarFotoCapturadaTest` 4/4;
    `EscanearViewModelTest` 16/16; `:framework:database:connectedDebugAndroidTest`
    22/22 (incluido `CapturaFlujoTest`... no: incluye
    `CapturedPhotoProcessorTest.processCropsTheGuidedRegionInsteadOfTheWholeFrame`,
    2000x1000 recuadro 50% → **1000x500**); E2E `:app` 8/8 en `emulator-5554`
    (`CapturaFlujoTest` 3 + `ProcesadoFlujoTest` 2 + `NavigationTest` 3). `git diff
    init.sh` vacio; `schemas/` sin diff.
  - Docs: `docs/specs/captura-recorte-encuadre.md` (T0-T7 `[x]`, T6 fisico HECHO,
    Implementation Notes con el gate + Enfoque B + finding Low),
    `docs/architecture.md` (seccion "Captura de foto": el recuadro recorta +
    `EncuadreGeometria`/`recorteEnImagen`), `feature_list.json` (`passing` +
    evidence), evidencia en `docs/evidence/captura-recorte-encuadre/junit/`.
- Verification run: ver "Completed".
- Evidence captured: JUnit XML en `docs/evidence/captura-recorte-encuadre/junit/`;
  evidencia en `feature_list.json`.
- Files or artifacts updated: `feature/escanear/.../EncuadreGeometria.kt` (nuevo)
  + test, `CameraEncuadre.kt`, `EscanearViewModel.kt`, `EscanearViewModelTest.kt`,
  `domain/expenses/.../data/PhotoProcessor.kt` (+`CropRegion`),
  `usecases/GuardarFotoCapturada.kt` + test, `framework/database/.../photo/PhotoPipelineMath.kt`
  + test, `source/CapturedPhotoProcessor.kt` + androidTest, `test/unit/.../FakePhotoProcessor.kt`,
  `docs/specs/captura-recorte-encuadre.md`, `docs/architecture.md`,
  `feature_list.json`, `PROGRESS.md`.
- Known risk or unresolved issue:
  - **T6 (fisico) HECHO 2026-10-17** (Tab S9+ `SM-X810`): captura real con ticket;
    foto recortada **819x1280 (aspecto 0,64)** vs **960x1280 (0,75)** del fotograma
    completo previo => el aspecto 0,64 = aspecto del recuadro (~640x1000 px en la
    Tab), recorta de verdad; 0 EXIF; preview a pantalla completa sin zoom
    (Scenario 5); el usuario confirmo visualmente. Bonus: con la foto recortada la
    extraccion nube devolvio el total CORRECTO 35,30 € (con el fotograma completo
    daba 32,09 €). Evidencia en `docs/evidence/captura-recorte-encuadre/`. Nota: el
    realme GT 2 (`2312DRAABG`) ahora SI esta conectado (no se uso; T6 se hizo en la Tab).
  - `captura-recorte-encuadre` → **`accepted`**: feature-validator devolvio
    veredicto `accept` (re-ejecuto los gates en verde y corrobora la evidencia
    fisica 819x1280 al pixel con la matematica; confirmo que el codigo de
    `recorteEnImagen` era correcto y la expectativa corregida era la erronea). 5
    findings Low no bloqueantes (notas obsoletas de T6 corregidas; revision visual
    humana de PNG pendiente; nits de orientacion/encoding de la evidencia;
    supuesto aspecto preview==ImageCapture valido en el dispositivo probado).
  - Finding Low (no bloqueante): con el recorte en pipeline la decodificacion
    puede ser a resolucion alta en sensores grandes (memoria transitoria); acotar
    `inSampleSize` si molesta.
- Next best step: planificar `revision-datos` (deps aceptadas) o `extraccion-local`;
  y commit/push del arbol si el usuario lo pide.

### Session 024

- Date: 2026-10-17
- Goal: Commit + push de `captura-recorte-encuadre` (hecho) y planificar la
  siguiente feature por orden de `feature_list.json` con dependencias aceptadas
  (`revision-datos`) con el subagente `planner` (skill feature-spec).
- Completed:
  - Commit + push: `9602a03 feat: captura-recorte-encuadre - recorte de la foto al
    recuadro guiado (accepted)` (subido a `origin/master`).
  - Selección de `revision-datos` (index 9; deps `procesado-ui` y
    `persistencia-room` = accepted).
  - Spec creada: `docs/specs/revision-datos.md` (~410 líneas; 6 escenarios,
    Repository Research con ficheros reales, T-series, U1-U9 con DEFAULT).
  - Decisiones de diseño (DEFAULT, no bloqueantes): navegación tras Guardar SIN
    rutas nuevas (subflujo interno de `:feature:escanear` + callback
    `EscanearScreen(onGastoGuardado)` que `:app` usa para ir a la pestaña Gastos,
    placeholder hasta `listado-gastos`); mapeo `TicketExtraido`→`Ticket`/`Linea` en
    un caso de uso de dominio nuevo `GuardarTicket` (`TicketRepository.save`;
    `@Transaction` ya en `TicketDao.upsert`); alcance de "Editar" = solo cabecera
    (comercio/fecha/total/categoría con chips de la enum de 6), líneas en solo
    lectura (`edicion-lineas` posterior); "Reintentar" en la revisión = non-goal
    (ya existe mismo-motor en `procesado-ui`). "Ticket en el listado" verificable
    vía `TicketRepository.observeByDateRange` + E2E de navegación a `pantalla_gastos`
    (R-REV1). Extracción vacía → total 0.0/fecha hoy sin bloquear (R-REV2). Reset del
    subflujo a Encuadre tras Guardar (R-REV3). `CancellationException` siempre
    re-lanzada (R-REV4). `edicion-lineas` aislado como non-goal (R-REV5).
- Verification run: planificación (sin gates de producto; cero cambios de código).
- Evidence captured: `docs/specs/revision-datos.md`.
- Files or artifacts updated: `docs/specs/revision-datos.md` (nuevo),
  `feature_list.json` (nota de planificación de `revision-datos`), `PROGRESS.md`.
- Known risk or unresolved issue:
  - "Ticket en el listado" no tiene listado real todavía (`listado-gastos` es
    posterior): se verifica por repositorio + E2E de navegación a la pestaña Gastos.
  - Las decisiones U1-U9 tienen DEFAULT no bloqueante; confirmar con el usuario si
    alguna no encaja.
- Next best step: `feature-implementer` sobre `docs/specs/revision-datos.md` y,
  después, validación independiente con `feature-validator`.

### Session 025

- Date: 2026-10-17
- Goal: Implementar `revision-datos` (spec) con `feature-implementer`. El
  implementer se interrumpio a mitad; la sesion la continuo el orquestador.
- Completed:
  - Codigo: `DatosRevision` (holder de cabecera corregida) y caso de uso
    `GuardarTicket` (dominio: mapea `TicketExtraido`→`Ticket`/`Linea` y persiste
    con `TicketRepository.save`, `@Transaction` ya en `TicketDao.upsert`);
    `RevisionDatosContent.kt` (mockup 07: recibo, badge de motor, campos
    comercio/fecha/total, chips de las 6 categorias, lineas solo lectura, Guardar);
    estados `RevisionDatos`/`Guardando`/`Guardado` + eventos en `EscanearViewModel`;
    cableado (`ProcesadoContent` "Revisar ticket" habilitado con `onRevisar`,
    `EscanearScreen(onGastoGuardado)` + BackHandler, `TallyApp` navega a la pestana
    Gastos sin ruta nueva). Reseteo del subflujo a Encuadre tras Guardar (R-REV3);
    `CancellationException` re-lanzada (R-REV4).
  - Gates en verde: JVM `GuardarTicketTest 5/5` + `EscanearViewModelTest 21/21`
    (+5) + dominio (TicketRepositoryTest 6/6, etc.); instrumentado
    `:framework:database:connectedDebugAndroidTest 22/22` (TicketDaoTest 5/5);
    E2E `:app` 9/9 = CapturaFlujoTest 3 + ProcesadoFlujoTest 2 (`procesado_revisar`
    habilitado) + RevisionDatosFlujoTest 1 (resultado→Revisar→chip→Guardar→
    `pantalla_gastos`) + NavigationTest 3. `git diff init.sh` vacio; `schemas/` sin
    diff; 0 versiones literales fuera del toml. (Nota: un primer intento de E2E
    fallo por pasar `-P...class=` a `:framework:database` por error del orquestador;
    se separo y quedo en verde.)
  - Docs: `docs/specs/revision-datos.md` (T1-T7 `[x]`), `docs/architecture.md`
    (seccion nueva "Revision y guardado (pantalla 07)"), `feature_list.json`
    (`passing` + evidence), evidencia en `docs/evidence/revision-datos/junit/`.
- Verification run: ver "Completed".
- Evidence captured: JUnit XML en `docs/evidence/revision-datos/junit/`; evidencia
  en `feature_list.json`.
- Files or artifacts updated: `domain/expenses/.../entities/DatosRevision.kt` (nuevo),
  `usecases/GuardarTicket.kt` (nuevo) + test, `feature/escanear/.../RevisionDatosContent.kt`
  (nuevo), `EscanearViewModel.kt` + test, `EscanearScreen.kt`, `ProcesadoContent.kt`,
  `app/.../navigation/TallyApp.kt`, `app/src/androidTest/.../RevisionDatosFlujoTest.kt`
  (nuevo), `ProcesadoFlujoTest.kt`, `test/unit/.../FakeTicketDataSource.kt`,
  `docs/specs/revision-datos.md`, `docs/architecture.md`, `feature_list.json`,
  `PROGRESS.md`.
- Known risk or unresolved issue:
  - **T-fisico opcional PENDIENTE** (no bloqueante; el spec lo marca opcional): Tab
    S9+ con ticket real captura→Continuar→Revisar→corregir→Guardar→`run-as ls`.
  - `revision-datos` queda en `passing`; `accepted` exige validacion independiente
    (`feature-validator`) que persiste el orquestador.
  - "Ticket en el listado" no tiene listado real (`listado-gastos` es posterior):
    verificado por repositorio (JVM) + E2E de navegacion a la pestana Gastos.
- Next best step: validacion independiente de `revision-datos` con
  `feature-validator`; despues, `edicion-lineas`/`descarga-modelo`/`extraccion-local`.

### Session 026

- Date: 2026-10-17
- Goal: Validacion independiente de `revision-datos` con `feature-validator` y
  repair de los hallazgos.
- Completed:
  - Validacion 1 (`feature-validator`): veredicto **`revise`** por un defecto real
    **M1** y 5 findings Low (L1-L5). M1: reintentar "Guardar" tras un fallo era un
    no-op silencioso (la orden `OrdenGuardado.Guardar` no cambiaba y
    `MutableStateFlow` conflaciona valores iguales). El validador lo confirmo
    empiricamente con un probe temporal (eliminado).
  - **Repair**:
    - **M1**: `OrdenGuardado.Guardar` pasa a `data class Guardar(val intento: Int)`
      + contador `intentoGuardado`; `onGuardar()` reintenta aunque no se edite.
      Test nuevo `reintentarTrasFalloGuardaSinEditarNadaPersisteElTicket`.
    - **L1**: test nuevo `editarFechaYTotalActualizaLaCabeceraEIgnoraTextoInvalido`.
    - **L2**: `ProcesadoFlujoTest` renombrado a `...ConRevisarHabilitado`.
    - **L3**: evidencia visual del mockup 07 capturada en `emulator-5554` sin nube
      (OCR blank -> extraccion vacia -> revision manual): `mockup07-revision.png` +
      `-uia.xml` y `mockup07-tras-guardar-gastos.png` + `-uia.xml`.
    - L4/L5 no bloqueantes (L4 = mejora semantica de nombre; L5 se acepta).
  - Incidencia operativa: el emulador se quedo sin almacenamiento (APK debug
    180 MB; `/data` al 91%). Autorizado por el usuario, se paro el emulador, se
    subio `disk.dataPartition.size` de 6G a 12G en `config.ini` y se arranco con
    `-wipe-data` (AVD de pruebas). Quedo con ~11G libres.
  - Re-verificacion en verde: `EscanearViewModelTest` **23/23** (0 fallos);
    `assembleDebug testDebugUnitTest` BUILD SUCCESSFUL; E2E `:app` **9/9**
    (`RevisionDatosFlujoTest` 1/1 + `ProcesadoFlujoTest` 2/2 renombrado +
    `CapturaFlujoTest` 3/3 + `NavigationTest` 3/3).
  - Docs/estado: `docs/specs/revision-datos.md` (seccion "Repair y Validacion"
    + Validator Checklist `[x]`), `feature_list.json` (evidence del repair +
    notas), evidencia en `docs/evidence/revision-datos/`.
- Verification run: ver "Completed".
- Evidence captured: `docs/evidence/revision-datos/mockup07-*.png`/`-uia.xml`;
  JUnit XML actualizados (`EscanearViewModelTest` 23/23; `:app` 9/9).
- Files or artifacts updated: `feature/escanear/.../EscanearViewModel.kt` (fix M1),
  `feature/escanear/src/test/.../EscanearViewModelTest.kt` (+2 tests),
  `app/src/androidTest/.../ProcesadoFlujoTest.kt` (rename),
  `docs/specs/revision-datos.md`, `feature_list.json`, `docs/evidence/revision-datos/`,
  `PROGRESS.md`.
- Known risk or unresolved issue:
  - `revision-datos` → **`accepted`**: re-validacion (ronda 2) con
    `feature-validator` -> `accept` (M1 confirmado resuelto con probe empirico;
    gates re-ejecutados en verde).
  - T-fisico opcional pendiente (no bloqueante).
  - L4/L5 no bloqueantes (nombre de evento; back bloqueado durante Guardando).
- Next best step: planificar/implementar `edicion-lineas` (dep `revision-datos`
  accepted) o `descarga-modelo`/`extraccion-local`; y commit/push del árbol.

### Session 027

- Date: 2026-10-17
- Goal: Commit + push de `revision-datos` (hecho) y planificar la siguiente
  feature (`edicion-lineas`, dep `revision-datos` accepted) con `planner`.
- Completed:
  - Commit + push: `25017d1 feat: revision-datos - revision y guardado del ticket
    (accepted)` (a `origin/master`).
  - Seleccion de `edicion-lineas` (index 10; dep `revision-datos` = accepted).
  - Spec creada: `docs/specs/edicion-lineas.md` (~380 lineas).
  - Decisiones con DEFAULT (no bloqueantes): MISMA pantalla y MISMO estado
    (`RevisionDatos`) sin ruta nueva (la pantalla 07 evoluciona al mockup 08);
    modelo de edicion en dominio puro `LineaEditable(id, concepto, cantidad,
    precioUnitario)` + helpers puros (`totalRecalculado`, `lineasEditablesIniciales`,
    `nuevaLineaEditable`, `aLineas`); el **total del ticket NUNCA se sobrescribe**
    con el recalculado (footer solo referencia; sin boton "usar total calculado");
    persistencia: ampliar `DatosRevision` con `lineas` y `GuardarTicket` persiste
    `datos.lineas.aLineas()` (upsert `@Transaction` ya reemplaza el conjunto; sin
    tocar Room/`schemas`); UI sin decoracion ficticia del mockup 08 (contador real
    de articulos). Validacion cantidad >= 1 / precio >= 0, parseo tolerante,
    filtrado de vacias. E2E enfocado en `RevisionDatosFlujoTest`.
- Verification run: planificacion (sin gates de producto; cero cambios de codigo).
- Evidence captured: `docs/specs/edicion-lineas.md`.
- Files or artifacts updated: `docs/specs/edicion-lineas.md` (nuevo),
  `feature_list.json` (nota de planificacion), `PROGRESS.md`.
- Known risk or unresolved issue:
  - Ampliar `DatosRevision`/`GuardarTicket` desincroniza tests de `revision-datos`
    (se actualizan a proposito; sin tocar Room).
  - "Lineas persistidas" no verificable por E2E (no hay listado real hasta
    `listado-gastos`): se cubre en JVM por `TicketRepository.observeByDateRange`.
- Next best step: `feature-implementer` sobre `docs/specs/edicion-lineas.md` y,
  despues, `feature-validator`; o commit/push del arbol si el usuario lo pide.

### Session 028

- Date: 2026-10-18
- Goal: Implementar `edicion-lineas` (spec `docs/specs/edicion-lineas.md`).
- Completed:
  - Verificacion base `bash init.sh` en verde antes de tocar codigo.
  - Dominio: `LineaEditable` + helpers puros (`lineasEditablesIniciales`,
    `nuevaLineaEditable`, `totalRecalculado`, `aLineas`) en
    `entities/LineaEditable.kt`; `DatosRevision.lineas`; `GuardarTicket` persiste
    `datos.lineas.aLineas()` (total = `datos.total`, nunca la suma). Sin cambios
    de Room/`schemas`.
  - VM: `onLineaAnadida`, `onLineaConceptoEditado`, `onLineaCantidadEditada`
    (entero >= 1), `onLineaPrecioEditado` (>= 0, coma/punto), `onLineaEliminada`;
    inválidos se ignoran.
  - UI: `RevisionDatosContent` con editor de lineas (filas keyeadas por id,
    borrar en `error`), "Añadir producto o línea" (borde discontinuo), dock con
    "Total recalculado" (`tnum`, contador real) y "Descartar"/"Guardar cambios".
    Se conserva el campo "Total (con IVA)" (desviacion ya justificada en spec).
  - Tests: `LineaEditableTest` (nuevo), `GuardarTicketTest` y
    `EscanearViewModelTest` ampliados; `RevisionDatosFlujoTest` +2 metodos E2E.
  - `docs/architecture.md` actualizado (seccion revision/edicion de lineas).
- Verification run:
  - `gradlew.bat assembleDebug testDebugUnitTest :test:unit:test
    :domain:expenses:test --rerun-tasks` → BUILD SUCCESSFUL; LineaEditableTest
    7/7, GuardarTicketTest 8/8, EscanearViewModelTest 31/31, resto en verde.
  - `:app:compileDebugAndroidTestKotlin` → OK.
  - Primer intento del E2E sin emulador (apagado); tras arrancarlo el
    orquestador, con `ANDROID_SERIAL=emulator-5554` (/data 12G, 17 % usado):
    - `:app:connectedDebugAndroidTest` → **16/16, 0 skipped** (instalacion
      limpia): RevisionDatosFlujoTest 3/3 (2 nuevos), ProcesadoFlujoTest 2/2,
      CapturaFlujoTest 3/3, NavigationTest 3/3, ExtraccionNubeRealTest 5/5.
      Una pasada intermedia dio `CapturaFlujoTest.denegado` skipped por el
      `pm grant` manual de la evidencia (assumption preexistente); repetida → 0.
    - `:framework:database:connectedDebugAndroidTest --rerun` → 22/22
      (TicketDaoTest 5/5).
  - Hallazgo visual corregido: en 411dp "Guardar cambios" se partia/recortaba
    en el dock; fix `contentPadding` 12dp + `maxLines = 1`; gate JVM y suite
    `:app` re-ejecutados en verde tras el fix.
- Evidence captured: JUnit XML (JVM + instrumentados) y capturas/uiautomator
  01-05 (sin lineas → linea editada 4,50 € → dos lineas 6,50 € → eliminada
  4,50 € con Total (con IVA) intacto en 0,00 → Gastos) en
  `docs/evidence/edicion-lineas/`; busquedas de decoracion ficticia / versiones
  literales / diffs prohibidos limpias.
- Files or artifacts updated: ver `git status` (dominio, VM, UI, Screen, tests,
  E2E, `docs/architecture.md`, `feature_list.json`, `PROGRESS.md`).
- Known risk or unresolved issue:
  - La persistencia exacta de las lineas se verifica en JVM (no hay `sqlite3`
    en el emulador ni listado real hasta `listado-gastos`).
  - Los campos de linea usan `rememberSaveable` local (patron de `CampoEditable`):
    al escribir un valor invalido el campo lo muestra pero el modelo conserva el
    ultimo valido (el footer refleja el modelo).
- Status: `edicion-lineas` = **accepted**. `feature-validator` independiente
  devolvió ACCEPT: re-ejecutó el gate JVM (LineaEditableTest 7/7,
  GuardarTicketTest 8/8, EscanearViewModelTest 31/31) y el E2E enfocado
  `RevisionDatosFlujoTest` 3/3 en emulator-5554; escenarios 1-6 cumplidos.
  Hallazgos bajos no bloqueantes: L1 (texto local del campo vs último valor
  válido; posible follow-up: precio vacío = 0 o estado de error visual) y L2
  (id reutilizado tras borrar la línea de id máximo; inocuo).
- Next best step: `planner` sobre `descarga-modelo` (dep `spike-gemma-mediapipe`
  accepted); después `extraccion-local`.

### Session 029

- Date: 2026-10-18
- Goal: corregir los hallazgos L1 y L2 del validator de `edicion-lineas`.
- Completed:
  - L1: `parsearPrecio` trata el campo vacío como 0; `CampoEditable` marca el
    campo en error (`esValido`) si cantidad/precio no son válidos. Las funciones
    `parsearCantidad`/`parsearPrecio` pasan a `LineaEditable.kt` (dominio) para
    que ViewModel y pantalla compartan la regla.
  - L2: `nuevaLineaEditable(id)` + contador `proximoIdLinea` en el ViewModel
    (solo crece, se reinicia en `onRevisar`); ya no se reutilizan ids.
- Verification run: `assembleDebug testDebugUnitTest :domain:expenses:test
  :app:compileDebugAndroidTestKotlin --rerun-tasks` → BUILD SUCCESSFUL;
  LineaEditableTest 9/9, GuardarTicketTest 8/8, EscanearViewModelTest 33/33.
- Known risk or unresolved issue:
  - (RESUELTO) E2E `RevisionDatosFlujoTest` (paso nuevo: precio vacío → total baja) sin
    ejecutar: `emulator-5554` apagado (solo había un dispositivo inalámbrico, que
    no se usó). Estado de la feature sigue `accepted`.
- Next best step: arrancar `emulator-5554` y ejecutar `RevisionDatosFlujoTest`;
  después `planner` sobre `descarga-modelo`.
- Seguimiento: con `emulator-5554` arrancado, `RevisionDatosFlujoTest` 3/3
  (0 fallos, 0 skipped) incluido el paso de precio vacío. Pendiente cerrado.

### Session 030

- Date: 2026-10-18
- Goal: planificar `descarga-modelo` (dep `spike-gemma-mediapipe` accepted) con `planner`.
- Completed:
  - Spec creada: `docs/specs/descarga-modelo.md` (~230 lineas).
  - Decisiones con DEFAULT: WorkManager (red no medida, notificacion `dataSync`,
    backoff) como unica dependencia nueva; `HttpURLConnection` con `Range` sobre
    `.part` y redirecciones manuales (no se envia `Authorization` al CDN);
    token HF pegado por el usuario en `noBackupFilesDir`; modelo real
    3.655.827.456 bytes (3,4 GB; corrige el "2-3 GB" de la doc); dominio puro
    (`EstadoModelo`, `ModelDownloader`, casos de uso) con implementaciones
    `internal` en `framework/ai/modelo/`; integridad solo por tamaño.
  - D1 (modulo Gradle nuevo `:feature:ajustes`): **aprobada por el usuario**
    el 2026-10-18 (AGENTS.md exige preguntar antes de crear modulos).
- Verification run: planificacion (sin gates de producto; cero cambios de codigo).
- Evidence captured: `docs/specs/descarga-modelo.md`.
- Files or artifacts updated: `docs/specs/descarga-modelo.md` (nuevo),
  `feature_list.json` (nota de planificacion), `PROGRESS.md`.
- Known risk or unresolved issue:
  - R-DM1: la verificacion fisica en la Tab S9+ necesita un token HF con la
    licencia Gemma aceptada; sin el solo se verifica el protocolo contra un
    servidor local de pruebas.
  - R-DM2..R-DM6: ver la spec (CDN de HF, version `main`, fichero preexistente,
    limites `dataSync` en Android 14+, WiFi medido).
- Next best step: `implementer` sobre `docs/specs/descarga-modelo.md`.

### Session 031

- Date: 2026-10-18
- Goal: cerrar la implementación de `descarga-modelo` (continuación de una
  instancia anterior sin commits) y verificar en `emulator-5554`.
- Completed:
  - Código ya presente: dominio (`EstadoModelo`, `ModelDownloader`, casos de uso),
    `framework/ai/modelo` (motor `HttpURLConnection` + `Range`, worker WorkManager
    2.12.0, token en `noBackupFilesDir`), módulo `:feature:ajustes`, ruta Ajustes
    e icono en Resumen. Capturas 01-07 en `docs/evidence/descarga-modelo/`.
  - Docs duraderas: 3,4 GB en `technical-discovery.md` y `risks-and-open-questions.md`;
    `:feature:ajustes` y `framework/ai/modelo` en `architecture.md`; checkboxes de la spec.
- Verification run (todo en `emulator-5554`; adb en `.../Android/Sdk/platform-tools`):
  - `gradlew.bat assembleDebug testDebugUnitTest :test:unit:test :domain:expenses:test`
    → BUILD SUCCESSFUL.
  - `:app:connectedDebugAndroidTest` → 19/19 (0 fallos), incl. `AjustesModeloFlujoTest` 3/3.
  - `:framework:ai:connectedDebugAndroidTest` → `ModelDownloadEngineDeviceTest` 4/4 y
    `MlKitOcrEngineTest` 7/7; `GemmaMediaPipeSpikeTest` falla por diseño (no hay modelo
    en `.../files/models`, requiere `adb push`; ajeno a esta feature).
  - Estáticas: 0 versiones literales en `*.gradle.kts`, 0 Retrofit/OkHttp en código
    (solo un comentario), 0 `BuildConfig`/tokens `hf_` en código, diff vacío de
    `init.sh`, `AGENTS.md` y `build-logic/`.
- NO realizado: DESCARGA REAL de 3,4 GB. El token HF que el usuario guardó en la app
  del emulador se perdió: la primera ejecución de `connectedDebugAndroidTest` falló
  `conWifiElWorkerArrancaYSinTokenAvisaDeLaLicencia` (el test asume app sin token; con
  token el worker terminó en Error genérico "No se pudo completar la descarga", causa
  sin determinar por no haber logcat) y AGP desinstala la app al terminar, lo que
  borra `noBackupFilesDir`. Tras la reinstalación no hay token; no se inventó ninguno.
- Known risk or unresolved issue:
  - Con token válido el worker acabó en error genérico en esa corrida: hay que
    diagnosticar con `adb logcat -s ModeloDescarga` en la descarga real (puede ser
    licencia Gemma no aceptada, red del emulador o un bug del motor).
  - Los tests instrumentados desinstalan la app: ejecutar la descarga real DESPUÉS de
    ellos y no volver a lanzar `connected*` hasta terminar.
  - `AjustesModeloFlujoTest` depende de que no haya token guardado.
- Next best step: reinstalar (`gradlew installDebug`), pegar el token en Ajustes y
  ejecutar el paso 4 (descarga real, corte de WiFi, reanudación, Eliminar) con logcat;
  si sale completa, `passing`. Tab S9+ opcional.

### Session 031 (seguimiento): descarga real

- Date: 2026-10-18
- Completed:
  - Descarga real en `emulator-5554` con el token del usuario ya guardado en la app
    (no leido ni mostrado). Primer intento: `HTTP 404` por **errata de repo en
    `ModelPaths.URL_MODELO`** (`google/gemma-3n-E2B-it-litertlm` no existe; correcto
    `google/gemma-3n-E2B-it-litert-lm`, confirmado con la API publica de HF; el
    fichero `gemma-3n-E2B-it-int4.litertlm` mide 3.655.827.456 bytes). Corregido en
    `ModelPaths.kt`, `docs/specs/descarga-modelo.md` y `docs/risks-and-open-questions.md`.
  - Tras el fix: Descargando -> corte de WiFi -> "En pausa · Esperando WiFi" con el
    `.part` conservado (500.597.515 bytes) -> WiFi on -> "Reanudando desde 500597515
    bytes (Range)" -> "Descarga completa: 3655827456 bytes" -> estado Descargado.
    Fichero final de 3.655.827.456 bytes. Evidencia: capturas 08-10 en
    `docs/evidence/descarga-modelo/` (0 coincidencias `hf_` en los volcados).
- Status: `descarga-modelo` = **accepted** (validator independiente: ACCEPT, JVM re-ejecutado + fichero final de 3655827456 bytes por adb; sin hallazgos bloqueantes).
- Known risk or unresolved issue:
  - Tab S9+ (red movil real, rendimiento fisico): prueba opcional, no bloqueante.
  - `AjustesModeloFlujoTest` falla si hay un token guardado; los `connected*Test`
    desinstalan la app y borran token y modelo descargado.
  - El modelo real queda en el emulador para `extraccion-local` (no volver a lanzar
    `connected*` si se quiere conservar).
- Next best step: planificar `extraccion-local` (deps accepted); aislar `AjustesModeloFlujoTest` del token guardado.

### Session 032

- Date: 2026-10-18
- Goal: push de `descarga-modelo` (hecho: `33290c1` en `origin/master`) y planificar
  `extraccion-local` (deps `spike-gemma-mediapipe`, `ocr-mlkit`, `descarga-modelo` accepted).
- Completed:
  - Spec creada: `docs/specs/extraccion-local.md` (~190 lineas), sin decisiones bloqueantes.
  - Decisiones con DEFAULT: `GemmaLocalEngine` internal en `framework/ai/extraccion/` con
    interfaz interna `LocalLlm` (`MediaPipeLocalLlm` real) para probar el motor en JVM;
    binding `@Named("local") TextEngine` (el `TextEngine` sin calificar sigue siendo la
    nube); reutiliza `TextEngine`, `TicketExtraidoParser` y `ExtraerTicket`; prompt/schema de
    la nube + cierre "solo JSON"; detector de objeto JSON completo
    (`JsonObjectStreamDetector`) que corta la generacion (el spike degenero hasta
    `maxTokens` en 53 s); `maxTokens` 2048, `topK` 1, `temperature` 0,2; modelo cargado en
    cada extraccion y cerrado en `finally`; totales contra el papel registrados, no gate duro.
  - `feature_list.json`: nota de planificacion y "5 textos" -> "4 textos" (alineado con
    `ocr-mlkit`/`extraccion-nube`).
- Verification run: planificacion (sin gates de producto; cero cambios de codigo).
- Evidence captured: `docs/specs/extraccion-local.md`.
- Known risk or unresolved issue:
  - Latencia sin medir (9,2 tok/s en el spike -> estimacion 16-45 s por ticket); si supera
    60 s hay que revisar el plan B de R1.
  - Gate fisico obligatorio en la Tab S9+ (4 textos validos + latencia); el emulador es solo humo.
  - `connected*AndroidTest` desinstala la app y borra el modelo del emulador: ejecutarlos al
    final, tras los `am instrument`.
- Next best step: `implementer` sobre `docs/specs/extraccion-local.md`.

### Session 033: implementacion de extraccion-local

- Date: 2026-10-18
- Completed:
  - `GemmaLocalEngine` (internal, `framework/ai/extraccion`) + interfaz interna `LocalLlm`/`LlmRun`
    + `MediaPipeLocalLlm` (IO, carga por extraccion, cierre ordenado tras cancelar) +
    `JsonObjectStreamDetector` (corta la generacion al cerrar el objeto JSON o en
    `<end_of_turn>`) + `ModeloLocalNoDisponibleException` (dominio) + `buildLocalPrompt`.
    Binding `@Named("local") TextEngine` en el `di.kt` existente y accesor
    `textEngineLocal()` en `TextEngineEntryPoint`; el `TextEngine` sin calificar sigue siendo la
    nube. `maxTokens=2048`, `topK=1`, `temperature=0.2`, semilla 42. Parser compartido sin cambios.
  - Tests JVM: `GemmaLocalEngineTest` 10, `JsonObjectStreamDetectorTest` 8,
    `TicketExtractionPromptTest` 7 (+1 local). Test real `ExtraccionLocalRealTest` en `:app`.
- Verification run:
  - Base previa: `gradlew.bat assembleDebug testDebugUnitTest :test:unit:test :domain:expenses:test` verde.
  - Final: `assembleDebug testDebugUnitTest :test:unit:test --rerun :domain:expenses:test
    --rerun :framework:ai:testDebugUnitTest`: un fallo no reproducible
    (`EscanearViewModelTest.emptyOcrResultReachesRevisionAsEmptyTextWithoutBlocking`, ajeno);
    `:feature:escanear:testDebugUnitTest --rerun` verde a continuacion.
  - Humo `emulator-5554` (x86_64): con 2,5 GB de RAM el lowmemorykiller mata la app al cargar el
    modelo; con `-memory 5120` (sin tocar el AVD) `ExtraccionLocalRealTest` 5/5 con `am instrument`.
    4/4 pasan el parser con `motor=LOCAL`, `ranOnMainThread=false`; carga 252-607 ms (primera 1.676),
    generacion 10,8-26,0 s, prompt 740-1.009 tokens, respuesta 152-426; totales correctos 2/4
    (ticket 2 = 3,50 OK, ticket 4 = 7,91 OK; ticket 1 = 32,09 vs 35,30; ticket 3 = 36,10 vs 13,90).
    Detalle en `docs/evidence/extraccion-local/latencias.md` y `emulador_humo_logcat.txt`.
  - Incidente: al lanzar el resto de la suite `:app` con `am instrument -e notClass` use un nombre
    de paquete erroneo y `AjustesModeloFlujoTest` se ejecuto y BORRO el modelo del emulador; se
    restauro con `adb push` desde `E:\AI-Expert\Modelos\` (3.655.827.456 bytes, ya verificado con
    un ticket posterior) y el fichero `hf_access_token` sigue en `no_backup`. Esa suite :app dio
    14/14 OK. NO se ejecuto `:app:connectedDebugAndroidTest`.
- Status: `extraccion-local` = **passing** (gate fisico Tab S9+ ejecutado, ver seguimiento; lista para `feature-validator`).
- Hallazgo de calidad: tickets 1 y 3 fallan el total en local (R3); iterar prompt local (T8, max 2
  rondas) tras ver los resultados de la Tab.

### Session 033 (seguimiento): gate fisico en la Tab S9+

- Date: 2026-10-18
- Completed:
  - Tab S9+ (`SM-X810`, Android 16, arm64, ~11,4 GB RAM) conectada por adb inalambrico:
    `installDebug installDebugAndroidTest`, modelo subido con `adb push` desde
    `E:\AI-Expert\Modelos\` (3.655.827.456 bytes en 95 s) y
    `am instrument -e class ...ExtraccionLocalRealTest` = **OK (5 tests, 144 s)**.
  - 4/4 extracciones validas con `motor=LOCAL`, `ranOnMainThread=false`; carga 6.887 ms
    (fria) y ~0,6 s despues; generacion 36.620 / 32.526 / 25.142 / 39.263 ms (< 60 s: plan B
    de R1 no necesario). Totales 2/4 correctos (tickets 2 y 4), registrados sin ser gate (D5).
  - Evidencia: `docs/evidence/extraccion-local/tab_s9_logcat.txt` y seccion Tab S9+ de `latencias.md`.
- Hallazgo operativo: el primer intento salto los 5 tests por `assumeTrue` (la app veia "No
  descargado") porque el directorio `models/` lo creo el shell de adb. Se resolvio recreandolo
  con `run-as com.robertomr.tally mkdir` y moviendo el fichero. El `README.md` del gate debe
  crear el directorio con `run-as` (o descargar desde Ajustes).
- Status: `extraccion-local` = **accepted** (validator independiente: ACCEPT; repitio el gate JVM y el gate fisico en la Tab S9+: 4/4 validos, generacion 26,8-41,2 s).
- Known risk or unresolved issue:
  - Calidad 2/4 en totales (ambiguedad OCR, R3): hallazgo de la comparativa con la nube.
  - Test inestable ajeno visto una vez: `EscanearViewModelTest.emptyOcrResultReachesRevisionAsEmptyTextWithoutBlocking`.
- Next best step: `planner` sobre `router-automatico` (deps `extraccion-local` y `extraccion-nube` accepted). Opcional: endurecer el test flaky `EscanearViewModelTest.emptyOcrResult...` y aislar `AjustesModeloFlujoTest` del token guardado.

### Session 034

- Date: 2026-10-18
- Goal: push de `extraccion-local` (hecho: `ddbb2c9` en `origin/master`) y planificar
  `router-automatico` (deps `extraccion-local`, `extraccion-nube`, `descarga-modelo` accepted).
- Completed:
  - Spec creada: `docs/specs/router-automatico.md` (~170 lineas), sin decisiones bloqueantes.
  - Diseno: `EngineRouter` en dominio como el `TextEngine` sin calificar del grafo Hilt,
    delegando en `@Named("local")` o `@Named("nube")`; politica pura
    `elegirMotor(preferencia, capaz, modeloDisponible)`; Local solo si `capaz` (RAM total >= 10 GB,
    heuristica) y `ModelDownloader.rutaModelo() != null`, si no Nube; sin fallback ante fallo
    (ADR 0001); `PreferenciaMotorSource` fija en AUTO (punto que sustituira `ajustes-motor`);
    sin cambios de `ExtraerTicket`, `EscanearViewModel`, UI ni Room. `reintento-otro-motor` y
    `ajustes-motor` quedan fuera de alcance.
  - D1 (producto) decidida por el usuario: **Auto = LOCAL PRIMERO** (el local acierta 2/4 totales
    y tarda 26-41 s en la Tab; mitigacion con `reintento-otro-motor`).
- Verification run: planificacion (sin gates de producto; cero cambios de codigo).
- Evidence captured: `docs/specs/router-automatico.md`.
- Known risk or unresolved issue:
  - RAM del realme GT 2 desconocida (registrar `MemTotal` de ambos dispositivos).
  - Con el modelo en el emulador el router elegiria Local si el emulador fuera "capaz"; la
    heuristica de RAM (>= 10 GB) lo evita, pero hay que verificar la regresion de E2E.
  - Cuota de Gemini agotada desde 2026-10-17: la prueba fisica de nube puede no poder ejecutarse.
- Next best step: `implementer` sobre `docs/specs/router-automatico.md`.

### Session 035

- Date: 2026-10-18
- Goal: implementar `router-automatico` (spec `docs/specs/router-automatico.md`, D1 = Auto local primero).
- Completed:
  - Dominio: `PreferenciaMotor`, `CapacidadDispositivo`, `PreferenciaMotorSource`, `EngineRouter` +
    `elegirMotor` (pura). Framework: `router/RamCapacidadDispositivo` (`internal`, RAM >= 10 GB,
    `ramSuficiente`). `AiModule`: `@Named("nube")`, `@Named("local")`, capacidad, preferencia fija
    AUTO y el `TextEngine` sin calificar = router. `TextEngineEntryPoint` + `textEngineNube()`;
    `ExtraccionNubeRealTest` usa `textEngineNube()`; test de identidad de `ExtraccionLocalRealTest`
    reescrito. Doble `FakeCapacidadDispositivo`. Sin cambios de VM/UI/Room/`init.sh`/`schemas`.
  - Docs: `docs/architecture.md` (router) y `docs/risks-and-open-questions.md` (R-RT).
- Verification run:
  - `gradlew.bat assembleDebug testDebugUnitTest :test:unit:test :domain:expenses:test` verde;
    169 tests JVM, 0 fallos (EngineRouterTest 5, RamCapacidadDispositivoTest 2).
  - emulator-5554 (MemTotal 5033804 kB): `installDebug installDebugAndroidTest` + `am instrument`
    sin AjustesModeloFlujoTest/ExtraccionLocalRealTest/ExtraccionNubeRealTest: OK 11 tests; identidad
    del router OK. El modelo en el emulador siguio presente (3655827456 B) antes y despues; el router
    elige Nube por RAM < 10 GB. ExtraccionNubeRealTest no se ejecuto (cuota Gemini agotada).
- Known risk or unresolved issue: verificacion fisica pendiente (Tab S9+ y realme GT 2); MemTotal
  del realme sin medir; R-RT1 calidad local 2/4.
- Next best step: verificacion fisica con permiso del usuario; `feature_list.json` queda `in_progress`.
- Update (sesión 035, verificación en Tab S9+): test nuevo `ExtraccionLocalRealTest#routerPicksLocalOnCapableDeviceWithModel`
  (assume RAM >= 10 GB y modelo). Tab S9+ (MemTotal 11454088 kB), `installDebug installDebugAndroidTest` sin
  desinstalar (modelo intacto, 3655827456 B) + `am instrument`: identidad + router OK (2 tests, 113 s); el router
  eligio LOCAL y extrajo texto_crudo_2 (total 3.5, 47149 ms, motor=LOCAL). Evidencia: `docs/evidence/router-automatico/`.
  `router-automatico` -> **passing**. Pendiente OPCIONAL no bloqueante: UI con camara real en la Tab (badge Local,
  cancelacion fisica de procesado-ui) y realme GT 2 sin modelo (cuota Gemini, MemTotal).

### Session 035 (seguimiento): validacion de router-automatico

- Date: 2026-10-18
- Completed:
  - `feature-validator` independiente: **ACCEPT**. Re-ejecuto el gate JVM (169 tests, `EngineRouterTest` 5/5,
    `RamCapacidadDispositivoTest` 2/2) y repitio en la Tab S9+ los dos tests del router por `am instrument`
    (OK, 2 tests): router sin calificar `motor=LOCAL`, total 3,5, `loadMillis=574` + `generationMillis=29390`
    (~30 s, < 60 s).
  - Aclarada la incoherencia de latencia: el `generationMillis=107103` del log original no es atribuible a la
    extraccion medida; corregido en `docs/evidence/router-automatico/README.md`.
- Status: `router-automatico` = **accepted**.
- Known risk or unresolved issue:
  - Test inestable ajeno (pasa en aislamiento): `EscanearViewModelTest.capturingProcessesAndSavesThePhotoAtCaptureTime`
    (y `emptyOcrResult...`, visto antes): carrera bajo ejecucion paralela. Endurecer cuando convenga.
  - Opcionales: UI con camara real en la Tab (badge Local, cancelacion fisica de `procesado-ui`) y realme GT 2
    sin modelo (cuota Gemini, `MemTotal`; R-RT2 abierto).
- Next best step: `planner` sobre `reintento-otro-motor` (dep `router-automatico` accepted).

### Session 036

- Date: 2026-10-18
- Goal: push de `router-automatico` (hecho: `33adcf8` en `origin/master`) y planificar
  `reintento-otro-motor` (dep `router-automatico` accepted).
- Completed:
  - Spec creada: `docs/specs/reintento-otro-motor.md` (~170 lineas, 10 escenarios), sin modulos ni dependencias nuevas.
  - Diseno: `ExtractorPorMotor` (dominio) implementado por `EngineRouter`; casos de uso
    `ExtraerTicketConMotor` y `ObtenerOtroMotor`; el reintento reutiliza el `textoCrudo` del Borrador
    (sin OCR nuevo) e ignora la preferencia; `MotorNoDisponibleException` sin fallback; boton
    "Probar con {otro}" en resultado y error; `ProcesandoExtraccion`/`ExtraccionFallida` llevan el
    `motor` (badge honesto); "Reintentar" repite con el motor que fallo; sin reintento desde la
    revision editable.
  - Decisiones del usuario: **D4 = conservar el resultado anterior** mientras dura el reintento
    (cancelar o fallar vuelve al anterior, con `anterior` en la orden y `Restaurar`). D2 (Local solo
    si capaz y modelo descargado) y D3 (boton deshabilitado con motivo) por defecto.
- Verification run: planificacion (sin gates de producto; cero cambios de codigo).
- Evidence captured: `docs/specs/reintento-otro-motor.md`.
- Known risk or unresolved issue:
  - Cuota de Gemini agotada desde 2026-10-17: la mitad Nube de la prueba fisica puede no ser posible.
  - R-RO2: liberacion/cancelacion del modelo local al encadenar un Local cancelado con otro intento.
  - Cambia el constructor de `EscanearViewModel` y los campos de `CapturaUiState`
    (actualizar `EscanearViewModelTest`); test inestable previo `capturingProcessesAndSavesThePhotoAtCaptureTime`.
- Next best step: `implementer` sobre `docs/specs/reintento-otro-motor.md`.

### Session 037

- Date: 2026-10-18
- Goal: implementar `reintento-otro-motor` (spec `docs/specs/reintento-otro-motor.md`, sesion 036).
- Completed:
  - Dominio: `DisponibilidadMotor`, `OfertaMotor`, `MotorNoDisponibleException`, `ExtractorPorMotor`,
    `disponibilidadMotor()` pura, `EngineRouter` implementa tambien `ExtractorPorMotor` (ignora la
    preferencia, sin fallback), casos de uso `ExtraerTicketConMotor` y `ObtenerOtroMotor`.
  - DI (`AiModule`, unico `di.kt`): `EngineRouter` singleton expuesto como `TextEngine` y `ExtractorPorMotor`.
  - `EscanearViewModel`: `ReintentarCon`/`Restaurar`, estado con `motor`/`anterior`/`otro`,
    eventos `onReintentarConOtroMotor` y `onVolverAlResultadoAnterior`; `onReintentarExtraccion`
    repite con el motor que fallo; `onCancelarProcesado` restaura el anterior si lo hay (D4).
    Badge desde el estado (S8).
  - UI: boton `procesado_reintentar_otro` (+ `procesado_reintentar_otro_motivo` si deshabilitado) en
    resultado y error; `procesado_volver_anterior` en el error con `anterior`. Tags previos intactos.
  - Docs: `docs/architecture.md` (seccion Procesado).
- Verification run (todo real, esta sesion):
  - `gradlew.bat assembleDebug testDebugUnitTest :test:unit:test :domain:expenses:test
    :app:compileDebugAndroidTestKotlin --console=plain` -> BUILD SUCCESSFUL. Base previa tambien verde.
  - JUnit XML: `EscanearViewModelTest` 44/44 (10 nuevos), `EngineRouterTest` 11/11 (5 nuevos),
    `ExtraerTicketConMotorTest` 3/3, `ObtenerOtroMotorTest` 2/2. Sin fallos de los tests inestables ajenos.
  - Tab S9+ (unica conectada; emulator-5554 NO conectado, no arrancado): `installDebug
    installDebugAndroidTest` (-r) y `am instrument -e class ...ExtraccionLocalRealTest` -> OK (6 tests, 820 s):
    el grafo Hilt con el router como `TextEngine` + `ExtractorPorMotor` funciona en dispositivo.
    Modelo `gemma-3n-E2B-it-int4.litertlm` 3655827456 B intacto antes y despues.
- NO ejecutado (pendiente): E2E `:app` en emulator-5554 (`ProcesadoFlujoTest` ampliado con
  `resultadoOfreceProbarConElOtroMotorYElBadgeCambiaODeshabilitaConMotivo`, solo compilado; necesita camara
  virtual) y verificacion fisica de UI en la Tab (ver abajo).
- Desviacion de la spec: `onReintentarConOtroMotor` desde `ExtraccionFallida` conserva el `anterior`
  que ya tuviera (la spec decia `anterior=null`), para que cancelar no pierda R1 (coherente con D4).
- Verificacion fisica pendiente (orquestador + usuario, Tab S9+, modelo descargado): (1) Local -> resultado ->
  "Probar con Nube · Gemini" (badge Nube, sin nueva captura/OCR en logcat); (2) Nube -> "Probar con Local",
  cancelar a mitad -> vuelve al resultado anterior; (3) error de nube -> "Probar con Local" y "Reintentar"
  repite con Nube; (4) modelo eliminado -> boton deshabilitado con "Descarga el modelo local en Ajustes para
  usarlo". Cuota Gemini agotada desde 2026-10-17 puede limitar la parte Nube.
- Status: `reintento-otro-motor` = **in_progress** (falta E2E emulador y verificacion fisica).
- Next best step: ejecutar E2E `:app` en emulador y la verificacion fisica; luego `validator`.

## Session 038 - reintento-otro-motor E2E + validacion (2026-10-18)

- E2E `:app` en emulator-5556 (emulator-5554 quedo offline; AVD limpio de 4 GB): `adb shell am instrument`
  con `-e notClass` de los tests reales -> OK (15 tests, 48 s), incluido
  `resultadoOfreceProbarConElOtroMotorYElBadgeCambiaODeshabilitaConMotivo`. Evidencia:
  `docs/evidence/reintento-otro-motor/README.md`.
- Validator independiente: **accept** (JVM dominio + escanear y assembleDebug en verde; sin imports de
  Android en dominio; sin secretos). Desviacion de S6 (conservar `anterior` desde error) aceptada.
- Pendiente opcional no bloqueante: verificacion fisica en Tab S9+ (Local->Nube sin nuevo OCR, cancelar a
  mitad, modelo eliminado, error de nube; cuota Gemini agotada).
- Status: `reintento-otro-motor` = **accepted**. Next: `ajustes-motor`.

## Session 039 - ajustes-motor implementacion (2026-10-18)

- Implementado (spec `docs/specs/ajustes-motor.md`, defaults: SharedPreferences sin dependencia nueva; Local
  forzado no disponible = `MotorNoDisponibleException` sin invocar motor; aviso solo por modelo no
  descargado; Nube sin red = fallo propio del motor). Dominio: `PreferenciaMotorStore`,
  `ObservarPreferenciaMotor`, `ConfigurarPreferenciaMotor`, check en `EngineRouter.extract`. Framework:
  `SharedPreferencesPreferenciaMotor` (internal) + `AiModule`. UI: `SeccionMotor` en Ajustes,
  `AjustesViewModel` (`preferencia`, `avisoLocal`, `onElegirMotor`). Doble `FakePreferenciaMotorStore`.
- Verificacion real: `gradlew.bat assembleDebug testDebugUnitTest :domain:expenses:test :test:unit:test
  --rerun-tasks` -> BUILD SUCCESSFUL. JUnit: `EngineRouterTest` 16, `PreferenciaMotorUseCasesTest` 2,
  `AjustesViewModelTest` 13, `SharedPreferencesPreferenciaMotorTest` 4, `EscanearViewModelTest` 44/44.
- E2E en emulator-5556 (solo emulador; ningun dispositivo fisico usado): `AjustesMotorFlujoTest` 3/3 y suite
  `:app` completa (sin los *RealTest) OK (18 tests). Persistencia tras `am force-stop` verificada con
  uiautomator + captura. Evidencia: `docs/evidence/ajustes-motor/README.md`.
- Pendiente (usuario): verificacion fisica en Tab S9+ (Nube forzada -> badge Nube en el siguiente ticket y
  viceversa con Local; Local sin modelo -> error visible; reinicio). Cuota Gemini agotada puede limitar Nube.
- Status: `ajustes-motor` = **passing** (listo para validator).

## Session 040 - ajustes-motor validacion (2026-10-18)

- Validator independiente: **accept** (assembleDebug + testDebugUnitTest verdes, diff revisado contra spec y
  AGENTS.md, E2E de la Session 039 apoyado en el README de evidencia). Hallazgo menor: orden de imports en
  `AjustesScreen.kt`.
- Pendiente opcional no bloqueante: verificacion fisica en Tab S9+ (Nube/Local forzado en la siguiente
  extraccion, Local sin modelo = error visible, persistencia tras reinicio).
- Status: `ajustes-motor` = **accepted**.

## Session 041 - periodo-selector implementacion (2026-10-18)

- Implementado (spec `docs/specs/periodo-selector.md`; defaults D1-D7 aprobados por el usuario: seleccion sin
  persistir y Mes por defecto, sin segmento Personalizado, semana lunes-domingo fija, `Clock` en nuevo `di.kt`
  de `:app`, sin modulo nuevo (se usa `:feature:resumen`), cifra provisional suma + nº de tickets, sin refresco
  a medianoche). Dominio: `PeriodoTipo`, `Periodo.de`, `ObtenerPeriodo(Clock)`, `ObservarTicketsDelPeriodo`.
  `:app`: `di/di.kt` (`AppModule`, `Clock`) y `TicketRepositoryEntryPoint` (solo para el E2E, patron de
  `TextEngineEntryPoint`). `:feature:resumen`: Hilt + dominio, `ResumenViewModel`, `ResumenState`,
  `PeriodoSelector`, `EtiquetaPeriodo` y `ResumenScreen` (conserva tags `pantalla_resumen`/`boton_ajustes`).
- Desviacion registrada: `stateAsResultIn` y `AcScaffold` (AGENTS.md) NO existen en el repo; `ResumenViewModel`
  usa `stateIn` directo como `AjustesViewModel`. No se crearon aqui.
- Verificacion real: `gradlew.bat :domain:expenses:test :feature:resumen:testDebugUnitTest --rerun` y
  `gradlew.bat assembleDebug testDebugUnitTest` -> BUILD SUCCESSFUL. JUnit: `PeriodoTest` 11,
  `ObtenerPeriodoTest` 4, `ObservarTicketsDelPeriodoTest` 3, `ResumenViewModelTest` 5, `EtiquetaPeriodoTest` 5.
- E2E en emulator-5556 (emulator-5554 offline; ningun dispositivo fisico usado; sin connected*):
  `PeriodoSelectorFlujoTest` 2/2 y suite `:app` completa sin los *RealTest 20/20. El E2E borra lo que siembra.
  Evidencia: `docs/evidence/periodo-selector/README.md` (capturas por segmento + `selected` por uiautomator).
- Observacion: la base del emulador conserva tickets de 0,00 € dejados por otros E2E (`RevisionDatosFlujoTest`);
  no afecta al test (usa lineas base).
- Pendiente: revision visual humana de las capturas; tests inestables ajenos conocidos de `EscanearViewModelTest`
  no se vieron fallar en esta sesion.
- Status: `periodo-selector` = **passing** (listo para validator). Siguiente: `resumen-total`.

## Session 042 - periodo-selector validacion (2026-10-18)

- Validator independiente: **accept** (tests de dominio y `:feature:resumen` re-ejecutados; codigo, bordes de
  calendario y capturas revisados; E2E apoyado en la evidencia de emulator-5556).
- Nota menor: `sembrarUno` del E2E decide la fecha de captura por el total sembrado (fragil).
- Status: `periodo-selector` = **accepted**.

## Session 043 - resumen-total implementacion (2026-10-18)

- Implementado (spec `docs/specs/resumen-total.md`, defaults D1-D6): promedio sobre dias transcurridos si el
  periodo esta en curso; comparativa contra el periodo anterior completo; anterior vacio = `variacion` null y
  badge neutro "Sin datos del período anterior"; badge sin semantica verde/rojo; texto "N tickets"; `Double`
  con redondeo al formatear. Dominio: `Periodo.anterior()`, `ResumenPeriodo`, `CalcularResumenPeriodo`,
  `ObservarResumenDelPeriodo`. `:feature:resumen`: `TarjetaTotal`, `FormatoResumen` (`EstiloTabular` tnum),
  `ResumenUiState.resumen` (sustituye `numTickets`/`suma`), `ResumenViewModel` con el nuevo caso de uso.
  Sin modulos ni dependencias nuevas; `build-logic/` intacto. `docs/architecture.md` actualizado.
- Verificacion: `assembleDebug testDebugUnitTest` verde; PeriodoTest 15, CalcularResumenPeriodoTest 12,
  ObservarResumenDelPeriodoTest 2, ResumenViewModelTest 6, FormatoResumenTest 6. E2E en emulator-5556 (5554
  offline; sin fisicos ni connected*): `OK (23 tests)` incluyendo `ResumenTotalFlujoTest` 3/3. Evidencia y
  capturas en `docs/evidence/resumen-total/README.md`.
- Pendiente: revision visual humana de tnum/capturas (la fuente Inter si contiene `tnum`). Test inestable
  ajeno de `:feature:escanear` fallo una vez y paso al reejecutar. La base del emulador conserva tickets de
  otros E2E (el test usa linea base).
- Status: `resumen-total` = **passing** (listo para validator).

## Session 044 - resumen-total validacion (2026-10-18)

- Validator independiente: **accept** (tests de dominio y `:feature:resumen` re-ejecutados; matematica de
  metricas, formato espanol y capturas revisados; E2E apoyado en la evidencia de emulator-5556).
- Pendiente humano no bloqueante: revision visual de las cifras tabulares (`tnum`).
- Status: `resumen-total` = **accepted**.

## Session 045 - grafica-donut implementacion (2026-10-18)

- Implementado (spec `docs/specs/grafica-donut.md`, defaults D1-D7): dominio `DistribucionCategorias`/
  `PorcionCategoria`, `CalcularDistribucionCategorias` (centimos Long HALF_UP, resto mayor, orden por importe,
  omite 0/NaN/inf) y `ObservarDistribucionDelPeriodo`. `:feature:resumen`: `TarjetaDistribucion`, `DonutCategorias`
  (Canvas + `segmentosDonut` pura), `ResumenUiState.distribucion` (combine en el VM). `CategoriaUi.kt` en
  `:feature:common` (D2; `:feature:resumen` -> `:feature:common` y `:feature:common` -> `:domain:expenses`, sin
  modulos nuevos; `:feature:escanear` no tocado, duplicacion de etiqueta/icono anotada como deuda).
- D1: paleta semantica; el ambar pasa a `#B86A22` (el test dio 3,18-3,71:1 vs 3:1 exigido). DESIGN.md actualizado,
  Open Design Question #1 cerrada. Deuda menor: sin ancho maximo centrado en tablet (como `TarjetaTotal`).
- Verificacion: `assembleDebug testDebugUnitTest :app:assembleDebugAndroidTest` verde; JUnit Calcular 11,
  Observar 2, Segmentos 5, Contraste 2, ResumenViewModelTest 9. E2E emulator-5556: `OK (27 tests)` incl.
  `GraficaDonutFlujoTest` 4. Evidencia y capturas en `docs/evidence/grafica-donut/README.md`.
- Pendiente humano: revision visual de paleta y capturas y simulacion de daltonismo.
- Status: `grafica-donut` = **passing** (listo para validator).

## Session 046 - grafica-donut validacion (2026-10-18)

- Validator independiente: **accept** (tests de dominio y `:feature:resumen` re-ejecutados; matematica de la
  distribucion, `segmentosDonut`, contraste y capturas revisados; E2E apoyado en la evidencia de emulator-5556).
- Pendiente humano no bloqueante: revision visual de la paleta y simulacion de daltonismo.
- Status: `grafica-donut` = **accepted**.

## Session 047 - listado-gastos implementacion (2026-10-18)

- Implementado (spec `docs/specs/listado-gastos.md`, D1-D6 con defaults): dominio `GrupoDiaTickets`,
  `AgruparTicketsPorDia` y `ObservarListadoDelPeriodo` (conversion a centimos extraida a `aCentimos` interna
  compartida con la distribucion). D3: `PeriodoSelector`, `formatoImporte`, `EstiloTabular` y `etiquetaPeriodo` pasan a
  `:feature:common` (tags `periodo_*` y comportamiento sin cambios) junto con `FilaTicket`/`CabeceraDia`/`etiquetaDia`.
  `:feature:gastos` pasa a feature completa (VM, State, `LazyColumn` con claves; ahora usa `tally.hilt`);
  `:feature:resumen` gana `SeccionRecientes` (5 filas + "Ver todos (N)"); `:app` gana la ruta `ticket/{ticketId}` con
  `TicketPendienteScreen` (TODO `detalle-ticket`). Se conserva el tag `pantalla_gastos` en la raiz.
- Desviaciones: `GastosContenido` es publico (estado fijo en E2E del vacio); chip de categoria con texto
  `onSurfaceVariant` (el ambar no llega a 4,5:1); sin `Clock` en el VM (`hoy` = `ObtenerPeriodo(DIA).desde`).
- Verificacion y resultados reales en `docs/evidence/listado-gastos/README.md`: JVM verde, E2E emulator-5556
  `OK (34 tests)` con 2 saltados (assumeTrue): `denegado` (preexistente) y el vacio de Resumen/Gastos con datos reales
  (la base del emulador tiene 18 tickets hoy, total 0); vacio de Gastos cubierto por `ListadoGastosVacioTest`.
- Hallazgos: `EscanearViewModelTest.cameraFailureShowsErrorStateWithoutSaving` fallo una vez de forma intermitente
  (modulo no tocado). Los 18 tickets de total 0 de la base vienen de E2E previos que guardan sin limpiar.
- Pendiente: captura del vacio y de varios dias; revision visual humana.
- Status: `listado-gastos` = **passing** (listo para validator).

## Session 048 - listado-gastos validacion (2026-10-18)

- Validator independiente: **accept** (tests de dominio, common, gastos y resumen + assembleDebug
  re-ejecutados; E2E apoyado en la evidencia de emulator-5556, OK 34 tests, 2 saltados por `assumeTrue`).
- Orquestador tras el accept: nota del patron de fila/grupo en `DESIGN.md` y arreglo cosmetico de
  indentacion/KDoc en `ResumenViewModel.kt` (tests de resumen y gastos en verde).
- Deuda no bloqueante: capturas de vacio y de varios dias; E2E de vacio de Resumen (el emulador tiene 18
  tickets de 0,00 EUR residuales de otros E2E); test intermitente
  `EscanearViewModelTest.cameraFailureShowsErrorStateWithoutSaving`; revision visual humana.
- Status: `listado-gastos` = **accepted**.

## Session 049 - rango-personalizado implementacion (2026-10-19)

- Implementado (spec `docs/specs/rango-personalizado.md`, D1-D8 con los defaults): `PeriodoTipo.PERSONALIZADO`,
  `Periodo.personalizado` (anterior = N dias previos; `de(PERSONALIZADO)` lanza), `ObtenerPeriodo(tipo, personalizado)`;
  `:feature:common`: `FechasPicker.kt` (conversion UTC), `RangoFechasDialog.kt` (`DatePickerDialog` + `DateRangePicker`,
  sin futuros), `PeriodoSelector` de 4 segmentos con picker interno, `etiquetaPeriodo(periodo, hoy)` con anio cuando
  cruza anio o no es el actual; ViewModels de Resumen y Gastos con `Seleccion(tipo, rango)` y `onSeleccionarRango`
  (invertido ignorado), `rango` en los UiState; `TarjetaTotal` recibe `hoy`.
- Decisiones: Material3 del BOM 2026.02.01 no exige `@OptIn` para el date range picker (BOM sin tocar). La forma
  `extraLarge` del tema es una pildora de 9999 dp y deformaba el dialogo: se fija `RoundedCornerShape(28.dp)`.
  `PeriodoTipo.entries` adaptado en `PeriodoTest` y `PeriodoSelectorFlujoTest`.
- Verificacion (detalle en `docs/evidence/rango-personalizado/README.md`): `assembleDebug testDebugUnitTest` verde;
  dominio, common (9+3), resumen (16) y gastos (7) en verde; E2E en emulator-5556 `RangoPersonalizadoFlujoTest`
  OK (2 tests) y suite completa `OK (37 tests)`; selector cabe a 360 dp. emulator-5556 no estaba arrancado: se lanzo
  desde el AVD `Pixel_10_Pro` con `-port 5556` (tardo varios minutos en aparecer en adb).
- Pendiente: no se verifico cuales tests se saltan por `assumeTrue`; los tickets residuales de 0,00 EUR siguen en el
  emulador (no tocados); revision visual humana del picker.
- Status: `rango-personalizado` = **passing** (listo para validator).

## Session 050 - rango-personalizado validacion (2026-10-19)

- Validator independiente: **accept** (`assembleDebug` re-ejecutado; revision estatica de reglas, zona
  horaria del picker, periodo anterior, rango invertido, cabida a 360 dp y capturas; E2E apoyado en la
  evidencia de emulator-5556: `RangoPersonalizadoFlujoTest` 2 OK sin `assumeTrue`, suite `:app` 37 OK).
- Notas menores no bloqueantes: matcher de celdas del E2E dependiente del idioma (en-US), E2E sin probar en
  dia 1 del mes, imports cosmeticos en `GastosScreen.kt`, capturas 03-05 con 0,00 EUR por tickets residuales.
- Status: `rango-personalizado` = **accepted**.

## Session 051 - detalle-ticket implementacion (2026-10-19)

- Implementado (spec `docs/specs/detalle-ticket.md`, D1-D8 con los defaults): dominio `EdicionDatos.kt` (reductores
  puros, `conservarHora`), `ObtenerTicket`, `ActualizarTicket`, `BorrarTicket`; `TicketRepository.delete` traga el
  fallo de la foto (huerfano aceptado) y relanza `CancellationException`. `:feature:common`: `FormularioTicketEditable`,
  `DockEdicion`, `BadgeMotor`, `FotoOriginal` (muestreo en IO) movidos de escanear (tags `revision_*` intactos);
  `:feature:escanear` depende ahora de `:feature:common` y `EscanearViewModel` delega en los reductores.
  `:feature:gastos`: `TicketDetalleScreen/ViewModel/State` (id por `SavedStateHandle`, lectura unica, guarda anti
  doble toque). `:app`: ruta `ticket/{id}` real, barra inferior oculta, `TicketPendienteScreen` eliminado.
  `FakePhotoFileStore.deleteError` y `FakeTicketDataSource.deleteError/getByIdError` anadidos en `:test:unit`.
- Decisiones: la pantalla no recibe `ticketId` (lo lee el VM de la ruta); fallo de lectura = "no existe"; el
  dialogo de borrado fija forma 28 dp (la `extraLarge` del tema es pildora). Sin migracion Room ni deps nuevas;
  `framework/`, `schemas/`, `build-logic/`, `libs.versions.toml`, `init.sh` sin cambios.
- Verificacion y evidencia: ver `feature_list.json` y `docs/evidence/detalle-ticket/README.md` (JVM verde; E2E
  emulator-5556 `DetalleTicketFlujoTest` 6 OK; suite completa OK 43 tests). emulator-5556 estaba colgado
  (system_server caido) y se reinicio con `emu kill` + relanzar el AVD.
- Pendiente: revision visual humana de las capturas; skips por `assumeTrue` no desglosados por el runner.
- Status: `detalle-ticket` = **passing** (listo para validator).

## Session 052 - detalle-ticket validacion (2026-10-19)

- Validator independiente: **accept** (tests JVM de dominio, gastos, escanear y common re-ejecutados; refactor
  D1 sin cambio de comportamiento ni tags `revision_*`; borrado en cascada y semantica de fallo de foto;
  edicion que conserva campos; estados y limpieza revisados; E2E apoyado en la evidencia de emulator-5556).
- Notas menores: `RevisionFotoContent` ahora muestra "Foto no disponible" en vez de spinner eterno si la foto es
  ilegible; el dialogo de borrado cita el comercio guardado, no el editado sin guardar.
- Pendiente humano no bloqueante: revision visual de las capturas 02, 03, 05 y 06.
- Status: `detalle-ticket` = **accepted**.

## Session 053 - papel-ticket implementacion (2026-10-19)

- Feature `papel-ticket`: `PapelTicket.kt` en `:feature:common` (hoja con borde serrado `TicketSerradoShape`,
  sombra 2 dp + hairline, ancho max. 480 dp, `SeparadorPunteado`, `FilaRecibo`, `reciboTexto`, `importeDeLinea`).
  `FormularioTicketEditable` es ahora un solo papel (cabecera, desglose y total al final; sin barra verde ni tarjetas
  anidadas) y el resultado de `ProcesadoContent` (`ResumenDeTicket`) usa filas de recibo. Dominio, ViewModels,
  tags `revision_*`/`procesado_*`/`detalle_*` y edicion intactos; sin modulos ni dependencias.
- Decisiones: papel siempre claro (no hay tema oscuro), monoespaciada del sistema solo en el papel, sombra 2 dp,
  total al final (grep: ningun test depende del orden, usan `performScrollTo`), importe por fila = cantidad x precio.
  Con fontScale > 1,4 cantidad/precio se apilan y el importe de la fila baja a su linea. Excepciones en `DESIGN.md`.
- Verificacion: `gradlew.bat cleanTestDebugUnitTest assembleDebug testDebugUnitTest :test:unit:test` en verde
  (`PapelTicketTest` 2/2); E2E en `emulator-5556` con `am instrument -e notClass ExtraccionLocalRealTest,
  ExtraccionNubeRealTest`: OK (43 tests, incluye 3 nuevas aserciones `papel_ticket`). Capturas antes/despues
  (claro, "oscuro", 360 dp, fontScale 1,3 y 2,0) en `docs/evidence/papel-ticket/`.
- Pendiente: revision visual humana. Hallazgos previos no tocados: dock de edicion con fontScale 2,0 y chip
  "Categorizacion" del procesado se deforman.
- Status: `papel-ticket` = **passing** (listo para validator).

## Session 054 - papel-ticket validacion (2026-10-19)

- Validator independiente: **accept** (diff y codigo revisados; ningun tag `revision_*`/`procesado_*`/`detalle_*`
  perdido; tests no debilitados; accesibilidad, rendimiento y DESIGN.md revisados; capturas revisadas; JVM y E2E
  apoyados en la evidencia de emulator-5556, OK 43 tests sin desglose de `assumeTrue`).
- Orquestador: podadas las capturas duplicadas (`antes_*_b`) y las `despues_oscuro_*` (identicas al claro); deuda anotada.
- Deuda no bloqueante: fontScale 2,0 (dock de edicion y chip "Categorizacion", previos a esta feature); la
  revision editable aun se ve como formulario (campos con caja); semantica agrupada de `FilaRecibo`.
- Pendiente humano: revision visual.
- Status: `papel-ticket` = **accepted**.

## Session 055 - log-extracciones implementacion (2026-10-19)

- Feature `log-extracciones`: tabla `extracciones_log` en la misma `TallyDatabase` (v1 -> v2, decision D1 del usuario) con
  `MIGRATION_1_2` + `addMigrations` (sin `fallbackToDestructiveMigration`), `schemas/.../2.json`, `room-testing` en el
  catalogo y assets de schemas en `:framework:database`. Dominio: `RegistroExtraccion`, `Reloj`, `RegistroExtracciones`,
  decorador `EngineConRegistro` (ROUTER/REINTENTO, CANCELADA en `NonCancellable`, fallo del log ignorado), casos de uso
  observar/borrar/exportar CSV. DI: `:framework:ai` expone el decorador como `TextEngine` y `ExtractorPorMotor`.
  Ajustes: seccion debug (FLAG_DEBUGGABLE) con resumen, 20 ultimas, Copiar CSV, Borrar registro.
- Desviacion menor: entidad y DAO del log son publicos (como `TicketEntity`/`TicketDao`) porque Room no maneja bien
  DAOs `internal`; DataSource, Reloj y modulo Hilt siguen `internal`. `TextEngineEntryPoint` (main de `:app`) amplia
  3 accesores para los E2E.
- Verificacion: `gradlew.bat assembleDebug testDebugUnitTest :test:unit:test` en verde; en `emulator-5556`
  `:framework:database` OK 28 tests y `:app` OK 45 tests (`notClass` de los reales). Migracion real: el emulador tenia
  el build anterior con BD v1 (28 tickets/17 lineas); tras `installDebug` (install -r) y abrir la app: v2, mismos
  28/17 (suma 805.0 / 677.0), log vacio. Evidencia en `docs/evidence/log-extracciones/`.
- No cubierto en E2E: exito y cancelacion (requieren Gemini/motor retenible); estan en `EngineConRegistroTest`.
- Hallazgo previo no tocado: algun E2E existente (RevisionDatos/Procesado) deja 2 tickets vacios (ids 450, 451) en la
  BD del emulador por ejecucion completa; no se han borrado.
- Status: `log-extracciones` = **passing** (listo para validator).

## Session 056 - log-extracciones validacion (2026-10-19)

- Validator independiente: **accept** (`testDebugUnitTest` y `assembleDebug` re-ejecutados; SQL de `Migration(1,2)`
  identico a `2.json` y a la entidad, `1.json` intacto, sin `fallbackToDestructiveMigration`; privacidad,
  cancelacion, cobertura ROUTER/REINTENTO, retencion 500 y seccion solo debug revisados; instrumentados
  apoyados en la evidencia de emulator-5556: database 28 OK, app 45 OK, migracion real v1 a v2 con 28 tickets
  conservados).
- Hueco aceptado: E2E sin exito ni cancelacion (solo JVM con dobles y reloj fake).
- Notas menores: orden de imports en `AjustesScreen.kt`. Hallazgo ajeno: los E2E existentes dejan 2 tickets
  vacios en el emulador (tarea aparte).
- Status: `log-extracciones` = **accepted**.

## Session 057 - splash-animado (2026-10-19)

- Implementado: splash del sistema con `core-splashscreen` 1.2.0 (solo en `libs.versions.toml` y `:app`), tema
  `Theme.Tally.Starting` (fondo #002718) y `Theme.Tally`, emblema vectorial `ic_splash_tally` + AVD
  `avd_splash_tally` (trimPathEnd escalonado, ~680 ms, una pasada), `SplashPolitica` pura (hold max 700 ms, solo
  arranque en frio y primera creacion por proceso, 0 con animator_duration_scale 0 o SDK < 31) y
  `SplashPoliticaTest` 6/6. `MainActivity` llama a `installSplashScreen()` antes de `super.onCreate`; sin pantalla
  Compose ni ruta nueva. `DESIGN.md` enmendado (seccion "Splash de arranque").
- Verificacion en `emulator-5556` (API 37): gradlew build+tests+install en verde; `am instrument` con `notClass`
  de los reales `OK (45 tests)` sin tocar tests; screenrecord del arranque en frio (animacion fluida, sin blanco ni
  doble splash), escala 0 sin dibujo animado, escala restaurada a 1.0. Iteracion: 800 -> 700 ms para que el hold no
  corte la diagonal. Evidencia en `docs/evidence/splash-animado/`.
- No cubierto: camino API 26-30 (compat sin animar) no ejecutado en dispositivo; icono de launcher fuera de alcance.
- Status: `splash-animado` = **passing** (listo para validator). Siguiente: `seed-demo`.

## Session 058 - splash-animado validacion (2026-10-19)

- Validator independiente: **accept** (`:app:testDebugUnitTest` re-ejecutado; manifest, recursos, AVD, geometria
  fiel al mockup 01, `MainActivity`, `SplashPolitica` y `DESIGN.md` revisados; capturas vistas; instrumentados
  apoyados en la evidencia de emulator-5556 API 37: `SplashPoliticaTest` 6 OK, suite `:app` 45 OK).
- Limitaciones aceptadas: camino API 26-30 no ejecutado en dispositivo (icono estatico completo, hold 0); puntito
  breve al inicio de cada trazo (cabeza redonda); fluidez juzgada solo por cadencia de fotogramas; arranque en
  frio +0,2-0,4 s medido.
- Pendiente humano: ver la animacion en la tablet.
- Status: `splash-animado` = **accepted**.

## Session 059 - layout-tablet-coherente (2026-10-19)

- Implementado (rama `correcciones-visuales`): `ANCHO_MAXIMO_CONTENIDO` = 720 dp y `ColumnaCentrada`
  (`feature/common/LayoutAncho.kt`) como UNICA regla de ancho; `CabeceraPantalla` (`CabeceraPantalla.kt`) como
  cabecera comun (titulo `titleLarge` SemiBold `onSurface`, subtitulo `labelLarge`, atras y accion opcionales).
  Aplicado a Resumen ("Tally" -> "Resumen" + rango, tag `resumen_rango`), Gastos (tag `gastos_rango` conservado, el
  rango pasa a subtitulo bajo el titulo y el selector queda debajo), Detalle, Ajustes (sin `TopAppBar`; `:feature:ajustes`
  gana `implementation(project(":feature:common"))`, sin modulo ni version nuevos), Escanear (estados no camara en la
  columna; "Revisar foto" y "Revisar ticket" como cabeceras nuevas con `statusBarsPadding`; Procesado reutiliza su titulo),
  barra inferior (fondo completo, items en la columna), `DockEdicion` (fondo completo, contenido en la columna) y
  controles del obturador. Donut: sin porciones va solo y centrado; en fila, franja 40 % centrada + leyenda 60 %.
- Cambios de texto/estilo visibles en movil: Resumen "Tally" -> "Resumen" + rango; titulos de pantalla en `onSurface` (antes
  `primary` en Resumen/Gastos); Detalle y Ajustes titulo SemiBold; Ajustes 600 -> 720 dp y el contenido sube ~8 dp
  (cabecera de 56 dp en vez de TopAppBar de 64 dp); Escanear: "Revisar foto" y "Revisar ticket" (56 dp extra).
- Verificacion: `gradlew.bat assembleDebug testDebugUnitTest :app:installDebug :app:installDebugAndroidTest` en verde
  (197 tests JVM, 0 fallos); en `emulator-5556` `am instrument -e notClass ...Real*` = OK (45 tests), sin tocar tests.
  Capturas antes/despues en `docs/evidence/layout-tablet-coherente/` (movil 1280x2856@480, 360 dp, horizontal
  2560x1600@320 = 1280x800 dp; 700 y 840 dp). Medido: a 1280 dp la columna mide ~720 dp (140 dp de margen por lado);
  a 840 dp margen 60 dp; a 700 dp sin efecto. Emulador restaurado (wm size/density reset, user_rotation 0,
  accelerometer_rotation 1). Tablet fisica no tocada; sin `pm clear`.
- No cubierto: donut centrado en vacio/cargando solo por codigo (no se forzo un periodo sin gastos); estados
  Procesado/Revision sin captura "antes"; paddings de 24 dp en tablet (A3) fuera de alcance.
- Status: `layout-tablet-coherente` = **passing** (listo para validator). Siguiente: `seed-demo`.

## Session 060 - layout-tablet-coherente validacion (2026-10-19, rama correcciones-visuales)

- Validator independiente: **accept** (`assembleDebug` y `testDebugUnitTest` re-ejecutados; tags conservados,
  tests sin modificar, sin scrolls anidados, tope de 720 dp fuera de los paddings, `statusBarsPadding` sin
  duplicar, `DESIGN.md` y `architecture.md` coherentes, capturas revisadas; instrumentados apoyados en la
  evidencia de emulator-5556, OK 45 tests).
- Orquestador tras el accept: `semantics { heading() }` en el titulo de `CabeceraPantalla` (TalkBack) y suite
  `:app` re-ejecutada: OK 45 tests. Una ejecucion intermedia con el APK anterior dio 1 fallo no identificado
  que no se repitio al reinstalar y relanzar.
- Seguimiento: quitar el chip de rango duplicado de `TarjetaTotal` en Resumen (tocaria el tag `periodo_rango` y
  sus aserciones), unificar padding 16/20 dp, cabecera comun en Procesado, revision visual humana en la Tab.
- Status: `layout-tablet-coherente` = **accepted**.

## Session 061 - mejoras-visuales F1 implementacion (2026-10-19, rama correcciones-visuales)

- Alcance: hallazgos V01-V11, V19, V20 de `docs/specs/mejoras-visuales.md` (tareas 1-8). V12-V25 son de
  `mejoras-visuales-2`; V26-V30 diferidos. Decisiones por defecto: A2 quitar el chip de `TarjetaTotal` y conservar el
  subtitulo, A3 `extraLarge` = 28 dp en el tema, A4 division en dos features; A1 (campos con subrayado) diferida.
- Hecho: V01 (`consumeWindowInsets` en el `NavHost`), V02 (`enableEdgeToEdge` con `SystemBarStyle.light`), V03 (dock
  apilado con fontScale > 1,15, pesos 1:1,3 en fila y total en <=2 lineas), V04 (pasos apilados con fontScale > 1,4 y
  `maxLines=1`), V05 (forma `extraLarge` 28 dp, parches retirados, boton `error` en Ajustes), V06 (`aspectRatio` en
  `FotoOriginal`), V07 (`CabeceraPantalla` en los 3 estados de Procesado; "Resultado" en resultado/fallo), V08, V09,
  V10/V11 (`PADDING_PANTALLA` = 20 dp; "n/5 campos" alineado al papel con `PAPEL_ANCHO_MAXIMO` ahora publico), V19
  (fundido 150 ms entre pestanas, deslizamiento 300 ms hacia/desde Detalle y Ajustes), V20 (`imePadding` en Detalle y
  Revision de datos). Extra: Revisar foto usa fondo `surface` (con `surfaceContainerHigh` quedaba una banda bajo el
  reloj).
- Verificacion: `gradlew.bat assembleDebug testDebugUnitTest :app:installDebug :app:installDebugAndroidTest` en
  verde (197 tests JVM, 0 fallos); `am instrument -e notClass ...ExtraccionLocalRealTest,...ExtraccionNubeRealTest`
  en `emulator-5556`: OK (45 tests). Capturas antes/despues en `docs/evidence/mejoras-visuales/`.
- Tests tocados: `periodo_rango` -> `resumen_rango` (mismo valor esperado) en `RangoPersonalizadoFlujoTest` y
  `PeriodoSelectorFlujoTest`; "periodo" sin tilde en `FormatoResumenTest` y `ResumenTotalFlujoTest`. Nada debilitado.
- No hecho / pendiente: captura del dialogo "Eliminar modelo" (requiere modelo descargado) y de las transiciones;
  "Repetir foto" se corta a fontScale 2,0 (nuevo, candidato F2); V12-V25 en `mejoras-visuales-2`.
- Estado: `mejoras-visuales` = **passing**, pendiente de validator independiente. El test temporal de capturas
  (`ZzCapturaTemporalTest`) se elimino; el emulador quedo restaurado (1280x2856 @480, font_scale 1.0, rotacion 0/auto).

## Session 062 - mejoras-visuales (F1) validacion (2026-10-20, rama correcciones-visuales)

- Validator independiente: **accept** (`assembleDebug` y `testDebugUnitTest` re-ejecutados; 13 hallazgos HACER
  V01-V11, V19 y V20 verificados en diff y capturas; nada de F2/F3 colado; cambios de tests solo los declarados:
  `periodo_rango` a `resumen_rango` x4 con el mismo valor esperado y "periodo" sin tilde x3, sin debilitar
  aserciones; insets, transiciones, forma `extraLarge` 28 dp e `imePadding` revisados; instrumentados apoyados
  en la evidencia de emulator-5556, OK 45 tests).
- Aceptado sin verificar: dialogo "Eliminar modelo" de Ajustes (sin captura), transiciones (solo E2E de
  navegacion), `SystemBarStyle.light` solo en API 37, estados procesando y fallo de Procesado sin captura.
- Pendiente humano: revision visual de las capturas en la Tab.
- Candidato nuevo para `mejoras-visuales-2`: "Repetir foto" se corta a fontScale 2,0.
- Status: `mejoras-visuales` = **accepted**.

## Session 063 - mejoras-visuales-2 (F2) implementacion (2026-10-20, rama correcciones-visuales)

- Alcance: hallazgos V12-V25 de `docs/specs/mejoras-visuales.md` mas el candidato "Repetir foto" (Revisar foto a
  fontScale 2,0). No se tocaron V26-V30 (diferidos) ni V31-V39 (descartados). Se usaron los defaults de la spec; no
  quedo ninguna decision de diseno abierta.
- Hecho: V12 (selector con segmentos de 48 dp, dos filas con fontScale > 1,1, sin check en ese modo, elipsis en vez de
  recorte), V13 (`TarjetaSeccion` en `:feature:common`, usada por Resumen y las 4 tarjetas de Ajustes), V14
  (`TituloSeccion` con `heading` y mismo peso en Resumen, Ajustes y "Desglose de articulos"), V15 (`EstadoPantalla` y
  `CargandoPantalla`: vacio de Gastos, "Este ticket ya no existe", permiso y error de captura, fallo de Procesado;
  Gastos ya muestra indicador al cargar; tags conservados), V16 (area tactil de 48 dp en chips de categoria con
  `minimumInteractiveComponentSize`), V17 (`FilaRecibo` fusionada semanticamente), V18 (`selectableGroup` en la barra
  inferior), V21 (badge de motor dentro del scroll en Detalle y Revision), V22 (titulo de cabecera hasta 2 lineas), V23
  (`outline` -> `onSurfaceVariant` en los dos textos de 11 sp), V24 (icono Ajustes `Outlined`), V25 (`Role.Button` en
  obturador y "Cerrar"). "Repetir foto": dock de Revisar foto apilado con el mismo umbral que el dock de edicion
  (`UMBRAL_APILAR_BOTONES` = 1,15, ahora publico en `LayoutAncho.kt`) y pesos 1:1 en fila (con 1:1,4 ya salia
  "Repetir f..." a 360 dp).
- Verificacion: `gradlew.bat assembleDebug testDebugUnitTest :app:installDebug :app:installDebugAndroidTest` en verde
  (197 tests JVM, 0 fallos); `am instrument -e notClass ...ExtraccionLocalRealTest,...ExtraccionNubeRealTest` en
  `emulator-5556`: OK (45 tests), tambien tras eliminar los tests temporales de captura y reinstalar el APK.
  `emulator-5556` se relanzo (estaba apagado) con el AVD Pixel_10_Pro sin wipe.
- Tests tocados: ninguno (ni textos ni tags ni estructura lo exigieron). No se elimino ningun tag; solo se anade
  `listado_cargando` (indicador de carga de Gastos).
- Capturas en `docs/evidence/mejoras-visuales-2/` (24 PNG ligeros + README); incluyen por primera vez Procesado en curso,
  fallo de Procesado, Gastos vacio y cargando. Emulador restaurado (1280x2856 @480, font_scale 1.0, rotacion 0/auto).
- No hecho / pendiente: dialogo "Eliminar modelo" sin captura (exige modelo descargado y `AjustesContent` es internal);
  `captura_error` sin captura (composable privado). Detectado y fuera de alcance: a fontScale 2,0 el fallo de Procesado
  recorta sus botones ("Probar con Local", "Volver a la foto", "Reintentar") y la etiqueta de proceso queda pegada al
  borde; "Promedio diario" se parte en 3 lineas en la tarjeta de Resumen.
- Docs: `DESIGN.md` (fuentes grandes, estados de pantalla, tarjetas/secciones, contraste, accesibilidad tactil, radio de
  tarjeta) y `docs/architecture.md`.
- Estado: `mejoras-visuales-2` = **passing**, pendiente de validator independiente.

## Session 064 - mejoras-visuales-2 (F2) validacion (2026-10-20, rama correcciones-visuales)

- Validator independiente: **accept** (`assembleDebug` y `testDebugUnitTest` en cache, sin re-ejecutar los
  tests; V12-V25 y "Repetir foto" verificados en diff y capturas; ningun test modificado ni tag eliminado, solo
  anadido `listado_cargando`; APK de instrumentacion revisado sin tests temporales; accesibilidad y contraste
  `outline` a `onSurfaceVariant` revisados; instrumentados apoyados en la evidencia de emulator-5556, OK 45).
- Deuda pasada a `mejoras-visuales-3`: selector en dos filas sin check del segmento activo (casi solo color),
  fallo de Procesado a fontScale 2,0 y "Promedio diario" en tres lineas.
- Sin verificar visualmente: dialogo "Eliminar modelo", `captura_error`, transiciones y TalkBack.
- Status: `mejoras-visuales-2` = **accepted**.

## Session 065 - forma-pago implementacion (2026-10-20, rama master)

- Feature: `forma-pago` (spec `docs/specs/forma-pago.md`). Defaults de las decisiones abiertas aplicados: (1) cambio
  calculado y editable con aviso no bloqueante; (2) efectivo a tarjeta conserva entregado/cambio ocultos y se limpian al
  guardar; (3) entregado o cambio sin forma indicada infieren EFECTIVO; (4) entregado < total solo avisa; (5) bizum,
  vale o mixto = NO_INDICADA.
- Implementado: dominio (`FormaPago`, `Pago.kt` con `normalizarPago`/`normalizada()`/`avisoPago`, campos con valores
  por defecto en `Ticket`/`TicketExtraido`/`DatosRevision`, reductores `conFormaPago`/`conEntregadoTexto`/
  `conCambioTexto`); Room v3 (`TicketEntity`, `MIGRATION_2_3`, `di.kt` con `addMigrations(MIGRATION_1_2, MIGRATION_2_3)`,
  mapper tolerante, `3.json`); extraccion (bloque REGLAS DE PAGO sin tocar la REGLA DEL TOTAL, `responseSchema` con
  `forma_pago` enum y `entregado`/`cambio` opcionales, DTO y parser con defaults); UI (`FormaPagoUi.kt`, selector,
  campos y aviso en `FormularioTicketEditable`, `PagoEnPapel` en el resultado, chip en `FilaTicket` y en el detalle);
  VMs de Escanear y Detalle solo cablean lambdas. Sin modulos ni dependencias nuevas; `build-logic/` intacto.
- Tipo real de `total` en `DatosRevision`: `Double` (no texto); entregado/cambio son `Double?` con la misma
  representacion y formato es-ES `%.2f` en los campos.
- Verificacion (PowerShell, `ANDROID_SERIAL=emulator-5556`): `gradlew.bat assembleDebug testDebugUnitTest
  :app:installDebug :app:installDebugAndroidTest :framework:database:installDebugAndroidTest` BUILD SUCCESSFUL, 0 fallos
  JVM; `am instrument` de `com.robertomr.tally.framework.database.test`: OK 31 tests; `am instrument -e notClass
  ...ExtraccionLocalRealTest,...ExtraccionNubeRealTest` de `com.robertomr.tally.test`: OK 48 tests (incluye
  `FormaPagoFlujoTest` 3). Nunca `connected*` ni `pm clear`.
- Migracion REAL: BD v2 del emulador respaldada en el scratchpad (fuera del repo) y comprobada antes (user_version 2, 22
  tickets, 11 lineas); `install -r` y arranque COLD sin crash; despues user_version 3, 22 tickets, 11 lineas,
  `forma_pago` = NO_INDICADA en los 22, entregado/cambio NULL, datos antiguos identicos.
- Tests existentes tocados: solo `MigrationTest.laBdMigradaSeAbreConRoomRealYElLogFunciona` (una linea: el builder pasa
  de `addMigrations(MIGRATION_1_2)` a `addMigrations(MIGRATION_1_2, MIGRATION_2_3)`, imprescindible porque la BD ya es
  v3 y esa prueba abre una v1 con la misma construccion que `DatabaseModule`; aserciones intactas). Los constructores
  de `Ticket`/`TicketExtraido`/`DatosRevision` compilan sin cambios gracias a los valores por defecto. Ningun tag
  eliminado; nuevos tags aditivos: `revision_forma_pago(_efectivo|_tarjeta|_no_indicada)`, `revision_pago`,
  `revision_entregado`, `revision_cambio`, `revision_aviso_pago`, `papel_forma_pago`, `papel_entregado`, `papel_cambio`,
  `detalle_forma_pago`, `ticket_<id>_forma_pago`.
- Capturas en `docs/evidence/forma-pago/` (12 PNG ligeros + README). Con ellas se corrigieron tres defectos: chips de
  efectivo y tarjeta del mismo verde oscuro y poco contraste, "Tarjet/a" partido a 360 dp en el selector y chip cortado
  a letra 2,0 junto al badge del detalle. Emulador restaurado (480 dpi, font_scale 1.0); tests temporales eliminados y
  APK de instrumentacion reinstalado. Un fallo transitorio de Gradle (`classes.jar` bloqueado por otro proceso) se
  resolvio reintentando; ademas `FotoOriginal.kt` aparecio con tres imports corrompidos sin que lo editara esta sesion
  y se restauro con `git checkout` (sin cambios propios en ese fichero).
- Docs: `docs/domain-model.md`, `CONTEXT.md`, `DESIGN.md`, `docs/architecture.md`, `docs/risks-and-open-questions.md`
  (R-PAGO1).
- NO verificado: `ExtraccionNubeRealTest` (cuota Gemini agotada desde 2026-10-17, no se ejecuto) y
  `ExtraccionLocalRealTest` (sin modelo local en el emulador): la lectura real de la forma de pago por los motores solo
  esta cubierta por tests JVM del parser con JSON de ejemplo de los 4 textos crudos y por el prompt. Tampoco hay test de
  `EscanearViewModel` para los tres handlers nuevos (mismos reductores que el detalle, probados en JVM) ni E2E del flujo
  de escaneo con pago (depende de camara/motor real); el papel de solo lectura de las capturas es `PagoEnPapel` en un
  `PapelTicket` (el `ResumenDeTicket` real es privado). TalkBack sin probar.
- Status: `forma-pago` = **passing**, pendiente de validator independiente.
- Siguiente paso: validar `forma-pago`; luego `filtro-forma-pago` y `gasto-manual` (dependen de ella).

## Session 067 - galeria-escaneo implementacion (2026-10-20, master)

- Feature: `galeria-escaneo` (spec `docs/specs/galeria-escaneo.md`), defaults de las decisiones abiertas: tope 32 MiB,
  etiqueta "Galería" visible, `espresso-intents` solo androidTest (misma versión que Espresso), sin encuadre sobre la imagen
  elegida, sin corregir EXIF espejo.
- Riesgo 1: `runCatchingLanzandoCancelacion` SÍ captura `Throwable` (incluye OOM), pero el mensaje sería "Failed to
  allocate"; `CapturedPhotoProcessor` convierte `OutOfMemoryError` en `IllegalStateException("La imagen es demasiado grande
  para procesarla...")` y `LecturaImagen` mapea OOM/tope a "La imagen es demasiado grande. Elige una de menos de 32 MB.".
  Riesgo 7: `ErrorContent` usa `EstadoPantalla` (título "No se pudo guardar la foto", mensaje, botón "Volver al encuadre"
  que sin permiso regresa a la pantalla de permiso); sin cambios.
- Implementado: `CropRegion.Completo` (dominio, centinela `viewAspectRatio = 0`) resuelto en `CapturedPhotoProcessor` con las
  dimensiones ya orientadas; `LecturaImagen.kt` (lectura acotada en IO, mensajes); `EscanearScreen` (launcher
  `PickVisualMedia`, botón en `PermisoCamaraContent`); `CameraEncuadre` (`BotonGaleria`, `onElegirGaleria`);
  `EscanearViewModel.onImagenElegida`/`onImagenFallida`.
- Desviación respecto a la spec: en vez de cambiar `estadoDe` para ignorar el permiso con cualquier captura (rompía el test
  existente `withoutPermissionStateStaysPending`), `EntradaCaptura` lleva `deGaleria`; solo las entradas de galería
  ignoran el permiso. Así el test existente queda intacto y la cámara sin permiso sigue bloqueada. Por eso se añadió
  `onImagenFallida` (la spec reutilizaba `onCapturaFallida`).
- Tests existentes modificados: ninguno (el texto del `check` no lo asertaba ningún test). Tags nuevos aditivos:
  `captura_galeria`, `permiso_galeria`. Dependencia nueva: `androidx-test-espresso-intents` (solo androidTest de `:app`).
- Verificación (PowerShell, `ANDROID_SERIAL=emulator-5556`): `gradlew.bat assembleDebug testDebugUnitTest
  :app:installDebug :app:installDebugAndroidTest :framework:database:installDebugAndroidTest` BUILD SUCCESSFUL;
  `EscanearViewModelTest` 49 tests OK, `LecturaImagenTest` nuevo; `am instrument` database: OK 36; `am instrument -e notClass
  ...ExtraccionLocalRealTest,...ExtraccionNubeRealTest` de `com.robertomr.tally.test`: OK 52 (GaleriaFlujoTest 4). Primera
  pasada cortada por `keyDispatchingTimedOut` en `RangoPersonalizadoFlujoTest` (emulador lento); esa clase pasó sola (2/2) y
  en la pasada completa. Nunca `connected*` ni `pm clear`.
- Manual con el Photo Picker real (README en `docs/evidence/galeria-escaneo/`): EXIF 6 -> 720x1280 con OCR; 6000x4000 ->
  1280x853; PNG basura -> Error. Sin permiso: `force-stop` + `pm revoke` con la app parada (no en caliente); CAMERA vuelta a
  conceder al terminar. Imágenes y scan limpiados; font_scale 1.0.
- Docs: `captura-foto.md`, `captura-recorte-encuadre.md`, KDoc de `CameraEncuadre`, `CONTEXT.md`, `DESIGN.md`,
  `docs/build-brief.md`, `docs/architecture.md`, `docs/technical-discovery.md`. Manifest sin permisos nuevos.
- NO verificado: Continuar -> extracción -> guardado con una foto de galería (nube sin cuota, sin modelo local; mismo código
  que la cámara tras Revision); selector real sin permiso de cámara (cubierto por E2E con selector simulado); TalkBack; el
  tope de 32 MiB solo en JVM. El selector del emulador se comporta como multiselección (toca imagen + "Done").
- Status: `galeria-escaneo` = **passing**, pendiente de validator independiente.

## Session 066 - forma-pago validacion (2026-10-20, master)

- Validator independiente: **accept** (tests JVM de dominio, ai y gastos re-ejecutados; SQL de `MIGRATION_2_3`
  identico a `3.json` y al `defaultValue` de la entidad; ningun calculo de totales cambiado; regla del total del
  prompt sin editar; instrumentados apoyados en la evidencia de emulator-5556: database 31 OK, app 48 OK;
  migracion real v2 a v3 con 22 tickets conservados).
- Pendiente de seguimiento: motores reales sin ejecutar (nube por cuota de Gemini, local sin modelo): comprobar a
  mano los tickets 2, 3 y 4 con un ticket real en la Tab; 3 tests de `EscanearViewModel` para los handlers de
  pago; E2E de escaneo con pago; TalkBack.
- Status: `forma-pago` = **accepted**.

## Session 068 - galeria-escaneo validacion (2026-10-20, master)

- Validator independiente: **accept**, re-ejecutando en emulator-5556: `assembleDebug` y `testDebugUnitTest` OK
  (`EscanearViewModelTest` 49, `LecturaImagenTest` 5), `am instrument` de `:app` OK 52 y de `framework:database`
  OK 36. Manifest sin cambios, sin permisos de almacenamiento; diff de tests 100 % aditivo; OOM e imagen ilegible
  acaban en el estado Error; centinela de recorte resuelto con el aspecto ya rotado por EXIF.
- Desviacion aceptada: `EntradaCaptura` lleva `deGaleria` en lugar de cambiar `estadoDe`.
- Seguimiento: el E2E sin permiso se salta con `assumeTrue` si CAMERA ya esta concedida; flujo punta a punta
  (Continuar, extraccion, guardado) con foto de galeria sin probar (sin cuota ni modelo): probarlo a mano en la
  Tab; TalkBack; `GaleriaFlujoTest` deja un directorio vacio `Pictures/TallyTest`.
- Status: `galeria-escaneo` = **accepted**.

## Session 069 - gasto-manual implementacion (2026-10-20/21, master)

- Implementado (spec `docs/specs/gasto-manual.md`, defaults de la spec): `Ticket.motor: Motor? = null` +
  `esManual` (sin `Motor.MANUAL`, sin migración: Room v3 intacto, `MANUAL` en la columna `motor`, `rutaFoto = ""`;
  mapper tolerante `motorDesde`, ya no hay `Motor.valueOf`), `datosGastoManualInicial`, `ErrorGastoManual`,
  `GuardarGastoManual`; `NuevoGastoScreen/ViewModel/State` en `:feature:gastos`; `ChipManual` y `TarjetaSinFoto`
  en `:feature:common`; botón `gastos_anadir` en la cabecera de Gastos y `gastos_anadir_vacio` en el vacío;
  ruta `gasto/nuevo`, barra oculta y transición deslizante en `:app`.
- Añadido sobre la spec: aviso `Snackbar` "Gasto guardado (d/M/yyyy)" (tag `gasto_guardado_aviso`) para el
  riesgo del periodo no visible; se descarta al abrir Detalle/Nuevo gasto (tapaba el dock y bloqueaba un
  clic del E2E). El estado vacío de Gastos pasó a ser desplazable (a 360 dp con letra 2,0 los botones quedaban
  fuera de pantalla). Botones del vacío con alto mínimo 48 dp.
- Impacto de la nulabilidad: 77 usos de `Ticket(`; cero cambios en tests existentes (los `Motor.X` siguen
  compilando y `assertEquals` admite el nulo). Único uso de `Ticket.motor` en UI: el detalle.
- Verificación: ver `feature_list.json` (JVM, 56 E2E de `:app`, 40 de `:framework:database`, todo en
  emulator-5556). Capturas y README en `docs/evidence/gasto-manual/`.
- Observaciones: el diálogo de borrado de un manual dice "y su foto" (texto sin cambios por la spec); el
  `init.sh`/`testDebugUnitTest` no ejecuta los tests de `:domain:expenses` (JVM puro): hay que añadir
  `:domain:expenses:test`. Gastos manuales del mismo día se ordenan al final (00:00).
- NO verificado: tablet real, TalkBack, donut/resumen con un manual en pantalla (sin aserción propia).
- Status: `gasto-manual` = **accepted** (validator independiente: accept; 470 JVM, 56 E2E :app, 40 :framework:database en emulator-5556). Desviacion aceptada: Snackbar "Gasto guardado". Siguiente: `filtro-forma-pago`.

## Session 070 - filtro-forma-pago implementacion (2026-10-22, master)

- Implementado (spec `docs/specs/filtro-forma-pago.md`, decisiones 1-7): dominio `FiltroFormaPago` (TODOS/EFECTIVO/
  TARJETA con `admite`; NO_INDICADA solo con TODOS) y `FiltrarTicketsPorFormaPago`; `GastosViewModel` con
  `MutableStateFlow<FiltroFormaPago>` combinado con la seleccion de periodo, `GastosUiState.filtro` y
  `hayTicketsEnPeriodo`, `onSeleccionarFiltro`; el constructor pasa de `ObservarListadoDelPeriodo` a
  `ObservarTicketsDelPeriodo` + `FiltrarTicketsPorFormaPago` + `AgruparTicketsPorDia` (`ObservarListadoDelPeriodo`
  intacto, lo sigue usando quien lo usaba). UI: `FiltroFormaPagoSelector` (FilterChips en `FlowRow`, >= 48 dp,
  check en el activo) en `:feature:common`, bajo el `PeriodoSelector`; vacio por filtro `listado_vacio_filtro`
  con "Ver todos" (`filtro_vaciar`). Sin cambios de Room, modulos, dependencias ni `build-logic/`.
- `TallyApp.navigateTo` usa `popUpTo(start){saveState}` + `restoreState`: el ViewModel de la entrada Gastos se
  conserva en la barra inferior (verificado por E2E: Resumen y vuelta mantiene el chip) y por detalle/Nuevo gasto.
- Verificacion: `gradlew.bat assembleDebug testDebugUnitTest :domain:expenses:test` verde (FiltroFormaPagoTest 3,
  FiltrarTicketsPorFormaPagoTest 5, GastosViewModelTest 12 = 7 + 5 nuevos). E2E en emulator-5556 (lanzado desde
  `Pixel_10_Pro -port 5556`; Tab fisica no tocada): `FiltroFormaPagoFlujoTest` 4 + `FiltroFormaPagoVacioTest` 3 y
  suite `:app` completa sin los *RealTest `OK (63 tests)`. Evidencia en `docs/evidence/filtro-forma-pago/README.md`.
- NO verificado: lista con tickets Efectivo/Tarjeta en captura (el emulador solo tiene residuales sin forma de
  pago; esa parte la cubre el E2E con semilla propia), vacio por filtro con la UI real (se prueba con estado fijo),
  rotacion en dispositivo (por ser ViewModel; no se roto), tablet fisica, TalkBack, `:framework:database` (sin
  cambios de BD).
- Status: `filtro-forma-pago` = **accepted** (validator independiente: accept; 63 E2E :app en emulator-5556) (listo para validator). Siguiente: `mejoras-visuales-3`.

## Session 071 - mejoras-visuales-3 implementacion (2026-10-22, master)

- Implementado (spec `docs/specs/mejoras-visuales-3.md`, W01-W09; W10/W11 solo verificados): `EstadoPantallaDesplazable`
  en `common/ComponentesSeccion.kt` (usada por los dos vacios de Gastos, el permiso de camara y el error de captura);
  borde de 2 dp en `onSecondaryContainer` del segmento activo del selector de periodo en modo dos filas (W01) y
  `selectableGroup()` en `FiltroFormaPagoSelector` (W05); fallo de Procesado desplazable con botones apilados
  (Reintentar primero) con `fontScale` > 1,15 y etiqueta de proceso centrada con `PADDING_PANTALLA` (W02);
  micro-metricas de `TarjetaTotal` apiladas con `fontScale` > 1,5 (W03); `DockEdicion(textoGuardar,
  mostrarTotalRecalculado)` y Nuevo gasto con "Guardar gasto" y sin "Total recalculado" si no hay lineas (W04);
  vacios de Gastos con selectores dentro del scroll con `fontScale` > 1,15 (W06); `textoConfirmarBorrado`
  (gasto manual sin "y su foto", W07); botones de permiso/error/"no existe" con `heightIn(min = 48.dp)` y a ancho
  completo (W08/W09). W10 (chips de forma de pago) y W11 (boton Galeria): sin defecto visible en las capturas
  revisadas del boton Galeria a 2,0; se dejan intactos (los chips de pago no se capturaron con datos reales:
  no hay tickets con forma de pago en el emulador, queda sin verificar visualmente).
- Sin cambios de dominio, Room, modulos, dependencias ni `build-logic/`; ningun tag cambiado.
- Tests nuevos (aditivos): `TextoConfirmarBorradoTest` (3, JVM), `FuentesGrandesFlujoTest` (6) y
  `NuevoGastoDockFlujoTest` (1) en `:app` androidTest.
- Verificacion: `gradlew.bat assembleDebug testDebugUnitTest :domain:expenses:test` verde (485 tests, 0 fallos).
  Instrumentados en emulator-5556: clases nuevas `OK (7 tests)`; aisladas `ListadoGastosVacioTest` 3,
  `FiltroFormaPago*Test` 7, `ProcesadoFlujoTest` 3, `RevisionDatosFlujoTest` 3, todas OK. Suite completa
  (sin *RealTest): 70 tests, 1 fallo: `GastoManualFlujoTest.altaDetalleEdicionYBorradoDeUnGastoManual` (espera
  `detalle_ticket_<id>` tras pulsar el ticket, linea 145). **Reproducido identico sobre el arbol sin cambios**
  (git stash, build e instalacion del baseline): es previo a esta feature; causa sin diagnosticar.
  El build de androidTest dio OOM de D8 con paralelismo por defecto; se resolvio con `--max-workers=2`.
- Capturas antes/despues en `docs/evidence/mejoras-visuales-3/` (README con lectura honesta). Emulador restaurado
  (`wm size`/`density` reset, font_scale 1.0, rotacion intacta, permiso de camara re-concedido).
  No se instalo nada en la Tab fisica ni se leyo ningun token.
- NO verificado: revision visual humana de las imagenes (solo las mire yo), TalkBack, dialogo "Eliminar modelo",
  `captura_error` real, transiciones, motores reales, tablet fisica, camara real de papel, dialogo de borrado de
  un manual en pantalla (solo JVM), borde W01 sin test automatico (captura + codigo).
- Status: `mejoras-visuales-3` = **accepted** (validator independiente: accept; JVM 486 OK, E2E 70 OK en emulator-5556; flaky de GastoManualFlujoTest = tickets basura en la BD del emulador). Seguimiento: limpieza en @After de las pruebas que dejan tickets vacios. Siguiente: `seed-demo` (al final).


## Session 072 - gasto-manual-2 implementacion (2026-10-22, master)

- Implementado (spec `docs/specs/gasto-manual-2.md`, A, B y C; C no se complico y no se difirio):
  - A cambio derivado: `DatosRevision.conPagoCoherente()` (solo Efectivo; `cambio = entregado - total` a centimos, `null`
    si no cubre el total, nunca negativo; "solo cambio" extraido infiere `entregado = total + cambio`); retirados
    `conCambioTexto` y `onCambioEditado` (3 ViewModels, 3 pantallas, `FormularioTicketEditable`); `revision_cambio` es
    `readOnly` con "Se calcula: entregado - total"; aviso `revision_aviso_pago` con el faltante; `normalizarPago` recalcula
    el cambio ignorando el extraido que contradiga (con total <= 0 conserva lo extraido); `normalizada()` re-sincroniza el
    total si es automatico.
  - B total unico: `DatosRevision.totalAutomatico` (no se persiste, sin Room), `totalDeLineas`, `conModoTotalInicial`
    (B3), `conTotalSincronizado` (B4, privado), `conTotalAutomatico` (B5) y `onTotalAutomaticoElegido` en los 3
    ViewModels; `conTotalTexto` devuelve `null` en automatico; `hayCambios` del Detalle ignora solo el flag (B8). UI: un
    unico "Total" (`BloqueTotal`) con "Suma de las lineas - N articulos" + "Editar total a mano", o editable con aviso de
    diferencia + "Usar la suma de las lineas"; ayuda "Escribe un concepto para que cuente"; `DockEdicion` sin
    `lineas`/`mostrarTotalRecalculado` y sin bloque "Total recalculado". Nuevo gasto: requisito "Anade el precio de al menos
    un producto" con total automatico y suma 0.
  - C boton: `FilledTonalButton` pildora "Anadir gasto" (icono + texto, tag `gastos_anadir`) en la cabecera de Gastos con
    `fontScale` <= 1,15 y en fila propia a ancho completo con letra mayor; `gastos_anadir_vacio` intacto.
- Sin cambios de Room/migracion, modulos, dependencias ni `build-logic/`. Tags: solo se retira `revision_total_recalculado`;
  nuevos `revision_total_manual`, `revision_total_usar_suma`, `revision_aviso_total`.
- Tests nuevos: `TotalAutomaticoTest` (18), `PagoTest` (+10), VM de Nuevo gasto (+4), Detalle (+4 y +2 en PagoTest),
  Escanear (+3); E2E `TotalCambioAutomaticosFlujoTest` (3) y `AnadirGastoBotonFlujoTest` (4).
- Tests existentes cambiados (por la spec, sin debilitar mas de lo que exige el contrato): `PagoTest.cambioEsEditableTrasElCalculo`
  -> `cambioNoEsEditableSeDerivaSiempre...` y `textoInvalidoSeIgnoraYBlancoLimpia` (sin `conCambioTexto`; blanco limpia entregado y
  cambio); `TicketDetalleViewModelPagoTest.textoInvalidoDeEntregadoOCambioSeIgnora` -> solo entregado; `FormaPagoFlujoTest.entregadoCalcula...`
  (entregado 5 -> cambio vacio + aviso con faltante + guarda cambio null, y `revision_cambio` sin `SetText`); `RevisionDatosFlujoTest`
  (`revision_total_recalculado` -> `revision_total`, final "4,50 automatico" en vez de "0,00"); `NuevoGastoDockFlujoTest` y dos de
  `FuentesGrandesFlujoTest` reescritos para el dock de dos botones. FUERA de la lista de la spec:
  `EscanearViewModelTest.extraccionVaciaAnadirLineasAManoYGuardarPersisteLasIntroducidas` ahora espera total 12,50 (antes 0,0): con
  total 0 la primera linea pasa a automatico (B4a), consecuencia directa del contrato.
- Verificacion: `gradlew.bat assembleDebug testDebugUnitTest :domain:expenses:test :test:unit:test` verde, 527 tests, 0 fallos.
  Instrumentados solo en emulator-5556, por paquetes (`am instrument -e package`, sin *RealTest): captura 13, diseno 6, gastos 37,
  navigation 3, resumen 11 OK; ajustes 8 con 1 fallo (`AjustesModeloFlujoTest...NoDescargado`, `token_estado` fuera de pantalla por el
  `wm size 1080x2400` que deje puesto), OK 3/3 aislada con el tamano por defecto. Una suite completa previa cayo con
  `System has crashed` (emulador sin activity manager); `adb reboot` sin wipe y repeticion por paquetes.
- Entorno: los E2E dejan tickets vacios en la BD del emulador. Identificado: `RevisionDatosFlujoTest` (guarda por la UI sin @After; ahora
  ademas guarda 4,50). Se borraron por la UI los de hoy antes de la suite; `GastoManualFlujoTest` paso. Emulador restaurado
  (`wm size`/`density` reset, font_scale 1.0, rotacion 0). Nada en la Tab fisica, ningun token leido.
- Capturas antes/despues y README en `docs/evidence/gasto-manual-2/`. Las `despues-form-*` salen de una prueba temporal ya borrada
  (formulario con reductores reales), no de la pantalla Nuevo gasto completa.
- NO verificado: TalkBack, ticket escaneado real con IA, tablet fisica, formulario completo en tablet, "antes" a fontScale 1,3 y tablet,
  revision visual humana de las capturas.
- Status: `gasto-manual-2` = **accepted** (validator independiente: accept; JVM 527 OK; E2E por paquetes en emulator-5556 OK; GaleriaFlujoTest flaky de entorno, aislado OK). Notas: total del papel que difiere <=0,01 de la suma se abre automatico (B3); en automatico con conceptos vacios el total pasa a 0 (aviso visible). Siguiente: `seed-demo` (al final).

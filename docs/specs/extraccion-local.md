# Feature Implementation Spec: Extracción local con Gemma 3n (TextEngine)

## Source Feature

- `id`: extraccion-local
- `area`: ia
- `title`: Extracción local con Gemma 3n (TextEngine)
- `user_visible_behavior`: El mismo contrato TextEngine en el dispositivo: texto crudo → TicketExtraido validado con Gemma 3n/MediaPipe, sin red, con el motor registrado.
- `depends_on`: ["spike-gemma-mediapipe", "ocr-mlkit"] (ambas `accepted`; `descarga-modelo` también `accepted` y aporta el fichero)
- `status`: not_started (planificada 2026-10-18)
- `verification` (fuente, con desviación): (1) los textos crudos reales producen extracciones válidas en local (Tab S9+) — **4 textos, no 5** (desviación ya formalizada en `ocr-mlkit`/`extraccion-nube`: el usuario aportó 4 tickets; el orquestador debe alinear el texto de `feature_list.json`); (2) parser/validador compartido con la vía nube (unit tests); (3) latencia medida y registrada en `PROGRESS.md`.
- `notes` (fuente): comparativa honesta, mismo input para ambos motores (ADR 0002); `GemmaLocalEngine` internal en `framework/ai`.

## Goal

Implementar `GemmaLocalEngine` (`internal`, `:framework:ai`): un segundo `TextEngine` que convierte el texto crudo en un `TicketExtraido` validado con **Gemma 3n E2B (`gemma-3n-E2B-it-int4.litertlm`) vía MediaPipe `tasks-genai` 0.10.35, sin red**, devolviendo `motor = Motor.LOCAL`. Reutiliza sin cambios el contrato `TextEngine`, el `TicketExtraidoParser` compartido y el prompt/schema de la nube. Resuelve el requisito derivado de R1: **parada de generación** (sin ella degenera hasta `maxTokens`: 53 s medidos en el spike) y **re-medición de latencia con prompts de extracción reales**.

## Non-Goals

- `EngineRouter`, selección Auto/Local/Nube, capacidad del dispositivo, badge (`router-automatico`); **la app sigue usando la nube** por defecto (binding `TextEngine` sin calificar intacto; `ExtraerTicket` y las pantallas no cambian).
- `reintento-otro-motor`, fallback automático silencioso (ADR 0001), UI nueva, ViewModels, Room, descarga del modelo (ya hecha).
- Visión/multimodal de Gemma (solo texto), fine-tuning, caché de modelo cargado entre extracciones (ver D2).
- Cambios en `TicketExtraidoParser` salvo que la evidencia real lo exija (ver R-LOC4); cambios en `AGENTS.md`, ADRs, `init.sh`, `build-logic/`; **módulos Gradle nuevos (ninguno necesario)**; Retrofit/OkHttp; claves en el APK.
- Borrar el spike (`GemmaMediaPipeSpike` y su test se quedan como evidencia aceptada de R1).

## Job Story

When no tengo red (o prefiero que el ticket no salga del dispositivo) y ya descargué el modelo,
I want que Gemma 3n local convierta el texto crudo en un `TicketExtraido` validado con el mismo contrato que la nube,
so I can revisar y guardar el ticket con el motor Local registrado.

## Users And Permissions

Usuario único. Sin permisos nuevos. El texto del ticket **no sale del dispositivo** (ventaja de privacidad frente a Nube). El modelo ya está en `getExternalFilesDir(null)/models/` (lo expone `ModelDownloader.rutaModelo()`); no se descarga aquí.

## Acceptance Scenarios

### Scenario 1: los 4 textos crudos reales producen extracciones válidas en local (verificación 1)
Given el modelo descargado en el dispositivo y los 4 `texto_crudo_1..4.txt` (assets de `:app/src/androidTest`)
When `GemmaLocalEngine.extract` procesa cada uno
Then la salida pasa el `TicketExtraidoParser` compartido (schema + enum de 6), `motor == Motor.LOCAL`, la carga y la inferencia corren fuera del hilo principal y no hay crash. Los totales se contrastan con el papel (35,30 / 3,50 / 13,90 / 7,91) y se **registran** (acierto/fallo por ticket) sin ser gate duro (D5).

### Scenario 2: la generación se detiene sola (requisito derivado de R1)
Given una respuesta que contiene un objeto JSON completo
When el flujo parcial del modelo cierra el `}` de nivel superior (o emite `<end_of_turn>`)
Then el motor cancela la generación y no consume hasta `maxTokens`; el detector es puro y está probado en JVM (llaves dentro de strings, escapes, texto previo, truncado sin cierre).

### Scenario 3: parser/validador compartido (verificación 2)
Given respuestas del modelo local malformadas (JSON roto, sin objeto, categoría fuera de enum, truncada por límite de tokens)
When pasan por `GemmaLocalEngine`
Then lanzan `InvalidExtractionException` (el mismo parser que la nube, sin copia), nunca un resultado inventado ni reintento silencioso. Los tests de `TicketExtraidoParserTest` ya cubren el parser; aquí se prueba el cableado (JVM con runner falso).

### Scenario 4: modelo ausente y errores
Given `ModelDownloader.rutaModelo() == null` (no descargado)
When se llama `extract`
Then lanza `ModeloLocalNoDisponibleException` (mensaje claro "descarga el modelo en Ajustes") **sin tocar MediaPipe**; una `CancellationException` se propaga siempre (se aborta la generación nativa y se libera el modelo antes de relanzar). Texto blank lo resuelve `ExtraerTicket` (no cambia).

### Scenario 5: latencia medida (verificación 3)
Given las 4 extracciones reales en la Tab S9+
When se miden carga, prefill+generación y tokens
Then se registran en `PROGRESS.md` y en `docs/evidence/extraccion-local/` los ms crudos por ticket (`loadMillis`, `generationMillis`, `promptTokens`, `responseTokens`), contrastados con los 53 s del spike y la estimación de R1 (~11–22 s + prefill).

### Scenario 6: arquitectura y no regresión
Given la implementación
Then `domain/expenses` sigue sin imports de Android/MediaPipe/Firebase; `GemmaLocalEngine` y el runner son `internal` en `framework/ai`; un solo `di.kt` (`AiModule` ampliado); 0 versiones literales; sin claves ni `BuildConfig`; esquema Room sin cambios; `init.sh` sin diff; gates JVM y E2E existentes en verde.

## Repository Research

### Files Inspected
`AGENTS.md`; `PROGRESS.md` (sesiones 005–007, 030–031); `feature_list.json`; `docs/specs/{spike-gemma-mediapipe,extraccion-nube,descarga-modelo}.md`; `docs/adr/0001`; `docs/risks-and-open-questions.md` (R1: stop token + re-medición); `framework/ai/.../spike/GemmaMediaPipeSpike.kt` y su test; `.../extraccion/{GeminiCloudEngine,TicketExtractionPrompt}.kt`; `.../modelo/{ModelPaths,WorkManagerModelDownloader}.kt`; `.../di/di.kt`; `framework/ai/build.gradle.kts`; `domain/expenses/.../{data/TextEngine,data/ModelDownloader,entities/TicketExtraidoParser,entities/EstadoModelo,usecases/ExtraerTicket}.kt`; `app/.../di/TextEngineEntryPoint.kt`; `app/src/androidTest/.../ExtraccionNubeRealTest.kt`; `gradle/libs.versions.toml` (`mediapipeGenai = 0.10.35`). No inspeccionado: fuentes del AAR de `tasks-genai` (las firmas exactas se confirman al implementar).

### Existing Patterns To Follow
- Contrato `TextEngine` + parser compartido ya en dominio (de `extraccion-nube`); `GeminiCloudEngine` es el molde (lazy, `Dispatchers.IO`, `CancellationException` jamás tragada, `TicketExtraidoParser.parse(texto, motor)`).
- Spike: `LlmInference.createFromOptions` + `LlmInferenceSession`, `suspendCancellableCoroutine` con `cancelGenerateResponseAsync`, `ranOnMainThread`, métricas con `SystemClock`.
- Test real en `:app/src/androidTest` vía `EntryPointAccessors` (R-NUBE6: sin `HiltTestRunner`) con `assumeTrue` si falta el requisito ambiental.
- Tests JVM de `framework/ai` solo con `junit` + `coroutines-test` (sin dependencia de `:test:unit`; dobles locales al test).

### Current Gaps
- No existe `GemmaLocalEngine`; el spike usa `maxTokens = 512` (suma de entrada + salida: insuficiente para el prompt de extracción, ~900–1.300 tokens) y no corta la generación.
- El modelo vive en el directorio del **paquete de la app** (`com.robertomr.tally`): un test de `:framework:ai` corre con otro `targetContext` (`...framework.ai.test`) y no puede leerlo; por eso el test real va en `:app`.
- `connectedDebugAndroidTest` **desinstala la app y borra el modelo de 3,4 GB** (PROGRESS sesión 031): hay que ejecutar el test real con `adb shell am instrument` (D6).

## Technical Approach

1. **Dominio** (mínimo): `ModeloLocalNoDisponibleException : IllegalStateException` en `entities/` (junto a `InvalidExtractionException`). `TextEngine`, `ExtraerTicket` y el parser no cambian.
2. **Costura testable** (`framework/ai/extraccion/`, `internal`): `interface LocalLlm { suspend fun generate(modelPath: String, prompt: String, maxTokens: Int, onPartial: (String) -> Boolean): LlmRun }` (`onPartial` recibe el texto acumulado y devuelve `true` para detener) con `LlmRun(text, loadMillis, generationMillis, promptTokens, responseTokens, ranOnMainThread)`. `MediaPipeLocalLlm` es la implementación real (Context, `Dispatchers.IO`), derivada del spike.
3. **`GemmaLocalEngine(modelDownloader: ModelDownloader, llm: LocalLlm)`**: `motor = Motor.LOCAL`; `extract`: ruta nula → `ModeloLocalNoDisponibleException`; construye `TicketExtractionPrompt.buildLocalPrompt(texto)`; `llm.generate(..., onPartial = detector)`; recorta la salida al primer objeto JSON completo / antes de `<end_of_turn>`; `TicketExtraidoParser.parse(texto, Motor.LOCAL)`. Un `Mutex` evita dos cargas simultáneas de 3,6 GB. Sin `runCatching`; sin reintento.
4. **Parada** (`JsonObjectStreamDetector`, puro): acumula chunks, recorre caracteres con estado (profundidad de llaves, dentro de string, escape) y marca "completo" al cerrar el `{` de nivel superior; también marca completo ante `<end_of_turn>`/`<eos>`. Implementación MediaPipe: `session.generateResponseAsync(ProgressListener)`; al devolver el detector `true` se llama `cancelGenerateResponseAsync()`. (Confirmar contra el AAR si el listener entrega trozos o acumulado; acumular con `StringBuilder` en el primer caso.)
5. **Parámetros** (D3): `maxTokens = 2048` (suma entrada+salida); sesión con `topK = 1`, `temperature = 0.2f`, semilla fija (extracción determinista). Antes de generar, si `sizeInTokens(prompt) > maxTokens - 512` → `InvalidExtractionException("ticket demasiado largo para el motor local")`. Salida sin JSON completo al llegar a `maxTokens` → `InvalidExtractionException` vía parser.
6. **Prompt** (D4): `buildLocalPrompt` = `buildPrompt` (mismo schema, enum y regla de total con IVA que la nube → comparativa honesta, ADR 0002) + cierre "Devuelve únicamente el objeto JSON, sin texto antes ni después ni comentarios `//`". Nada de `responseSchema` (no existe en MediaPipe). Si la evidencia real muestra que hace falta la plantilla de chat de Gemma (`<start_of_turn>user … <end_of_turn>\n<start_of_turn>model\n`), se añade solo en la rama local y se registra (R-LOC3).
7. **Ciclo de vida** (D2): cargar por extracción y cerrar en `finally` (≈6,5 s medidos). Si el hilo cancela: `cancelGenerateResponseAsync`, esperar a que el futuro se resuelva (bloque `NonCancellable`) y solo entonces `session.close()`/`inference.close()`. Todo en `Dispatchers.IO`; nunca main.
8. **DI** (D1): en `AiModule`, `@Provides @Singleton @Named("local") fun provideLocalTextEngine(@ApplicationContext ctx, downloader: ModelDownloader): TextEngine = GemmaLocalEngine(downloader, MediaPipeLocalLlm(ctx))`. El `TextEngine` sin calificar sigue siendo `GeminiCloudEngine`. `TextEngineEntryPoint` gana `@Named("local") fun textEngineLocal(): TextEngine`.

## Expected File Changes

- `domain/expenses/.../entities/ModeloLocalNoDisponibleException.kt` — create.
- `framework/ai/.../extraccion/GemmaLocalEngine.kt`, `LocalLlm.kt` (interfaz + `LlmRun`), `MediaPipeLocalLlm.kt`, `JsonObjectStreamDetector.kt` — create (todo `internal`).
- `framework/ai/.../extraccion/TicketExtractionPrompt.kt` — modify (+ `buildLocalPrompt`; `buildPrompt` y `responseSchema` intactos).
- `framework/ai/.../di/di.kt` — modify (+ provider `@Named("local")`; no crear otro `di.kt`).
- `app/src/main/java/com/robertomr/tally/di/TextEngineEntryPoint.kt` — modify (+ accesor local).
- `framework/ai/src/test/java/.../extraccion/{GemmaLocalEngineTest,JsonObjectStreamDetectorTest}.kt` — create; `TicketExtractionPromptTest.kt` — modify (+ local: mismo schema/enum/regla IVA que la nube, cierre "solo JSON").
- `app/src/androidTest/java/com/robertomr/tally/extraccion/ExtraccionLocalRealTest.kt` — create (reutiliza los assets `texto_crudo_1..4.txt` ya existentes).
- `docs/evidence/extraccion-local/` — create (`extraccion_1..4.txt` con el logcat exacto, `latencias.md`, tabla de contraste con papel, comparativa con la nube).
- `docs/architecture.md` (sección TextEngine: añadir motor local y `@Named("local")`), `docs/risks-and-open-questions.md` (R1: latencias reales de extracción, parada resuelta), `docs/technical-discovery.md` (solo si cambian parámetros/plantilla) — update.
- `PROGRESS.md`, `feature_list.json` — los actualiza el orquestador/implementer al verificar (no este spec).
- No tocar: `gradle/libs.versions.toml`, `*.gradle.kts` (MediaPipe y Hilt ya están), `GemmaMediaPipeSpike*`, parser, `init.sh`, `AGENTS.md`.

## Visual Design Impact
UI involved: no. Sin artefactos de diseño. El badge/indicador es de `router-automatico`.

## Durable Documentation Impact
- `ARCHITECTURE.md`/`CONSTRAINTS.md` raíz: not needed (no existen; equivalente `docs/architecture.md`).
- `AGENTS.md`: not needed (la capa de IA ya describe `GemmaLocalEngine`).
- `docs/architecture.md`: update. `docs/risks-and-open-questions.md`: update (R1 y comparativa). `docs/technical-discovery.md`: update condicional. `docs/domain-model.md`: not needed.

## Implementation Plan
1. Dominio + detector + `LocalLlm` + `GemmaLocalEngine` con tests JVM (todo verificable sin dispositivo).
2. `MediaPipeLocalLlm` + DI + EntryPoint; `assembleDebug`.
3. Test real en `:app`; humo en `emulator-5554` (best-effort, D7).
4. Verificación física en Tab S9+, evidencias, docs, regresión (la suite `connected*` al final).

## Implementation Tasks
- [ ] T1 `ModeloLocalNoDisponibleException` + `JsonObjectStreamDetector` + tests (anidamiento, llaves en strings, escapes `\"`, prosa previa, `<end_of_turn>`, sin cierre). *Done: JUnit XML verde.*
- [ ] T2 `LocalLlm`/`LlmRun` + `GemmaLocalEngine` + `GemmaLocalEngineTest` con `LocalLlm` falso: modelo ausente (sin invocar el runner), respuesta válida → `Motor.LOCAL`, JSON malformado / categoría fuera de enum / truncada → `InvalidExtractionException`, `CancellationException` propagada, prompt local enviado con el texto, serialización por `Mutex`. *Done: tests verdes.*
- [ ] T3 `buildLocalPrompt` + ampliar `TicketExtractionPromptTest`. *Done: `:framework:ai:testDebugUnitTest` verde.*
- [ ] T4 `MediaPipeLocalLlm` (IO, métricas, `ProgressListener` + cancelación, cierre ordenado) + `AiModule` + `TextEngineEntryPoint`. *Done: `assembleDebug` verde; revisión: 0 imports de MediaPipe fuera de `framework/ai`.*
- [ ] T5 `ExtraccionLocalRealTest` en `:app`: `assumeTrue` ante `ModeloLocalNoDisponibleException`; por ticket: parser OK, `motor == LOCAL`, categoría de la enum, `Log.i("ExtraccionLocal", ...)` con extracción + métricas, total comparado con tolerancia 0,01 y **registrado** (no assert duro, D5); assert de `ranOnMainThread == false` vía log/ruta del runner. *Done: compila y corre; sin modelo se salta limpio.*
- [ ] T6 Humo en `emulator-5554` (modelo ya presente): si `LlmInference` no carga (ABI/RAM), registrar el error real y pasar a T7 (D7).
- [ ] T7 **Verificación física Tab S9+**: modelo en `…/Android/data/com.robertomr.tally/files/models/` (descargar desde Ajustes o `adb push`); `gradlew installDebug installDebugAndroidTest` y `adb shell am instrument -w -e class com.robertomr.tally.extraccion.ExtraccionLocalRealTest <paquete-test>/androidx.test.runner.AndroidJUnitRunner` (nombre exacto con `pm list instrumentation`; `ANDROID_SERIAL` o `adb -s`). Exportar los 4 logs a `docs/evidence/extraccion-local/`, rellenar `latencias.md`, contraste humano de totales y comparativa con la nube.
- [ ] T8 Si el ticket 2 (trampa base/IVA) falla (3,36 vs 3,50) o el JSON sale con comentarios/plantilla incorrecta: iterar el prompt **local** (máx. 2 rondas, registradas) o, si es de contenedor, endurecer el parser compartido con test nuevo (R-LOC4). Documentar el resultado honesto aunque no mejore.
- [ ] T9 Regresión: `assembleDebug testDebugUnitTest :test:unit:test --rerun :domain:expenses:test --rerun :framework:ai:testDebugUnitTest`; búsquedas (0 versiones literales; 0 `AIza|api_key|buildConfigField`; 0 imports Android/MediaPipe en `domain/**`); `schemas/` e `init.sh` sin diff; **al final** `:app:connectedDebugAndroidTest` (desinstala y borra el modelo: solo después de capturar toda la evidencia local).
- [ ] T10 Docs durables, evidencia en `feature_list.json` y sesión en `PROGRESS.md` (latencias reales).

## Verification Plan
- JVM (Windows `gradlew.bat`): los gates de T9; JUnit XML en `framework/ai/build/test-results/testDebugUnitTest/`.
- Dispositivo: T6 (emulador, humo) y T7 (Tab S9+, gate de la verificación 1 y 3). Cada log debe llevar `ranOnMainThread=false`, `loadMillis`, `generationMillis`, tokens y el resultado parseado.
- Criterio de éxito de la verificación 1: 4/4 pasan el parser compartido con `motor = LOCAL`, sin crash/ANR. Totales: se reporta aciertos/4; si ticket 2 devuelve la base 3,36, queda anotado como hallazgo de calidad (R3) con decisión sobre plan B de R1.
- E2E persistente: la feature no cambia UI ni flujos (app sigue en nube); la cobertura nueva es el test real instrumentado y los unit tests; se justifica no añadir E2E de UI.

## Evidence To Capture
JUnit XML (detector, engine, prompt); logs `ExtraccionLocal` ×4 y `latencias.md` (ms de carga/generación, tokens, tok/s, dispositivo, Android, versión `tasks-genai`, modelo); contraste con papel y con `docs/evidence/extraccion-nube/`; salida de las búsquedas de seguridad/arquitectura; texto del prompt final y parámetros (`maxTokens`, `topK`, `temperature`); registro de rondas de T8 si las hubo.

## Decisiones con DEFAULT (no bloqueantes)
- **D1** Binding local calificado `@Named("local")`; el sin calificar sigue siendo nube hasta `router-automatico` (evita cambiar procesado-ui y su E2E). Alternativa: reemplazar el binding por un router (descartada, fuera de alcance).
- **D2** Carga por extracción, sin caché (simple, memoria predecible); caché con descarga por inactividad queda para `router-automatico` si la latencia de 6,5 s molesta.
- **D3** `maxTokens=2048`, `topK=1`, `temperature=0.2`, semilla fija; el margen de salida (512) y la política de truncado están en el engine.
- **D4** Mismo prompt/schema que la nube + cierre local; la plantilla de chat solo si hace falta.
- **D5** Totales contra papel = métrica registrada, no gate duro (la verificación pide "extracciones válidas"; una asimetría de calidad Local < Nube es un hallazgo legítimo de la comparativa honesta).
- **D6** Test real en `:app` + ejecución con `am instrument` para no borrar el modelo.
- **D7** Tab S9+ = dispositivo normativo (verificación fuente); el emulador es humo opcional porque el spike dictaminó que no es dispositivo válido para MediaPipe (ABI/rendimiento no confirmados).
- **D8** Spike intacto; sin reintento silencioso ante JSON inválido (ADR 0001).

**Decisión bloqueante: ninguna.** No se crea módulo Gradle, no se añaden dependencias ni versiones. Prerrequisito humano no bloqueante del código: tener el modelo en la Tab S9+ y poder conectarla por ADB.

## Riesgos y preguntas abiertas
- **R-LOC1 Latencia**: spike = 9,2 tok/s; JSON de 150–400 tokens ⇒ ~16–45 s + prefill de ~1.000 tokens (a medir). Sin presupuesto objetivo fijado: se reporta; si >60 s por ticket, evaluar plan B de R1 (E4B no, más pequeño/otro modelo) en `risks`.
- **R-LOC2 Memoria**: ~3,6 GB residentes durante la extracción; con 12 GB OK en la Tab; el realme GT 2 es territorio de `router-automatico`.
- **R-LOC3 Plantilla de chat**: no se sabe si MediaPipe/LiteRT-LM aplica la plantilla del `.litertlm` a un prompt crudo (el spike respondió bien en crudo y emitió `<end_of_turn>`). Se mide en T7.
- **R-LOC4 Calidad del JSON**: sin structured output, Gemma puede copiar los `// comentarios` del schema del prompt o envolver en fences. Los fences ya los tolera el parser; los comentarios no (se evita por prompt; endurecer el parser solo con evidencia y test).
- **R-LOC5 API del AAR**: firmas exactas de `generateResponseAsync(ProgressListener)` y de las opciones de sesión (`setTopK`, `setTemperature`, `setRandomSeed`) se confirman al compilar; el contrato `TextEngine` no se ve afectado.
- **R-LOC6 Emulador/ABI**: posible `UnsatisfiedLinkError`/OOM en x86_64; no es fallo de la feature si ocurre (D7).
- **R-LOC7 Borrado del modelo** por `connected*` (ver T9/D6); `AjustesModeloFlujoTest` requiere app sin token (conocido).
- **Pregunta**: ¿se acepta D5 (totales como métrica y no gate)? DEFAULT: sí.

## Validator Checklist
- [ ] Alcance: solo motor local + DI calificada; la app sigue usando la nube; sin UI, router, reintentos ni módulos nuevos.
- [ ] Escenarios 1–6 con evidencia real (4 textos en la Tab S9+; sin resultados inventados).
- [ ] Parada de generación implementada y probada (detector JVM); latencia con prompt real registrada en `PROGRESS.md`.
- [ ] Parser compartido sin duplicar (misma clase que la nube); si se tocó, con tests nuevos.
- [ ] `domain` puro; `GemmaLocalEngine`/runner `internal`; un solo `di.kt`; carga en `Dispatchers.IO`; cierre ordenado tras cancelación.
- [ ] Sin claves, sin `BuildConfig`, 0 versiones literales, `init.sh`/`schemas` sin diff.
- [ ] Regresión JVM y E2E en verde; el modelo no se borró antes de capturar la evidencia.
- [ ] `feature_list.json` y `PROGRESS.md` actualizados con evidencia no inventada (verificación alineada a 4 textos).

# Feature Implementation Spec: Extracción en nube con Gemini (TextEngine)

## Source Feature

- `id`: extraccion-nube
- `area`: ia
- `title`: Extracción en nube con Gemini (TextEngine)
- `user_visible_behavior`: El texto crudo se convierte en TicketExtraido validado (comercio, fecha, total, líneas, categoría de la enum fija) con Gemini vía Firebase AI Logic, sin clave en el APK.
- `depends_on`: ["ocr-mlkit"] (accepted → satisfecha)
- `status`: not_started (a fecha de planificación, 2026-10-16)
- `source`: `feature_list.json`
- `verification` (fuente, con desviación registrada):
  1. Con los **4** textos crudos reales (desviación acordada 2026-10-16 respecto a los 5 planificados, misma desviación que `ocr-mlkit`: el usuario aportó 4 fotos), la salida valida el schema y la enum de categorías.
  2. Unit test del parser/validador con respuestas malformadas.
  3. Revisión: ninguna clave de API en el APK ni en BuildConfig.
- `notes` (fuente): Contrato TextEngine en domain/expenses/data (AGENTS.md). Prompt con schema y enum; ADR 0001 y 0002.

## Goal

Implementar la **etapa 2 del ADR 0002** para la vía nube: texto crudo →
`TicketExtraido` **validado** (comercio, fecha de emisión, total con IVA, líneas
y categoría de la enum fija de 6) con **Gemini vía Firebase AI Logic**
(`GeminiCloudEngine`, `internal` en `:framework:ai`), sin ninguna clave de API
en el APK ni en `BuildConfig`. Se crea el contrato `TextEngine` en dominio y el
**parser/validador compartido** que `extraccion-local` reutilizará tal cual
(mismo schema, mismas reglas, mismos tests). El motor usado queda **registrado
con la extracción** (`Motor.NUBE`) para el futuro badge/router.

## Non-Goals

- UI de ningún tipo: pantalla de procesado (mockup 06, `procesado-ui`), badge de
  motor, revisión/edición (`revision-datos`). "Continuar" sigue deshabilitado.
- ViewModels o cambios en `:feature:*`.
- `GemmaLocalEngine` / `extraccion-local` (solo se garantiza que reutilizará el
  mismo parser/validador), `EngineRouter`, `router-automatico` y
  `reintento-otro-motor` (sin fallback automático silencioso, ADR 0001).
- Persistencia en Room ni uso de `TicketRepository` (`revision-datos` guardará).
- Log de extracciones (`log-extracciones`): solo se deja el punto de extensión
  (el caso de uso `ExtraerTicket` es el que se envolverá).
- Escrituras en `AGENTS.md`, ADRs, `init.sh`, `build-logic/` y módulos Gradle
  nuevos.
- Cualquier clave/credencial inventada: **no se crea `google-services.json`**.
  Ver "Prerrequisito humano".

## Prerrequisito humano (Firebase AI Logic) — bloquea T7 y la verificación 1

Hoy **no existe ninguna configuración de Firebase en el repo** (0 resultados de
`google-services`/`firebase` en los `*.kts`). Para ejecutar el motor real hace
falta intervención humana:

1. Crear un proyecto Firebase (plan Spark: Firebase AI Logic con backend
   `googleAI` tiene free tier; alternativa `vertexAI` exige facturación —
   descartada por defecto).
2. Registrar la app Android con applicationId **`com.robertomr.tally`** (y la
   restricción de la API key por paquete + SHA-1 de debug).
3. Habilitar la API de Gemini en Firebase AI Logic.
4. Descargar `google-services.json` y dejarlo en **`app/google-services.json`**
   (fichero **gitignoreado por defecto**; lo aporta el humano, el agente no lo
   inventa).

Mientras ese fichero no exista, el build **debe seguir en verde** (plugin
`google-services` aplicado **condicionalmente**; ver T1/T5) y el test real se
**salta** con `assumeTrue` (patrón `MlKitOcrEngineTest`). T1–T6 son ejecutables
sin el prerrequisito; **T7 y la verificación 1 quedan bloqueadas hasta que el
usuario lo aporte**. Riesgo R2 (cuota/latencia Gemini) también se mide en T7.

## Job Story

When tengo el texto crudo del ticket (OCR ya hecho) y quiero los datos
estructurados sin teclear,
I want que un motor de extracción (aquí: Gemini en nube) convierta ese texto en
un `TicketExtraido` validado (con el total que pago, con IVA),
so I can revisar solo los detalles y guardar el ticket, sabiendo qué motor lo
extrajo.

## Users And Permissions

- Usuario único (sin cuentas; `docs/build-brief.md`). Con motor Nube el texto
  crudo del ticket **sale al servicio de Gemini** (implicación de privacidad ya
  asumida en ADR 0001/0002 y `docs/technical-discovery.md`; el motor Local no
  envía nada). Nada más cambia: sin permisos Android nuevos, sin cuentas.
- `google-services.json` es config pública de Firebase (no un secreto autoral):
  la regla "sin claves en el APK" se interpreta como **sin claves propias en
  código/BuildConfig** (ver Escenario 3).

## Acceptance Scenarios

### Scenario 1: los 4 textos crudos reales producen extracciones válidas (verificación 1)

Given los 4 textos crudos reales `docs/evidence/ocr-mlkit/texto_crudo_1..4.txt`
(copiados como assets del test) y Firebase AI Logic configurado
When se ejecuta la extracción real de cada uno con `GeminiCloudEngine`
(entorno: dispositivo con Google services + red)
Then cada respuesta pasa el parser/validador compartido: schema correcto y
`categoria` ∈ enum de 6 valores; las extracciones quedan como evidencia en
`docs/evidence/extraccion-nube/`.

### Scenario 2: el total es el total CON IVA, no la base (deuda R3)

Given el texto crudo del ticket 2 contiene "3,36 IVA: 0,14" (base + cuota) y
"TOTAL COMPRA 3,50"
When extrae el motor
Then `total` = **3.50** (impuestos incluidos), nunca 3.36 (base). El prompt
obliga a "Total (impuestos incl.) / TOTAL A PAGAR / TOTAL COMPRA"; los totales
de los 4 tickets se contrastan con el papel por el usuario (ticket 3 con
ambigüedad OCR 12,64 vs 13,90: el contraste humano decide y se registra).

### Scenario 3: ninguna clave de API en el APK ni en BuildConfig (verificación 3)

Given la feature implementada
When se revisa el código y el APK
Then no hay `buildConfigField` con claves, ni literales `AIza…`/`api_key` en
fuentes, ni claves autorales en recursos; el único material de config del APK
son los recursos estándar que inyecta el plugin `google-services` de Firebase
(mecanismo oficial "sin clave en el código"; `app/google-services.json` fuera de
control de versiones por defecto). `:domain:expenses` sigue sin imports de
Android/Firebase.

### Scenario 4: respuestas malformadas del modelo se rechazan (verificación 2)

Given el parser/validador compartido en dominio
When recibe respuestas malformadas (JSON roto, sin objeto, categoría fuera de
la enum o ausente, fecha ilegible, total negativo/no numérico, línea sin
concepto, cantidad < 1, precio negativo)
Then lanza `InvalidExtractionException` con motivo claro; y cuando recibe
respuestas válidas (incluidas con fences ```json y prosa alrededor, y números
con coma decimal española) devuelve el `TicketExtraido` normalizado. Unit tests
JVM en dominio (criterio 2).

### Scenario 5: política de errores y cancelación del motor

Given `ExtraerTicket` sobre un texto crudo
When el texto es vacío/blank
Then NO se invoca el motor (sin gastar cuota) y se devuelve un `TicketExtraido`
vacío con `motor` del motor disponible (vía manual posterior); cuando la red o
Gemini fallan, el error se propaga al llamador (sin resultados inventados) y
una `CancellationException` jamas se convierte en resultado.

### Scenario 6: arquitectura y motor registrado

Given la implementación
When se revisa la arquitectura
Then `TextEngine` está en `domain/expenses/data/` sin imports de Android ni
Firebase; `GeminiCloudEngine` es `internal` en `:framework:ai` (Firebase AI
Logic, `Dispatchers.IO`); un solo `di.kt` por módulo (`AiModule` ampliado, no
creado otro); `TicketExtraido.motor == Motor.NUBE`; sin módulos Gradle nuevos y
0 versiones literales fuera de `gradle/libs.versions.toml`.

### Scenario 7: regresión de lo aceptado

Given la feature implementada
When corren los gates y la revisión
Then `assembleDebug`/`testDebugUnitTest`/`:test:unit:test`/`:domain:expenses:test`
en verde, `NavigationTest` 3/3 y `CapturaFlujoTest` en verde (sin cambios de
UI: "Continuar" sigue deshabilitado), esquema Room sin cambios y `init.sh` sin
diffs.

## Repository Research

### Files Inspected

- `AGENTS.md` — capa de IA (`TextEngine` en `domain/expenses/data/`,
  `GeminiCloudEngine` en `framework/ai`, `EngineRouter` futuro), reglas de
  módulos, testing, "qué no hacer".
- `feature_list.json` — alcance/verificaciones de esta feature y continuidad
  (`extraccion-local` reutiliza el parser; `log-extracciones`,
  `router-automatico`, `reintento-otro-motor` consumen el contrato).
- `PROGRESS.md` — 6 features `accepted`; desviación 4 textos (sesión 016);
  criterio de dispositivos; finding Low de `runCatching`/`CancellationException`.
- `docs/adr/0001` (contrato `TextEngine`, motores `internal`, motor registrado,
  sin fallback silencioso) y `docs/adr/0002` (etapa 2 = texto crudo → datos
  estructurados; ambos motores consumen el mismo texto).
- `docs/domain-model.md` — regla **Total con IVA** y R3 (deuda de esta etapa);
  edge cases (comercio vacío, fecha ilegible → propone hoy, cantidad → 1).
- `docs/technical-discovery.md` — "Motor Nube: Gemini vía Firebase AI Logic SDK
  (sin clave de API en el APK; la gestione Firebase)"; observabilidad futura.
- `docs/risks-and-open-questions.md` — #3 Firebase (crear proyecto, cuota free
  tier, latencia desde España); R2, R3, R5 (JSON inválido → prompt con schema y
  enum + validación); research task "Firebase AI Logic con Gemini".
- `CONTEXT.md` — Extracción, Motor (Local | Nube), Categoría (enum de 6),
  Texto crudo, "sin porcentajes de confianza inventados".
- `docs/evidence/ocr-mlkit/texto_crudo_1..4.txt` — **insumo real** de esta
  feature (ruido térmico incluido; totales de referencia por ticket: 1) 32,09
  "Total (impuestos incl.)"; 2) base 3,36 + IVA 0,14 = **3,50** "TOTAL COMPRA";
  3) "Total (impuestos incl.)" 12,64 vs cambio 50,00−36,10 = 13,90 (ambigüedad
  → contraste humano); 4) 7,91 "TOTAL A PAGAR EUROS").
- `docs/specs/ocr-mlkit.md` (patrón de spec + contrato `OcrEngine`,
  `ReconocerTextoCrudo`, `PhotoFileStore.read`) y `docs/specs/captura-foto.md`
  (estructura de tareas/Expected File Changes).
- `domain/expenses/.../entities/{Ticket,Linea,Categoria,Motor}.kt` — tipos que
  reutiliza `TicketExtraido` (`Linea`, `Categoria`, `Motor` ya existen).
- `domain/expenses/.../data/OcrEngine.kt` + `usecases/ReconocerTextoCrudo.kt` —
  patrón de contrato (`suspend`, errores que propagan, `CancellationException`
  nunca tragada) y de caso de uso (`@Inject constructor` + `operator fun invoke`).
- `framework/ai/` (`di/di.kt`, `ocr/MlKitOcrEngine.kt`,
  `spike/GemmaMediaPipeSpikeTest.kt`) — patrón `AiModule` (internal object) e
  `internal` visible desde `androidTest` (precedente del spike) + patrón
  `Assume.assumeTrue` con motivo explícito para fixtures ausentes.
- `test/unit/src/main/kotlin/` — dobles compartidos (`FakePhotoFileStore`,
  `FakeOcrEngine`, `CoroutineTestRule`).
- `app/build.gradle.kts`, `build.gradle.kts` (raíz), `gradle/libs.versions.toml`
  — patrón de plugins con `apply false`, versiones solo en el toml; **sin
  google-services ni Firebase** (confirmado por búsqueda).
- `.gitignore` — excluye `*.task`/`*.litertlm`; hoy no excluye
  `google-services.json`.

### Existing Patterns To Follow

- Contratos en `domain/<área>/data/`, implementaciones `internal` en
  `framework/<área>/`, bindings en un `di.kt` por módulo (`AiModule`).
- Casos de uso con nombre en español (`GuardarFotoCapturada`,
  `ReconocerTextoCrudo` → `ExtraerTicket`) y `operator fun invoke`.
- Tests JVM en `domain/*/src/test/java/…`, dobles en `:test:unit`
  (`src/main/kotlin/`), instrumentados de integración con `assumeTrue` para
  dependencias ambientales (`MlKitOcrEngineTest`), JUnit XML como evidencia.
- Versiones solo en `gradle/libs.versions.toml`, decisión de versión registrada.

### Current Gaps

- Sin `TextEngine`, sin `TicketExtraido`, sin parser/validador, sin `ExtraerTicket`.
- Sin Firebase en el repo: no hay plugin `google-services`, ni `google-services.json`,
  ni SDK `firebase-ai`, ni ningún `GeminiCloudEngine`.
- Sin test de integración real en nube ni evidencias de extracción.

## Technical Approach

1. **Dominio — modelo y validación compartidos** (`:domain:expenses`, Kotlin
   puro): `TicketExtraido` (reutiliza `Linea`, `Categoria`, `Motor`) +
   `TicketExtraidoParser` (puro, sin I/O) + `InvalidExtractionException`. El
   parser es **la pieza que `extraccion-local` reutilizará**: misma entrada
   (texto de respuesta del modelo), mismas reglas. Parseo **tolerante en el
   contenedor** (trim, fences ```json, prosa alrededor del primer `{`…último
   `}`, números con coma decimal española) y **estricto en el schema**.
2. **Dominio — contrato y caso de uso**: `TextEngine` (`val motor: Motor` +
   `suspend fun extract(textoCrudo: String): TicketExtraido`) en
   `domain/expenses/data/`; `ExtraerTicket` en `usecases/` (texto blank →
   `TicketExtraido` vacío **sin invocar el motor**; errores propagan;
   `CancellationException` nunca tragada). El `motor` se registra **dentro del
   resultado** (`TicketExtraido.motor`), lo que deja listo el badge/router.
3. **Framework — `GeminiCloudEngine`** (`internal` en `framework/ai/extraccion/`,
   Firebase AI Logic, `Dispatchers.IO`, inicialización lazy del modelo): prompt
   en español con **schema JSON** (claves del dominio: `comercio`,
   `fecha_emision`, `total`, `categoria`, `lineas[{concepto,cantidad,precio_unitario}]`),
   **enum de 6 categorías** y regla explícita **"total = total CON IVA (Total
   (impuestos incl.)/TOTAL A PAGAR/TOTAL COMPRA), nunca la base"** (deuda R3).
   Se usa `responseMimeType = "application/json"` (+ `responseSchema` si la
   versión del SDK lo soporta). La respuesta se valida con el parser
   (`parse(text, Motor.NUBE)`). Sin claves en el código: SDK vía Firebase BoM;
   config solo de `app/google-services.json`. Error de config ausente →
   excepción con mensaje claro ("Firebase AI no configurado").
4. **Build sin credenciales**: plugin `com.google.gms.google-services` en el
   toml + `apply false` en la raíz, y aplicación **condicional** en `:app`
   (`if (file("google-services.json").exists()) apply(...)`) para que los gates
   estén en verde sin credenciales (alternativa descartada: plugin
   incondicional, que rompe el build en cuanto falta el fichero). `:app` solo
   aporta el plugin; el SDK `firebase-ai` vive en `:framework:ai`. `.gitignore`
   gana `app/google-services.json` (DEFAULT; decisión abierta).
5. **Tests**: (a) JVM en dominio del parser/validador con respuestas malformadas
   (criterio 2) + `ExtraerTicketTest` con `FakeTextEngine`; (b) test real
   instrumentado en `:app/src/androidTest` (donde el proceso tiene la config de
   Firebase) que obtiene `TextEngine` vía **Hilt** (`@HiltAndroidTest` +
   `HiltTestRunner`, patrón documentado en AGENTS.md para instrumentados) y
   corre los 4 textos con `assumeTrue` si Firebase no está configurado (patrón
   `MlKitOcrEngineTest`); (c) prompt builder test en `:framework:ai` (JVM).

## Contrato de dominio (extracción)

1. **`TicketExtraido`** (`domain/expenses/entities/`): `comercio: String`
   (`""` si no aparece), `fechaEmision: LocalDateTime?` (`null` si ilegible; la
   UI propondrá hoy según `docs/domain-model.md`), `total: Double?` (`null` si
   no encontrado; **siempre total con IVA**), `lineas: List<Linea>` (puede ser
   vacía), `categoria: Categoria` (siempre una de las 6), `motor: Motor`
   (quién extrajo: `NUBE` aquí, `LOCAL` en `extraccion-local`).
2. **`TicketExtraidoParser.parse(modelResponse: String, motor: Motor): TicketExtraido`**
   — lanza `InvalidExtractionException(motivo)` si: no hay objeto JSON, JSON
   malformado, `categoria` ausente o fuera de la enum (match case-insensitive
   sobre los nombres `ALIMENTACION…OTROS`, `null`/desconocida = inválida),
   `fecha_emision` presente pero no ISO (`yyyy-MM-dd` o
   `yyyy-MM-dd'T'HH:mm[:ss]`), `total` presente pero no numérico/∞/negativo,
   línea sin `concepto` no-blank, `cantidad` presente `< 1`, `precio_unitario`
   ausente/no numérico/negativo. Tolerancias: fences/prosa (ver arriba), números
   `"3,50"` como string, `cantidad`/`precio_unitario` omitidos → `cantidad = 1`,
   claves extra ignoradas. DTOs serializables **internos** del parser.
3. **`TextEngine`** (`domain/expenses/data/`): único método
   `suspend fun extract(textoCrudo: String): TicketExtraido` + `val motor: Motor`.
   Implementación única hoy: `GeminiCloudEngine`. `EngineRouter` (futuro) será
   otro proveedor de `TextEngine` o de selección; el contrato no cambia.
4. **`ExtraerTicket`** (`domain/expenses/usecases/`):
   `suspend operator fun invoke(textoCrudo: String): TicketExtraido` =
   blank → `TicketExtraido("", null, null, emptyList(), OTROS, engine.motor)`
   sin invocar el motor; si no, `engine.extract(textoCrudo)`. Sin `runCatching`:
   los errores propagan y la cancelación se re-lanza. Punto de extensión futuro
   de `log-extracciones` (envolver este caso de uso).

## Expected File Changes

- `gradle/libs.versions.toml` — modify; + plugin `org.jetbrains.kotlin.plugin.serialization`,
  + `kotlinxSerializationJson` (kotlinx-serialization-json, línea 1.9.x),
  + `googleServices` (plugin com.google.gms.google-services, 4.4.x) y
  + `firebaseBom` + librería `firebase-ai` (sin versión literal: BoM). El
  implementer fija las últimas estables compatibles (Kotlin 2.2.10 /
  compileSdk 36) y registra la decisión.
- `build.gradle.kts` (raíz) — modify; + `alias(libs.plugins.google.services) apply false`.
- `app/build.gradle.kts` — modify; aplicación **condicional** de
  `com.google.gms.google-services` si `app/google-services.json` existe;
  `testInstrumentationRunner = "dagger.hilt.android.testing.HiltTestRunner"`
  (solo runner; `NavigationTest`/`CapturaFlujoTest` deben seguir en verde);
  androidTest: + `hilt-android-testing`, + `kspAndroidTest(libs.hilt.compiler)`.
- `.gitignore` — modify; + `app/google-services.json` (DEFAULT).
- `app/google-services.json` — **NO create** (lo aporta el humano; prerrequisito).
- `framework/ai/build.gradle.kts` — modify; + `implementation(platform(libs.firebase.bom))`,
  + `implementation(libs.firebase.ai)`.
- `domain/expenses/build.gradle.kts` — modify; + plugin serialization,
  + `implementation(libs.kotlinx.serialization.json)`.
- `domain/expenses/src/main/java/com/robertomr/tally/domain/expenses/entities/TicketExtraido.kt` — create.
- `domain/expenses/src/main/java/com/robertomr/tally/domain/expenses/entities/TicketExtraidoParser.kt` — create (parser + DTOs internos).
- `domain/expenses/src/main/java/com/robertomr/tally/domain/expenses/entities/InvalidExtractionException.kt` — create.
- `domain/expenses/src/main/java/com/robertomr/tally/domain/expenses/data/TextEngine.kt` — create.
- `domain/expenses/src/main/java/com/robertomr/tally/domain/expenses/usecases/ExtraerTicket.kt` — create.
- `domain/expenses/src/test/java/com/robertomr/tally/domain/expenses/entities/TicketExtraidoParserTest.kt` — create (verificación 2).
- `domain/expenses/src/test/java/com/robertomr/tally/domain/expenses/usecases/ExtraerTicketTest.kt` — create.
- `test/unit/src/main/kotlin/com/robertomr/tally/test/unit/FakeTextEngine.kt` — create (invocaciones registradas, resultado/error configurables).
- `framework/ai/src/main/java/com/robertomr/tally/framework/ai/extraccion/GeminiCloudEngine.kt` — create (internal).
- `framework/ai/src/main/java/com/robertomr/tally/framework/ai/extraccion/TicketExtractionPrompt.kt` — create (internal; prompt + schema JSON + enum + regla total con IVA).
- `framework/ai/src/main/java/com/robertomr/tally/framework/ai/di/di.kt` — modify; + `@Provides TextEngine` (mismo `AiModule`; NO crear otro `di.kt`).
- `framework/ai/src/test/java/com/robertomr/tally/framework/ai/extraccion/TicketExtractionPromptTest.kt` — create (JVM: schema, enum de 6 y regla "con IVA" presentes en el prompt).
- `app/src/androidTest/assets/texto_crudo_1..4.txt` — create (copias exactas de `docs/evidence/ocr-mlkit/texto_crudo_1..4.txt`).
- `app/src/androidTest/java/com/robertomr/tally/extraccion/ExtraccionNubeRealTest.kt` — create (real; `assumeTrue` sin config).
- `docs/evidence/extraccion-nube/` — create; `extraccion_1..4.json` (o `.txt` con el contenido exacto del logcat cuando la
  respuesta exportada no es JSON; sin reformular) + nota de contraste humano.
- `docs/specs/extraccion-nube.md` — create (este documento).
- `docs/architecture.md` — update; sección "Extracción estructurada (TextEngine)".
- `docs/risks-and-open-questions.md` — update; research task "Firebase AI Logic con Gemini" con resultados reales (T7), nota en R3/R5.
- `docs/technical-discovery.md` — update; Integrations: wiring Firebase AI Logic (plugin condicional, `app/google-services.json` gitignoreado, sin claves autorales).
- `PROGRESS.md` y `feature_list.json` — update **al terminar implementación y verificación** (evidencia real); no por este spec.

## Visual Design Impact

- UI involved: **no** (la UI del procesado es `procesado-ui`; el badge de motor
  es `router-automatico`/`procesado-ui`).
- Design source: no aplicable. Sin decoración ficticia (R4/DESIGN.md).
- New design artifact required: no.

## Durable Documentation Impact

- `ARCHITECTURE.md` (raíz): not needed — no existe; el equivalente es `docs/architecture.md`.
- `CONSTRAINTS.md` (raíz): not needed — no existe; la restricción crítica (sin
  claves en el APK/BuildConfig) queda en `AGENTS.md`, `docs/architecture.md` y este spec.
- `AGENTS.md`: not needed — la capa de IA ya está documentada; no cambian las reglas.
- `docs/architecture.md`: update — sección "Extracción estructurada (TextEngine)":
  `TextEngine`/`TicketExtraido`/parser en dominio, `GeminiCloudEngine` en
  `:framework:ai`, Firebase vía `app/google-services.json` (sin claves autorales).
- `docs/risks-and-open-questions.md`: update — research task Firebase cerrada
  con datos reales (tras T7); R3 anotada (extracción sobre los 4 textos).
- `docs/technical-discovery.md`: update — Integrations (config Firebase; plugin condicional).
- `docs/domain-model.md`: not needed — Total con IVA y semántica de extracción ya documentados.
- `PROGRESS.md` + `feature_list.json`: update al finalizar (evidencia), no ahora.

## Implementation Plan

1. T1 build/catálogo (incluye degradación sin credenciales) → base compila.
2. T2–T4 dominio: modelo + parser + `TextEngine` + `ExtraerTicket` + tests JVM
   (incluye verificación 2 completa) + dobles.
3. T5 framework: `GeminiCloudEngine` + prompt + binding `di.kt` + test del prompt.
4. T6 test real instrumentado (`:app`, Hilt, skip sin config) con los 4 textos.
5. T7 **(bloqueado por prerrequisito humano)** run real → verificación 1 + evidencia.
6. T8–T10 verificación 3 (seguridad), regresión completa, docs durables y cierre.

## Implementation Tasks

- [x] T1 Catálogo + build: serialization (plugin + lib en `:domain:expenses`),
  `googleServices` + `firebaseBom`/`firebase-ai`, `apply false` en raíz,
  aplicación condicional en `:app`, `.gitignore` con `app/google-services.json`;
  0 versiones literales en `**/*.gradle.kts`. *Done: `assembleDebug` +
  `testDebugUnitTest` en verde SIN `google-services.json` presente.*
- [x] T2 Dominio: `TicketExtraido`, `InvalidExtractionException`,
  `TicketExtraidoParser` (tolerante-contenedor/estricto-schema; regla total con
  IVA documentada en KDoc). *Done: `:domain:expenses` compila y sigue sin
  imports de Android/Firebase (búsqueda).*
- [x] T3 Dominio: `TextEngine` + `ExtraerTicket` (blank → vacío sin motor;
  errores/cancelación). *Done: casos de uso en verde (T4).*
- [x] T4 Tests JVM (verificación 2): `TicketExtraidoParserTest` con respuestas
  malformadas y tolerancias + `ExtraerTicketTest` con `FakeTextEngine` (nuevo en
  `:test:unit`). *Done: tests en verde (JUnit XML) — nombres y conteos citables;
  Turbine/coroutines-test si hay flujos; `runTest` basta para casos puros.*
- [x] T5 Framework: `GeminiCloudEngine` (internal, `Dispatchers.IO`, lazy,
  config ausente → error claro, `CancellationException` jamas tragada) +
  `TicketExtractionPrompt` (schema + enum 6 + "total con IVA") +
  `@Provides TextEngine` en `AiModule` + `TicketExtractionPromptTest` (JVM).
  *Done: `assembleDebug` verde; sin claves en el código; un solo `di.kt`.*
- [x] T6 Test real: `ExtraccionNubeRealTest` en `:app/src/androidTest`
  (`@HiltAndroidTest` + `HiltTestRunner`; inyecta `TextEngine`, assert
  `motor == NUBE`; assets `texto_crudo_1..4.txt`; aserciones: cada texto pasa
  schema+enum — criterio 1 —, ticket 2 `total == 3.50` (trampa base/con-IVA) y
  totales 1=32.09 / 4=7.91 con tolerancia 0.01; ticket 3 solo registrado con la
  ambigüedad 12,64/13,90; skip `assumeTrue` con motivo si Firebase no está
  configurado). *Done: el test corre y se salta limpiamente sin config (verde)
  y pasa con config (T7). Regresión `NavigationTest`/`CapturaFlujoTest` verde
  tras el cambio de runner. Expectativas corregidas 2026-10-17 por contraste
  humano (el papel manda, U4): ticket 1 → `total == 35.30` (32,09 estaba mal
  derivado del OCR) y ticket 3 → `total == 13.90` (antes solo registrado);
  ver Implementation Notes.*
- [x] T7 Firebase: proyecto + registro
  `com.robertomr.tally` + Gemini habilitado + `app/google-services.json`;
  ejecución real de los 4 textos en dispositivo con Google services + red
  (emulador con GMS o Tab S9+; `ANDROID_SERIAL=…`), extracciones exportadas a
  `docs/evidence/extraccion-nube/`. *Done 2026-10-17: verificación 1 cumplida —
  `ExtraccionNubeRealTest` tests=5 failures=0 skipped=0 en `emulator-5554`
  (JUnit XML + extracciones reales en `docs/evidence/extraccion-nube/`) —
  + contraste humano del total con IVA por ticket (R3) registrado: papel
  35,30 / 3,50 / 13,90 / 7,91 vs extraído 35,30 / 3,50 / 13,90 / 7,91
  (tabla en `verificacion-1-estado.md`, estado "completada"). El debug token de
  App Check lo registró el usuario en la consola (decisión humana) y el modelo
  quedó en `gemini-3.8-flash` (U3; el servidor deprecó `gemini-2.5-flash` para
  proyectos nuevos).*
- [x] T8 Verificación 3 (seguridad): búsquedas (`AIza`, `api_key`,
  `buildConfigField`, `GEMINI`, secretos) sobre fuentes/recursos; revisión de
  que el único material de config es el mecanismo estándar de `google-services`
  y que `:domain:expenses` no importa Android/Firebase. *Done: informe de
  revisión citable en evidencia (sin claves autorales; `google-services.json`
  fuera de VCS).*
- [x] T9 Regresión completa **(2026-10-17 cierre: run final en `emulator-5554`
  con `CapturaFlujoTest` 3/3 + `NavigationTest` 3/3 + `ExtraccionNubeRealTest`
  5/5 en la misma suite (tests=11 failures=0 skipped=0); gates JVM en verde;
  init.sh sin diff, 0 versiones literales, `schemas/` sin cambios)**: `assembleDebug`, `testDebugUnitTest`,
  `:test:unit:test --rerun`, `:domain:expenses:test --rerun` (parser +
  ExtraerTicket + tests previos), `:framework:ai:testDebugUnitTest` (prompt),
  `:app:connectedDebugAndroidTest` (E2E + real) y, si aplica, el run real en
  Tab S9+; `init.sh` sin diffs (esta máquina no tiene bash: equivalencia =
  `assembleDebug` + `testDebugUnitTest`); 0 versiones literales; `schemas/` de
  Room sin diff. *Done: todos los gates en verde con JUnit XML fresco.*
- [x] T10 Cierre: docs durables (`architecture.md`, `risks-and-open-questions.md`,
  `technical-discovery.md`), evidencia en `feature_list.json` (sin inventar
  resultados; T7 sin ejecutar = declarado bloqueado) y sesión en `PROGRESS.md`
  + Implementation Notes de este spec. *Done: estado actualizado y coherente.*

## Verification Plan

Criterio de dispositivos: **emulador `emulator-5554`** para gates JVM/E2E; la
**extracción real en nube (verificación 1)** necesita un dispositivo con Google
services + red (Tab S9+ SM-X810 disponible, ADB inalámbrico
`adb-R52W60AFAPZ-izULbK._adb-tls-connect._tcp`; el emulador solo sirve si su
imagen incluye GMS; `realme GT 2` no está conectado a esta máquina). Con varios
dispositivos, `ANDROID_SERIAL=<serial> gradlew.bat …`.

Automatizada (Windows: `gradlew.bat`):

- `gradlew.bat assembleDebug` → BUILD SUCCESSFUL (base; con y sin
  `app/google-services.json` — degradación verificada).
- `gradlew.bat testDebugUnitTest` → BUILD SUCCESSFUL (incluye
  `TicketExtraidoParserTest`, `ExtraerTicketTest`, `TicketExtractionPromptTest`).
- `gradlew.bat :test:unit:test --rerun` → BUILD SUCCESSFUL (SmokeTest).
- `gradlew.bat :domain:expenses:test --rerun` → JUnit XML de los tests nuevos
  (verificación 2: malformadas rechazadas, tolerancias OK).
- `ANDROID_SERIAL=emulator-5554 gradlew.bat :app:connectedDebugAndroidTest` →
  `ExtraccionNubeRealTest` (skip limpio sin config) + `CapturaFlujoTest` +
  `NavigationTest` 3/3.
- `ANDROID_SERIAL=<serial-con-GMS> gradlew.bat :app:connectedDebugAndroidTest`
  con `app/google-services.json` presente → `ExtraccionNubeRealTest` **4/4 sin
  skips** (verificación 1) + regresión.
- `bash init.sh` (o equivalencia en esta máquina: los dos gates anteriores) →
  verde; `git diff init.sh` vacío.
- Búsquedas de regresión: 0 versiones literales fuera del toml; 0 imports de
  Android/Firebase en `domain/**`; `schemas/` de Room sin diff; "Continuar"
  `enabled = false` intacto.

Manual / revisión (verificación 3):

- Búsquedas `AIza|api[_-]?key|buildConfigField|GEMINI_API` en fuentes y recursos.
- Comprobar que `app/google-services.json` no está en `git status` (gitignoreado)
  y que ningún BuildConfig del APK lleva claves (`buildConfigField` = 0).
- Contraste humano de los totales con IVA de los 4 tickets vs papel (R3).

E2E persistente: `:app:connectedDebugAndroidTest` (no hay harness web/JS). La
feature no cambia UI ni flujos de usuario visibles; la cobertura nueva es el
test de integración real (`ExtraccionNubeRealTest`) y los unit tests del
parser; los E2E existentes se corrían como regresión (justificación: alcance
sin UI, patrón de `persistencia-room`).

## Evidence To Capture

- JUnit XML: `domain/expenses/build/test-results/test/` (parser + ExtraerTicket),
  `framework/ai/build/test-results/` (prompt), `app/build/outputs/androidTest-results/connected/debug/`
  (real + E2E; citar conteos, skips y dispositivos).
- `docs/evidence/extraccion-nube/extraccion_1..4.json` (o `.txt` con el
  contenido exacto) + logcat (tag propio)
  con las extracciones reales; nota del contraste humano de totales (R3),
  incluida la decisión del ticket 3 (12,64 vs 13,90).
- Informe de la revisión de seguridad (verificación 3) y confirmación de
  `.gitignore`/`google-services.json`.
- Decisión de versiones (firebase-ai/BoM, google-services, kotlinx-serialization)
  + 0 versiones literales.
- Todo registrado en `feature_list.json` (evidence) y `PROGRESS.md`; si T7 no se
  ejecuta por falta del prerrequisito, se declara **bloqueado** (sin inventar
  resultados) y la feature no queda `passing`.

## Riesgos

- **R-NUBE1 — Prerrequisito Firebase**: sin `google-services.json` no hay
  verificación 1; mitigación: T1–T6 independientes + skip explícito; el estado
  "bloqueado" se documenta, no se simula.
- **R-NUBE2 — R5 (calidad del JSON del modelo)**: mitigación: schema estricto +
  prompt con enum + tolerancias de contenedor; respuestas malformadas = error
  claro → vía manual/reintento (features futuras). Sin auto-retry silencioso
  (ADR 0001).
- **R-NUBE3 — Total base vs con IVA (R3)**: mitigación: regla explícita en el
  prompt + assert del ticket 2 (3,50 vs 3,36) + contraste humano de los 4.
- **R-NUBE4 — Cuota/latencia Gemini (R2)**: solo se mide en T7 (red de
  tethering si hace falta); free tier Spark suficiente para la demo.
- **R-NUBE5 — SDK Firebase AI Logic nuevo** (sin precedente en el repo): los
  nombres exactos de API (`Firebase.ai`, `GenerativeBackend`, `responseSchema`)
  se confirman contra la documentación del SDK al implementar; el contrato que
  se preserva es `TextEngine` (riesgo acotado a `:framework:ai`).
- **R-NUBE6 — Cambio de runner de `:app`** a `HiltTestRunner`: riesgo de romper
  E2E existentes; mitigación: T6 exige `NavigationTest`/`CapturaFlujoTest` en
  verde tras el cambio (alternativa `EntryPointAccessors` si fallara).

## Open Questions (para el usuario)

**DEFAULT** marcados; solo **U1 bloqueaba** (T7/verificación 1). Estado
2026-10-17: U1 cumplida (proyecto + `google-services.json` + token de App Check
registrado por el usuario), U3 y U4 resuelta abajo; U2 y U5 quedan en su
DEFAULT.

1. **U1 — Proyecto Firebase** (PRERREQUISITO): ¿creas el proyecto, registras
   `com.robertomr.tally` y aportas `app/google-services.json`? DEFAULT: sí,
   antes de T7; hasta entonces T7 queda bloqueada y el test real se salta.
2. **U2 — `google-services.json` en VCS**: DEFAULT **gitignoreado** (decisión
   conservadora; el validador sin el fichero ejecuta el test con skip). Si lo
   prefieres commiteado (es config pública según Google), se retira del
   `.gitignore` y se documenta la restricción de la API key por paquete+SHA-1.
3. **U3 — Modelo Gemini**: DEFAULT `gemini-2.5-flash` (rápido/económico; free
   tier); se registra el modelo final usado. **RESUELTA 2026-10-17**: el modelo
   final es **`gemini-3.8-flash`** — el servidor deprecó `gemini-2.5-flash`
   para proyectos nuevos (`ServerException: This model models/gemini-2.5-flash
   is no longer available to new users … use models/gemini-3.8-flash`,
   evidencia en `junit-app-emulador-5554-run-modelo-deprecado.xml`) y la
   contingencia fue cambiar al modelo que indicó el servidor
   (`TicketExtractionPrompt.MODELO`).
4. **U4 — Ticket 3 (OZONE)**: ¿el total de papel es 12,64 ("Total (impuestos
   incl.)") o 13,90 (50,00−36,10 de cambio)? DEFAULT: 12,64 para el assert
   documental; el contraste humano en T7 decide y se corrige la expectativa del
   test si el papel dice otra cosa (evidencia > test). **RESUELTA 2026-10-17
   (decisión del usuario, el papel manda)**: el total real del papel es
   **13,90** (50,00 − 36,10 de cambio); el "Total (impuestos incl.) 12,64" del
   OCR era engañoso. El assert pasa de "solo registrado" a `total == 13.90`.
5. **U5 — Backend**: DEFAULT `googleAI` (free tier, sin facturación);
   alternativa `vertexAI` (facturación GCP) descartada por defecto.

## Implementation Notes / Desviaciones

- **Desviación acordada 2026-10-16 (5 → 4 textos crudos)**: la verificación 1
  original hablaba de "los 5 textos crudos reales"; el usuario aportó **4**
  fotos/tickets (misma desviación ya formalizada en `ocr-mlkit`, sesión 016).
  Se ajusta el campo `verification` de `feature_list.json` a **4 textos** con
  esta justificación; la evidencia y los tests se alinean a
  `texto_crudo_1..4.txt`.
- **Desviación de implementación (sesión 018, R-NUBE6)**: el test real NO usa
  `HiltTestRunner`/`@HiltAndroidTest` (previsto en T6), sino
  `EntryPointAccessors.fromApplication` sobre `TallyApplication` + runner
  `AndroidJUnitRunner` estándar. Motivo: `HiltTestApplication` exige
  `HiltAndroidRule` en TODOS los tests instrumentados y rompía
  `NavigationTest`/`CapturaFlujoTest`; el propio riesgo R-NUBE6 preveía esta
  alternativa. El `@EntryPoint` (`TextEngineEntryPoint`) vive en el código MAIN
  de `:app` (en androidTest el casteo falla con `ClassCastException`).
  Regresión confirmada: `NavigationTest` 3/3 y `CapturaFlujoTest` 3/3 en verde.
- **Desviación de implementación (sesión 018, App Check)**: el test real instala
  el **debug provider** de Firebase App Check (`DebugAppCheckProviderFactory`),
  necesario porque Firebase AI Logic exige App Check enforced (obligatorio desde
  2026-11-02); sin provider de producción todavía (pendiente futuro). Dos
  hallazgos derivados, corregidos en esta sesión:
  1. `firebase-appcheck-debug` tiene que empaquetarse también en la **variante
     debug de `:app`** (`debugImplementation`), no solo en androidTest: en
     20.0.0 su `DebugAppCheckProviderFactory.create()` resuelve el provider vía
     `FirebaseApp.get(DebugAppCheckProvider.class)` (componente Firebase
     registrado por el manifest de la librería) y el descubrimiento de
     componentes solo lee el paquete de la app; con la librería solo en el APK
     de test, `create()` devuelve `null` ("No AppCheckProvider installed") y
     Gemini rechaza la petición.
  2. El debug token tiene que ser **fijo**: AGP desinstala la app al terminar
     cada `connectedDebugAndroidTest` y `firebase-appcheck-debug` 20.0.0 solo
     persiste su token generado en datos de la app (el mecanismo del string
     resource `com.google.firebase.appcheck.debug.DEBUG_TOKEN` ya no existe), de
     modo que cada ejecución generaba un UUID nuevo que habría que volver a
     registrar. El harness inyecta el token por
     `testInstrumentationRunnerArguments` desde `app/appcheck-debug-token.txt`
     (LOCAL, gitignoreado — mismo patrón que `google-services.json`; el SDK pide
     no subirlo a VCS); sin el fichero, el test recurre al factory estándar.
- **Desviación de implementación (sesión 018, versiones)**: fijadas en el
  catálogo con registro de decisión — `kotlinxSerializationJson` **1.11.0**,
  `googleServices` **4.5.0**, `firebaseBom` **35.0.2** (de él salen `firebase-ai`
  y `firebase-appcheck`/`firebase-appcheck-debug` **20.0.0**, sin versión
  literal). 0 versiones literales en los `*.gradle.kts`.
- **Desviación de implementación (sesión 018, modelo Gemini U3)**: el primer
  run real con App Check ya válido recibió
  `ServerException: This model models/gemini-2.5-flash is no longer available
  to new users … use models/gemini-3.8-flash` (proyecto nuevo `tally-586d1`;
  evidencia en `junit-app-emulador-5554-run-modelo-deprecado.xml`). Contingencia
  ya prevista en el prompt: `TicketExtractionPrompt.MODELO = "gemini-3.8-flash"`
  (el modelo que indicó el servidor), con su KDoc actualizado.
- **Corrección de expectativas del test real (2026-10-17, contraste humano R3/U4
  — el papel manda sobre el test)**: las expectativas derivadas del OCR estaban
  mal y se corrigen contra el papel, no al revés:
  1. **Ticket 1 (HOLA ALMIRANTE): 32,09 → 35,30.** La foto trae DOS bloques y el
     texto crudo ambos importes ("Total (impuestos incl.) 32,09" y "Total:
     35,30"); el usuario confirmó con el papel que el total CON IVA real es
     35,30. El test pasó de `ticket1ExtractionIsValidWithTotal3209` a
     `ticket1ExtractionIsValidWithTotal3530`.
  2. **Ticket 3 (OZONE): 12,64 → 13,90.** El "Total (impuestos incl.) 12,64" del
     OCR era engañoso; el papel manda: 50,00 entregado − 36,10 de cambio =
     13,90. El test pasó de "solo registrado"
     (`ticket3ExtractionIsLoggedOnlyBecauseOfTheOcrAmbiguity`) a
     `ticket3ExtractionIsValidWithTotal1390` (tolerancia 0,01). El modelo
     devolvió 13,90 en el run final sin endurecer el prompt (el importe 13,90
     aparece en el texto crudo y es consistente con Entregado−Cambio), por lo
     que `TicketExtractionPrompt.kt` NO se tocó en el cierre (su test sigue 6/6).
- **Estado de T7 (sesión 018, cierre 2026-10-17)**: **COMPLETADA**. El usuario
  registró el debug token de App Check en la consola (intervención humana
  puntual) y el run final quedó
  `ExtraccionNubeRealTest` **tests=5 failures=0 skipped=0** en `emulator-5554`
  (regresión E2E 11/11 en la misma suite). Extracciones reales exportadas a
  `docs/evidence/extraccion-nube/extraccion_1..4.txt` (contenido exacto del
  logcat tag `ExtraccionNube`) + tabla de contraste humano de los 4 totales
  (35,30 / 3,50 / 13,90 / 7,91 = extraído vs papel, 4/4 correctos) en
  `docs/evidence/extraccion-nube/verificacion-1-estado.md` (estado
  "completada").

## Validator Checklist

- [x] Implementation stays within this feature's scope (sin UI, sin ViewModels,
  sin Room, sin GemmaLocal/router/reintentos, sin claves inventadas).
- [x] Acceptance scenarios 1-7 pass (1 con los 4 textos reales; 2 con la regla
  total con IVA y el ticket 2 = 3,50).
- [x] Verification 1 con evidencia real (JUnit XML + `docs/evidence/extraccion-nube/`)
  o estado **bloqueado declarado** por falta del prerrequisito humano (sin
  resultados inventados).
- [x] Verification 2: unit tests del parser/validador con respuestas malformadas
  en verde y citados.
- [x] Verification 3: revisión sin claves de API en el APK ni en BuildConfig
  (informe + `.gitignore`).
- [x] `TextEngine` en `domain/expenses/data/` sin Android/Firebase; parser
  compartido en dominio (reutilizable por `extraccion-local`); `GeminiCloudEngine`
  `internal`; un solo `di.kt` (`AiModule`); `TicketExtraido.motor = Motor.NUBE`.
- [x] Versiones solo en `gradle/libs.versions.toml`; plugin `google-services`
  condicional (build verde sin credenciales); `init.sh` sin cambios.
- [x] Regresión: `NavigationTest` 3/3, `CapturaFlujoTest` verde, "Continuar"
  deshabilitado, esquema Room sin cambios.
- [x] `feature_list.json` y `PROGRESS.md` actualizados con evidencia no inventada
  (estado `not_started`/`in_progress` → `passing` solo con verificación real).
- [x] No unrelated product behavior or extra feature work was added.

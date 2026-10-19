# Technical Discovery

## Product Surface

- App Android nativa, single-activity, Kotlin 100 %, Jetpack Compose + Material 3.
- Arquitectura, módulos y reglas: `AGENTS.md` (fuente de verdad; este doc no la
  duplica). Resumen: Clean Architecture multi-módulo, Hilt, Coroutines/Flow,
  Room, sin XML.
- Dispositivos de prueba: Galaxy Tab S9+ (12 GB RAM, pantalla expandida) y
  realme GT 2 (compacto, no mueve el modelo local). La app debe verse
  correcta en ambos: layouts con ancho máximo centrado del diseño de Stitch.

## Candidate Stack

Fijado por AGENTS.md. Solo se detalla lo que AGENTS.md no cubre:

- **OCR**: ML Kit Text Recognition v2 (`text-recognition` latin), on-device,
  gratuito, sin clave de API. Produce el texto crudo.
- **Motor Local**: Gemma 3n con MediaPipe (LLM Inference / GenAI) como
  `GemmaLocalEngine` en `framework/ai`. El archivo del modelo NO va en el APK:
  se descarga desde Ajustes (WiFi + progreso + espacio; 3,4 GB reales, no "2-3 GB") con WorkManager y reanudación `Range` en `framework/ai/modelo`; el token de Hugging Face lo pega el usuario y vive en `noBackupFilesDir`.
- **Motor Nube**: Gemini vía Firebase AI Logic SDK como `GeminiCloudEngine`
  (sin clave de API en el APK; la gestione Firebase).
- **Gráfica**: donut dibujado con Canvas de Compose (el diseño usa SVG, se
  traduce a arcos). Sin librería de charts de terceros.

## Data and Storage

- Room como única fuente de verdad: tablas `tickets` y `lineas` (borrado en
  cascada por FK), escrituras multi-tabla con `@Transaction`.
- Fotos: ficheros en el directorio privado de la app (`filesDir/fotos/`), ruta
  guardada en Room. Nada en MediaStore (evita exposición en la galería).
- Texto crudo del OCR: **no** es un fichero sidecar. Vive con el Borrador en
  memoria y, al guardar, en la columna `tickets.texto_crudo_ocr` (esquema v1)
  del ticket, junto a su foto. Aclarado al implementar `ocr-mlkit` (esta era la
  discrepancia frente al esquema aceptado en `persistencia-room`).
- Fotos comprimidas a un máximo razonable (~1280 px lado largo, JPEG ~85)
  tras corregir orientación EXIF; no se guarda el original en resolución
  de cámara completa.

## Integrations

- Solo dos salidas externas: descarga inicial del modelo local (Hugging Face)
  e inference en nube (Firebase AI Logic). Nada más: sin REST propio, sin
  analíticas, sin crash reporting obligatorio (valorar más adelante).
- **Firebase AI Logic (Gemini en nube, `extraccion-nube`)**: SDK `firebase-ai`
  gestionado por el Firebase BoM (`gradle/libs.versions.toml`), backend
  `googleAI` (free tier; `vertexAI` descartado por exigir facturación) y modelo
  `gemini-flash-lite-latest` (variante **lite**, elegida 2026-10-17 por su mayor
  cuota diaria y menor coste; sustituye a `gemini-3.8-flash`, cuyo free tier solo
  limitaba a 20 req/día, y a `gemini-2.5-flash`, deprecado por el servidor para
  proyectos nuevos). La config viaja SOLO en `app/google-services.json`
  (gitignoreado; lo aporta el humano — prerrequisito U1 de la spec) mediante el
  plugin `com.google.gms.google-services`, aplicado **condicionalmente** en
  `:app` (`if (file("google-services.json").exists())`) para que el build siga
  en verde sin credenciales. Sin claves de API en el código ni en `BuildConfig`
  (0 `buildConfigField` en el repo; el único material de config del APK son los
  recursos estándar que inyecta el plugin).
- **App Check (obligatorio para Firebase AI Logic desde 2026-11-02)**: de
  momento solo el **debug provider** (`firebase-appcheck-debug`, en androidTest
  y en la variante debug de `:app`; su componente Firebase tiene que
  empaquetarse en el APK de la app para que el descubrimiento de componentes lo
  encuentre). El debug token es **fijo y local**
  (`app/appcheck-debug-token.txt`, gitignoreado, mismo patrón que
  `google-services.json`) porque AGP desinstala la app al terminar cada
  `connectedDebugAndroidTest` y el token generado por instalación no sobrevive;
  hay que registrarlo una vez a mano en la consola (App Check > Manage debug
  tokens). Provider de producción (Play Integrity) pendiente de decisión futura.

## Authentication and Authorization

- Ninguna. Sin cuentas ni login (decisión de producto). El acceso a los datos
  equivale a desbloquear el dispositivo; los datos viven en almacenamiento
  privado de la app. Requisito de privacidad explícito en la demo: el ticket
  nunca sale del dispositivo cuando se usa el motor Local.

## Deployment and Operations

- Distribución: APK firmado instalado a mano en los dos móviles (no hay
  Play Store en el MVP).
- Operación día 1: el propio autor. El error humano a prevenir: perder la
  foto por un crash → la foto se persiste en el momento de la captura.
- Sin CI obligatorio; builds locales de Gradle.

## Testing and Verification

- Unit: casos de uso, repositorios y ViewModels con dobles de `:test:unit`,
  Turbine para Flow. Crítico: reglas del router, patrón Período (rangos),
  borrado en cascada, y el extractor con textos crudos de ejemplo (sin red).
- Instrumentado (si hay tiempo): Room en memoria + Hilt `@TestInstallIn`.
- Verificación E2E: manual, en los dos dispositivos (ver build-brief).

## Observability

- Mínimo y local: registro de cada extracción con motor usado, duración,
  longitud del texto crudo y resultado (éxito/fallo/cancelada). Sirve para la
  memoria del curso y para diagnosticar en la demo.
- Decisión (`log-extracciones`, 2026-10-19): tabla Room `extracciones_log` en la
  misma `TallyDatabase` (v2, migración 1 -> 2), solo metadatos (sin texto ni
  mensajes de excepción), retención de 500 filas, registro en todos los builds y
  consulta/exportación CSV solo en debug (sección de Ajustes). Sin telemetría
  remota.

## Constraints

- 6 semanas, 1 persona: cualquier dependencia nueva debe justificarse por
  ahorrar tiempo, no por elegancia.
- Offline-first: el motor Local debe poder completar el flujo sin red
  (incluida la primera descarga del modelo hecha antes).
- Sin claves de API en el APK ni en BuildConfig (regla de AGENTS.md).
- Windows como entorno de desarrollo: comprobar que el toolchain de Android
  y las herramientas de MediaPipe funcionan en este entorno (tarea de la S1).

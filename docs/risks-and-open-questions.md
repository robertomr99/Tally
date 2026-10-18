# Risks and Open Questions

## Blocking Next Phase

1. **Soporte real de Gemma 3n en MediaPipe (Android)** — **RESUELTO 2026-10-12
   (spike `spike-gemma-mediapipe`, sesión 006): DECISIÓN GO.**
   - Artefacto validado: `com.google.mediapipe:tasks-genai:0.10.35`
     (`LlmInference` + `LlmInferenceSession`) con
     `google/gemma-3n-E2B-it-int4.litertlm` (Gemma 3n E2B-it cuantizado int4,
     formato LiteRT-LM, 3.655.827.456 bytes ≈ 3,4 GB, origen Hugging Face
     `google/gemma-3n-E2B-it-litert-lm`; fuera del APK y del repo).
   - Dispositivo: Galaxy Tab S9+ (`SM-X810`, `gts9pwifi`), Android 16 (API 36),
     11.454.088 kB de RAM (~11,4 GB).
   - Resultado real del harness (test instrumentado, off-main-thread verificado
     con `ranOnMainThread=false`): carga `loadMillis=6517`; generación
     `generationMillis=53208` con `promptTokens=17` / `responseTokens=492`
     (~9,2 tokens/s); respuesta correcta y no vacía: "2 + 2 es igual a 4."
     (muestra corroborante en logcat del mismo día:
     `loadMillis=6933`, `generationMillis=53985`).
   - Justificación del GO: la carga de 3,4 GB en ~6,5 s es excelente y la
     latencia medida queda dentro del criterio del proyecto ("decenas de
     segundos", ver más abajo). **Matiz importante**: los 53 s de generación
     incluyen ~480 tokens degenerados de repetición de `<end_of_turn>`/`<eos>`
     hasta el tope de `maxTokens=512` (el modelo contestó en una frase y siguió
     generando tokens especiales). La respuesta útil llegó de inmediato; con
     secuencias de parada (`<end_of_turn>`) la latencia efectiva será de
     ~1–2 s tras el prefill. Para una extracción real (~100–200 tokens JSON)
     la estimación a 9,2 tokens/s es ~11–22 s + prefill del texto del ticket.
   - Requisitos que esto impone a `extraccion-local` (fuera de este spike):
     usar secuencias de parada / token de fin de turno en el wrapper definitivo
     (evita la generación degenerada y acota la latencia), y re-medir la latencia
     con prompts de extracción largos (prefill de decenas de líneas).
   - **Plan B (concreto, si la extracción real no cumple latencia o calidad)**:
     1) Gemma 2 2B-it en formato `.task` (también soportada por MediaPipe,
     más ligera y rápida); 2) Gemma 3n E2B sin visión si la convivencia con el
     pipeline de OCR/ML Kit presiona la memoria; 3) E4B-it int4 solo si la
     calidad de extracción lo exige y el presupuesto de latencia de la demo lo
     permite (generación esperada más lenta); 4) si el motor local no fuera
     viable en absoluto, el ADR 0001 ya contempla enrutar a Gemini en nube.
2. **Alojamiento y descarga del modelo**: elegir fuente (Hugging Face), tamaño
   (3,4 GB reales, 3.655.827.456 bytes; `descarga-modelo`: WorkManager, solo red no medida, token HF del usuario, reanudación con `Range`) y UX de descarga (solo WiFi, progreso, espacio libre).
3. **Firebase AI Logic**: crear proyecto, habilitar Gemini, verificar cuota del
   free tier y latencia desde España. Sin clave en el APK.

## Implementation-Time Questions

- **Latencia del modelo local**: puede ser lenta (decenas de segundos). Se
  necesita cancelación, progreso y un mensaje honesto; decidir timeout si lo
  hay. No prometer % real de progreso (es estimación).
- **Qué muestra el indicador de motor**: chip con "Motor Local (Gemma 3n)" o
  "Motor Nube (Gemini)" + estado del proceso. Sustituye al "OCR Verificado
  99,4 %" del diseño (ver CONTEXT.md).
- **Colores de categoría en la gráfica**: decisión de diseño pendiente,
  centralizada en `DESIGN.md` (Open Design Questions #1).
- **Navegación expandida (Tab S9+)**: decisión de diseño pendiente,
  centralizada en `DESIGN.md` (Open Design Questions #3).
- **EXIF/rotación y compresión de la foto**: **RESUELTO 2026-10-15
  (captura-foto, sesión 012): pipeline decidido y verificado.** La orientación
  EXIF se hornea en los píxeles (rotaciones 180/90/270 de las orientaciones
  3/6/8; las de espejo 2/4/5/7 quedan fuera del MVP), la salida se escala a
  ≤1280 px de lado largo conservando proporción y se recomprime JPEG (~85)
  sin tag de orientación; el original de cámara NO se guarda. Implementado en
  `CapturedPhotoProcessor` + `PhotoPipelineMath` (`:framework:database`),
  verificado con fixture EXIF Orientation=6 (tests instrumentados en emulador
  y Tab S9+) y con captura real (960×1280 JPEG en `filesDir/fotos/`).
- **"Frecuentes" de comercios**: chips sugeridos desde el historial (GROUP BY
  comercio). Incluir si es barato; si no, quitar sin drama.

## Later / Not MVP

- Métricas de precisión con dataset etiquetado (20–30 tickets anotados).
- Navigation rail / split-screen para expandido.
- Modo oscuro cuidado, exportación, backup, presupuestos (ya en non-goals).
- Persistencia de borradores tras crash (hoy solo la foto sobrevive).

## Assumptions

- EUR y español de España; tickets impresos (no manuscritos).
- Un solo usuario; sin red garantizada; el modelo local puede pre-descargarse
  antes de la demo.
- Los dos móviles de prueba son representativos para el tribunal.
- El texto del ticket cabe en el contexto del modelo local (recibos de
  supermercado típicos, decenas de líneas).

## Risks

- **R1 — Integración MediaPipe**: **resuelta el 2026-10-12** con el spike
  `spike-gemma-mediapipe` (decisión GO arriba, en "Blocking Next Phase" #1):
  Gemma 3n E2B-it int4 carga y responde en la Tab S9+ con
  `tasks-genai:0.10.35`, off-main-thread. Queda como requisito derivado usar
  secuencias de parada en el motor definitivo (latencia efectiva ~1–2 s tras
  prefill frente a los 53 s medidos con cola degenerada) y re-medir con prompts
  de extracción reales. Plan B documentado arriba.
- **R2 — Quota/latencia de Gemini en la demo**: mitigación: red propia
  (tethering), límites conocidos de antemano, datos sembrados de reserva.
  **Estado 2026-10-17 (`procesado-ui`, sesión 021)**: la latencia ya es visible
  al usuario en la pantalla 06 (progreso circular indeterminado + "Cancelar"
  siempre disponible, sin % inventado). Durante la verificación física se
  **agotó la cuota diaria del free tier** (`gemini-3.8-flash`, límite 20
  req/día) tras las pruebas reales acumuladas: la extracción falló con error de
  cuota honesto en pantalla (sin datos inventados) y la **cancelación en vuelo
  física** no pudo ejecutarse ese día (la petición fallaba al instante, sin
  ventana para cancelar). La cancelación queda cubierta por tests JVM
  (`EscanearViewModelTest` con `FakeTextEngine.gate`). La medición formal de
  latencia sigue en S3.
  **Actualización 2026-10-17 (sesión 022)**: por decisión del usuario se cambió el
  motor de nube a la variante **lite** (`gemini-flash-lite-latest`) para tener más
  cuota diaria y menor coste; el motor de nube es solo **fallback** (ADR 0001: el
  principal es Gemma on-device, `router-automatico`). Verificado real:
  `ExtraccionNubeRealTest` **5/5** en `emulator-5554` con los 4 totales correctos
  (35,30 / 3,50 / 13,90 / 7,91) y en **9,2 s totales** (frente a los 82 s del
  ticket 1 con `gemini-3.8-flash`): el lite es bastante más rápido. La cuota
  concreta del lite queda por medir; el techo diario sigue existiendo (R2 abierta)
  pero deja de agotarse en ~4 corridas.
- **R3 — Calidad de OCR en tickets reales**: arrugas, térmicos descoloridos.
  Mitigación: encuadre guiado en cámara, flujo de corrección manual sólido.
  **Estado 2026-10-16 (`ocr-mlkit`, sesión 016)**: la etapa 1 del ADR 0002 está
  implementada y verificada on-device (ML Kit `text-recognition` 16.0.1 bundled;
  OCR real en emulador y Tab S9+). Se aportaron **4 fixtures reales** (desviación
  acordada; el spec planificaba 5) en
  `framework/ai/src/androidTest/assets/ticket_real_1..4.jpg` + `.tokens.txt`
  (`MlKitOcrEngineTest` 7/7 verde en emulador y Tab S9+) y el usuario confirmó el
  **contraste humano**: los 4 textos crudos contienen comercio, fecha, productos
  y totales y "coinciden razonablemente con el papel" (ruido típico de térmicos).
  Los textos están en `docs/evidence/ocr-mlkit/texto_crudo_1..4.txt`. **Deuda
  anotada para la etapa 2** (`extraccion-nube`/`extraccion-local`): el texto
  crudo trae base, cuota y total por separado; la extracción debe preferir el
  **total con IVA** (lo que paga el usuario), no la base. R3 se considera
  mitigada para la etapa 1 con esta verificación; sigue abierta end-to-end hasta
  que la extracción estructurada funcione sobre estos 4 textos.
  **Estado 2026-10-17 (`extraccion-nube`, sesión 018, cierre)**: la etapa 2 está
  implementada (prompt con la regla explícita "total con IVA", test trampa
  ticket 2 → 3.50 en `ExtraccionNubeRealTest`, validador estricto) y la
  extracción real sobre los 4 textos crudos pasó **5/5** en `emulator-5554`.
  **Contraste humano con el papel DECIDIDO por el usuario (2026-10-17, U4 del
  spec — el papel manda sobre el test)**: totales CON IVA reales
  **35,30 / 3,50 / 13,90 / 7,91** (ticket 1 = 35,30, no el 32,09 del OCR: la
  foto trae dos bloques; ticket 3 = 13,90 por 50,00−36,10 de cambio, no el
  12,64 etiquetado "Total (impuestos incl.)" en el OCR). El modelo extrajo
  exactamente esos 4 totales (4/4 correctos) y las expectativas del test se
  corrigieron al papel (32,09→35,30; 12,64→13,90). R3 queda **cerrada
  end-to-end** para estos 4 tickets reales; tabla citable en
  `docs/evidence/extraccion-nube/verificacion-1-estado.md`.
- **R4 — Scope creep de UI**: el diseño de Stitch tiene decoración no
  realizable (hash, IRPF, verificación). Mitigación: DESIGN.md marca lo
  normativo y lo decorativo.
- **R5 — Calidad de datos estructurados del modelo local**: JSON inválido o
  campos alucinados. Mitigación: prompt con schema y enum de categorías,
  validación + reintento de parseo, y revisión humana siempre.

## Research Tasks

| Tarea | Entregable | Semana |
| --- | --- | --- |
| Gemma 3n + MediaPipe en Android | Modelo corriendo con "hola mundo" en Tab S9+ (hecho 2026-10-12, decisión GO) | S1 |
| Firebase AI Logic con Gemini | Extracción REAL probada 2026-10-17 (sesión 018, `extraccion-nube` T7) en Tab S9+ (SM-X810) y emulador `emulator-5554` (imagen con GMS verificada): wiring completo en verde (`google-services.json` + plugin `google-services` condicional + `firebase-ai` vía BoM 35.0.2, backend `googleAI`, `responseSchema`); modelo final **`gemini-3.8-flash`** (el servidor deprecó `gemini-2.5-flash` para proyectos nuevos) con App Check debug registrado por el usuario; `ExtraccionNubeRealTest` **5/5** con los 4 tickets reales y totales 4/4 según contraste humano (35,30 / 3,50 / 13,90 / 7,91) (evidencia en `docs/evidence/extraccion-nube/`). Cuota sin medir; latencias observadas por testcase 4-36 s (R2: medición formal pendiente, S3). ACTUALIZADO 2026-10-17 (sesión 022): modelo de nube cambiado a `gemini-flash-lite-latest` (lite) por cuota/coste; verificado real 5/5 en 9,2 s | S1 |
| ML Kit Text Recognition v2 | Integración (etapa 1 ADR 0002) implementada y verificada on-device; 4 tickets reales aportados (desviación acordada 2026-10-16 respecto a 5) con contraste humano confirmado "coincide razonablemente con el papel"; textos en `docs/evidence/ocr-mlkit/texto_crudo_1..4.txt` | S1–S2 |
| Medición de latencia local/nube | Números reales para el indicador y la memoria | S3 |

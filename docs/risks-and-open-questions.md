# Risks and Open Questions

## Blocking Next Phase

1. **Soporte real de Gemma 3n en MediaPipe (Android)** — **RESUELTO 2026-10-12
   (spike `spike-gemma-mediapipe`, sesión 006): DECISIÓN GO.**
   - Artefacto validado: `com.google.mediapipe:tasks-genai:0.10.35`
     (`LlmInference` + `LlmInferenceSession`) con
     `google/gemma-3n-E2B-it-int4.litertlm` (Gemma 3n E2B-it cuantizado int4,
     formato LiteRT-LM, 3.655.827.456 bytes ≈ 3,4 GB, origen Hugging Face
     `google/gemma-3n-E2B-it-litertlm`; fuera del APK y del repo).
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
   (~2–3 GB esperados) y UX de descarga (solo WiFi, progreso, espacio libre).
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
- **EXIF/rotación y compresión de la foto**: decidir pipeline exacto al
  implementar la captura (decisión ya tomada: corregir + comprimir).
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
- **R3 — Calidad de OCR en tickets reales**: arrugas, térmicos descoloridos.
  Mitigación: encuadre guiado en cámara, flujo de corrección manual sólido.
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
| Firebase AI Logic con Gemini | Extracción de ejemplo desde el realme | S1 |
| ML Kit Text Recognition v2 | Texto crudo de 5 tickets reales | S1–S2 |
| Medición de latencia local/nube | Números reales para el indicador y la memoria | S3 |

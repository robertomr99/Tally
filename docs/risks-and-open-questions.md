# Risks and Open Questions

## Blocking Next Phase

1. **Soporte real de Gemma 3n en MediaPipe (Android)**: confirmar versión de
   MediaPipe GenAI/LLM Inference que la soporta, obtener el archivo del modelo
   (.task/.litertlm) y verificar RAM/latencia en la Tab S9+. *Semana 1: si
   esto no funciona, el plan B es Gemma 2 (también soportada por MediaPipe) o
   centrar la parte local en demostrar el router con un modelo más pequeño.*
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

- **R1 — Integración MediaPipe**: la mayor incertidumbre técnica del proyecto.
  Mitigación: atacar en S1 con spike; plan B ya apuntado arriba.
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
| Gemma 3n + MediaPipe en Android | Modelo corriendo con "hola mundo" en Tab S9+ | S1 |
| Firebase AI Logic con Gemini | Extracción de ejemplo desde el realme | S1 |
| ML Kit Text Recognition v2 | Texto crudo de 5 tickets reales | S1–S2 |
| Medición de latencia local/nube | Números reales para el indicador y la memoria | S3 |

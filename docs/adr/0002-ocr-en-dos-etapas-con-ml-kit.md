# ADR 0002: OCR en dos etapas con ML Kit Text Recognition

## Status

Accepted

## Context

La extracción necesita texto legible a partir de la foto del ticket. Gemma 3n
es multimodal y podría leer la imagen directamente, pero MediaPipe en Android
encarece el camino de visión, la latencia y la fiabilidad, y el motor Nube
tendría otro formato de entrada. El proyecto compara dos motores de extracción
sobre la misma entrada.

## Decision

Pipeline en dos etapas: **foto → texto crudo (ML Kit Text Recognition v2,
latin, on-device, sin clave) → datos estructurados (TextEngine: Local o
Nube)**. El texto crudo se guarda con el ticket como evidencia y entrada de
reintentos. Ambos motores consumen exactamente el mismo texto.

## Alternatives Considered

- **Gemma 3n en modo visión (foto directa)**: menos piezas, pero más lento en
  local, rompe la simetría de comparación entre motores y complica el contrato
  `TextEngine` (que pasaría a consumir imágenes).
- **OCR en el servidor**: descartado, no hay backend y es otra cuota/gasto.

## Consequences

Los errores de OCR se propagan al extractor (mitigado: encuadre guiado en
cámara y revisión/corrección manual obligatoria). ML Kit es gratuito,
determinista y rápido. La misma entrada para ambos motores hace la comparativa
de la demo honesta y reproducible.

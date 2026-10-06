# ADR 0001: Doble motor de extracción con router automático

## Status

Accepted

## Context

La app extrae datos de tickets con IA. Debe funcionar sin conexión en
dispositivos potentes (Galaxy Tab S9+, 12 GB) y existir una alternativa para
los que no pueden mover el modelo local (realme GT 2). El proyecto se demuestra
en ambos dispositivos y el motor usado debe ser siempre visible. El curso exige
mostrar la decisión técnica, no solo el resultado.

## Decision

- Contrato `TextEngine` en `domain/expenses/data/`; implementaciones
  `GemmaLocalEngine` (MediaPipe, on-device) y `GeminiCloudEngine`
  (Firebase AI Logic) en `framework/ai`, `internal`.
- `EngineRouter` elige motor al iniciar cada extracción: Local si el
  dispositivo es capaz y el modelo está descargado; si no, Nube.
- El usuario puede forzar motor (Ajustes: Auto/Local/Nube) y cambiar de motor
  al reintentar una extracción. El fallback automático silencioso NO existe:
  el reintento es una acción del usuario, así el motor usado queda siempre
  claro y registrado con el ticket.
- El motor en uso se expone en el estado de UI (Badge de motor).

## Alternatives Considered

- **Solo Gemini en nube**: más simple, pero la app no funcionaría offline y no
  demostraría IA on-device (el corazón del proyecto).
- **Solo Gemma local**: excluye el realme GT 2 y la demo pierde mitad del
  argumento.
- **Fallback automático local→nube**: ocultaría qué motor respondió y
  complicaría el indicador; se rechazó en favor del reintento explícito.

## Consequences

Dos integraciones que mantener y probar; gestión de la descarga del modelo
(~2–3 GB) fuera del APK; consumo de cuota de Gemini; a cambio: offline-first
real, comparativa limpia entre motores (mismo texto crudo) y un argumento
sólido para la memoria del curso.

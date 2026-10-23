# Build Brief

## Problem

Los gastos diarios se pagan con tickets de papel que se arrugan, se pierden y
nadie procesa. Saber cuánto se gasta y en qué categoría exige anotar a mano,
que nadie mantiene. La foto del ticket es fácil; convertir la foto en datos
fiables y en una visión por categorías es lo que no existe sin IA.

## Current Workaround / Existing System

Guardar los tickets en una cartera/caja y apuntar gastos a mano en una app o
libreta (si se hace). Alternativas existentes o piden datos bancarios, o exigen
teclear cada ticket, o no funcionan sin conexión.

## Target Users

Un único usuario local: el dueño del dispositivo (el autor del proyecto).
Sin cuentas, sin login, sin sincronización: el acceso a los datos es la
posesión física del teléfono. Los datos viven en el almacenamiento privado de
la app (Room + ficheros). Si se pierde el móvil, se pierden los datos: aceptado
explícitamente.

## Goals

- Fotografiar un ticket y obtener, con IA, sus datos estructurados: comercio,
  fecha/hora, total, líneas de producto y categoría.
- Funcionar **sin conexión** en dispositivos potentes (Gemma 3n local vía
  MediaPipe) y **con alternativa en nube** (Gemini) en los que no pueden.
- Que la app decida sola qué motor usar y lo **muestre siempre** al usuario.
- El usuario revisa y corrige **siempre** antes de guardar (reintento de
  extracción y edición manual completa, incluido añadir/quitar líneas).
- Seguimiento con gráfica de gastos por categoría y período seleccionable
  (día / semana / mes / rango personalizado) con listado de tickets debajo.
- Guardar la foto original como evidencia junto a cada ticket.

## Non-Goals

- Backend propio, cuentas de usuario o sincronización entre dispositivos.
- Exportación de datos, copias de seguridad o CSV.
- Presupuestos por categoría, alertas o notificaciones.
- Escaneo por lotes de varios tickets.
- Multi-moneda e idiomas: solo EUR y español de España.
- Onboarding/tutorial, widgets, modo oscuro como feature (solo el que dé M3 gratis).
- Desglose fiscal (base imponible, cuota IVA) y datos de "recibo oficial"
  (NIF, hash, verificaciones): el diseño de Stitch los muestra como decoración;
  no se extraen ni se generan.
- Comparativas históricas mes a mes más allá del badge "vs período anterior".

## MVP Slice

Flujo vertical completo, de punta a punta, en dos dispositivos reales:

1. Resumen: selector de período, total, gráfica de distribución por categoría
   (donut), desglose y listado de tickets del período.
2. Captura: cámara con encuadre guiado o foto elegida de la galería, y foto
   original guardada. (Ampliación de alcance decidida por el usuario el
   2026-10-20, feature `galeria-escaneo`.)
3. Procesado: OCR (ML Kit) → extracción (motor Local o Nube según router),
   con indicador del motor y cancelación.
4. Revisión: ticket digitalizado con datos extraídos, categoría corregible
   (chips de la lista fija), líneas editables (añadir/quitar/editar),
   botones Reintentar / Editar / Guardar.
5. Persistencia en Room + foto en ficheros privados; borrado en cascada.
   Además, alta manual de un gasto sin ticket desde la pestaña Gastos (feature
   `gasto-manual`, decidida por el usuario el 2026-10-20): la vía manual ya no
   es solo el respaldo de una extracción fallida.
6. Ajustes básicos: forzar motor (Auto/Local/Nube) y estado del modelo local.

## Validation Plan

- **Demo E2E en ambos móviles** (criterio de éxito del curso): Galaxy Tab S9+
  (12 GB, motor Local) y realme GT 2 (motor Nube), en directo.
- Preparar ~10 tickets reales de distintos comercios/categorías + un ticket
  deliberadamente "malo" (arrugado, borroso) para demostrar la vía de
  reintento y corrección manual sin bloqueo.
- Datos de demo sembrados (tickets falsos) por si la IA en directo falla: en la build
  debug, Ajustes > "Datos de demo" > "Sembrar datos de demo" crea 13 gastos falsos
  (6 categorías, 3 formas de pago, 2 manuales y el ticket "Comercio ilegible" para
  corregir a mano) y "Quitar datos de demo" los borra sin tocar tickets reales
  (`seed-demo`).
- Comprobación de la vía manual: extracción vacía → rellenar a mano → guardar.

## Success Criteria

1. El flujo completo foto → extracción → corrección → guardado → gráfica
   funciona en directo en los dos dispositivos, cada uno con su motor.
2. El indicador de motor refleja el motor realmente usado en cada extracción.
3. Una extracción mala nunca bloquea: reintento con el otro motor o corrección
   manual terminan en un ticket guardado.
4. El proyecto se defiende en la memoria del curso con las decisiones
   documentadas (docs/ y AGENTS.md).

## Notes

- Proyecto final de curso: 6 semanas, 1 persona. Alcance realista; los
  non-goals son negociables solo hacia abajo.
- AGENTS.md es la fuente de verdad técnica (arquitectura, módulos, stack);
  este documento no la duplica.
- Reparto orientativo de semanas: S1 base técnica + captura + persistencia;
  S2 OCR + extracción Nube + revisión; S3 modelo Local + router + indicador;
  S4 seguimiento + gráfica + períodos; S5 tablet/ajustes + pulido; S6 demo,
  datos sembrados y memoria. La semana 3 (modelo local) es la de mayor riesgo:
  no atrasarla.
- Diseño: fuente de verdad visual en `DESIGN.md` y `docs/design/stitch/`.

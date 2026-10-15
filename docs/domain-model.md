# Domain Model

## Core Concepts

- **Ticket**: agregado raíz. Campos: comercio, fecha/hora de emisión, total
  (editable), categoría, nota opcional, fecha de captura, motor usado en la
  extracción, ruta de la foto, texto crudo del OCR.
- **Línea**: producto del ticket. Campos: concepto, cantidad (entero ≥ 1),
  precio final unitario (IVA incluido). Pertenece a un Ticket.
- **Categoría**: enum fija de 6 valores (Alimentación, Hogar, Transporte,
  Ocio, Salud, Otros) con icono y color en UI. No es una tabla editable.
- **Motor**: Local | Nube. Se registra con el ticket (qué motor extrajo).
- **Período**: par de fechas derivado de Día / Semana / Mes / Rango personalizado.

## Relationships

- Ticket 1—N Líneas (las líneas se guardan y editan con el ticket).
- Ticket 1—1 Foto original (ruta en ficheros privados; borrado en cascada).
- Ticket N—1 Categoría (de la enum fija).
- Período 1—N Tickets (por fecha de emisión, no de captura).

## States and Lifecycles

El Ticket tiene dos estados:

- **Borrador** (no persistido): desde la captura hasta pulsar Guardar. Vive en
  memoria. La foto **sí** se guarda en disco al capturarla, para no perderla
  si la app muere durante la revisión.
- **Guardado** (persistido en Room): cuenta en el resumen. Es editable
  siempre (reutiliza la pantalla de edición) y borrable.

Transiciones:

- Foto capturada → Borrador (se lanza OCR + extracción; el usuario puede
  cancelar el procesado y quedarse en el Borrador con campos vacíos).
- Reintento de extracción → nueva extracción sobre el Borrador (puede cambiar
  de motor; se actualiza el motor registrado).
- Guardar → Guardado (persiste ticket, líneas y vincula la foto).
- Editar (Guardado) → edición en memoria → Guardar cambios → Guardado.
- Borrar → se elimina el ticket, sus líneas y su foto (cascada, con confirmación).
- Repetir foto → se descarta el Borrador actual y se borra de disco la foto del
  borrador reemplazado (best-effort); la foto de un ticket Guardado solo se
  borra en cascada con él.

## Important Scenarios

- **Extracción correcta**: los campos llegan poblados; el usuario corrige
  categoría y guarda.
- **Extracción inservible**: el usuario reintenta (con el otro motor) o rellena
  a mano en la edición; la foto nunca se pierde; guardar sigue siendo posible
  con los campos vacíos rellenados manualmente.
- **OCR vacío** (foto ilegible): la app ofrece edición manual con campos vacíos
  y no considera la foto "gastada".
- **Fechas**: si la fecha de emisión es ilegible, se propone hoy y el usuario la
  corrige; el ticket entra en el período de su fecha de emisión corregida.
- **Total vs líneas**: el total es el dato autoritativo para el seguimiento;
  la suma de líneas se muestra como referencia ("Total recalculado") y puede
  no coincidir (OCR imperfecto). No hay error ni sobrescritura automática.

## Edge Cases

- Comercio ilegible → campo vacío con placeholder "Sin comercio"; no bloquea.
- Cantidad de línea ilegible → 1 por defecto.
- Líneas que no cuadran con el total → permitido (regla anterior).
- Ticket de otro día/mes → entra en el período de su fecha de emisión.
- Foto girada (EXIF) → se corrige la orientación al guardar la foto.
- Sin modelo local descargado y sin red → la extracción Nube falla con error
  claro; la vía manual sigue disponible.

# Domain Model

## Core Concepts

- **Ticket**: agregado raíz. Campos: comercio, fecha/hora de emisión, total
  (editable), categoría, nota opcional, fecha de captura, motor usado en la
  extracción, ruta de la foto, texto crudo del OCR, forma de pago y, solo en
  efectivo, importe entregado y cambio (`forma-pago`). Un **gasto manual**
  (`gasto-manual`) es un Ticket con `motor = null` (sin extracción), ruta de foto
  vacía y sin texto crudo: `Ticket.esManual`. En Room se guarda `MANUAL` en la
  columna `motor` y `""` en `ruta_foto` (esquema v3 intacto); la lectura del
  motor es tolerante (`MANUAL` o un valor desconocido → manual, nunca lanza).
- **Forma de pago**: enum `FormaPago` = `EFECTIVO` | `TARJETA` | `NO_INDICADA`
  (`NO_INDICADA` es el valor de los tickets anteriores a la migración 2 → 3 y de
  los que no dicen cómo se pagó; bizum, vale o pago mixto también se guardan
  como `NO_INDICADA`). `FormaPago.desde(texto)` es tolerante: mayúsculas,
  espacios y tildes se ignoran y un valor desconocido o nulo da `NO_INDICADA`.
- **Entregado** y **cambio**: importes informativos (`Double?`, ≥ 0). **Nunca son
  el total**: el total con IVA sigue siendo lo que cuesta y lo único que suman
  Resumen, donut y listado.
- **Línea**: producto del ticket. Campos: concepto, cantidad (entero ≥ 1),
  precio final unitario (IVA incluido). Pertenece a un Ticket.
- **Categoría**: enum fija de 6 valores (Alimentación, Hogar, Transporte,
  Ocio, Salud, Otros) con icono y color en UI. No es una tabla editable.
- **Motor**: Local | Nube. Se registra con el ticket (qué motor extrajo). No hay
  un motor "manual": su ausencia (`null`) significa gasto manual.
- **Período**: par de fechas derivado de Día / Semana / Mes / Rango personalizado.

## Relationships

- Ticket 1—N Líneas (las líneas se guardan y editan con el ticket).
- Ticket 1—0..1 Foto original (ruta en ficheros privados; borrado en cascada;
  ausente en un gasto manual).
- Ticket N—1 Categoría (de la enum fija).
- Período 1—N Tickets (por fecha de emisión, no de captura).

## States and Lifecycles

El Ticket tiene dos estados:

- **Borrador** (no persistido): desde la captura hasta pulsar Guardar. Vive en
  memoria. La foto **sí** se guarda en disco al capturarla, para no perderla
  si la app muere durante la revisión. El Borrador lleva también el texto crudo
  del OCR (etapa 1 del ADR 0002), que se recupera tal cual para extraer y
  reintentar sin repetir el OCR.
- **Guardado** (persistido en Room): cuenta en el resumen. Es editable
  siempre (reutiliza la pantalla de edición) y borrable.

Transiciones:

- Foto capturada → Borrador (se lanza OCR + extracción; el usuario puede
  cancelar el procesado y quedarse en el Borrador con campos vacíos).
- Reintento de extracción → nueva extracción sobre el Borrador (puede cambiar
  de motor; se actualiza el motor registrado).
- Guardar → Guardado (persiste ticket, líneas y vincula la foto).
- Gasto manual → Guardado directamente (sin Borrador ni foto): desde Gastos,
  "Nuevo gasto" con total > 0 y fecha no futura (el comercio es opcional;
  categoría Otros por defecto; hora 00:00 de la fecha elegida). Después se edita
  y borra como cualquier ticket. No genera registro en el log de extracciones.
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
- **Total con IVA**: el precio que le cuesta al usuario es el total **con
  impuestos incluidos**. El texto crudo del OCR trae base, cuota y total por
  separado; la extracción estructurada (`extraccion-nube`/`extraccion-local`)
  debe preferir el total con IVA, no la base (confirmado en `ocr-mlkit`,
  sesión 016).

- **Forma de pago, entregado y cambio** (`forma-pago`): invariante persistida,
  aplicada por la función pura `normalizarPago` (al parsear la extracción y al
  guardar, `DatosRevision.normalizada()`):
  1. con forma `NO_INDICADA` y entregado o cambio presentes → `EFECTIVO` (tener
     entregado o cambio implica efectivo);
  2. con forma distinta de `EFECTIVO` → entregado y cambio son `null`;
  3. `EFECTIVO` con total conocido (> 0): solo entregado → cambio = entregado −
     total (si ≥ 0); solo cambio → entregado = total + cambio (ticket con
     "EFECTIVO / TOTAL A PAGAR 7,91 / CAMBIO 2,09" → entregado 10,00);
  4. valores negativos o no finitos → `null`.

  En edición el cambio se calcula al escribir entregado o total y sigue siendo
  editable; si `entregado < total` o `|entregado − total − cambio| > 0,01` aparece
  un aviso NO bloqueante (`avisoPago`). Al pasar de efectivo a tarjeta, entregado
  y cambio se conservan ocultos en el estado de edición (volver a efectivo los
  recupera) y se limpian al guardar. La regla del total no cambia: ni entregado
  ni cambio son nunca el total (en el texto 3, total 13,90 / entregado 50,00 /
  cambio 36,10; en tarjeta "A DEVOLVER 0,00" no es cambio).

## Edge Cases

- Comercio ilegible → campo vacío con placeholder "Sin comercio"; no bloquea.
- Cantidad de línea ilegible → 1 por defecto.
- Líneas que no cuadran con el total → permitido (regla anterior).
- Ticket de otro día/mes → entra en el período de su fecha de emisión.
- Foto girada (EXIF) → se corrige la orientación al guardar la foto.
- Sin modelo local descargado y sin red → la extracción Nube falla con error
  claro; la vía manual sigue disponible.

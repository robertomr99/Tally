# Context

Glosario compartido del proyecto Tally. Sirve para que agentes y persona hablen
el mismo idioma: nada de implementación, planes ni decisiones aquí.

## Glossary

### Tally
Nombre de la app: control de gastos personales a partir de fotografías de
tickets de compra. Android nativo, todo local.

### Ticket
Registro de gasto guardado en la app, normalmente derivado de un ticket de papel
fotografiado (o, sin papel, un **Gasto manual**). Contiene: comercio, fecha/hora, importe total (con IVA, lo que
cuesta), líneas, categoría y forma de pago (con entregado y cambio en efectivo).
En la UI se presenta como "ticket digitalizado" con estética de recibo.

### Forma de pago
Cómo se pagó un ticket: **Efectivo**, **Tarjeta** o **No indicada** (también
para bizum, vale o pago mixto y para los tickets anteriores a la función). Es
solo información: no cambia el importe que cuenta el ticket.

### Filtro de forma de pago
Control **Todos / Efectivo / Tarjeta** de la pestaña Gastos. Solo afecta al
listado (Resumen y donut no cambian) y se combina con el periodo. **No indicada**
solo aparece en Todos. Vive mientras vive la pantalla; no se guarda entre sesiones.

### Entregado
Importe que el cliente dio en efectivo (p. ej. 50,00 €). Solo existe con forma
de pago Efectivo. **No es el total**: el total es lo que cuesta el ticket.

### Cambio
Importe que se devolvió al cliente en efectivo (p. ej. 36,10 € = entregado 50,00
− total 13,90). Solo con Efectivo. **No es el total.** Un "A DEVOLVER 0,00" de un
pago con tarjeta no es un cambio.

### Ticket digitalizado
Nombre en UI del "ticket virtual": pantalla de detalle con los datos
estructurados extraídos, con apariencia de recibo, junto a la foto original
como evidencia. No es un renderizado del papel.

### Gasto manual
Ticket que el usuario añade a mano desde la pestaña Gastos, sin foto y sin
extracción (no tiene Motor). Usa el mismo formulario editable que la revisión;
exige un total mayor que 0 y una fecha no futura, y el comercio es opcional. En
la UI se marca con el chip "Manual" y la tarjeta "Sin foto". No es una
extracción: no aparece en el log ni cuenta como motor local o de nube.

### Foto original
Imagen capturada por el usuario con la cámara o elegida de su galería (opcional:
un Gasto manual no tiene). Es
evidencia inmutable: nunca se reemplaza, solo se borra junto con su ticket
(borrado en cascada). Es la copia procesada guardada en el almacenamiento
privado de la app; la imagen de la galería no se modifica ni se enlaza, y puede
no ser una captura reciente.

### OCR
Paso que convierte la foto en texto crudo. Se hace con ML Kit Text
Recognition, en el dispositivo, sin red.

### Texto crudo
Salida textual del OCR, tal cual, antes de estructurar. Se guarda junto al
ticket como evidencia de la extracción.

### Extracción
Conversión del texto crudo en datos estructurados (comercio, fecha, total,
líneas, categoría). La hace un Motor. Siempre revisable y corregible a mano.

### Motor
Implementación de extracción. Solo hay dos: **Local** (Gemma 3n con MediaPipe,
en el dispositivo, sin red) y **Nube** (Gemini vía Firebase AI Logic). El motor
usado se registra con cada extracción y se muestra siempre en la UI.

### Router (EngineRouter)
Componente que elige el motor al iniciar una extracción: Local si el
dispositivo puede y el modelo está descargado; si no, Nube. El usuario puede
forzar el motor en Ajustes y en la pantalla de procesado.

### Línea
Producto de un ticket: concepto, cantidad y precio final (IVA incluido). Las
líneas son informativas y editables; no llevan categoría propia.

### Categoría
Tipo de gasto del ticket completo. Lista fija de 6 valores, con icono y color:
Alimentación, Hogar, Transporte, Ocio, Salud, Otros. El modelo elige de esta
lista; el usuario corrige entre estas opciones.

### Período
Ventana temporal del seguimiento: Día, Semana, Mes o Rango personalizado
(definido por el usuario con dos fechas). Los gastos se agrupan por fecha de
emisión del ticket, no por fecha de captura.

### Comercio
Nombre del establecimiento del ticket. Campo editable con sugerencias de
comercios ya usados ("Frecuentes").

### Nota
Texto libre opcional ligado a un ticket (contexto personal: "compra compartida
con Sofía").

### Ajustes
Pantalla básica de configuración: forzar motor (Auto/Local/Nube) e información
del modelo local (descargado o no). Nada más.

## Rejected / Ambiguous Terms

### Tipo de gasto
Usar `Categoría` siempre. Son la misma cosa; solo puede haber un término.

### Recibo
Usar `Ticket`. En España el dominio del usuario es "ticket".

### Ticket virtual
Usar `Ticket` (dominio) o `Ticket digitalizado` (UI). "Virtual" solo como
explicación coloquial, nunca en documentos ni código.

### Confianza (porcentaje OCR)
El diseño de Stitch muestra "OCR Verificado 99,4 %" con un número que la app no
puede calcular de forma fiable. No mostrar porcentajes inventados: mostrar
estado (completitud de campos: "6/6 campos extraídos") y motor usado.

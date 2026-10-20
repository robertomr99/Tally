# Evidencia mejoras-visuales (F1, sesión 061)

Entorno: `emulator-5556` (Pixel_10_Pro, API 37). Configuraciones (valores originales anotados antes de empezar y
restaurados al final: `wm size` 1280x2856, `wm density` 480, `font_scale` 1.0, `user_rotation` 0,
`accelerometer_rotation` 1):

- `movil`: tamaño por defecto, ~427 dp de ancho.
- `360`: `wm size 720x1560` + `wm density 320`.
- `fs13` / `fs20`: 360 dp con `settings put system font_scale` 1.3 / 2.0.
- `tablet`: `wm size 2560x1600` + `wm density 320` (1280x800 dp), `accelerometer_rotation 0`.

Las pantallas de Escanear (cámara, Revisar foto, Resultado, Revisar ticket) se tomaron con un test instrumentado
temporal ya eliminado que recorre el flujo REAL con la cámara virtual (extracción vacía, de ahí el "0/5 campos" y el
papel casi vacío) y se detiene en cada estado. El resto, a mano con `adb`. El mismo flujo se capturó antes y después.
Capturas reducidas (360/480/800 px de ancho; las de cámara/foto en JPEG). No hay vídeos.

## Pares antes / después

| Hallazgo | Antes | Después |
| --- | --- | --- |
| V01 hueco doble bajo la barra de estado | `antes-movil-resumen` (título a y~145, reloj blanco) | `despues-movil-resumen` (título justo bajo la barra) |
| V02 iconos de la barra de estado | `antes-movil-resumen` (reloj casi invisible) | `despues-movil-resumen`, `despues-movil-camara` (iconos oscuros) |
| V03 dock con fuente grande | `antes-fs13-revision-datos`, `antes-fs20-revision-datos` ("Desca...", "Guard...", total en 5 líneas) | `despues-fs13-revision-datos`, `despues-fs20-revision-datos`, `despues-360-revision-datos` (botones completos, total en <=2 líneas) |
| V04 chip Categorización a 2,0 | `antes-fs20-procesado` (círculo de 3 líneas) | `despues-fs20-procesado` (pasos apilados, píldoras) |
| V05 forma de diálogos | (píldora 9999 dp en `AlertDialog` sin forma propia) | `despues-movil-dialogo-borrar`, `despues-movil-datepicker` (28 dp del tema) |
| V06 marco de la foto | `antes-movil-revision-foto`, `antes-tablet-revision-foto` (marco a todo el ancho) | `despues-movil-revision-foto`, `despues-tablet-revision-foto` (borde ajustado a la imagen) |
| V07 cabecera en Procesado | `antes-fs20-procesado`, `antes-tablet-procesado` | `despues-movil-procesado`, `despues-fs20-procesado`, `despues-tablet-procesado` ("Resultado" + badge) |
| V08 rango duplicado | `antes-movil-resumen` (chip en la tarjeta) | `despues-movil-resumen` (solo subtítulo) |
| V09 "periodo" | `antes-movil-resumen` ("período anterior") | `despues-movil-resumen` ("periodo anterior") |
| V10/V11 alineación y 20 dp | `antes-tablet-procesado` ("0/5" a la izquierda del papel) | `despues-tablet-procesado`, `despues-movil-gastos`, `despues-movil-procesado` |
| V20 `imePadding` | `antes-movil-detalle-teclado` (el dock queda bajo el teclado) | `despues-movil-detalle-teclado` (dock sobre el teclado, campo visible) |

Sin par: `despues-movil-camara` (no cambia salvo la barra de estado), `despues-tablet-resumen`.

## Qué se ve (lectura honesta del agente; revisión visual humana pendiente)

- Resumen, Gastos, Detalle, Ajustes, Revisar foto, Resultado y Revisar ticket arrancan a la misma distancia de la
  barra de estado. Iconos y reloj oscuros en todas. En la cámara la franja de estado sigue siendo `surface` y la vista
  empieza debajo (igual que antes; no es edge-to-edge de cámara).
- Revisar foto: se cambió el fondo de `surfaceContainerHigh` a `surface`; con el hueco ya único, el color distinto
  dibujaba una banda visible bajo el reloj. En tablet la foto ya no tiene fondo gris a todo el ancho.
- Con fontScale 2,0 el dock apila los botones y ocupa ~1/3 de la pantalla; el formulario sigue haciendo scroll.
  El resultado de Procesado a 2,0 deja poca altura al papel (scroll interno).

## No resuelto / fuera de F1 (visto en las capturas)

- Selector de periodo a 2,0 recorta "Sema"/"Me"/"Personal" y el título "Detalle del ti..." se elide (V12, V22: F2).
- Revisar foto a 2,0: el botón "Repetir foto" se corta en "Rep" (dock sin apilar). No estaba en la auditoría;
  candidato para F2.
- No se capturó el diálogo "Eliminar modelo" de Ajustes (exige un modelo descargado; fabricar el fichero habría
  alterado la disponibilidad del motor local en los E2E). Usa el mismo `AlertDialog` sin forma propia y `Button` en
  paleta `error`; la forma de 28 dp se ve en el diálogo de borrar y en el DatePicker.
- Las transiciones (150/300 ms) no se capturaron en imagen; se comprobaron por la suite E2E de navegación.

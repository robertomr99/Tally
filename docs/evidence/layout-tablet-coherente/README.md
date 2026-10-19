# Evidencia layout-tablet-coherente

Entorno: `emulator-5556` (Pixel_10_Pro AVD, API 37). Configuraciones:

- `movil`: tamano por defecto, 1280x2856 @480 dpi (~427 dp de ancho).
- `360`: `wm size 720x1560` + `wm density 320` (360 dp).
- `tablet`: `wm size 2560x1600` + `wm density 320` (1280x800 dp, horizontal natural, `user_rotation 0`,
  `accelerometer_rotation 0` durante la prueba).
- `700dp` / `840dp`: `wm size 1400x1000` / `1680x1000` @320 dpi (comprobar el umbral del tope).

Todo se restauro al terminar: `wm size reset`, `wm density reset`, `user_rotation 0`, `accelerometer_rotation 1`
(valores originales anotados antes de empezar). Capturas reducidas (500/360/1000 px de ancho). Los datos son los del
emulador (32 tickets de octubre); el token de Hugging Face no se toco.

## Pares antes/despues

| Pantalla | movil | 360 dp | horizontal 1280 dp |
| --- | --- | --- | --- |
| Resumen | `antes-movil-resumen` / `despues-movil-resumen` | `antes-360-resumen` / `despues-360-resumen` | `antes-tablet-resumen` / `despues-tablet-resumen` (+ `-scroll`, `-recientes`) |
| Gastos | `antes-movil-gastos` / `despues-movil-gastos` | `antes-360-gastos` / `despues-360-gastos` | `antes-tablet-gastos` / `despues-tablet-gastos` |
| Detalle | `antes-movil-detalle` / `despues-movil-detalle` | `antes-360-detalle` / `despues-360-detalle` | `antes-tablet-detalle` / `despues-tablet-detalle` |
| Ajustes | `antes-movil-ajustes` / `despues-movil-ajustes` | `antes-360-ajustes` / `despues-360-ajustes` | `antes-tablet-ajustes` / `despues-tablet-ajustes` |
| Escanear (camara) | sin cambios (capturas identicas en peso y aspecto; borradas por tamano) | idem | `antes-tablet-escanear` / `despues-tablet-escanear` |
| Revisar foto | `despues-movil-revision-foto` | - | `despues-tablet-revision-foto` |
| Procesado (resultado) | - | - | `despues-tablet-procesado-resultado` |
| Revisar ticket | `despues-movil-revision-datos` | - | `despues-tablet-revision-datos` |
| Umbral | - | `despues-700dp-resumen` (sin tope), `despues-840dp-resumen` (margen 60 dp por lado) | - |

## Lo que se ve (honesto)

- Horizontal: Resumen, Gastos, Detalle y Ajustes comparten la misma columna centrada de 720 dp y cabecera. En
  Resumen el selector ya no ocupa todo el ancho; el donut queda en su franja y equilibrado con la leyenda. La barra
  inferior mantiene el fondo completo con los 3 items agrupados en la columna. En el Detalle el fondo del dock llega
  a los bordes y sus botones/total quedan en la columna. En la camara el visor es a todo el ancho y el obturador y
  "Cerrar" quedan dentro de la columna. En "Revisar foto" el marco de la foto es a todo el ancho (decision de la spec)
  y el titulo y los botones van en la columna.
- Movil y 360 dp: mismo ancho/padding que antes. Cambios visibles: Resumen "Resumen" + rango en vez de "Tally";
  titulos en `onSurface` y SemiBold; Gastos con el rango bajo el titulo (antes bajo el selector); Ajustes sube ~8 dp;
  "Revisar foto" / "Revisar ticket" son cabeceras nuevas.
- No verificado con captura: donut centrado en vacio/cargando (no se logro forzar un periodo sin gastos); estados de
  Procesado en curso y fallido.

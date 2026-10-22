# Evidencia de gasto-manual

Capturas hechas en `emulator-5556` (Pixel_10_Pro, 1280x2856 a 480 dpi) con
`adb -s emulator-5556`. Para 360 dp se usó `wm size 720x1440` + `wm density 320`,
para letra grande `font_scale 2.0` y para horizontal `user_rotation 1`; todo se
restauró (1280x2856, 480, 1.0, rotación 0, auto-rotación 1). Los gastos de prueba
se crearon por la UI con comercio "Panaderia La Espiga" (5/10/2026, 12,50 €) y se
borraron por la UI al terminar; las filas "Sin comercio 0,00 €" del emulador son
residuos de otros E2E anteriores, no de esta feature.

| Fichero | Qué muestra |
| --- | --- |
| `01-gastos-boton-mas.png` | Gastos con el botón "+" (`gastos_anadir`) en la cabecera, 1280x2856. |
| `02-nuevo-gasto-requisito.png` | Nuevo gasto vacío: Guardar/Descartar deshabilitados y el texto "Indica un total mayor que 0" con icono, sin barra inferior. |
| `03-nuevo-gasto-360dp-fuente-2.png` | Nuevo gasto a 360 dp y letra 2,0: dock apilado, requisito en dos líneas. |
| `04-gastos-360dp-fuente-2.png`, `04b-gastos-360dp.png` | Gastos a 360 dp con letra 2,0 y 1,0: el "+" se ve sin recorte. |
| `05-gastos-vacio-dos-botones-360dp.png` | Estado vacío (periodo 5-6 oct) con "Escanear ticket" y "Añadir gasto manualmente" apilados. |
| `06-…`, `07-gastos-vacio-fuente-2-desplazado.png` | Vacío a 360 dp y letra 2,0: el contenido no cabe, se desplaza (arreglo hecho tras ver este recorte) y ambos botones son alcanzables. |
| `08-nuevo-gasto-valido-360dp.png` | Total 12,5 escrito: el requisito desaparece y "Guardar cambios" se habilita. |
| `09-gasto-manual-en-listado.png` | Tras guardar: fila "Panaderia La Espiga · Otros · 12,50 €" sin hora (00:00) y total del día 12,50 €. |
| `10-…`, `11-detalle-gasto-manual-*.png` | Detalle: chip "Manual", tarjeta "Sin foto / Este gasto se añadió a mano", sin badge de IA ni "Ver foto original"; a 360 dp y letra 2,0. |
| `12-…`, `13-…-horizontal.png` | Horizontal (2856x1280, columna central de 720 dp): vacío (desplazable) y Nuevo gasto. |

Notas: el snackbar "Gasto guardado (d/M/yyyy)" no quedó en una captura estática
(se comprobó por `gasto_guardado_aviso` en `GastoManualFlujoTest` y a ojo en una
pasada previa). La horizontal es un móvil girado, no una tablet real (no se usó
ningún dispositivo físico).

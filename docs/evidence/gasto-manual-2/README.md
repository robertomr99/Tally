# Evidencia de gasto-manual-2

Capturas reducidas (480 px de ancho; tablet 800 px) tomadas en `emulator-5556`
(Pixel_10_Pro, 1280x2856 @ 480 dpi) con `wm size 1080x2400` / `wm density 480`
(= 360 dp), `font_scale` 1,0 / 1,3 / 2,0 y, para la tablet, `wm size 2560x1600` /
`wm density 320`. Al terminar se restauraron `wm size`, `wm density` y
`font_scale` (1.0); la rotación no se tocó (`user_rotation` 0). Nada se instaló en
la Tab física y no se leyó ningún token.

La revisión visual de las imágenes la hizo solo el implementer; falta la revisión
humana.

## Antes (rama sin cambios)

| Fichero | Qué muestra |
| --- | --- |
| `antes-gastos-360.png` | Gastos a 360 dp: "+" suelto en la cabecera. |
| `antes-gastos-fs20.png` | Gastos a fontScale 2,0 con el "+" en la cabecera. |
| `antes-nuevo-gasto-360.png` | Nuevo gasto: campo "Total (con IVA)" y requisito "Indica un total mayor que 0". |

## Después

| Fichero | Qué muestra |
| --- | --- |
| `despues-gastos-360.png`, `despues-gastos-fs13.png`, `despues-gastos-fs20.png` | Gastos con tickets: botón "Añadir gasto" en la cabecera (360 dp) y en fila propia a ancho completo (1,3 y 2,0). |
| `despues-gastos-tablet.png` | Tablet: botón a la derecha del título, dentro de la columna centrada. |
| `despues-gastos-vacio-360.png`, `despues-gastos-vacio-fs20.png`, `despues-gastos-vacio-tablet.png` | Vacío: el botón de cabecera coexiste con "Escanear ticket" y "Añadir gasto manualmente". |
| `despues-gastos-filtro-vacio-360.png`, `...-fs13.png`, `...-fs20.png` | Vacío por filtro (Tarjeta): botón presente y "Ver todos". |
| `despues-detalle-total-automatico-360.png` | Detalle real en la app de un ticket con una línea: "Total" automático 4,50 con "Suma de las líneas · 1 artículo" y "Editar total a mano". |
| `despues-form-360-1.png` | Formulario (Efectivo): total 13,90, entregado 50,00, Cambio 36,10 de solo lectura con "Se calcula: entregado - total". |
| `despues-form-360-2.png` | Entregado 5,00: Cambio vacío y aviso "Lo entregado no cubre el total: faltan 8,90 €. Puedes guardar igualmente." |
| `despues-form-360-3.png` | Dos líneas (PAN 3 x 1,50, AGUA 1 x 2,00): Total 6,50 automático. |
| `despues-form-360-4.png` | Total tecleado 20,00 con una línea de 5,00: aviso de diferencia (15,00) y "Usar la suma de las líneas". |
| `despues-form-fs20-1..4.png` | Los mismos cuatro estados a fontScale 2,0. |

Lectura honesta:

- Las capturas `despues-form-*` NO son de la pantalla Nuevo gasto real: se
  generaron con una prueba instrumentada temporal (ya borrada) que pinta
  `FormularioTicketEditable` + `DockEdicion` con los reductores reales de dominio
  y un scroll al final, porque teclear en la pantalla real por `adb input` se
  rompía con el teclado (Gboard / entrada por voz). El resto (Gastos, Detalle) son
  de la app real.
- No hay "antes" a fontScale 1,3 ni en tablet, ni "antes" de Revisión/Detalle con
  cambio editable; el "antes" a 360 dp de Nuevo gasto muestra el campo "Total (con
  IVA)".
- Un E2E (`TotalCambioAutomaticosFlujoTest`) cubre en el emulador, sobre la app
  real, los escenarios 1, 2, 4, 5 y 7 de la spec con aserciones sobre semántica
  (`SetText` no definido en `revision_total` automático y en `revision_cambio`).

## Verificación ejecutada

- JVM: `gradlew.bat assembleDebug testDebugUnitTest :domain:expenses:test :test:unit:test`
  -> 527 tests, 0 fallos, 0 omitidos.
- Instrumentados en `emulator-5556`, suite sin `*RealTest` por paquetes
  (`am instrument -e package`): captura 13, diseno 6, gastos 37, navigation 3,
  resumen 11 OK; ajustes 8 con 1 fallo
  (`AjustesModeloFlujoTest.ajustesDesdeResumenMuestraModeloLocalNoDescargado`:
  `token_estado` fuera de pantalla porque el `wm size 1080x2400` que dejé puesto
  la hacía demasiado corta); aislada con el tamaño por defecto: `OK (3 tests)`.
- Un primer intento de suite completa se cortó con `INSTRUMENTATION_ABORTED: System
  has crashed` (el emulador perdió el activity manager: "Activity class does not
  exist"); se resolvió con `adb reboot` (sin wipe) y repitiendo por paquetes.
- Tests con tickets basura: `RevisionDatosFlujoTest` (guarda por la UI y no limpia;
  ahora además guarda un ticket de 4,50 por el total automático) y otros E2E de
  captura/procesado dejan tickets vacíos en la BD del emulador. Se borraron por la
  UI los de hoy antes de ejecutar la suite.

## Tags

Solo se retira `revision_total_recalculado`; se añaden `revision_total_manual`,
`revision_total_usar_suma` y `revision_aviso_total`. Se conservan `revision_*`,
`gastos_anadir` y `gastos_anadir_vacio`.

## No verificado

TalkBack; un ticket escaneado real con IA (el recálculo del cambio y el total
automático por extracción solo están en JVM y en E2E con tickets sembrados);
tablet física; el formulario completo dentro de la pantalla Nuevo gasto en tablet;
capturas "antes" a fontScale 1,3 y en tablet.

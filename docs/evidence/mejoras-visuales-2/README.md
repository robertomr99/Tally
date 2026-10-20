# Evidencia mejoras-visuales-2 (F2, sesión 063)

Entorno: `emulator-5556` (Pixel_10_Pro, API 37). Valores originales anotados antes de empezar y restaurados al final
(comprobado con `wm size`, `wm density`, `settings get`): `wm size` 1280x2856 (sin override), `wm density` 480,
`font_scale` 1.0, `user_rotation` 0, `accelerometer_rotation` 1. La tablet física no se tocó.

Configuraciones: `movil` (por defecto, ~427 dp), `360` (`wm size 720x1560` + `wm density 320`), `fs13`/`fs20` (360 dp con
`font_scale` 1.3 / 2.0) y `tablet` (`wm size 2560x1600` + `wm density 320`, 1280x800 dp apaisado; con rotación 0 el
tamaño ya es apaisado).

Cómo se tomaron: Resumen, Gastos, Detalle y Ajustes, a mano con `adb` sobre los datos reales del emulador. Los
estados de Escanear (Revisar foto, Revisar ticket) con un test instrumentado TEMPORAL que recorre el flujo real con la
cámara virtual (extracción vacía), y los estados que F1 dejó sin captura (Procesado en curso, Resultado/fallo, Gastos
vacío y cargando) renderizando los composables públicos con datos de ejemplo. Los dos tests temporales se eliminaron y
el APK de instrumentación se reinstaló sin ellos. Imágenes reducidas (480/800 px). Sin vídeos.

Los "antes" de las pantallas no tocadas por F2 son las capturas "después" de F1 en `../mejoras-visuales/`
(`despues-movil-ajustes`, `despues-movil-detalle-teclado`, `despues-fs20-revision-foto.jpg` con "Rep" cortado, etc.).
Los "antes" nuevos de este directorio se tomaron con un build del commit `1e627af` (F1).

## Pares antes / después

| Hallazgo | Antes | Después |
| --- | --- | --- |
| V12 selector de periodo a fuente grande | `antes-fs13-resumen`, `antes-fs20-gastos`, `antes-fs20-resumen` ("Personalizado" recortado, "Sema/na", "Me") | `despues-fs13-resumen`, `despues-fs20-resumen`, `despues-fs20-gastos` (dos filas, etiquetas completas, segmentos de 48 dp) |
| V13 tarjetas de Ajustes como las de Resumen | `../mejoras-visuales/despues-movil-ajustes` (radio 12, sin borde) | `despues-360-ajustes`, `despues-fs20-ajustes`, `despues-tablet-ajustes` (radio 24, borde, padding 20) |
| V14 títulos de sección | (código) | mismos pesos y `heading` en `despues-360-ajustes`, `despues-movil-revisar-ticket` ("Desglose de artículos" ya no es Bold) |
| V15 estados vacío/error/carga | `antes-fs20-gastos` (sin spinner al cargar) | `despues-movil-gastos-vacio`, `despues-fs13-gastos-cargando`, `despues-movil-fallo`, `despues-fs20-fallo` |
| V16 chips de categoría | (código) | `despues-movil-revisar-ticket`, `despues-movil-detalle` (mismo chip visual, área táctil de 48 dp) |
| V21 badge de motor dentro del scroll | `../mejoras-visuales/despues-fs20-detalle` | `despues-fs20-detalle`, `despues-movil-detalle` |
| V22 título de cabecera a 2 líneas | `../mejoras-visuales/despues-fs20-detalle` ("Detalle del ti...") | `despues-fs20-detalle` ("Detalle del / ticket") |
| V23 contraste de textos de 11 sp | (código: `outline` -> `onSurfaceVariant`) | `despues-movil-revisar-ticket`, `despues-fs20-procesando` |
| V24 icono Ajustes `Outlined` | `antes-fs13-resumen` (relleno) | `despues-fs13-resumen` (contorno) |
| "Repetir foto" cortado | `../mejoras-visuales/despues-fs20-revision-foto.jpg` ("Rep") | `despues-fs20-revisar-foto`, `despues-360-revisar-foto`, `despues-tablet-revisar-foto` |
| Detalle con cambios (dock) | - | `despues-fs20-detalle-cambios`, `despues-tablet-detalle-cambios` |
| Procesado en curso / Revisar ticket a 2,0 | - | `despues-fs20-procesando`, `despues-fs20-revisar-ticket` |

V17 (agrupación de `FilaRecibo`), V18 (`selectableGroup` de la barra inferior) y V25 (`Role.Button` en cámara) son
semántica sin cambio visual: no tienen captura; se comprueban por código y porque la suite sigue en verde.

## Qué se ve (lectura honesta del agente; revisión visual humana pendiente)

- Selector a 1,3 y 2,0: dos filas, "Día / Semana / Mes" arriba y "Personalizado" a ancho completo debajo, sin recortes.
  A cambio, en ese modo no se pinta el check del segmento activo (solo el fondo); no cabía en la fila de tres. En
  fuente normal sigue siendo una fila con check.
- Ajustes: las cuatro tarjetas (motor, modelo local, token y diagnóstico en debug) comparten forma, borde y padding con
  las de Resumen; el título de cada una es el mismo estilo.
- Revisar foto: a 1,3/2,0 los botones se apilan (Continuar primero). A 360 dp y fuente normal "Repetir foto" ya cabía
  en el dock tras igualar los pesos (con el peso 1:1,4 original salía "Repetir f...").
- "Detalle del ticket" a 2,0 baja a dos líneas en vez de cortarse.
- Gastos vacío: icono de 40 dp, titular, mensaje y botón con el mismo patrón que el estado de fallo; la carga de Gastos
  ya muestra el indicador (antes quedaba en blanco).

## No resuelto / visto en las capturas y fuera de alcance

- Fallo de Procesado a 2,0 (`despues-fs20-fallo`): "Probar con Local ·", "Volver a la foto" y "Reintentar" se cortan
  (botones de `ProcesadoContent` sin apilar) y la etiqueta "Proceso fallido en la nube (Gemini)" de la cabecera de
  proceso queda pegada al borde izquierdo. Mismo tipo de defecto que "Repetir foto" pero NO está en V12-V25 ni en el
  candidato pedido; se deja anotado como candidato para una siguiente pasada.
- "Promedio diario" / "0,00 € / día" se parten en tres líneas a 2,0 en la tarjeta de Resumen (no auditado).
- Diálogo "Eliminar modelo" de Ajustes: no capturado. Solo se muestra con el modelo descargado y fabricar el fichero
  habría alterado la disponibilidad del motor local en los E2E; `AjustesContent` es `internal` y no se puede pintar
  desde `:app`. La forma de 28 dp ya está comprobada en F1 (diálogo de borrar y DatePicker).
- Estado de error de captura (`captura_error`): composable privado, no capturado; comparte `EstadoPantalla` con el
  permiso y el vacío.
- Las transiciones y el comportamiento de TalkBack no se comprobaron en imagen.

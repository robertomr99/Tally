# Evidencia forma-pago (sesión 065)

Entorno: `emulator-5556` (Pixel_10_Pro, API 37). Solo se usó ese dispositivo (con `-s emulator-5556` y
`ANDROID_SERIAL`); la tablet y el móvil físicos no se tocaron. Valores originales (1280x2856, densidad 480,
`font_scale` 1.0) restaurados al final (`wm density reset`, `settings put system font_scale 1.0`; comprobado).

## Migración Room 2 a 3 con `install -r` sobre la BD real

1. Copia de seguridad fuera del repo (scratchpad de la sesión) de `tally.db`, `-wal` y `-shm` sacados con `run-as`
   antes de instalar. Estado v2: `PRAGMA user_version` = 2, 22 tickets, 11 líneas, 0 filas en `extracciones_log`.
2. `gradlew.bat ... :app:installDebug` (instala con `-r`, sin `pm clear`) y arranque en frío de la app (`am start -W`:
   `LaunchState: COLD`, `Status: ok`, sin `AndroidRuntime` en logcat; Room validó el esquema contra el hash de v3).
3. Copia de la BD tras el arranque: `user_version` = 3, 22 tickets, 11 líneas, `forma_pago = 'NO_INDICADA'` en los 22
   (`count(entregado)` = 0, `count(cambio)` = 0); los campos antiguos de `tickets` y `lineas` comparados fila a fila
   con la copia v2: 0 diferencias. `.schema tickets` muestra `forma_pago TEXT NOT NULL DEFAULT 'NO_INDICADA'`,
   `entregado REAL`, `cambio REAL` (idéntico al `createSql` de `framework/database/schemas/.../3.json`).

Tras ejecutar la suite E2E la BD queda con 24 tickets: los 22 originales (idénticos) más 2 residuales de los E2E
existentes (deuda conocida), todos `NO_INDICADA`.

## Tests

- JVM: `FormaPagoTest` (3), `PagoTest` (18), `TicketExtraidoParserPagoTest` (12, JSON de ejemplo de los 4 textos
  crudos y casos límite), `PagoUsecasesTest` (5), `TicketDetalleViewModelPagoTest` (4),
  `TicketExtractionPromptPagoTest` (4). Ningún test existente modificado salvo `MigrationTest` (ver PROGRESS).
- Instrumentados en `emulator-5556`: `:framework:database` 31/31 (incluye `MigracionFormaPagoTest` 3 y
  `MigrationTest` 2) y `:app` 48/48 con `-e notClass ExtraccionLocalRealTest,ExtraccionNubeRealTest` (incluye
  `FormaPagoFlujoTest` 3: chips del listado, selector y campos del detalle, aviso no bloqueante y persistencia).
- `ExtraccionNubeRealTest` NO se ejecutó (cuota de Gemini agotada desde 2026-10-17, consumiría cuota real) y
  `ExtraccionLocalRealTest` tampoco (necesita el modelo local, que no está en el emulador).

## Capturas

Tomadas con un test instrumentado TEMPORAL (siembra 3 tickets "Demo ...", navega con el flujo real y captura con
`UiAutomation.takeScreenshot`, reducidas a 540 px de ancho) y otro TEMPORAL que renderiza `PagoEnPapel` dentro de
`PapelTicket` con las tres formas. Ambos se eliminaron, los tickets "Demo" se borraron y el APK de instrumentación se
reinstaló sin ellos. Configuraciones: móvil (por defecto), 360 dp (`wm density 569`), letra 1,3 y 2,0
(`font_scale`). Los tickets de ejemplo no tienen foto ("Foto no disponible").

| Captura | Qué muestra |
| --- | --- |
| `01-listado-movil`, `02-listado-360dp`, `03-listado-letra-2-0` | Listado con Efectivo (chip menta con billete), Tarjeta (chip verde oscuro con tarjeta) y No indicada (sin chip); el importe de la fila es el total (13,90 €, nunca el entregado 50,00 €); en 360 dp y con letra grande el chip baja de línea |
| `04-detalle-efectivo-movil`, `07-detalle-efectivo-360dp` | Detalle en efectivo: selector con Efectivo activo, Entregado 50,00 y Cambio 36,10 bajo el total 13,90 |
| `05-detalle-tarjeta-movil`, `06-detalle-no-indicada-movil` | Tarjeta activa (campos ocultos) y No indicada activa |
| `08-selector-letra-1-3`, `09-selector-letra-2-0` | Selector apilado (> `UMBRAL_APILAR_BOTONES`) con icono + texto + check, 48 dp o más |
| `10-detalle-chip-letra-2-0` | Cabecera del detalle con letra 2,0: el chip "Tarjeta" baja de línea bajo el badge de motor (antes se cortaba "Tarje") |
| `11-papel-solo-lectura-movil`, `12-papel-solo-lectura-360dp` | Papel de solo lectura: filas Entregado y Cambio y "Pagado en efectivo" / "Pagado con tarjeta"; nada con No indicada |

Defectos encontrados y corregidos con estas capturas: (1) los chips de Efectivo y Tarjeta salían del mismo verde oscuro
con texto de poco contraste (`tertiary-container`/`primary-container`): ahora menta claro con texto oscuro y verde
bosque con texto blanco; (2) a 360 dp "Tarjeta" se partía en "Tarjet/a" dentro del selector en fila: ahora icono encima y
texto debajo; (3) a letra 2,0 el chip del detalle se cortaba junto al badge de motor: `FlowRow`.

Limitación: el papel de solo lectura de la captura 11/12 es `PagoEnPapel` dentro de `PapelTicket` (el resultado real
del procesado, `ResumenDeTicket`, es privado y la extracción real depende de la cuota de Gemini). El selector, los
campos y el listado son la app real.

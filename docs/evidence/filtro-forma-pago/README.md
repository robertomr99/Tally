# Evidencia: filtro-forma-pago

Verificado solo en `emulator-5556` (Pixel_10_Pro, 1280x2856 a 480 dpi). Ningun dispositivo fisico.

## Capturas

- `01-todos.png`: Gastos, periodo Mes, tres chips bajo el selector de periodo; Todos activo (check).
- `02-efectivo-vacio-filtro.png` / `03-tarjeta-vacio-filtro.png`: el emulador solo tiene tickets residuales
  sin forma de pago; al elegir Efectivo o Tarjeta aparece el vacio por filtro con "Ver todos" (flujo real,
  periodo con tickets pero ninguno cumple). `03` muestra Tarjeta.
- `04-fontscale-2-resolucion-1080.png`: 1080 px a 480 dpi (360 dp) y `font_scale` 2.0: los chips pasan a dos
  filas, sin recortes ni solapes con el selector.
- `05-tablet-800dp.png`: 1600x2560 a 240 dpi (800 dp): el control queda dentro de la columna de 720 dp.

No hay captura de la lista con tickets Efectivo/Tarjeta: el emulador no los tiene y no se siembran fuera del
test. Eso lo cubre el E2E (semilla propia).

## Tests

- JVM: `gradlew.bat assembleDebug testDebugUnitTest :domain:expenses:test`: FiltroFormaPagoTest 3,
  FiltrarTicketsPorFormaPagoTest 5, GastosViewModelTest 12 (5 nuevos).
- Instrumentados: `FiltroFormaPagoFlujoTest` (4: filtrar, periodo, detalle/Nuevo gasto/barra inferior, Resumen
  sin cambios) y `FiltroFormaPagoVacioTest` (3: vacio por filtro, vacio real, 360 dp a letra 2,0) -> OK (7).
  Suite `:app` completa con `-e notClass` de los *RealTest -> OK (63).
- La semilla del E2E (3 tickets de hoy, comercio `E2E filtro <uuid>`) se borra en `@After`.

## Restaurado

`wm size reset`, `wm density reset`, `font_scale` 1.0, `user_rotation` 0 (valores de partida).

## No verificado

Rotacion en dispositivo (ViewModel, sin prueba), tablet fisica, TalkBack, vacio por filtro con datos reales
sembrados (se usa estado fijo), `ExtraccionLocal/NubeRealTest`.

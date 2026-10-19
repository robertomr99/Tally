# Evidencia: rango-personalizado

Fecha: 2026-10-19 (sesion 049). Dispositivo: emulador `emulator-5556` (Pixel_10_Pro, API 36, 1280x2856 @480 dpi,
locale en-US). No se uso ningun dispositivo fisico.

## Comandos y resultados

- `.\gradlew.bat :domain:expenses:test --rerun --console=plain` -> BUILD SUCCESSFUL (tests nuevos de `Periodo`,
  `ObtenerPeriodo` y `CalcularResumenPeriodo` con rangos personalizados).
- `.\gradlew.bat :feature:resumen:testDebugUnitTest :feature:gastos:testDebugUnitTest :feature:common:testDebugUnitTest --rerun`
  -> BUILD SUCCESSFUL: `ResumenViewModelTest` 16, `GastosViewModelTest` 7, `EtiquetaPeriodoTest` 9,
  `FechasPickerTest` 3 (0 fallos).
- `.\gradlew.bat assembleDebug testDebugUnitTest --console=plain` -> BUILD SUCCESSFUL.
- `$env:ANDROID_SERIAL="emulator-5556"; .\gradlew.bat :app:installDebug :app:installDebugAndroidTest` -> Installed.
- `adb -s emulator-5556 shell am instrument -w -e class com.robertomr.tally.resumen.RangoPersonalizadoFlujoTest ...`
  -> `OK (2 tests)`.
- Suite completa (`-e notClass ...ExtraccionLocalRealTest,...ExtraccionNubeRealTest`) -> `OK (37 tests)`, 97,9 s.
  No se comprobo cuales (si alguno) se saltaron por `assumeTrue`; la salida de `am instrument` no los distingue.

## Decisiones tecnicas

- Material3 via Compose BOM 2026.02.01: `DatePickerDialog`, `DateRangePicker`, `rememberDateRangePickerState` y
  `SelectableDates` compilan SIN `@OptIn`; no se toco el BOM.
- Conversion del picker: `Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()` (`FechasPicker.kt`);
  `FechasPickerTest` prueba ida y vuelta con zona por defecto Madrid, Pacific/Auckland, Los_Angeles y UTC.
- Hallazgo: la forma `extraLarge` del tema es una pildora (9999 dp) y `DatePickerDialog` la heredaba (dialogo
  ovalado, boton "Aplicar rango" recortado y no pulsable). Se fija `shape = RoundedCornerShape(28.dp)`.
- E2E: las celdas de dia se localizan por su texto accesible (`"Thursday, October 1, 2026"`, depende del idioma)
  casando el numero de dia como token suelto y el anio; la primera coincidencia es el mes en curso. Elegida por ser
  estable en este emulador; no hizo falta el modo de entrada de texto.
- E2E con linea base leida del repositorio + sembrado (bordes dia 1 a las 00:05 y hoy a las 23:50, con captura distinta;
  fuera: dia anterior y manana) y limpieza en `@After`. Los tickets residuales de 0,00 EUR del emulador NO se tocaron.
- Selector con 4 segmentos y pesos (0,8/1,1/0,8/1,6), padding 6 dp: a 360 dp (1080x2400 @420 dpi) cabe, incluso con
  "Personalizado" seleccionado (check + texto) - `05-selector-360dp.png`. Densidad restaurada con `wm reset`.

## Capturas

- `02-picker-marcado.png`: picker con 1-12 Oct marcado.
- `03-resumen-rango.png`: Resumen con `1 Oct - 12 Oct` (sin gastos en el tramo en este emulador).
- `04-gastos-rango.png`: Gastos con el mismo rango.
- `05-selector-360dp.png`: selector a 360 dp con Personalizado activo.

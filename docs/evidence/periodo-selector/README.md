# Evidencia periodo-selector (2026-10-18)

Solo emulador `emulator-5556` (`Pixel_10_Pro(AVD)`); `emulator-5554` estaba offline y no se uso ningun
dispositivo fisico. No se leyo ni imprimio el token de Hugging Face. Sin `connected*`.

## JVM
`gradlew.bat :domain:expenses:test :feature:resumen:testDebugUnitTest --rerun` y despues
`gradlew.bat assembleDebug testDebugUnitTest` -> BUILD SUCCESSFUL.
- `PeriodoTest` 11/11 (DIA, semana lun/dom, semana que cruza mes 27-abr..3-may-2026 y anio
  29-dic-2025..4-ene-2026, febrero 28/29, meses de 30/31, 31-dic/1-ene, `desde > hasta` rechazado).
- `ObtenerPeriodoTest` 4/4 (`Clock.fixed` en Europe/Madrid: 2026-03-29 y 2026-10-25 a 00:30/01:30/03:30/23:30,
  las dos 02:30 del solape de octubre y la noche del 31-dic).
- `ObservarTicketsDelPeriodoTest` 3/3 (bordes inclusivos 00:00 del `desde` y 23:59 del `hasta`; emision vs
  captura; reactividad al guardar).
- `ResumenViewModelTest` 5/5 (Mes por defecto, alternar Dia/Semana/Mes cambia rango, recuento y suma,
  filtro por emision, actualizacion sola al guardar), `EtiquetaPeriodoTest` 5/5.

## Instrumentado (`am instrument`, sin connected*)
- `$env:ANDROID_SERIAL="emulator-5556"; .\gradlew.bat :app:installDebug :app:installDebugAndroidTest`.
- `-e class com.robertomr.tally.resumen.PeriodoSelectorFlujoTest` -> OK (2 tests): siembra hoy / hace 3 dias /
  hace 40 dias (+ emitido hoy capturado hace 40 dias y al reves) con el repositorio real, Mes activo por
  defecto, alterna Mes -> Dia -> Semana -> Mes comprobando rango, recuento y suma (linea base leida del
  repositorio + sembrado); y guardar un ticket con el Mes activo actualiza la cifra sola. `@After` borra los
  tickets sembrados.
- Suite `:app` con `-e notClass ...ExtraccionLocalRealTest,...ExtraccionNubeRealTest` -> OK (20 tests).
- Capturas `periodo-selector-dia.png`, `-semana.png`, `-mes.png`; `uiautomator dump` tras tocar cada segmento:
  solo el tocado queda `selected="true"` y la etiqueta cambia a `18 Oct` / `12 Oct – 18 Oct` /
  `1 Oct – 31 Oct`. Revision visual de las imagenes: pendiente de revision humana.

## Observaciones
- La base del emulador conserva 6 tickets con total 0,00 que dejan otros E2E existentes (p. ej.
  `RevisionDatosFlujoTest` guarda tickets), por eso las capturas muestran `0,00 €` / `6 tickets`;
  `PeriodoSelectorFlujoTest` lo tiene en cuenta con lineas base.
- Se anade `TicketRepositoryEntryPoint` (main de `:app`) para que el E2E use el repositorio real, mismo
  patron que `TextEngineEntryPoint`.

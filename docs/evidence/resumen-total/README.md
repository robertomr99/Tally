# Evidencia resumen-total (2026-10-18)

Solo emulador `emulator-5556` (`Pixel_10_Pro(AVD)`, 1280x2856); `emulator-5554` offline y ningun
dispositivo fisico usado. Sin `connected*`. No se leyo ni imprimio el token de Hugging Face.

## JVM (BUILD SUCCESSFUL: `assembleDebug testDebugUnitTest`)
- `PeriodoTest` 15 (4 nuevos de `anterior()`: dia 1-mar/29-feb-2028/1-ene, semana 27-abr y cruce de anio,
  mes mar->feb 28/29, ene->dic, may->abr 30, tipo conservado y encadenado sin huecos).
- `CalcularResumenPeriodoTest` 12 (escenarios 1-5: 60,50 / 3 / +51,25 %; anterior vacio y ambos vacios con
  variacion null; actual vacio -100 %; promedio mes en curso, primer/ultimo dia, periodo terminado, hoy < desde,
  semana en miercoles, dia).
- `ObservarResumenDelPeriodoTest` 2 (Turbine; Clock fijo Europe/Madrid 2026-10-25).
- `ResumenViewModelTest` 6 (adaptado a `ResumenUiState.resumen` + comparativa por tipo),
  `FormatoResumenTest` 6 (`1.428,50 €`, `46,08 € / día`, `-4,2 %`, `+51,3 %`, `0,0 %`, null).
- Nota: en la primera ejecucion `:feature:escanear:testDebugUnitTest` fallo una vez (test inestable ajeno ya
  conocido); reejecutado con `--rerun` y en la suite completa paso.

## E2E (emulator-5556)
`$env:ANDROID_SERIAL="emulator-5556"; .\gradlew.bat :app:installDebug :app:installDebugAndroidTest` y
`adb -s emulator-5556 shell am instrument -w -e notClass ...ExtraccionLocalRealTest,...ExtraccionNubeRealTest
com.robertomr.tally.test/androidx.test.runner.AndroidJUnitRunner` -> `OK (23 tests)`
(`ResumenTotalFlujoTest` 3, `PeriodoSelectorFlujoTest` 2, `NavigationTest` 3, resto de regresion).
`ResumenTotalFlujoTest` siembra 10,00 + 20,50 hoy y 40,00 en el mes anterior, calcula las expectativas desde la
linea base del repositorio y borra lo sembrado en `@After`.

## Capturas (siembra temporal para que el total del mes sea exactamente 1.428,50 €, ya borrada)
- `resumen-total-mes.png`: `1.428,50 €`, badge `-4,1 % vs período anterior` (icono de bajada, sin rojo/verde),
  `79,36 € / día` (dias transcurridos), `9 tickets`.
- `resumen-total-dia.png`: mismo total, badge neutro `Sin datos del período anterior` (ayer sin gasto),
  `1.428,50 € / día`.
- `resumen-total-semana.png`: rango `12 Oct – 18 Oct`, `204,07 € / día` (1.428,50 / 7: hoy es domingo 18, asi que
  han transcurrido los 7 dias de la semana).

## tnum / fuente
Los binarios de `inter_regular.ttf` e `inter_semibold.ttf` contienen la etiqueta de feature `tnum`
(`grep -c tnum` = 1 en ambos), asi que la fuente empaquetada la soporta. Todos los textos numericos de la
tarjeta usan `EstiloTabular` (`fontFeatureSettings = "tnum"`). El agente no puede certificar visualmente que
los digitos sean tabulares: queda para revision humana de las capturas. El formato `1.428,50 €` (separador de
miles `.` con 4 digitos) sale correcto en Android/ICU (captura y texto de uiautomator).

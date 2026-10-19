# Evidencia listado-gastos (2026-10-18)

Solo emulador `emulator-5556` (1280x2856); `emulator-5554` offline, ningun dispositivo fisico, sin `connected*`.
No se leyo ni imprimio el token de Hugging Face.

## JVM
`.\gradlew.bat assembleDebug testDebugUnitTest :test:unit:test :domain:expenses:test --rerun-tasks` -> BUILD SUCCESSFUL
(una pasada previa fallo en `EscanearViewModelTest.cameraFailureShowsErrorStateWithoutSaving`, modulo no tocado; la
repeticion completa fue verde, test aparentemente intermitente). Conteo JUnit: `AgruparTicketsPorDiaTest` 7,
`ObservarListadoDelPeriodoTest` 3, `GastosViewModelTest` 4, `ResumenViewModelTest` 11 (+2), `EtiquetaDiaTest` 6,
`EtiquetaPeriodoTest` 5 (movido a `:feature:common`), `FormatoResumenTest` 6, `CategoriaUiContrasteTest` 2,
`SegmentosDonutTest` 5.

## E2E (emulator-5556)
`$env:ANDROID_SERIAL="emulator-5556"; .\gradlew.bat :app:installDebug :app:installDebugAndroidTest` y
`adb -s emulator-5556 shell am instrument -w -e notClass ...ExtraccionLocalRealTest,...ExtraccionNubeRealTest
com.robertomr.tally.test/androidx.test.runner.AndroidJUnitRunner` -> `OK (34 tests)`, 2 saltados por assumption:
`CapturaFlujoTest.denegadoMuestraEstadoInAppSinCrash` (preexistente) y
`ListadoGastosFlujoTest.periodoSinTicketsMuestraElVacioEnGastosYEnResumen` (la base del emulador tiene 18 tickets
hoy, todos con total 0,00, asi que el periodo Dia no esta vacio). El vacio de Gastos se cubre con
`ListadoGastosVacioTest` (2/2, estado fijo sin ViewModel) y con `GastosViewModelTest`; el vacio de Resumen no
tiene E2E en este emulador.
`ListadoGastosFlujoTest` 6/6 (lista exacta del dia con orden y total del dia calculados desde linea base,
reactividad al guardar y alternar periodo, total <= 0 aparece sin sumar, tocar fila en Gastos y Resumen ->
`pantalla_detalle_ticket` y Atras, Resumen con 5 filas y "Ver todos (N)" -> Gastos). Regresion verde:
`ResumenTotalFlujoTest`, `PeriodoSelectorFlujoTest`, `GraficaDonutFlujoTest`, `NavigationTest`,
`RevisionDatosFlujoTest`. Los tests limpian lo sembrado en `@After`.
Una primera ejecucion de `ListadoGastosFlujoTest` fallo por dos carreras del propio test (asercion inmediata del
texto "Ver todos" y cabecera de dia fuera de la lista perezosa) y por un cierre de actividad que no llego a
`DESTROYED`; corregidos los dos primeros, el tercero no se repitio en 4 ejecuciones posteriores.

## Capturas (datos sembrados temporalmente y borrados despues)
- `01-gastos-tres-dias.png`: pestana Gastos, Mes, cabecera "Hoy, 18 de octubre" con total y filas.
- `02-resumen-recientes.png`: Resumen con "Gastos del periodo" (5 filas con fecha corta) y "Ver todos (24)".
Pendiente: captura del vacio y de varios dias (los datos reales del emulator solo tienen hoy).

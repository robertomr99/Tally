# Evidencia: seed-demo (datos de demo, solo build debug)

Dispositivo: `emulator-5556` (Pixel_10_Pro AVD, 1280x2856 @480). Nada en dispositivos fisicos.

## Que se ve y donde
Ajustes (engranaje de Resumen) -> al final, debajo de "Diagnostico de extracciones": tarjeta "Datos de demo"
con contador "Tickets de demo: n" y los botones "Sembrar datos de demo" / "Quitar datos de demo" (ambos piden
confirmacion). Solo en build debug; en release la seccion no existe.

## Capturas
- `01-ajustes-sembrado.png` seccion tras sembrar y repetir ("Ya estaban los 13 tickets de demo").
- `02-gastos-dia.png`, `03-gastos-mes.png` Gastos con tickets de demo (Dia / Mes).
- `04-resumen-donut.png` Resumen (Mes) con donut de 6 categorias.
- `05-detalle-ticket-malo.png` "Comercio ilegible" (0,00, Otros) en Detalle; `06-detalle-corregido.png` tras editar comercio/total/categoria.
- `fs2-360dp-01-ajustes-sembrado.png` 360 dp (wm size 1080x2400 @480) y fontScale 2,0: botones y texto legibles, sin truncar.
- `tablet-01-ajustes-sembrado.png` 1600x2560 @240: respeta ColumnaCentrada.
Capturadas con `SembrarDemoFlujoTest` y `-e evidencia <prefijo>`; wm size/density y font_scale restaurados despues (1280x2856, 480, 1.0, rotacion 0).
En Resumen salen 33 tickets en el mes porque el emulador conserva tickets residuales de otros E2E; los de demo son 13.

## Comandos
- JVM: `gradlew.bat assembleDebug testDebugUnitTest :domain:expenses:test :test:unit:test` verde (`:feature:ajustes` 46 tests, 27 nuevos).
- Instalacion solo con `ANDROID_SERIAL=emulator-5556` (`:app:installDebug :app:installDebugAndroidTest`).
- E2E: `adb -s emulator-5556 shell am instrument -w -e class com.robertomr.tally.ajustes.SembrarDemoFlujoTest com.robertomr.tally.test/androidx.test.runner.AndroidJUnitRunner` -> OK (3 tests).
- Suite completa: `... -e notClass ...ExtraccionLocalRealTest,...ExtraccionNubeRealTest ...` -> OK (81 tests), 2251 s.
- Release: `gradlew.bat :app:assembleRelease` OK (app-release-unsigned.apk). Escaneo de los 5 classes*.dex: sin `GeneradorDatosDemo`, `DatosDemoViewModel`, `QuitarDatosDemo`, `SembrarDatosDemo` ni la cadena `tally-demo`; solo aparece el nombre del stub vacio `SeccionDatosDemo`. El APK debug si los contiene (control positivo).

## Aislamiento
`SembrarDemoFlujoTest` siembra un ticket real "Super Barrio" (mismo comercio y fecha que D01), y en `@After` comprueba que
los tickets sin marca son identicos (id, campos, lineas) tras sembrar/repetir/quitar/editar el malo, y borra solo lo marcado.

## No verificado
Modo avion/sin red en el emulador, TalkBack, revision visual humana, build release instalada y ejecutada, Tab fisica.

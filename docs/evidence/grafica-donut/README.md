# Evidencia grafica-donut (2026-10-18)

Solo emulador `emulator-5556` (`Pixel_10_Pro(AVD)`, 1280x2856); `emulator-5554` offline y ningun dispositivo
fisico usado. Sin `connected*`. No se leyo ni imprimio el token de Hugging Face.

## JVM
`.\gradlew.bat assembleDebug testDebugUnitTest :app:assembleDebugAndroidTest` -> BUILD SUCCESSFUL.
Conteo JUnit (XML): `CalcularDistribucionCategoriasTest` 11, `ObservarDistribucionDelPeriodoTest` 2,
`SegmentosDonutTest` 5, `CategoriaUiContrasteTest` 2, `ResumenViewModelTest` 9 (3 nuevos; adaptado a
`ResumenUiState.distribucion` y a los estados intermedios de `combine`), 0 fallos.

## Contraste (D1, medido en `CategoriaUiContrasteTest`)
| Categoria | color | sobre tarjeta | sobre pista | icono blanco |
| --- | --- | --- | --- | --- |
| ALIMENTACION | `#B86A22` (antes `#C87D32`) | 3,71 | 3,18 | 4,10 |
| HOGAR | `#5D6E3F` | 5,04 | 4,32 | 5,57 |
| TRANSPORTE | `#4A6B82` | 5,11 | 4,38 | 5,65 |
| OCIO | `#7A587A` | 5,42 | 4,64 | 5,99 |
| SALUD | `#2E776E` | 4,77 | 4,09 | 5,28 |
| OTROS | `#5F6561` | 5,39 | 4,62 | 5,96 |
El ambar original no llegaba a 3:1; se oscurecio solo ese color, tal como preveia D1.

## E2E (emulator-5556)
`$env:ANDROID_SERIAL="emulator-5556"; .\gradlew.bat :app:installDebug :app:installDebugAndroidTest` y
`adb -s emulator-5556 shell am instrument -w -e notClass ...ExtraccionLocalRealTest,...ExtraccionNubeRealTest
com.robertomr.tally.test/androidx.test.runner.AndroidJUnitRunner` -> `OK (27 tests)`, con
`GraficaDonutFlujoTest` 4 (distribucion con importes y % que suman 100, alternar periodo, vacio, guardado reactivo).
Siembra con el repositorio real, expectativas desde linea base y limpieza en `@After`.
Nota: el test de vacio usa `assumeTrue` (si hoy hay gasto en la base queda cubierto en JVM).

## Capturas (siembra temporal con un test desechable ya borrado; datos eliminados)
- `grafica-donut-vacio.png`: periodo Dia sin gasto.
- `grafica-donut-2-categorias.png`: Alimentacion y Hogar.
- `grafica-donut-6-categorias.png`: 6 categorias (44/23/17/11/5/<1 %, suman 100), leyenda y centro "Mayor gasto".
La captura queda en el viewport superior: el desglose con barras queda bajo el pliegue y se verifico por tags en el E2E.

## Pendiente humano
Revision visual de la paleta y de las capturas, y simulacion de daltonismo (Ajustes > Accesibilidad >
Correccion de color: deuteranomalia/protanomalia). El agente no puede certificarlo.

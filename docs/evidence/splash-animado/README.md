# Evidencia splash-animado

Entorno: `emulator-5556` (Pixel_10_Pro AVD, `sdk_gphone16k_x86_64`), **API 37**
(`ro.build.version.sdk` = 37). El camino API 26-30 (compat sin animar, hold 0)
**no se ejecuto en dispositivo**: solo revision de codigo y recursos
(`ic_splash_tally` completo como estado estatico, `SplashPolitica` devuelve false
por debajo de 31, cubierto en el test JVM).

## Capturas (extraidas de `screenrecord`, 720x1600, fotogramas cada ~26 ms)

- `01-animacion-emblema-frio.png`: arranque en frio, recorte del centro. Se ve
  fondo `primary` aparecer, las 4 barras dibujandose escalonadas y despues la
  diagonal hasta el emblema completo. La cadencia del video fue regular
  (~35-40 fps en el emulador), animacion de ~0,8 s incluyendo el fundido de entrada.
- `02-escala-animacion-0.png`: con `animator_duration_scale` = 0 el emblema
  aparece ya completo, sin dibujarse. Secuencia completa: launcher, splash,
  fundido del sistema, Resumen (sin pantalla blanca ni segundo splash).

## Observaciones honestas

- En este emulador el arranque en frio tarda ~2,1-2,5 s hasta el primer frame de la
  app, asi que el splash completo (emblema ya dibujado) se queda ~1,5 s quieto
  esperando a la app: es el tiempo real de proceso/Hilt del emulador, no un hold
  artificial (el hold maximo es 700 ms y ya habia vencido). Tras el splash, Resumen
  aparece con su propio estado de carga ("—") unos fotogramas antes de los datos
  (comportamiento previo de Resumen, no tocado).
- Un punto breve al inicio de cada trazo es la cabeza del trazo redondeado
  empezando a dibujarse; no es un artefacto aparte.
- No se pudo juzgar fluidez perceptiva a mano alzada; solo por regularidad de
  fotogramas del video del emulador.

## Tiempos `am start -W` (TotalTime, ms, arranque en frio tras `force-stop`)

- Linea base (build anterior al splash, instalado): 2106, 2088, 2090.
- Con splash (hold 700 ms): 2157, 2482, 2120; build final 2319, 2411, 2519.
  Diferencia ~+0,2-0,4 s (ruido del emulador incluido), dentro del margen de 800 ms.
- Con escala 0: 2097, 2225, 2113.
- Relanzado en caliente (HOME + `am start -W`): `Status: ok`, sin splash largo.
  Rotacion (user_rotation 1): la actividad se recrea y sigue siendo la de
  `MainActivity` (hold 0 por `savedInstanceState != null`, no medido en ms).

## Verificacion ejecutada

- `gradlew.bat assembleDebug testDebugUnitTest :app:installDebug :app:installDebugAndroidTest`
  (ANDROID_SERIAL=emulator-5556): BUILD SUCCESSFUL; `SplashPoliticaTest` 6/6.
- `am instrument -w -e notClass ExtraccionLocalRealTest,ExtraccionNubeRealTest ...`
  en emulator-5556: `OK (45 tests)`, sin tocar los tests.
- `animator_duration_scale` restaurado a 1.0; rotacion restaurada (user_rotation 0,
  accelerometer_rotation 1).

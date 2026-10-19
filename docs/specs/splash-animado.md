# Spec: splash-animado

## Source feature

- id: `splash-animado` | area: arranque | depends_on: `navegacion-base` (accepted) | status: not_started
- Título: Splash screen con animación al inicio.
- Comportamiento: al abrir la app se muestra un splash con animación de marca (sin parpadeos ni pantalla en blanco) que da paso a Resumen; API nativa de SplashScreen en Android 12+ y comportamiento equivalente en versiones anteriores.
- Petición del usuario (2026-10-19): "un splash screen con animación al inicio".

## Goal / Non-goals

Goal: arranque en frío con splash de marca (emblema "tally marks" de `docs/design/stitch/01-splash/`: 4 barras + diagonal, `primary-fixed` #BFEDD2 sobre `primary` #002718) cuyo icono se dibuja con una animación corta, sin doble splash, sin pantalla blanca y sin cambiar la navegación.

Non-goals: pantalla Compose de splash propia ni ruta de navegación nueva; esperar a Room/Hilt/OCR/modelos; texto o eslogan en el splash; tema oscuro; icono de launcher adaptativo (hoy no hay `android:icon`; fuera de alcance, ver D7); cambios en `:domain:*`, `:framework:*` o `:feature:*`; módulos Gradle nuevos; `seed-demo`.

## Job story

Cuando abro Tally desde el launcher, quiero ver la marca animarse un instante y aterrizar directamente en Resumen, para sentir que la app arranca de forma cuidada y sin saltos visuales.

## Acceptance scenarios

1. Given arranque en frío en emulator-5556, When se lanza `MainActivity`, Then se ve fondo `primary` con el emblema dibujándose (barras y diagonal) y a continuación Resumen, sin fotograma blanco/gris intermedio ni segundo splash.
2. Given Android 12+, Then el splash es el del sistema (no hay segunda pantalla de marca tras él). Given API 26-30, Then la compat muestra el mismo fondo y el emblema estático completo, sin parpadeo.
3. Given la escala de animación del sistema = 0 (reducir animaciones), Then el emblema aparece ya completo y no hay espera artificial (hold = 0).
4. Given arranque en frío normal, Then el hold artificial máximo es 700 ms (D3) y `am start -W` no empeora en más de ~800 ms respecto a la línea base sin splash.
5. Given la app ya abierta, When roto el dispositivo, vuelvo desde recientes o relanzo en caliente con el proceso vivo, Then no hay hold artificial adicional (D4).
6. Given los E2E existentes (`NavigationTest` y resto de `app/src/androidTest`), Then siguen en verde sin modificarlos; ninguna animación infinita en Resumen.
7. Given el splash, Then no contiene información que solo se transmita por animación (no hay texto ni datos).

## Repository research (inspeccionado)

- `AGENTS.md`, `DESIGN.md` (secciones colores, accesibilidad, Don't: "NO inventar animaciones más allá de las del mockup 06" -> esta feature es petición explícita del usuario; hay que enmendar esa línea, ver Durable docs), `docs/design/stitch/01-splash/code.html` y `screen.png` (fondo `primary`, 4 barras redondeadas 10x80 dp aprox. + diagonal -28 grados, color `primary-fixed`, sin texto; reloj/wifi/píldora son decoración de la captura, no se copian), `docs/specs/bootstrap-gradle.md` (solo cita el mockup como referencia del placeholder).
- `app/src/main/AndroidManifest.xml`: `Application` con `android:theme="@android:style/Theme.Material.Light.NoActionBar"`, sin `android:icon`; `MainActivity` sin tema propio.
- `app/src/main/java/com/robertomr/tally/MainActivity.kt`: `@AndroidEntryPoint`, `ComponentActivity`, `setContent { TallyTheme { TallyApp() } }`; sin `installSplashScreen`.
- `app/src/main/java/com/robertomr/tally/navigation/TallyApp.kt`: un `NavHost` con `startDestination = TallyRoute.Resumen.route`; Resumen ya es el destino inicial (no se toca).
- `app/` NO tiene directorio `res/` (sin themes.xml, drawables ni icono): hay que crearlo.
- `gradle/libs.versions.toml`: minSdk 26, targetSdk 36, compileSdk 36; `coreKtx = 1.18.0`; no existe `core-splashscreen`. `app/build.gradle.kts` usa `libs.androidx.core.ktx`; no tiene `testImplementation` (no hay `app/src/test`).
- `feature/common/.../theme/Color.kt`: `ForestPrimary 0xFF002718`, `ForestPrimaryFixed 0xFFBFEDD2` (fuente de los colores del splash). La app solo tiene `lightColorScheme` (DESIGN.md).
- `app/src/androidTest/**`: 16 tests E2E (NavigationTest, flujos...). No inspeccionado el contenido de cada uno; usan Compose test sobre `MainActivity`/semántica.
- No inspeccionado: API level de emulator-5556 (el implementer lo registra con `adb -s emulator-5556 shell getprop ro.build.version.sdk`).

## Decisiones (con default)

- D1 API: `androidx.core:core-splashscreen` frente a SplashScreen nativa sola. DEFAULT: la librería compat. Justificación: minSdk 26 < 31, la nativa solo existe en 12+ (y en 12 el splash por defecto sin la compat es un icono de launcher sin control de fondo en <31, con "pantalla blanca" por `windowBackground`); la compat unifica tema, `setKeepOnScreenCondition` y `postSplashScreenTheme` en todas las versiones y en 12+ delega en la API nativa. Versión: entrada `coreSplashscreen` en `[versions]` + alias `androidx-core-splashscreen` en `[libraries]` (`gradle/libs.versions.toml`, nunca literal en `build.gradle.kts`); usar la última estable compatible con compileSdk 36 (1.2.x si resuelve; si exige compileSdk > 36 bajar a 1.0.1 y anotarlo, como ya hace el repo con otras libs). Se añade `implementation(libs.androidx.core.splashscreen)` solo en `:app`.
- D2 Qué es la animación. DEFAULT: AVD vectorial (`AnimatedVectorDrawable`) como `windowSplashScreenAnimatedIcon`, SIN pantalla Compose propia. Justificación: una pantalla Compose tras el splash del sistema produciría doble splash (el sistema ya pinta fondo + icono), duplicaría ruta/estado en `:app` y retrasaría Resumen y los E2E; con el AVD el contenido de Resumen ya está compuesto tras el splash. Alternativa rechazada: pantalla Compose (solo valdría para <31 con animación, a costa de lo anterior).
  - Animación: `trimPathEnd` 0->1 sobre trazos con `strokeLineCap=round` en `primary-fixed`: 4 barras escalonadas (~120 ms cada una, solape de ~60 ms) y después la diagonal (~250 ms). Total <= ~800 ms (el sistema limita el AVD del splash a 1000 ms). Una sola pasada, sin bucle. El estado final (sin animar) es exactamente el emblema del mockup 01 completo.
  - Geometría: viewport 288x288 dp; el emblema debe caber dentro del círculo central de 192 dp (zona visible sin fondo de icono); trazo barras ~10 dp, diagonal a -28 grados; proporciones del mockup, no inventar elementos extra.
  - API 26-30: la compat muestra el drawable estático (sin animar); por eso el icono debe verse completo como VectorDrawable/AVD en su estado final. Aceptado.
- D3 Duración y salida. DEFAULT: NO se espera a Hilt/Room/estado inicial (Resumen ya gestiona su propio Loading; esperar a Room podría colgar el arranque). `setKeepOnScreenCondition` mantiene el splash solo hasta que pasen `SPLASH_MIN_MS = 700` desde el inicio de `onCreate` (para que se vea la animación en arranques rápidos), techo duro 700 ms = hold máximo artificial (queda muy por debajo de 1-1,5 s). En API < 31 (no hay animación) hold = 0. Si la app tarda más de 700 ms en el primer frame, el splash dura lo que tarde el sistema, sin añadir nada.
- D4 Arranque en frío/caliente/rotación. DEFAULT: hold solo si `savedInstanceState == null` Y es la primera creación de `MainActivity` en este proceso (flag en un `object`/companion de `:app`, p. ej. `SplashEstado.yaMostrado`). Rotación y recreación -> `savedInstanceState != null` -> hold 0; vuelta desde recientes con proceso vivo -> flag ya puesto -> hold 0. El splash nativo de 12+ que el sistema pueda pintar en arranque en caliente no se controla y es el del sistema (no se anima más de lo debido porque el hold es 0).
- D5 Reducir animaciones. DEFAULT: leer `Settings.Global.ANIMATOR_DURATION_SCALE` (`Float`, default 1f) con `Settings.Global.getFloat(contentResolver, ..., 1f)`; si es 0f -> hold 0 (el AVD del sistema salta a su estado final por sí mismo). Lógica pura en una función `SplashPolitica.debeMantener(esPrimeraCreacion, sdkInt, escalaAnimacion, transcurridoMs): Boolean` testeable en JVM.
- D6 Tema/fondo. DEFAULT: fondo fijo `primary` #002718 (la app no tiene tema oscuro; el splash tampoco varía con modo oscuro). Estilo `Theme.Tally.Starting` (parent `Theme.SplashScreen`) con `windowSplashScreenBackground`, `windowSplashScreenAnimatedIcon`, `windowSplashScreenAnimationDuration` (~800) y `postSplashScreenTheme` = nuevo `Theme.Tally` (parent `android:Theme.Material.Light.NoActionBar`, mismo que hoy). Colores en `app/src/main/res/values/colors.xml` con comentario "espejo de ForestPrimary/ForestPrimaryFixed en Color.kt" (XML no puede leer Compose; duplicación aceptada y documentada). `android:theme` del `<application>` pasa a `Theme.Tally`; el `<activity>` MainActivity usa `Theme.Tally.Starting`. Barras del sistema: no se cambia el comportamiento actual (sin edge-to-edge nuevo).
- D7 Icono de launcher. DEFAULT: fuera de alcance (no existe hoy). Se anota como seguimiento posible: reusar el emblema como adaptive icon. No tocar `android:icon`.
- D8 Accesibilidad. El splash del sistema no tiene contentDescription ni texto y no se anuncia con TalkBack; no hay información exclusiva de la animación (el emblema es decorativo de marca). Respeta D5. Contraste #BFEDD2 sobre #002718 alto.
- D9 Pruebas automáticas. DEFAULT: test unitario JVM de `SplashPolitica` (`app/src/test/...`, añadir `testImplementation(libs.junit)` en `app/build.gradle.kts`, alias ya existe). No se añade E2E nuevo del splash (no se puede asertar la ventana de splash con Compose test; justificación en Verification). Los E2E existentes no se tocan.

## Expected file changes

- `gradle/libs.versions.toml`: versión + alias `androidx-core-splashscreen`.
- `app/build.gradle.kts`: `implementation(libs.androidx.core.splashscreen)`; `testImplementation(libs.junit)`.
- `app/src/main/AndroidManifest.xml`: temas (aplicación y actividad).
- `app/src/main/res/values/colors.xml`, `values/themes.xml` (nuevos).
- `app/src/main/res/drawable/ic_splash_tally.xml` (VectorDrawable del emblema) y `drawable/avd_splash_tally.xml` + `res/animator/*.xml` si hace falta (objectAnimator de `trimPathEnd`).
- `app/src/main/java/com/robertomr/tally/MainActivity.kt`: `installSplashScreen()` ANTES de `super.onCreate`, `setKeepOnScreenCondition { SplashPolitica.debeMantener(...) }`, `setContent` sin cambios.
- `app/src/main/java/com/robertomr/tally/SplashPolitica.kt` (nuevo, función pura + flag de primera creación) y `app/src/test/java/com/robertomr/tally/SplashPoliticaTest.kt`.
- Sin cambios en `di.kt`, `TallyApp.kt`, `TallyRoute.kt`, dominio ni features.

## Durable documentation impact

- `DESIGN.md`: update (enmendar "NO inventar animaciones..." para incluir la animación del splash pedida por el usuario; documentar tokens/colores del splash y que el diseño del splash es solo el emblema). Lo hace el orquestador/implementer (el planner solo escribe en `docs/specs/`).
- `AGENTS.md`: not needed. `docs/architecture.md`/ADR: not needed (sin decisión arquitectónica nueva; librería de UI de sistema). `CONTEXT.md`: not needed.
- `PROGRESS.md` y `feature_list.json`: los actualiza el orquestador con la evidencia.

## Implementation plan / tasks (en orden)

1. T1 Catálogo: añadir versión y alias en `libs.versions.toml`; dependencia en `:app`; `gradlew.bat :app:dependencies` o assemble para confirmar resolución con compileSdk 36.
2. T2 Recursos: colores, VectorDrawable (estado final = mockup 01), AVD con trimPathEnd escalonado, `Theme.Tally.Starting` y `Theme.Tally`.
3. T3 Manifest: aplicar los temas (sin `android:icon`).
4. T4 `SplashPolitica` + test JVM (casos: primera creación con escala 1 y 0, <31, recreación, transcurrido >= 700, techo).
5. T5 `MainActivity`: `installSplashScreen()` antes de `super.onCreate`; condición con `SystemClock.uptimeMillis()`; marcar flag al terminar; `setContent` intacto.
6. T6 Verificación (abajo) y actualización de `DESIGN.md`.

## Verification plan

Comandos base: `gradlew.bat assembleDebug testDebugUnitTest --console=plain` (o `bash init.sh`) en verde; el test de `SplashPolitica` incluido.

E2E: ejecutar la suite existente SOLO en emulator-5556 con `am instrument` (por ejemplo `adb -s emulator-5556 shell am instrument -w com.robertomr.tally.test/androidx.test.runner.AndroidJUnitRunner` tras instalar los APK debug y test con `adb -s emulator-5556 install -r`); NUNCA `connected*`, NUNCA `pm clear`; emulator-5554 está offline y la tablet no se toca. Sin cambios en los tests; si alguno falla por el hold de 700 ms, la mitigación es reducir el hold, no tocar el test.

Manual en emulator-5556 (registrar API level):
1. Cierre en frío: `adb -s emulator-5556 shell am force-stop com.robertomr.tally`; `adb -s emulator-5556 shell am start -W -n com.robertomr.tally/.MainActivity` (anotar `TotalTime`/`WaitTime`, 3 repeticiones). Comparar con línea base previa (git stash del cambio o commit anterior) para confirmar <= ~800 ms de diferencia.
2. Grabación: `adb -s emulator-5556 shell screenrecord --time-limit 5 /sdcard/splash.mp4` en paralelo al `am start`; `adb pull` a `docs/evidence/` o ruta que use el repo para evidencia (o capturas seriadas con `adb exec-out screencap -p > splash_N.png` en bucle si no hay herramienta para extraer fotogramas). Comprobar: fondo verde -> emblema se dibuja -> Resumen; sin fotograma blanco ni segundo splash.
3. Reducir animaciones: `adb -s emulator-5556 shell settings put global animator_duration_scale 0`, repetir 1-2 y confirmar emblema completo sin espera; restaurar con `settings put global animator_duration_scale 1.0` al terminar (obligatorio).
4. Rotación y reapertura en caliente (`input keyevent HOME` y relanzar): sin hold ni splash largo.
5. Si solo hay un emulador API >= 31, declarar explícitamente que el camino API 26-30 no se ejecutó en dispositivo (solo revisión de código/recursos) y ponerlo en la evidencia.

Evidence: salida de `am start -W`, vídeo/capturas del arranque (con y sin escala 0), resultado de la suite `am instrument` en 5556, resultado de `testDebugUnitTest`, nota de API level del emulador.

## Validator checklist

- `core-splashscreen` solo en `libs.versions.toml`; ninguna versión literal en `build.gradle.kts`; sin módulos nuevos; dominio/framework/features intactos; un `di.kt` por módulo sin cambios.
- Una sola implementación de splash (sistema/compat), sin pantalla Compose ni ruta extra; `startDestination` sigue siendo Resumen.
- Hold <= 700 ms, 0 con escala de animación 0, 0 en rotación/caliente; sin condición que espere a Room/Hilt.
- Emblema fiel al mockup 01 (sin reloj/wifi/píldora ni texto), colores = tokens Forest, animación una pasada.
- Sin animaciones infinitas; E2E sin modificar y en verde en 5556; ningún `pm clear` ni `connected*`; escala de animación restaurada.
- `DESIGN.md` actualizado; evidencia (vídeo/capturas + tiempos) registrada.

## Risks / unknowns

- R1: en API 26-30 el icono no se anima (limitación de la compat); aceptado en D2.
- R2: el overlay `SplashScreenView` durante el hold podría interferir con E2E que interactúan en los primeros 700 ms (poco probable con semántica Compose); mitigación en Verification.
- R3: versión de `core-splashscreen` vs compileSdk 36 (ver D1).
- R4: AVD de trimPath en splash del sistema es sensible a `windowSplashScreenAnimationDuration`; calibrar contra captura real.
- R5: la grabación en emulador puede no capturar el splash fielmente en el primer fotograma; usar `screenrecord` + capturas.
- Spec dentro del tamaño objetivo; feature bien dimensionada (una pantalla/ventana, sin modelo de datos).

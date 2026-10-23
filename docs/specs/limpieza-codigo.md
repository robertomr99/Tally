# Spec: limpieza-codigo

## Metadatos de la feature
- id: `limpieza-codigo` | area: `base` | status: `not_started` | depends_on: `[]`
- Titulo: Limpieza de codigo: imports, dependencias y comentarios sin valor.
- Plan aprobado por el usuario: `C:\Users\Roberto\.claude\plans\quiero-que-hagas-una-compressed-seal.md`.
- Comportamiento visible: ninguno. Refactor de higiene sin cambio de logica.

## Objetivo y no-objetivos
Objetivo: eliminar imports, dependencias/entradas de catalogo y codigo sin consumidores, y comentarios que se pudren (sesion N / PROGRESS.md / etiquetas de tarea / KDoc vacios), sin alterar comportamiento ni resultado visual.

No-objetivos (anotar como deuda en PROGRESS.md, no tocar): `build-logic/`; modulo `:domain:core` vacio; `GemmaMediaPipeSpike` en main; renombrar `di.kt` (lo exige AGENTS.md); `onAvisoVisto()`; `totalRecalculado()` vs `importeDeLinea`; visibilidad `internal` en `framework/database`; etiquetas `spec/R-/mockup` restantes (~385); etiquetas `T#` en codigo de `src/main` salvo que sean puro ruido en cabecera. Sin modulos ni dependencias nuevos. Sin cambios en tests salvo imports y comentarios.

## Escenarios de aceptacion
1. Given el arbol limpio, When se ejecuta `bash init.sh`, Then assembleDebug y testDebugUnitTest pasan.
2. Given los modulos de dominio, When `gradlew.bat :domain:expenses:test :test:unit:test`, Then verde y el numero de tests ejecutados es >= 527 (mismo que antes).
3. Given los tests instrumentados, When `gradlew.bat assembleDebugAndroidTest`, Then compila (valida imports de androidTest y retirada de `androidx-test-rules`/redundancias).
4. Given el build de release, When `gradlew.bat :app:assembleRelease`, Then termina bien (o falla igual que en la linea base si ya fallaba; registrarlo).
5. Given los greps finales (ver Verificacion), Then no quedan los 15 imports, ni "sesion N"/"PROGRESS" en codigo/toml/xml, ni referencias colgantes a entradas borradas.
6. Given los paquetes tocados, When se ejecutan sus instrumentados en `emulator-5556`, Then pasan igual que antes (sin `*RealTest`).

## Investigacion del repo (verificada por el planner; la auditoria era estatica)
Leido: AGENTS.md, CLAUDE.md, plan, `feature_list.json`, `gradle/libs.versions.toml`, todos los `build.gradle.kts` relevantes, `build-logic/**` (grep), `themes.xml`, `Type.kt`, `FormatoUi.kt`, listas de usos por grep. No se compilo nada.

Correcciones a la auditoria / hallazgos:
- Imports sin usar: confirmados 15, pero en resumen el reparto real es: `SeccionRecientes.kt` -> solo `FontWeight`; `TarjetaTotal.kt` -> `BorderStroke`, `Surface`; `TarjetaDistribucion.kt` -> `BorderStroke`, `Surface`. En `TarjetaTotal`/`TarjetaDistribucion` `FontWeight` SI se usa: no borrarlo. Total = 3+1+1+2+1+2+5 = 15. `ProcesadoContent.kt`: `fillMaxHeight` (l.18) y `entities.Linea` (l.63); `height` se queda. `FormularioTicketEditable.kt` y `RevisionFotoContent.kt`: `layout.height` (l.14 y l.12); `heightIn` se queda. `GastosScreen.kt`: `layout.Arrangement` (l.4) y `ui.Alignment` (l.31). `ProcesadoFlujoTest.kt`: l.3 PackageManager, l.13 AndroidJUnit4, l.20 RunWith. `AnadirGastoBotonFlujoTest.kt` l.6 `Composable`.
- `androidx-test-rules`: ningun `.kt` usa `GrantPermissionRule` ni `androidx.test.rules`; solo `app/build.gradle.kts:75` (+ comentario l.74) y toml (`androidxTestRules` l.52 con su comentario l.50-51, libreria l.180 con comentario l.179).
- `material-icons-extended` en `:feature:ajustes`: `feature/ajustes/build.gradle.kts:24-25`; ningun fichero de ese modulo importa `androidx.compose.material.icons`. SE QUEDA en `resumen`, `gastos`, `common`, `escanear` (no tocar; ni la entrada del catalogo ni su comentario l.144-146).
- Catalogo: `build-logic/build.gradle.kts:8` usa `libs.android.gradle.plugin` (SE QUEDA, junto con `agp`) y `build-logic` usa por `findLibrary` solo hilt-android/hilt-compiler/androidx-compose-*. Sin consumidores: `kotlin-gradle-plugin`, `kotlin-compose-gradle-plugin`, `ksp-gradle-plugin`, `hilt-gradle-plugin` (toml l.80-83) y `androidx-lifecycle-runtime-ktx` (l.133; `lifecycle` se queda via `-runtime-compose`). El comentario l.78 "(los usa build-logic)" pasa a describir solo el artefacto AGP. Las entradas de `[plugins]` NO se tocan (el `build.gradle.kts` raiz las usa).
- Redundancias `androidTestImplementation` (androidTest hereda `implementation` del mismo modulo): `framework/database/build.gradle.kts` l.43 `coroutines.core`, l.45 `room.runtime`, l.46 `room.ktx`; `framework/ai/build.gradle.kts` l.43 `coroutines.core`. Mantener `turbine`, `room.testing`, `androidx.test.*`, `junit`. Si alguna rompe la compilacion de androidTest, restaurarla.
- Cifras tabulares: `Type.kt:109` `TextStyle.tabularNumbers()` sin llamadas. `FormularioTicketEditable.kt:668-669` `cifrasTabulares()` privado SIN llamadas (borrar con su KDoc). `AjustesScreen.kt:534` `cifrasTabulares()` privado con 1 uso (l.305). `EstiloTabular` (`FormatoUi.kt:7`) es el unico a conservar: ya lo usan resumen y `ListadoTicketsUi`. `PapelTicket.kt:151` combina fuente monoespaciada y tnum: NO se toca.
- Comentarios "sesion N"/"PROGRESS": toml l.31, 35, 41-42, 61, 66; `CapturaFlujoTest.kt` l.37 y l.59; `PhotoFileStoreTest.kt:130`; `EscanearViewModel.kt:839`; `MlKitOcrEngineTest.kt:34`. (Total 12 lineas de 5 ficheros.)
- `themes.xml`: el texto obsoleto esta en la linea 3 (no la 4): "el mismo que se usaba antes de esta feature".
- Etiquetas `T#`: 47 apariciones en 34 ficheros de test, 9 en 4 de main. Politica abajo.

## Enfoque tecnico
Cuatro bloques independientes, compilando tras cada uno. Cero cambios de logica. No borrar `@Provides/@Binds/@Inject/@Preview` ni nada referenciado por anotacion/KSP.
Consolidacion de cifras tabulares: sustituir `X.cifrasTabulares()` en `AjustesScreen.kt` por `X.merge(EstiloTabular)` (import `com.robertomr.tally.feature.common.EstiloTabular`; el modulo ya depende de `:feature:common`), borrar los dos helpers privados y `tabularNumbers()`, y limpiar el import de `TextStyle` si queda sin uso. Resultado equivalente (`fontFeatureSettings = "tnum"`).

## Cambios esperados de ficheros
- Imports: los ficheros listados arriba.
- `gradle/libs.versions.toml`, `app/build.gradle.kts`, `feature/ajustes/build.gradle.kts`, `framework/database/build.gradle.kts`, `framework/ai/build.gradle.kts`.
- `feature/common/.../theme/Type.kt`, `FormularioTicketEditable.kt`, `feature/ajustes/.../AjustesScreen.kt`.
- Comentarios: ficheros de la seccion anterior, cabeceras de test con `spec x, T#`, `app/src/main/res/values/themes.xml`.
- No tocar: `build-logic/`, `AGENTS.md`, docs de arquitectura (no necesitan cambio). `PROGRESS.md` y `feature_list.json` los actualiza el orquestador (deuda fuera de alcance incluida).

## Politica de comentarios
Quitar: referencias a sesion/PROGRESS.md; etiquetas de tarea `spec x, T#` en cabeceras de test; KDoc que solo repiten el nombre del simbolo; texto obsoleto de themes.xml. Conservar los que explican un porque (Espresso 3.7.0 vs 3.5.0, navigation-compose/compileSdk, App Check debug provider, `pm grant`, etc.), aunque citen spec/ADR; al quitar la cita "sesion 008 de PROGRESS.md" se deja el motivo tecnico. Duda -> conservar. No reescribir en masa los `T#` de `src/main`.

## Tareas
1. Medir linea base: `gradlew.bat :domain:expenses:test :test:unit:test` y anotar el numero de tests (esperado 527); `git status` limpio salvo `feature_list.json`/`docs/resumen-para-slides.md` ya pendientes.
2. Quitar los 15 imports. Compilar (`assembleDebug assembleDebugAndroidTest`).
3. Catalogo y dependencias: antes de borrar, grep por alias en todos los `*.kts` y `build-logic/**` (nombre con puntos y con guiones). Aplicar bajas de la seccion de investigacion. Compilar con `assembleDebug assembleDebugAndroidTest :app:assembleRelease`.
4. Codigo: borrar `tabularNumbers()` y `cifrasTabulares()` (x2), usar `EstiloTabular` en `AjustesScreen.kt`. Compilar.
5. Comentarios segun politica. Compilar.
6. Verificacion completa (abajo) y registro de evidencia.

## Verificacion
- `bash init.sh` (o `gradlew.bat assembleDebug testDebugUnitTest --console=plain`).
- `gradlew.bat :domain:expenses:test :test:unit:test`: >= 527 tests.
- `gradlew.bat assembleDebugAndroidTest` y `gradlew.bat :app:assembleRelease`.
- `git diff`: solo imports, dependencias/catalogo, los tres helpers y comentarios; sin hunks de logica; `git diff --stat -- build-logic` vacio.
- Greps finales (0 resultados): `[Ss]esi[oó]n \d|PROGRESS` en `*.kt,*.kts,*.toml,*.xml`; `tabularNumbers|cifrasTabulares`; `androidx-test-rules|androidxTestRules|androidx\.test\.rules`; `kotlin-gradle-plugin|kotlin-compose-gradle-plugin|ksp-gradle-plugin|hilt-gradle-plugin|androidx-lifecycle-runtime-ktx`; `libs\.(androidx\.lifecycle\.runtime\.ktx|androidx\.test\.rules)`. Ademas: cada fichero de los 15 imports ya no contiene esas lineas `import`.
- Instrumentados en `emulator-5556` solo de paquetes tocados, p. ej. `gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.package=com.robertomr.tally.resumen"` y lo mismo para `.gastos`, `.captura`, `.ajustes` (usa AjustesScreen); excluir `*RealTest`. Si el emulador no esta disponible, registrarlo como limitacion (la compilacion cubre lo estatico).

## Evidencia a capturar
Conteo de tests antes/despues, salida de cada comando, resultado de los greps, `git diff --stat`, lista de KDoc/etiquetas eliminados, entradas de catalogo borradas y resultados instrumentados.

## Checklist del validator
- [ ] 15 imports fuera y ninguno usado eliminado; compila.
- [ ] Solo las entradas/dependencias listadas se borraron; `agp`, `lifecycle`, `android-gradle-plugin` y `material-icons-extended` en los otros 4 modulos intactos; `build-logic/` sin diff.
- [ ] Un unico estilo tabular (`EstiloTabular`); sin cambio visual en Ajustes.
- [ ] Comentarios con porque conservados; ninguna referencia sesion/PROGRESS.
- [ ] Tests >= 527, init.sh, androidTest, release verdes; instrumentados pasan.
- [ ] Deuda fuera de alcance anotada en PROGRESS.md por el orquestador.

## Riesgos
- Quitar una dependencia que algo usa solo transitivamente (androidTest, `:app:assembleRelease`): mitigar compilando tras cada bloque y restaurando si falla.
- Borrar un comentario con razon tecnica: ante la duda, conservar.
- Etiquetas `T#` son subjetivas: limitar a cabeceras de test.
- Release puede requerir firma/credenciales; si falla igual que antes, no es regresion.

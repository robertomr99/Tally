# Spec: mejoras-visuales-3

## Source feature

- id: `mejoras-visuales-3` | area: diseno | status: not_started | depends_on: `mejoras-visuales-2` (accepted). Rama `correcciones-visuales`.
- Título: "Mejoras visuales (3): fuentes grandes y deuda de accesibilidad". Entrada de `feature_list.json` leída entera (notes y verification incluidas).
- Origen: 3 seguimientos del validator de `mejoras-visuales-2` (sesión 064) + una pasada de revisión de lo añadido después (`forma-pago`, `filtro-forma-pago`, `gasto-manual`, `galeria-escaneo`), leída en el código real. Continúa la numeración de `docs/specs/mejoras-visuales.md` (V01-V39) con `W01-W12`.
- Tamaño: correcto para una sola sesión (12 cambios, todos S, sin dominio ni framework). Lo que exige decisión de diseño del usuario queda diferido (ver "Diferido").

## Goal / Non-goals

Goal: que Tally siga siendo legible y operable con fuente al doble de tamaño y que lo añadido tras `mejoras-visuales-2` use los mismos componentes y reglas que el resto (`ColumnaCentrada`, `CabeceraPantalla`, `PADDING_PANTALLA`, `UMBRAL_APILAR_BOTONES`, `EstadoPantalla`/`TarjetaSeccion`/`TituloSeccion`), con el estado activo nunca solo por color.

Non-goals: V26-V30 de `mejoras-visuales.md` (campos solo con subrayado, snackbar de confirmación, transición interna de Escanear, dock en móvil horizontal, aviso al salir de la Revisión); tema oscuro; `WindowSizeClass`/rail; módulos, dependencias o versiones nuevas; `:domain:*` y `:framework:*`; cambiar tags existentes; cambiar comportamiento o copy funcional salvo los textos listados en W04 y W07. Las features no se importan entre sí: todo lo compartido va en `:feature:common`.

## Job story

Cuando uso Tally con la fuente del sistema al doble (o en 360 dp), quiero ver completos los botones, textos y el estado seleccionado de cada control, para poder usar toda la app sin que nada se corte, se pegue al borde o dependa de distinguir un color.

## Hallazgos (pasada de revisión)

`common` = `feature/common/src/main/java/com/robertomr/tally/feature/common/`. Evidencia = `fichero:línea` leído o captura de `docs/evidence/mejoras-visuales-2/`. Dec.: HACER / VERIFICAR (se captura; solo se toca si la captura muestra defecto) / DIFERIR.

| Id | Pantalla | Hallazgo | Evidencia | Dec. |
| --- | --- | --- | --- | --- |
| W01 | Selector de periodo (Resumen, Gastos) en dos filas | El segmento activo solo se distingue por el fondo `secondaryContainer`: `conCheck = false` en las 4 llamadas (no cabe un check en la fila de tres) | `common/PeriodoSelector.kt:61-79`, `despues-fs20-resumen.png` | HACER |
| W02 | Fallo de Procesado a fontScale 2,0 | "Probar con Local", "Volver a la foto" y "Reintentar" cortados (botones con `height(48.dp)` fijo y `weight(1f)` en una `Row`), cuerpo `EstadoPantalla` con `weight(1f)` sin scroll (se aplasta) y etiqueta "Proceso fallido en..." sin margen lateral, pegada al borde | `escanear/ProcesadoContent.kt:247-324,346-349,396-453`, `despues-fs20-fallo.png` | HACER |
| W03 | Resumen, `TarjetaTotal` a 2,0 | "Promedio diario" y "0,00 € / día" en 3 líneas: dos `MicroMetrica` con `weight(1f)` en `Row` dentro de una tarjeta con 20 dp de padding y 12 dp internos (~110 dp de texto a 360 dp) | `resumen/TarjetaTotal.kt:62-75` | HACER |
| W04 | Nuevo gasto | `DockEdicion` reutilizado tal cual: muestra "Total recalculado 0,00 € / Calculado a partir de 0 artículos" aunque el usuario haya escrito un total (la referencia es la suma de líneas) y el botón dice "Guardar cambios" aunque no hay nada guardado | `gastos/NuevoGastoScreen.kt:182-188`, `common/FormularioTicketEditable.kt:485-602` | HACER |
| W05 | `FiltroFormaPagoSelector` | Sin `selectableGroup()` (el selector de pago de la revisión y la barra inferior sí lo tienen); a 2,0 los chips deben bajar de fila sin recortes (el check sustituye al icono: no solo color, correcto) | `common/FiltroFormaPagoSelector.kt:44-72`, `FormularioTicketEditable.kt:701-720` | HACER (grupo) + VERIFICAR (2,0) |
| W06 | Gastos vacío / vacío por filtro | A 360 dp y 2,0 la cabecera + selector de dos filas + chips ocupan ~360 dp: el icono, titular, mensaje y los dos botones (o "Ver todos") quedan bajo el pliegue y solo se llega desplazando el cuerpo del vacío | `gastos/GastosScreen.kt:97-148,181-248` (el scroll vive solo dentro del vacío) | HACER |
| W07 | Detalle de un gasto manual | El diálogo de borrado dice "...junto con sus líneas y su foto" aunque un gasto manual no tiene foto | `gastos/TicketDetalleScreen.kt:413-452`, `domain/.../Ticket.kt:40` (`esManual`) | HACER |
| W08 | Escanear: permiso de cámara / error de captura / galería | `captura_permitir` y `captura_reintentar` con `height(48.dp)` fijo; los botones del permiso (Permitir + Elegir de la galería) son de ancho distinto y no apilados a ancho completo como en el vacío de Gastos; los dos estados sin scroll | `escanear/EscanearScreen.kt:262-315,358-384` | HACER |
| W09 | Detalle "ya no existe" | `detalle_volver` con `height(48.dp)` fijo | `gastos/TicketDetalleScreen.kt:322-341` | HACER |
| W10 | Chips de forma de pago (`ChipFormaPago`) en `FilaTicket` y detalle | Chip de 11 sp con padding vertical de 2 dp y `maxLines = 1` sin elipsis; ya viven en `FlowRow` | `common/FormaPagoUi.kt:87-112`, `common/ListadoTicketsUi.kt:165-181` | VERIFICAR |
| W11 | Cámara: botón Galería | Etiqueta "Galería" a 11 sp, blanco al 80 % sobre la vista previa viva; el hit-target es de 48 dp (ok) | `escanear/CameraEncuadre.kt:352-378` | VERIFICAR (solo si la captura la muestra ilegible) |
| W12 | Estados desplazables duplicados | El patrón `BoxWithConstraints + verticalScroll + heightIn(min = alto)` está copiado en los dos vacíos de Gastos y falta en permiso/error/fallo | `gastos/GastosScreen.kt:184-209,216-247` | HACER (se extrae a `common/ComponentesSeccion.kt`, ver W06/W08) |

## Diferido (decisión de diseño del usuario o fuera de alcance; NO tocar)

- V26 campos del papel solo con subrayado; V27 snackbar de confirmación tras guardar/borrar (el aviso `gasto_guardado_aviso` ya existe; no se unifica aquí); V28 transición interna de Escanear; V29 dock en móvil horizontal; V30 aviso al salir de la Revisión con la barra inferior.
- No verificables hoy (se declaran en el informe): diálogo "Eliminar modelo" (exige modelo descargado), `captura_error` real, TalkBack, transiciones, `ExtraccionNubeRealTest`/`ExtraccionLocalRealTest`, cámara real de papel.

## Acceptance scenarios

1. Given el selector de periodo en modo dos filas (fontScale > 1,1) con cualquier segmento activo, When se mira, Then el segmento activo tiene un borde de 2 dp en `onSecondaryContainer` (contraste no textual >= 3:1 contra `surfaceContainerLow`) además del fondo; los inactivos no tienen borde; en modo una fila sigue el check actual. Semántica `selected`/`Role.Tab` y tags `periodo_*` intactos.
2. Given el fallo de Procesado a 360 dp y fontScale 2,0, Then "Probar con Local", "Volver a la foto", "Reintentar" (y "Volver al resultado anterior" si existe) se leen completos, sin recorte, en botones apilados (Reintentar primero) de >= 48 dp con `PADDING_PANTALLA`, el cuerpo (titular, mensaje, nota) se desplaza si no cabe y la etiqueta "Proceso fallido en..." queda centrada con margen lateral de `PADDING_PANTALLA` y puede envolver. A fontScale <= 1,15 el diseño es el actual (una fila 1:1).
3. Given la tarjeta de total a 2,0, Then "Promedio diario" y "Tickets" se apilan en una columna (cada una a ancho completo) y la etiqueta cabe en una línea a 360 dp; a fuente normal y 1,3 sigue en fila (el implementer baja el umbral a 1,15 solo si a 1,3 la etiqueta envuelve en la captura).
4. Given Nuevo gasto, Then el botón dice "Guardar gasto" (tag `revision_guardar` intacto) y el bloque "Total recalculado ..." solo aparece cuando hay al menos una línea; sin líneas el dock tiene solo los dos botones. Revisión de datos y Detalle conservan "Guardar cambios" y el bloque siempre visible.
5. Given el filtro de forma de pago, Then el contenedor `filtro_pago` es un `selectableGroup` y a 360 dp / 2,0 los tres chips se ven completos en una o dos filas.
6. Given Gastos sin tickets en el periodo, o sin coincidencias por filtro, a 360 dp y fontScale 2,0, Then "Escanear ticket" / "Añadir gasto manualmente" / "Ver todos" se alcanzan con un solo gesto de desplazamiento (selector y chips dentro del mismo scroll con fontScale > 1,15) y quedan completos; a fuente normal el diseño no cambia.
7. Given el diálogo de borrado de un gasto manual, Then el texto es "Se eliminará «X» por N € junto con sus líneas. Esta acción no se puede deshacer."; en un ticket con foto sigue "...sus líneas y su foto...".
8. Given el permiso de cámara denegado o el error de captura a 2,0, Then los botones miden >= 48 dp sin altura fija, van apilados a ancho completo (Permitir cámara, Elegir de la galería) y el estado se desplaza si no cabe; tags `captura_permitir`, `permiso_galeria`, `captura_reintentar` intactos.
9. Given "Este ticket ya no existe" a 2,0, Then "Volver" no se corta.
10. Given los chips de forma de pago y el botón Galería a 2,0 y en tablet horizontal, Then no hay recortes (si las capturas muestran un defecto se corrige con el cambio mínimo y se anota; si no, se deja intacto y se registra "verificado sin cambios").
11. Given la suite JVM y `:app`, Then siguen en verde sin debilitar ni modificar aserciones existentes; ningún tag se elimina ni cambia.

## Repository research

Inspeccionado: `AGENTS.md`, `CLAUDE.md`, `PROGRESS.md` (cabecera; el log de sesiones solo parcialmente), `feature_list.json` (entradas `mejoras-visuales`, `-2`, `-3`, `forma-pago`, `filtro-forma-pago`, `gasto-manual`, `galeria-escaneo`), `docs/specs/mejoras-visuales.md`, `docs/evidence/mejoras-visuales-2/README.md`, `DESIGN.md` (Layout, Components), y el código completo de `PeriodoSelector`, `FiltroFormaPagoSelector`, `LayoutAncho`, `ComponentesSeccion`, `CabeceraPantalla`, `FormaPagoUi`, `ListadoTicketsUi` (FilaTicket), `FormularioTicketEditable` (dock, sección de pago), `GastosScreen`, `NuevoGastoScreen`, `TicketDetalleScreen`, `TarjetaTotal`, `ProcesadoContent`, `EscanearScreen`, `CameraEncuadre` (controles), `GastoManualFlujoTest` (cabecera) y `FiltroFormaPagoVacioTest` (patrón de test con `fontScale = 2f` vía `LocalDensity`). No inspeccionado: imágenes (no se pueden ver; las "capturas" de esta spec son para revisión humana), `RangoFechasDialog`, `AjustesScreen`, `PapelTicket`, `RevisionFotoContent`, `FotoOriginal`, el resto de E2E, `docs/domain-model.md`.

Hechos: `UMBRAL_APILAR_BOTONES = 1.15f` y `PADDING_PANTALLA = 20.dp` ya existen; `PeriodoSelector` usa `UMBRAL_APILAR_SELECTOR = 1.1f` (privado); `ComponentesSeccion.kt` ya aloja `TarjetaSeccion`, `TituloSeccion`, `EstadoPantalla`, `CargandoPantalla`. Tests de `RevisionDatosFlujoTest` aseveran `revision_total_recalculado` ("0,00" sin líneas) en la Revisión: ese comportamiento NO cambia. Ningún test asevera los textos "Guardar cambios" ni "y su foto".

## Technical approach

- W01: en `Segmento`, parámetro `conBorde: Boolean = false`; en el modo apilado se pasa `true` y, si `activo`, se añade `.border(2.dp, MaterialTheme.colorScheme.onSecondaryContainer, RoundedCornerShape(50))` (orden: `clip` -> `background` -> `border` -> `clickable`). Sin tocar los pesos ni la fila normal.
- W02: en `ExtraccionFallidaContent`: cuerpo central (`EstadoPantalla` + `BotonOtroMotor` + "Volver al resultado anterior") dentro de `EstadoPantallaDesplazable` / `Column(weight(1f).verticalScroll)`; fila inferior fija con `apilar = fontScale > UMBRAL_APILAR_BOTONES`: `Column` (Reintentar primero, `fillMaxWidth`) o `Row` actual; todos los botones `heightIn(min = 48.dp)` en vez de `height(48.dp)` (incluido `procesado_reintentar_otro` y `BotonCancelar` si procede). `CabeceraDeProceso`: `padding(horizontal = PADDING_PANTALLA)` y `textAlign = Center` en la etiqueta `procesado_proceso`. Tags intactos. Cuidado: no tocar el cuerpo de `ProcesandoExtraccionContent`/`ExtraccionListaContent` salvo el padding de `CabeceraDeProceso`, que se comparte.
- W03: en `TarjetaTotal`, si `LocalDensity.current.fontScale > UMBRAL_APILAR_METRICAS` (const privada = 1,5f) usar `Column(spacedBy(12.dp))` con las dos `MicroMetrica` a ancho completo; si no, la `Row`. Tags `resumen_promedio`, `resumen_num_tickets` intactos.
- W04: `DockEdicion(..., textoGuardar: String = "Guardar cambios", mostrarTotalRecalculado: Boolean = true)`; `ContenidoDock` oculta la fila del total (y el subtítulo apilado) si es `false`. `NuevoGastoContenido` pasa `textoGuardar = "Guardar gasto"` y `mostrarTotalRecalculado = ui.datos.lineas.isNotEmpty()`. Valores por defecto = comportamiento actual (Revisión y Detalle no cambian).
- W05: `.selectableGroup()` en el `FlowRow` de `FiltroFormaPagoSelector` (import `androidx.compose.foundation.selection.selectableGroup`).
- W06/W12: nueva `EstadoPantallaDesplazable(titulo, modifier, ..., contenido)` en `ComponentesSeccion.kt` = `BoxWithConstraints` (recibe el `modifier`, que lleva el tag) + `EstadoPantalla` con `verticalScroll` y `heightIn(min = maxHeight)`. `EstadoVacio` y `EstadoVacioFiltro` la usan (mismos tags `listado_vacio`/`listado_vacio_filtro`). Con `fontScale > UMBRAL_APILAR_BOTONES`, en `GastosContenido` los dos vacíos (no la lista ni la carga) se renderizan dentro de un único `Column(weight(1f).verticalScroll)` que incluye `PeriodoSelector` y `FiltroFormaPagoSelector` (en ese caso el `EstadoPantalla` interno no lleva scroll propio). Si la captura previa muestra que ya cabe, se omite esta rama y se registra. Nota: mover el `PeriodoSelector` entre ramas reinicia su `pickerAbierto`; es inocuo (el diálogo se cierra al aplicar).
- W07: `internal fun textoConfirmarBorrado(nombre: String, total: String, esManual: Boolean): String` en `TicketDetalleScreen.kt` (o fichero propio de `:feature:gastos`); `ConfirmarBorrado` recibe `esManual = ui.ticket.esManual`. Título y botones iguales.
- W08/W09: `EstadoPantallaDesplazable` en `PermisoCamaraContent` y `ErrorContent`; botones `fillMaxWidth().heightIn(min = 48.dp)`; `NoExiste`: `heightIn(min = 48.dp)`.
- W10/W11: solo captura. Candidatos de arreglo si se confirma defecto: `ChipFormaPago` -> `overflow = Ellipsis`/padding vertical 4 dp (no cambiar el tamaño de texto); `BotonGaleria` -> mismo fondo semitransparente detrás de la etiqueta que usa la pildora "Alinea los bordes...".

## Expected file changes

Editar: `common/PeriodoSelector.kt`, `common/FiltroFormaPagoSelector.kt`, `common/FormularioTicketEditable.kt` (solo `DockEdicion`/`ContenidoDock`), `common/ComponentesSeccion.kt` (+`EstadoPantallaDesplazable`), `resumen/TarjetaTotal.kt`, `gastos/NuevoGastoScreen.kt`, `gastos/GastosScreen.kt`, `gastos/TicketDetalleScreen.kt`, `escanear/ProcesadoContent.kt`, `escanear/EscanearScreen.kt`; condicionales (W10/W11): `common/FormaPagoUi.kt`, `escanear/CameraEncuadre.kt`. Crear: `feature/gastos/src/test/.../TextoConfirmarBorradoTest.kt`, `app/src/androidTest/java/com/robertomr/tally/diseno/FuentesGrandesFlujoTest.kt`, `app/src/androidTest/.../gastos/NuevoGastoDockFlujoTest.kt` (o añadir métodos al final de `GastoManualFlujoTest` sin tocar los existentes), `docs/evidence/mejoras-visuales-3/README.md` + imágenes. Antes de crear cualquier fichero, comprobar que no existe uno equivalente. No tocar `:domain:*`, `:framework:*`, manifest, `libs.versions.toml`, `build-logic/`, `schemas/`, ni los tests existentes.

## Visual design impact

Solo ajustes a fuente grande y un indicador de estado; a fuente normal en móvil y tablet no cambia nada salvo W04 (texto "Guardar gasto" y dock sin total recalculado en blanco) y W07 (texto del diálogo). Debe ser coherente con DESIGN.md: borde de selección en `onSecondaryContainer` (tokens existentes, sin colores nuevos), márgenes `PADDING_PANTALLA`, botones >= 48 dp, estados con `EstadoPantalla*`, columna de 720 dp.

## Durable documentation impact

- `DESIGN.md`: actualizar "Fuentes grandes" (línea ~162: el selector en dos filas ya no es "sin check" sino con borde de 2 dp; métricas de `TarjetaTotal` apiladas > 1,5; fallo de Procesado apilado/desplazable; vacío de Gastos con selectores dentro del scroll), "Estados de pantalla" (`EstadoPantallaDesplazable`), dock de "Nuevo gasto" ("Guardar gasto", total recalculado solo con líneas; línea ~308) y el diálogo de borrado de un gasto manual. Update.
- `docs/evidence/mejoras-visuales-3/README.md`: crear (lectura honesta, valores restaurados, qué no se verificó). `PROGRESS.md` y `feature_list.json` (evidence, status): los actualiza el orquestador, no el planner. `AGENTS.md`, `CONTEXT.md`, `docs/architecture.md`, `docs/domain-model.md`, ADRs: not needed (sin cambio de arquitectura ni de dominio). `docs/specs/mejoras-visuales.md` queda como histórico (not needed). Sugerencia para `feature_list.json`: ampliar `user_visible_behavior`/`verification` con "Nuevo gasto sin total recalculado engañoso", "vacío de Gastos alcanzable con fuente grande" y "diálogo de borrado correcto para gasto manual".

## Implementation plan / tasks (en orden)

1. Línea base: `gradlew.bat assembleDebug testDebugUnitTest --console=plain` en verde. `adb devices` y confirmar que `emulator-5556` está presente; anotar `wm size`, `wm density`, `font_scale`, `user_rotation`, `accelerometer_rotation`.
2. Capturas ANTES (build actual, 360 dp, 1,3, 2,0 y tablet 2560x1600 @320): selector en dos filas (Resumen y Gastos), fallo de Procesado, Resumen (tarjeta de total), Nuevo gasto (vacío y con total escrito), Gastos vacío y vacío por filtro, permiso de cámara, cámara con Galería, diálogo de borrado de un gasto manual (crear uno con comercio único y borrarlo después), listado con chips de pago, detalle con chip. Los estados sin acceso real se pintan con un test TEMPORAL `Zz...` que se elimina y se reinstala el APK de instrumentación sin él (precedente de `mejoras-visuales-2`).
3. W12 + W06 + W08 + W09 (`EstadoPantallaDesplazable` y su uso).
4. W01 (borde) y W05 (grupo).
5. W02 (fallo de Procesado).
6. W03 (métricas).
7. W04 (dock de Nuevo gasto) y W07 (texto del borrado).
8. Tests nuevos (abajo) y `gradlew.bat assembleDebug testDebugUnitTest`.
9. W10/W11: revisar las capturas y corregir solo si hay defecto.
10. Capturas DESPUÉS (mismas configuraciones), restaurar `wm size reset`, `wm density reset`, `font_scale`, rotación.
11. `DESIGN.md` y README de evidencia.

## Verification plan

- Entorno: SOLO `emulator-5556`. Prohibido `connected*`, `pm clear`, instalar en cualquier dispositivo físico (la Tab tiene datos reales) y leer el token de Hugging Face (ni `app/appcheck-debug-token.txt`). Instalación con `adb -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk` y el APK `androidTest` análogo (preferible a `installDebug`, que instala en todos los dispositivos conectados; si se usa Gradle, `ANDROID_SERIAL=emulator-5556` obligatorio y comprobar antes con `adb devices`).
- JVM: `gradlew.bat assembleDebug testDebugUnitTest --console=plain` (hoy 197+ tests; sin fallos).
- Instrumentados: `adb -s emulator-5556 shell am instrument -w -e notClass com.robertomr.tally.extraccion.ExtraccionLocalRealTest,com.robertomr.tally.extraccion.ExtraccionNubeRealTest com.robertomr.tally.test/androidx.test.runner.AndroidJUnitRunner` (confirmar el nombre con `pm list instrumentation`); suite completa y, aparte, las clases nuevas. Si una clase ajena falla por lentitud del emulador (precedente `keyDispatchingTimedOut`), repetirla aislada y anotarlo.
- Capturas con `adb -s emulator-5556 exec-out screencap -p`, reducidas (480/800 px), en `docs/evidence/mejoras-visuales-3/` con prefijos `antes-`/`despues-`: móvil, 360 dp (`wm size 720x1560` + `wm density 320`), fontScale 1,3 y 2,0 (`settings put system font_scale`) y tablet horizontal (`wm size 2560x1600` + `wm density 320`).

## Plan de tests (aditivo; no se modifica ni debilita nada existente, no se cambian tags)

- JVM `TextoConfirmarBorradoTest` (`:feature:gastos`): manual -> sin "foto"; con foto -> contiene "y su foto"; nombre en blanco ya resuelto por el llamador (se pasa `este ticket`); el importe formateado aparece tal cual.
- E2E `FuentesGrandesFlujoTest` (`createComposeRule`, `LocalDensity` con `fontScale = 2f`, `Box(Modifier.width(360.dp))`, patrón de `FiltroFormaPagoVacioTest`): (a) `PeriodoSelector` apilado: los 4 `periodo_*` visibles, el activo con `assertIsSelected`, alto >= 48 dp; (b) `ExtraccionFallidaContent`: `procesado_volver_foto`, `procesado_reintentar`, `procesado_reintentar_otro` visibles (con `performScrollTo` si están en el cuerpo), alto >= 48 dp y ancho >= 200 dp; (c) `GastosContenido` vacío y vacío por filtro: `performScrollTo` + `assertIsDisplayed` en `boton_escanear_vacio`, `gastos_anadir_vacio` y `filtro_vaciar`; (d) `DockEdicion` con lista vacía y `mostrarTotalRecalculado = false`: `revision_total_recalculado` no existe y "Guardar gasto" visible; con una línea sí existe. Si algún composable fuera `internal` y no accesible desde `:app`, se cubre solo con captura y se declara.
- E2E `NuevoGastoDockFlujoTest` (app real, patrón de `GastoManualFlujoTest`, comercio único y limpieza en `@After`): en Nuevo gasto con total escrito y sin líneas, `revision_total_recalculado` no existe y el botón se llama "Guardar gasto"; tras `revision_anadir_linea` el total recalculado aparece. Los existentes `GastoManualFlujoTest` y `RevisionDatosFlujoTest` deben seguir en verde (confirman que el dock por defecto no cambia).
- No hay test automático del borde de W01 (no es semántica): evidencia = captura + revisión de código; declararlo.

## Evidence to capture

Salidas de `testDebugUnitTest` y `am instrument` (nº de tests, 0 fallos, clases nuevas incluidas); tabla antes/después por configuración; confirmación de restauración (`wm size`, `wm density`, `font_scale`, rotación, `adb devices` sin tocar la Tab); lista de textos cambiados ("Guardar gasto", diálogo de borrado) y confirmación de que ningún tag cambió (`git diff` de `testTag(`).

El informe del implementer DEBE decir explícitamente: (1) qué ve el usuario y dónde (pantalla por pantalla, con configuración 360 dp / 1,3 / 2,0 / tablet: p. ej. "Resumen > selector en dos filas: el segmento activo lleva borde de 2 dp"); (2) qué se cambió respecto a lo VERIFICAR (W10/W11) o que se dejó intacto y por qué; (3) qué NO se verificó: imágenes no inspeccionadas por el agente (revisión visual humana pendiente), TalkBack, diálogo "Eliminar modelo", `captura_error` real, transiciones, motores reales, tablet física (la "tablet" es el emulador girado), cámara real de papel; (4) que no se instaló en ningún dispositivo físico y no se leyó ningún token.

## Validator checklist

- [ ] W01-W09 cumplen los escenarios 1-9 según código y capturas; W10/W11 con captura y decisión registrada.
- [ ] `git diff` sin tags eliminados ni cambiados; tests existentes sin modificar; solo ficheros nuevos de test.
- [ ] A fuente normal no hay cambios visuales salvo W04 y W07.
- [ ] Sin dependencias, módulos ni versiones nuevas; `:domain`/`:framework` intactos; features sin importarse; componentes compartidos en `:feature:common`.
- [ ] Verificación solo en `emulator-5556`, sin `connected*` ni `pm clear`; emulador restaurado; nada instalado en físicos; ningún token leído.
- [ ] `DESIGN.md` actualizado; V26-V30 no implementados a escondidas.
- [ ] El informe declara lo no verificado.

## Decisiones cerradas (defaults)

- Indicador del selector apilado: borde de 2 dp (no 1,5 dp de la nota) para asegurar contraste no textual; sin check (no cabe).
- Umbral de métricas de Resumen: 1,5 (ajustable a 1,15 solo con evidencia de captura a 1,3).
- Nuevo gasto: "Guardar gasto"; el total recalculado se oculta sin líneas (no se elimina de Revisión/Detalle).
- Vacío de Gastos: selectores dentro del scroll solo con fontScale > 1,15; fuente normal intacta.
- Diálogo de borrado: se omite "y su foto" solo para gasto manual; título "Eliminar ticket" sin cambio.

## Riesgos

- Mover `PeriodoSelector`/`FiltroFormaPagoSelector` a otro contenedor en el vacío con fuente grande puede duplicar tags durante una transición o hacer que `ListadoGastosVacioTest`/`FiltroFormaPago*Test` fallen por orden de nodos: ejecutar esas clases aisladas.
- `CabeceraDeProceso` se comparte entre los tres estados de Procesado: el padding horizontal nuevo no debe romper `ProcesadoFlujoTest`.
- Ocultar el total recalculado cambia la altura del dock al añadir la primera línea (salto menor); aceptado.
- `EstadoPantallaDesplazable` dentro de `ColumnaCentrada` de Escanear: comprobar que no recorta el estado en tablet.

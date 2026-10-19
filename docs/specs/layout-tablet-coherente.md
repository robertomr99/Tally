# Spec: layout-tablet-coherente

## Source feature

- id: `layout-tablet-coherente` | area: diseno | status: not_started | depends_on: `listado-gastos`, `detalle-ticket`, `papel-ticket`, `splash-animado` (todas accepted).
- Título: Layout coherente en tablet y horizontal, con cabeceras comunes.
- Petición del usuario (2026-10-19, Tab S9+ horizontal ~1280 dp): en Resumen el donut sale pegado a la izquierda y el selector Día/Semana/Mes/Personalizado ocupa todo el ancho, mientras que en Gastos todo está colapsado en el centro. Quiere UNA regla de ancho/alineación para todas las pantallas y revisar títulos y subtítulos.
- Decisiones ya tomadas por el usuario: (1) regla única = columna centrada de 720 dp en todo; móvil sin cambios; (2) cabecera común título + subtítulo.

## Goal / Non-goals

Goal: una constante `ANCHO_MAXIMO_CONTENIDO = 720.dp` y un contenedor compartido en `:feature:common` que todas las pantallas usan; un componente `CabeceraPantalla` compartido; donut centrado/balanceado; jerarquía tipográfica de títulos coherente. Por debajo de 720 dp de ancho disponible el layout es idéntico al actual.

Non-goals: `WindowSizeClass`, `NavigationRail`, split-screen, paddings de 24 dp en tablet (DESIGN.md los cita; se queda como seguimiento, ver D9); fijar orientación en el manifest; rediseñar tarjetas o colores; cambios en `:domain:*` y `:framework:*`; módulos Gradle o dependencias nuevas; tocar `libs.versions.toml`; tocar el contenido de los E2E salvo lo imprescindible (ver Verification).

## Job story

Cuando uso Tally en una tablet o con el móvil en horizontal, quiero que todas las pantallas compartan la misma columna centrada y la misma cabecera, para que el contenido no se estire ni se descoloque entre pantallas.

## Acceptance scenarios

1. Given Resumen a 1280 dp de ancho, Then cabecera, selector, tarjetas y recientes forman una columna de 720 dp centrada (márgenes laterales iguales); el selector NO ocupa todo el ancho; el donut queda centrado o equilibrado con su leyenda dentro de la tarjeta.
2. Given Resumen con distribución vacía (`donut_vacio`) o cargando (`null`), When ancho >= 480 dp en la tarjeta, Then el donut aparece centrado horizontalmente (hoy queda a la izquierda al estar en Row sin leyenda).
3. Given Gastos, Detalle, Ajustes, Escanear (estados que no son cámara) a 1280 dp, Then su contenido usa exactamente el mismo ancho (720) y alineación que Resumen; el dock de Detalle/Revisión y los botones de Procesado quedan dentro de la columna.
4. Given cámara/encuadre y revisión de foto, Then el `PreviewView`/imagen y su fondo siguen a todo el ancho; solo los controles/títulos usan la columna.
5. Given la barra inferior en 1280 dp, Then su fondo (y borde superior) ocupa todo el ancho, y los items de los 3 destinos quedan agrupados dentro de una columna de 720 dp centrada.
6. Given cualquier pantalla con cabecera, Then título `titleLarge` SemiBold `onSurface`, subtítulo opcional `labelLarge`/`onSurfaceVariant`, acción trailing opcional y flecha atrás opcional, con el mismo padding y la misma altura mínima; Resumen muestra "Resumen" + rango del periodo; Gastos "Gastos" + rango (tag `gastos_rango` se mantiene).
7. Given móvil (p. ej. 411 dp) y 360 dp, Then cada pantalla mantiene el mismo árbol de ancho/padding que en las capturas "antes" (la cabecera común sustituye a la actual, pero sin cambios de ancho; los cambios de título son los de D3/D4 y se listan en el informe).
8. Given los E2E de `:app` y los tests JVM, Then siguen en verde conservando todos los tags existentes (`detalle_volver`, `detalle_eliminar`, `ajustes_volver`, `pantalla_ajustes`, `gastos_rango`, `bottom_bar_*`, `leyenda_*`, `desglose_*`, `donut_vacio`, `donut_centro`, etc.).

## Repository research

Inspeccionado en esta sesión: `AGENTS.md`, `DESIGN.md` (Layout 142-151, Components 160-203, Responsive Baseline 220-224), `feature_list.json` (entrada), `docs/specs/splash-animado.md` (formato), `feature/resumen/.../TarjetaDistribucion.kt` completo. Hechos del orquestador, verificados por exploración previa y NO releídos aquí (el implementer debe releer cada fichero antes de editar): ver tabla.

| Pantalla / componente | Fichero:línea | Estado actual |
| --- | --- | --- |
| Resumen | `ResumenScreen.kt:63-117` | Column fillMaxSize+verticalScroll sin tope; cabecera = texto "Tally" (titleLarge SemiBold primary) + icono Ajustes; selector horizontal=20; tarjetas start/end=20 |
| Gastos | `GastosScreen.kt:40,75-150` | Box TopCenter + Column widthIn(max=720); título "Gastos" titleLarge + subtítulo `etiquetaPeriodo` labelLarge (`gastos_rango`) |
| Detalle | `TicketDetalleScreen.kt:127-263` | Column fillMaxSize statusBarsPadding; barra propia atrás + "Detalle del ticket" + eliminar; `BadgeMotor`; scroll padding 16; `DockEdicion` (`FormularioTicketEditable.kt:472-479`) a todo el ancho |
| Ajustes | `AjustesScreen.kt:128-182` | Scaffold + `TopAppBar` M3 "Ajustes"; contenido widthIn(max=600) literal en :150 |
| Escanear | `EscanearScreen.kt:112-338`, `ProcesadoContent.kt:102-335`, `RevisionFotoContent.kt`, `RevisionDatosContent.kt:59-110` | sin título; columnas fillMaxSize sin tope; botones fillMaxWidth padding 24; `PapelTicket.kt:62` `PAPEL_ANCHO_MAXIMO=480` centrado; `CameraEncuadre` PreviewView fillMaxSize |
| Barra inferior | `feature/common/.../TallyBottomBar.kt:55-80` | fillMaxWidth, items SpaceAround |
| Selector | `feature/common/.../PeriodoSelector.kt:61-67` | Row fillMaxWidth, pesos 0.8/1.1/0.8/1.6 |
| Donut | `TarjetaDistribucion.kt:40,85-107`; `DonutCategorias.kt:61` | `ANCHO_FILA=480`; Row(donut sin centrar, Leyenda weight 1) vs Column centrada; donut 176 dp fijo |
| Títulos de tarjetas | `TarjetaTotal.kt:59` labelMedium mayúsculas; `TarjetaDistribucion.kt:69` titleMedium+subtítulo bodySmall; `SeccionRecientes.kt:42` titleMedium start=4; desglose labelSmall mayúsculas; `CabeceraDia` en `ListadoTicketsUi.kt:56-84` | inconsistentes en padding y peso |

Hechos: no existe constante ni componente de ancho máximo compartido; sin WindowSizeClass/NavigationRail; manifest sin `screenOrientation`/`resizeableActivity` (rota libre). Los E2E no asertan anchos; dependen de tags y de que Resumen/Gastos sigan scrollables (`ResumenTotalFlujoTest`, `GraficaDonutFlujoTest` con `performScrollTo`, `PeriodoSelectorFlujoTest`, `RangoPersonalizadoFlujoTest` con `performScrollToNode`, `ListadoGastosFlujoTest` con `boundsInRoot.top`, `DetalleTicketFlujoTest` con barra oculta, `NavigationTest`, `CapturaFlujoTest`, `ProcesadoFlujoTest`, `RevisionDatosFlujoTest`, `Ajustes*FlujoTest`, `LogExtraccionesFlujoTest`). No inspeccionado: contenido de cada test; API del emulador.

## Decisiones (con default)

- D1 Contenedor. DEFAULT: nuevo fichero `feature/common/.../LayoutAncho.kt` (mismo paquete `com.robertomr.tally.feature.common`; no crea módulo) con `val ANCHO_MAXIMO_CONTENIDO = 720.dp` (público) y dos piezas:
  - `@Composable fun ColumnaCentrada(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit)` = `Box(modifier.fillMaxWidth(), contentAlignment = TopCenter) { Box(Modifier.widthIn(max = ANCHO_MAXIMO_CONTENIDO).fillMaxWidth() ...content) }`. Hace `fillMaxHeight` solo si el llamador lo pide (se pasa el modifier al Box exterior), para que `verticalScroll` pueda vivir dentro o fuera según pantalla.
  - `fun Modifier.columnaCentrada(): Modifier = this.widthIn(max = ANCHO_MAXIMO_CONTENIDO)` + `fillMaxWidth()` para uso donde ya existe un contenedor con `Alignment.CenterHorizontally`/`TopCenter` (barra inferior, dock). Si el implementer ve que una sola forma basta, deja solo el composable y justifica en el informe.
  - Semántica del 720: es el ancho total de la columna INCLUIDOS los paddings horizontales de cada pantalla (se aplica el tope fuera del padding, p. ej. `ColumnaCentrada { Column(Modifier.padding(horizontal=...)) }`). En móvil (< 720) no hay efecto. Gastos pasa de `widthIn(max=720)` literal a la constante; Ajustes de 600 a 720 (cambio aceptado, regla única).
- D2 Dónde NO. Fondos de pantalla y de barra inferior (a todo el ancho); `CameraEncuadre`/`PreviewView` y el visor de foto del detalle; el `Surface` de la barra inferior. Dentro de la barra, sus items van en `ColumnaCentrada` (items SpaceAround dentro de 720 dp en vez de 1280). Dock de edición (`DockEdicion`) y botones de procesado: contenedor de fondo a todo el ancho si lo tienen, contenido dentro de la columna; si el dock no tiene fondo propio, se envuelve entero en la columna.
- D3 `CabeceraPantalla` en `feature/common/.../CabeceraPantalla.kt`. Firma: `@Composable fun CabeceraPantalla(titulo: String, modifier: Modifier = Modifier, subtitulo: String? = null, subtituloTag: String? = null, onBack: (() -> Unit)? = null, backTag: String? = null, accion: (@Composable RowScope.() -> Unit)? = null)`. Fila: [IconButton atrás 48 dp con `Icons.AutoMirrored.Filled.ArrowBack` si `onBack`] + Column(weight 1: título `titleLarge` SemiBold `onSurface`, maxLines 1 ellipsis; subtítulo `labelLarge` `onSurfaceVariant`) + acción. Altura mínima 56 dp, padding horizontal 20 dp en Resumen/Gastos/Escanear (igual que hoy) y 4 dp a la izquierda con atrás (el IconButton aporta su zona táctil, como `TopAppBar`); el implementer iguala al padding actual de cada pantalla para no mover nada en móvil. No incluye `statusBarsPadding`: lo aplica el llamador donde hoy ya lo hay (Detalle) o lo da el `Scaffold`. Tags: los llamadores aplican con `Modifier.testTag` sobre `modifier`/`subtituloTag`; `gastos_rango` pasa a `subtituloTag`. Acción trailing y el botón atrás reciben sus tags (`detalle_eliminar`, `ajustes_volver`, `detalle_volver`) desde el llamador (`backTag` para el atrás; la acción lleva su propio `testTag` dentro del lambda).
- D4 Marca "Tally" en Resumen. DEFAULT: se sustituye por título "Resumen" + subtítulo con el rango del periodo (consistente con Gastos); la marca "Tally" queda en el splash y en el nombre de la app. Alternativa: título "Tally" + subtítulo rango (sin cambio de marca). Se elige "Resumen" porque la cabecera común nombra la pantalla; la decisión es del usuario si prefiere conservar la marca (decisión abierta A1). Acción trailing: icono Ajustes con su tag actual. El subtítulo reutiliza la misma función de etiqueta del periodo que Gastos (mover a `:feature:common` solo si hoy está duplicada; si es `internal` de `:feature:gastos`, NO importar entre features: calcular en Resumen con lo que el ViewModel ya expone o añadir un campo `etiquetaPeriodo` al estado de Resumen; el implementer comprueba qué existe y no inventa ruta nueva de dominio).
- D5 Escanear. DEFAULT: título solo en estados sin cámara (Procesado: "Procesando ticket"; Revisión de foto: "Revisar foto"; Revisión de datos/resultado: "Revisar ticket"; Error/permisos: "Escanear"), con `CabeceraPantalla` sin atrás ni acción (la navegación existente - Cancelar/Reintentar - no cambia). En el estado cámara/encuadre NO hay cabecera: no se tapa el encuadre; los controles flotantes existentes se mantienen en su sitio, con su fila de controles limitada a la columna si es una fila a todo el ancho. Los títulos nuevos no deben duplicar texto ya presente (revisar `ProcesadoContent`/`RevisionDatosContent`: si ya hay un título equivalente, reutilizarlo, no duplicarlo) y no deben romper asserts de texto de `ProcesadoFlujoTest`/`RevisionDatosFlujoTest` (buscar por `onNodeWithText` antes de elegir los textos; si un E2E usa `onNodeWithText("...")` con un texto que ahora aparece dos veces, cambiar el texto de la cabecera, no el test).
- D6 PapelTicket. DEFAULT: sin cambios; su `widthIn(max=480)` centrado vive dentro de la columna de 720 (el papel es más estrecho que la columna, queda centrado). Dock y botones de procesado: dentro de la columna 720 (más anchos que el papel; aceptado, botones `fillMaxWidth` de la columna menos padding 24).
- D7 Donut. DEFAULT: mantener `Row` (donut + leyenda) cuando `maxWidth >= ANCHO_FILA (480)` y hay `porciones`: con la columna de 720 el ancho interior de la tarjeta es ~640, así que la fila siempre aplica en tablet (176 + 24 + ~440 de leyenda en 2 columnas, equilibrado y es el comportamiento del mockup `flex-col sm:flex-row`); Column centrada pasaría a ser una pila alta con mucho aire lateral. Correcciones: (a) si `porciones` está vacío (cargando/vacío), mostrar SOLO el donut centrado (`Box(Modifier.fillMaxWidth(), Center)`), en cualquier ancho; (b) en fila, el donut va dentro de un `Box` con `Modifier.weight(0.38f)` + `contentAlignment = Center` y la leyenda con `weight(0.62f)` (centrado vertical ya existe) para que el donut no quede pegado al borde izquierdo; el implementer ajusta pesos tras ver la captura (no quitar los tags `leyenda_*`, `donut_centro`, `desglose_*`). Tamaño del donut sin cambio (176 dp fijo); no se escala.
- D8 Selector de periodo. DEFAULT: queda dentro de la columna 720 (ocupa 720 menos márgenes, como en Gastos); no se limita más. Sin cambios en `PeriodoSelector.kt` salvo si el contenedor lo requiere. Decisión abierta A2: limitar a ~480 centrado.
- D9 Tipografía de títulos de tarjeta. DEFAULT, 4 niveles documentados en DESIGN.md: (1) título de pantalla = `CabeceraPantalla` (`titleLarge` SemiBold); (2) título de sección/tarjeta = `titleMedium` SemiBold `onSurface` (Distribución, Recientes, secciones de Ajustes/Detalle), subtítulo de tarjeta `bodySmall` `onSurfaceVariant`, padding start igual al de la tarjeta (quitar el `start=4` de `SeccionRecientes` si alinea mal con el borde de las tarjetas; verificar en captura); (3) etiqueta de métrica = `labelMedium`/`labelSmall` mayúsculas `onSurfaceVariant` (TarjetaTotal, cabeceras del desglose); (4) cabecera de grupo de lista (`CabeceraDia`) se queda como está. No se cambian textos, solo estilos donde se desvíen del nivel. Paddings 24 dp de tablet: seguimiento, no ahora.
- D10 Barras de estado/IME. Detalle conserva `statusBarsPadding`; Ajustes, al dejar el `Scaffold` sin `TopAppBar`, conserva `Scaffold` (insets) y pone `CabeceraPantalla` como primer hijo del contenido.
- D11 Pruebas. DEFAULT: tests JVM en `feature/common/src/test` solo para lógica pura si la hay (p. ej. cálculo "enFila" extraído a función `fun debeIrEnFila(ancho: Dp, porciones: Int)` en `TarjetaDistribucion.kt` o test Compose JVM no aplicable). No hay Robolectric: no se prueban anchos en JVM. Los anchos se verifican con capturas (abajo). Un E2E nuevo solo si es barato: p. ej. en `PeriodoSelectorFlujoTest`/`GraficaDonutFlujoTest` aserta `donut_vacio` existente; no se añaden asserts de ancho porque en el emulador estándar el ancho es el del AVD.

## Expected file changes

- Nuevos: `feature/common/src/main/java/com/robertomr/tally/feature/common/LayoutAncho.kt`, `.../CabeceraPantalla.kt`. (Comprobar antes que no existe equivalente en `feature/common`.)
- Editar: `ResumenScreen.kt`, `TarjetaDistribucion.kt`, `SeccionRecientes.kt`, `TarjetaTotal.kt` (solo si el nivel difiere) en `feature/resumen`; `GastosScreen.kt` en `feature/gastos`; `TicketDetalleScreen.kt`, `FormularioTicketEditable.kt` (dock), `AjustesScreen.kt`, `EscanearScreen.kt`, `ProcesadoContent.kt`, `RevisionFotoContent.kt`, `RevisionDatosContent.kt` (rutas reales: localizar con Glob), `TallyBottomBar.kt`, `PapelTicket.kt` (solo si hay que sacar su constante; por defecto intacto).
- `DESIGN.md`: reescribir `## Layout` (líneas ~142-151: un solo contenedor de 720 dp en todo, barra inferior con fondo completo, componente `CabeceraPantalla`, jerarquía D9) y `## Responsive Baseline` (regla única, `WindowSizeClass` y paddings 24 dp = no MVP); actualizar la línea "TopAppBar plana ... título 'Tally'" de Components según D4 y la del Detalle ("barra superior" -> cabecera común).
- No tocar: `:domain:*`, `:framework:*`, `build-logic/`, `libs.versions.toml`, manifest.

## Implementation plan / tasks

1. Línea base: `bash init.sh` verde. Capturas "antes" (ver Verification) de Resumen, Gastos, Detalle, Ajustes, Escanear (procesado/revisión) en las 3 configuraciones, guardadas en el directorio de evidencia de sesión fuera de `docs/` o en `docs/evidence/layout-tablet-coherente/` (decide el implementer; sin commitear binarios si el repo no lo hace).
2. Crear `LayoutAncho.kt` y `CabeceraPantalla.kt` con `@Preview` solo si el repo ya usa previews.
3. Aplicar a Gastos (el menor riesgo: sustituir literal 720 por la constante y su cabecera) y Ajustes (sustituir `TopAppBar`, 600 -> constante).
4. Resumen: columna + cabecera (D4) + arreglos del donut (D7) + tipografía (D9).
5. Detalle y dock (D2/D10); Escanear estados no cámara (D5/D6); barra inferior (D2).
6. Actualizar `DESIGN.md`.
7. `gradlew.bat assembleDebug testDebugUnitTest --console=plain`, luego suite `:app` instrumentada en emulator-5556 con `am instrument` (nunca `connected*`, nunca `pm clear`; la tablet física no se toca).
8. Capturas "después", comparación y registro de evidencia en `PROGRESS.md`/`feature_list.json` (lo hace el orquestador).

## Verification plan

- Comandos: `bash init.sh` o `gradlew.bat assembleDebug testDebugUnitTest --console=plain`; `adb -s emulator-5556 ...` para instalar y `am instrument -w` de la suite `:app` completa (patrón ya usado en PROGRESS.md).
- Ancho tablet/horizontal en emulator-5556: guardar estado `adb -s emulator-5556 shell wm size` y `wm density` y `settings get system user_rotation`/`accelerometer_rotation`; aplicar `wm size 2560x1600` + `wm density 320` (aprox. 1280x800 dp) y `settings put system accelerometer_rotation 0` + `user_rotation 1` para horizontal; 360 dp: `wm size 720x1560` + `wm density 320`; móvil: tamaño por defecto. Restaurar siempre con `wm size reset`, `wm density reset`, `settings put system user_rotation 0`, `accelerometer_rotation` anterior. Capturas con `adb exec-out screencap -p`. Si la suite instrumentada va con el tamaño modificado, restaurar antes de correrla.
- Por pantalla (Resumen con datos y vacío, Gastos, Detalle, Ajustes, Escanear en procesado y revisión): captura antes/después en móvil, 360 dp y horizontal. Criterio: en móvil y 360 dp "después" es igual a "antes" salvo los cambios de cabecera listados (D4/D5); en horizontal la columna mide 720 dp (comprobable midiendo los bordes de tarjetas en la captura: 1280-720 = 280 dp de margen total, 140 a cada lado) y los fondos/encuadre/barra llegan a los bordes.
- Regla "solo por debajo del tope": a 840 dp de ancho (justo por encima de 720) los márgenes son (840-720)/2 y a 700 dp la captura coincide con el comportamiento móvil.
- E2E: suite `:app` completa en verde, sin cambiar tags. Para los tests con cabeceras nuevas (Escanear), comprobar que no hay `onNodeWithText` ambiguo.

## Evidence to capture

Salida de `testDebugUnitTest` y de la suite `:app` (nº de tests, 0 fallos); tabla de capturas antes/después con las 3 configuraciones; confirmación de restauración (`wm size`, `wm density`, rotación) tras la sesión; lista de cambios de texto/título (Resumen: "Tally" -> "Resumen"; Ajustes: TopAppBar -> cabecera; Escanear: títulos nuevos).

## Validator checklist

- [ ] Una sola constante `ANCHO_MAXIMO_CONTENIDO` y ningún literal 720/600/480 de ancho de pantalla residual (el 480 de `PapelTicket` y de `ANCHO_FILA` del donut son anchos de componente, no de pantalla).
- [ ] Un solo componente `CabeceraPantalla` usado por Resumen, Gastos, Detalle, Ajustes y Escanear (no cámara); sin `TopAppBar` en Ajustes.
- [ ] Cámara/encuadre, visor de foto y fondo de la barra a todo el ancho; items de la barra dentro de 720.
- [ ] Donut centrado en vacío/cargando y equilibrado con leyenda; tags `leyenda_*`, `desglose_*`, `donut_*` intactos.
- [ ] Móvil y 360 dp sin diferencias de ancho respecto a "antes"; horizontal con columna de 720 centrada en las 5 pantallas.
- [ ] DESIGN.md actualizado; sin dependencias, módulos ni versiones nuevas; dominio y framework intactos; features sin importarse entre sí.
- [ ] Suite `:app` y JVM en verde; emulador restaurado; la tablet no se tocó; sin `pm clear`.

## Riesgos

- Mover el tope fuera de `verticalScroll` o envolver listas `LazyColumn` en un `Column` scrollable rompería `performScrollTo`/`performScrollToNode`; aplicar el tope con `widthIn` sobre el mismo scrollable o dentro, nunca anidar scrolls verticales.
- `ListadoGastosFlujoTest` usa `boundsInRoot.top`: la cabecera común no debe cambiar el orden vertical de los elementos.
- Cabecera de Escanear: textos duplicados pueden hacer ambiguos `onNodeWithText`.
- `etiquetaPeriodo` de Gastos puede ser `internal` de su feature (D4).
- Cambio visual de Ajustes de 600 a 720 dp, y Resumen "Tally" -> "Resumen": aceptados salvo veto del usuario.

## Decisiones abiertas (default)

- A1 Marca en Resumen: default "Resumen" + rango; alternativa "Tally" + rango.
- A2 Ancho del selector: default 720 completo; alternativa ~480 centrado.
- A3 Paddings 24 dp en tablet (DESIGN.md): default fuera de alcance.
- A4 Donut: default Row (donut 38 % + leyenda 62 %) en columna de 720; alternativa Column centrada.

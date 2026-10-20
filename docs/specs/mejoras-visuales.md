# Spec: mejoras-visuales

## Source feature

- id: `mejoras-visuales` | area: diseno | status: not_started | depends_on: `layout-tablet-coherente`, `papel-ticket` (accepted). Rama `correcciones-visuales`.
- Petición del usuario: "mira todas las mejoras visuales que puedas hacer, haz la spec y empieza el proceso de implementar y validar".
- Esta spec es una AUDITORÍA (39 hallazgos, V01-V39) más el contrato de la parte que se hace ahora. Por tamaño se **recomienda dividir** (ver "Reparto"): esta feature implementa el bloque F1 (13 hallazgos, el de mayor impacto); F2 `mejoras-visuales-2` (12 hallazgos) y F3 (5 diferidos) quedan documentados aquí para que el orquestador cree las entradas en `feature_list.json` (el planner no puede editarlo).

## Goal / Non-goals

Goal (F1): arreglar los defectos visuales de mayor severidad: hueco doble bajo la barra de estado, iconos de la barra de estado ilegibles, cabecera común ausente en Procesado, dock/chips rotos con fuentes grandes, diálogo en píldora, marco de foto, información duplicada, ortografía, alineaciones y transiciones. Una sola regla de forma en el tema.

Non-goals: rediseñar tarjetas/colores/mockups, tema oscuro, `WindowSizeClass`/`NavigationRail`, módulos Gradle o dependencias nuevas, tocar `libs.versions.toml`, `:domain:*`, `:framework:*`, `build-logic/`, cambiar el flujo de Escanear, snackbars (F3). Las features no se conocen entre sí: todo componente compartido va en `:feature:common`.

## Job story

Cuando abro Tally en móvil, con fuente grande o en tablet, quiero que cada pantalla empiece en el mismo sitio, se lea con claridad y no repita información ni se rompa, para que la app se perciba cuidada y coherente.

## Hallazgos de la auditoría

Ficheros bajo `feature/...` abreviados (`common` = `feature/common/src/main/java/com/robertomr/tally/feature/common/`). Evidencia = captura en `docs/evidence/` o `fichero:línea` leído. Riesgo = riesgo para tags/E2E. Decisión: F1 / F2 = HACER en esa feature; F3 = DIFERIR; X = DESCARTAR.

| Id | Pantalla | Hallazgo | Evidencia | Sev. | Esf. | Riesgo | Dec. |
| --- | --- | --- | --- | --- | --- | --- | --- |
| V01 | Todas (Resumen, Gastos, Detalle, Ajustes, Revisión) | Doble inset superior: el `Scaffold` de `TallyApp` pasa `innerPadding` con la barra de estado y cada pantalla vuelve a aplicar `statusBarsPadding()`: ~50 dp de hueco vacío sobre el título (título a y=145 con reloj a y=30; en Revisar foto se ve una banda de otro color). Procesado/Permiso/Error solo aplican un inset, así que el contenido arranca a alturas distintas según el estado | `layout-tablet-coherente/despues-movil-resumen.png`, `-revision-foto.png`; `TallyApp.kt:47-52`, `ResumenScreen.kt:65`, `GastosScreen.kt:75`, `TicketDetalleScreen.kt:131`, `RevisionFotoContent.kt:65`, `RevisionDatosContent.kt:66` | alta | S | bajo (cambia posiciones absolutas, no el orden; `ListadoGastosFlujoTest` usa `boundsInRoot.top` relativo) | F1 |
| V02 | Todas | Iconos y reloj de la barra de estado blancos sobre fondo claro (casi invisibles); `MainActivity` no llama a `enableEdgeToEdge` y el tema es `Theme.Material.Light` sin estilo de barras: el aspecto cambia entre API<35 y >=35 | `despues-movil-resumen.png` (reloj "9:28" apenas visible), `papel-ticket/despues_360dp_fs20_*.png`; `MainActivity.kt:41`, `themes.xml:4` | alta | S | ninguno | F1 |
| V03 | Detalle y Revisión de datos | Con fontScale 1,3/2,0 el dock trunca "Descartar"/"Guardar cambios" ("Desca...", "Guard...") y "Total recalculado" ocupa 5 líneas | `papel-ticket/despues_360dp_fs20_3_detalle_a.png`, `..._2_revision_b.png`; `FormularioTicketEditable.kt:491-572` | alta | M | bajo (tags `revision_descartar/guardar/total_recalculado` se conservan) | F1 |
| V04 | Procesado | A fontScale 2,0 el chip "Categorización" pasa a un círculo de 3 líneas (`ChipPaso` clip pill + texto que envuelve) | `papel-ticket/despues_360dp_fs20_1_procesado.png`; `ProcesadoContent.kt:496-577` | media | S | bajo | F1 |
| V05 | Ajustes, diálogos | `AlertDialog` "Eliminar modelo" no fija forma: hereda `extraLarge` del tema = píldora 9999 dp; otros dos diálogos parchean `RoundedCornerShape(28.dp)`. El botón destructivo es `TextButton` en Ajustes y `Button` rojo en Detalle | `AjustesScreen.kt:172-188`, `theme/Theme.kt:66`, `RangoFechasDialog.kt:81`, `TicketDetalleScreen.kt:387`, `detalle-ticket/04-confirmar-borrado.png` | alta | S | ninguno | F1 |
| V06 | Revisar foto / visor | El borde y el recorte redondeado se dibujan alrededor de todo el área (`fillMaxSize`) y no de la imagen real (`ContentScale.Fit`): marco a todo el ancho con barras vacías a los lados | `despues-movil-revision-foto.png`; `FotoOriginal.kt:76-85` | media | S | bajo (tags `captura_foto`, `detalle_foto` van sobre la `Image`) | F1 |
| V07 | Procesado (3 estados) | Sin `CabeceraPantalla` (la spec anterior lo pedía en D5 y no se aplicó): titular propio `headlineSmall`, badge flotante; a 2,0 el badge se solapa con la barra de estado. Rótulos distintos a Revisar foto/Revisar ticket | `despues-tablet-procesado-resultado.png`, `fs20_1_procesado.png`; `ProcesadoContent.kt:107-150,166-175,240-248` | alta | M | medio (E2E de procesado: buscar `onNodeWithText`; "Analizando ticket de compra" no aparece en tests, verificado por grep) | F1 |
| V08 | Resumen | El rango del periodo aparece dos veces: subtítulo de cabecera (`resumen_rango`) y chip dentro de `TarjetaTotal` (`periodo_rango`) | `despues-movil-resumen.png`; `TarjetaTotal.kt:67-81`, `ResumenScreen.kt:74` | media | S | medio (ver Impacto en tests) | F1 |
| V09 | Resumen | Ortografía mixta "periodo" (7 textos) / "período" (2 textos de UI) | `TarjetaTotal.kt:130`, `FormatoResumen.kt:7` vs `SeccionRecientes.kt:42,51`, `TarjetaDistribucion.kt:45,115`, `GastosScreen.kt:152` | baja | S | medio (3 tests) | F1 |
| V10 | Gastos, Resumen, Procesado | Desfase de 4 dp: `CabeceraDia` y `SeccionRecientes` ponen `start=4` dentro del padding de 20 (texto a 24 dp frente al título y las tarjetas a 20). En Procesado el "n/5 campos extraídos" queda a la izquierda, fuera del papel centrado | `despues-movil-gastos.png`, `despues-tablet-procesado-resultado.png`; `ListadoTicketsUi.kt:66`, `SeccionRecientes.kt:43,53` | media | S | ninguno | F1 |
| V11 | Detalle, Revisión, Ajustes, Procesado | Margen horizontal de pantalla 16 dp (Detalle `:173`, Revisión `:85`, Ajustes `:138`, dock `:494`, Revisar foto `:80`) frente a 20 dp en cabecera, selector, tarjetas y lista; botones de Procesado a 24 dp | `despues-movil-detalle.png` vs `despues-movil-resumen.png` | media | S | ninguno | F1 |
| V12 | Resumen, Gastos (selector) | Segmentos de 44 dp (<48) y etiqueta `softWrap=false`+`Clip`: "Personalizado" se recorta con fuente grande (no capturado, deducido del código) | `PeriodoSelector.kt:99,118-126` | media | M | bajo (tags `periodo_*`) | F2 |
| V13 | Ajustes vs Resumen | Dos lenguajes de tarjeta: Resumen `surfaceContainerLow`+borde 0,4+radio 24; Ajustes `Card` `surfaceContainer`, sin borde, radio 12, padding 16 | `despues-movil-ajustes.png`; `AjustesScreen.kt:197-202,268-273`, `TarjetaTotal.kt:50-52` | media | M | bajo (tags `seccion_*` pasan al modifier) | F2 |
| V14 | Resumen, Ajustes, Formulario | Títulos de sección sin `heading()` (solo la cabecera lo lleva) y pesos distintos ("Desglose de artículos" Bold, resto SemiBold) | `TarjetaDistribucion.kt:69`, `SeccionRecientes.kt:41`, `AjustesScreen.kt:208,279,377`, `FormularioTicketEditable.kt:300` | media | S | ninguno | F2 |
| V15 | Gastos, Detalle, Escanear | Estados vacío/error/carga con lenguajes distintos: vacío de Gastos (icono 24 + `headlineSmall` + botón), "Este ticket ya no existe" (solo `titleMedium`), permiso (icono 48), error de captura (sin icono), fallo de procesado (icono 40 error); carga: Gastos en blanco sin spinner, spinner 40 dp / 32 dp / indicador de Detalle | `GastosScreen.kt:96,137-167`, `TicketDetalleScreen.kt:142-149,278-303`, `EscanearScreen.kt:222-358`, `ProcesadoContent.kt:258-285` | media | M | medio (tags `listado_vacio`, `detalle_no_existe`, `captura_permiso`, `captura_error` se conservan) | F2 |
| V16 | Formulario | Chips de categoría de ~32 dp de alto (<48 dp táctil) | `FormularioTicketEditable.kt:228-247` | baja | S | ninguno | F2 |
| V17 | Procesado, Revisión, Detalle | `FilaRecibo` sin semántica agrupada: TalkBack lee "2×", concepto e importe por separado | `PapelTicket.kt:175-258` | media | S | bajo (`onNodeWithText` exacto sigue casando con nodos fusionados) | F2 |
| V18 | Barra inferior | Los 3 items no están agrupados semánticamente (`selectableGroup`) | `TallyBottomBar.kt:77-92` | baja | S | ninguno | F2 |
| V19 | Navegación | `NavHost` usa la transición por defecto (fundido de 700 ms entre pestañas y al abrir Detalle/Ajustes): lento y sin dirección | `TallyApp.kt:48-52` | media | S | bajo | F1 |
| V20 | Detalle, Revisión | Sin `imePadding` ni `windowSoftInputMode`: con edge-to-edge forzado (targetSdk 36) el teclado puede tapar el dock/campos (a verificar en emulador) | grep `imePadding` = 0 resultados | media | S | bajo | F1 |
| V21 | Detalle, Revisión | `BadgeMotor` fijo fuera del scroll: gasta altura (sobre todo en fuente grande/horizontal) | `TicketDetalleScreen.kt:159-162`, `RevisionDatosContent.kt:71-74` | baja | S | bajo (tag `revision_badge_motor`) | F2 |
| V22 | Cabecera | Título con `maxLines=1`+elipsis: "Detalle del ticket" se corta a fuente grande | `CabeceraPantalla.kt:78`, `fs20_3_detalle_a.png` | baja | S | ninguno | F2 |
| V23 | Procesado, Dock | Texto de 11 sp en `outline` (#717973 sobre #EFF5F0, ~4,4:1, bajo AA): "Procesado en...", "Calculado a partir de..." | `ProcesadoContent.kt:459`, `FormularioTicketEditable.kt:521`, `Color.kt:50` | baja | S | ninguno | F2 |
| V24 | Resumen | Icono Ajustes `Filled` frente al resto de iconos de cabecera `Outlined` | `ResumenScreen.kt:11,79` | baja | S | ninguno | F2 |
| V25 | Cámara | Obturador y "Cerrar" con `clickable` sin `Role.Button` | `CameraEncuadre.kt:334,355` | baja | S | ninguno | F2 |
| V26 | Formulario del papel | Campos `OutlinedTextField` con caja dentro del papel: sigue pareciendo formulario. Alternativa `TextField` con contenedor transparente (solo subrayado, mantiene label, error, teclado, tags). Beneficio: lectura de recibo. Riesgo: foco/error menos visibles, componente más probado (E2E de edición) | `FormularioTicketEditable.kt:176-194`, `despues-movil-detalle.png` | media | M | medio | F3 (decisión A1) |
| V27 | Guardar/Eliminar | Sin confirmación visible tras guardar o borrar (se navega sin feedback); exige host de snackbar compartido (`CompositionLocal` en `:feature:common`) | `TicketDetalleScreen.kt:123` | media | M | medio | F3 |
| V28 | Escanear | Los cambios de estado internos (encuadre, revisión, procesado) son cortes secos | `EscanearScreen.kt:123-206` | baja | M | medio (duplica árboles durante la transición) | F3 |
| V29 | Detalle/Revisión en móvil horizontal | El dock fijo deja poca altura útil al formulario | sin captura; deducido | baja | M | bajo | F3 |
| V30 | Revisión de datos | La barra inferior permite salir con edición a medias sin avisar (cambio funcional, no visual) | `TallyApp.kt:37-46` | media | M | medio | F3 |
| V31 | Tablet | Márgenes de 24 dp en tablet (DESIGN.md) | la columna de 720 dp ya centra | baja | - | - | X: cubierto por `layout-tablet-coherente`; solo se documenta |
| V32 | Resumen | Leyenda del donut y desglose repiten nombre y porcentaje | mockup 02 los incluye ambos | baja | - | - | X: normativo en mockup |
| V33 | Resumen | Etiquetas en mayúsculas literales ("GASTO TOTAL DEL PERIODO", "DESGLOSE DETALLADO") | DESIGN.md (label-caps) | baja | - | - | X: decisión de diseño documentada |
| V34 | Resumen | "Tickets / 32 tickets" redundante | `TarjetaTotal.kt:100-104` | baja | S | medio (textos fijados en specs/tests) | X: ganancia mínima, riesgo desproporcionado |
| V35 | Global | `labelSmall` de 11 sp muy pequeño | `Type.kt:101` | baja | - | - | X: escala del sistema de diseño; mitigado por V23 |
| V36 | Splash | Sin hallazgos (aceptado, 700 ms acotados) | `splash-animado/README.md` | - | - | - | X |
| V37 | Selector de rango | Contenido del `DateRangePicker` correcto (cabecera propia en español); solo la forma, ya en V05 | `RangoFechasDialog.kt:86-110` | - | - | - | X: cubierto por V05 |
| V38 | Gastos | Cabeceras de día no son sticky | `GastosScreen.kt:107` | baja | M | medio (`LazyColumn`/`boundsInRoot`) | X: no aporta |
| V39 | Ajustes | La barra inferior se muestra en Ajustes con Resumen activo | `TallyApp.kt:40` | baja | - | - | X: comportamiento coherente (Ajustes cuelga de Resumen) |

Recuento por decisión: **F1 = 13** (V01-V11, V19, V20); **F2 = 12** (V12-V18, V21-V25); **DIFERIR F3 = 5** (V26-V30); **DESCARTAR = 9** (V31-V39). Total 39. Por severidad dentro de HACER: alta 5 (V01, V02, V03, V05, V07), media 14, baja 6.

No inspeccionado visualmente: `splash-animado` (solo README), `rango-personalizado/02-picker-marcado.png`, estados reales de fallo de Procesado, selector a fontScale alto, visor de foto. Se leyó el código de todas las pantallas.

## Reparto y alcance (F1 = esta feature)

F1 es de una sola sesión: tema, `MainActivity`, `TallyApp`, y retoques puntuales en 8 ficheros de UI. F2 es aditivo (un componente `TarjetaSeccion`/`TituloSeccion`/`EstadoPantalla` y retoques a11y). Se aconseja que el orquestador añada `mejoras-visuales-2` (depends_on `mejoras-visuales`) y deje F3 como notas.

## Acceptance scenarios (F1)

1. Given cualquier pestaña en móvil, When se abre, Then el título queda a una sola distancia de la barra de estado (≈ alto de barra + 8 dp, sin ~50 dp de hueco extra), idéntica en Resumen, Gastos, Detalle, Ajustes, Revisar foto, Revisar ticket y Procesado.
2. Given la app en API 36/37, Then reloj e iconos de la barra de estado se ven oscuros sobre el fondo claro, también con el sistema en modo oscuro (la app no tiene tema oscuro).
3. Given Procesado (en curso, resultado, fallo), Then hay `CabeceraPantalla` (título `titleLarge`, subtítulo opcional) con los mismos paddings que el resto y no se solapa con la barra de estado a fontScale 2,0; el texto "Analizando ticket de compra" pasa a ser el título (sin cambio de texto), el subtítulo es "Extrayendo comercio, fecha e importes del ticket.", y resultado/fallo se titulan "Resultado".
4. Given fontScale 1,3 y 2,0 en Detalle (con cambios) y Revisar ticket, Then "Descartar" y "Guardar cambios" se leen completos (botones apilados o texto sin elipsis) y "Total recalculado" ocupa <=2 líneas; el chip "Categorización" conserva forma de píldora (pasos apilados o con `maxLines=1`).
5. Given el diálogo "Eliminar modelo", Then tiene esquinas de 28 dp y el botón destructivo en paleta `error` como en Detalle; no queda ningún `RoundedCornerShape(28.dp)` de parche en diálogos y `extraLarge` del tema vale 28 dp.
6. Given Revisar foto con foto vertical, Then el borde y el redondeo envuelven la imagen visible, no el área completa.
7. Given Resumen, Then el rango del periodo aparece una sola vez (cabecera); `TarjetaTotal` ya no muestra el chip.
8. Given los textos de UI, Then se escribe siempre "periodo" (sin tilde) incluida "Sin datos del periodo anterior" y "vs periodo anterior".
9. Given Gastos y Resumen, Then cabeceras de día, "Gastos del periodo" y "Sin tickets en este periodo." alinean a 20 dp con título y tarjetas; en Procesado "n/5 campos extraídos" queda alineado con el papel.
10. Given Detalle, Revisión, Ajustes y Revisar foto, Then los márgenes horizontales de pantalla son 20 dp (constante común); el papel conserva su padding interior de 16 dp.
11. Given navegar entre pestañas, Then la transición es un fundido corto (~150 ms) y abrir Detalle/Ajustes desliza horizontalmente (~300 ms), con pop inverso.
12. Given el teclado abierto en un campo del Detalle o de la Revisión, Then el dock y el campo enfocado quedan visibles (con `imePadding`), verificado en emulador.
13. Given la suite `:app` y los tests JVM, Then siguen en verde, con las actualizaciones justificadas de abajo.

## Repository research

Inspeccionado: `AGENTS.md`, `PROGRESS.md` (cabecera), `feature_list.json` (entrada), `docs/specs/layout-tablet-coherente.md`, y el código completo de `CabeceraPantalla`, `LayoutAncho`, `Theme`, `Type`, `TallyBottomBar`, `PeriodoSelector`, `RangoFechasDialog`, `ListadoTicketsUi`, `FormularioTicketEditable`, `PapelTicket`, `BadgeMotor`, `FotoOriginal`, `EtiquetaPeriodo`, `ResumenScreen`, `TarjetaTotal`, `TarjetaDistribucion`, `SeccionRecientes`, `GastosScreen`, `TicketDetalleScreen`, `AjustesScreen`, `EscanearScreen`, `ProcesadoContent`, `RevisionFotoContent`, `RevisionDatosContent`, `CameraEncuadre`, `TallyApp`, `MainActivity`, `AndroidManifest.xml`, `themes.xml`, y 14 capturas de `docs/evidence/layout-tablet-coherente`, `papel-ticket`, `detalle-ticket`. No inspeccionado: `DESIGN.md` y `docs/design/stitch/` (se asume la regla ya citada por specs previas: mayúsculas, radios, 20/24 dp), contenido de los E2E salvo los hits de grep.

Hechos clave: el layout 720 dp, `CabeceraPantalla` y `ColumnaCentrada` ya existen (rama actual). `compileSdk/targetSdk 36`, `minSdk 26`. `grep imePadding|enableEdgeToEdge|consumeWindowInsets` en el repo: 0 resultados.

## Technical approach (F1)

- V01: en `TallyApp.kt` el `NavHost` pasa a `Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)`; los `statusBarsPadding()` de las pantallas resuelven a 0 y se mantienen (Resumen/Gastos/Detalle/Revisión). Sin tocar el `Scaffold` ni la barra inferior. Comprobar que Procesado/Permiso/Error no pierden el inset (lo aporta `innerPadding`).
- V02: `MainActivity` llama a `enableEdgeToEdge(statusBarStyle = SystemBarStyle.light(TRANSPARENT, TRANSPARENT), navigationBarStyle = SystemBarStyle.light(TRANSPARENT, TRANSPARENT))` antes de `setContent` (API de `androidx.activity`, ya presente; sin dependencia nueva). Si `SystemBarStyle` no resolviera con la versión del catálogo, usar `WindowCompat.getInsetsController` con `isAppearanceLightStatusBars = true` y registrar el motivo.
- V05: `TallyShapes.extraLarge = RoundedCornerShape(28.dp)` en `Theme.kt`; retirar los parches `shape=` de `RangoFechasDialog.kt:81` y `TicketDetalleScreen.kt:387`; comprobar con `grep "shapes.extraLarge"`/`ExtraLarge` que nada más dependa de la píldora (botones y chips ya fijan `9999.dp` explícito). Ajustes: `Button` rojo como Detalle (`confirmar_eliminar_modelo` se conserva).
- V07: `CabeceraPantalla` (con `statusBarsPadding` heredado de V01) arriba de `ProcesandoExtraccionContent`, `ExtraccionListaContent`, `ExtraccionFallidaContent`; `CabeceraDeProceso` (badge + "Procesando en...") queda debajo. Margen lateral 20 dp.
- V03/V04: en `ContenidoDock`, si `LocalDensity.current.fontScale > 1.3f` los botones se apilan en `Column` (Guardar primero) y la fila de total pasa a dos líneas (etiqueta/total); mismo patrón `apilar` que `FilaLinea`. En `PasosDeProceso` con fontScale > 1.4 usar `Column` o `FlowRow`, y `maxLines=1` + `softWrap=false` en `ChipPaso`.
- V06: `FotoOriginal` calcula `bitmap.width/height` y aplica `Modifier.aspectRatio(r)` dentro de un `BoxWithConstraints`/`fillMaxSize`, de modo que `clip` y `border` rodeen la imagen real.
- V08: quitar el chip de `TarjetaTotal` (y su parámetro `periodo`/`hoy` si queda sin uso); el rango queda solo en la cabecera.
- V09: "período" -> "periodo" en `TarjetaTotal.kt:130` y `FormatoResumen.kt:7`; no se tocan los mensajes de excepción de dominio (no son UI).
- V10/V11: quitar `start/end=4` de `CabeceraDia` y `start=4` de `SeccionRecientes`; nueva constante `val PADDING_PANTALLA = 20.dp` en `LayoutAncho.kt` y usarla en Detalle, Revisión, Ajustes, Revisar foto, dock y botones de Procesado (24 -> 20; los estados centrados de permiso/error se quedan a 24). Alinear "campos extraídos" dentro de un contenedor del ancho del papel.
- V19: `NavHost(enterTransition, exitTransition, popEnterTransition, popExitTransition)`: entre las 3 pestañas `fadeIn/fadeOut(150)`; hacia/desde `Ticket` y `Ajustes` `slideInHorizontally`/`slideOutHorizontally` (300). Decidir por `initialState/targetState.destination.route`.
- V20: `imePadding()` en las columnas raíz de `TicketDetalleContenido` y `RevisionDatosContent`; verificar en emulador (teclado con `adb shell input text` sobre un campo enfocado y captura).

## Expected file changes (F1)

Editar: `feature/common/.../theme/Theme.kt`, `LayoutAncho.kt` (`PADDING_PANTALLA`), `ListadoTicketsUi.kt`, `FormularioTicketEditable.kt` (dock), `FotoOriginal.kt`; `feature/resumen/.../TarjetaTotal.kt`, `FormatoResumen.kt`, `SeccionRecientes.kt`, `ResumenScreen.kt`; `feature/gastos/.../TicketDetalleScreen.kt`; `feature/ajustes/.../AjustesScreen.kt`; `feature/escanear/.../ProcesadoContent.kt`, `RevisionFotoContent.kt`, `RevisionDatosContent.kt`; `app/.../MainActivity.kt`, `app/.../navigation/TallyApp.kt`. Tests: `FormatoResumenTest.kt:38`, `ResumenTotalFlujoTest.kt:84,106`, `RangoPersonalizadoFlujoTest.kt:157,181,207`, `PeriodoSelectorFlujoTest.kt:109`. `DESIGN.md`: añadir la escala de radios (12 filas, 16 interior, 24 tarjetas, 28 diálogos/`extraLarge`, píldora explícita), margen de pantalla 20 dp y la regla "un solo inset superior". No crear ficheros nuevos salvo que algo falte (comprobar antes). No tocar `:domain:*`, `:framework:*`, manifest, `libs.versions.toml`.

## Impacto en tests (cambios de texto/tag/estructura)

| Cambio | Tests afectados | Qué se actualiza y por qué no debilita |
| --- | --- | --- |
| V08: desaparece el tag `periodo_rango` | `RangoPersonalizadoFlujoTest` (3 usos), `PeriodoSelectorFlujoTest:109` | Se asierta `resumen_rango` con el MISMO valor esperado (`etiquetaPeriodo(...)`): el dato sigue verificado en el nodo que ya existe y es el único que lo muestra |
| V09: "período" -> "periodo" | `FormatoResumenTest:38`, `ResumenTotalFlujoTest:84,106` | Cambia solo la constante literal esperada; misma aserción de igualdad exacta |
| V07: cabeceras nuevas en Procesado | `ProcesadoFlujoTest`, `RevisionDatosFlujoTest` | Sin cambios esperados; el implementer comprueba `onNodeWithText` ambiguos ("Revisar ticket" es botón en Resultado y título en Revisión, estados excluyentes; el título de Resultado/Fallo es "Resultado", sin colisión). Si aparece ambigüedad se cambia el texto de la cabecera, no el test |
| V01/V11/V20: posiciones y paddings | E2E con `boundsInRoot` (`ListadoGastosFlujoTest`) | Solo se usan posiciones relativas; si alguno falla se valida que el orden vertical sigue igual antes de tocar nada |
| Tags conservados | todos los demás | Ningún tag se elimina salvo `periodo_rango` |

## Verification plan

- JVM: `gradlew.bat assembleDebug testDebugUnitTest --console=plain` (hoy 197 tests, 0 fallos).
- Instrumentados solo en `emulator-5556` (Pixel_10_Pro, API 37): `adb -s emulator-5556 install -r` de `:app` y `androidTest`, luego `am instrument -w` de la suite `:app` completa (patrón de la sesión 059). Nunca `connected*`, nunca `pm clear`; la tablet física no se toca; no leer el token HF.
- Capturas antes/después (`adb exec-out screencap -p`) en `docs/evidence/mejoras-visuales/`: móvil por defecto, 360 dp (`wm size 720x1560` + `wm density 320`), fontScale 1,3 y 2,0 (`settings put system font_scale`), horizontal 1280x800 dp (`wm size 2560x1600` + `wm density 320`, rotación `user_rotation 1`/`accelerometer_rotation 0`). Pantallas: Resumen, Gastos, Detalle (con cambios), Ajustes (+ diálogo), Revisar foto, Procesado (resultado), Revisar ticket, teclado abierto. Guardar antes de empezar los valores de `wm size`, `wm density`, `font_scale`, `user_rotation`, `accelerometer_rotation` y restaurarlos al final (`wm size reset`, `wm density reset`).
- Criterio: V01 medible en captura (título a la misma y en 7 pantallas); V02 iconos oscuros; V03/V04 sin elipsis ni círculo a 1,3 y 2,0; V05 esquinas 28 dp; V06 borde ajustado a la imagen; V20 dock visible con teclado.

## Implementation tasks (orden)

1. Línea base: `gradlew.bat assembleDebug testDebugUnitTest` verde; capturas "antes" que falten (en `layout-tablet-coherente` ya hay móvil/360/tablet; faltan fontScale 1,3/2,0 de Resumen/Gastos/Ajustes/Procesado y teclado).
2. Tema y shell: V05 (`Theme.kt`, retirar parches, Ajustes), V02 (`MainActivity`), V01 (`TallyApp`), V19 (transiciones). Compilar y capturar Resumen/Revisar foto.
3. Constante `PADDING_PANTALLA` y V10/V11 en Gastos, Resumen, Detalle, Revisión, Ajustes, Revisar foto.
4. V07 cabecera de Procesado, V10 completitud alineada, V04 pasos con fuente grande.
5. V03 dock con fuente grande, V20 `imePadding`.
6. V06 `FotoOriginal`.
7. V08/V09 duplicado y ortografía + actualización de los 4 tests listados.
8. `DESIGN.md`; JVM + suite `:app` en `emulator-5556`; capturas "después"; el orquestador registra evidencia en `PROGRESS.md`/`feature_list.json`.

## Evidence to capture

Salida de `testDebugUnitTest` y de `am instrument` (nº de tests, 0 fallos); tabla antes/después por configuración; confirmación de restauración (`wm size`, `wm density`, `font_scale`, rotación); lista de textos cambiados (periodo, "Resultado", chip eliminado) y del tag eliminado `periodo_rango`.

## Validator checklist

- [ ] V01-V11, V19, V20 cumplidos según los escenarios; capturas lo demuestran.
- [ ] `extraLarge` del tema = 28 dp y ningún diálogo con `RoundedCornerShape(28.dp)` de parche.
- [ ] Solo desaparece el tag `periodo_rango`; los tests tocados mantienen aserciones equivalentes (valor exacto).
- [ ] Sin dependencias, módulos, versiones nuevas; dominio y framework intactos; features sin importarse entre sí.
- [ ] Suite `:app` y JVM en verde; emulador restaurado; sin `pm clear`; la tablet no se tocó; sin lectura del token.
- [ ] F2/F3 no se han implementado a escondidas (alcance).

## Decisiones abiertas (default)

- A1 Campos del papel (V26): default = TextField de solo subrayado, en F3, tras revisar capturas de F1; el usuario decide si quiere cambiar el aspecto de formulario por el de recibo.
- A2 Duplicado del rango (V08): default = se quita el chip de `TarjetaTotal` y se conserva el subtítulo de cabecera (coherente con Gastos). Alternativa: quitar el subtítulo en Resumen.
- A3 Forma `extraLarge` (V05): default = 28 dp en el tema (un solo sitio). Alternativa: conservar píldora y fijar forma en cada diálogo.
- A4 Reparto: default = dividir en `mejoras-visuales` (F1) y `mejoras-visuales-2` (F2); el orquestador debe crear la entrada nueva.

## Riesgos

- `consumeWindowInsets` + `statusBarsPadding` puede dejar el inset en 0 en algún estado de Escanear (cámara) si el contenedor no recibe `innerPadding`; revisar con captura de la cámara (no hay cámara real en el AVD, usar el estado de permiso/encuadre disponible).
- Cambiar `extraLarge` afecta a cualquier componente M3 que lo use por defecto (DatePicker, ModalBottomSheet si lo hubiera): verificar con captura del picker.
- `enableEdgeToEdge` con `SystemBarStyle.light` en API <35 cambia el color de la barra; verificación solo en API 37 (documentar la limitación).
- Transiciones: durante el cruce hay dos pantallas compuestas; vigilar tags duplicados en E2E (el fundido actual de 700 ms ya lo permite).

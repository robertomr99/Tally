# Feature Implementation Spec: Gráfica de distribución por categoría (donut)

## Source Feature

- `id`: grafica-donut
- `area`: seguimiento
- `depends_on`: periodo-selector (`accepted`); resumen-total también `accepted` (ya aporta `TarjetaTotal`).
- `status`: not_started (al planificar)
- `source`: `feature_list.json`; `DESIGN.md` > Open Design Question #1 (se resuelve aquí).
- Comparte contenedor (`ResumenScreen`) con `listado-gastos`, que NO se implementa aquí.

## Goal

Debajo de la tarjeta de total, una segunda tarjeta "Distribución por categorías" (mockup 02, líneas 201-400): donut dibujado con `Canvas` de Compose, leyenda, y desglose detallado con importe, porcentaje y barra tonal por categoría, para el período activo; con estado vacío cuando no hay gasto. El reparto (céntimos, porcentajes que suman 100, orden, categorías a 0 omitidas) se calcula en dominio con una función pura testeable. Resuelve la paleta de categorías (propuesta en D1).

## Non-Goals

- Listado de gastos / detalle de ticket (`listado-gastos`); rango personalizado; `Periodo.anterior()` ya existe, no se toca.
- Interacción de filtrado por categoría (tocar un segmento o fila). El mockup solo trae `hover` y un botón decorativo `query_stats`: NO se implementa (ver D6).
- Animaciones (el mockup tiene `transition-all`; DESIGN.md prohíbe inventar animaciones fuera de mockup 06).
- Librerías de gráficas, módulos Gradle nuevos, cambios en Room/DAO/esquema, dependencias nuevas, `build-logic/`.
- Subtítulos descriptivos por categoría del mockup ("Supermercados y restaurantes"): decoración inventada, se omiten (D7).
- Tema oscuro: no inspeccionado; la paleta se define para el tema claro existente.

## Job Story

Cuando miro el panel,
quiero ver en qué categorías se me va el gasto del período,
para detectar de un vistazo dónde recortar.

## Acceptance Scenarios

### Scenario 1: Distribución con datos conocidos (dominio)
Given tickets del período: ALIMENTACION 10,00 + 28,00, HOGAR 20,00, TRANSPORTE 15,00 (total 73,00)
When se calcula la distribución
Then porciones ordenadas por importe desc: ALIMENTACION 3800 cént. (52 %), HOGAR 2000 (27 %), TRANSPORTE 1500 (21 %); la suma de porcentajes enteros es exactamente 100; `totalCentimos = 7300`.

### Scenario 2: Porcentajes suman 100 (resto mayor)
Given tres categorías con importes iguales (1,00 cada una) o 33,3 %/33,3 %/33,3 %
When se reparte
Then los porcentajes son 34/33/33 (el resto sobrante va a las mayores partes fraccionarias; empate: más céntimos, luego orden de `Categoria.entries`) y suman 100. Con 6 categorías de reparto 16,67 % cada una suman 100.

### Scenario 3: Categorías a cero omitidas y orden estable
Given solo hay gasto en OCIO y SALUD (las otras 4 con 0 o sin tickets)
Then la distribución contiene exactamente 2 porciones; leyenda, desglose y donut no pintan las categorías ausentes. Importes iguales ordenan por `Categoria.ordinal`.

### Scenario 4: Estado vacío
Given el período no tiene tickets (o todos suman <= 0 céntimos)
Then dominio devuelve `DistribucionCategorias.VACIA` (sin porciones, total 0) y la UI pinta el anillo de fondo vacío, centro "Sin gastos" y el copy "Aún no hay gastos en este periodo.", sin leyenda ni desglose, tag `donut_vacio`. Sin crash ni NaN.

### Scenario 5: Una sola categoría
Given todo el gasto en HOGAR
Then 1 porción al 100 %: anillo completo de ese color (sin hueco de separación), centro "Mayor gasto 100 % Hogar".

### Scenario 6: Proporciones del donut y reactividad
Given el dataset del escenario 1
Then los barridos de los arcos son proporcionales a `céntimos/total` (no a los porcentajes redondeados), parten a las 12 h (-90°) en sentido horario, y la suma de barridos + huecos es 360°. Al alternar Día/Semana/Mes o guardar/borrar un ticket, donut, leyenda y desglose se recalculan solos (Flow), sin `load()`.

### Scenario 7: Accesibilidad y formato
Then el donut lleva `contentDescription` con el texto equivalente ("Alimentación 52 %, Hogar 27 %, ..."); cada categoría es identificable sin color (nombre + icono en desglose, nombre + % en leyenda); importes `1.428,50 €` y porcentajes con `tnum`.

## Repository Research

### Files Inspected
- `AGENTS.md`, `CLAUDE.md`, `PROGRESS.md` (primeras 670 líneas), `feature_list.json` (grafica-donut, seed-demo), `DESIGN.md` (Open Design Question #1, Accessibility Baseline: gráfica nunca única fuente), `docs/domain-model.md` (Categoría: enum fija de 6 con icono y color), `docs/design/stitch/stitch-design-system.md` (sección "Semantic Financial Categories"), `docs/design/stitch/02-resumen-principal/code.html` líneas 201-400 (se usó el HTML; `screen.png` no abierto), `docs/specs/periodo-selector.md` (cabecera) y `docs/specs/resumen-total.md`.
- Dominio: `entities/Categoria.kt` (ALIMENTACION, HOGAR, TRANSPORTE, OCIO, SALUD, OTROS), `Ticket.kt` (`total: Double`, `categoria`), `usecases/ObservarTicketsDelPeriodo.kt` ("total, donut y listado lo reutilizan"), `ObservarResumenDelPeriodo.kt` (`combine` + `Clock`).
- Presentación: `feature/resumen/.../ResumenViewModel.kt` (`ResumenUiState(cargando, tipo, periodo, resumen)`, `flatMapLatest` + `stateIn`), `ResumenScreen.kt` (Column con `TarjetaTotal` dentro de `spacedBy(12.dp)`), `TarjetaTotal.kt` (patrón de tarjeta: `Surface` 24 dp `surfaceContainerLow` + borde `outlineVariant` 40 %), `FormatoResumen.kt` (`EstiloTabular`, `formatoImporte` en `EtiquetaPeriodo.kt`), `feature/resumen/build.gradle.kts` (deps: `:domain:expenses`, icons-extended, `:test:unit` en test; NO depende de `:feature:common`), `feature/common` (solo tema + `TallyBottomBar`; solo `:app` lo consume).
- `feature/escanear/.../ProcesadoContent.kt` (`etiquetaCategoria` internal, copy es: "Alimentación"...) y `RevisionDatosContent.kt` (`iconoCategoria`: Restaurant, Home, DirectionsCar... internal); `app/src/androidTest/.../resumen/ResumenTotalFlujoTest.kt` (patrón de siembra con `TicketRepositoryEntryPoint`, línea base + limpieza en `@After`).

### Mockup 02 (traducción)
- Tarjeta `surface-container-low rounded-3xl p-5` con borde; título "Distribución por Categorías" (title-md semibold) + subtítulo ("6 categorías activas procesadas con IA" -> aquí "N categorías con gasto").
- Donut 176 dp (viewBox 100, r=38, trazo 12 -> grosor ≈ 21 dp), anillo de fondo `#dee4df` (= `surfaceContainerHighest`), arcos desde -90°; centro: "MAYOR GASTO" (label-sm uppercase) / "38%" (title-md bold, `primary`) / nombre.
- Leyenda: rejilla de 2 columnas, punto 12 dp + "Nombre (38%)". Desglose: cabecera "DESGLOSE DETALLADO" / "IMPORTE / RATIO"; fila `surface-container rounded-2xl p-3`: icono en tile 36 dp coloreado, nombre semibold, importe `title-sm` bold `primary` tnum + porcentaje a la derecha, y barra de 6 dp (`surface-container-highest`) con relleno del color de la categoría.
- El mockup usa una rampa de verdes/grises (`#133e2b, #2b6a47, #414943, #71ab9a, #a3d1b6, #c1c8c1`): ver D1.

### Current Gaps
- No existe `DistribucionCategorias`, ni color/etiqueta/icono de categoría en `:feature:resumen`. `etiquetaCategoria`/`iconoCategoria` son `internal` en `:feature:escanear` (los features no se conocen entre sí).

## Technical Approach

**Dominio (`:domain:expenses`, Kotlin puro).**
- `entities/DistribucionCategorias.kt`: `data class PorcionCategoria(categoria: Categoria, centimos: Long, fraccion: Double, porcentaje: Int)`; `data class DistribucionCategorias(porciones: List<PorcionCategoria>, totalCentimos: Long)` con `companion VACIA` y `val estaVacia`. `fraccion = centimos / totalCentimos` (para ángulos y barras); `porcentaje` entero para texto.
- `usecases/CalcularDistribucionCategorias.kt` (`@Inject constructor()`, función pura `invoke(tickets: List<Ticket>): DistribucionCategorias`, sin Clock):
  1. Céntimos por ticket: `BigDecimal.valueOf(total).movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()` (evita errores binarios tipo 1,005); no finito o <= 0 -> se ignora.
  2. Agrupar por `categoria` y sumar (`Long`); descartar categorías con suma <= 0. Si no queda ninguna -> `VACIA`.
  3. Porcentajes por método del resto mayor: `base = cént*100 / total` (entero), resto `cént*100 % total`; repartir `100 - sum(base)` unidades de a 1 a las porciones con mayor resto (empate: más céntimos, luego menor `ordinal`). Garantiza suma 100.
  4. Orden final: céntimos desc, empate por `ordinal`. "Otros" no se fuerza al final (D4).
- `usecases/ObservarDistribucionDelPeriodo.kt` (`@Inject constructor(observarTickets: ObservarTicketsDelPeriodo, calcular: CalcularDistribucionCategorias)`): `invoke(periodo): Flow<DistribucionCategorias> = observarTickets(periodo).map(calcular::invoke)`. Sin repositorio nuevo ni cambios de Room.
- Los céntimos suman lo mismo que `ResumenPeriodo.total` redondeado salvo diferencia de 0,01 € por redondeo por ticket (aceptado; los tests usan importes de 2 decimales).

**Presentación (`:feature:resumen`).**
- `ResumenUiState` gana `distribucion: DistribucionCategorias? = null`. `ResumenViewModel` inyecta además `ObservarDistribucionDelPeriodo` y en el `flatMapLatest` combina `observarResumenDelPeriodo(periodo)` con `observarDistribucion(periodo)` (`combine`), sin `load()`.
- `TarjetaDistribucion.kt` (composable `internal`, sin ViewModel): cabecera, `DonutCategorias`, `LeyendaCategorias`, `DesgloseCategorias`, `EstadoVacioDistribucion`. Layout: `BoxWithConstraints`; si `maxWidth >= 480.dp` donut y leyenda en fila, si no en columna (como `flex-col sm:flex-row`). Contenedor con `widthIn(max=...)` centrado en tablet (DESIGN.md).
- `DonutCategorias`: `Canvas` 176 dp; pista con `drawArc(useCenter=false, style=Stroke(width = size*0.12, cap=Butt))` en `surfaceContainerHighest`; arcos desde -90°. Barridos calculados por función pura `segmentosDonut(porciones, huecoGrados = 2f): List<SegmentoDonut(inicio, barrido)>` (en fichero propio, testeable en JVM): con 1 sola porción 360° sin hueco; con N>1 se resta `huecoGrados` a cada barrido, mínimo 1°. El centro (Column superpuesta): "MAYOR GASTO" / `{porcentaje} %` / nombre de la 1.ª porción. `semantics { contentDescription = ... }` con el texto equivalente.
- `Leyenda`: `FlowRow`/grid 2 columnas, punto 12 dp del color + "Nombre (38 %)". `Desglose`: filas con tile 36 dp (color + icono blanco), nombre, importe `formatoImporte(centimos/100.0)` y `38 %` con `EstiloTabular`, y barra 6 dp con `fraccion` (`fillMaxWidth(fraccion.toFloat())`). Porcentaje 0 con gasto > 0 se muestra "<1 %".
- `CategoriaUi.kt`: `etiquetaCategoria`, `iconoCategoria`, `colorCategoria(categoria)` (mismas iconos/etiquetas que escanear). Ubicación: ver D2.
- Tags para E2E: `donut_grafica`, `donut_vacio`, `donut_centro`, `leyenda_<CATEGORIA>`, `desglose_<CATEGORIA>`, `desglose_<CATEGORIA>_importe`, `desglose_<CATEGORIA>_porcentaje` (`<CATEGORIA>` = `Categoria.name`).

## Expected File Changes

- `domain/expenses/.../entities/DistribucionCategorias.kt` — create.
- `domain/expenses/.../usecases/CalcularDistribucionCategorias.kt`, `ObservarDistribucionDelPeriodo.kt` — create.
- `domain/expenses/src/test/.../usecases/CalcularDistribucionCategoriasTest.kt`, `ObservarDistribucionDelPeriodoTest.kt` — create (`FakeTicketDataSource` de `:test:unit`, Turbine).
- `feature/resumen/.../ResumenViewModel.kt`, `ResumenScreen.kt` — modify; `TarjetaDistribucion.kt`, `DonutCategorias.kt` (incluye `segmentosDonut`), `CategoriaUi.kt` — create.
- `feature/resumen/src/test/.../ResumenViewModelTest.kt` — modify; `SegmentosDonutTest.kt` — create.
- `app/src/androidTest/.../resumen/GraficaDonutFlujoTest.kt` — create.
- `feature/resumen/build.gradle.kts` — modify solo si D2 = `:feature:common` (añadir `implementation(project(":feature:common"))`; sin versiones). `gradle/libs.versions.toml`: sin cambios.
- `DESIGN.md` — update: cerrar Open Design Question #1 con la decisión y la paleta final con contraste. `PROGRESS.md`, `feature_list.json` — update por orquestador/implementador. Sin `di.kt` nuevo (casos de uso `@Inject`).

## Visual Design Impact

UI: sí. Fuente: mockup 02 (líneas 201-400) + `DESIGN.md`. Estados: normal, vacío (anillo de fondo + copy), cargando (`distribucion == null`: tarjeta con anillo de fondo y "—" en el centro, sin spinner), una sola categoría. No se copian "procesadas con IA" ni el botón `query_stats`. Capturas en `docs/evidence/grafica-donut/` (6 categorías, 2 categorías, vacío, tablet si hay Tab; la revisión visual de imagen la hace un humano).

## Durable Documentation Impact

- `DESIGN.md`: update (cerrar Question #1; sección Colors con la paleta de categorías). `CONTEXT.md`: not needed. `docs/domain-model.md`: not needed (la categoría ya declara "color en UI"). `docs/architecture.md`: update solo si enumera casos de uso. `AGENTS.md`, `ARCHITECTURE.md`, `CONSTRAINTS.md`: not needed.

## Implementation Plan

1. TDD de dominio: `CalcularDistribucionCategorias` (céntimos, resto mayor, omitidos, orden, vacío).
2. `ObservarDistribucionDelPeriodo` + test de reactividad.
3. ViewModel/UiState y su test.
4. `segmentosDonut` + test; `CategoriaUi` (decidir D1/D2 antes).
5. `TarjetaDistribucion` y subcomponentes; integrar en `ResumenScreen`.
6. E2E, regresión, capturas, `DESIGN.md`, evidencia.

## Implementation Tasks

- [ ] T1 `DistribucionCategorias` + `CalcularDistribucionCategorias` y test: escenarios 1-5; 3x33,33 % (34/33/33); 6 categorías iguales; 7 categorías de reparto desigual con restos (suma 100); empate de restos por céntimos y `ordinal`; importe con 3 decimales (1,005 -> 101 cént.); total <= 0, `NaN` e infinito ignorados; lista vacía; categoría con 0,00; porción diminuta (0,01 € frente a 1000 €) conserva `porcentaje` 0 y `fraccion` > 0.
- [ ] T2 `ObservarDistribucionDelPeriodo` + test (Turbine): guardar un ticket de otra categoría añade porción; borrarlo la quita; ticket fuera del período no cuenta.
- [ ] T3 `ResumenViewModel`/`ResumenUiState` + `ResumenViewModelTest`: alternar tipo cambia `distribucion`; período vacío -> `VACIA`; `distribucion == null` solo mientras carga.
- [ ] T4 `segmentosDonut` + `SegmentosDonutTest`: suma de barridos + huecos = 360°, 1 porción = 360° sin hueco, orden y arranque -90°, barrido mínimo 1°, proporcional a `fraccion`.
- [ ] T5 `CategoriaUi` (paleta según D1) y test JVM de contraste: cada color vs `surfaceContainerLow` >= 3:1 y vs `surfaceContainerHighest` (pista) >= 3:1; pares adyacentes distinguibles (ver D1).
- [ ] T6 `TarjetaDistribucion` + subcomponentes; vacío/cargando/1 categoría; `contentDescription`; tags; `tnum` en importes y porcentajes; integrar bajo `TarjetaTotal`.
- [ ] T7 E2E `GraficaDonutFlujoTest` (ver verificación).
- [ ] T8 Verificación completa, revisión de reglas, `DESIGN.md`, `PROGRESS.md` y `feature_list.json` con evidencia.

## Verification Plan

- `.\gradlew.bat assembleDebug testDebugUnitTest --console=plain` (o `bash init.sh`): verde.
- `.\gradlew.bat :domain:expenses:test --rerun` y `:feature:resumen:testDebugUnitTest --rerun`: conteo JUnit por clase (cubre "proporciones correctas con dataset de prueba" y "estado vacío").
- E2E en **emulator-5556** (5554 está offline): `:app:assembleDebug :app:assembleDebugAndroidTest`, instalar ambos APK y `adb -s emulator-5556 shell am instrument -w -e class com.robertomr.tally.resumen.GraficaDonutFlujoTest <paquete>.test/androidx.test.runner.AndroidJUnitRunner`. **Nunca** `connected*`. No leer el token HF.
  - Caso A: sembrar con el repositorio real tickets de ≥3 categorías hoy (patrón de `ResumenTotalFlujoTest`: línea base leída + sembrado, limpieza en `@After`); comprobar por tag que existen leyenda/desglose de esas categorías, que `desglose_<CAT>_importe` coincide con la expectativa calculada de forma independiente en el test, que los `%` mostrados suman 100 y que las categorías sin gasto no existen (`assertDoesNotExist`).
  - Caso B: alternar Día/Semana/Mes y comprobar que cambia la distribución.
  - Caso C (vacío): período Día sin tickets (si la línea base de hoy no está vacía, `assumeTrue` y cubrir el vacío en JVM) -> `donut_vacio` visible, sin desglose.
  - Regresión E2E: `ResumenTotalFlujoTest`, `PeriodoSelectorFlujoTest`, `NavigationTest` (omitir `*RealTest`).
- Verificación 3 de feature_list: revisión de código de `colorCategoria` + test de contraste (T5) + captura en emulador con las 6 categorías en `docs/evidence/grafica-donut/`; la revisión visual (incluida simulación de daltonismo: emulador Ajustes > Accesibilidad > Corrección de color, deuteranomalía/protanomalía) queda a humano.
- Revisión: 0 imports de Android en `domain/**`; `BigDecimal`/`java.math` aceptado en dominio; sin librerías de gráficas; sin módulos ni dependencias nuevas; sin versiones literales; sin `NavController`; `git diff` vacío en `build-logic/`.

## Evidence To Capture

- Conteo JUnit por clase; salida de `am instrument` en emulator-5556; capturas (6 categorías, 2 categorías, vacío); resultado del test de contraste con la paleta final; decisiones D1-D7 resueltas en `PROGRESS.md`.

## Validator Checklist

- [ ] Alcance: sin listado, sin rango personalizado, sin filtrado por categoría, sin animaciones.
- [ ] Reparto: porcentajes suman 100 en todos los datasets de test; céntimos en `Long`; 0 omitidas; orden determinista.
- [ ] Donut con `Canvas` (sin librerías), arcos proporcionales a `fraccion`, arranque -90°.
- [ ] Estado vacío y cargando sin NaN ni crash; leyenda y desglose siempre presentes (gráfica no única fuente).
- [ ] Paleta conforme a D1 y contraste >= 3:1 demostrado; color + nombre + (icono en desglose).
- [ ] `tnum` en importes y porcentajes; formato `1.428,50 €`.
- [ ] Dominio puro, Flow reactivo sin `load()`, sin módulos nuevos, sin versiones literales; `DESIGN.md` actualizado.
- [ ] Tests y E2E existentes en verde.

## Open Decisions (defaults si no hay respuesta)

- **D1 Paleta de categorías (Open Design Question #1).** Default: **paleta semántica apagada del design system**, un color fijo por categoría (no por posición): ALIMENTACION ámbar `#C87D32`, TRANSPORTE azul acero `#4A6B82`, OCIO ciruela `#7A587A`, HOGAR musgo `#5D6E3F`, SALUD verde azulado `#2E776E`, OTROS pizarra `#5F6561`. Justificación: (a) la rampa verde del mockup no discrimina 6 categorías: `#a3d1b6` y `#c1c8c1` apenas contrastan con la pista `#dee4df` (< 1,5:1, prácticamente invisibles) y los verdes contiguos tienen luminancia similar; (b) con paleta semántica la categoría conserva su color en gráfica, chips y futuro listado. Contraste estimado (WCAG, luminancia relativa) sobre `surfaceContainerLow #eff5f0`: acero ≈5,1:1, ciruela ≈5,4:1, musgo ≈5,1:1, verde azulado ≈4,8:1, pizarra ≈5,4:1, ámbar ≈2,9:1 (justo por debajo de 3:1 exigido a gráficos, WCAG 1.4.11). Propuesta: oscurecer solo el ámbar a `#B86A22` (≈3,7:1; icono blanco ≈4,1:1) y verificarlo en T5; si se prefiere fidelidad al design system, aceptar el desvío con nota. Daltonismo: ámbar/musgo y verde azulado/pizarra pueden confundirse en deuteranopia/protanopia, por eso el color nunca es el único canal: hueco de 2° entre arcos, leyenda con nombre y %, desglose con icono + nombre + importe, y orden por importe. Alternativa: rampa verde del mockup (más "marca", pero falla contraste y discriminación; no recomendada).
- **D2 Dónde viven `etiquetaCategoria`/`iconoCategoria`/`colorCategoria`.** Default: nuevo `CategoriaUi.kt` en `:feature:common` + `implementation(project(":feature:common"))` en `:feature:resumen` (no es módulo nuevo; permitido por AGENTS.md "Composables reutilizables -> feature/common"; lo reutilizará `listado-gastos`); `:feature:escanear` no se toca en esta feature (duplicación temporal anotada como deuda). Alternativa: copia local en `:feature:resumen` sin tocar Gradle. Si hubiera objeción a que un feature dependa de common, confirmar antes.
- **D3 Porcentaje entero vs un decimal.** Default entero con resto mayor (como el mockup, "38%"); "<1 %" para porciones diminutas. Alternativa: 1 decimal (más preciso, más ruido visual).
- **D4 Orden.** Default estrictamente por importe desc (empate por `ordinal`); "Otros" no va siempre al final. Alternativa: "Otros" último aunque no sea el menor.
- **D5 Cálculo en céntimos.** Default `Long` calculado desde `Ticket.total: Double` (redondeo HALF_UP vía `BigDecimal.valueOf`), sin migrar el modelo a céntimos (D6 de resumen-total sigue vigente).
- **D6 Interacción.** Default: sin filtrado ni selección (el mockup no define la acción de tocar; `listado-gastos` puede añadir "ver gastos de la categoría" después). Alternativa: tocar una fila resalta su segmento (no incluido por ser interacción nueva no diseñada).
- **D7 Textos del mockup.** Default: título "Distribución por categorías", subtítulo "N categorías con gasto", cabecera de desglose "DESGLOSE DETALLADO / IMPORTE / RATIO"; se omiten subtítulos por categoría, "procesadas con IA" y el botón `query_stats`.

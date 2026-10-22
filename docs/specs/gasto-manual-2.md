# Spec: gasto-manual-2

## Source feature

- id: `gasto-manual-2` | area: seguimiento | status: not_started | depends_on: `gasto-manual`, `forma-pago`, `mejoras-visuales-3`. Entrada de `feature_list.json` leída entera.
- Título: "Correcciones de gasto manual: cambio y total automáticos, y alta de gasto más descubrible".
- Origen (petición literal del usuario, 2026-10-22): (1) "el cambio no tiene que ponerse a mano... con lo entregado y lo gastado debería hacer la operación automáticamente"; (2) "el total con IVA no tiene sentido porque no se actualiza conforme le vas metiendo productos: o lo quitas y dejas solo el total recalculado o haces la suma automática"; (3) "el + para añadir un gasto nuevo lo veo poco intuitivo, dale una vuelta".
- Tamaño: en el límite superior pero cabe en una sesión (un dato nuevo en `DatosRevision`, sin Room ni migración; tres bloques: A cambio, B total, C botón). Si hay que recortar, difiere C (independiente de A y B) como `gasto-manual-3`; A y B no se separan porque comparten reductores y UI del formulario.

## Goal / Non-goals

Goal: que el formulario del papel (Nuevo gasto, Revisión y Detalle) no pida al usuario datos que se derivan de otros, que haya un único total coherente en pantalla, y que añadir un gasto sea reconocible sin adivinar qué hace un "+".

Non-goals: cambios de Room/esquema; tocar `Ticket`, el prompt o los motores de IA; editar IVA o desglose de impuestos (decoración prohibida por DESIGN.md); precios negativos (descuentos) en líneas; que el total deje de ser dato persistido; FAB, navigation rail o `WindowSizeClass`; cambiar el flujo de guardado, la validación de fecha o el filtro de forma de pago; módulos o dependencias nuevas.

## Job story

Cuando añado un gasto a mano (o corrijo un ticket), quiero escribir solo lo que sé (productos, o un total, y lo entregado) y que la app calcule el cambio y el total, para no hacer cuentas ni ver dos totales que se contradicen; y cuando abro Gastos quiero ver de un vistazo cómo añadir un gasto.

## Decisiones cerradas (defaults; el implementer no las reabre)

### A. Cambio (todos los flujos: Nuevo gasto, Revisión, Detalle)
- A1. Con Efectivo, `cambio = entregado - total` redondeado a céntimos, siempre derivado y de SOLO LECTURA. Se elimina `conCambioTexto` y el callback `onCambioEditado` de los tres ViewModels, de las tres pantallas y de `FormularioTicketEditable`. El campo `revision_cambio` pasa a `OutlinedTextField(readOnly = true)` (mantiene `EditableText`, así los asertos de lectura actuales siguen valiendo; no tiene `SetText`), etiqueta "Cambio (€)" y `supportingText` "Se calcula: entregado - total".
- A2. Sin `entregado` (vacío): `cambio = null`, el campo se ve vacío. Con `entregado < total` (tras redondear): `cambio = null` (nunca negativo) y aviso visible no bloqueante `ENTREGADO_MENOR_QUE_TOTAL` con el faltante: "Lo entregado no cubre el total: faltan X €. Puedes guardar igualmente." (icono + texto, tag `revision_aviso_pago`). Con `total <= 0` no se calcula (queda como esté).
- A3. Reductor único `DatosRevision.conPagoCoherente()` (en `Pago.kt`/`EdicionDatos.kt`) aplicado al final de TODO reductor que toca total, líneas (total automático), forma de pago o entregado, y dentro de `datosRevisionInicial`/`datosRevisionDe`. Solo actúa con `EFECTIVO`; con Tarjeta/No indicada deja entregado/cambio ocultos como hoy (se limpian al guardar). Caso extraído "solo cambio" (entregado null, cambio C, total T > 0): se infiere `entregado = T + C` al abrir, para que lo mostrado sea coherente; si luego el usuario borra entregado, `conEntregadoTexto("")` pone entregado y cambio a `null`.
- A4. Extracción por IA: `normalizarPago` RECALCULA el cambio cuando hay entregado y total > 0 (`E >= T` -> `E - T`; si no, `null`), ignorando el cambio extraído que contradiga. El cambio extraído solo se usa para inferir `entregado` cuando falta. Con `total <= 0` conserva lo extraído (no hay con qué calcular). Como el parser ya llama a `normalizarPago`, Procesado, Revisión, Detalle y lo persistido quedan coherentes (una sola fuente). `avisoPago` y `AvisoPago.CAMBIO_NO_CUADRA` se conservan sin cambios en dominio (los tests actuales los ejercitan con datos construidos a mano); la UI conserva su rama `when`, inalcanzable en uso normal. Justificación: el cambio deja de ser dato independiente, pero se persiste (listado/detalle lo leen) y por eso `normalizada()` lo recalcula al guardar (cura tickets antiguos con cambio tecleado distinto).

### B. Total único
- B1. Nuevo campo `DatosRevision.totalAutomatico: Boolean = false` (el valor por defecto conserva el comportamiento y los tests actuales de quien construye `DatosRevision` a mano). No se persiste: se deriva al abrir. Nueva función pura `totalDeLineas(lineas): Double` = suma `cantidad x precio` SOLO de las líneas con concepto no en blanco (las que `aLineas()` guardará), redondeada a céntimos. `totalRecalculado` se deja tal cual (tests existentes).
- B2. Modos: AUTOMÁTICO (`totalAutomatico = true`: `total = totalDeLineas`, campo de solo lectura) o TECLEADO (el usuario manda; si hay líneas y no cuadran, hay aviso). Con 0 líneas siempre TECLEADO.
- B3. Estado inicial: Nuevo gasto = TECLEADO, total 0. Revisión (`datosRevisionInicial`) y Detalle (`datosRevisionDe`): sin líneas -> TECLEADO; con líneas y `|suma - total| <= 0,01` -> AUTOMÁTICO; con líneas y total ausente/<= 0 -> AUTOMÁTICO con `total = suma`; con líneas y total que NO cuadra -> TECLEADO conservando el total del papel/guardado (nunca se pierde).
- B4. Reductores de línea (añadir, concepto, cantidad, precio, borrar) terminan en `conTotalSincronizado()`: sin líneas -> TECLEADO y el total queda como estaba (borrar la última línea no pierde la cifra); en AUTOMÁTICO -> `total = totalDeLineas`; en TECLEADO -> el total no se toca, salvo (a) si se añade la PRIMERA línea y `total == 0` -> pasa a AUTOMÁTICO, y (b) si tras editar `|suma - total| <= 0,01` y hay líneas -> pasa a AUTOMÁTICO (el aviso desaparece y, desde ahí, el total sigue a las líneas).
- B5. Transiciones explícitas del usuario, un solo callback nuevo `onTotalAutomaticoElegido: (Boolean) -> Unit` -> `DatosRevision.conTotalAutomatico(si)`: "Usar la suma de las líneas" (TECLEADO -> AUTOMÁTICO, `total = suma`) y "Editar total a mano" (AUTOMÁTICO -> TECLEADO, el total queda igual). `conTotalTexto` devuelve `null` (se ignora) en AUTOMÁTICO.
- B6. UI: se elimina "Total (con IVA)" y se elimina el bloque "Total recalculado" del dock. Queda UN total al final del papel, etiqueta "Total", tag `revision_total`, mismo estilo `headlineSmall` bold a la derecha:
  - AUTOMÁTICO: campo `readOnly`, `supportingText` "Suma de las líneas · N artículos", y debajo `TextButton` "Editar total a mano" (tag `revision_total_manual`, >= 48 dp).
  - TECLEADO sin líneas: campo editable como hoy.
  - TECLEADO con líneas que no cuadran: campo editable + aviso (icono `WarningAmber` + texto, tag `revision_aviso_total`, no bloqueante): "Las líneas suman S € y el total del ticket es T € (diferencia D €). Se guarda el total del ticket." + `TextButton` "Usar la suma de las líneas" (tag `revision_total_usar_suma`).
  - Fila de línea con concepto en blanco y precio > 0: `supportingText` en el concepto "Escribe un concepto para que cuente" (esa fila no se guarda ni suma).
- B7. Nuevo gasto: `errorGastoManual` no cambia (`total > 0`, fecha no futura). Texto del requisito: manual "Indica un total mayor que 0" (sin cambio, lo aserta `GastoManualFlujoTest:186,207`); en AUTOMÁTICO con suma 0 "Añade el precio de al menos un producto". `GuardarGastoManual`, `GuardarTicket` y `ActualizarTicket` siguen persistiendo `datos.total`; `normalizada()` re-sincroniza `total = totalDeLineas` si `totalAutomatico` (defensa).
- B8. Detalle: `hayCambios` compara ignorando solo el flag (`datos.copy(totalAutomatico = inicial.totalAutomatico) != inicial`) para que alternar el modo sin cambiar valores no habilite Guardar. Un ticket antiguo con líneas que no suman su total se abre en TECLEADO con el aviso (informativo, honesto); no se reescribe nada hasta que el usuario guarde.
- B9. Hoy en Nuevo gasto SÍ se pueden añadir líneas (`revision_anadir_linea`, `NuevoGastoViewModel.onLineaAnadida`); no hace falta UI nueva, solo el comportamiento de B4.

### C. Botón de añadir gasto
- C1. Se descarta el FAB: en el código no existe ningún FAB (la barra inferior ya aloja Escanear), DESIGN.md (línea ~316) lo rechazó para Gastos y tapar contenido a fontScale 2,0 en 360 dp obligaría a más buffer inferior y a competir con los botones del vacío.
- C2. Botón extendido en la cabecera de Gastos: `FilledTonalButton` (`secondaryContainer`/`onSecondaryContainer`, ya usado en chips activos y dock tonal; no compite con el primario "Escanear ticket" del vacío), pill `RoundedCornerShape(9999.dp)`, `heightIn(min = 48.dp)`, icono `Icons.Outlined.Add` 18 dp + texto "Añadir gasto" (`labelLarge`, `maxLines = 1`). Tag `gastos_anadir` conservado; el `contentDescription` del icono pasa a `null` (el texto ya nombra la acción).
- C3. Layouts: fontScale <= `UMBRAL_APILAR_BOTONES` (1,15): va como `accion` de `CabeceraPantalla`, a la derecha del título y subtítulo (360 dp: cabe, "Gastos" es corto; el subtítulo envuelve si hace falta). fontScale > 1,15 (incluye 2,0): la cabecera queda sin acción y el botón va en una fila propia a ancho completo (`PADDING_PANTALLA`, 48 dp mín.) justo bajo la cabecera, fija (no en el scroll) y sobre los selectores; sigue una sola copia del tag en pantalla. Tablet 720 dp: dentro de `ColumnaCentrada`, a la derecha del título como en móvil.
- C4. Vacío y filtro: el botón de cabecera sigue visible en ambos (`ListadoGastosVacioTest` asserta `gastos_anadir` y `gastos_anadir_vacio` visibles a la vez). `gastos_anadir_vacio` queda igual (texto "Añadir gasto manualmente", `OutlinedButton`). No cambia `onAnadirGasto` ni la ruta.

## Acceptance scenarios

1. Given Nuevo gasto en Efectivo con total 13,90, When escribo Entregado 50, Then Cambio muestra 36,10 sin poder editarse; al cambiar el total a 20 pasa a 30,00; con Entregado 5 el cambio queda vacío y aparece "faltan 8,90 €" sin bloquear Guardar; con Entregado vacío no hay cambio ni aviso.
2. Given Revisión y Detalle de un ticket en efectivo, Then el Cambio es de solo lectura y es `entregado - total` aunque la IA devolviera otro cambio; al guardar se persiste el calculado.
3. Given Nuevo gasto sin líneas, When tecleo un total 12,5, Then no hay aviso de total ni "Total recalculado"; Guardar se habilita.
4. Given Nuevo gasto con total 0, When añado una línea PAN 3 x 1,50 y otra AGUA 1 x 2, Then el Total pasa solo a 6,50 (solo lectura, "Suma de las líneas · 2 artículos") y sigue cada edición/borrado; al borrar las dos vuelve editable conservando 6,50. Guardar persiste 6,50.
5. Given Nuevo gasto con total tecleado 20 y una línea de 5, Then el total sigue en 20, hay aviso con diferencia 15,00 y botón "Usar la suma de las líneas"; al pulsarlo el total es 5,00 (automático). Si en cambio completo líneas hasta sumar 20,00 el aviso desaparece y el total pasa a automático.
6. Given un ticket escaneado con total extraído 35,30 y líneas que suman 2,10, Then Revisión muestra Total 35,30 editable + aviso con 33,20 de diferencia; Guardar persiste 35,30; editar líneas no cambia el total mientras no cuadre. Con total ausente y líneas, el total sale de la suma.
7. Given el Detalle de un ticket cuyo total cuadra con sus líneas, When cambio una cantidad, Then el total se actualiza solo; "Editar total a mano" lo vuelve editable.
8. Given Gastos a 360 dp y fontScale 1,0, 1,3, 2,0 y tablet 720 dp, Then el botón "Añadir gasto" (icono + texto) se ve completo y abre Nuevo gasto; con 1,3 y 2,0 va en fila propia a ancho completo; en el vacío y con el filtro vacío sigue presente.
9. Given la suite JVM y `:app`, Then verde sin debilitar tests; tags `revision_*`, `gastos_anadir`, `gastos_anadir_vacio` conservados; solo se retira `revision_total_recalculado` (ver "Tests que cambian").

## Repository research

Inspeccionado: `AGENTS.md`, `CLAUDE.md`, `PROGRESS.md` (cabecera), `feature_list.json` (entrada), `docs/specs/mejoras-visuales-3.md` y `gasto-manual.md`/`forma-pago.md` (por referencias), `DESIGN.md` (193-335), `CONTEXT.md`, `docs/domain-model.md` (77-103); código: `feature/common/FormularioTicketEditable.kt` (entero), `PapelTicket.kt` (parcial), `CabeceraPantalla.kt`, `TallyBottomBar.kt`; `feature/gastos/NuevoGastoViewModel.kt`, `NuevoGastoScreen.kt`, `GastosScreen.kt`, `TicketDetalleViewModel.kt` (parcial); `feature/escanear/RevisionDatosContent.kt` (parcial) y `EscanearViewModel.kt` (reductores); `domain/expenses`: `DatosRevision.kt`, `EdicionDatos.kt`, `Pago.kt`, `LineaEditable.kt`, `ErrorGastoManual.kt`, `GuardarGastoManual.kt`, `GuardarTicket.kt`, `ActualizarTicket.kt`; tests: `PagoTest`, `GuardarGastoManualTest` (parcial), `TicketDetalleViewModelPagoTest`, `EscanearViewModelTest` (960-1090), `NuevoGastoViewModelTest` (grep), `DetalleTicketFlujoTest`, `FormaPagoFlujoTest`, `RevisionDatosFlujoTest`, `NuevoGastoDockFlujoTest`, `FuentesGrandesFlujoTest`, `ListadoGastosVacioTest`. NO inspeccionado: `TicketDetalleScreen.kt` ni `RevisionDatosContent.kt` completos (solo las llamadas a `FormularioTicketEditable`/`DockEdicion` vía grep), `ProcesadoContent`, `TicketExtraidoParser`, `TallyApp.kt`, imágenes de evidencia.

Hechos que condicionan el diseño: `conTotalTexto`/`conFormaPago`/`conEntregadoTexto` ya recalculan cambio con `conCambioRecalculado` (privada, sin forzar a null); `aLineas()` descarta líneas sin concepto; `DockEdicion` recibe `lineas`+`mostrarTotalRecalculado` y los tests `FuentesGrandesFlujoTest` lo invocan con esos parámetros; `docs/domain-model.md` ("Total vs líneas") dice hoy "no hay sobrescritura automática": esta feature lo enmienda de forma acotada (solo en modo AUTOMÁTICO, nunca sobre un total tecleado/del papel que no cuadre).

## Technical approach / Expected file changes

Dominio (`domain/expenses`, Kotlin puro): `DatosRevision.kt` (+`totalAutomatico`, KDoc de "Total vs líneas" actualizado, iniciales con B3); `LineaEditable.kt` (+`totalDeLineas`); `EdicionDatos.kt` (+`conTotalSincronizado`, `conTotalAutomatico`, `conPagoCoherente`; líneas y pago los usan; `conTotalTexto` ignora en AUTOMÁTICO; borrar `conCambioTexto`; `conEntregadoTexto("")` limpia cambio; `datosRevisionDe` con B3); `Pago.kt` (`normalizarPago` recalcula; `normalizada()` re-sincroniza total). Sin cambios en `GuardarGastoManual`/`GuardarTicket`/`ActualizarTicket` salvo KDoc.

UI: `feature/common/FormularioTicketEditable.kt` (firma: `-onCambioEditado`, `+onTotalAutomaticoElegido`; bloque de total B6; `CampoImporteOpcional` read-only para cambio; `DockEdicion` sin `lineas` ni `mostrarTotalRecalculado`, borrar `SubtituloTotalRecalculado`; el campo de total editable debe re-sincronizar su texto cuando el dato cambia por otra vía, como `CampoImporteOpcional`, o usar ramas de composables distintas por modo); `feature/gastos/NuevoGastoViewModel.kt` y `NuevoGastoScreen.kt` (quitar `onCambioEditado`, añadir `onTotalAutomaticoElegido`, texto del requisito según modo), `TicketDetalleViewModel.kt` y `TicketDetalleScreen.kt` (idem, `hayCambios` B8), `feature/escanear/EscanearViewModel.kt`, `EscanearScreen.kt`, `RevisionDatosContent.kt` (idem), `feature/gastos/GastosScreen.kt` (botón C). Nuevo: ningún módulo ni fichero de producción.

## Visual design impact

Total único al final del papel; dock más ligero (solo Descartar/Guardar, ayuda a fontScale 2,0 donde ya se apilaba); cambio en tipografía de recibo, no editable; avisos con icono + texto (no solo color) en `onSurfaceVariant`; botón tonal en cabecera de Gastos. Sin colores ni tokens nuevos. Tablet: todo dentro de `ColumnaCentrada`.

## Durable documentation impact

- `DESIGN.md`: update (líneas ~242-255 cambio de solo lectura y aviso con faltante; ~290-292 dock sin "Total recalculado"; ~313-326 botón extendido "Añadir gasto" en vez de "+", fila propia > 1,15, y bloque Nuevo gasto sin "Total recalculado"; añadir el patrón de total único/aviso de diferencia).
- `docs/domain-model.md`: update ("Total vs líneas" líneas 77-79: suma automática y excepción del total que no cuadra; "Forma de pago" 97-98: el cambio ya no es editable y siempre se recalcula).
- `CONTEXT.md`: update (entrada "Cambio", línea ~32: se calcula; añadir "Total automático").
- `docs/evidence/gasto-manual-2/README.md`: crear. `AGENTS.md`, `docs/architecture.md`, ADRs, `build-brief.md`: not needed (sin cambio arquitectónico). `PROGRESS.md` y `feature_list.json`: los actualiza el orquestador (sugerencia: añadir a `verification` "cambio no editable en los tres flujos" y "aviso de diferencia total/líneas").

## Implementation plan / tasks (en orden)

1. Línea base `gradlew.bat assembleDebug testDebugUnitTest --console=plain`; `adb devices` y confirmar `emulator-5556`; anotar `wm size`, `wm density`, `font_scale`, rotación. Capturas ANTES (360 dp, 1,3, 2,0, tablet 2560x1600 @320): Gastos (con y sin tickets), Nuevo gasto con líneas, Detalle de un ticket en efectivo.
2. Dominio A: `conPagoCoherente`, `normalizarPago`, retirar `conCambioTexto`. Tests JVM.
3. Dominio B: `totalAutomatico`, `totalDeLineas`, reductores B3-B5, `normalizada()`. Tests JVM.
4. ViewModels (3) y cableado de callbacks; tests de ViewModel.
5. `FormularioTicketEditable` (cambio read-only, total único, avisos) y `DockEdicion`.
6. `GastosScreen` (botón C).
7. Actualizar los tests listados en "Tests que cambian" y añadir los nuevos; `gradlew.bat assembleDebug testDebugUnitTest`.
8. Instrumentados en `emulator-5556`; capturas DESPUÉS (mismas configuraciones); restaurar `wm size reset`, `wm density reset`, `font_scale 1.0`, rotación.
9. Docs (DESIGN, domain-model, CONTEXT) y README de evidencia.

## Verification plan

- Entorno: SOLO `emulator-5556`. Prohibido `connected*`, `pm clear`, instalar en dispositivo físico, `installDebug` sin `ANDROID_SERIAL=emulator-5556` (comprobar antes `adb devices`) y leer el token de Hugging Face. Instalar con `adb -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk` y el APK `androidTest` análogo.
- JVM: `gradlew.bat assembleDebug testDebugUnitTest --console=plain`.
- Instrumentados: `adb -s emulator-5556 shell am instrument -w -e notClass com.robertomr.tally.extraccion.ExtraccionLocalRealTest,com.robertomr.tally.extraccion.ExtraccionNubeRealTest com.robertomr.tally.test/androidx.test.runner.AndroidJUnitRunner` (confirmar nombre con `pm list instrumentation`); suite completa y clases nuevas aparte. Si una clase ajena cae por lentitud del emulador, repetirla aislada y anotarlo.
- Capturas con `adb -s emulator-5556 exec-out screencap -p` reducidas (480/800 px) en `docs/evidence/gasto-manual-2/` con prefijos `antes-`/`despues-`: Gastos (botón; vacío; filtro vacío), Nuevo gasto (cambio calculado, faltante, total automático, aviso de diferencia), Revisión/Detalle con aviso, en 360 dp, fs 1,3, fs 2,0 y tablet.

## Plan de tests

Nuevos (aditivos):
- JVM dominio (`:domain:expenses`), `TotalAutomaticoTest`: `totalDeLineas` (ignora concepto en blanco, redondeo); B3 los 5 casos de arranque; B4 (primera línea con total 0 -> automático; con total 20 -> tecleado; auto-cuadre; borrar última línea conserva cifra; AUTOMÁTICO sigue ediciones); B5 transiciones; `conTotalTexto` ignora en AUTOMÁTICO; `normalizada()` re-sincroniza.
- JVM dominio, ampliar `PagoTest` con casos nuevos sin tocar los existentes: cambio derivado y `null` con entregado < total, entregado vacío limpia cambio, `normalizarPago` ignora el cambio extraído que contradice (E=50, C=30, T=13,90 -> 36,10), solo-cambio infiere entregado, tarjeta conserva ocultos.
- ViewModel: `NuevoGastoViewModelTest` (añadir líneas -> total; tecleado + línea -> se mantiene; `onTotalAutomaticoElegido`), `TicketDetalleViewModelPagoTest`/`TicketDetalleViewModelTest` (cambio recalculado al editar total o líneas; `hayCambios` B8; guardar persiste cambio calculado), `EscanearViewModelTest` (total ausente + líneas = suma). Mismo patrón Turbine/`CoroutineTestRule` del fichero.
- E2E `:app` nuevo `TotalCambioAutomaticosFlujoTest` (comercio único con UUID, siembra con `TicketRepositoryEntryPoint` y borra en `@After`, nunca `pm clear`): Nuevo gasto añade 2 líneas y ve el total; efectivo/entregado calcula cambio no editable (`assert(keyNotDefined(SetText))` en `revision_cambio`); aviso de diferencia en Detalle de un ticket sembrado (total 10, líneas 3,9) y "Usar la suma"; guardar y leer de Room. Nuevo `AnadirGastoBotonFlujoTest`: a fs 1,0 y 2,0 (`LocalDensity`) `gastos_anadir` visible, `assertHeightIsAtLeast(48.dp)`, con texto "Añadir gasto", y abre `pantalla_nuevo_gasto` (no guarda nada).

## Tests que cambian (existentes; cada cambio mantiene la intención y se anota en el informe)

- `PagoTest.cambioEsEditableTrasElCalculo` y la línea `r.conCambioTexto("abc")/("  ")` de `textoInvalidoSeIgnoraYBlancoLimpia`: ya no existe cambio editable. Se reemplazan por casos de "cambio no editable / entregado vacío limpia cambio" (misma comprobación de texto inválido sobre entregado).
- `TicketDetalleViewModelPagoTest.textoInvalidoDeEntregadoOCambioSeIgnora`: se retira solo la llamada `vm.onCambioEditado("-5")`; `onEntregadoEditado("abc")` y `expectNoEvents()` quedan.
- `FormaPagoFlujoTest.entregadoCalculaElCambioYElAvisoNoBloqueaElGuardado` (194-204): se sustituye "reemplazar revision_cambio = 30 y ver aviso" por entregado 5 -> cambio vacío + `revision_aviso_pago` visible + guardar persiste entregado 5 y cambio `null`; los asertos 155/165/191 (`texto("revision_cambio")`) siguen válidos por `readOnly`.
- `RevisionDatosFlujoTest.anadirEditarYEliminarLineasYGuardarCambios...` (93-139): `revision_total_recalculado` -> `revision_total` con los mismos valores (0,00; 6,50; 4,50); el aserto final "el total no cambia, 0,00" pasa a "el total es 4,50 (automático)". Cambio de contrato explícito de la feature (en `mejoras-visuales-3` se declaró lo contrario).
- `NuevoGastoDockFlujoTest` (clase entera) y `FuentesGrandesFlujoTest.dockSinTotalRecalculado...`/`dockConLineasMuestraElTotalRecalculado` (136-165): el bloque del dock se retira; se reescriben para verificar dock de solo dos botones, `revision_total_recalculado` inexistente, texto "Guardar gasto" visible y, en Nuevo gasto, que al añadir una línea con total 0 el campo `revision_total` pasa a mostrar la suma.
- Sin cambio y deben seguir verdes (confirman compatibilidad): `DetalleTicketFlujoTest` (total 10 con líneas 3,9 queda TECLEADO y editable), `EscanearViewModelTest.editarLineasNoCambiaElTotal...` (35,30 vs 2,10 -> TECLEADO), `NuevoGastoViewModelTest` (total 12,5 + línea -> 12,5), `GuardarGastoManualTest`, `ListadoGastosVacioTest`, `FiltroFormaPago*Test`, `GastoManualFlujoTest`.

## Evidence to capture

Salidas de `testDebugUnitTest` y `am instrument` (nº de tests, 0 fallos, clases nuevas); tabla antes/después por configuración; restauración de `wm`/`font_scale`/rotación y `adb devices` sin tocar dispositivo físico; `git diff` de `testTag(` mostrando que solo se retira `revision_total_recalculado` y se añaden `revision_total_manual`, `revision_total_usar_suma`, `revision_aviso_total`; lista de tests cambiados con motivo. El informe del implementer debe decir EXPLÍCITAMENTE qué ve el usuario y dónde: (a) en Nuevo gasto/Revisión/Detalle, bajo Forma de pago > Efectivo, "Cambio (€)" calculado y no editable y el aviso de faltante; (b) al final del papel, un solo "Total" (automático con "Suma de las líneas" y "Editar total a mano", o editable con aviso de diferencia y "Usar la suma de las líneas"); (c) en la cabecera de Gastos, el botón "Añadir gasto" con icono (o en fila propia bajo la cabecera con fuente > 1,15); y qué NO se pudo verificar (TalkBack, ticket escaneado real con IA, tablet física).

## Validator checklist

- El cambio no se puede teclear en ningún flujo y coincide con `entregado - total`; nunca negativo; aviso con faltante visible; persistido recalculado aunque la IA diera otro.
- Un solo total en pantalla; no existen "Total (con IVA)" ni "Total recalculado"; el total sigue a las líneas en AUTOMÁTICO; un total tecleado/del papel que no cuadra se conserva con aviso y "Usar la suma".
- `GuardarGastoManual`/`ActualizarTicket` persisten el total mostrado; sin migración Room.
- Botón "Añadir gasto" legible a 360 dp, fs 1,3, fs 2,0 y tablet; presente en vacío y con filtro; tags conservados.
- Tests existentes no debilitados; cada cambio de la lista justificado; verificación solo en `emulator-5556`; sin lectura del token de Hugging Face.

## Unknowns / riesgos

- Riesgo: fila de línea con concepto vacío no suma (decisión B1/B6); si el usuario lo considera confuso, alternativa diferida: contarla con concepto "Artículo".
- Riesgo: sincronizar el texto de un campo editable cuando el total cambia por otra vía (modo AUTOMÁTICO <-> TECLEADO, "Usar la suma"); resolver con ramas de composables distintas o `LaunchedEffect(valor)`.
- Sin imágenes revisadas por el planner: la comprobación visual de las capturas es humana.

# Feature Implementation Spec: Estetica de recibo ("papel de ticket") en el ticket digitalizado

## Source Feature

- `id`: papel-ticket · `area`: revision · `status`: not_started (al planificar, 2026-10-19)
- `depends_on`: detalle-ticket (`accepted`, commiteada).
- `user_visible_behavior` (fuente): el resultado tras escanear, la revision editable y el detalle del ticket guardado se muestran como un recibo de papel (papel blanco, borde serrado, tipografia de recibo y linea punteada antes del total) sin perder legibilidad ni edicion.
- `verification` (fuente): (1) componente compartido en procesado, revision y detalle sin cambiar tags ni comportamiento; (2) capturas en emulator-5556 de las tres pantallas, tambien en tema oscuro y a 360 dp; (3) contraste y accesibilidad sin regresion; edicion y E2E existentes en verde.
- Peticion del usuario: "que el resumen y cuando escanees de sensacion de ticket real". Alcance elegido: SOLO las tres pantallas del ticket digitalizado. Panel Resumen, Gastos (lista) y Ajustes NO cambian (`DESIGN.md`, Product Feel: la estetica de recibo se reserva al ticket digitalizado).

## Goal

Un componente `PapelTicket` en `:feature:common` (hoja blanca con borde superior e inferior serrados dibujados con `Shape`/`Path`, sombra sutil, ancho maximo centrado, tipografia de recibo, separadores punteados y total destacado al final) que envuelve el contenido del ticket en: `ExtraccionListaContent` (solo lectura), `FormularioTicketEditable` (revision y detalle). Misma informacion, mismos datos, misma edicion; cambia el envoltorio visual.

## Non-Goals

- Modulos Gradle nuevos, dependencias nuevas, ficheros de fuente (`res/font`), cambios en `build-logic/`, dominio, Room, ViewModels, navegacion, `init.sh`.
- Cambios en Resumen, Gastos, Ajustes, selector de periodo, barra inferior, estados `ProcesandoExtraccionContent` y `ExtraccionFallidaContent` (la silueta de esqueleto del procesado en curso no cambia).
- Decoracion ficticia de los mockups 06/07/08 (NIF, direccion, IVA desglosado, "Operacion #", hash, "OCR Verificado %", codigo de barras con significado, "Tally Vision"). Solo se copia la forma (serrado, punteado, total grande).
- Tema oscuro nuevo (ver D3), animaciones nuevas, zoom de foto, imprimir/compartir.
- Cambiar el modelo de edicion (campos, validaciones, `rememberSaveable`, `key(version)`), el dock o los badges.

## Job Story

Cuando el motor acaba de leer mi ticket, o abro uno guardado,
quiero verlo como el recibo que fotografie,
para reconocerlo de un vistazo y comprobar que lo leido coincide con el papel.

## Acceptance Scenarios

### Scenario 1: resultado de solo lectura (procesado)
Given una extraccion completada (con y sin lineas, con campos nulos)
When se muestra `procesado_pantalla` con el resultado
Then el contenido (comercio y fecha arriba, categoria, lineas como filas `n× CONCEPTO ..... importe`, punteado, `Total (con IVA)` grande al final) va dentro de un `PapelTicket` (tag `papel_ticket`); campos sin dato muestran "—"; `procesado_completitud`, `procesado_revisar`, `procesado_reintentar_otro*`, el badge y los pasos siguen FUERA del papel con sus tags y textos.

### Scenario 2: revision editable
When se abre `revision_pantalla`
Then comercio, fecha, chips de categoria, lineas editables, boton anadir y total editable estan en UN solo `PapelTicket`, en este orden: cabecera (comercio, fecha, categoria) · punteado · "Desglose de articulos" con contador · lineas separadas por punteado · anadir · punteado · total. Todos los tags `revision_*` existen y se comportan igual (mismo teclado, validacion en error, borrar fila, contador, `revision_total_recalculado` en el dock). El dock y el badge de motor quedan fuera del papel.

### Scenario 3: detalle guardado
`pantalla_detalle_ticket` muestra igual que la revision (el formulario es el mismo); la foto (`detalle_ver_foto` o `detalle_foto_ausente`) y el badge quedan encima del papel, fuera de el; `detalle_*`, Descartar/Guardar con la regla `hayCambios` y eliminar con confirmacion no cambian.

### Scenario 4: usabilidad y accesibilidad
- Cada campo editable, chip, boton borrar y "Anadir" conservan zona tactil >= 48 dp; las etiquetas flotantes y `contentDescription` existentes se mantienen (TalkBack lee igual que hoy; serrado y punteado no tienen semantica).
- Texto sobre papel: `onSurface` (#171D1A) y `onSurfaceVariant` (#414943) sobre blanco (> 9:1); errores en `error`. Sin informacion solo por color (chips con check, como hoy).
- Con `fontScale` 1.3 y 2.0 y a 360 dp: nada recortado ni superpuesto, las filas envuelven a varias lineas, la pantalla sigue desplazable hasta el total y el dock; sin scroll horizontal.

### Scenario 5: tablet y tema oscuro
- Ancho > 480 dp: el papel mide como maximo 480 dp y se centra (dock y botones siguen a ancho completo; fuera de alcance).
- Tema oscuro del sistema: ver D3 (la app no tiene tema oscuro hoy); el papel se ve igual que en claro.

## Repository Research (inspeccionado)

- `feature/common/.../FormularioTicketEditable.kt`: `ContenedorRecibo` (card blanca 16 dp + barra `primary` de 6 dp + `SeparadorDiscontinuo` privado con `Canvas`) y `EditorLineas` (segunda card; cada `FilaLinea` es otra card `surfaceContainerLow` con 3 `OutlinedTextField`: concepto; cantidad 96 dp + precio + borrar 48 dp). `CampoEditable` privado con `rememberSaveable`. `DockEdicion` publico.
- `feature/escanear/.../ProcesadoContent.kt`: `ExtraccionListaContent` -> `ResumenDeTicket` privado (card con `FilaDeResumen` etiqueta/valor y `FilaDeLinea` "cantidad× precioUnitario"); `procesado_completitud` debajo. Tiene su propio `formatoImporte` internal.
- `RevisionDatosContent.kt` (escanear) y `TicketDetalleScreen.kt` (gastos): ensamblan `BadgeMotor` + columna scroll con `padding(horizontal = 16.dp)` + `FormularioTicketEditable` + `DockEdicion`; el detalle antepone `SeccionFoto`. No hace falta tocarlos salvo ajustes de padding.
- Tema (`feature/common/theme`): `lightColorScheme` unico (`TallyColorScheme`), `surfaceContainerLowest` = #FFFFFF, `surface` = #F5FBF6. No hay `isSystemInDarkTheme`, `darkColorScheme` ni `uiMode` en todo el repo: **la app es solo claro**. Tipografia Inter (`InterFontFamily`), sin otra familia usada.
- Tests que dependen de la UI: `RevisionDatosFlujoTest` (tags `revision_*`, `performScrollTo` sobre `revision_total`, textos de contador/total recalculado), `ProcesadoFlujoTest` (`procesado_badge_motor`, `procesado_completitud`, "Probar con"), `DetalleTicketFlujoTest` (`detalle_*`, `revision_badge_motor` texto exacto, `hasText(comercio)` en lista), `ListadoGastosFlujoTest` (no toca estas pantallas; se corre como regresion). Unit tests JVM existentes en `feature/common/src/test` (EtiquetaDia, FechasPicker, EtiquetaPeriodo).
- No inspeccionado: `.html` completo de 06/08 (solo se uso 07: `receipt-sawtooth-top/bottom`, `border-dashed` antes del total, importes `tabular-nums`) y `screen.png` de 07 (vista de referencia: cabecera con comercio grande, filas `1x` + concepto + importe a la derecha).

## Technical Approach

Decisiones (D) con su default; las marcadas [ABIERTA] las puede revertir el usuario sin rehacer la estructura.

- **D1 Componente** (`feature/common/PapelTicket.kt`, fichero nuevo, un solo `di.kt` no aplica): API publica
  - `PapelTicket(modifier, testTag = "papel_ticket", content: @Composable ColumnScope.() -> Unit)`: `Box(fillMaxWidth, contentAlignment = TopCenter)` con `Column(widthIn(max = 480.dp).fillMaxWidth())` -> `.shadow(2.dp, forma, clip = true)` -> `.background(surfaceContainerLowest, forma)` -> hairline `outlineVariant` 40 % con la misma forma -> padding interior (horizontal 16 dp; vertical >= alto del diente + 16 dp para que nada quede bajo el serrado).
  - `TicketSerradoShape(dienteAncho = 12.dp, dienteAlto = 6.dp) : Shape`: `createOutline` construye un `Path` cerrado con dientes triangulares arriba y abajo; nº de dientes = `max(1, round(ancho / dienteAncho))` y ancho de diente = `ancho / n` (el serrado termina rasante en ambos extremos a cualquier ancho). Sin imagenes ni `res/`.
  - `SeparadorPunteado(modifier)`: `Canvas` de 1-2 dp con `PathEffect.dashPathEffect` y `StrokeCap.Round` (puntos), color `outlineVariant`; sustituye al `SeparadorDiscontinuo` privado (retirarlo).
  - `FilaRecibo(cantidad, concepto, importe, modifier)` para el modo solo lectura: `n×` ancho fijo + concepto (varias lineas permitidas) + punto-guia + importe alineado a la derecha; si el concepto envuelve, la guia puede omitirse. Implementacion libre (`Layout` propio recomendado: `Row` con pesos no rellena el hueco restante). Importe de fila = `cantidad × precioUnitario` (misma formula que `totalRecalculado`; si `cantidad > 1` anadir debajo `n × precio` en `labelSmall`). Es un dato derivado, no inventado.
  - Estilo de recibo: `reciboTexto(base: TextStyle) = base.copy(fontFamily = FontFamily.Monospace, fontFeatureSettings = "tnum")` (helper publico o `object ReciboTipografia`).
- **D2 Tipografia [ABIERTA]**: `FontFamily.Monospace` del sistema (ya disponible en Compose; sin dependencia ni fichero). Se aplica solo dentro del papel a: nombre del comercio (titulo, negrita), fecha, texto de lineas, importes y total. Etiquetas flotantes, chips, botones, contador y todo fuera del papel siguen en Inter. Es una excepcion a "Inter en todos los roles" de `DESIGN.md`: debe quedar documentada ahi (ver Durable Docs). Alternativa descartada: empaquetar una fuente de recibo (peso y licencias para un cambio puramente estetico). Los campos editables usan monoespaciada en el texto escrito (`textStyle`) para coherencia; si en captura molesta, limitar la monoespaciada a lectura.
- **D3 Tema oscuro [ABIERTA, hallazgo]**: hoy NO existe tema oscuro, asi que "oscuro" es identico a claro. Default: el papel usa roles del tema actual (blanco / tinta oscura) y se declara que **siempre es claro** (decision de producto: papel blanco, `DESIGN.md`). La captura "oscuro" se hace con `adb -s emulator-5556 shell cmd uiautomator night yes` y sirve para demostrar que el papel no cambia ni pierde contraste; luego `night no`. Si en el futuro se anade `darkColorScheme`, el papel deberia fijar colores propios (papel/tinta) y los componentes M3 internos tendrian que recibir un esquema claro local: queda anotado como seguimiento, no se implementa aqui.
- **D4 Sombra [ABIERTA]**: `DESIGN.md` limita sombras a FAB y docks; el papel es una excepcion deliberada y minima (2 dp + hairline) porque blanco sobre `surface` (#F5FBF6) casi no se distingue y el borde serrado necesita separarse. Documentar la excepcion. Alternativa: solo hairline.
- **D5 Estructura del formulario**: `FormularioTicketEditable` pasa a ser UN `PapelTicket` en vez de dos cards: cabecera (icono `Storefront` opcional + `revision_comercio`, `revision_fecha`, `revision_categoria`) · `SeparadorPunteado` · seccion lineas (`revision_lineas`, cabecera "Desglose de articulos" + `revision_lineas_contador`, filas sin card anidada separadas por punteado, `revision_anadir_linea`) · `SeparadorPunteado` · `revision_total` destacado (`headlineSmall` negrita monoespaciada, etiqueta "Total (con IVA)"). Se elimina la barra `primary` de 6 dp. El total pasa de ir antes de las lineas a ir al final (como un recibo): los tests usan `performScrollTo`, no orden; el implementador debe confirmarlo con grep antes de dar por buena la reordenacion. `OutlinedTextField` se mantiene (default seguro de accesibilidad; un `TextField` solo-subrayado seria mas "ticket" pero es riesgo visual [ABIERTA]). Firma publica de `FormularioTicketEditable` sin cambios, asi `RevisionDatosContent` y `TicketDetalleScreen` casi no se tocan.
- **D6 Solo lectura**: `ResumenDeTicket` se reescribe sobre `PapelTicket`: comercio (grande, centrado, monoespaciada) y fecha; `Categoria: X`; punteado; lineas con `FilaRecibo` (o "—" si no hay); punteado; `Total (con IVA)` grande a la derecha (o "—"). `etiquetaCompletitud` queda debajo del papel con su tag.
- **D7 Fuera del papel**: foto original/"Foto no disponible", badges de motor, pasos de procesado, completitud, errores de guardado (`revision_error_guardado`, `detalle_error`), botones y dock. Paddings de la columna scroll: se permite pasar de 16 a 12-16 dp horizontal para ganar ancho a 360 dp, sin cambiar tags.
- **D8 Semantica**: `PapelTicket` no anade `contentDescription`; los elementos decorativos (serrado, punteado, guias) no son nodos de accesibilidad. Anadir el tag `papel_ticket` (nuevo, uno por pantalla).

## Expected File Changes

- Crear `feature/common/src/main/java/com/robertomr/tally/feature/common/PapelTicket.kt` (D1).
- Modificar `FormularioTicketEditable.kt` (D5; retirar `ContenedorRecibo`/`SeparadorDiscontinuo`, reubicar `EditorLineas`/`FilaLinea`; `DockEdicion`, `CampoEditable` y funciones de formato se conservan).
- Modificar `feature/escanear/.../ProcesadoContent.kt` solo `ResumenDeTicket`/`FilaDeResumen`/`FilaDeLinea` (D6) y, si procede, `ExtraccionListaContent` (padding).
- Opcional pequeno: `RevisionDatosContent.kt`, `TicketDetalleScreen.kt` (padding horizontal). No tocar ViewModels ni `:app`.
- Tests: `feature/common/src/test/.../PapelTicketTest.kt` (JVM, helpers puros: nº de dientes por ancho, importe de fila); anadir `papel_ticket` `assertIsDisplayed` en los flujos existentes (`ProcesadoFlujoTest`, `RevisionDatosFlujoTest`, `DetalleTicketFlujoTest`) sin cambiar las demas aserciones.
- Docs: ver siguiente seccion. No tocar `build.gradle.kts` ni `libs.versions.toml`.

## Visual Design Impact

Si hay UI. Referencias: mockups 07 (cuerpo del recibo, serrado, punteado, total grande) y 08 (filas editables), adaptados; se descarta la decoracion ficticia. Capturas obligatorias para el validador humano (todas en emulator-5556): procesado-resultado, revision y detalle, cada una en claro, "oscuro" (D3) y 360 dp; mas una con fontScale 1.3 en la revision. Prohibido: texto ficticio, imagenes de recibo, `res/font`.

## Durable Documentation Impact

- `DESIGN.md`: update (excepcion monoespaciada dentro del papel, excepcion de sombra del papel, "papel siempre claro", API del componente). Lo hace el implementador (el planner solo escribe en `docs/specs/`).
- `AGENTS.md`: not needed (`PapelTicket` ya cae en "Composables reutilizables -> `feature/common/`").
- `docs/adr/`: not needed (sin decision arquitectonica nueva; no contradice ADRs).
- `ARCHITECTURE.md` / `CONSTRAINTS.md`: not needed. `PROGRESS.md` y `feature_list.json`: los actualiza el orquestador con la evidencia.

## Implementation Plan

1. Leer los cuatro flujos E2E y `grep` de los tags/orden usados para confirmar que reordenar el total no rompe nada.
2. Crear `PapelTicket.kt` con shape, separador y `FilaRecibo`; probarlo con `@Preview` o captura rapida.
3. Reescribir `FormularioTicketEditable` (D5) conservando tags, `rememberSaveable`, validaciones, `key(linea.id)` y tamanos tactiles.
4. Reescribir `ResumenDeTicket` (D6).
5. Ajustar paddings de las pantallas; revisar 360 dp y fontScale 2.0.
6. Tests JVM + aserciones `papel_ticket`; documentar en `DESIGN.md`.
7. Verificar y capturar.

## Verification Plan

- `gradlew.bat assembleDebug testDebugUnitTest --console=plain` (o `bash init.sh`) en verde.
- En emulator-5556 (5554 esta offline), SOLO con `am instrument` (nunca `connected*`, nunca `pm clear`): `RevisionDatosFlujoTest`, `ProcesadoFlujoTest`, `DetalleTicketFlujoTest` y `ListadoGastosFlujoTest` en verde (misma invocacion que en `PROGRESS.md`; cada clase con `-e class`).
- Capturas con `adb -s emulator-5556 exec-out screencap -p`: tres pantallas x {claro, `cmd uiautomator night yes`, 360 dp (`wm size 1080x1920` + `wm density 480` o AVD equivalente; restaurar con `wm size reset`/`wm density reset`)}; revision con `settings put system font_scale 1.3` y 2.0 (restaurar a 1.0).
- Revision manual con TalkBack opcional: orden de lectura comercio -> fecha -> categoria -> lineas -> total.

## Evidence To Capture

Salida de build/unit tests; resultados `am instrument` de las cuatro clases; rutas de las capturas (claro, oscuro, 360 dp, fontScale); nota de que el tema oscuro no existe (D3); lista de tags `papel_ticket` verificados. La aprobacion visual final es humana.

## Validator Checklist

- [ ] `PapelTicket` en `:feature:common`, serrado con `Shape`/`Path` (sin imagenes), sin modulos, dependencias, fuentes ni cambios de versions.
- [ ] Las tres pantallas usan el mismo componente; Resumen, Gastos, Ajustes sin diferencias.
- [ ] Ningun tag `revision_*`, `procesado_*`, `detalle_*` desaparecio o cambio; edicion, validacion, Descartar/Guardar y borrado intactos.
- [ ] Foto, badge, dock, completitud y errores fuera del papel.
- [ ] Zonas tactiles >= 48 dp; contraste texto >= 4.5:1; fontScale 2.0 y 360 dp sin recorte ni scroll horizontal.
- [ ] Sin decoracion ficticia ni porcentajes; "—" para campos vacios.
- [ ] `DESIGN.md` documenta las excepciones (monoespaciada, sombra, papel claro).
- [ ] Capturas y E2E existentes en verde adjuntos; aprobacion visual humana pendiente/registrada.

## Open Questions / Defaults

1. Tema oscuro inexistente -> papel siempre claro (D3).
2. Monoespaciada del sistema solo dentro del papel (D2).
3. Sombra 2 dp en el papel pese a la regla de solo-tonal (D4).
4. Total movido al final del papel (D5); `OutlinedTextField` conservado.
5. Importe por fila = cantidad × precio unitario en solo lectura (D1); ya no se muestra solo "n× precio".

# Feature Implementation Spec: Forma de pago (efectivo/tarjeta) con entregado y cambio

## Source Feature

- `id`: forma-pago
- `area`: revision
- `depends_on`: detalle-ticket, papel-ticket (ambas accepted)
- `status`: not_started
- `source`: `feature_list.json` (sin commitear al planificar; junto a filtro-forma-pago, gasto-manual, galeria-escaneo)

## Goal

Cada ticket guarda cómo se pagó: EFECTIVO, TARJETA o NO_INDICADA. El **total (con IVA) sigue siendo lo que cuesta** y lo único que cuentan Resumen, donut y listado. En efectivo se guardan además **Entregado** (p. ej. 50,00) y **Cambio** (p. ej. 36,10) como información. Efectivo y tarjeta se distinguen visualmente en papel, resultado, detalle y listado. La IA intenta leer la forma de pago, el entregado y el cambio sin tocar la regla del total. Todo es editable en revisión y detalle. Incluye migración Room 2→3 sin pérdida de datos.

## Non-Goals

- Desglose de IVA por tipos (el IVA solo vive dentro del total).
- Filtrar por forma de pago (feature `filtro-forma-pago`), alta manual (`gasto-manual`), galería (`galeria-escaneo`).
- Cambiar totales de Resumen/donut/listado: siguen usando `ticket.total` tal cual. Nunca se suma `entregado` ni `cambio`.
- Otros medios de pago (bizum, vale, mixto): se mapean a NO_INDICADA.
- Bloquear el guardado por incoherencias entregado/cambio (solo aviso).
- Módulos Gradle nuevos, dependencias nuevas, `fallbackToDestructiveMigration`.

## Job Story

When termino de revisar un ticket y recuerdo que lo pagué en efectivo o con tarjeta,
I want to ver y corregir la forma de pago (y, en efectivo, cuánto entregué y cuánto me devolvieron),
so I can distinguir de un vistazo cómo pagué sin que eso altere cuánto gasté.

## Users And Permissions

- Usuario único local. Sin permisos nuevos, sin red nueva.

## Acceptance Scenarios

### Scenario 1: Efectivo con entregado y cambio (texto crudo 3)
Given un ticket con "Entregado: 50,00 € / Efectivo / Cambio: 36,10 € / Total 13,90"
When se extrae y se guarda sin editar
Then total = 13,90, forma = EFECTIVO, entregado = 50,00, cambio = 36,10; Resumen/donut/listado suman 13,90 (no 50,00 ni 36,10); el papel muestra filas "Entregado" y "Cambio" bajo el total y "Pagado en efectivo".

### Scenario 2: Tarjeta (texto crudo 2)
Given "TOTAL COMPRA 3,50 / TARJETA 3,50 / A DEVOLVER 0,00"
When se extrae
Then total = 3,50, forma = TARJETA, entregado y cambio = null (el "A DEVOLVER 0,00" no es total ni cambio guardado); el papel muestra "Pagado con tarjeta".

### Scenario 3: Sin indicios (texto crudo 1) y tickets antiguos
Given un texto sin palabras de pago, o un ticket guardado antes de la migración
When se muestra
Then forma = NO_INDICADA, sin chip ni filas de pago, importe idéntico al anterior.

### Scenario 4: Efectivo con total sin etiquetar (texto crudo 4)
Given "EFECTIVO / TOTAL A PAGAR 7,91 / 10,00 / CAMBIO 2,09"
When se extrae
Then total = 7,91 (regla del total intacta), forma = EFECTIVO, cambio = 2,09; entregado = 10,00 si el modelo lo infiere, o se deriva como total + cambio al normalizar (ver Enfoque).

### Scenario 5: Edición
Given revisión o detalle de un ticket
When el usuario elige Tarjeta con entregado/cambio rellenos
Then los campos Entregado/Cambio se ocultan; al guardar se persisten como null. Si vuelve a Efectivo antes de guardar, reaparecen con sus valores.

### Scenario 6: Coherencia
Given forma EFECTIVO, total 13,90
When el usuario escribe Entregado 50,00
Then Cambio se calcula 36,10 (editable); si lo cambia a un valor que no cuadre (|entregado − total − cambio| > 0,01) o entregado < total, aparece un aviso no bloqueante y el guardado sigue permitido.

### Scenario 7: Migración
Given una BD v2 con tickets y líneas
When se instala la nueva versión encima (install -r)
Then abre sin perder datos, todos los tickets con forma_pago = 'NO_INDICADA', entregado/cambio NULL.

## Repository Research

### Files Inspected (por el orquestador y re-confirmadas por glob)

- `domain/expenses/.../entities/Ticket.kt` (:14-26), `TicketExtraido.kt`, `DatosRevision.kt`, `EdicionDatos.kt` (:24 `datosRevisionInicial`/`datosRevisionDe`, reductores `conTotalTexto`/`conFechaTexto`), `TicketExtraidoParser.kt` (DTO con `ignoreUnknownKeys`; vive en domain/entities).
- `usecases/GuardarTicket.kt`, `ActualizarTicket.kt` — copian campo a campo.
- `framework/database/.../entity/TicketEntity.kt`, `Migrations.kt` (solo MIGRATION_1_2), `di.kt`, `schemas/1.json` y `2.json`, `androidTest/.../MigrationTest.kt`; mapper en `source/TicketLocalDataSource.kt:50-89`.
- `framework/ai/.../extraccion/TicketExtractionPrompt.kt` (:62-95 prompt, :123-146 `responseSchema()`) y `TicketExtractionPromptTest`.
- `feature/common`: `FormularioTicketEditable.kt` (:151-154 'Total (con IVA)', tag `revision_total`), `PapelTicket.kt`, `ListadoTicketsUi.kt` (`FilaTicket`); `feature/escanear/ProcesadoContent.kt` (`ResumenDeTicket` ~:927), `feature/gastos/TicketDetalleScreen.kt`.
- `app/src/androidTest/assets/texto_crudo_1..4.txt`, `ExtraccionNubeRealTest`, `ExtraccionLocalRealTest`.

### Existing Patterns To Follow

- Reductores puros en `EdicionDatos.kt` con test en `EdicionDatosTest.kt`; `hayCambios = datos != inicial` en los ViewModels.
- Migración como `log-extracciones`: `exportSchema`, JSON exportado, `MigrationTestHelper`, verificación real con `install -r`.
- Tags E2E aditivos; los E2E aseveran tags, no el literal 'Total (con IVA)'.

### Current Gaps

- Ninguna noción de forma de pago en dominio, BD, prompt ni UI. Al implementar, confirmar el tipo exacto de `total` en `DatosRevision` (Double vs texto) y reutilizar la misma representación para entregado/cambio.
- Cuota de Gemini agotada desde 2026-10-17: no hay verificación nube real garantizada.

## Technical Approach

### (a) Dominio (Kotlin puro)
- `enum class FormaPago { EFECTIVO, TARJETA, NO_INDICADA }` en `domain/expenses/entities/FormaPago.kt`, con `companion fun desde(valor: String?): FormaPago` tolerante (mayúsculas/espacios/tildes; desconocido o null → NO_INDICADA, nunca lanza).
- Campos nuevos **con valores por defecto al final** de `Ticket`, `TicketExtraido`, `DatosRevision`: `formaPago = NO_INDICADA`, `entregado: Double? = null`, `cambio: Double? = null` (o texto en DatosRevision si el total va como texto). Así los ~41 usos de `Ticket(...)` en tests compilan sin cambios.
- Invariante persistida: `entregado`/`cambio` solo no nulos si `formaPago == EFECTIVO`. Función pura `DatosRevision.normalizada()` / `TicketExtraido.normalizado()`:
  1. entregado o cambio presentes y forma NO_INDICADA → EFECTIVO (tener entregado/cambio implica efectivo);
  2. forma ≠ EFECTIVO → entregado = cambio = null;
  3. EFECTIVO con total y entregado y sin cambio → cambio = entregado − total si ≥ 0; con total y cambio y sin entregado → entregado = total + cambio (derivación, p. ej. texto 4);
  4. valores negativos o no finitos → null.
- Decisión coherencia: el cambio **se calcula** al editar entregado/total (editable después), **se avisa** (no se bloquea) si no cuadra. Función pura `avisoPago(datos): AvisoPago?` (`ENTREGADO_MENOR_QUE_TOTAL`, `CAMBIO_NO_CUADRA`) tolerancia 0,01.
- Reductores nuevos: `conFormaPago(forma)`, `conEntregadoTexto(texto)` (recalcula cambio si entregado y total válidos y el cambio no fue tocado a mano, o siempre que entregado cambie; simple: siempre recalcula al editar entregado/total), `conCambioTexto(texto)`. Cambiar de forma **conserva** entregado/cambio en el estado de edición (ocultos); `normalizada()` los limpia al guardar. `datosRevisionInicial` y `datosRevisionDe(ticket)` copian los campos nuevos.

### (b) Room 2→3
- `TallyDatabase version = 3`. `TicketEntity`: `@ColumnInfo(name="forma_pago", defaultValue="NO_INDICADA") val formaPago: String = "NO_INDICADA"`, `@ColumnInfo(name="entregado") Double?`, `@ColumnInfo(name="cambio") Double?`.
- `MIGRATION_2_3`: tres `ALTER TABLE tickets ADD COLUMN` (`forma_pago TEXT NOT NULL DEFAULT 'NO_INDICADA'`, `entregado REAL`, `cambio REAL`). El `defaultValue` de la entidad debe coincidir exactamente con el `createSql` de `3.json` (Room valida el esquema al abrir: mismatch = crash). Registrar en `di.kt` con `addMigrations(MIGRATION_1_2, MIGRATION_2_3)`; sin fallbackToDestructive.
- Mapper: `FormaPago.desde(entity.formaPago)`; al escribir, `forma.name`.
- `MigrationTest` 2→3: crea v2 con ticket(s) y líneas, migra, comprueba conservación, valor por defecto y NULL; y valida contra `3.json` con `runMigrationsAndValidate`.

### (c) Extracción
- Prompt (`TicketExtractionPrompt`): añadir al esquema en prosa `forma_pago` ("EFECTIVO" | "TARJETA" | "NO_INDICADA"), `entregado` (número o null), `cambio` (número o null). Bloque nuevo **REGLAS DE PAGO**, separado de la REGLA DEL TOTAL, que no se edita:
  - `total` sigue siendo lo que cuesta con IVA; ni `entregado` ni `cambio` son nunca el total;
  - `entregado` solo si el ticket lo dice (rótulos "entregado", "recibido", importe pagado en efectivo); si no, null;
  - `cambio` ("cambio", "a devolver", "vuelta", "devuelto") es lo devuelto, no el total; "A DEVOLVER 0,00" con tarjeta → null;
  - tarjeta/visa/mastercard/contactless/datáfono/crédito/débito → TARJETA; efectivo/metálico → EFECTIVO; sin indicios → NO_INDICADA;
  - un importe tipo "12,64 € Total (impuestos incl.)" suelto no cambia el total elegido.
- `responseSchema()` Firebase: `forma_pago` como string enum (valores exactos), `entregado`/`cambio` number nullable y **no required**. `buildLocalPrompt` hereda vía `buildPrompt`.
- DTO (`TicketExtraidoDto`): `forma_pago: String? = null`, `entregado: Double? = null`, `cambio: Double? = null` con defaults (respuestas antiguas/sin campos siguen válidas); el parser mapea con `FormaPago.desde` y aplica `normalizado()`.
- `TicketExtractionPromptTest`: conservar todos los literales previos (claves, '6 valores', 'total CON IVA', los tres rótulos, '3.50', '3.36', 'base imponible'); añadir `forma_pago`, `entregado`, `cambio`, `EFECTIVO`, `TARJETA`, `NO_INDICADA`, 'datáfono', 'A DEVOLVER'. Comprobar que "6 valores" no cambia de sentido (si cuenta campos, mantener el literal y aclarar que se refiere a la lista de candidatos de total).
- Tests JVM del parser (`TicketExtraidoParserTest`) con JSON de ejemplo de los 4 textos: 1 → NO_INDICADA; 2 → TARJETA sin entregado/cambio; 3 → EFECTIVO 50,00/36,10/total 13,90; 4 → EFECTIVO, total 7,91, cambio 2,09, entregado 10,00; más JSON sin los campos nuevos, forma desconocida ("BIZUM") → NO_INDICADA, entregado con forma TARJETA → limpiado.

### (d) UI (`:feature:common`, `:feature:escanear`, `:feature:gastos`)
- `FormularioTicketEditable`: selector segmentado Efectivo / Tarjeta / No indicada (chips o `SingleChoiceSegmentedButtonRow`, alto ≥ 48 dp, `Modifier.selectable(role = Role.RadioButton)` / `semantics selected`, icono + texto, no solo color). Tags: `revision_forma_pago_efectivo`, `_tarjeta`, `_no_indicada`. Campos `revision_entregado` y `revision_cambio` solo con EFECTIVO, teclado decimal, formateo es-ES igual que el total; aviso `revision_aviso_pago` (texto, no solo color). Etiqueta 'Total (con IVA)' y `revision_total` intactos.
- `PapelTicket`: bajo el total, estilo recibo, filas "Entregado" y "Cambio" (efectivo) y línea "Pagado en efectivo" / "Pagado con tarjeta"; nada para NO_INDICADA. Tags `papel_forma_pago`, `papel_entregado`, `papel_cambio`.
- `ResumenDeTicket` (resultado, solo lectura), `TicketDetalleScreen` y `FilaTicket`: chip pequeño con icono (billete/tarjeta) + texto "Efectivo"/"Tarjeta" junto al chip de categoría; NO_INDICADA sin chip. Tag `ticket_<id>_forma_pago`. El importe de la fila sigue siendo `total`. Contentdescription accesible.
- Colores/iconos de diferenciación: tokens de `DESIGN.md` (documentar); icono distinto además del color. Sin recursos XML de layout.
- ViewModels (`EscanearViewModel`, `TicketDetalleViewModel`): solo cablear lambdas `onFormaPago`, `onEntregado`, `onCambio` hacia los reductores; `hayCambios` no cambia.

### (e) Persistencia
`GuardarTicket` y `ActualizarTicket` aplican `datos.normalizada()` y copian `formaPago/entregado/cambio` (más el valor de la extracción si procede). Tests de ambos y fakes de `:test:unit` actualizados (el fake de repositorio guarda el Ticket completo; sin cambios si copia el objeto).

## Expected File Changes

- Crear: `entities/FormaPago.kt`; tests `FormaPagoTest`, `TicketExtraidoParserTest` ampliado, `3.json` (exportado por KSP), `MigrationTest` ampliado.
- Modificar: `Ticket.kt`, `TicketExtraido.kt`, `DatosRevision.kt`, `EdicionDatos.kt`, `EdicionDatosTest.kt`, `TicketExtraidoParser.kt`, `GuardarTicket.kt`, `ActualizarTicket.kt` (+ sus tests), `TicketEntity.kt`, `TallyDatabase.kt`, `Migrations.kt`, `framework/database/di.kt`, `TicketLocalDataSource.kt`, `TicketExtractionPrompt.kt` (+ test), `FormularioTicketEditable.kt`, `PapelTicket.kt`, `ListadoTicketsUi.kt`, `ProcesadoContent.kt`, `TicketDetalleScreen.kt`, ViewModels de escanear y gastos, strings es.
- E2E en `app/src/androidTest`: ampliar flujos de revisión/detalle con los tags nuevos (elegir tarjeta/efectivo, rellenar entregado, comprobar listado).
- Sin cambios en `gradle/libs.versions.toml` ni `build-logic/`.

## Visual Design Impact

- UI involved: yes. Design source: `DESIGN.md` (se actualiza con la diferenciación efectivo/tarjeta).
- Estados: papel, formulario (3 formas, con/sin aviso), resultado, detalle, fila de listado; tablet (columna 720 dp) y teléfono.
- New design artifact required: no (se resuelve con tokens Material 3 existentes); capturas como evidencia.

## Durable Documentation Impact

- `ARCHITECTURE.md`/`docs/architecture.md`: update — esquema v3, enum en dominio, normalización pura.
- `CONSTRAINTS.md`: not needed (no existe norma nueva; la invariante vive en domain-model).
- `AGENTS.md`: not needed.
- `docs/domain-model.md`: update — FormaPago, entregado, cambio, invariantes y normalización.
- `CONTEXT.md`: update — glosario: forma de pago, entregado, cambio (y aclarar que total ≠ entregado).
- `DESIGN.md`: update — diferenciación visual.
- `docs/risks-and-open-questions.md`: update — riesgo de confundir cambio/entregado con total en OCR, esquema Room, cuota Gemini.

## Implementation Plan

1. Dominio: enum, campos con defaults, normalización, avisos, reductores, tests.
2. Room 2→3: entidad, migración, di, mapper, 3.json, MigrationTest.
3. Casos de uso y parser/prompt/schema con tests.
4. UI y ViewModels; strings; tags.
5. E2E y docs; verificación en emulador.

## Implementation Tasks

- [ ] `FormaPago` + `desde()` tolerante + test.
- [ ] Campos con default en Ticket/TicketExtraido/DatosRevision; compilar todos los módulos sin tocar tests existentes.
- [ ] `normalizada()`, `avisoPago()`, reductores y `EdicionDatosTest`.
- [ ] Entidad, MIGRATION_2_3, di.kt, mapper, exportar 3.json y comprobar `defaultValue` idéntico al createSql.
- [ ] `MigrationTest` 2→3.
- [ ] Prompt, `responseSchema`, DTO, parser; actualizar `TicketExtractionPromptTest`; `TicketExtraidoParserTest` con 4 casos + bordes.
- [ ] `GuardarTicket`/`ActualizarTicket` + tests.
- [ ] Selector, campos, aviso en `FormularioTicketEditable`; papel; resultado; detalle; listado.
- [ ] E2E con tags nuevos; docs.
- [ ] Verificación en emulator-5556 y capturas.

## Verification Plan

- `bash init.sh` (assembleDebug + testDebugUnitTest) verde.
- Instrumentación solo en **emulator-5556** con `adb -s emulator-5556 shell am instrument ...`; nunca `connected*`, nunca `pm clear`; la tablet y sus datos reales no se tocan.
- Migración real: con la APK anterior (v2) instalada y datos en el emulador, `adb -s emulator-5556 install -r` de la nueva y comprobar que abre y conserva tickets, con forma "No indicada"; registrar salida.
- MigrationTest 2→3 por `am instrument -e class ...MigrationTest`.
- Textos crudos 1..4: `ExtraccionNubeRealTest` y `ExtraccionLocalRealTest` solo **registran** la forma de pago obtenida; no exigir totales ni forma si la cuota de Gemini está agotada; la garantía fuerte son los tests JVM del parser con JSON de ejemplo.
- E2E de revisión/detalle/listado en emulator-5556; los existentes siguen verdes (tags previos intactos).
- Capturas: papel, detalle y listado con efectivo, tarjeta y no indicada.

## Evidence To Capture

- Salida de `init.sh`, tests unitarios nuevos, `MigrationTest` OK, log de `install -r` y recuento de tickets antes/después, `3.json` en git, capturas, registro de forma_pago de las ejecuciones reales (o nota de cuota agotada).

## Validator Checklist

- [ ] Alcance respetado (sin IVA por tipos, sin filtro, sin gasto manual).
- [ ] Escenarios 1-7 comprobados.
- [ ] Totales de Resumen/donut/listado idénticos a antes; nunca suman entregado/cambio.
- [ ] Literales previos del prompt conservados; REGLA DEL TOTAL sin editar.
- [ ] Migración sin fallbackToDestructive; `defaultValue` coincide con 3.json; `install -r` real evidenciado.
- [ ] Selector accesible (≥ 48 dp, selected/Role, no solo color); tags previos conservados.
- [ ] Verificación solo en emulator-5556; no se usó `pm clear` ni `connected*`.
- [ ] `feature_list.json` y `PROGRESS.md` actualizados con evidencia; docs actualizadas.

## Open Decisions (con default)

1. Cambio: calculado y editable + aviso no bloqueante (default) vs. solo calculado.
2. Efectivo→tarjeta: conserva ocultos en edición y limpia al guardar (default).
3. Entregado/cambio sin forma indicada: se infiere EFECTIVO (default).
4. Entregado < total: aviso, no bloqueo (default).
5. Otros medios (bizum, vale, mixto): NO_INDICADA (default).

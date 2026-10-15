# Feature Implementation Spec: Tickets y líneas persisten en Room con borrado en cascada

## Source Feature

- `id`: persistencia-room
- `area`: persistencia
- `depends_on`: [bootstrap-gradle] (accepted 2026-10-06)
- `status`: not_started (planificada 2026-10-13)
- `source`: `feature_list.json`

## Goal

Montar la capa de persistencia local de Tally: esquema Room (`tickets` y
`lineas`) con borrado en cascada por FK, escritura transaccional del agregado
Ticket+Líneas, y consultas por rango de fechas de emisión expuestas como
`Flow`. La capa de dominio (`:domain:expenses`) define entidades, interfaces de
DataSource y el repositorio concreto; `:framework:database` implementa Room y el
almacenamiento del fichero de foto. Al terminar, las features posteriores
(`procesado-ui`, `listado-gastos`, `detalle-ticket`, ...) solo consumen el
repositorio: no vuelven a tocar esquema ni wiring de persistencia.

## Non-Goals

- UI de ningún tipo (listado, resumen, detalle): esta feature no cambia
  pantallas; la API `Flow` se consume después (`listado-gastos`,
  `resumen-total`).
- Casos de uso (`domain/expenses/usecases/`) y ViewModels: no hay consumidor
  todavía; llegan con `procesado-ui`/`listado-gastos`. El repositorio es la API
  pública de esta feature.
- Captura, compresión o guardado de fotos (`captura-foto`): aquí solo se borra
  el fichero de la foto al borrar el ticket.
- Borradores de ticket (estado Borrador del domain-model): solo se persiste el
  estado Guardado. La persistencia de borradores tras crash es riesgo conocido
  fuera de scope.
- Migraciones de esquema (v1 sin migración), seeds de datos, índices de
  rendimiento beyond el índice del FK.
- Motor de extracción, OCR, IA: solo el enum `Motor` como campo del ticket.
- Modificar `init.sh`, `build-logic/`, ADRs ni documentos de descubrimiento.

## Job Story

Cuando guardo un ticket ya revisado,
quiero que el ticket y sus líneas se persistan juntos y que al borrarlo no
quede ningún resto (ni líneas ni foto en disco),
para que la UI pueda consultar mis gastos por período sin sorpresas ni datos
huérfanos.

## Users And Permissions

- Autor/estudiante: único actor (sin auth en el producto). Acceso = desbloquear
  el dispositivo; los datos viven en almacenamiento privado de la app. Verificación
  por defecto en el emulador `emulator-5554` (criterio enmendado en
  `bootstrap-gradle`/`navegacion-base`; esta feature no tiene criterio de
  dispositivo en `feature_list.json`).

## Acceptance Scenarios

### Scenario 1: guardado transaccional del agregado

Given un ticket con 3 líneas y ruta de foto
When lo guardo con el repositorio
Then el ticket y las 3 líneas quedan juntos en Room: `getById` devuelve el
agregado completo y la consulta por rango lo incluye; las líneas se reemplazan
como conjunto al re-guardar el mismo ticket (edición futura reutiliza `save`).

### Scenario 2: borrado en cascada (líneas y foto)

Given un ticket persistido con 2 líneas y su foto en disco
When borro el ticket
Then desaparecen el ticket y sus 2 líneas (cascada por FK) y el fichero de la
foto se elimina del disco; un segundo borrado del mismo id es un no-op sin
crash.

### Scenario 3: consulta por rango de fechas por Flow

Given tickets con fechas de emisión distintas
When consulto el rango que cubre solo algunos
Then solo llegan esos tickets por `Flow`; al guardar un ticket nuevo dentro del
rango, el `Flow` emite una lista actualizada sin volver a consultar.

### Scenario 4: límites de módulos (revisión de código)

Given el código implementado
When inspecciono `:domain:expenses` y `:framework:database`
Then el dominio no importa nada de Android ni de Room; los DAO devuelven
`Flow`; las escrituras multi-tabla van con `@Transaction`; las implementaciones
son `internal` y viven en `framework`.

## Repository Research

### Files Inspected

- `AGENTS.md` — reglas: entidades de dominio en `domain/<área>/entities/`,
  interfaces de DataSource y repositorios concretos (`@Inject constructor`) en
  `domain/<área>/data/`, implementaciones de DataSource y entidades/DAO Room en
  `framework`, un `di.kt` por módulo, implementaciones de `framework` como
  `internal`, DAO devuelven `Flow`, escrituras multi-tabla con `@Transaction`.
- `docs/domain-model.md` — campos de Ticket (comercio, fecha/hora de emisión,
  total editable, categoría, nota opcional, fecha de captura, motor, ruta de la
  foto, texto crudo del OCR) y Línea (concepto, cantidad=1, precio final
  unitario con IVA); Ticket 1-N Líneas, Ticket 1-1 Foto con borrado en cascada;
  categoría enum fija de 6; motor Local|Nube; períodos por fecha de emisión.
- `docs/technical-discovery.md` — tablas `tickets` y `lineas` (borrado en
  cascada por FK), escrituras multi-tabla con `@Transaction`; fotos y texto
  crudo en `filesDir`, ruta guardada en Room (nada en MediaStore); testing:
  unit con dobles de `:test:unit` + Turbine, instrumentado con Room en memoria.
- `CONTEXT.md` — lenguaje del dominio: Ticket, Línea, Categoría, Motor, Período
  (nombres de identificadores de dominio).
- `framework/database/build.gradle.kts` — ya existe `:framework:database` con
  `tally.android.library` + `tally.hilt`, namespace
  `com.robertomr.tally.framework.database`; sin dependencias aún.
- `framework/database/src/main/java/.../di/di.kt` — `internal object
  DatabaseModule` vacío (patrón de `framework/ai`).
- `framework/ai/build.gradle.kts` y `framework/ai/src/androidTest/` — patrón de
  tests instrumentados: `testInstrumentationRunner` en `defaultConfig` +
  `androidTestImplementation` de `androidx.test`/junit.
- `domain/expenses/build.gradle.kts`, `domain/core/build.gradle.kts` — módulos
  Kotlin JVM puros (sin dependencias de Android, sin fuentes aún).
- `test/unit/build.gradle.kts` — `api(coroutines-test)`, `api(turbine)`,
  `testImplementation(junit)`; hoy solo `SmokeTest` en `src/test/kotlin`.
- `gradle/libs.versions.toml` — `room-runtime`/`room-ktx`/`room-compiler` (2.7.2)
  ya listos "para la feature persistencia-room"; plugin `ksp` disponible; falta
  `javax.inject` y `dagger-compiler`.
- `build-logic/.../HiltConventionPlugin.kt` — `tally.hilt` aplica ksp +
  hilt-compiler (Android); los módulos JVM puros no lo usan.
- `settings.gradle.kts`, `app/build.gradle.kts` — `:domain:expenses` y
  `:framework:database` ya están incluidos y `:app` ya depende de ambos; sin
  cambios necesarios.
- `docs/specs/navegacion-base.md` — referencias de estilo y de verificación.

### Existing Patterns To Follow

- Módulo framework = `tally.android.library` + `tally.hilt`; un `di.kt` con
  `internal object XxxModule` (@Module @InstallIn(SingletonComponent::class)).
- Test instrumentado sin UI en `framework/<módulo>/src/androidTest/`
  (patrón del spike de `:framework:ai`).
- Versiones solo en `gradle/libs.versions.toml`; identificadores técnicos en
  inglés, nombres de dominio según `CONTEXT.md` (patrón del código existente:
  `ResumenScreen`, `BottomBarDestino`).
- Tests unitarios de dominio con dobles de `:test:unit` y Turbine.

### Current Gaps

- `:framework:database` no tiene Room, entidades, DAOs ni dependencias.
- `:domain:expenses` no tiene fuentes: faltan entidades, interfaces y
  repositorio.
- `:test:unit` no tiene dobles compartidos (solo `SmokeTest`); necesita
  dependencia de `:domain:expenses` para implementar sus interfaces.
- Faltan en el catálogo `javax.inject` (para el `@Inject constructor` de
  AGENTS.md) y `dagger-compiler` (generación de fábricas en módulo JVM puro).
- Sin tests de DAO (Room in-memory) ni de repositorio.

## Technical Approach

- **Entidades de dominio** en `domain/expenses/entities/`: `Ticket` (agregado
  con `id: Long`, `comercio: String`, `fechaEmision: LocalDateTime`,
  `total: Double`, `categoria: Categoria`, `nota: String?`, `fechaCaptura:
  LocalDateTime`, `motor: Motor`, `rutaFoto: String`, `textoCrudoOcr: String?`,
  `lineas: List<Linea>`), `Linea` (`concepto: String`, `cantidad: Int`,
  `precioUnitario: Double`), enums `Categoria` (ALIMENTACION, HOGAR,
  TRANSPORTE, OCIO, SALUD, OTROS) y `Motor` (LOCAL, NUBE). Nombres de dominio
  en español según `CONTEXT.md`; métodos técnicos en inglés. `Double` para
  importes (decisión: MVP de curso, el modelo ya contempla que total y suma de
  líneas no coincidan).
- **Interfaces de DataSource** en `domain/expenses/data/`: `TicketDataSource`
  (`suspend fun save(ticket: Ticket): Long` upsert del ticket + reemplazo del
  conjunto de líneas en una transacción; `suspend fun delete(id: Long)`;
  `suspend fun getById(id: Long): Ticket?`; `fun observeByDateRange(from:
  LocalDate, to: LocalDate): Flow<List<Ticket>>` con rango inclusivo por
  `fechaEmision`) y `PhotoFileStore` (`fun delete(path: String): Boolean`,
  borrado best-effort del fichero de la foto).
- **Repositorio** `TicketRepository` en `domain/expenses/data/` (clase concreta,
  `@Inject constructor(TicketDataSource, PhotoFileStore)`): `save` delega;
  `delete(id)` = `getById` (para la ruta de la foto) → `dataSource.delete`
  (cascada de líneas por FK) → `photoFileStore.delete(rutaFoto)` best-effort (si
  el fichero no existe, no es error); id inexistente = no-op. Orden DB-antes-
  foto: si falla el borrado del fichero queda un huérfano en disco, preferible
  a borrar la foto y perder el ticket. `observeByDateRange` delega.
- **Room** en `:framework:database`: `entity/TicketEntity` (tabla `tickets`,
  PK auto) y `entity/LineaEntity` (tabla `lineas`, FK `ticket_id` → `tickets.id`
  `ON DELETE CASCADE`, índice en `ticket_id`); columnas snake_case
  (`comercio`, `fecha_emision`, `total`, `categoria`, `nota`, `fecha_captura`,
  `motor`, `ruta_foto`, `texto_crudo_ocr`; `concepto`, `cantidad`,
  `precio_unitario`); `dao/TicketDao` con `@Transaction suspend fun upsert(ticket,
  lineas)` (upsert ticket + delete/insert de líneas), `deleteById`, `getById`,
  `observeByDateRange` (`Flow`); `TallyDatabase` (@Database v1,
  `exportSchema = true`, `@TypeConverters` con `LocalDateTime` ↔ epoch millis);
  `source/TicketLocalDataSource` y `source/PhotoFileStoreImpl` (`internal`,
  implementan las interfaces del dominio; mapping entity↔dominio aquí).
  `ksp(libs.room.compiler)` + `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`.
- **DI** en `framework/database/di/di.kt` (mismo fichero `internal object
  DatabaseModule`): `@Provides @Singleton` de `TallyDatabase`
  (`Room.databaseBuilder`, nombre `tally.db`), `@Provides` de `TicketDao`,
  `TicketDataSource` y `PhotoFileStore` (con `@ApplicationContext`); sin `@Binds`
  (módulo object, estilo `@Provides`, como el patrón actual de `di.kt`).
- **Procesado de anotaciones en dominio**: `:domain:expenses` gana
  `implementation(libs.javax.inject)` + plugin `ksp` + `ksp(libs.dagger.compiler)`
  para generar `TicketRepository_Factory` (Hilt/Dagger solo ve fábricas
  generadas en el módulo que compila la clase; sin ello el `@Inject constructor`
  de AGENTS.md sería inerte). Riesgo/contingencia: si ksp+dagger falla en módulo
  JVM puro, alternativa = `@Provides` del repositorio en `DatabaseModule` y
  registrar la desviación.
- **Dobles** en `test/unit/src/main/kotlin/`: `FakeTicketDataSource`
  (`MutableStateFlow` interno para que `observeByDateRange` emita al guardar;
  ids autoincrementales) y `FakePhotoFileStore` (registra rutas borradas,
  resultado configurable). `:test:unit` añade `api(project(":domain:expenses"))`.
- **Tests**: unit de `TicketRepositoryTest` en
  `domain/expenses/src/test/kotlin/` con los dobles de `:test:unit` + Turbine;
  instrumentados `TicketDaoTest` (Room in-memory) y `PhotoFileStoreTest`
  (fichero real en `filesDir`) en `framework/database/src/androidTest/`
  (patrón `framework/ai`).
- Sin cambios en `:app`, `settings.gradle.kts`, features, tema ni navegación.

## Expected File Changes

- `gradle/libs.versions.toml` — modify; `javax.inject` (1) y `dagger-compiler`
  (`version.ref = "hilt"`, misma línea 2.60.1) solo aquí.
- `domain/expenses/build.gradle.kts` — modify; `javax.inject`, plugin ksp +
  `ksp(libs.dagger.compiler)`, `coroutines-core`; `testImplementation`
  `:test:unit` + junit.
- `domain/expenses/src/main/java/com/robertomr/tally/domain/expenses/entities/{Ticket,Linea,Categoria,Motor}.kt`
  — create.
- `domain/expenses/src/main/java/com/robertomr/tally/domain/expenses/data/{TicketDataSource,PhotoFileStore,TicketRepository}.kt`
  — create.
- `domain/expenses/src/test/java/com/robertomr/tally/domain/expenses/data/TicketRepositoryTest.kt`
  — create.
- `framework/database/build.gradle.kts` — modify; room-runtime/ktx,
  `ksp(libs.room.compiler)`, `coroutines-core`, `testInstrumentationRunner` y
  `androidTestImplementation` (androidx.test + junit), `room.schemaLocation`.
- `framework/database/src/main/java/com/robertomr/tally/framework/database/entity/{TicketEntity,LineaEntity}.kt`,
  `dao/TicketDao.kt`, `TallyDatabase.kt`, `Converters.kt`,
  `source/{TicketLocalDataSource,PhotoFileStoreImpl}.kt` — create.
- `framework/database/src/main/java/com/robertomr/tally/framework/database/di/di.kt`
  — modify; bindings de Room y DataSource.
- `framework/database/src/androidTest/java/com/robertomr/tally/framework/database/{TicketDaoTest,PhotoFileStoreTest}.kt`
  — create.
- `test/unit/build.gradle.kts` — modify; `api(project(":domain:expenses"))`.
- `test/unit/src/main/kotlin/com/robertomr/tally/test/unit/{FakeTicketDataSource,FakePhotoFileStore}.kt`
  — create.
- `framework/database/schemas/` — create (esquema exportado v1).
- `PROGRESS.md`, `feature_list.json` — update al completar (evidencia).

## Visual Design Impact

- UI involved: no. La feature es solo de datos; ningún cambio visible.
- Design source: not applicable (el consumo visual llega con
  `listado-gastos`/`resumen-total`).
- Screens or states affected: none.
- New design artifact required: no.

## Durable Documentation Impact

- `docs/architecture.md`: update menor — el diagrama ya lista
  `:framework:database`; añadir una línea sobre Room (`tickets`/`lineas`) y la
  regla "Room es única fuente de verdad" si no está.
- `ARCHITECTURE.md` (raíz): not needed — la arquitectura operativa vive en
  `AGENTS.md`/`docs/architecture.md` (mismo criterio que features anteriores).
- `CONSTRAINTS.md`: not needed — no se introducen MUST/MUST NOT nuevos (la
  regla de Room ya está en `AGENTS.md`).
- `AGENTS.md`: not needed — no cambia el workflow.
- Other docs: `PROGRESS.md` y `feature_list.json` — update obligatorio con
  evidencia al completar. `docs/domain-model.md` sin cambios (es la fuente).

## Implementation Plan

1. Catálogo: `javax.inject` y `dagger-compiler`.
2. Entidades de dominio (`Ticket`, `Linea`, `Categoria`, `Motor`) en
   `:domain:expenses`.
3. Interfaces `TicketDataSource`/`PhotoFileStore` + `TicketRepository`
   (`@Inject constructor`) en `domain/expenses/data/`; wiring de ksp en el
   módulo.
4. Room en `:framework:database`: entidades, `TicketDao`, `TallyDatabase`,
   converters, schema export.
5. Implementaciones `internal` (`TicketLocalDataSource`, `PhotoFileStoreImpl`) y
   bindings en `di.kt`.
6. Dobles en `:test:unit` (con `api(:domain:expenses)`).
7. `TicketRepositoryTest` (unit, con dobles) y `TicketDaoTest` +
   `PhotoFileStoreTest` (instrumentados, Room in-memory / fichero real).
8. Verificación completa (comandos + revisión de código).
9. `docs/architecture.md` (update menor) + `PROGRESS.md` + `feature_list.json`
   con evidencia.

## Implementation Tasks

- [x] T1 Catálogo: `javax.inject` + `dagger-compiler` sin versiones literales fuera del toml.
- [x] T2 Entidades de dominio `Ticket`/`Linea`/`Categoria`/`Motor` en `domain/expenses/entities/`.
- [x] T3 `TicketDataSource` + `PhotoFileStore` + `TicketRepository` en `domain/expenses/data/`; `:domain:expenses` compila con javax.inject + ksp.
- [x] T4 Room: `TicketEntity`/`LineaEntity` (FK `ON DELETE CASCADE` + índice), `TicketDao` (`@Transaction`, `Flow`), `TallyDatabase`, `Converters`, schemas/.
- [x] T5 `TicketLocalDataSource` + `PhotoFileStoreImpl` (`internal`) + bindings en `di.kt`.
- [x] T6 Dobles `FakeTicketDataSource`/`FakePhotoFileStore` en `test/unit/src/main/kotlin/`; `:test:unit` con `api(:domain:expenses)`.
- [x] T7 `TicketRepositoryTest` (save/delete/cascada foto/no-op/rango con Turbine).
- [x] T8 `TicketDaoTest` (guardado transaccional, reemplazo de líneas, borrado en cascada, rango por `Flow`) + `PhotoFileStoreTest` en `framework/database/src/androidTest/`.
- [x] T9 Verificación en verde + revisión de código (escenario 4) + `docs/architecture.md` + `PROGRESS.md` + `feature_list.json`.

## Verification Plan

- `.\gradlew.bat assembleDebug` → BUILD SUCCESSFUL.
- `.\gradlew.bat testDebugUnitTest` → BUILD SUCCESSFUL (regresión).
- `.\gradlew.bat :test:unit:test` → BUILD SUCCESSFUL (regresión SmokeTest).
- `.\gradlew.bat :domain:expenses:test` → BUILD SUCCESSFUL; `TicketRepositoryTest`
  cubre escenario 2 (borrado de foto) y la delegación de save/rango.
- `$env:ANDROID_SERIAL="emulator-5554"; .\gradlew.bat :framework:database:connectedDebugAndroidTest`
  → `TicketDaoTest` y `PhotoFileStoreTest` en verde (escenarios 1–3). E2E de UI
  persistente: **no aplica** — la feature no tiene comportamiento visible ni
  flujos de usuario; la superficie persistente queda cubierta por los tests
  instrumentados de DAO y la UI que consuma `Flow` (`listado-gastos`,
  `resumen-total`) añadirá su cobertura E2E.
- Revisión de código: 0 imports de Android/Room en `domain/**`; DAOs con
  `Flow`; `@Transaction` en la escritura multi-tabla; implementaciones
  `internal`; `Select-String` de versiones literales (`x.y.z`, `version="0-9`,
  GAV literal) en `**/*.gradle.kts` → 0 coincidencias fuera del toml.
- `init.sh`: sin cambios (`git diff init.sh` vacío); su contenido
  (`assembleDebug` + `testDebugUnitTest`) ya se ejecuta arriba.

## Evidence To Capture

- Salidas BUILD SUCCESSFUL de los comandos Gradle anteriores y del
  `connectedDebugAndroidTest` (nombres de test y dispositivo `emulator-5554`).
- JUnit XML en `domain/expenses/build/test-results/` y
  `framework/database/build/outputs/androidTest-results/connected/debug/`.
- Resultado de la revisión de código (dominio sin Android, `@Transaction`,
  `Flow` en DAO, versiones).
- Entradas actualizadas: `feature_list.json` (`evidence` de
  `persistencia-room`) y `PROGRESS.md` (sesión correspondiente).

## Validator Checklist

- [x] Implementation stays within this feature's scope.
- [x] Acceptance scenarios pass.
- [x] Verification evidence is present.
- [x] Persistent E2E coverage added o justificado (justificado: sin UI ni flujos
      de usuario en esta feature; superficie cubierta por `TicketDaoTest` y
      `PhotoFileStoreTest` instrumentados).
- [x] `feature_list.json` y `PROGRESS.md` fueron actualizados correctamente.
- [x] No unrelated product behavior or extra feature work was added.
- [x] Ninguna versión literal fuera de `gradle/libs.versions.toml`.
- [x] `:domain:expenses` sin dependencias de Android ni de Room; las
      implementaciones técnicas viven en `:framework:database` y son `internal`.
- [x] `init.sh`, `AGENTS.md`, `build-logic/`, ADRs y docs de descubrimiento sin
      cambios indebidos.

## Open Questions

Ninguna bloqueante (todas resueltas por precedente o decisión):

1. **Dispositivo de verificación instrumentada**: `emulator-5554`
   (precedente de las sesiones 007–009 de `PROGRESS.md`; `feature_list.json` no
   exige dispositivo físico para esta feature).
2. **Dónde viven los tests del repositorio**: en
   `domain/expenses/src/test/` con `testImplementation(project(":test:unit"))`
   (sin ciclo: la dependencia `:test:unit` → `:domain:expenses` es de
   compilación y la inversa solo de test). Contingencia si Gradle lo rechaza:
   mover `TicketRepositoryTest` a `test/unit/src/test/` y dejarlo registrado.
3. **Procesado de anotaciones en el dominio**: `javax.inject` + ksp +
   `dagger-compiler` en `:domain:expenses` (decisión). Contingencia: `@Provides`
   del repositorio en `DatabaseModule` y registro de la desviación.

# Arquitectura

Documento de referencia de la estructura de módulos y la dirección de
dependencias. Las reglas operativas completas están en `AGENTS.md`.

## Diagrama de módulos

```
:app                      (application, ensambla todo, define navegación)
 ├─ :domain:core          (Kotlin puro, sin Android)
 ├─ :domain:expenses      (Kotlin puro, sin Android)
 ├─ :framework:database   (android library internal; implementa interfaces de domain)
 ├─ :framework:ai         (android library internal; implementa interfaces de domain; OCR, motores y modelo/ = descarga con WorkManager)
 ├─ :feature:common       (compose; tema, tipografía y UI compartida: PeriodoSelector, FilaTicket/CabeceraDia, formato de importe y etiquetas de día/período)
 ├─ :feature:resumen      (compose; panel: selector Día/Semana/Mes, total y donut; usa :domain:expenses y :feature:common)
 ├─ :feature:ajustes      (compose; Ajustes: sección "Modelo local", descarga y token)
 ├─ :feature:gastos       (compose; listado de tickets del período agrupado por día; usa :domain:expenses y :feature:common)
 └─ :feature:escanear     (compose; pantallas)

:test:unit                (librería Kotlin; dobles de prueba compartidos)

build-logic/              (included build; convention plugins de Gradle)
```

## Regla de dependencias

```
app ──────────► feature:* ──────────► domain:*
  │                 │                     ▲
  │                 └──────► common ◄─────┤
  └──────────────► framework:* ───────────┘
```

- `domain` no importa nada de Android, de `framework` ni de `feature`.
- `framework` implementa interfaces declaradas en `domain`. Nunca al revés.
- Los módulos `feature` no se conocen entre sí. Solo `:app` sabe cómo se conectan.
- `:test:unit` contiene dobles compartidos; no forma parte del APK.

## Capas

| Capa | Módulos | Contenido |
| --- | --- | --- |
| Presentación | `:app`, `:feature:*` | Compose, ViewModels, navegación |
| Dominio | `:domain:*` | Entidades, casos de uso, interfaces de DataSource |
| Datos / técnicas | `:framework:*` | Room, MediaPipe, Firebase AI Logic |
| Pruebas | `:test:unit` | Dobles compartidos |

## Persistencia

- Room (en `:framework:database`) es la única fuente de verdad: base `tally.db`
  con las tablas `tickets` y `lineas` (borrado en cascada de líneas por FK).
- Los DAO devuelven `Flow` y las escrituras multi-tabla van con `@Transaction`.
  La foto del ticket vive en `filesDir` y su ruta se guarda en Room.
- **Versión 2 (`log-extracciones`)**: añade la tabla `extracciones_log` (solo
  metadatos: marca de tiempo, motor, origen, resultado, tipo/clase de fallo,
  duración y longitud del texto; nunca texto, datos del ticket ni mensajes de
  excepción). Primera migración del proyecto: `MIGRATION_1_2` registrada con
  `addMigrations` (sin `fallbackToDestructiveMigration`), esquemas exportados en
  `framework/database/schemas/` (`1.json`, `2.json`) y test con
  `MigrationTestHelper`. Retención de 500 filas (insertar + recortar en una
  `@Transaction`).

- **Versión 3 (`forma-pago`)**: `tickets` gana `forma_pago TEXT NOT NULL DEFAULT 'NO_INDICADA'`,
  `entregado REAL` y `cambio REAL` (nulables). `MIGRATION_2_3` son tres `ALTER TABLE ... ADD COLUMN`
  y su `DEFAULT` replica exactamente el `defaultValue` de `TicketEntity` y el `createSql` de
  `schemas/.../3.json` (Room valida el esquema al abrir: un desajuste tumbaría la app). Registrada con
  `addMigrations(MIGRATION_1_2, MIGRATION_2_3)`, sin `fallbackToDestructiveMigration`; el mapper
  usa `FormaPago.desde(...)` y no falla ante valores desconocidos. `MigrationTest`/`MigracionFormaPagoTest`
  y verificación con `install -r` sobre la BD real.
- **Forma de pago en dominio**: `FormaPago` (enum) y `Pago.kt` (`normalizarPago`, `DatosRevision.normalizada()`,
  `avisoPago`) son Kotlin puro en `:domain:expenses`; `Ticket`, `TicketExtraido` y `DatosRevision` llevan
  `formaPago/entregado/cambio` con valores por defecto al final. `GuardarTicket` y `ActualizarTicket` normalizan
  al guardar. Los totales de Resumen, donut y listado siguen usando solo `ticket.total`.
- **Extracción**: el prompt añade un bloque REGLAS DE PAGO separado de la REGLA DEL TOTAL;
  `responseSchema()` declara `forma_pago` (enum) y `entregado`/`cambio` opcionales; el DTO del parser
  los lee con valores por defecto y los importes ilegibles se descartan sin invalidar la extracción.

### Registro de extracciones (`log-extracciones`)

- `EngineConRegistro` (dominio, Kotlin puro) decora al `EngineRouter` e
  implementa `TextEngine` y `ExtractorPorMotor`: es el punto único de registro.
  `extract(texto)` = origen ROUTER, `extract(motor, texto)` = REINTENTO. En
  `:framework:ai/di` las dos interfaces se resuelven al mismo singleton
  decorado; casos de uso y ViewModels no cambian.
- Duración con `Reloj` inyectable (`RelojSistema` en `:framework:database`).
  Cancelación: se registra CANCELADA en `NonCancellable` y la
  `CancellationException` se relanza siempre; un fallo al escribir el log nunca
  altera la extracción. Texto blank: no se registra (los casos de uso no llegan
  al motor).
- Consulta solo en builds debuggables (`FLAG_DEBUGGABLE`, sin `BuildConfig`):
  sección "Diagnóstico de extracciones" en Ajustes con resumen por motor,
  últimas 20 filas, "Copiar CSV" y "Borrar registro".

## Captura de foto

- El pipeline de la foto capturada vive en `:framework:database`
  (`CapturedPhotoProcessor`, interno; matemática pura en `PhotoPipelineMath`):
  hornea la orientación EXIF en los píxeles, **recorta la región del encuadre
  guiado** (`captura-recorte-encuadre`: la foto guardada es el recuadro del mockup
  04, no el fotograma completo), escala a ≤1280 px de lado largo conservando
  proporción y recomprime JPEG (~85) sin tag de orientación. El contrato
  (`PhotoProcessor.process(rawJpeg, crop: CropRegion)`) está en `:domain:expenses`;
  el original de cámara no se guarda. La MISMA geometría (`EncuadreGeometria` en
  `:feature:escanear`, pura) pinta la retícula y calcula el recorte; el mapeo
  vista→imagen modela el `FILL_CENTER` de la preview (`PhotoPipelineMath.recorteEnImagen`).
- La foto se persiste EN EL MOMENTO DE LA CAPTURA en `filesDir/fotos/` con
  nombre RELATIVO (`fotos/captura_<uuid>.jpg`), contrato de
  `PhotoFileStore.save`: nunca se devuelve ni se escribe una ruta absoluta.
  La foto sobrevive a la muerte de la app; "Repetir foto" borra la foto del
  borrador reemplazado (best-effort).
- CameraX vive en `:feature:escanear` (binding de UI: preview + `ImageCapture`
  en `CameraEncuadre`). No existe `:framework:camera`: crearlo exige
  aprobación explícita del usuario (AGENTS.md).

## OCR (texto crudo)

- Etapa 1 del ADR 0002 (`ocr-mlkit`): de la foto ya persistida se obtiene el
  **texto crudo** con ML Kit Text Recognition v2 (latin, variante **bundled**:
  modelo en el APK, on-device, sin Play Services, red ni clave de API).
- El contrato `OcrEngine` (`suspend fun recognize(image: ByteArray): String`)
  vive en `:domain:expenses/data/` y no conoce Android ni ML Kit. La
  implementación `MlKitOcrEngine` es `internal` en `:framework:ai` (decode +
  OCR en `Dispatchers.IO`, Task envuelta en `suspendCancellableCoroutine`) y se
  binding con `@Provides OcrEngine` en el `di.kt` del módulo.
- `PhotoFileStore.read(path)` (en `:framework:database`) lee los bytes de la
  foto guardada; el caso de uso `ReconocerTextoCrudo` (dominio) encadena
  `read` → `recognize`.
- El texto crudo se registra con el Borrador en memoria (`(rutaFoto, textoCrudo)`
  en `EscanearViewModel`); viaja en `Ticket.textoCrudoOcr` / columna
  `tickets.texto_crudo_ocr` cuando `revision-datos` persista, sin ficheros
  sidecar. OCR vacío o fallo del motor **no bloquean** la captura (la foto ya
  está en disco). "Continuar" ya está habilitado (`procesado-ui`); el texto es
  visible en un panel **solo debug** de la pantalla 05 (`captura_texto_crudo`).

## Extracción estructurada (TextEngine)

- Etapa 2 del ADR 0002 (`extraccion-nube`): del texto crudo se obtiene un
  `TicketExtraido` **validado** (comercio, fecha de emisión, **total con IVA**,
  líneas y categoría de la enum fija de 6) con Gemini vía **Firebase AI Logic**.
- El contrato `TextEngine` (`val motor: Motor` +
  `suspend fun extract(textoCrudo: String): TicketExtraido`) vive en
  `:domain:expenses/data/` sin imports de Android ni Firebase. El motor usado
  queda **registrado dentro del resultado** (`TicketExtraido.motor`), lo que deja
  listo el badge/router de `router-automatico`.
- El **parser/validador compartido** `TicketExtraidoParser` (dominio, Kotlin
  puro: kotlinx-serialization) es el mismo que reutilizará `extraccion-local`:
  tolerante en el contenedor (fences ```json, prosa, coma decimal española) y
  estricto en el schema; lo malformado lanza `InvalidExtractionException` con
  motivo claro (sin resultados inventados). La regla **total con IVA** (deuda
  R3) está en el prompt y en el validador.
- `GeminiCloudEngine` es `internal` en `:framework:ai/extraccion/`
  (`Dispatchers.IO`, modelo lazy, `responseMimeType` + `responseSchema`, backend
   `googleAI`, `gemini-flash-lite-latest` — variante **lite** elegida por el
   usuario 2026-10-17 tras deprecar el servidor `gemini-2.5-flash` y agotarse la
   cuota de `gemini-3.8-flash`; ver R2); se binding con `@Provides @Named("nube") TextEngine` en el
  `di.kt` del módulo. Errores de red/cuota/config se propagan al llamador y una
  `CancellationException` jamás se convierte en resultado.
- `GemmaLocalEngine` (`extraccion-local`, `internal` en `:framework:ai/extraccion/`)
  es el segundo `TextEngine`: Gemma 3n E2B vía MediaPipe `tasks-genai`, sin red,
  `motor = Motor.LOCAL`. MediaPipe queda tras la interfaz interna `LocalLlm`
  (`MediaPipeLocalLlm` real; un runner falso permite probar el motor en JVM). Usa
  el mismo prompt/schema que la nube (`buildLocalPrompt` = `buildPrompt` + cierre
  "solo JSON") y el mismo `TicketExtraidoParser`. La generación se corta sola con
  `JsonObjectStreamDetector` (cierre de la `}` de nivel superior o `<end_of_turn>`),
  el modelo se carga por extracción en `Dispatchers.IO` y se cierra en `finally`
  (tras cancelar, se espera a que termine la generación nativa); parámetros
  `maxTokens=2048` (entrada+salida), `topK=1`, `temperature=0.2`, semilla fija. Modelo
  ausente → `ModeloLocalNoDisponibleException`. Se binding como
  `@Provides @Singleton @Named("local") TextEngine` en el mismo `di.kt`.
- **Router (`router-automatico`)**: `EngineRouter` (dominio, Kotlin puro,
  `data/EngineRouter.kt`) es el `TextEngine` **sin calificar** del grafo Hilt y
  delega por extracción en `@Named("local")` o `@Named("nube")`. La política es la
  función pura `elegirMotor(preferencia, capaz, modeloDisponible)`: `NUBE` → Nube;
  `LOCAL` → Local (la política es pura; `EngineRouter.extract` rechaza LOCAL
  forzado si Local no está disponible, ver `ajustes-motor` abajo); `AUTO` (D1, **local primero**) → Local solo
  si `capaz && modeloDisponible`, si no Nube. `capaz` = `CapacidadDispositivo`
  (`RamCapacidadDispositivo`, `internal`, RAM total >= 10 GB, heurística) y
  `modeloDisponible` = `ModelDownloader.rutaModelo() != null`. La preferencia sale
  de `PreferenciaMotorSource`, implementada por `PreferenciaMotorStore` (`ajustes-motor`). **Sin fallback automático** (ADR 0001): el error del motor elegido
  se propaga. `EngineRouter.motor` re-evalúa la política en cada lectura (badge
  previo a extraer); el badge final y `Ticket.motor` usan `TicketExtraido.motor`.
- **Preferencia de motor (`ajustes-motor`)**: Ajustes tiene la sección "Motor de
  extracción" (Auto/Local/Nube, tags `motor_opcion_*`). `PreferenciaMotorStore`
  (dominio, extiende `PreferenciaMotorSource` con `preferencia: Flow` y `guardar`)
  se implementa en `framework/ai/preferencias/SharedPreferencesPreferenciaMotor`
  (`internal`; fichero `tally_ajustes`, clave `preferencia_motor`; valor ilegible o
  desconocido = AUTO; `MutableStateFlow` interno + `apply()`); sin dependencia
  nueva. El router relee `actual()` en cada extracción: el cambio aplica a la
  siguiente. Casos de uso `ObservarPreferenciaMotor`/`ConfigurarPreferenciaMotor`.
  Con LOCAL forzado y Local no disponible (`NO_CAPAZ` tiene prioridad sobre
  `SIN_MODELO`), `extract` lanza `MotorNoDisponibleException` sin invocar ningún
  motor ni pasar a Nube; NUBE forzada sin red = fallo propio de `GeminiCloudEngine`.
  El reintento explícito (`extract(motor, texto)`) sigue ignorando la preferencia.
  Ajustes avisa (`motor_aviso_local`) si se elige Local sin modelo descargado.
- El caso de uso `ExtraerTicket` (dominio) envuelve el motor: texto blank →
  `TicketExtraido` vacío **sin invocar el motor** (sin gastar cuota); es el
  punto de extensión futuro de `log-extracciones`.
- **Firebase sin claves autorales**: la config viaja solo en
  `app/google-services.json` (fuera de VCS; lo aporta el humano) mediante el
  plugin `com.google.gms.google-services`, aplicado **condicionalmente** en
  `:app` (`if (file("google-services.json").exists())`) para que los gates estén
  en verde sin credenciales. El único material de config del APK son los
  recursos estándar que inyecta ese plugin (verificación 3 de `extraccion-nube`:
  0 `buildConfigField`, 0 claves autorales).
- App Check: Firebase AI Logic exige **App Check enforced** (obligatorio desde
  2026-11-02). El test real instala el **debug provider** de App Check
  (`firebase-appcheck-debug`, solo en androidTest + variante debug de `:app`;
  sin provider de producción todavía) con un **debug token fijo** aportado por
  el humano (`app/appcheck-debug-token.txt`, fuera de VCS) y registrado a mano
  en la consola de Firebase; ver `docs/evidence/extraccion-nube/`. Además, la
  **variante debug de `:app`** instala ese mismo debug provider al arrancar
  (`AppCheckDebugInitializer`, un `ContentProvider` declarado solo en
  `app/src/debug/`), de modo que la app real en debug —no solo el test— puede
  llamar a Gemini; el token debug lo genera el dispositivo y se registra a mano
  en la consola (fuera del APK). Nunca viaja en release.

## Procesado de la extracción (pantalla 06)

- `procesado-ui` cablea el procesado como subflujo interno de `:feature:escanear`
  (sin rutas nuevas en `:app`): "Continuar" en la pantalla 05 abre el estado
  `ProcesandoExtraccion` del `EscanearViewModel`, que ejecuta `ExtraerTicket` y
  termina en `ExtraccionLista` (resultado de solo lectura con completitud de
  campos) o `ExtraccionFallida` (error honesto, sin datos inventados ni fallback,
  ADR 0001).
- La cancelación es **estructurada**: la orden de extracción es una entrada
  `MutableStateFlow<OrdenExtraccion>`; `flatMapLatest` cancela la corrutina activa
  al cambiar de orden (Continuar/Reintentar/Cancelar/Volver) y
  `runCatchingLanzandoCancelacion` re-lanza **siempre** `CancellationException`.
  Cancelar conserva la foto y el Borrador y no escribe en Room.
- Los pasos del mockup 06 son reales: Encuadre y OCR ya están completados (ocurren
  en la captura); Categorización es el paso activo mientras corre `ExtraerTicket`.
  No hay porcentajes de progreso ni de confianza inventados. El **badge de motor**
  muestra el motor real (`ExtraerTicket.motor`, passthrough de `TextEngine.motor`).
- La latencia real de Gemini (R2) se hace visible aquí: progreso indeterminado +
  "Cancelar" siempre disponible.
- **Reintento con el otro motor (`reintento-otro-motor`)**: `EngineRouter`
  implementa además la interfaz de dominio `ExtractorPorMotor`
  (`disponibilidad(motor)`, `extract(motor, texto)`), registrada en `AiModule` con
  la **misma instancia** singleton que el `TextEngine` sin calificar. La
  extracción explícita **ignora la política y la preferencia**, no hace fallback
  y, si el motor no está `DISPONIBLE`, lanza `MotorNoDisponibleException` sin
  invocarlo. Disponibilidad pura `disponibilidadMotor(motor, capaz, modelo)`:
  Nube siempre `DISPONIBLE` (la red no es entrada); Local solo con `capaz &&
  modelo` (`NO_CAPAZ` gana sobre `SIN_MODELO`). Casos de uso
  `ExtraerTicketConMotor` (texto blank → vacío con ese motor) y `ObtenerOtroMotor`
  (¬motor + disponibilidad). El VM reutiliza el `textoCrudo` del Borrador: ni
  OCR ni foto nuevos. `ProcesandoExtraccion`/`ExtraccionFallida` llevan el `motor`
  (badge desde el estado, no desde `viewModel.motor`) y `ExtraccionLista` /
  `ExtraccionFallida` el `otro: OfertaMotor`. El resultado previo viaja como
  `anterior` en la orden `ReintentarCon` (D4): cancelar el reintento o su fallo
  vuelven a él (`Restaurar`); el éxito lo reemplaza y `Ticket.motor` es el del
  resultado aceptado. "Reintentar" repite con el motor que falló, no con el que
  elegiría el router ahora.

## Revisión y guardado (pantalla 07)

- `revision-datos` cierra el flujo de escaneo: desde el resultado (pantalla 06)
  "Revisar ticket" abre el estado `RevisionDatos` del `EscanearViewModel` (mockup
  07), donde el usuario corrige la **cabecera** (comercio, fecha, total y categoría
  con chips de la enum fija de 6) y Guarda.
- `edicion-lineas` (mockup 08) evoluciona la **misma pantalla y el mismo estado**
  (`RevisionDatos`, sin rutas ni estados nuevos): editor de líneas (añadir,
  eliminar, editar concepto/cantidad/precio), footer "Total recalculado" y dock
  "Descartar" / "Guardar cambios". El modelo de edición es dominio puro:
  `LineaEditable` (id de cliente, no persistido, usado como `key` de Compose) +
  `lineasEditablesIniciales`, `nuevaLineaEditable`, `totalRecalculado` y
  `List<LineaEditable>.aLineas()` (descarta conceptos en blanco, cantidad ≥ 1,
  precio ≥ 0). `DatosRevision.lineas` lleva lo editado y `GuardarTicket` persiste
  `datos.lineas.aLineas()`; `TicketDao.upsert @Transaction` reemplaza el conjunto
  (sin cambios de Room). El **total del ticket no se deriva de las líneas**
  (regla "Total vs líneas"): el recalculado es solo referencia y el total solo
  cambia editando el campo "Total (con IVA)". El VM ignora cantidades/precios
  no parseables o fuera de rango (conserva el último valor válido).
- El mapeo `TicketExtraido` → `Ticket`/`Linea` vive en **dominio**: el caso de uso
  `GuardarTicket` (`@Inject constructor(TicketRepository)`, `operator fun invoke`)
  construye el agregado `Ticket` (con `DatosRevision` corregido, `fechaCaptura` =
  ahora, `motor` del `TicketExtraido`, `rutaFoto` relativa, `textoCrudoOcr` del
  Borrador, `nota = null`) y delega en `TicketRepository.save` (`@Transaction` en
  `TicketDao.upsert`, ya existente). Funciona también con extracción vacía.
- La foto ya está en disco (`filesDir/fotos`): solo se vincula su ruta. El Borrador
  no se persiste; al Guardar, el `EscanearViewModel` pasa por `Guardando` → `Guardado`
  y `EscanearScreen` emite el callback `onGastoGuardado`; `:app` navega a la pestaña
  **Gastos** (placeholder hasta `listado-gastos`) y el subflujo se resetea a
  `Encuadre`. Las pantallas no reciben el `NavController`.
- Back desde la revisión = volver al resultado sin escribir en Room
  (`CancellationException` siempre re-lanzada). El badge de motor se conserva.

## Seguimiento: período del panel

- Dominio (`:domain:expenses`, Kotlin puro): `PeriodoTipo` (DIA, SEMANA, MES, PERSONALIZADO) y
  `Periodo(tipo, desde, hasta)` con rangos **inclusivos** por fecha de **emisión**;
  la semana es siempre lunes-domingo (no depende del `Locale`).
  `ObtenerPeriodo(Clock)` calcula "hoy" con el `Clock` inyectado (correcto en los
  cambios de hora de Europe/Madrid) y `ObservarTicketsDelPeriodo` delega en
  `TicketRepository.observeByDateRange`. Es el punto de entrada que reutilizarán
  `resumen-total`, `grafica-donut`, `listado-gastos` y `rango-personalizado`.
- Rango personalizado (`rango-personalizado`): `Periodo.personalizado(desde, hasta)` (`desde <= hasta` o
  `IllegalArgumentException`); `Periodo.de(PERSONALIZADO, _)` lanza; el período anterior son los N días
  inmediatamente previos. `ObtenerPeriodo(tipo, personalizado)` devuelve el rango dado. Los ViewModels de Resumen y
  Gastos guardan una `Seleccion(tipo, rango)` privada (selección independiente por pantalla, solo en memoria);
  `onSeleccionarRango` ignora un rango invertido. `PeriodoSelector` (`:feature:common`) tiene 4 segmentos y abre
  `RangoFechasDialog` (`DatePickerDialog` + `DateRangePicker`, sin días futuros); el picker devuelve millis a
  medianoche UTC y se convierte con `ZoneOffset.UTC` (`FechasPicker.kt`). La etiqueta añade el año si el rango cruza
  año o no es el actual.
- `Clock` se provee en el `di.kt` de `:app` (`Clock.systemDefaultZone()`); los tests
  pasan `Clock.fixed(...)`.
- `ResumenViewModel` (`:feature:resumen`): `MutableStateFlow<PeriodoTipo>` (Mes por
  defecto, sin persistir) -> `flatMapLatest` -> Flow de Room; sin `load()`. La pantalla
  muestra la `TarjetaTotal`. Desviación conocida: `stateAsResultIn`/`AcScaffold` de
  AGENTS.md no existen aún; se usa `stateIn` como en Ajustes.
- Tarjeta de total (`resumen-total`): `Periodo.anterior()` devuelve siempre el período
  anterior **completo** (día = ayer, semana = lunes-domingo previo, mes natural previo).
  `CalcularResumenPeriodo` (pura, con `hoy` explícito) produce `ResumenPeriodo(total,
  numTickets, promedioDiario, totalAnterior, variacion)`: el promedio divide entre los
  días transcurridos si el período está en curso y entre los totales si ya terminó
  (mín. 1); `variacion` es `null` si el anterior no tiene gasto (badge neutro "Sin datos
  del período anterior"). `ObservarResumenDelPeriodo` combina los Flow del período y del
  anterior con el `Clock` inyectado. `ResumenUiState.resumen` alimenta `TarjetaTotal`;
  importes en `Double`, redondeo solo al formatear (`FormatoResumen.kt`, cifras `tnum`).
- Donut de categorías (`grafica-donut`): `CalcularDistribucionCategorias` (pura) agrupa los
  tickets por categoría en céntimos `Long` (HALF_UP desde el `Double`), omite las de 0 y reparte
  porcentajes enteros por resto mayor (suman 100; orden por importe desc, empate por `ordinal`).
  `ObservarDistribucionDelPeriodo` la aplica al Flow de `ObservarTicketsDelPeriodo`.
  `ResumenUiState.distribucion` (`null` = cargando) alimenta `TarjetaDistribucion` (`Canvas`,
  sin librerías de gráficas; geometría pura en `segmentosDonut`). Etiqueta, icono y color de
  categoría viven en `:feature:common` (`CategoriaUi.kt`); `:feature:resumen` depende de
  `:feature:common` y `:feature:common` de `:domain:expenses` (no son módulos nuevos).

### Listado de tickets (`listado-gastos`)

- Dominio: `AgruparTicketsPorDia` (orden `fechaEmision` desc, empate `id` desc; total del día en céntimos con
  la misma conversión HALF_UP que la distribución; un total <= 0 o no finito aparece pero no suma) y
  `ObservarListadoDelPeriodo` (reutiliza `ObservarTicketsDelPeriodo`; sin repositorio ni DAO nuevos).
- `:feature:gastos` (pestaña Gastos): `GastosViewModel` (`MutableStateFlow<PeriodoTipo>` -> `flatMapLatest`
  -> `stateIn`; `grupos == null` = cargando, vacío = lista vacía) y una única `LazyColumn` con claves
  `d_<fecha>`/`t_<id>`. `:feature:resumen` añade `SeccionRecientes` (máx. 5 filas, "Ver todos (N)") dentro de
  su `Column` con scroll, sin lista perezosa anidada.
- `PeriodoSelector`, `formatoImporte`, `EstiloTabular`, `etiquetaPeriodo`, `etiquetaDia`, `FilaTicket` y
  `CabeceraDia` viven en `:feature:common` para que gastos y resumen no se conozcan entre sí.
- Navegación: `:app` añade la ruta `ticket/{ticketId}` (`TallyRoute.Ticket`) con `TicketDetalleScreen`
  (`detalle-ticket`); las pantallas solo reciben callbacks (`onTicketClick`, `onVerTodos`, `onEscanear`,
  `onBack`). La barra inferior se oculta en esa ruta.

### Detalle de ticket y borrado (`detalle-ticket`)

- Dominio: `EdicionDatos.kt` (reductores puros de edición extraídos de `EscanearViewModel`, con
  `conservarHora`), `ObtenerTicket`, `ActualizarTicket` (mismo id; no toca foto, captura, motor, texto OCR
  ni nota) y `BorrarTicket`. `TicketRepository.delete` borra primero la BD y luego la foto; un fallo del
  fichero (false o excepción, salvo `CancellationException`) deja un huérfano en `filesDir/fotos` aceptado y no
  convierte el borrado en error.
- `:feature:common` aloja el formulario editable (`FormularioTicketEditable`), `DockEdicion`, `BadgeMotor` y
  `FotoOriginal` (decodifica en IO con `inSampleSize`), compartidos por la revisión (`:feature:escanear`, que
  gana la dependencia a `:feature:common`) y el detalle (`:feature:gastos`); los features siguen sin conocerse.
- `TicketDetalleViewModel`: id por `SavedStateHandle`, lectura única del ticket (`getById`), estado
  `Cargando | NoExiste | Datos` por cadena de operadores; guardar/borrar con guarda anti doble toque y `hecho`
  que dispara `onBack` una vez. Sin migración Room ni dependencias nuevas.
- `mejoras-visuales-2`: `:feature:common/ComponentesSeccion.kt` añade `TarjetaSeccion`, `TituloSeccion`,
  `EstadoPantalla` y `CargandoPantalla` (usados por Resumen, Ajustes, Gastos, Detalle y Escanear sin que los
  features se conozcan) y `LayoutAncho.kt` la constante `UMBRAL_APILAR_BOTONES`. Sin módulos ni dependencias nuevos.

### Papel de ticket (`papel-ticket`)

- `:feature:common` aloja `PapelTicket.kt`: `PapelTicket` (hoja con borde serrado `TicketSerradoShape`,
  sombra 2 dp y hairline, ancho máx. 480 dp), `SeparadorPunteado`, `FilaRecibo`, `reciboTexto` (monoespaciada del
  sistema, solo dentro del papel) e `importeDeLinea` (cantidad por precio). Lo envuelven `FormularioTicketEditable`
  (revisión y detalle: cabecera, desglose y total al final en un solo papel) y el resultado de solo lectura de
  `ProcesadoContent.kt`. Sin módulos, dependencias, ficheros de fuente, dominio ni ViewModels tocados. El papel es
  siempre claro (la app no tiene tema oscuro); excepciones documentadas en `DESIGN.md`.

### Layout de ancho y cabecera comun (`layout-tablet-coherente`)

- `:feature:common` aloja `LayoutAncho.kt` (`ANCHO_MAXIMO_CONTENIDO` = 720 dp y `ColumnaCentrada`) y
  `CabeceraPantalla.kt`. Todas las pantallas (Resumen, Gastos, Detalle, Ajustes, Escanear sin camara) y `TallyBottomBar`
  / `DockEdicion` componen su contenido con `ColumnaCentrada`; fondos, camara y scroll quedan a todo el ancho. Los
  features no se importan entre si: solo dependen de `:feature:common`. `:feature:ajustes` gana esa dependencia de
  proyecto (no es un modulo nuevo). Sin `WindowSizeClass`, dominio ni framework tocados.

### Pulido visual (`mejoras-visuales`)

- `:app` (`MainActivity`) llama a `enableEdgeToEdge` con `SystemBarStyle.light` (iconos oscuros) y `TallyApp` marca
  `innerPadding` como consumido en el `NavHost` (un solo inset superior); define las transiciones (fundido 150 ms entre
  pestañas, deslizamiento 300 ms hacia/desde Detalle y Ajustes). `:feature:common` añade `PADDING_PANTALLA` (20 dp),
  `PAPEL_ANCHO_MAXIMO` pública y la forma `extraLarge` = 28 dp en el tema; Procesado (`:feature:escanear`) usa la
  `CabeceraPantalla` común. Sin módulos, dependencias, dominio ni framework tocados; el tag `periodo_rango` desaparece
  (el rango vive solo en `resumen_rango`).

### Forma de pago (`forma-pago`)

- `:feature:common/FormaPagoUi.kt` (etiqueta, icono, colores, `ChipFormaPago`, `PagoEnPapel`) lo comparten el
  formulario editable (selector, campos Entregado/Cambio y aviso), el resultado del procesado, `FilaTicket` y el
  detalle sin que los features se conozcan. Los ViewModels de Escanear y del Detalle solo cablean
  `onFormaPagoElegida/onEntregadoEditado/onCambioEditado` hacia los reductores puros de `EdicionDatos.kt`.
  Sin módulos ni dependencias nuevos.

## Convenciones de build

- `build-logic/` expone 4 convention plugins: `tally.android.application`,
  `tally.android.library`, `tally.android.compose`, `tally.hilt`.
- Todas las versiones viven en `gradle/libs.versions.toml`. Ningún
  `build.gradle.kts` declara versiones literales.
- Un fichero `di.kt` por módulo Gradle que use Hilt; las implementaciones de
  `framework` se declaran `internal`.

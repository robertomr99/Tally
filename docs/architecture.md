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
 ├─ :feature:common       (compose; tema, tipografía y utilidades de UI)
 ├─ :feature:resumen      (compose; pantallas)
 ├─ :feature:ajustes      (compose; Ajustes: sección "Modelo local", descarga y token)
 ├─ :feature:gastos       (compose; pantallas)
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
   cuota de `gemini-3.8-flash`; ver R2); se binding con `@Provides TextEngine` en el
  `di.kt` del módulo. Errores de red/cuota/config se propagan al llamador y una
  `CancellationException` jamás se convierte en resultado.
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

## Convenciones de build

- `build-logic/` expone 4 convention plugins: `tally.android.application`,
  `tally.android.library`, `tally.android.compose`, `tally.hilt`.
- Todas las versiones viven en `gradle/libs.versions.toml`. Ningún
  `build.gradle.kts` declara versiones literales.
- Un fichero `di.kt` por módulo Gradle que use Hilt; las implementaciones de
  `framework` se declaran `internal`.

# Arquitectura

Documento de referencia de la estructura de módulos y la dirección de
dependencias. Las reglas operativas completas están en `AGENTS.md`.

## Diagrama de módulos

```
:app                      (application, ensambla todo, define navegación)
 ├─ :domain:core          (Kotlin puro, sin Android)
 ├─ :domain:expenses      (Kotlin puro, sin Android)
 ├─ :framework:database   (android library internal; implementa interfaces de domain)
 ├─ :framework:ai         (android library internal; implementa interfaces de domain)
 ├─ :feature:common       (compose; tema, tipografía y utilidades de UI)
 ├─ :feature:resumen      (compose; pantallas)
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
  hornea la orientación EXIF en los píxeles, escala a ≤1280 px de lado largo
  conservando proporción y recomprime JPEG (~85) sin tag de orientación. El
  contrato (`PhotoProcessor`) está en `:domain:expenses`; el original de cámara
  no se guarda.
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
  está en disco). "Continuar" sigue deshabilitado; el texto es visible en un
  panel **solo debug** de la pantalla 05 (`captura_texto_crudo`).

## Convenciones de build

- `build-logic/` expone 4 convention plugins: `tally.android.application`,
  `tally.android.library`, `tally.android.compose`, `tally.hilt`.
- Todas las versiones viven en `gradle/libs.versions.toml`. Ningún
  `build.gradle.kts` declara versiones literales.
- Un fichero `di.kt` por módulo Gradle que use Hilt; las implementaciones de
  `framework` se declaran `internal`.

# AGENTS.md

Contexto permanente para agentes que trabajan en este repositorio.
La justificación y los diagramas están en `docs/architecture.md`; aquí solo van
las reglas operativas. Si algo de este fichero contradice al código existente,
pregunta antes de decidir.

## Leer primero / flujo de trabajo

Documentos de descubrimiento (fuente de verdad de producto y diseño):

- `CONTEXT.md` — glosario y lenguaje del dominio.
- `docs/build-brief.md` — problema, usuarios, MVP, non-goals, validación.
- `docs/domain-model.md` — entidades, estados y reglas del dominio.
- `docs/risks-and-open-questions.md` — riesgos y preguntas abiertas.
- `docs/technical-discovery.md` — stack, datos, OCR, motores, despliegue.
- `DESIGN.md` — dirección visual y qué es normativo de `docs/design/stitch/`.
- `docs/adr/` — decisiones aceptadas; no las contradigas sin nuevo ADR.

Antes de escribir código:

1. Lee `PROGRESS.md` (estado verificado y siguiente paso).
2. Elige la primera feature lista de `feature_list.json` cuyas
   `depends_on` estén `passing`; una sola feature por sesión.
3. Ejecuta `bash init.sh` (ruta estándar de arranque y verificación).
4. Si la verificación base falla, arregla la base antes de añadir features.
5. Al terminar: actualiza `PROGRESS.md` y `feature_list.json` con evidencia.
   Una feature solo está `passing` con verificación ejecutada y evidencia
   registrada, no por código escrito.

## Proyecto

App Android nativa de control de gastos a partir de fotos de tickets.
OCR en dispositivo, extracción de datos estructurados con un modelo de lenguaje
(local o en nube), almacenamiento local y panel de gastos por categoría.

## Stack

- Kotlin, 100 %. Sin Java en código fuente.
- Jetpack Compose + Material 3. Sin XML de layouts.
- Clean Architecture + MVVM, multi-módulo Gradle.
- Single-Activity: una sola `MainActivity`.
- Hilt para inyección de dependencias.
- Coroutines + Flow para concurrencia. Nada de callbacks ni RxJava.
- Room para persistencia. Sin backend propio ni sincronización remota.
- Versiones centralizadas en `gradle/libs.versions.toml`. No escribas versiones
  literales en ningún `build.gradle.kts`.

## Módulos y regla de dependencias

```
:app                  ensambla todo y define la navegación
:domain:*             Kotlin puro, sin dependencias de Android
:framework:*          implementaciones técnicas (Room, MediaPipe, Firebase)
:feature:*            pantallas Compose + ViewModels
:test:unit            dobles de prueba compartidos
```

Reglas que no se rompen:

- `domain` no importa nada de Android, de `framework` ni de `feature`.
- `framework` implementa interfaces declaradas en `domain`. Nunca al revés.
- Los módulos `feature` no se conocen entre sí. Solo `:app` sabe cómo se conectan.
- Las implementaciones y los módulos Hilt de `framework` se declaran `internal`.
- Un fichero `di.kt` por módulo Gradle.

## Dónde va cada cosa

- Entidades de dominio → `domain/<área>/entities/`
- Interfaces de DataSource → `domain/<área>/data/`
- Repositorios (clases concretas, `@Inject constructor`, reciben interfaces) →
  `domain/<área>/data/`
- Casos de uso, uno por acción, con `operator fun invoke(...)` →
  `domain/<área>/usecases/`
- Implementaciones de DataSource → `framework/<área>/`
- Entidades Room y DAOs → `framework/<área>/database/`
- Pantallas y ViewModels → `feature/<pantalla>/`
- Composables reutilizables, tema y utilidades de UI → `feature/common/`

## Capa de presentación

Cada feature tiene exactamente estos ficheros:

- `XScreen.kt` — composable raíz. Obtiene el ViewModel con `hiltViewModel()`
  y recoge el estado con `collectAsState`.
- `XViewModel.kt` — `@HiltViewModel`. Recibe casos de uso por constructor.
  Expone `StateFlow`. No importa nada de Compose.
- `XState.kt` — estado puramente visual (scroll, menús, snackbars) con
  `mutableStateOf` y setters privados. Se crea con `rememberXState()`.

Reglas:

- Flujo unidireccional: el estado baja, los eventos suben como lambdas.
- Los ViewModels declaran el estado como cadena de operadores sobre
  `MutableStateFlow` de entrada (`combine`, `flatMapLatest`, `debounce`).
  No escribas funciones `load()` imperativas que muten estado.
- Todo `Flow` del dominio se convierte con `stateAsResultIn(viewModelScope)`,
  que produce `Result<T>` con Loading / Success / Error.
- Usa `AcScaffold<T>` para pintar según el `Result`. No repitas el `when`
  en cada pantalla.

## Navegación

- Un `NavHost` en `:app`. Rutas tipadas con `sealed class`.
- Las pantallas no reciben el `NavController`. Reciben callbacks (`onClick`,
  `onBack`).

## Capa de IA

El contrato vive en dominio y las implementaciones en framework, igual que
cualquier otro DataSource.

- `TextEngine` (interfaz) → `domain/expenses/data/`
- `GemmaLocalEngine` (MediaPipe, on-device) → `framework/ai/`
- `GeminiCloudEngine` (Firebase AI Logic) → `framework/ai/`
- `EngineRouter` elige motor en tiempo de ejecución según disponibilidad.

Reglas:

- `domain` no conoce MediaPipe ni Firebase. Solo la interfaz.
- La carga del modelo local nunca ocurre en el hilo principal.
- El motor en uso se expone en el estado de UI y se muestra al usuario.
- Ninguna clave de API dentro del APK. Nada de `BuildConfig` con secretos.

## Persistencia

- Room es la única fuente de verdad. No hay caché con TTL ni refresco remoto.
- Los DAO devuelven `Flow` para que la UI se actualice sola.
- Escrituras que tocan varias tablas van con `@Transaction`.

## Testing

- Tests unitarios de casos de uso, repositorios y ViewModels.
- `Turbine` para tests de `Flow`; `CoroutineTestRule` para el dispatcher.
- Dobles compartidos en `:test:unit`.
- En tests instrumentados, sustituye módulos Hilt con `@TestInstallIn` y usa
  Room en memoria.

## Convenciones

- Conventional Commits.
- Nombres y comentarios en español; identificadores de código en inglés.
- Antes de crear un fichero nuevo, comprueba si ya existe uno equivalente.
- Si una tarea requiere romper alguna regla de este documento, dilo y espera
  confirmación en vez de saltártela.

## Qué no hacer

- No añadas Retrofit, OkHttp ni ninguna capa de red REST: no hay API propia.
- No metas dependencias de Android en módulos `domain`.
- No crees módulos Gradle nuevos sin preguntar.
- No toques `build-logic/` salvo que se te pida explícitamente.

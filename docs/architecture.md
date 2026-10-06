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
 └─ :feature:resumen      (compose; pantallas)

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

## Convenciones de build

- `build-logic/` expone 4 convention plugins: `tally.android.application`,
  `tally.android.library`, `tally.android.compose`, `tally.hilt`.
- Todas las versiones viven en `gradle/libs.versions.toml`. Ningún
  `build.gradle.kts` declara versiones literales.
- Un fichero `di.kt` por módulo Gradle que use Hilt; las implementaciones de
  `framework` se declaran `internal`.

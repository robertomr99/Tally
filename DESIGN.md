---
name: Tally — dirección visual
description: Sistema visual Forest Precision (Material 3 Expressive, verde pino + menta, Inter) para la app de gastos por ticket fotografiado.
designAssets:
  sourceOfTruth:
    - path: docs/design/stitch/stitch-design-system.md
      role: Tokens del sistema (colores, tipografía, elevación, formas, componentes)
      status: fuente de verdad (con las adaptaciones de este documento)
    - path: docs/design/stitch/02-resumen-principal/
      role: Resumen con selector de período, donut y desglose
      status: fuente de verdad
    - path: docs/design/stitch/06-procesando-ticket/
      role: Estado de procesado OCR/IA con indicador de motor
      status: fuente de verdad (sustituir textos decorativos)
    - path: docs/design/stitch/07-ticket-generado/
      role: Revisión del ticket digitalizado (Reintentar/Editar/Guardar)
      status: fuente de verdad (sustituir textos decorativos)
    - path: docs/design/stitch/08-edicion-ticket/
      role: Edición completa (líneas añadibles/borrables, total recalculado)
      status: fuente de verdad
    - path: docs/design/stitch/01-splash/
    - path: docs/design/stitch/03-listado-gastos/
    - path: docs/design/stitch/04-camara-encuadre/
    - path: docs/design/stitch/05-revision-foto/
    - path: docs/design/stitch/09-selector-rango-fechas/
      role: Pantallas de apoyo (misma autoridad que las anteriores)
      status: fuente de verdad
  generatedConcepts: []
colors:
  primary: "#002718"
  on-primary: "#ffffff"
  primary-container: "#133e2b"
  on-primary-container: "#7da990"
  secondary: "#2b6a47"
  on-secondary: "#ffffff"
  secondary-container: "#acefc2"
  on-secondary-container: "#2f6e4b"
  tertiary-container: "#003f33"
  on-tertiary-container: "#71ab9a"
  error: "#ba1a1a"
  background: "#f5fbf6"
  surface: "#f5fbf6"
  surface-container-low: "#eff5f0"
  surface-container: "#e9efea"
  surface-container-high: "#e4eae5"
  surface-container-highest: "#dee4df"
  on-surface: "#171d1a"
  on-surface-variant: "#414943"
  outline: "#717973"
  outline-variant: "#c1c8c1"
typography:
  family: Inter (incluida en la app; nada de fuentes por red)
  headline:
    fontSize: 24–28px, peso 500–600
  title:
    fontSize: 14–22px, peso 600
  body:
    fontSize: 14–16px, peso 400
  label:
    fontSize: 11–14px, peso 500–600
  regla-importe:
    fontFeatureSettings: "tnum" (cifras tabulares) en TODOS los importes
rounded:
  sm: 8dp (inputs, chips, mini-indicadores)
  md: 12dp (cards, filas)
  lg: 16dp (bottom sheets, contenedor de recibo)
  xl: 28dp (diálogos; forma extraLarge del tema)
  pill: 9999dp (botones primarios, FAB, pills de navegación, chips activos)
spacing:
  base: rejilla de 8dp (malla de 4dp para listas densas)
  margins: 16dp móvil / 24dp tablet
---

# Design Direction

## Overview

Identidad **Forest Precision**: minimalismo corporativo moderno sobre Material 3
Expressive. Verde pino profundo como color estructural, menta como acento de
acción/estado activo, superficies tonales casi sin sombras. Densidad de
información alta con ritmo vertical disciplinado. Cero tono infantil o
"gamificado": rigor técnico y calma. En español todo el copy.

## Existing Design Assets

- **`docs/design/stitch/`**: exportación de Google Stitch con 9 pantallas
  (mockup `screen.png` + `code.html` cada una) y el design system
  `stitch-design-system.md`. Es la **fuente de verdad visual**.
- **`code.html`**: referencia de estructura, contenido y proporciones. El HTML
  NO se traslada literalmente (es web/Tailwind); se traduce a componentes
  Compose + Material 3.
- Los `screen.png` son mockups con datos de ejemplo ficticios.

## Generated Concept Images

Ninguna. No se generan conceptos: existe un diseño propio con autoridad de
fuente de verdad.

## Product Feel

Operativo, preciso, tranquilo. "Gestión financiera seria con la velocidad de
una herramienta de desarrollador". La estética de recibo (papel blanco,
bordes serrados, línea punteada antes del total) se reserva al ticket
digitalizado; el resto de la app es superficies tonales verdes.

## Colors

Roles Material 3 en el front matter. Notas de uso (del design system):

- Botones primarios/FAB: `primary` con icono en `secondary-container`.
- Elemento activo (pill de navegación, chip de categoría seleccionado,
  chip de período activo): `secondary-container` + `on-secondary-container`.
- Elevación TONAL, no sombras: superficies de la escala
  `surface-container-*`; sombras solo en FAB y docks inferiores.
  **Excepción (spec `papel-ticket`)**: el `PapelTicket` lleva una sombra de
  2 dp más hairline `outline-variant` al 40 %, porque blanco sobre `surface`
  (#F5FBF6) apenas se distingue y el borde serrado necesita separarse. No se
  extiende a ningún otro componente.
- **Papel siempre claro**: la app no tiene tema oscuro (solo `lightColorScheme`).
  El papel usa `surface-container-lowest` (blanco) con `on-surface` /
  `on-surface-variant` (> 9:1) y se declara claro por decisión de producto. Si
  se añade `darkColorScheme`, el papel deberá fijar colores propios (papel/tinta)
  y los componentes M3 internos recibir un esquema claro local.
- Categorías: paleta semántica apagada del design system
  (`stitch-design-system.md`, sección "Semantic Financial Categories").
  DECIDIDO en `grafica-donut`: un color fijo por categoría (no por posición): ALIMENTACION ámbar `#B86A22` (oscurecido desde `#C87D32` para llegar a 3:1), TRANSPORTE azul acero `#4A6B82`, OCIO ciruela `#7A587A`, HOGAR musgo `#5D6E3F`, SALUD verde azulado `#2E776E`, OTROS pizarra `#5F6561`. Contraste WCAG medido (`CategoriaUiContrasteTest`) sobre `surface-container-low` 3,71-5,42:1 y sobre la pista `surface-container-highest` 3,18-4,64:1; icono blanco 4,10-5,99:1. El color nunca es el único canal (nombre, %, icono, hueco de 2° entre arcos). Revisión visual y de daltonismo pendiente de humano.

## Typography

- **Inter** en todos los roles, empaquetada en la app (no descargable en
  runtime: la app funciona sin red).
- **Excepción (spec `papel-ticket`)**: dentro del `PapelTicket` (comercio, fecha,
  líneas, importes y total, también el texto escrito en los campos editables) se
  usa `FontFamily.Monospace` del sistema con `tnum` (`reciboTexto(base)`). Sin
  ficheros de fuente ni dependencias. Etiquetas flotantes, chips, botones,
  contadores y todo lo que está fuera del papel siguen en Inter.
- Importes SIEMPRE con cifras tabulares (`tnum`); en Compose:
  `TextStyle(fontFeatureSettings = "tnum")`.
- Símbolo de euro alineado a la altura de los dígitos (title-lg junto al
  display del total).
- Etiquetas de metadatos en mayúsculas con tracking amplio (`label-sm/md`).

## Layout

- Rejilla de 8dp; margen horizontal de pantalla único de **20dp** en todas las
  anchuras (`PADDING_PANTALLA` en `feature/common/LayoutAncho.kt`): cabecera,
  selector, tarjetas, listas, formularios, docks y botones de Procesado. El
  papel de ticket conserva su padding interior de 16dp. Los 24dp de tablet del
  mockup quedan como seguimiento, no MVP.
- **Un solo inset superior**: el `Scaffold` de `TallyApp` reserva la barra de
  estado en `innerPadding` y el `NavHost` lo marca como consumido
  (`consumeWindowInsets`), de modo que el título de toda pantalla arranca a la
  misma distancia (alto de barra + el padding de `CabeceraPantalla`). Las
  pantallas pueden seguir llamando a `statusBarsPadding()` (resuelve a 0); ninguna
  pinta un fondo distinto al `surface` en esa franja. `MainActivity` activa
  `enableEdgeToEdge` con barras transparentes e iconos oscuros (la app solo tiene
  tema claro). Detalle y Revisión de datos aplican `imePadding()` para que el dock
  y el campo enfocado queden sobre el teclado.
- **Fuentes grandes**: con `fontScale` > 1,15 el dock de edición apila sus botones
  (Guardar primero) y baja el subtítulo del total; con > 1,4 los pasos de Procesado
  se apilan sin chevrons.
- **Transiciones** (`NavHost`): entre pestañas, fundido de 150 ms; hacia/desde
  Detalle y Ajustes, deslizamiento horizontal de 300 ms con pop inverso.
- **Regla única de ancho**: TODAS las pantallas usan una sola columna centrada
  de `ANCHO_MAXIMO_CONTENIDO` = 720 dp (`feature/common/LayoutAncho.kt`,
  contenedor `ColumnaCentrada`). Los 720 dp incluyen los paddings horizontales
  de cada pantalla. Por debajo de 720 dp de ancho disponible no tiene efecto:
  el layout es el de móvil. No hay otros anchos máximos de pantalla (los 480 dp
  de `PapelTicket` y de la fila donut+leyenda son anchos de componente).
- **A todo el ancho** (no van en la columna): fondos de pantalla y de la barra
  inferior, la vista de cámara/encuadre, el marco de la foto en "Revisar foto"
  y el fondo del dock de edición. Su contenido (items de la barra, botones del
  dock, controles del obturador, títulos) sí se agrupa en la columna.
- **Compacto** (realme GT 2): columna única, bottom bar de 3 destinos
  (Resumen / Gastos / Escanear), FAB "Escanear" anclado encima.
- **Expandido** (Tab S9+ y móvil horizontal): MISMO contenido en la columna
  de 720 dp centrada (`max-w-3xl` del mockup 03); la barra inferior mantiene
  su fondo completo con los 3 items agrupados en la columna. Navigation rail y
  split-screen quedan para pulido (no MVP).
- **Jerarquía de títulos** (4 niveles): (1) título de pantalla =
  `CabeceraPantalla` (`titleLarge` SemiBold `onSurface`, subtítulo opcional
  `labelLarge` `onSurfaceVariant`, altura mínima 56 dp, atrás y acción
  opcionales); (2) título de sección/tarjeta = `titleMedium` SemiBold con
  subtítulo `bodySmall`; (3) etiqueta de métrica = `labelMedium`/`labelSmall`
  en mayúsculas `onSurfaceVariant`; (4) cabecera de grupo de lista
  (`CabeceraDia`, "Gastos del periodo") = `titleMedium`/`labelLarge` alineada
  con las filas de la lista.
- Listas con buffer inferior explícito para no quedar tapadas por
  FAB/bottom bar.

## Shapes

Pill (9999dp) para botones primarios, FAB, pills de navegación y chips
activos. 12dp en cards y filas. 16dp en bottom sheets, contenedor de recibo y
radios interiores. 24dp en las tarjetas grandes de Resumen. 28dp en diálogos
(`AlertDialog`, `DatePickerDialog`): es la forma `extraLarge` del tema
(`theme/Theme.kt`), así que los diálogos NO fijan `shape` propia. La píldora se
pide siempre explícita (`RoundedCornerShape(9999.dp)`) y nunca depende de
`extraLarge`. 8dp en inputs, chips de categoría e indicadores mínimos.
Elevación tonal con contornos hairline (`outline-variant` al 30–40 %) en lugar
de sombras.

## Components

- **Cabecera de pantalla** (`CabeceraPantalla`, sustituye a la TopAppBar M3
  y a las barras propias): plana sobre `surface`, título de pantalla
  ("Resumen", "Gastos", "Detalle del ticket", "Ajustes", "Revisar foto",
  "Revisar ticket"), subtítulo opcional (Resumen y Gastos: rango del período),
  flecha atrás y acción a la derecha opcionales. Sin bordes duros. La marca
  "Tally" solo aparece en el splash y como nombre de la app. Procesado
  mantiene su propio título ("Analizando ticket de compra") y la cámara no
  lleva cabecera para no tapar el encuadre.
- **Selector de período** (pantalla 02): segmented control en píldora con
  Día / Semana / Mes / Personalizado; activo en `secondary-container`. Personalizado abre un
  `DatePickerDialog` con `DateRangePicker` M3 (mockup 09 reducido: sin chips de atajos, tarjetas ni "Restablecer";
  forma del diálogo 28 dp, no la píldora del tema; sin días futuros). Cabe a 360 dp con los 4 segmentos.
- **Tarjeta de total**: etiqueta uppercase, cifra display con `tnum`, badge
  comparativo "vs período anterior", micro-métricas (promedio diario, nº tickets).
- **Donut de categorías** + leyenda + desglose con barras tonales (pantalla 02).
  Con ancho interior >= 480 dp donut (franja del 40 %, centrado en ella) y
  leyenda (60 %) van en fila; por debajo, en columna centrada. Sin porciones
  (cargando o vacío) el donut va solo, centrado en la tarjeta.
- **Badge de motor** (componente NUEVO, obligatorio): chip persistente que
  indica el motor real: "Motor Local · Gemma 3n" (icono memory) o "Motor Nube ·
  Gemini" (icono cloud), con estado (procesando/done). Sustituye los chips
  "Motor OCR v4.2" / "Tally Vision" / "% verificado" del mockup.
- **Contenedor de recibo**: superficie blanca (`surface-container-lowest`),
  borde serrado sutil arriba/abajo (pantalla 07), barra de acento `primary`,
  línea punteada antes del total.
- **Chips de categoría**: radio-group con icono; activo = `secondary-container`
  (pantalla 08). 6 opciones fijas.
- **Editor de líneas**: grid concepto/cantidad/precio/borrar por fila + botón
  "Añadir producto o línea" con borde discontinuo (pantalla 08).
- **Dock inferior de acciones** (pantallas 07/08): Reintentar (outlined) /
  Editar (tonal) / Guardar (filled pill). En edición: Descartar / Guardar
  cambios + "Total recalculado".
- **Procesado** (pantalla 06): badge on-device/nube, silueta de ticket con
  scan beam, progreso circular, pasos Encuadre → OCR → Categorización,
  botón Cancelar. El toggle "Cambiar a Nube/Dispositivo" del mockup es el
  reintento con el otro motor.
- **Listado de gastos** (pantalla 03, spec listado-gastos): secciones por día
  con cabecera "Hoy, 18 de octubre" y "Total: X €"; filas de 56 dp con icono
  de categoría, comercio ("Sin comercio" si está vacío), hora (oculta a las
  00:00), chip de categoría e importe tabular a la derecha. Del mockup se
  omiten buscador, chips de filtro, insignias y avatar. El texto del chip usa
  `onSurfaceVariant`: el color de categoría no llega a 4,5:1 sobre su fondo.

- **Detalle de ticket guardado** (spec detalle-ticket, mockups 07/08): cabecera
  común con flecha atrás, "Detalle del ticket" y papelera en paleta `error`;
  badge del motor real, "Ver foto original" (visor a pantalla completa, sin
  zoom) o tarjeta "Foto no disponible"; el mismo formulario y dock que la
  revisión ("Descartar" / "Guardar cambios", ambos deshabilitados sin cambios).
  Sin barra inferior. Eliminar abre un `AlertDialog` de forma 28 dp (la forma
  `extraLarge` del tema es una píldora, hay que fijarla) con "Eliminar" en
  `error` y "Cancelar". Ticket inexistente: "Este ticket ya no existe" + "Volver".

## Core Screens

| Pantalla | Mockup |
| --- | --- |
| Splash | `docs/design/stitch/01-splash/` |
| Resumen (período + gráfica + listado) | `docs/design/stitch/02-resumen-principal/` |
| Listado de gastos | `docs/design/stitch/03-listado-gastos/` |
| Cámara y encuadre | `docs/design/stitch/04-camara-encuadre/` |
| Revisión de la foto | `docs/design/stitch/05-revision-foto/` |
| Procesando ticket (motor + progreso) | `docs/design/stitch/06-procesando-ticket/` |
| Ticket digitalizado (revisión/guardar) | `docs/design/stitch/07-ticket-generado/` |
| Edición del ticket | `docs/design/stitch/08-edicion-ticket/` |
| Detalle de ticket guardado | `docs/design/stitch/07-ticket-generado/` + `08-edicion-ticket/` |
| Selector de rango de fechas | `docs/design/stitch/09-selector-rango-fechas/` |

## Responsive Baseline

Compacto y expandido con el mismo árbol de composables y UNA regla de ancho:
la columna centrada de 720 dp (`ColumnaCentrada`, ver Layout), sin
`WindowSizeClass`. Verificado a 360 dp, ~427 dp, 700 dp (sin efecto), 840 dp
(márgenes de 60 dp) y 1280 dp (márgenes de 280 dp en total). `WindowSizeClass`,
paddings de 24 dp en tablet, navigation rail y el split-screen del design
system NO son MVP.

## Accessibility Baseline

- Contraste: texto sobre `primary` siempre blanco; nunca texto claro sobre
  `secondary-container` salvo `on-secondary-container`.
- Táctil: mínimo 44dp de altura en botones/chips (los mockups lo cumplen).
- La gráfica nunca es la única fuente de información: siempre con leyenda y
  desglose textual equivalente.
- Sin dependencia del color para estados: chips activos llevan icono check.

## Do's and Don'ts

**Do**

- Traducir los mockups a Material 3 Compose (roles de color, componentes M3).
- Usar `tnum` en cualquier cifra, siempre.
- Mostrar el motor real con el Badge de motor en procesado y ticket.
- Estados vacíos y de error con la misma paleta y un copy en español claro
  (el diseño no los cubre: son hueco, se crean siguiendo esta guía).

**Don't**

- NO copiar literalmente la decoración ficticia de los mockups: "OCR Verificado
  99,4 %", "HASH 8fbc…", "Registro contable Tally #TL-2023-882", "Deducción
  IRPF", "NIF", "Tarjeta Débito **** 4812", "Operación #8294", "Cifrado de
  extremo a extremo", "Garantía de privacidad criptográfica". La app no genera
  esos datos.
- NO implementar desglose de base imponible/IVA: las líneas llevan precio
  final y el total es un campo propio.
- `PapelTicket` (`:feature:common`, spec `papel-ticket`): hoja blanca con bordes
  superior e inferior serrados dibujados con un `Shape`/`Path` (`TicketSerradoShape`,
  dientes de 12x6 dp ajustados al ancho), ancho máximo 480 dp centrado,
  `SeparadorPunteado` y `FilaRecibo` (`n× CONCEPTO ..... importe`; con fontScale
  > 1,4 el importe baja a su línea). Lo usan el resultado del procesado, la
  revisión y el detalle; Resumen, Gastos y Ajustes no. Tag `papel_ticket`.
- NO usar fuentes ni recursos por red (offline-first): Inter incluida en el APK.
- NO inventar animaciones más allá de las del mockup 06 (scan beam, radar,
  progreso circular) y la del splash de arranque (feature `splash-animado`,
  petición del usuario 2026-10-19, ver "Splash de arranque").

## Splash de arranque

Feature `splash-animado`. Splash del sistema (`androidx.core:core-splashscreen`;
en Android 12+ delega en la API nativa), sin pantalla Compose ni ruta propia.

- Diseño normativo: solo el emblema del mockup 01 (4 barras + diagonal a -28°,
  extremos redondeados). No se copian reloj, wifi ni píldora de la captura. Sin
  texto ni eslogan.
- Tokens: fondo `primary` #002718 (`@color/splash_fondo`, espejo de `ForestPrimary`)
  y emblema `primary-fixed` #BFEDD2 (`@color/splash_emblema`, espejo de
  `ForestPrimaryFixed`) en `app/src/main/res/values/colors.xml`. Fondo fijo: no
  hay tema oscuro.
- Animación: una sola pasada (`trimPathEnd` 0 a 1): 4 barras escalonadas
  (180 ms cada una, 80 ms de desfase) y luego la diagonal (300 ms), ~680 ms en total,
  `fast_out_slow_in`. Sin bucle. Estado final sin animar = emblema completo (es lo
  que ve API 26-30, donde la compat no anima).
- Hold artificial máximo 700 ms solo en arranque en frío; 0 con
  `animator_duration_scale` = 0, en rotación y en arranque en caliente.
- Accesibilidad: decorativo, sin información exclusiva de la animación.

## Open Design Questions

1. RESUELTA (`grafica-donut`): paleta semántica apagada del design system, no la
   rampa de verdes de la pantalla 02 (la rampa no discrimina 6 categorías ni
   contrasta con la pista). Ver sección Colors.
2. Acento del fondo de cabecera del recibo (pantalla 07): mantener barra
   `primary` tal cual.
3. Navigation rail en expandido: pospuesto (no MVP).

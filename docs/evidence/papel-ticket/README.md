# Evidencia papel-ticket (sesión 053)

Capturas tomadas en `emulator-5556` (Pixel_10_Pro, 1280x2856, 1,0x) con un test instrumentado temporal ya
eliminado (compone `ExtraccionListaContent` y `RevisionDatosContent` con datos de ejemplo y abre el detalle real
de un ticket sembrado, borrado al terminar). Mismos datos antes y después: MERCADONA S.A., 5 líneas (una con
concepto largo), total 18,45 EUR, recalculado 17,10 EUR. Cada pantalla tiene `_a` (arriba) y `_b` (desplazada
hasta el total y el serrado inferior); el procesado cabe en una sola captura.

| Prefijo | Condición |
| --- | --- |
| `antes_claro_*` | Código anterior a la feature (resultado, revisión, detalle) |
| `despues_claro_*` | Con `PapelTicket`, 427 dp de ancho, fontScale 1,0 |
| (sin `despues_oscuro_*`) | Se capturo con `cmd uimode night yes` y salia visualmente identico al claro (la app no tiene tema oscuro; papel siempre claro), asi que se descartaron junto a los duplicados `antes_claro_2_revision_b` y `antes_claro_3_detalle_b` |
| `despues_360dp_*` | `wm size 1080x2400` + `wm density 480` (360 dp) |
| `despues_360dp_fs13_*` / `despues_360dp_fs20_*` | 360 dp con `font_scale` 1,3 y 2,0 |

Sufijos: `1_procesado`, `2_revision_a/b`, `3_detalle_a/b`. Ajustes restaurados tras las capturas (`wm size reset`,
`wm density reset`, `font_scale 1.0`, `uimode night no`).

## Qué se ve (lectura honesta del agente; revisión visual humana pendiente)

- Procesado: papel blanco con dientes arriba y abajo, comercio en monoespaciada negrita centrado, fecha y categoría,
  punteado, filas `n× CONCEPTO ..... importe` con guía de puntos, subtítulo `n × precio` si cantidad > 1, punteado y
  `Total (con IVA)` grande a la derecha. Más cercano a un recibo que la tarjeta anterior. Completitud, badge y
  botones siguen fuera del papel.
- Revisión y detalle: un solo papel; los campos siguen siendo `OutlinedTextField` en caja (decisión de la spec), así
  que el formulario sigue pareciendo más un formulario que un recibo; la monoespaciada, el punteado entre filas y el
  total al final aportan el aire de ticket. La barra verde y las tarjetas anidadas han desaparecido.
- 360 dp y fontScale 1,3: sin recortes ni scroll horizontal. fontScale 2,0: sin recortes dentro del papel (cantidad
  y precio se apilan y el importe baja a su línea) pero el dock inferior (anterior a esta feature, "Descartar" /
  "Guardar" truncados y total recalculado en 5 líneas) ocupa media pantalla, y en el procesado el chip
  "Categorización" se deforma: ambos fuera del alcance y ya existían.
- En las capturas de prueba (compuestas con `setContent`) el badge solapa la barra de estado; en la app real el
  detalle aplica `statusBarsPadding`.

## Deuda anotada tras la validacion

- fontScale 2,0: el dock de edicion trunca "Descartar/Guardar" y el chip "Categorizacion" del procesado se deforma.
  Ya existian antes de esta feature; no se tocaron.
- La revision editable y el detalle siguen viendose como formulario (campos `OutlinedTextField` con caja). Si se quiere
  mas aspecto de recibo: campos solo con subrayado (feature aparte).
- `FilaRecibo` expone tres nodos de accesibilidad; mejora sugerida: `semantics(mergeDescendants = true)` con una
  descripcion tipo "2 Leche entera, 2,18 euros".

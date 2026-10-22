# Evidencia mejoras-visuales-3 (sesion 071)

Entorno: `emulator-5556` (Pixel_10_Pro, API 37), originales `wm size` 1280x2856, `wm density` 480, `font_scale` 1.0,
`user_rotation` 0, `accelerometer_rotation` 1; todo restaurado al terminar (rotacion no tocada). Configuraciones:
360 dp = `wm size 720x1560` + `wm density 320`; tablet = `2560x1600` @320 (emulador, no tablet fisica). Nada se
instalo en la Tab fisica y no se leyo ningun token.

Las imagenes las he mirado yo (agente); la revision visual humana sigue pendiente.

| Hallazgo | Antes | Despues | Lectura |
| --- | --- | --- | --- |
| W01 selector en dos filas | `antes-fs20-resumen.png` | `despues-fs20-resumen.png`, `despues-fs13-resumen.png`, `despues-fs20-gastos.png` | El segmento activo (Mes / Dia) lleva borde verde oscuro de 2 dp ademas del fondo. Sin test automatico (no es semantica). |
| W03 tarjeta de total | `antes-fs20-resumen.png` (Promedio en 3 lineas) | `despues-fs20-resumen.png` | A 2,0 las metricas van apiladas y caben en una linea. A 1,3 siguen en fila y caben (`antes-fs13-resumen.png` = `despues-fs13-resumen.png`): umbral 1,5 correcto. |
| W02 fallo de Procesado | `antes-fs20-fallo.png` (botones cortados, etiqueta pegada al borde) | `despues-fs20-fallo.png`, `despues-tablet-fallo.png` | Botones apilados completos, etiqueta centrada con margen; el cuerpo se desplaza (mensaje parcialmente bajo el pliegue, alcanzable con scroll). Tablet/fuente normal: diseno de siempre. |
| W06 vacio de Gastos | `antes-fs20-gastos-vacio*.png` | `despues-fs20-gastos-vacio*.png`, `despues-tablet-gastos-vacio.png` | Selectores y vacio en un unico scroll; en reposo la imagen es similar, la diferencia es que todo el contenido se desplaza (los botones solo se alcanzan desplazando, asertado por `FuentesGrandesFlujoTest`). |
| W08 permiso de camara | `antes-fs20-permiso-camara.png` | `despues-fs20-permiso-camara.png` | Botones de ancho completo y apilados; a 2,0 ya cabia sin scroll. |
| W04 Nuevo gasto | `antes-360-nuevo-gasto.png` ("Total recalculado 0,00", "Guardar cambios") | `despues-360-nuevo-gasto.png` | "Guardar gasto" y dock sin el bloque de total recalculado. |
| W11 boton Galeria | `antes-fs20-escanear.png`, `antes-fs13-escanear.png` | sin cambios | Etiqueta legible sobre la vista previa a 2,0: se deja intacto. |
| W10 chips de pago | no capturado | sin cambios | El emulador no tiene tickets con forma de pago; sin verificar visualmente, codigo intacto. |
| W07 dialogo de borrado manual | no capturado | n/a | Solo cubierto por `TextoConfirmarBorradoTest` (JVM). |
| W05, W09 | n/a | n/a | W05 (`selectableGroup`) sin efecto visual; W09 (`NoExiste`) sin captura. |

Estados sin acceso real (fallo de Procesado, vacios de Gastos) pintados con un test TEMPORAL que se elimino y el APK
de instrumentacion se reinstalo sin el. El "antes" de esos estados se tomo con el arbol sin cambios (`git stash`).

## Tests
- JVM: `assembleDebug testDebugUnitTest :domain:expenses:test` verde (485 tests, 0 fallos), incluido `TextoConfirmarBorradoTest`.
- Instrumentados (`am instrument`, sin *RealTest): clases nuevas OK (7); `ListadoGastosVacioTest`, `FiltroFormaPago*Test`,
  `ProcesadoFlujoTest`, `RevisionDatosFlujoTest` OK aisladas. Primera suite completa: 70 tests, 1 fallo
  (`GastoManualFlujoTest.altaDetalleEdicionYBorradoDeUnGastoManual`) por tickets basura de hoy en la BD del emulador
  (flaky de entorno, no regresion). Diagnosticado por el validator; tras borrarlos por la UI, suite completa 70 OK.
  Nota: la suite deja ~2 tickets vacios por ejecucion (preexistente); borrarlos por la UI antes de repetirla.

## No verificado
TalkBack, dialogo "Eliminar modelo", `captura_error` real, transiciones, motores reales, tablet fisica, camara real de
papel, antes de tablet, borrado de manual en pantalla.

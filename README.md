# Notas MCP

Bloc de notas Android con el aspecto de las notas del teléfono. La meta del proyecto es que una IA (Claude,
ChatGPT…) pueda **leer y escribir tus notas y tareas** por MCP, igual que Stone, Luma y Calculadora.

## Estado (0.5.0)

- Notas con título y texto, deshacer/rehacer, vistas de lista, tarjeta y cuadrícula, y búsqueda.
- Editor con formato (H1–H4, negrita, cursiva, subrayado, tachado, listas, casillas, tablas), fotos, reconocimiento de
  texto, dibujo, dictado, fondos, recordatorios, compartir y acceso directo en la pantalla de inicio.
- Carpetas propias, «Bloqueado» (con el bloqueo del teléfono) y «Recientemente eliminados» (30 días).
- Selección múltiple: fijar, bloquear, mover y eliminar.
- Tareas: añadir, marcar hechas, editar, eliminar y recordatorios.
- **Modo MCP** (Ajustes): tu IA busca, lee y (si le das permiso) escribe tus notas y tareas con herramientas
  `notes_*` y `tasks_*`; las bloqueadas nunca salen. Mismo relay y Gateway Hub que Stone, Luma y Calculadora.
- Widgets: Nota (4×2, 2×2), Notas (4×2) y Tareas (2×3).
- Tema global (Sistema, Claro, Oscuro y color de acento), aparte del fondo de cada nota.
- Internet solo para el Modo MCP (apagado de fábrica); sin anuncios ni copias en la nube.

APK: [`android/Notas-0.6.0.apk`](android/Notas-0.6.0.apk) (`com.zirakusa.notas`, firmado como Stone y Gateway Hub).

## Compilar y probar

```bash
cd android
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
```

## Siguiente

Ver [`ROADMAP.md`](ROADMAP.md): pulido del editor y Notas de IA.

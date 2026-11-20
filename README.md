# Notas MCP

Bloc de notas Android con el aspecto de las notas del teléfono. La meta del proyecto es que una IA (Claude,
ChatGPT…) pueda **leer y escribir tus notas y tareas** por MCP, igual que Stone, Luma y Calculadora.

## Estado (0.2.0)

- Notas con título y texto, deshacer/rehacer, vistas de lista, tarjeta y cuadrícula, y búsqueda.
- Editor con formato (H1–H4, negrita, cursiva, subrayado, tachado, listas, casillas, tablas), fotos, reconocimiento de
  texto, dibujo, dictado, fondos, recordatorios, compartir y acceso directo en la pantalla de inicio.
- Carpetas propias, «Bloqueado» (con el bloqueo del teléfono) y «Recientemente eliminados» (30 días).
- Selección múltiple: fijar, bloquear, mover y eliminar.
- Tareas: añadir, marcar hechas, editar, eliminar y recordatorios.
- Sin internet: nada sale del teléfono (solo notificaciones para los recordatorios).

APK: [`android/Notas-0.2.0.apk`](android/Notas-0.2.0.apk) (`com.zirakusa.notas`, firmado como Stone y Gateway Hub).

## Compilar y probar

```bash
cd android
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
```

## Siguiente

Ver [`ROADMAP.md`](ROADMAP.md): widgets (0.3.0) y Modo MCP con herramientas `notes_*` (0.4.0).

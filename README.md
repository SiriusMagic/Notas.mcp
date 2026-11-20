# Notas MCP

Bloc de notas Android con el aspecto de las notas del teléfono. La meta del proyecto es que una IA (Claude,
ChatGPT…) pueda **leer y escribir tus notas y tareas** por MCP, igual que Stone, Luma y Calculadora.

## Estado (0.1.0)

- Notas con título y texto, deshacer/rehacer, vistas de lista, tarjeta y cuadrícula, y búsqueda.
- Carpetas propias, «Bloqueado» (con el bloqueo del teléfono) y «Recientemente eliminados» (30 días).
- Selección múltiple: fijar, bloquear, mover y eliminar.
- Tareas: añadir, marcar hechas, editar y eliminar.
- Sin internet ni permisos; nada sale del teléfono.

APK: [`android/Notas-0.1.0.apk`](android/Notas-0.1.0.apk) (`com.zirakusa.notas`, firmado como Stone y Gateway Hub).

## Compilar y probar

```bash
cd android
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
```

## Siguiente

Ver [`ROADMAP.md`](ROADMAP.md): editor completo y recordatorios (0.2.0) y Modo MCP con herramientas `notes_*` (0.3.0).

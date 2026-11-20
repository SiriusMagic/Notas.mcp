# Arquitectura de Notas

Carpeta = package (`android/app/src/main/java/com/zirakusa/notas/`):

| Carpeta | Qué hay |
|---|---|
| `data/` | `Note`, `Folder`, `Task`, `Attachment`, `Section` y `Layout` (modelos); `TextEdits` (herramientas de texto puras: títulos, marcas, listas, casillas, tablas, Enter que sigue la lista; con pruebas); `NotesRules` (reglas puras: qué muestra cada carpeta, búsqueda sin acentos, título por la primera línea, fechas, papelera de 30 días; con pruebas); `NoteStore`/`SqlNoteStore` (SQLite local `notas.db` versión 2 con `onUpgrade` probado, siempre con parámetros; fotos y dibujos en `files/attachments`). |
| `ui/` | `NotesState` (único dueño del estado de notas: carpeta, búsqueda, vista, selección, editor, bloqueo), `TasksState` (tareas), `Editing` (la nota abierta: texto con cursor, leer/escribir, adjuntos), `NoteFormat` (muestra las marcas como formato sin mover el cursor), `EditorPlatform` (lo que el editor pide al teléfono), `UndoHistory` (deshacer/rehacer por palabras) y `NotesApp` (cajón, lista o editor, diálogos). |
| `ui/screens/` | `NotesScreen` (barra de arriba, título grande, barra de búsqueda), `NoteList` (lista, tarjeta, cuadrícula), `SelectionBar` (fijar, bloquear, mover, eliminar), `FolderDrawer`, `EditorScreen`, `TasksScreen`, `InputDialog` y `Chrome` (botones redondos, píldoras). Editor: `EditorScreen`, `EditorBars` (barras y paneles), `FormatPanel` (Aa), `Panels` (cámara, fondo, IA), `NoteMenu` (⋮), `DrawingPanel`, `Attachments` y `ReminderPicker`. Solo pintan y avisan. |
| `ui/theme/` | Colores (negro, gris oscuro y el amarillo de Stone) y `Skins` (los fondos de nota). |
| `Platform.kt` | Cámara, galería, reconocimiento de texto, dictado, compartir y acceso directo (implementa `EditorPlatform`). |
| `NoteFiles.kt` | Fotos al derecho y a 2048 px, dibujos en PNG, comprimir, y ML Kit para leer texto (en el teléfono). |
| `Reminders.kt` | `AlarmReminders` (una alarma por nota o tarea), `ReminderReceiver` (notificación) y `BootReceiver` (las repone al reiniciar). |
| `LockGate.kt` | Pide el bloqueo del teléfono (BiometricPrompt) antes de abrir «Bloqueado», como la Bóveda de Luma. |
| `MainActivity.kt` | Crea el almacén y el estado; al salir guarda la nota abierta y vuelve a bloquear. |

Reglas:
- Texto visible en `res/values/strings.xml` (español) y `values-en`.
- Archivos de pantalla de unas 150 líneas como mucho; si crece, se divide.
- Los diálogos con campo de texto usan `InputDialog` (ancho completo): el `AlertDialog` estándar con un campo no llega a
  quedar quieto en las pruebas de Robolectric.
- Cada cambio de tablas sube la versión de `notas.db` y añade su paso en `onUpgrade` con prueba (`MigrationTest`).
- Las notas son texto con marcas sencillas (`# `, `**`, `☐ `, `| |`…): así se buscan, se comparten y una IA las lee tal
  cual. El formato solo se dibuja (`NoteFormat`).
- El APK no tiene permiso de internet (se quita el que trae ML Kit) hasta que exista el Modo MCP.

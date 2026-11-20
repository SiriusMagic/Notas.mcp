# Arquitectura de Notas

Carpeta = package (`android/app/src/main/java/com/zirakusa/notas/`):

| Carpeta | Qué hay |
|---|---|
| `data/` | `Note`, `Folder`, `Task`, `Section` y `Layout` (modelos); `NotesRules` (reglas puras: qué muestra cada carpeta, búsqueda sin acentos, título por la primera línea, fechas, papelera de 30 días; con pruebas); `NoteStore`/`SqlNoteStore` (SQLite local `notas.db`, siempre con parámetros). |
| `ui/` | `NotesState` (único dueño del estado de notas: carpeta, búsqueda, vista, selección, editor, bloqueo), `TasksState` (tareas), `UndoHistory` (deshacer/rehacer por palabras) y `NotesApp` (cajón, lista o editor, diálogos). |
| `ui/screens/` | `NotesScreen` (barra de arriba, título grande, barra de búsqueda), `NoteList` (lista, tarjeta, cuadrícula), `SelectionBar` (fijar, bloquear, mover, eliminar), `FolderDrawer`, `EditorScreen`, `TasksScreen`, `InputDialog` y `Chrome` (botones redondos, píldora Notas/Tareas). Solo pintan y avisan. |
| `ui/theme/` | Colores: negro, gris oscuro y el amarillo de Stone. |
| `LockGate.kt` | Pide el bloqueo del teléfono (BiometricPrompt) antes de abrir «Bloqueado», como la Bóveda de Luma. |
| `MainActivity.kt` | Crea el almacén y el estado; al salir guarda la nota abierta y vuelve a bloquear. |

Reglas:
- Texto visible en `res/values/strings.xml` (español) y `values-en`.
- Archivos de pantalla de unas 150 líneas como mucho; si crece, se divide.
- Los diálogos con campo de texto usan `InputDialog` (ancho completo): el `AlertDialog` estándar con un campo no llega a
  quedar quieto en las pruebas de Robolectric.
- La base de datos aún no tiene migraciones (versión 1). Desde la primera versión publicada, cada cambio de tablas sube
  la versión y añade su `onUpgrade` con prueba.

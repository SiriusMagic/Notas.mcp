# Arquitectura de Notas

Carpeta = package (`android/app/src/main/java/com/zirakusa/notas/`):

| Carpeta | Qué hay |
|---|---|
| `data/` | `Note`, `Folder`, `Task`, `Attachment`, `Section` y `Layout` (modelos); `TextEdits` (herramientas de texto puras: títulos, marcas, listas, casillas, tablas, Enter que sigue la lista; con pruebas); `NotesRules` (reglas puras: qué muestra cada carpeta, búsqueda sin acentos, título por la primera línea, fechas, papelera de 30 días; con pruebas); `NoteStore`/`SqlNoteStore` (SQLite local `notas.db` versión 2 con `onUpgrade` probado, siempre con parámetros; fotos y dibujos en `files/attachments`). |
| `ui/` | `NotesState` (único dueño del estado de notas: carpeta, búsqueda, vista, selección, editor, bloqueo), `TasksState` (tareas), `Editing` (la nota abierta: texto con cursor, leer/escribir, adjuntos), `NoteFormat` (muestra las marcas como formato sin mover el cursor), `EditorPlatform` (lo que el editor pide al teléfono), `UndoHistory` (deshacer/rehacer por palabras) y `NotesApp` (cajón, lista o editor, diálogos). |
| `ui/screens/` | `NotesScreen` (barra de arriba, título grande, barra de búsqueda), `NoteList` (lista, tarjeta, cuadrícula), `SelectionBar` (fijar, bloquear, mover, eliminar), `FolderDrawer`, `EditorScreen`, `TasksScreen`, `InputDialog` y `Chrome` (botones redondos, píldoras). Editor: `EditorScreen`, `EditorBars` (barras y paneles), `FormatPanel` (Aa), `Panels` (cámara, fondo, IA), `NoteMenu` (⋮), `DrawingPanel`, `Attachments` y `ReminderPicker`. También `SettingsScreen` (Ajustes), `McpScreen` (Modo MCP) y `WidgetsScreen` («Añadir widget»). Solo pintan y avisan. |
| `ui/theme/` | `Theme` (tema global: `ThemeMode` Sistema/Claro/Oscuro, `Accent` y `NotesColors`, que todas las pantallas leen) y `Skins` (el «Fondo» de cada nota; el 0 sigue el tema). |
| `Platform.kt` | Cámara, galería, reconocimiento de texto, dictado, compartir y acceso directo (implementa `EditorPlatform`). |
| `NoteFiles.kt` | Fotos al derecho y a 2048 px, dibujos en PNG, comprimir, y ML Kit para leer texto (en el teléfono). |
| `Reminders.kt` | `AlarmReminders` (una alarma por nota o tarea), `ReminderReceiver` (notificación) y `BootReceiver` (las repone al reiniciar). |
| `mcp/` | Modo MCP, como Calculadora y Luma: `Tokens` (LLM/Hub/Redirect, cifrados con `SecretBox`), `NotesTools` (las herramientas `notes_*` y `tasks_*`; nunca ven notas bloqueadas ni la papelera), `McpHandler` (JSON-RPC, tokens, límites), `RelayLink` (relay de Stone), `McpMode` (estado e interruptores; `StoreData` avisa de cada cambio) y `HubLink` (enlace para Gateway Hub con firma). |
| `widget/` | `Widgets`: Nota 4×2 y 2×2, Notas 4×2 y Tareas 2×3 (RemoteViews; se redibujan con cada cambio). |
| `LockGate.kt` | Pide el bloqueo del teléfono (BiometricPrompt) antes de abrir «Bloqueado», como la Bóveda de Luma. |
| `Inbox.kt` | `InboxProvider` («add_note»): una app de la familia (los conteos de Lucía, en Gateway Hub) deja una nota **nueva** en la carpeta que diga (`data/Inbox`, puro, con prueba). Solo añade, nunca lee, cambia ni borra; solo apps con la misma firma (permiso `ACT` y comprobación de firma). |
| `Summary.kt` | `SummaryLines` (pura, con prueba) y `SummaryProvider`: la tarjeta de Notas en «Hoy» de Inicio (tareas pendientes y últimas notas; nunca bloqueadas ni papelera), solo para apps con la misma firma. |
| `MainActivity.kt` | Crea el almacén y el estado, arranca el Modo MCP, abre notas desde accesos directos, recordatorios y widgets; al salir guarda la nota abierta y vuelve a bloquear. |

Reglas:
- Texto visible en `res/values/strings.xml` (español) y `values-en`.
- Colores siempre de `NotesColors` (nunca fijos en una pantalla), para que el tema global llegue a todo. Encima del
  acento se usa `NotesColors.OnAccent`.
- Archivos de pantalla de unas 150 líneas como mucho; si crece, se divide.
- Los diálogos con campo de texto usan `InputDialog` (ancho completo): el `AlertDialog` estándar con un campo no llega a
  quedar quieto en las pruebas de Robolectric.
- Cada cambio de tablas sube la versión de `notas.db` y añade su paso en `onUpgrade` con prueba (`MigrationTest`).
- Las notas son texto con marcas sencillas (`# `, `**`, `☐ `, `| |`…): así se buscan, se comparten y una IA las lee tal
  cual. El formato solo se dibuja (`NoteFormat`).
- Internet solo lo usa el Modo MCP (apagado de fábrica), hacia el relay. ML Kit no envía estadísticas (backend «cct» quitado).

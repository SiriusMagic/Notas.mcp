# Notas 0.6.0

- **Conteos de Lucía**: cuando cuentas en voz alta con Lucía (Gateway Hub) y dices «termina», el conteo llega como una
  nota nueva en la carpeta **Conteos** (se crea sola), con una tabla «Punto | Cantidad» y el total.
- Solo apps firmadas por el mismo desarrollador pueden dejar notas así (`InboxProvider`, permiso
  `com.zirakusa.family.permission.ACT`), y solo pueden **añadir**: nunca leer, cambiar ni borrar tus notas.
- Si Notas está abierta, la lista se actualiza sola.

# Notas 0.5.0

- **Resumen para Inicio**: la tarjeta «Notas y tareas» de la pantalla «Hoy» de Inicio muestra las tareas pendientes
  («☐ Comprar pan») y las últimas notas (hasta 5); si no hay nada, «No tienes pendientes». Nunca incluye notas
  bloqueadas ni la papelera. Solo apps firmadas por el mismo desarrollador pueden leerlo (`SummaryProvider`).
- 38 pruebas.

# Notas 0.4.0

- **Ajustes › Tema**, para toda la app: **Sistema**, **Claro** u **Oscuro**, y **Color de acento** (amarillo, naranja,
  rosa, morado, azul o verde). Es independiente del «Fondo» de cada nota: una nota con fondo propio lo conserva; las
  notas «sin fondo» siguen el tema.
- Los iconos de la barra de estado se ven oscuros sobre pantallas claras (tema claro o nota con fondo claro).
- 37 pruebas.

# Notas 0.3.0

- **Ajustes** (abajo en el cajón) con el **Modo MCP**, igual que Stone, Luma y Calculadora: Activar MCP, Crear token
  (LLM o Hub, solo lectura o lectura y escritura, caducidad), pausar, revocar y **Redireccionar a Hub** («Ir al Hub»).
  También Política de privacidad y la versión.
- Tu IA usa tus notas: `notes_list`, `notes_search`, `notes_get`, `notes_folders` y `tasks_list`; con un token de
  escritura también `notes_create`, `notes_update` (o añadir al final), `notes_move`, `notes_pin`, `notes_delete` (a la
  papelera, 30 días), `folders_create`, `tasks_create`, `tasks_complete` y `tasks_delete`.
- Las notas **bloqueadas** y la papelera nunca salen por MCP (piden «no encontrado», igual que una nota que no existe).
- Llega por el mismo relay que Stone (`…/mcp?token=not_…`); en el hub aparece como `notes_`. Tokens cifrados en el
  Keystore del teléfono.
- **Widgets** («Más › Añadir widget» en el cajón): Nota 4×2 y 2×2, Notas 4×2 y Tareas 2×3 (marca una tarea con el
  círculo o añade una con +). Se actualizan solos cuando cambias algo, también si lo cambia la IA.
- **Comentarios** abre la página de sugerencias del proyecto en el navegador.
- Internet solo para el Modo MCP (apagado de fábrica).
- 35 pruebas.

# Notas 0.2.0

Editor completo, como el de las notas del teléfono:
- **Leer y escribir:** al abrir una nota se lee (Compartir, Fondo, ⋮); al tocar el texto se escribe (↶ ↷, ⋮, ✓). Fecha
  arriba del título.
- **Barra de íconos:** ✦ Notas de IA (llegará con el Modo MCP), **Aa** (H1–H4, Cuerpo, negrita, cursiva, subrayado,
  tachado, lista numerada, viñetas y casillas), **cámara** (Tomar foto, Seleccionar foto y **Reconocimiento de texto**
  con ML Kit dentro del teléfono), **dibujo** (lápiz, pluma, marcador, borrador, colores, deshacer), **casillas**
  redondas que se marcan al tocarlas, **tabla** y **dictado** por voz.
- Las listas siguen solas al pulsar Enter (y terminan con un Enter en una línea vacía).
- **Menú ⋮:** Fijar, Fondo, Bloquear, Compartir (texto y fotos), Agregar a pantalla principal, Recordatorio, Comprimir
  imágenes, Mover nota, Eliminar y «Creado en».
- **Fondos:** rosa, papel, rayado, cuadrícula, cuadrícula crema, puntos, niebla, cielo, noche y bosque; también en las
  tarjetas de la lista.
- **Recordatorios** de notas y de tareas («Añadir un recordatorio.»), con notificación; vuelven a ponerse si el teléfono
  se reinicia.
- Botón ✦ junto a la búsqueda.
- La base de datos pasa a la versión 2: las notas de la 0.1.0 se conservan.
- Sigue sin internet: el permiso que pide ML Kit se quita del APK. Solo 64 bits (ARM), como Luma.
- 28 pruebas.

# Notas 0.1.0

Primera base, con el aspecto de las notas del teléfono (negro, tarjetas grises, amarillo de Stone):
- **Notas:** título y texto, guardado al salir, deshacer/rehacer por palabras, contador de caracteres. Una nota vacía no
  se guarda; sin título, su primera línea hace de título.
- **Vistas:** lista, tarjeta y cuadrícula (se recuerda). Búsqueda sin mayúsculas ni acentos.
- **Carpetas:** Todas las notas, Sin categoría, carpetas propias (Nuevo; mantener pulsado = eliminar), Bloqueado y
  Recientemente eliminados (30 días, restaurar, eliminar definitivamente, vaciar).
- **Selección:** mantener pulsada una nota; fijar (van primero), bloquear, mover a carpeta y eliminar; seleccionar todo.
- **Bloqueado:** se abre con la huella, la cara o el PIN del teléfono y se cierra al salir de la app.
- **Tareas:** nueva tarea, marcar hecha (tachada, abajo), editar y eliminar.
- Sin permisos ni internet; las notas quedan fuera de las copias en la nube.
- 17 pruebas.

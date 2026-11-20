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

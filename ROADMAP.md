# Hoja de ruta

## 0.2.0: editor completo
- Barra del editor: casillas de verificación dentro de la nota, formato (Aa: negrita, títulos, listas), fotos de la
  cámara o la galería, dibujo a mano y tablas.
- Fijar y bloquear también desde el menú del editor; compartir una nota como texto.
- Tareas con **recordatorio** (hora y notificación) y «Ordenar por los más recientes / por hora de recordatorio».
- Pegar desde el portapapeles como nota nueva; widget de pantalla de inicio.

## 0.3.0: Modo MCP
- Como Stone, Luma y Calculadora: tokens (LLM/Hub) cifrados con `SecretBox`, relay de Stone, «Redireccionar a Hub» y
  enlace firmado para Gateway Hub (módulo `notes_`).
- Herramientas `notes_*`: buscar, leer, crear, editar, mover y eliminar notas; listar carpetas; `tasks_*` para tareas.
  Las notas bloqueadas nunca salen por MCP.
- «Notas de IA» (botón ✦): resumir una nota o un video de YouTube a través de la IA conectada.

## Después
- Copia de seguridad y restauración en un archivo (exportar / importar), sin nube.

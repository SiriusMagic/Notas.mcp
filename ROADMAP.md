# Hoja de ruta

## 0.3.0: widgets y pulido del editor
- Widgets de pantalla de inicio: una nota (4×2 y 2×2), lista de notas (4×2) y tareas con «+» (2×3).
- Tablas como cuadrícula editable (hoy son filas de texto `| a | b |`), alineación y sangría de párrafos, color de texto.
- Fotos dentro del texto (hoy van debajo) y fondos con ilustraciones.
- «Ordenar por los más recientes / por hora de recordatorio» en Tareas.
- Pegar desde el portapapeles como nota nueva.

## 0.4.0: Modo MCP
- Como Stone, Luma y Calculadora: tokens (LLM/Hub) cifrados con `SecretBox`, relay de Stone, «Redireccionar a Hub» y
  enlace firmado para Gateway Hub (módulo `notes_`).
- Herramientas `notes_*`: buscar, leer, crear, editar, mover y eliminar notas; listar carpetas; `tasks_*` para tareas.
  Las notas bloqueadas nunca salen por MCP.
- «Notas de IA» (botón ✦): resumir, ordenar o completar una nota, o un video de YouTube, con la IA conectada.

## Después
- Copia de seguridad y restauración en un archivo (exportar / importar), sin nube.

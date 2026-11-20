# Notas MCP: reglas para agentes

- Lee `ARQUITECTURA.md` y abre solo los archivos que necesites.
- Pruebas: `cd android && ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease` (debe pasar todo).
  Nunca desactives ni saltes una prueba. Lint sin avisos nuevos.
- Cada versión: sube `versionCode` y `versionName`, copia el APK a `android/Notas-<versión>.apk` (borra el anterior)
  y anótalo en `CHANGELOG.md`.
- Privacidad: las notas viven solo en el teléfono (`notas.db`, fuera de copias). Nada sale salvo por el Modo MCP que el
  usuario active (cuando exista). El APK no pide internet.
- Prohibido: SQL concatenado (siempre `?`), `eval` o motores de scripts.
- Firma: la de depuración del entorno, igual que Stone, Luma, Calculadora y Gateway Hub.

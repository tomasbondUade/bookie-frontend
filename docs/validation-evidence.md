# Evidencia de validación — issue #65

Actualizado: 2026-11-26. Rama `main` observada: `e73420c4304198edaae3e95e47d0b08aaf3397c8`.

Este registro distingue compilación automatizada de recorridos manuales. Un build verde no demuestra que el backend desplegado, la navegación de cada flujo ni la accesibilidad funcionen en un dispositivo.

## Validación automatizada integrada

Los PRs frontend [#124](https://github.com/tomasbondUade/bookie-frontend/pull/124) (paginación de búsqueda) y [#126](https://github.com/tomasbondUade/bookie-frontend/pull/126) (selectores de fecha y número) se integraron en `main` el 2026-11-26. Sus checks requeridos pasaron antes del merge: validación del Gradle Wrapper, build/test/lint, análisis Kotlin, CodeQL, revisión de dependencias, detección de secretos y compilación del framework iOS.

Estos checks no reemplazan las pruebas manuales descritas abajo.

## Ejecución local histórica en Linux

La siguiente ejecución se realizó el 2026-11-20 sobre `main` en `f18c7963440eb0423391138d0c3b5f4985e9d7c1`; no debe interpretarse como una nueva ejecución local sobre el HEAD actual.

Comando:

```bash
./gradlew :shared:allTests :androidApp:assembleDebug --offline --max-workers=2
```

Resultado: **PASS**. `:shared:testAndroidHostTest`, `:shared:allTests` y `:androidApp:assembleDebug` completaron correctamente. Compilaron `commonMain`, `androidMain` y `iosSimulatorArm64Main`; Gradle deshabilitó `iosSimulatorArm64Test` porque las pruebas de simulador requieren macOS. Se generó el APK debug.

También pasaron:

```bash
python3 -m unittest discover -s scripts/tests -p 'test_*.py' -v
python3 scripts/check_platform_readiness.py
./gradlew :androidApp:lintDebug --offline --max-workers=2
```

Resultado: **6/6** pruebas del preflight, **8/8** verificaciones estáticas de plataforma y Android Lint **PASS**.

La versión validada incluía navegación Atrás de Android. Ese comportamiento fue compilado, no probado manualmente en un teléfono durante esa ejecución.

## Pendiente de validación manual

| Verificación | Estado | Motivo |
| --- | --- | --- |
| Recorridos manuales en Android | NO EJECUTADO en esta evidencia | Requiere registrar el dispositivo, commit, entorno de API y resultado |
| Recorridos manuales en iOS | NO EJECUTADO | Se requiere macOS y Xcode |
| Accesibilidad TalkBack / VoiceOver | NO EJECUTADO | Requiere interactuar con dispositivos o simuladores |
| Integración completa contra el backend publicado | NO VERIFICADA aquí | Falta registrar una sesión reproducible con cuentas y datos de prueba |
| Fallback de búsqueda Open Library | INTEGRADO; producción no verificada aquí | Backend PR [#160](https://github.com/tomasbondUade/bookie-backend/pull/160) ya fue integrado; falta evidencia de una prueba en el entorno publicado |
| Restauración y cambio de cuenta de #63 | PENDIENTE | Frontend PR [#123](https://github.com/tomasbondUade/bookie-frontend/pull/123) ya se sincronizó con `main` en el commit `d7bba42`; la aprobación sigue vigente y los checks del nuevo commit están corriendo. Falta repetir el smoke test A → cerrar sesión → B, incluyendo Descubrir |

No hay evidencia para marcar como PASS los recorridos manuales de registro/cierre de sesión, aislamiento entre dos cuentas, escritura en API ni los demás escenarios de [la matriz](end-to-end-validation.md).

## Estado de issues

- #63 sigue abierto; su implementación está en el PR #123 y no debe cerrarse hasta completar su validación pendiente y mergear el PR.
- #65 sigue abierto: los recorridos manuales Android/iOS y la verificación de accesibilidad todavía necesitan resultados documentados.
- Los PRs #124 y #126 ya están en `main`; no quedan como trabajo pendiente de esos issues.

## Registro de resultados futuros

Por cada fila de la [matriz](end-to-end-validation.md), registrar:

`ID | fecha | target/OS | dispositivo | commit | API entorno | ejecutor | PASS/FAIL/BLOQUEADO | evidencia/issue`

Sanear capturas y logs. No guardar JWT, refresh tokens, contraseñas ni emails reales.

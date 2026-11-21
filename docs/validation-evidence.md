# Evidencia de validación — issue #65

Fecha: 2026-11-20. Rama integrada observada: `main` en `f18c7963440eb0423391138d0c3b5f4985e9d7c1`.

Este registro distingue compilación automatizada de recorridos manuales. Un build verde no demuestra que el backend desplegado, la navegación de cada flujo ni la accesibilidad funcionen en un dispositivo.

## Ejecutado en Linux

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

La versión incluye la navegación Atrás de Android: cierra primero detalles internos y vuelve desde pantallas secundarias; en Inicio conserva la acción del sistema. Este comportamiento está compilado, no probado manualmente en un teléfono en esta ejecución.

## No ejecutado

| Verificación | Estado | Motivo |
| --- | --- | --- |
| Recorridos manuales en Android | NO EJECUTADO | `adb` no está instalado/disponible en este entorno; no se adjuntó un resultado de dispositivo |
| Recorridos manuales en iOS | NO EJECUTADO | Se requiere macOS y Xcode |
| Accesibilidad TalkBack / VoiceOver | NO EJECUTADO | Requiere interactuar con dispositivos o simuladores |
| Integración contra backend real | NO EJECUTADO | No se verificó una sesión de staging con cuentas/datos de prueba |
| Fallback de búsqueda Open Library en producción | PENDIENTE | Backend PR [#160](https://github.com/tomasbondUade/bookie-backend/pull/160) requiere revisión, merge y despliegue |

No hay evidencia para marcar como PASS el registro/cierre de sesión, aislamiento de dos cuentas, escritura en API ni los demás recorridos manuales de [la matriz](end-to-end-validation.md).

## Alcance de issues

- El frontend PR #120 integra las pantallas principales y PR #121 agrega el detalle de publicaciones y el manejo de Atrás; ambos están en `main`.
- El issue #63, asignado a Martina, no se modificó ni se considera cerrado en esta validación.
- El issue #65 sigue necesitando resultados manuales Android y iOS conforme a su criterio de aceptación. Esta evidencia actualiza el estado del repositorio, pero no afirma paridad manual.

## Registro de resultados futuros

Por cada fila de la [matriz](end-to-end-validation.md), registrar:

`ID | fecha | target/OS | dispositivo | commit | API entorno | ejecutor | PASS/FAIL/BLOQUEADO | evidencia/issue`

Sanear capturas y logs. No guardar JWT, refresh tokens, contraseñas ni emails reales.

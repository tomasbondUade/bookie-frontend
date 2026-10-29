# Evidencia de validación — #65

Fecha de inspección: 2026-10-29. Fuente: GitHub Actions y archivos del repositorio, **no** ejecución manual de dispositivos.

## Verificado en GitHub (antes del PR #65)

| Rama / PR | CI Android/shared | CodeQL | Secret Scan | Alcance |
| --- | --- | --- | --- | --- |
| main | PASS | PASS | PASS | Baseline; no integra las funcionalidades de los PRs |
| #77 — progreso dominio | PASS | PASS | PASS | Pruebas de rama |
| #78 — modelos biblioteca | PASS | PASS | PASS | Pruebas de rama |
| #79 — caché SQLDelight | PASS | PASS | PASS | Pruebas de rama |
| #80 — repositorio Online First | PASS | PASS | PASS | Pruebas de rama |
| #81 — Flow SQLDelight | **FAIL** | PASS | PASS | Dos tests de emisiones temporizados; la corrección posterior está en #82 |
| #82 — caché reactiva integrada | PASS | PASS | PASS | Pruebas de rama |
| #83 — progreso/sesiones | PASS | PASS | PASS | Pruebas de rama |
| #84 — listas | PASS | PASS | PASS | Pruebas de rama |
| #85 — posts comunidad | PASS | PASS | PASS | Pruebas de rama |
| #86 — sagas | PASS | PASS | PASS | Pruebas de rama |
| #87 — auth | PASS | PASS | PASS | Pruebas de rama |
| #88 — biblioteca | PASS | PASS | PASS | Pruebas de rama |

El fallo de #81 se observó en [Actions run 40889971852](https://github.com/tomasbondUade/bookie-frontend/actions/runs/40889971852). La rama #82 pasó CI tras corregir la sincronización de las pruebas Flow: [Actions run 40902436791](https://github.com/tomasbondUade/bookie-frontend/actions/runs/40902436791). El PR #88 pasó [CI run 40906904865](https://github.com/tomasbondUade/bookie-frontend/actions/runs/40906904865).

**Importante:** los checks verdes se ejecutaron por separado, no sobre una rama integrada de todas las funcionalidades. No equivalen a recorridos end-to-end verificados.

## Hallazgos de inspección estática

1. **Android sin permiso INTERNET en main:** `androidApp/src/main/AndroidManifest.xml` no declaraba `android.permission.INTERNET`. Se agregó en la rama #65 y un preflight de CI impide regresiones. Pendiente: confirmar el nuevo CI y probar conectividad real.
2. **La aplicación arranca con UI de demostración:** `shared/src/commonMain/kotlin/org/bookie/app/App.kt` en main muestra botón `Click me!` y `Greeting`. Android `MainActivity` e iOS `MainViewController` llaman a ese `App()`. Es un **bloqueador de E2E**, no evidencia de que los casos de uso estén conectados. Corresponde coordinar con navegación/composición de los issues #4/#19/#20 y las pantallas asignadas al equipo.
3. **CI Linux no ejecuta iOS:** `.github/workflows/ci.yml` usa `ubuntu-latest`, compila commonMain, ejecuta `testAndroidHostTest`, lint y assembleDebug. No hay resultado de compilación ni ejecución en simulador iOS. Pendiente macOS/Xcode.
4. **PRs de features abiertos:** no hay prueba integrada de auth + biblioteca + listas + comunidades + sagas + retos en `main`. La paridad debe evaluarse tras el merge ordenado.

## Estado de ejecución manual

| Entorno | Estado | Motivo |
| --- | --- | --- |
| Android emulador/dispositivo | NO EJECUTADO | No se ejecutó app conectada a staging en esta revisión |
| iOS simulador/dispositivo | NO EJECUTADO | No se dispone de evidencia de ejecución macOS/Xcode |
| API staging real | NO EJECUTADO | Sin credenciales ni entorno de staging verificados |
| Accesibilidad con TalkBack/VoiceOver | NO EJECUTADO | Requiere pantallas y dispositivos/simuladores |

## Registro de resultados

Para cada fila de [la matriz](end-to-end-validation.md), completar:

`ID | fecha | target/OS | dispositivo | commit | API entorno | ejecutor | PASS/FAIL/BLOQUEADO | evidencia/issue`

No completar como PASS filas sin evidencia. No usar secretos ni datos privados en registros.

## Criterio de cierre

Se puede considerar listo el **trabajo preparatorio de validación** cuando el CI del PR #65 esté verde, la matriz sea reproducible y los bloqueadores estén identificados. **No cerrar como paridad Android/iOS validada** hasta ejecutar los recorridos en ambos targets con la app integrada y adjuntar evidencias.

# Validación integral Android/iOS — issue #65

## Alcance y reglas de evidencia

Esta matriz es un **plan reproducible**, no una certificación de pruebas ya ejecutadas. No marcar **PASS** sin dispositivo/simulador, versión/commit, pasos, resultado y evidencia. Una prueba de repositorio con MockEngine no demuestra navegación ni integración con el backend real.

**Estados:** PENDIENTE = no ejecutado; BLOQUEADO = falta integración/entorno; PASS = ejecutado y verificado; FAIL = reproducido y registrado en un issue.

**Entornos:** Android API 24+ con backend de staging; iOS 18.2+ en macOS/Xcode y simulador Apple Silicon. Usar cuentas de prueba A y B, credenciales de Supabase de staging, datos de prueba y API accesible. No usar secretos reales en logs ni screenshots.

## Matriz de recorridos

| ID | Flujo | Targets | Precondición | Pasos reproducibles | Resultado esperado | Estado inicial |
| --- | --- | --- | --- | --- | --- | --- |
| AUTH-01 | Registro y confirmación | Android / iOS | Cuenta nueva; staging disponible | Abrir registro, completar datos, enviar, confirmar email, volver a login | Verificación pendiente si corresponde; sin sesión falsa antes de confirmar | BLOQUEADO: UI #27 y composición |
| AUTH-02 | Inicio, restauración y cierre | Android / iOS | Cuenta A confirmada | Iniciar sesión, cerrar app, abrir, cerrar sesión, reiniciar | JWT seguro y renovable; logout borra credenciales y datos personales de A | BLOQUEADO: UI #27/#63 y composición |
| AUTH-03 | Sesión expirada / red caída | Android / iOS | Sesión de prueba; red desconectable | Caducar token, desconectar red, reintentar, reconectar | Estado de error claro; sin guardar cambios rechazados ni exponer tokens | BLOQUEADO |
| SEARCH-01 | Búsqueda | Android / iOS | API catálogo y UI conectadas | Buscar título existente, consulta sin resultados, búsqueda vacía y error de red | Resultados reales; loading, empty, error, retry; atrás preserva navegación | BLOQUEADO: navegación |
| LIB-01 | Biblioteca Online First | Android / iOS | Cuenta A con libros; caché disponible | Abrir biblioteca, filtrar estado/favoritos, paginar, desconectar, reabrir | Datos cacheados marcados como stale; no se confunden con servidor actualizado | BLOQUEADO: #32 e integración de PRs |
| LIB-02 | Cambiar estado/progreso | Android / iOS | Libro del catálogo; cuenta A | Agregar libro, cambiar estado, guardar progreso, provocar HTTP 403/500 | Guardado solo tras 2xx; error mantiene estado anterior; no hay cola offline | BLOQUEADO: #32/#64 |
| LIB-03 | Quitar libro personal | Android / iOS | Estado personal de cuenta A | Abrir detalle, solicitar eliminar, cancelar; repetir y confirmar | Se borra solo estado personal por ID; catálogo global intacto; confirmación visible | BLOQUEADO: #32/#64 |
| CACHE-01 | Aislamiento de cuentas | Android / iOS | Cuentas A y B con bibliotecas distintas | Abrir A, llenar caché, logout, entrar B sin red | B nunca ve libros/estadísticas privados de A | BLOQUEADO: auth y caché integrados |
| DISC-01 | Descubrimiento | Android / iOS | Datos de staging | Explorar por estado de ánimo y Book Tinder, registrar swipe | Respuestas de API reales; estados vacío/error; no mocks visibles | BLOQUEADO: #50/#51 |
| COMM-01 | Comunidades y publicaciones | Android / iOS | Comunidad de prueba; cuenta A | Explorar, abrir feed, crear post, reabrir, simular 401/403 | Post con autor del JWT, feed actualizado, error recuperable | BLOQUEADO: UI/composición #53–#56 |
| LIST-01 | Listas personalizadas | Android / iOS | Cuenta A y libro existente | Crear, editar, agregar, quitar libro, eliminar lista | CRUD real y datos aislados por usuario; no falsa confirmación | BLOQUEADO: UI #58 |
| CHAL-01 | Retos de lectura | Android / iOS | Reto de prueba | Abrir, participar, actualizar y consultar progreso | Progreso del servidor y estados de error correctos | BLOQUEADO: #41 |
| SERIES-01 | Sagas | Android / iOS | Saga con libros y progreso | Abrir saga, cambiar orden recomendado/publicación/cronológico, revisar mapa | Orden correcto, progreso personal consistente, estados loading/empty/error | BLOQUEADO: #61/#62 integración |
| PROFILE-01 | Perfil y estadísticas | Android / iOS | Cuenta A con sesiones | Abrir perfil, verificar estadísticas, cerrar sesión | Valores reales; sin datos de A después de logout | BLOQUEADO: #59/#63 |
| NAV-01 | Navegación y accesibilidad | Android / iOS | Pantallas integradas | Recorrer tabs, volver atrás, rotar/reanudar, activar lector de pantalla y fuente grande | Sin pantallas huérfanas; etiquetas/semantics, foco y contraste legibles | BLOQUEADO: #4/#19/#20/#21/#22 |

## Checklist transversal por cada fila

- [ ] Registrar dispositivo, OS, commit y URL del backend (sin secretos).
- [ ] Verificar loading, contenido, vacío, error y retry; sin datos de demostración en producción.
- [ ] Verificar permisos de red, navegación atrás, regreso desde background y cambios de cuenta.
- [ ] Confirmar que HTTP 4xx/5xx y desconexión **no** muestran éxito ni cambian optimistamente datos personales.
- [ ] Comprobar labels/semantics de controles, foco accesible, contraste, tamaño de texto y orden de lectura.
- [ ] Guardar captura/log saneado y resultado PASS/FAIL; abrir issue separado por cada fallo con pasos exactos.

## Comandos automatizados

En Linux CI / Android:

```bash
python3 -m unittest discover -s scripts/tests -p 'test_*.py' -v
python3 scripts/check_platform_readiness.py
./gradlew --no-daemon --stacktrace :shared:compileCommonMainKotlinMetadata :shared:testAndroidHostTest :androidApp:lintDebug :androidApp:assembleDebug
```

En macOS con Xcode instalado:

```bash
xcodebuild -version
xcrun simctl list devices available
./gradlew :shared:iosSimulatorArm64Test
xcodebuild -list -project iosApp/iosApp.xcodeproj
```

Abrir `iosApp/iosApp.xcodeproj`, elegir esquema `iosApp`, simulador iOS 18.2+ y ejecutar Build/Run. Repetir la matriz en el simulador. Si no hay Apple Silicon o runner macOS, reportar **NO EJECUTADO**: la compilación Android o el análisis estático **no** prueban iOS.

## Gestión de defectos

Por cada FAIL crear issue separado con: título, target/OS, commit, ambiente, precondiciones, pasos, esperado, obtenido, evidencia saneada, severidad y vínculo a la fila. No publicar JWT, emails reales, refresh tokens ni contraseñas. Si el fallo depende de otro PR, enlazarlo y no duplicar trabajo.

## Dependencias de integración

Las funcionalidades de Bookie se están desarrollando en PRs independientes. Antes de ejecutar E2E, integrar navegación (#4/#19/#20), auth (#8/#27/#28/#63), biblioteca (#7/#10/#32/#64), listas (#57/#58), comunidades (#53–#56), retos (#41) y sagas (#61/#62). Ver [evidencia](validation-evidence.md).

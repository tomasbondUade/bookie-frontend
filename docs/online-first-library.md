# Biblioteca Online First (#7)

## Flujo de lectura

`ObserveLibraryOnlineFirst` expone `Flow<LibraryLoad>` a Presentation sin Ktor
ni SQLDelight. En cada consulta se identifica primero la cuenta autenticada,
se lee la página local y se intenta actualizarla con `GET /library`:

1. `LibraryLoad.Cached(page)`: copia local disponible; **puede estar desactualizada**.
   Mostrar los libros inmediatamente y una indicación visible de datos guardados.
2. `LibraryLoad.Fresh(page)`: respuesta confirmada por la API; reemplaza la
   página solicitada en la caché y elimina la indicación de desactualización.
   Un error al escribir la caché no convierte una respuesta remota válida en error.
3. `LibraryLoad.Failed(message, cachedPage)`: no se pudo actualizar. Si existe
   `cachedPage`, conservar el listado y mostrar un aviso de conexión/actualización.
   Si es null, mostrar error o estado vacío con opción de reintentar.

El `LibraryExperienceViewModel` (#10) consume el caso de uso y convierte sus
emisiones a un `StateFlow` inmutable mediante `LibraryContentState`. La
pantalla Compose de #32 debe observar ese estado, sin conocer Ktor ni SQLDelight. **No** inferir frescura a partir de
`getLibrary(): Resource<LibraryPage>`: esa API de compatibilidad puede devolver
caché en un `Resource.Success` sin indicar si está desactualizada.

`ObserveCachedLibrary` y `ObserveCachedLibraryState` (#70) observan solo
cambios de SQLDelight, sin solicitudes HTTP. Son útiles para actualizar
automáticamente progreso y páginas mientras la app está abierta. La
observación de la caché **no** significa que exista conexión o sincronización.

## Persistencia, privacidad y cuentas

- Android e iOS usan el archivo persistente `bookie-cache.db` (#25).
  Los registros sobreviven al cierre de la app; no hay expiración por tiempo (TTL).
- Cada página y estado se almacena con la clave de cuenta autenticada.
  Nunca se consulta una página de otra cuenta. Si la cuenta cambia durante una
  solicitud, no se entrega el resultado de esa solicitud a la nueva cuenta.
- La integración de autenticación (#8) debe capturar el ID de la cuenta **antes**
  de cerrar sesión o cambiarla e invocar `ClearLibraryCacheOnLogout(accountId)`.
  Esto borra solo la cuenta anterior. Hasta conectar #8, no hay logout real
  al que enlazar este caso de uso.
- No se almacenan contraseñas, tokens de Supabase ni notas privadas en SQLDelight.
  `privateNotes` es null en el modelo reconstruido desde caché; las notas
  de sesiones permanecen exclusivamente en el servidor.
- Los filtros (estado/favoritos), página y tamaño forman parte de la clave de
  caché. Una consulta remota refresca la página pedida, no todas las páginas.

## Escrituras y conexión

`saveState` y `createReadingSession` envían cambios directamente al backend.
Una respuesta fallida **no** se presenta como guardada ni invalida la caché.
Después de una respuesta exitosa se invalida la caché de esa cuenta; la próxima
lectura obtiene los datos del servidor. El snapshot de progreso y una sesión
de lectura son operaciones separadas. La clave de idempotencia de la sesión
es responsabilidad de quien la crea; no se generan reintentos automáticos.

No hay cola offline, escrituras optimistas persistidas, reintentos automáticos,
sincronización bidireccional ni detección de red por plataforma. Se intenta
el refresh remoto y se interpreta el resultado; no se promete disponibilidad
de red. La fuente de verdad siempre es la API.

## Verificación

- `./gradlew :shared:testAndroidHostTest` prueba caché SQLDelight en SQLite
  y las pruebas compartidas del repositorio/casos de uso.
- `./gradlew :androidApp:assembleDebug` valida el build Android.
- iOS requiere macOS/Xcode: `./gradlew :shared:iosSimulatorArm64Test`.

El adaptador MVVM y los contratos de acciones de biblioteca están en #10.
La composición de la pantalla y navegación pertenecen a #32; la conexión
real con la sesión de Supabase depende de #8. Véase
[`library-experience-integration.md`](library-experience-integration.md).

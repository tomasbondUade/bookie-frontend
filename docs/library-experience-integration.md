# Integración de biblioteca Online First (#10)

## Alcance y dependencias

Este issue integra los casos de uso y el contrato de presentación de biblioteca
sobre el repositorio Online First de #7/#26. El diseño visual de la pantalla,
la composición de dependencias y la navegación se coordinan con #32; no se
duplican pantallas ni se conectan llamadas HTTP desde Compose.

La fuente de verdad es el backend autenticado:
- `GET /library?page=1&limit=20&status=READING&filter=fav` para consultas.
- `POST /library/state` para agregar un libro existente del catálogo o
  actualizar estado/progreso, con solo los campos provistos por el usuario.
- `DELETE /library/state/:id` para quitar un **estado personal**. `:id` es
  `LibraryBookState.id`, nunca `Book.id`; el catálogo global no se elimina.

## Conexión desde #32

Crear una instancia de `OnlineFirstLibraryRepository` usando el cliente HTTP
con JWT, la caché persistente de plataforma y el `AccountIdProvider` de
autenticación. La misma instancia implementa `LibraryRepository` y
`LibraryFeedRepository`:

```kotlin
val repository = OnlineFirstLibraryRepository(remote, cache, accountIdProvider)
val viewModel = LibraryExperienceViewModel(
    observeLibrary = ObserveLibraryOnlineFirst(repository),
    saveState = SaveLibraryState(repository),
    deleteState = DeleteLibraryState(repository),
)
```

En la pantalla Compose, observar `viewModel.uiState: StateFlow<LibraryExperienceUiState>`.
El estado es inmutable y está separado en `content`, `action`,
`statusFilter`, `favoritesOnly` y `pendingRemoval`:

- `LibraryContentState.Loading`: mostrar indicador de carga y, si existe
  `previous`, la última página con advertencia de datos desactualizados.
- `Ready(snapshot)`: listar `entries`; si `stale=true`, marcar la caché
  como potencialmente desactualizada. `warning` comunica errores de red
  sin ocultar los libros. `canLoadMore` y `isLoadingMore` controlan el
  botón de siguiente página.
- `Empty`: estado vacío real; también puede estar desactualizado.
- `Error`: sin datos disponibles y sin conexión; mostrar `retry()`.
- `LibraryActionState.Working`: bloquear acciones duplicadas;
  `Succeeded` solo se emite tras respuesta exitosa del backend;
  `Failed` conserva el contenido anterior.

Acciones disponibles: `refresh()`, `retry()`, `loadNextPage()`,
`setStatusFilter()`, `setFavoritesOnly()`, `addCatalogBook()`,
`changeStatus()`, `updateProgress()`, `save()`,
`requestRemoval(stateId)`, `cancelRemoval()`, `confirmRemoval()`
y `dismissAction()`. La eliminación requiere confirmación explícita en UI.

## Consistencia y privacidad

El ViewModel no modifica la lista de manera optimista. Tras un POST o DELETE
confirmado, el repositorio invalida la caché de la cuenta y el ViewModel
vuelve a cargar la primera página. Un error de red mantiene la última caché
disponible y no genera escrituras pendientes. La cuenta se obtiene desde
autenticación; nunca se envía un `userId` del cliente.

Los filtros por `TO_READ`, `READING`, `READ` y favoritos se aplican en
el backend y forman parte de la clave de caché. Cada cambio de filtro
reinicia la paginación. El ViewModel deduplica estados por ID al combinar
páginas y conserva una marca de frescura por página.

## Verificación

```bash
./gradlew :shared:testAndroidHostTest
./gradlew :androidApp:assembleDebug
```

Las pruebas usan fakes de repositorios y Ktor MockEngine: cubren
caché/servidor, estados vacíos, filtros, paginación, fallas de red,
confirmación de cambios, rechazo de escrituras y eliminación por ID personal.
La compilación y pruebas iOS requieren macOS/Xcode y se validan por separado.

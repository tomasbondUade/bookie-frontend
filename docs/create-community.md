# Crear comunidad (#54)

## Contrato real
`POST /communities` está protegido con `JwtAuthGuard`. El backend obtiene
el propietario del token mediante `@CurrentUser()`. DTO de entrada:
`name` y `description` obligatorios; `tags` opcional. El formulario
solicita solamente los campos obligatorios, sin enviar `userId`,
`ownerId`, token ni otros datos de la cuenta en el cuerpo.

La respuesta exitosa es la comunidad creada por el servidor. La pantalla
solo la selecciona tras recibir `Resource.Success`; muestra sus
publicaciones mediante el flujo de lectura público existente del #53.
En caso de error 400, 401, 409, red, etc., conserva el formulario y
expone un mensaje. Sin sesión nunca envía el POST.

## Arquitectura
`CommunityScreen -> CommunityViewModel -> CreateCommunity ->
CommunityRepository -> CommunityRepositoryImpl ->
KtorCommunityRemoteDataSource -> POST /communities`.

Reutiliza `CommunityDto`, `Community`, `AuthRepository`, los
componentes Bookie y la configuración compartida de cliente HTTP.
No modifica `MainActivity.kt`, login ni código de publicación de
otros integrantes.

## Validación
`./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug`
Cubre payload, JWT inyectado por cliente, errores HTTP, validaciones
y estado de confirmación del ViewModel.

Prueba manual disponible ahora: abrir Comunidad > Crear comunidad y
comprobar validación de campos y aviso de inicio de sesión.
La creación autenticada real requiere integrar #27 en el composition root
y proporcionar `createAuthenticatedCommunityViewModel(bundle)`.
No se puede declarar la creación en el servidor probada con un usuario
real hasta disponer de ese flujo.

Stack de ramas: #50 -> #52 -> #53 -> #54; si los PR anteriores siguen
abiertos, crear el PR del #54 contra
`feature/53-community-exploration`.

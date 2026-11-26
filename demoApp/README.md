# Bookie Demo sin conexión

APK independiente para recorrer las pantallas del rediseño con datos ficticios. Usa `App`, los cinco destinos de navegación, los formularios, los ViewModels y los casos de uso de producción; inyecta repositorios locales en lugar de clientes HTTP o Supabase.

- Nombre en el celular: **Bookie Demo**.
- Identificador: `org.bookie.app.demo`; puede convivir con `org.bookie.app`.
- Cuenta inicial: `demo@bookie.app` / `bookie123`.
- El módulo no solicita permiso de Internet y no envía datos ni correos.
- Los datos son temporales. Se conservan al navegar y rotar el celular; cerrar completamente la app, finalizar su proceso o usar **Reiniciar** carga el conjunto inicial.
- **Acceso** muestra las instrucciones y la cuenta de prueba en cualquier pantalla.
- Las portadas sin imagen usan el marcador visual existente. Los enlaces externos de compra y vista previa no se simulan.

## Recorrido sugerido

1. Abrir la app, ver la animación inicial y entrar a Biblioteca o Perfil para abrir el acceso. Iniciar sesión con la cuenta de prueba. Una contraseña incorrecta permite revisar el estado de error.
2. En Biblioteca, alternar estados y favoritos, buscar un título, editar un libro y guardar cambios. Abrir **Registrar progreso**, cambiar página/porcentaje y guardar. Registrar una sesión y revisar su historial: esta operación es independiente de guardar el progreso.
3. En Descubrir, buscar `Dune`, `fantasia` o `todos`. `todos` habilita el recorrido de paginación de los 12 libros. Abrir una ficha, puntuar, guardar y leer/ordenar las reseñas. Las notas privadas no aparecen en las reseñas.
4. En **Para vos**, probar Me gusta y No me interesa; la siguiente selección excluye los libros ya elegidos. Estas elecciones no agregan por sí solas un libro a Biblioteca. En **Por ánimo**, probar `aventura`, `espacio`, `misterio` o `romance` y guardar un resultado.
5. En Biblioteca → Mis listas, crear una lista, cambiar su nombre/visibilidad, buscar y agregar libros, quitar uno y eliminar la lista con confirmación.
6. En Comunidad, abrir una comunidad y una publicación, comentar y responder. Crear una comunidad y publicar dentro de ella. Consultar compatibilidad con otro lector: el porcentaje es un ejemplo ficticio, no un cálculo del servicio real.
7. En Perfil, consultar las estadísticas derivadas de la biblioteca, editar preferencias y cerrar sesión. Registrar otra cuenta local para probar el formulario y una biblioteca inicialmente vacía. Las cuentas locales tienen biblioteca, listas, preferencias y progreso separados; las publicaciones se comparten dentro de la demo.
8. Desde Inicio, abrir Sagas y universos y recorrer el mapa y los órdenes de lectura. **Abrir libro** muestra la ficha real del catálogo en un diálogo de la demo. Abrir Retos, participar y marcar libros como leídos para actualizar el progreso al refrescar.
9. Usar Reiniciar para descartar los cambios y repetir el recorrido desde el inicio.

No representa pruebas contra el backend ni contra Supabase. El registro local inicia sesión directamente y no reproduce la entrega de un correo de verificación.

## Compilación y validación

El módulo queda excluido por defecto del proyecto y del APK de producción. Para habilitarlo:

```sh
./gradlew -PbookieDemo=true :demoApp:assembleDebug :demoApp:testDebugUnitTest :demoApp:lintDebug
```

Salida: `demoApp/build/outputs/apk/debug/demoApp-debug.apk`.

Las pruebas comprueban acceso/registro local, aislamiento de cuentas, búsqueda/paginación, filtros y cambios de biblioteca, idempotencia de sesiones, listas, publicaciones y respuestas, preferencias/reseñas, retos y sagas. También verifican que el acceso conserve la ficha de catálogo o de ánimo que se estaba consultando.

Se instaló y abrió la demo en el emulador Android 37.1. Se revisaron el inicio, el formulario con teclado, el error de credenciales y el ingreso correcto a la biblioteca con sus libros iniciales. El emulador mostró su aviso de System UI; se descartó para continuar la revisión y se restauró su configuración al finalizar. No se modificó la pantalla de bloqueo de la app.

La demo no incorpora las Activities temporales de capturas, no modifica la pantalla de bloqueo ni requiere ajustes especiales del dispositivo. Los cambios de este módulo están en una rama separada del PR del rediseño.

# Issue #63 — Cuenta, sesión y cierre de sesión

## Objetivo y alcance
Reutilizar el cliente Supabase del issue #8 y el `AuthRepository` común para
mostrar los datos reales de la cuenta y gestionar su ciclo de vida.

- `App()` usa una única configuración de cuenta por aplicación cuando no recibe
  `AppFeatureViewModels.auth` inyectado. No se modifica `MainActivity`.
- Se restaura la sesión una vez por `AuthViewModel` y se protegen las pantallas
  para que un usuario desconectado no vea contenidos de otra cuenta.
- Perfil presenta únicamente el usuario y correo guardados con la sesión.
- Preferencias/Estadísticas usan los ViewModels provistos por la raíz de composición
  del equipo. No se crea una API de perfil ni un nuevo repositorio de estadísticas.
- La operación de cierre primero invalida SQLDelight para el ID anterior,
  después llama a `SupabaseAuthRepository.signOut()`. Si limpiar caché falla,
  conserva la sesión y permite reintentar.
- SupabaseAuthRepository ya permite cierre de sesión local si hay un corte de red.
- Android toma los valores de `bookie_supabase_url` y
  `bookie_supabase_publishable_key` de recursos existentes; iOS usa Info.plist.
  No se agrega una IP de desarrollador ni se guardan claves privadas.

## Pruebas manuales
1. Iniciar sesión y abrir Mi cuenta: nombre de usuario y correo reales.
2. Cerrar sesión y comprobar regreso al formulario; reabrir la app y comprobar
   que no se restaura la sesión anterior.
3. Iniciar sesión con otra cuenta y confirmar que no ve libros cacheados
   de la anterior.
4. Reiniciar el proceso de la app mientras se está autenticado y verificar
   restauración de sesión.
5. Desconectar NestJS y cerrar sesión: Supabase debe seguir pudiendo cerrar la
   sesión local; los datos de la cuenta anterior no deben mostrarse.

Nota: Si la configuración de Supabase no está presente en un target, la app
mantiene las pantallas públicas ya existentes y no inventa credenciales.

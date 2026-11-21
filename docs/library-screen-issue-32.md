# #32 — Biblioteca Online First

## What is implemented
- Compose `LibraryScreen` on the existing `LibraryExperienceViewModel`.
- Server-side status and favorite filters, paginated list, loading/empty/error states.
- Visible stale/cached and network warning states; retry without hiding cached books.
- Save status, favorite toggle and reading progress through existing ViewModel methods.
- Loading/saving feedback; no UI-side optimistic confirmation.
- "Explorar libros" routes to the existing Search tab.
- New `createAuthenticatedLibraryViewModel(authClients, cache)` composition
  reuses `OnlineFirstLibraryRepository`, the real authenticated client,
  `AccountIdProvider`, and platform-created SQLDelight `LibraryCache`.
- The app root can provide this ViewModel via `AppFeatureViewModels.library`.
  Without an authenticated composition root, Library shows a meaningful sign-in
  requirement instead of showing fabricated book content.

## Integration boundary
Android and iOS already have `createAndroidLibraryCache(context)` and
`createIosLibraryCache()` respectively. The platform root must call the
factory with its existing `AuthClientBundle` and platform cache, and own
lifecycle/clear the old account cache on sign-out (#63).
`MainActivity.kt` has not been modified. Currently it calls `App()` with no
feature ViewModels, so authenticated data requires an application-root integration
before real on-device library reads can be validated. No hardcoded IP, JWT, user ID,
or parallel auth stack has been introduced.

The endpoint to add/search books is elsewhere in the app: there is no free text
field here for arbitrary `bookId`, and the UI only changes books already
present in the authenticated library. Removal is not exposed as specified in #32.

## Verification
Run `./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug`.
The underlying ViewModel tests cover cached/fresh pages, filters, pagination,
failed writes, successful server-acknowledged writes and progress validation.
Manual authenticated verification requires the existing login #27 to be
connected through the app composition root.

## Android debug, actual session setup

The app now automatically connects the library tab to the existing Supabase
AuthScreen / AuthViewModel / AuthClientBundle and account-scoped SQLDelight cache
when the following strings are configured:

- `bookie_api_base_url` — existing debug resource already has `http://127.0.0.1:3000`.
- `bookie_supabase_url` — the public URL of your actual Supabase project.
- `bookie_supabase_publishable_key` — the **public** Supabase publishable/anon key;
  never use a secret/service-role key.

Fill the last two resource values locally in
`androidApp/src/main/res/values/bookie_api_config.xml` (no secrets), or
provide those values from the team's Android resource build configuration.
Do **not commit personalized environment configuration**. Once both are
available, open Biblioteca: the existing auth form restores or creates a
Supabase session; after success the user-specific Online First repository
starts loading. Reconnect `adb reverse tcp:3000 tcp:3000` after reconnecting USB.
To test offline caching, first open an authenticated library with internet
and successful API data, then disable connection to the backend and refresh;
the UI marks saved snapshots as stale.

The library tab uses this integration on the current Android app even though
`MainActivity` calls `App()`. Existing public search, reviews and communities
remain available. In a fully injected app composition root, provide
`AppFeatureViewModels.library` to use the same UI. The new library-only auth
entry is a transitional wiring of the **existing** repository, not a
separate login implementation.

iOS will use the same structure when the host provides
`BOOKIE_SUPABASE_URL` and `BOOKIE_SUPABASE_PUBLISHABLE_KEY` in Info.plist.

Logout / clearing old account caches, and making the entire app share one
login screen, belong to #63. Avoid switching accounts until #63 is integrated.

## Safe local Supabase setup (#32)

From the **frontend root**, run:

```bash
bash scripts/configure-local-supabase-android.sh /absolute/path/to/bookie-backend
```

The script reads the **local API URL and ANON_KEY** from `npx supabase status -o json`.
It creates `androidApp/src/debug/res/values/bookie_supabase_local.xml`,
which is **ignored by Git**. Never paste the complete status output publicly
(it may contain service role and other secrets). The shared
`SupabaseAuthConfig` allows `http://127.0.0.1` or `http://localhost`
**only** when explicitly enabled by the Android debuggable build. Remote
HTTP is prohibited, and release builds still require HTTPS.

Connect an Android phone over USB and run both reverse mappings:

```bash
~/Library/Android/sdk/platform-tools/adb reverse tcp:3000 tcp:3000
~/Library/Android/sdk/platform-tools/adb reverse tcp:54321 tcp:54321
```

The backend `.env` must point to **the same** local Supabase
(`SUPABASE_URL=http://127.0.0.1:54321`). Start both Supabase and NestJS
to test real login + user-specific library. Install the debug APK again
after creating the local Android resource. The resource is public-client
config (not a service_role key) but should still remain local.

## Shared session for search-to-library writes (#32)

The default `App()` path uses its existing `ConfiguredLibraryExperience`
to create **both** `LibraryExperienceViewModel` and
`createAuthenticatedMoodSearchViewModel(authClients)`. These share one
`AuthClientBundle`, `SupabaseAuthRepository` and JWT-aware
`bookieClient`. Public Search remains usable even while signed out,
but `Guardar en mi biblioteca` returns `SignInRequired` unless the same
repository's `AuthState` is `SignedIn`.

After logging into Biblioteca, opening Buscar, selecting a book and tapping
Guardar sends `POST /library/state` with the existing JWT. Only the backend's
successful response displays `Libro guardado en tu biblioteca`. Returning to
Biblioteca triggers the existing `refresh()`, so the new book is reloaded
Online First from cache and server.

The standalone public search fallback is only used when the Supabase local
configuration is absent; `AppFeatureViewModels.moodSearch` still has priority
for fully injected app configurations. No MainActivity or token handling changes.

Manual verification: sign in via Biblioteca -> Buscar -> choose a book ->
Guardar -> Biblioteca -> see book -> airplane mode / stop backend ->
Actualizar -> previous cache is shown with stale warning. Test the reverse
ADB ports 3000 and 54321 when using a local server.

Session cleanup/account switching still belongs to #63; do not claim those
flows are covered here.

## State-driven progress editing (#32)

The screen now stages status/progress edits as immutable `LibraryEditDraft`
records in the existing `LibraryExperienceUiState.drafts` map (StateFlow
owned by the ViewModel). Compose displays draft state and dispatches events.
Selecting a status does not call the backend; "Guardar cambios" performs
one authenticated server write for status and optional progress fields.

- TO_READ (Pendiente): show recorded progress but do not allow editing it.
- READING (Leyendo): optional current page and percentage (0–100), both
  staged before the same server-confirmed save.
- READ (Terminado): recorded progress is read-only.
- "Cancelar" discards the draft; server data remains unchanged.
- On failed save, the draft remains available for retry.
- The backend itself does not currently enforce state-based edit restrictions;
  these are UI and ViewModel rules for issue #32.

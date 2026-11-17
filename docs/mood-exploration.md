# Mood discovery (#50)

## Backend contract checked
`GET /books/mood?q=<free text>` is public. The NestJS `MoodSearchQueryDto`
requires a non-empty string `q`. The service returns a JSON **array** of
catalog books with an additional numeric `relevance`. The catalog fields
are the same fields represented by the existing `BookDto`; `relevance` is
optional so the ordinary `/books/search` endpoint keeps working.
The repository does not fetch this endpoint via an authentication guard.
The endpoint may return an empty array when the catalog contains no matching entries.

The controller does not declare a dedicated response DTO in Swagger; the
service implementation and backend `test/mood-search.e2e-spec.ts` were used
as the response contract. Document the full response schema in OpenAPI later.

## Architecture
`MoodSearchScreen -> MoodSearchViewModel -> SearchBooksByMood ->
MoodBookRepository -> MoodBookRepositoryImpl -> KtorMoodBookRemoteDataSource`.
Only the data source knows HTTP. The repository maps to existing `Book`.
`BookieBookCard`, `BookDetail` and shared loading/empty/error components are reused.

Saving a book calls the existing `SaveLibraryState` use case with the
backend catalog `book.id` and status `TO_READ`. A success state is shown
**only** after the server returns success. The library endpoint requires
a valid Supabase JWT; public mood discovery does not.

## Make the view reachable from App() without modifying MainActivity
The current Android and iOS entry points use `App()` with no injected feature
ViewModels. The standalone discovery fallback therefore uses the existing
`createPlatformHttpClient` and reads the public backend URL from a platform
resource. Without this URL, the Search tab shows an explicit configuration
message instead of a fake search.

- Android **debug**: the default `bookie_api_base_url` is
  `http://127.0.0.1:3000` in `androidApp/src/debug/res/values/bookie_api_config.xml`.
  Connect the device by USB and execute `adb reverse tcp:3000 tcp:3000` so
  each developer's own local backend can be reached without an individual IP.
  This works with Android Studio's emulator too; `http://10.0.2.2:3000` is
  an alternative emulator-only host mapping.
- Android **release/main**: the value is intentionally blank until the real
  deployed HTTPS address is known. Do not commit development HTTP URLs or
  cleartext permissions to the main manifest; the debug overlay is isolated.
- The backend must run with PostgreSQL+pgvector and books with embeddings.
  Verify it locally with `curl -G http://localhost:3000/books/mood
  --data-urlencode "q=Una aventura"` before running the app.
- iOS: set `BOOKIE_API_BASE_URL` in `iosApp/iosApp/Info.plist`.
- Use the **reachable API URL**, not a UI/mock hostname. Do not put API host,
  tokens, or secrets in `MainActivity` or DTOs.
- The authenticated composition root can supply
  `AppFeatureViewModels.moodSearch = createAuthenticatedMoodSearchViewModel(authClients)`
  using the **existing** `AuthClientBundle` from issue #8.
  That bundle provides the existing `SupabaseAuthRepository` and the JWT-aware
  `bookieClient` from #34. Its owner is responsible for closing the bundle.
- MoodSearchViewModel checks the existing `AuthRepository.state`: only
  `AuthState.SignedIn` can attempt `SaveLibraryState`. All other states
  display "Iniciá sesión" and **do not send POST /library/state**.
  Even for a signed-in session, only a confirmed 2xx response shows "guardado".
  An HTTP 401 returns to the sign-in-required state.
- The standalone `App()` fallback intentionally remains public-only. There
  is currently **no login UI/composition root wired into platform entry points**;
  #27 is owned by another teammate. Until #27 supplies a session, attempting
  to save displays the sign-in requirement instead of sending an anonymous POST.
- No changes to existing auth, token storage, Ktor client, `MainActivity.kt`,
  or the issue #27 login UI are included in #50. Credentials and JWTs are
  never put into UI state, resources, source control, or logs.

## Verification
`./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug`

The feature author confirmed `BUILD SUCCESSFUL` and that mood discovery
returned real books on an Android phone connected to the local backend via USB.
The new auth-guard change needs the same Gradle tests and Android build check.
New MoodSearchViewModel tests cover signed-out save blocking, confirmed
server success, and failed/unauthorized saves.

On a device: enter a free-form sentence, check real results, select a book,
return to results, check blank/empty/error/retry. With a signed-out or public-only session, tap "Guardar en mi biblioteca":
it must show a sign-in message and not send a request. After the #27 login
integration, verify authenticated saving against a backend that checks
Supabase JWTs; this device flow is **not** claimed as verified today.

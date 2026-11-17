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

- Android: set `bookie_api_base_url` in
  `androidApp/src/main/res/values/bookie_api_config.xml`.
- iOS: set `BOOKIE_API_BASE_URL` in `iosApp/iosApp/Info.plist`.
- Use the **reachable API URL**, not a UI/mock hostname. Do not put API host,
  tokens, or secrets in `MainActivity` or DTOs.
- If a real app composition root already supplies an authenticated
  `AppFeatureViewModels.moodSearch`, it is used instead of this fallback,
  so the same shared authenticated Ktor client can perform the library save.
- The standalone fallback is unauthenticated: searching can work, but saving
  will receive 401 until auth is wired at the app composition root. Do not
  mark save-to-library device testing as passed without logging in.

## Verification (not yet executed against a live backend)
`./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug`

On a device: enter a free-form sentence, check real results, select a book,
return to results, check blank/empty/error/retry. Verify saving with an
authenticated composition root and observe server confirmation. Backend
must be reachable from the device; this repository cannot supply that URL.

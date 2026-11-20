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

# Community discovery (#53)

## Backend contract
Confirmed against `CommunitiesController`, `CommunitiesService`,
`PostsController`, `PostsService`, Prisma schema and the pagination DTO.

- `GET /communities?page=1&limit=20`: PUBLIC. Returns
  `{data:[{id,name,description,tags,imageUrl,ownerId,createdAt}],meta:{page,limit,total,totalPages}}`.
  Defaults 1/20; page >= 1, limit 1..50.
- `GET /communities/recommended`: PROTECTED with JWT. Returns a JSON ARRAY
  of public communities with `score`, `matchedTags` and `reason`.
  Without #27 integrated, public-only App() shows "Iniciá sesión" rather
  than sending an unauthenticated GET.
- `GET /communities/:id/posts?limit=20`: PUBLIC. Returns a JSON ARRAY
  of posts with author info. It has no page parameter (only limit).
  The UI displays the available posts, without inventing pagination.

## Architecture
`CommunityScreen -> CommunityViewModel ->
GetCommunities/GetRecommendedCommunities/GetCommunityPosts ->
CommunityRepository -> CommunityRepositoryImpl ->
KtorCommunityRemoteDataSource`.

A public fallback is configured from platform resources in `App()`.
Once an authenticated app root exists, it can supply
`createAuthenticatedCommunityViewModel(bundle)` through
`AppFeatureViewModels.community`, reusing the existing secure
`AuthClientBundle` from #8. The app root owns/cleans up that bundle.

No changes to #8 auth, #27 login UI, or MainActivity.
No fake user, JWT, mock communities or inferred join endpoints.
The backend exposes no community search endpoint and the posts endpoint
does not support pagination.

## Verification
Run `./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug`.
Run the backend locally and `adb reverse tcp:3000 tcp:3000` with the
phone connected by USB. Open the Community tab; verify actual community
list, details/posts, empty/error/retry, and "Ver más comunidades" if the
backend returns multiple pages. Tap "Ver recomendaciones para mí" while
signed out: it should ask for sign-in without sending a request.
Testing recommendations with a real JWT depends on #27.

The #53 branch is stacked on #52 (itself stacked on #50); create its
PR against `feature/52-book-reviews` while both earlier PRs remain open.

# Book reviews (#52)

## Verified backend contract
The existing public NestJS endpoint is `GET /books/:id/reviews`.
The path requires the **internal Book.id**, not Google Books ID, and does not
require a Supabase JWT. Optional query params:
- `page` (default 1, minimum 1);
- `limit` (default 10, range 1..50);
- `sort` (`recent` or `top`).

The JSON body is `{ data: [...], meta: { page, limit, total, totalPages } }`.
Each item contains `id`, optional `rating`, optional `comment`, `updatedAt`
and `user: { id, username, avatarUrl }`. Review cards show the author,
available rating/comment and update date. **Private notes are not returned.**

Backend reads reviews from `UserBookState` entries with a rating and/or a
nonempty comment. A catalog book with no reviews returns an empty page;
a nonexistent book ID gives HTTP 404. No review-write API is available
for this feature.

## Architecture
`BookReviewsSection -> BookReviewsViewModel -> GetBookReviews ->
BookReviewsRepository -> BookReviewsRepositoryImpl ->
KtorBookReviewsRemoteDataSource`.

The same `BookDetail` used by existing search and the #50 mood search can
render optional public reviews without changing the book data model.
The #52 branch currently builds on the unmerged #50 branch. After #50 is
merged, merge/rebase onto updated `main` before creating a PR.

## Verification
Run `./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug`.
Run the Bookie local backend and `adb reverse tcp:3000 tcp:3000` on a
physical Android device. Search a book, open its detail, scroll to
"Reseñas de lectores", and check real results or "todavía no tiene reseñas".
Use the sorting and "Ver más reseñas" action when data is available.

When books have no reviews locally, do not fabricate test reviews as a
replacement for backend data. Unit/MockEngine tests cover parsing, empty
pages, sort/pagination, and error handling.

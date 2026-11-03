# Series Explorer API Contract

The backend's endpoint index lists these routes, but its OpenAPI documentation does not yet describe every response field. The mobile DTOs below follow the current Prisma includes and return shapes in `bookie-backend/src/series/series-progress.service.ts`.

## `GET /series`

Public endpoint. Query parameters are `page` (default 1, minimum 1), `limit` (default 20, range 1-50), and optional `universeId`.

Response shape: `{ data: Series[], meta: { page, limit, total, totalPages } }`. Each item includes `id`, `universeId`, `title`, `description`, `coverUrl`, `totalBooks`, the related `universe`, and `_count.nodes`.

## `GET /universes`

Requires a JWT. Returns an array of universe records with `_count.series` and personalized `progress`: `universeId`, `name`, `totalBooks`, `readBooks`, `completionPercentage`, and `isCompleted`.

## `GET /universes/:id`

Requires a JWT. Returns the universe record with its `series`; each series includes its `nodes` with nested `book` records and its `edges`. The app maps the series metadata and node count for discovery, then opens the existing series map by series ID rather than duplicating map behavior here.

Authentication failures are surfaced as session-required (401) or access-denied (403) UI states. If backend response includes change, update these DTOs and this contract together.

# Local library cache policy (issue #25)

The Bookie API is authoritative. SQLDelight stores **only server-confirmed** library
pages and progress snapshots. This is a read cache, not an offline write queue.
The cache is intentionally independent from UI and Ktor; the repository will
coordinate cache-first reads and remote refresh in #26 and #7.

## Privacy and accounts

- Every row and page key includes the authenticated account's internal user ID.
  A blank ID is rejected. Never use the public catalog book ID as an account ID.
- The caller must invoke `clearAccount(accountId)` on logout and before switching
  accounts. The cache implementation removes that account's states and pages.
  No data from one account may be returned for another.
- SQLite is not encrypted in this issue. **Private notes are not cached**:
  `privateNotes` is always null in locally reconstructed states. Reading-session
  notes and tokens/passwords are not stored at all. These remain remote-only.
  Ratings, comments, reading status, and reading progress **are** stored locally
  and should be considered personal information. Do not back up or share the
  database file. If storing private notes offline is later required, implement
  platform-specific encryption and document key management first.
- Cached library records survive app restarts until the account is cleared or
  a later server-confirmed refresh replaces them. No TTL or background refresh
  is performed by this component; the repository will expose stale/offline
  status in #26/#7. Do not label cached data as synchronized.

## API and page semantics

`replacePage` atomically updates the requested page, including an empty page,
using the filter (status, favorites), page number, and page size as a compound
cache key. Pages for other filters remain separate. It stores books by their
**library state ID**, distinct from catalog book ID. A missing page returns null.
The cache never computes reading progress from a page count; the API has no
`pageCount` field.

The cache does not accept offline mutations or queue requests. The server
must confirm a write before the repository refreshes/invalidate cached pages.
A separate library-state delete is implemented by #31; the repository should
invalidate relevant pages after a successful delete in #26.

package org.bookie.app.data.local

import org.bookie.app.domain.model.LibraryBookState
import org.bookie.app.domain.model.LibraryPage
import org.bookie.app.domain.model.LibraryStatus

/**
 * Local, account-scoped read cache. Only API-confirmed snapshots should be written.
 * Private notes are deliberately not persisted without encryption.
 */
interface LibraryCache {
    suspend fun readPage(
        accountId: String,
        page: Int,
        limit: Int,
        status: LibraryStatus? = null,
        favoritesOnly: Boolean = false,
    ): LibraryPage?

    suspend fun replacePage(
        accountId: String,
        page: LibraryPage,
        status: LibraryStatus? = null,
        favoritesOnly: Boolean = false,
    )

    suspend fun readState(accountId: String, stateId: String): LibraryBookState?

    /** Erases all cached data for the account; call on logout or account switch. */
    suspend fun clearAccount(accountId: String)
}

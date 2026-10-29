package org.bookie.app.domain.repository

import kotlinx.coroutines.flow.Flow
import org.bookie.app.domain.model.LibraryBookState
import org.bookie.app.domain.model.LibraryPage
import org.bookie.app.domain.model.LibraryStatus

/**
 * Observes only the local cache, not the network. Every non-null value may be stale.
 * Null means no local snapshot is available for this account and query.
 */
interface ObservableLibraryRepository {
    fun observeCachedLibrary(
        page: Int = 1,
        limit: Int = 20,
        status: LibraryStatus? = null,
        favoritesOnly: Boolean = false,
    ): Flow<LibraryPage?>

    fun observeCachedState(stateId: String): Flow<LibraryBookState?>
}

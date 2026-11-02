package org.bookie.app.domain.repository

import kotlinx.coroutines.flow.Flow
import org.bookie.app.domain.model.LibraryLoad
import org.bookie.app.domain.model.LibraryStatus

/**
 * Cache-first library reads. The flow emits cached data (possibly stale) before
 * the server result; failures retain the last cached page when available.
 */
interface LibraryFeedRepository {
    fun observeLibrary(
        page: Int = 1,
        limit: Int = 20,
        status: LibraryStatus? = null,
        favoritesOnly: Boolean = false,
    ): Flow<LibraryLoad>
}

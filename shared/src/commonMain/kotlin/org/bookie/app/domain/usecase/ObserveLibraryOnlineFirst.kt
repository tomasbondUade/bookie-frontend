package org.bookie.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.bookie.app.domain.model.LibraryLoad
import org.bookie.app.domain.model.LibraryStatus
import org.bookie.app.domain.repository.LibraryFeedRepository

/** Cache-first read and remote refresh. Suitable for a ViewModel's StateFlow. */
class ObserveLibraryOnlineFirst(private val repository: LibraryFeedRepository) {
    operator fun invoke(
        page: Int = 1,
        limit: Int = 20,
        status: LibraryStatus? = null,
        favoritesOnly: Boolean = false,
    ): Flow<LibraryLoad> = repository.observeLibrary(page, limit, status, favoritesOnly)
}

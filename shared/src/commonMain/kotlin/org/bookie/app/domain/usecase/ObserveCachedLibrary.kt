package org.bookie.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.bookie.app.domain.model.LibraryPage
import org.bookie.app.domain.model.LibraryStatus
import org.bookie.app.domain.repository.ObservableLibraryRepository

/** A reactive read of local data; it does not refresh the server. */
class ObserveCachedLibrary(private val repository: ObservableLibraryRepository) {
    operator fun invoke(
        page: Int = 1,
        limit: Int = 20,
        status: LibraryStatus? = null,
        favoritesOnly: Boolean = false,
    ): Flow<LibraryPage?> =
        repository.observeCachedLibrary(page, limit, status, favoritesOnly)
}

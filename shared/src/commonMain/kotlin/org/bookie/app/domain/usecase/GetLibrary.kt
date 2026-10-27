package org.bookie.app.domain.usecase

import org.bookie.app.domain.model.LibraryPage
import org.bookie.app.domain.model.LibraryStatus
import org.bookie.app.domain.repository.LibraryRepository
import org.bookie.app.domain.result.Resource

class GetLibrary(
    private val libraryRepository: LibraryRepository,
) {
    suspend operator fun invoke(
        page: Int = 1,
        limit: Int = 20,
        status: LibraryStatus? = null,
        favoritesOnly: Boolean = false,
    ): Resource<LibraryPage> =
        libraryRepository.getLibrary(page, limit, status, favoritesOnly)
}

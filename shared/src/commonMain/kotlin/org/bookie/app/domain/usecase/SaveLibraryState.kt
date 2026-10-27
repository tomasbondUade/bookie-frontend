package org.bookie.app.domain.usecase

import org.bookie.app.domain.model.LibraryBookState
import org.bookie.app.domain.model.LibraryStateInput
import org.bookie.app.domain.repository.LibraryRepository
import org.bookie.app.domain.result.Resource

class SaveLibraryState(
    private val libraryRepository: LibraryRepository,
) {
    suspend operator fun invoke(input: LibraryStateInput): Resource<LibraryBookState> =
        libraryRepository.saveState(input)
}

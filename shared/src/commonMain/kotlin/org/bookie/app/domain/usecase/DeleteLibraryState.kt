package org.bookie.app.domain.usecase

import org.bookie.app.domain.repository.LibraryRepository
import org.bookie.app.domain.result.Resource

/** Removes a personal library entry, not the shared catalog book. */
class DeleteLibraryState(
    private val libraryRepository: LibraryRepository,
) {
    suspend operator fun invoke(stateId: String): Resource<Unit> {
        require(stateId.isNotBlank()) { "Library state ID cannot be blank." }
        return libraryRepository.deleteState(stateId)
    }
}

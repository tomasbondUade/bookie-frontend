package org.bookie.app.domain.usecase

import org.bookie.app.domain.model.ReadingSession
import org.bookie.app.domain.repository.LibraryRepository
import org.bookie.app.domain.result.Resource

class GetReadingSessions(
    private val libraryRepository: LibraryRepository,
) {
    suspend operator fun invoke(stateId: String): Resource<List<ReadingSession>> =
        libraryRepository.getReadingSessions(stateId)
}

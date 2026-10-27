package org.bookie.app.domain.usecase

import org.bookie.app.domain.model.ReadingSession
import org.bookie.app.domain.model.ReadingSessionInput
import org.bookie.app.domain.repository.LibraryRepository
import org.bookie.app.domain.result.Resource

class CreateReadingSession(
    private val libraryRepository: LibraryRepository,
) {
    suspend operator fun invoke(
        stateId: String,
        input: ReadingSessionInput,
        idempotencyKey: String,
    ): Resource<ReadingSession> =
        libraryRepository.createReadingSession(stateId, input, idempotencyKey)
}

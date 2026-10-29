package org.bookie.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.bookie.app.domain.model.LibraryBookState
import org.bookie.app.domain.repository.ObservableLibraryRepository

/** Observes the cached state/progress of one personal library entry. */
class ObserveCachedLibraryState(private val repository: ObservableLibraryRepository) {
    operator fun invoke(stateId: String): Flow<LibraryBookState?> =
        repository.observeCachedState(stateId)
}

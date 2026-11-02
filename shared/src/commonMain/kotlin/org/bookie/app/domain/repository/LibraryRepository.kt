package org.bookie.app.domain.repository

import org.bookie.app.domain.model.LibraryBookState
import org.bookie.app.domain.model.LibraryPage
import org.bookie.app.domain.model.LibraryStateInput
import org.bookie.app.domain.model.LibraryStatus
import org.bookie.app.domain.model.ReadingSession
import org.bookie.app.domain.model.ReadingSessionInput
import org.bookie.app.domain.result.Resource

interface LibraryRepository {
    suspend fun getLibrary(
        page: Int = 1,
        limit: Int = DEFAULT_PAGE_SIZE,
        status: LibraryStatus? = null,
        favoritesOnly: Boolean = false,
    ): Resource<LibraryPage>

    suspend fun deleteState(stateId: String): Resource<Unit>

    suspend fun saveState(input: LibraryStateInput): Resource<LibraryBookState>

    suspend fun createReadingSession(
        stateId: String,
        input: ReadingSessionInput,
        idempotencyKey: String,
    ): Resource<ReadingSession>

    suspend fun getReadingSessions(stateId: String): Resource<List<ReadingSession>>
}

private const val DEFAULT_PAGE_SIZE = 20

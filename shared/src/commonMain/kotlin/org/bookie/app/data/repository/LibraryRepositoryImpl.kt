package org.bookie.app.data.repository

import org.bookie.app.data.mapper.toDomain
import org.bookie.app.data.mapper.toDto
import org.bookie.app.data.remote.dto.LibraryStatusDto
import org.bookie.app.data.remote.LibraryRemoteDataSource
import org.bookie.app.data.remote.toApiError
import org.bookie.app.data.remote.toMessage
import org.bookie.app.domain.model.LibraryBookState
import org.bookie.app.domain.model.LibraryPage
import org.bookie.app.domain.model.LibraryStateInput
import org.bookie.app.domain.model.LibraryStatus
import org.bookie.app.domain.model.ReadingSession
import org.bookie.app.domain.model.ReadingSessionInput
import org.bookie.app.domain.repository.LibraryRepository
import org.bookie.app.domain.result.Resource

class LibraryRepositoryImpl(
    private val remoteDataSource: LibraryRemoteDataSource,
) : LibraryRepository {
    override suspend fun getLibrary(
        page: Int,
        limit: Int,
        status: LibraryStatus?,
        favoritesOnly: Boolean,
    ): Resource<LibraryPage> = safely {
        remoteDataSource.getLibrary(
            page = page,
            limit = limit,
            status = status?.let { LibraryStatusDto.valueOf(it.name) },
            favoritesOnly = favoritesOnly,
        ).toDomain()
    }

    override suspend fun deleteState(stateId: String): Resource<Unit> = safely {
        remoteDataSource.deleteState(stateId)
    }

    override suspend fun saveState(input: LibraryStateInput): Resource<LibraryBookState> =
        safely { remoteDataSource.saveState(input.toDto()).toDomain() }

    override suspend fun createReadingSession(
        stateId: String,
        input: ReadingSessionInput,
        idempotencyKey: String,
    ): Resource<ReadingSession> = safely {
        remoteDataSource.createReadingSession(
            stateId = stateId,
            input = input.toDto(),
            idempotencyKey = idempotencyKey,
        ).toDomain()
    }

    override suspend fun getReadingSessions(stateId: String): Resource<List<ReadingSession>> =
        safely { remoteDataSource.getReadingSessions(stateId).map { it.toDomain() } }

    private suspend fun <T> safely(block: suspend () -> T): Resource<T> = try {
        Resource.Success(block())
    } catch (exception: Exception) {
        Resource.Error(exception.toApiError().toMessage())
    }
}

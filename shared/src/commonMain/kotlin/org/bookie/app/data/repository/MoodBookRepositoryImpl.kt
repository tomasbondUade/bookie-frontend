package org.bookie.app.data.repository

import org.bookie.app.data.mapper.toDomain
import org.bookie.app.data.remote.MoodBookRemoteDataSource
import org.bookie.app.data.remote.toApiError
import org.bookie.app.data.remote.toMessage
import org.bookie.app.domain.model.MoodBook
import org.bookie.app.domain.repository.MoodBookRepository
import org.bookie.app.domain.result.Resource

class MoodBookRepositoryImpl(
    private val remoteDataSource: MoodBookRemoteDataSource,
) : MoodBookRepository {
    override suspend fun searchByMood(query: String): Resource<List<MoodBook>> = try {
        Resource.Success(
            remoteDataSource.searchByMood(query).map { dto ->
                MoodBook(book = dto.toDomain(), relevance = dto.relevance)
            },
        )
    } catch (exception: Exception) {
        // Existing error translation preserves coroutine cancellation.
        Resource.Error(exception.toApiError().toMessage())
    }
}

package org.bookie.app.data.repository

import org.bookie.app.data.mapper.toDomain
import org.bookie.app.data.remote.ApiError
import org.bookie.app.data.remote.ReadingStatsRemoteDataSource
import org.bookie.app.data.remote.toApiError
import org.bookie.app.data.remote.toMessage
import org.bookie.app.domain.model.ReadingStats
import org.bookie.app.domain.repository.ReadingStatsRepository
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind

class ReadingStatsRepositoryImpl(
    private val remoteDataSource: ReadingStatsRemoteDataSource,
) : ReadingStatsRepository {
    override suspend fun getStats(): Resource<ReadingStats> = try {
        Resource.Success(remoteDataSource.getStats().toDomain())
    } catch (exception: Exception) {
        val apiError = exception.toApiError()
        Resource.Error(
            message = apiError.toMessage(),
            kind = if (apiError is ApiError.Http && apiError.statusCode == 401) {
                ResourceErrorKind.UNAUTHORIZED
            } else {
                ResourceErrorKind.GENERAL
            },
        )
    }
}

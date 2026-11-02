package org.bookie.app.data.repository

import org.bookie.app.data.mapper.toDomain
import org.bookie.app.data.mapper.toDto
import org.bookie.app.data.remote.ApiError
import org.bookie.app.data.remote.SeriesReadingRemoteDataSource
import org.bookie.app.data.remote.toApiError
import org.bookie.app.data.remote.toMessage
import org.bookie.app.domain.model.*
import org.bookie.app.domain.repository.SeriesReadingRepository
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind

class SeriesReadingRepositoryImpl(
    private val remote: SeriesReadingRemoteDataSource,
) : SeriesReadingRepository {
    override suspend fun getActiveSeries(): Resource<List<SeriesProgress>> =
        safely { remote.getActiveSeries().map { it.toDomain() } }

    override suspend fun getProgress(seriesId: String): Resource<SeriesProgress> =
        safely { remote.getProgress(seriesId).toDomain() }

    override suspend fun getMap(seriesId: String, path: ReadingPath): Resource<SeriesReadingMap> =
        safely { remote.getMap(seriesId, path.toDto()).toDomain() }

    private suspend fun <T> safely(block: suspend () -> T): Resource<T> = try {
        Resource.Success(block())
    } catch (exception: Exception) {
        val error = exception.toApiError()
        Resource.Error(
            error.toMessage(),
            if (error is ApiError.Http && error.statusCode == 401) ResourceErrorKind.UNAUTHORIZED
            else ResourceErrorKind.GENERAL,
        )
    }
}

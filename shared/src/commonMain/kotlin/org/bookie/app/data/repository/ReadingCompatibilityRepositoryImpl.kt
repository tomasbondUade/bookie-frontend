package org.bookie.app.data.repository

import org.bookie.app.data.mapper.toDomain
import org.bookie.app.data.remote.ApiError
import org.bookie.app.data.remote.ReadingCompatibilityRemoteDataSource
import org.bookie.app.data.remote.toApiError
import org.bookie.app.data.remote.toMessage
import org.bookie.app.domain.model.ReadingCompatibility
import org.bookie.app.domain.repository.ReadingCompatibilityRepository
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind

class ReadingCompatibilityRepositoryImpl(
    private val remoteDataSource: ReadingCompatibilityRemoteDataSource,
) : ReadingCompatibilityRepository {
    override suspend fun getCompatibility(targetUserId: String): Resource<ReadingCompatibility> = try {
        Resource.Success(remoteDataSource.getCompatibility(targetUserId).toDomain())
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

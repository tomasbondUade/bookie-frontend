package org.bookie.app.data.repository

import org.bookie.app.data.mapper.toCreateSwipeDto
import org.bookie.app.data.mapper.toDomain
import org.bookie.app.data.remote.ApiError
import org.bookie.app.data.remote.BookTinderRemoteDataSource
import org.bookie.app.data.remote.toApiError
import org.bookie.app.data.remote.toMessage
import org.bookie.app.domain.model.BookTinderFeed
import org.bookie.app.domain.model.SwipeType
import org.bookie.app.domain.repository.BookTinderRepository
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind

class BookTinderRepositoryImpl(
    private val remote: BookTinderRemoteDataSource,
) : BookTinderRepository {
    override suspend fun getFeed(limit: Int): Resource<BookTinderFeed> =
        safely { remote.getFeed(limit).toDomain() }

    override suspend fun recordSwipe(bookId: String, type: SwipeType): Resource<Unit> = safely {
        val result = remote.recordSwipe(type.toCreateSwipeDto(bookId))
        check(result.success) { result.message ?: "El servidor no confirmó el swipe." }
    }

    private suspend fun <T> safely(block: suspend () -> T): Resource<T> = try {
        Resource.Success(block())
    } catch (exception: Exception) {
        val error = exception.toApiError()
        Resource.Error(error.toMessage(), error.toResourceKind())
    }

    private fun ApiError.toResourceKind(): ResourceErrorKind = when (this) {
        is ApiError.Http -> when (statusCode) {
            401 -> ResourceErrorKind.UNAUTHORIZED
            403 -> ResourceErrorKind.FORBIDDEN
            404 -> ResourceErrorKind.NOT_FOUND
            409 -> ResourceErrorKind.CONFLICT
            400, 422 -> ResourceErrorKind.VALIDATION
            else -> ResourceErrorKind.GENERAL
        }
        ApiError.Network, ApiError.Timeout -> ResourceErrorKind.NETWORK
        ApiError.Parsing, ApiError.Unknown -> ResourceErrorKind.GENERAL
    }
}

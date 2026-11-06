package org.bookie.app.data.repository

import kotlin.coroutines.cancellation.CancellationException
import org.bookie.app.data.mapper.toTinderDomain
import org.bookie.app.data.remote.ApiError
import org.bookie.app.data.remote.TinderRemoteDataSource
import org.bookie.app.data.remote.toApiError
import org.bookie.app.data.remote.toMessage
import org.bookie.app.data.remote.dto.CreateSwipeDto
import org.bookie.app.domain.model.*
import org.bookie.app.domain.repository.BookTinderRepository
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind

class BookTinderRepositoryImpl(private val remote: TinderRemoteDataSource) : BookTinderRepository {
    override suspend fun getTinderBooks(limit: Int): Resource<TinderFeed> = safely {
        remote.getFeed(limit).toTinderDomain()
    }

    override suspend fun registerSwipe(swipe: BookSwipe): Resource<SwipeResult> = safely {
        require(swipe.bookId.isNotBlank()) { "Book ID is required" }
        val response = remote.sendSwipe(CreateSwipeDto(swipe.bookId, swipe.type.name))
        SwipeResult(response.success, response.message)
    }

    private suspend fun <T> safely(block: suspend () -> T): Resource<T> = try {
        Resource.Success(block())
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (exception: Exception) {
        val apiError = exception.toApiError()
        val kind = when (apiError) {
            is ApiError.Http -> if (apiError.statusCode == 401) ResourceErrorKind.UNAUTHORIZED else ResourceErrorKind.GENERAL
            ApiError.Network, ApiError.Timeout -> ResourceErrorKind.NETWORK
            else -> ResourceErrorKind.GENERAL
        }
        Resource.Error(apiError.toMessage(), kind)
    }
}

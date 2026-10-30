package org.bookie.app.data.repository

import org.bookie.app.data.mapper.toDomain
import org.bookie.app.data.mapper.toDto
import org.bookie.app.data.remote.ApiError
import org.bookie.app.data.remote.BookListsRemoteDataSource
import org.bookie.app.data.remote.toApiError
import org.bookie.app.data.remote.toMessage
import org.bookie.app.domain.model.BookList
import org.bookie.app.domain.model.BookListItem
import org.bookie.app.domain.model.BookListPage
import org.bookie.app.domain.model.CreateBookListInput
import org.bookie.app.domain.model.UpdateBookListInput
import org.bookie.app.domain.repository.BookListsRepository
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind

class BookListsRepositoryImpl(
    private val remote: BookListsRemoteDataSource,
) : BookListsRepository {
    override suspend fun getLists(page: Int, limit: Int): Resource<BookListPage> =
        safely { remote.getLists(page, limit).toDomain() }

    override suspend fun createList(input: CreateBookListInput): Resource<BookList> =
        safely { remote.createList(input.toDto()).toDomain() }

    override suspend fun updateList(listId: String, input: UpdateBookListInput): Resource<BookList> =
        safely { remote.updateList(listId, input.toDto()).toDomain() }

    override suspend fun deleteList(listId: String): Resource<Unit> = safely {
        check(remote.deleteList(listId).success) { "El servidor no confirmó la eliminación." }
    }

    override suspend fun addBook(listId: String, bookId: String): Resource<BookListItem> =
        safely { remote.addBook(listId, bookId).toDomain() }

    override suspend fun removeBook(listId: String, bookId: String): Resource<Unit> = safely {
        check(remote.removeBook(listId, bookId).success) { "El servidor no confirmó la eliminación." }
    }

    private suspend fun <T> safely(block: suspend () -> T): Resource<T> = try {
        Resource.Success(block())
    } catch (exception: Exception) {
        val error = exception.toApiError() // rethrows coroutine cancellation
        Resource.Error(error.toMessage(), error.toResourceKind())
    }

    private fun ApiError.toResourceKind(): ResourceErrorKind = when (this) {
        is ApiError.Http -> when (statusCode) {
            400, 422 -> ResourceErrorKind.VALIDATION
            401 -> ResourceErrorKind.UNAUTHORIZED
            403 -> ResourceErrorKind.FORBIDDEN
            404 -> ResourceErrorKind.NOT_FOUND
            409 -> ResourceErrorKind.CONFLICT
            else -> ResourceErrorKind.GENERAL
        }
        ApiError.Network, ApiError.Timeout -> ResourceErrorKind.NETWORK
        ApiError.Parsing, ApiError.Unknown -> ResourceErrorKind.GENERAL
    }
}

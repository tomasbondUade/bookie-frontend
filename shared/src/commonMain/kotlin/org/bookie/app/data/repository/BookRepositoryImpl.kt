package org.bookie.app.data.repository

import org.bookie.app.data.mapper.toDomain
import org.bookie.app.data.remote.BookSearchRemoteDataSource
import org.bookie.app.data.remote.toMessage
import org.bookie.app.data.remote.toApiError
import org.bookie.app.domain.model.Book
import org.bookie.app.domain.repository.BookRepository
import org.bookie.app.domain.result.Resource

class BookRepositoryImpl(
    private val remoteDataSource: BookSearchRemoteDataSource,
) : BookRepository {
    override suspend fun searchBooks(query: String): Resource<List<Book>> = try {
        Resource.Success(remoteDataSource.searchBooks(query).map { it.toDomain() })
    } catch (exception: Exception) {
        Resource.Error(exception.toApiError().toMessage())
    }
}

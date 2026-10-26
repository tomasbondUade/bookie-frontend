package org.bookie.app.data.repository

import org.bookie.app.data.mapper.toDomain
import org.bookie.app.data.remote.ApiError
import org.bookie.app.data.remote.BookSearchRemoteDataSource
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
        Resource.Error(exception.toApiError().message())
    }
}

private fun ApiError.message(): String = when (this) {
    ApiError.Network -> "No se pudo conectar con el servidor."
    ApiError.Timeout -> "La búsqueda tardó demasiado."
    is ApiError.Http -> "El servidor respondió con HTTP $statusCode."
    ApiError.Parsing -> "No se pudo interpretar la respuesta del servidor."
    ApiError.Unknown -> "No se pudo completar la búsqueda."
}

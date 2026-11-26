package org.bookie.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import org.bookie.app.data.remote.dto.BookDto

interface BookSearchRemoteDataSource {
    suspend fun searchBooks(query: String, page: Int = 1): List<BookDto>
}

class KtorBookSearchRemoteDataSource(
    private val httpClient: HttpClient,
) : BookSearchRemoteDataSource {
    override suspend fun searchBooks(query: String, page: Int): List<BookDto> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) return emptyList()
        require(page >= 1) { "La página debe ser mayor o igual a 1." }

        return httpClient.get("books/search") {
            parameter("q", normalizedQuery)
            parameter("page", page)
        }.body()
    }
}

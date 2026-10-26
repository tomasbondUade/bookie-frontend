package org.bookie.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import org.bookie.app.data.remote.dto.BookDto

interface BookSearchRemoteDataSource {
    suspend fun searchBooks(query: String): List<BookDto>
}

class KtorBookSearchRemoteDataSource(
    private val httpClient: HttpClient,
) : BookSearchRemoteDataSource {
    override suspend fun searchBooks(query: String): List<BookDto> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) return emptyList()

        return httpClient.get("books/search") {
            parameter("q", normalizedQuery)
        }.body()
    }
}

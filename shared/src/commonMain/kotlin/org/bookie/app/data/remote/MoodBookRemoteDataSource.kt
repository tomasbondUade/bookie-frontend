package org.bookie.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import org.bookie.app.data.remote.dto.BookDto

interface MoodBookRemoteDataSource {
    suspend fun searchByMood(query: String): List<BookDto>
}

class KtorMoodBookRemoteDataSource(
    private val httpClient: HttpClient,
) : MoodBookRemoteDataSource {
    override suspend fun searchByMood(query: String): List<BookDto> {
        val normalized = query.trim()
        if (normalized.isEmpty()) return emptyList()

        // Public controller: GET /books/mood?q=free text. Do not mock production results.
        return httpClient.get("books/mood") {
            parameter("q", normalized)
        }.body()
    }
}

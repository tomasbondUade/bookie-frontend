package org.bookie.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.bookie.app.data.remote.dto.BookTinderFeedDto
import org.bookie.app.data.remote.dto.CreateSwipeDto
import org.bookie.app.data.remote.dto.SwipeResultDto

interface BookTinderRemoteDataSource {
    suspend fun getFeed(limit: Int): BookTinderFeedDto
    suspend fun recordSwipe(input: CreateSwipeDto): SwipeResultDto
}

class KtorBookTinderRemoteDataSource(
    private val httpClient: HttpClient,
) : BookTinderRemoteDataSource {
    override suspend fun getFeed(limit: Int): BookTinderFeedDto {
        require(limit in MIN_FEED_LIMIT..MAX_FEED_LIMIT) {
            "El límite debe estar entre $MIN_FEED_LIMIT y $MAX_FEED_LIMIT."
        }
        return httpClient.get("books/tinder") {
            parameter("limit", limit)
        }.body()
    }

    override suspend fun recordSwipe(input: CreateSwipeDto): SwipeResultDto {
        require(input.bookId.isNotBlank()) { "El ID interno del libro es obligatorio." }
        return httpClient.post("swipes") {
            contentType(ContentType.Application.Json)
            setBody(input)
        }.body()
    }

    private companion object {
        const val MIN_FEED_LIMIT = 1
        const val MAX_FEED_LIMIT = 50
    }
}

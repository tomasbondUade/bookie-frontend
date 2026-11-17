package org.bookie.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.appendPathSegments
import org.bookie.app.data.remote.dto.BookReviewsPageDto
import org.bookie.app.domain.model.BookReviewSort

interface BookReviewsRemoteDataSource {
    suspend fun getReviews(
        bookId: String,
        page: Int,
        limit: Int,
        sort: BookReviewSort,
    ): BookReviewsPageDto
}

class KtorBookReviewsRemoteDataSource(
    private val client: HttpClient,
) : BookReviewsRemoteDataSource {
    override suspend fun getReviews(
        bookId: String,
        page: Int,
        limit: Int,
        sort: BookReviewSort,
    ): BookReviewsPageDto {
        require(bookId.isNotBlank()) { "El ID del libro es obligatorio." }
        require(page >= 1) { "La página debe ser mayor que cero." }
        require(limit in 1..50) { "El límite debe estar entre 1 y 50." }

        // Public endpoint; no JWT, no write, no fabricated data.
        return client.get {
            url { appendPathSegments("books", bookId, "reviews") }
            parameter("page", page)
            parameter("limit", limit)
            parameter("sort", sort.apiValue)
        }.body()
    }
}

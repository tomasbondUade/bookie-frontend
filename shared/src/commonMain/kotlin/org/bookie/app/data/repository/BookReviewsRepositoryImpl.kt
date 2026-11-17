package org.bookie.app.data.repository

import org.bookie.app.data.remote.BookReviewsRemoteDataSource
import org.bookie.app.data.remote.dto.BookReviewsPageDto
import org.bookie.app.data.remote.toApiError
import org.bookie.app.data.remote.toMessage
import org.bookie.app.domain.model.BookReview
import org.bookie.app.domain.model.BookReviewSort
import org.bookie.app.domain.model.BookReviewsPage
import org.bookie.app.domain.repository.BookReviewsRepository
import org.bookie.app.domain.result.Resource

class BookReviewsRepositoryImpl(
    private val remote: BookReviewsRemoteDataSource,
) : BookReviewsRepository {
    override suspend fun getReviews(
        bookId: String,
        page: Int,
        limit: Int,
        sort: BookReviewSort,
    ): Resource<BookReviewsPage> = try {
        Resource.Success(remote.getReviews(bookId, page, limit, sort).toDomain())
    } catch (exception: Exception) {
        // toApiError preserves coroutine cancellation.
        Resource.Error(exception.toApiError().toMessage())
    }
}

private fun BookReviewsPageDto.toDomain(): BookReviewsPage = BookReviewsPage(
    reviews = data.map { dto ->
        BookReview(
            id = dto.id,
            rating = dto.rating,
            comment = dto.comment?.trim()?.takeIf(String::isNotEmpty),
            updatedAt = dto.updatedAt,
            username = dto.user.username,
            avatarUrl = dto.user.avatarUrl,
        )
    },
    page = meta.page,
    limit = meta.limit,
    total = meta.total,
    totalPages = meta.totalPages,
)

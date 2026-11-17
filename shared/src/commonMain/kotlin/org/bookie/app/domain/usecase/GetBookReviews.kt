package org.bookie.app.domain.usecase

import org.bookie.app.domain.model.BookReviewSort
import org.bookie.app.domain.model.BookReviewsPage
import org.bookie.app.domain.repository.BookReviewsRepository
import org.bookie.app.domain.result.Resource

/** Uses the backend's internal book.id; it is not a Google Books identifier. */
class GetBookReviews(private val repository: BookReviewsRepository) {
    suspend operator fun invoke(
        bookId: String,
        page: Int = 1,
        limit: Int = 10,
        sort: BookReviewSort = BookReviewSort.RECENT,
    ): Resource<BookReviewsPage> =
        repository.getReviews(bookId = bookId, page = page, limit = limit, sort = sort)
}

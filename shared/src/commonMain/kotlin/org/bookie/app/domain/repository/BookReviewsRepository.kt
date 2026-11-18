package org.bookie.app.domain.repository

import org.bookie.app.domain.model.BookReviewSort
import org.bookie.app.domain.model.BookReviewsPage
import org.bookie.app.domain.result.Resource

interface BookReviewsRepository {
    suspend fun getReviews(
        bookId: String,
        page: Int = 1,
        limit: Int = 10,
        sort: BookReviewSort = BookReviewSort.RECENT,
    ): Resource<BookReviewsPage>
}

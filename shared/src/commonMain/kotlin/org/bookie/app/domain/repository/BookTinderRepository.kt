package org.bookie.app.domain.repository

import org.bookie.app.domain.model.BookTinderFeed
import org.bookie.app.domain.model.SwipeType
import org.bookie.app.domain.result.Resource

interface BookTinderRepository {
    suspend fun getFeed(limit: Int = 10): Resource<BookTinderFeed>
    suspend fun recordSwipe(bookId: String, type: SwipeType): Resource<Unit>
}

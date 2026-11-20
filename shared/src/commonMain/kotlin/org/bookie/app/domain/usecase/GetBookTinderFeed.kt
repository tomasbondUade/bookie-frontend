package org.bookie.app.domain.usecase

import org.bookie.app.domain.model.BookTinderFeed
import org.bookie.app.domain.repository.BookTinderRepository
import org.bookie.app.domain.result.Resource

class GetBookTinderFeed(private val repository: BookTinderRepository) {
    suspend operator fun invoke(limit: Int = 10): Resource<BookTinderFeed> =
        repository.getFeed(limit)
}

package org.bookie.app.domain.usecase

import org.bookie.app.domain.model.BookListPage
import org.bookie.app.domain.repository.BookListsRepository
import org.bookie.app.domain.result.Resource

class GetBookLists(private val repository: BookListsRepository) {
    suspend operator fun invoke(page: Int = 1, limit: Int = 20): Resource<BookListPage> =
        repository.getLists(page, limit)
}

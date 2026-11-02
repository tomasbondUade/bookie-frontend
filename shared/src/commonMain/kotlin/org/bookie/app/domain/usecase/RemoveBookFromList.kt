package org.bookie.app.domain.usecase

import org.bookie.app.domain.repository.BookListsRepository
import org.bookie.app.domain.result.Resource

class RemoveBookFromList(private val repository: BookListsRepository) {
    suspend operator fun invoke(listId: String, bookId: String): Resource<Unit> =
        repository.removeBook(listId, bookId)
}

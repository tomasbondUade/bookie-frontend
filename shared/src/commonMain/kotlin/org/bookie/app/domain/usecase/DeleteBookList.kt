package org.bookie.app.domain.usecase

import org.bookie.app.domain.repository.BookListsRepository
import org.bookie.app.domain.result.Resource

class DeleteBookList(private val repository: BookListsRepository) {
    suspend operator fun invoke(listId: String): Resource<Unit> = repository.deleteList(listId)
}

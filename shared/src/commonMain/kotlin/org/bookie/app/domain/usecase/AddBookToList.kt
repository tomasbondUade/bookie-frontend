package org.bookie.app.domain.usecase

import org.bookie.app.domain.model.BookListItem
import org.bookie.app.domain.repository.BookListsRepository
import org.bookie.app.domain.result.Resource

class AddBookToList(private val repository: BookListsRepository) {
    suspend operator fun invoke(listId: String, bookId: String): Resource<BookListItem> =
        repository.addBook(listId, bookId)
}

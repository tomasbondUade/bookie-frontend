package org.bookie.app.domain.usecase

import org.bookie.app.domain.model.BookList
import org.bookie.app.domain.model.UpdateBookListInput
import org.bookie.app.domain.repository.BookListsRepository
import org.bookie.app.domain.result.Resource

class UpdateBookList(private val repository: BookListsRepository) {
    suspend operator fun invoke(listId: String, input: UpdateBookListInput): Resource<BookList> =
        repository.updateList(listId, input)
}

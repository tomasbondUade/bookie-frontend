package org.bookie.app.domain.usecase

import org.bookie.app.domain.model.BookList
import org.bookie.app.domain.model.CreateBookListInput
import org.bookie.app.domain.repository.BookListsRepository
import org.bookie.app.domain.result.Resource

class CreateBookList(private val repository: BookListsRepository) {
    suspend operator fun invoke(input: CreateBookListInput): Resource<BookList> =
        repository.createList(input)
}

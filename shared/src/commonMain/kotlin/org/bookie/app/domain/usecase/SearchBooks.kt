package org.bookie.app.domain.usecase

import org.bookie.app.domain.model.Book
import org.bookie.app.domain.repository.BookRepository
import org.bookie.app.domain.result.Resource

class SearchBooks(
    private val bookRepository: BookRepository,
) {
    suspend operator fun invoke(query: String, page: Int = 1): Resource<List<Book>> =
        bookRepository.searchBooks(query, page)
}

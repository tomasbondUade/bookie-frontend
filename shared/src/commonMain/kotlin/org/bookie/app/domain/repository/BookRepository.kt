package org.bookie.app.domain.repository

import org.bookie.app.domain.model.Book
import org.bookie.app.domain.result.Resource

interface BookRepository {
    suspend fun searchBooks(query: String): Resource<List<Book>>
}

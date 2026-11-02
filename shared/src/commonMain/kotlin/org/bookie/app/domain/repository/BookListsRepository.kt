package org.bookie.app.domain.repository

import org.bookie.app.domain.model.BookList
import org.bookie.app.domain.model.BookListItem
import org.bookie.app.domain.model.BookListPage
import org.bookie.app.domain.model.CreateBookListInput
import org.bookie.app.domain.model.UpdateBookListInput
import org.bookie.app.domain.result.Resource

/**
 * Personal lists are always remote. The authenticated account is determined
 * by the access token, never by a user ID passed from Presentation.
 */
interface BookListsRepository {
    suspend fun getLists(page: Int = 1, limit: Int = 20): Resource<BookListPage>
    suspend fun createList(input: CreateBookListInput): Resource<BookList>
    suspend fun updateList(listId: String, input: UpdateBookListInput): Resource<BookList>
    suspend fun deleteList(listId: String): Resource<Unit>
    suspend fun addBook(listId: String, bookId: String): Resource<BookListItem>
    suspend fun removeBook(listId: String, bookId: String): Resource<Unit>
}

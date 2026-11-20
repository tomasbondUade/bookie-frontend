package org.bookie.app

import org.bookie.app.data.auth.AuthClientBundle
import org.bookie.app.data.remote.KtorBookListsRemoteDataSource
import org.bookie.app.data.remote.KtorBookSearchRemoteDataSource
import org.bookie.app.data.repository.BookListsRepositoryImpl
import org.bookie.app.data.repository.BookRepositoryImpl
import org.bookie.app.domain.usecase.AddBookToList
import org.bookie.app.domain.usecase.CreateBookList
import org.bookie.app.domain.usecase.DeleteBookList
import org.bookie.app.domain.usecase.GetBookLists
import org.bookie.app.domain.usecase.RemoveBookFromList
import org.bookie.app.domain.usecase.SearchBooks
import org.bookie.app.domain.usecase.UpdateBookList
import org.bookie.app.presentation.lists.BookListsViewModel

/** Reuses the authenticated client so list ownership is derived from the current JWT. */
fun createAuthenticatedBookListsViewModel(authClients: AuthClientBundle): BookListsViewModel {
    val lists = BookListsRepositoryImpl(KtorBookListsRemoteDataSource(authClients.bookieClient))
    val books = BookRepositoryImpl(KtorBookSearchRemoteDataSource(authClients.bookieClient))
    return BookListsViewModel(
        getBookLists = GetBookLists(lists),
        createBookList = CreateBookList(lists),
        updateBookList = UpdateBookList(lists),
        deleteBookList = DeleteBookList(lists),
        addBookToList = AddBookToList(lists),
        removeBookFromList = RemoveBookFromList(lists),
        searchBooksUseCase = SearchBooks(books),
    )
}

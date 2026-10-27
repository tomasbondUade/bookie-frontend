package org.bookie.app.presentation.search

import org.bookie.app.domain.model.Book

sealed interface SearchUiState {
    data object Idle : SearchUiState

    data object Loading : SearchUiState

    data class Success(
        val books: List<Book>,
        val query: String = "",
        val selectedBook: Book? = null,
    ) : SearchUiState

    data class Empty(val query: String) : SearchUiState

    data class Error(
        val message: String,
        val query: String = "",
    ) : SearchUiState
}

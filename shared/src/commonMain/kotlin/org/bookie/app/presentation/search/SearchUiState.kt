package org.bookie.app.presentation.search

import org.bookie.app.domain.model.Book

sealed interface SearchUiState {
    data object Idle : SearchUiState

    data object Loading : SearchUiState

    data class Success(val books: List<Book>) : SearchUiState

    data class Error(val message: String) : SearchUiState
}

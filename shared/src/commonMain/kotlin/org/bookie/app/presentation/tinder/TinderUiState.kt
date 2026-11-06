package org.bookie.app.presentation.tinder

import org.bookie.app.domain.model.Book

sealed interface TinderUiState {
    data object Loading : TinderUiState
    data class ShowingBook(
        val book: Book,
        val reasons: List<String>,
        val position: Int,
        val total: Int,
        val sending: Boolean = false,
        val error: String? = null
    ) : TinderUiState
    data object Finished : TinderUiState
    data class Error(val message: String, val unauthorized: Boolean = false) : TinderUiState
}

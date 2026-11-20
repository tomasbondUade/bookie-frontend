package org.bookie.app.presentation.preferences

import org.bookie.app.domain.model.Book
import org.bookie.app.domain.model.ReadingPreferences

sealed interface ReadingPreferencesUiState {
    data object Idle : ReadingPreferencesUiState

    data object Loading : ReadingPreferencesUiState

    data class LoadError(
        val message: String,
        val sessionExpired: Boolean = false,
    ) : ReadingPreferencesUiState

    data class Viewing(
        val preferences: ReadingPreferences,
        val notice: String? = null,
    ) : ReadingPreferencesUiState

    data class Editing(
        val baseline: ReadingPreferences,
        val preferredGenres: List<String>,
        val preferredAuthors: List<String>,
        val favoriteBookIds: List<String>,
        val selectedBooks: List<Book>,
        val genreInput: String = "",
        val authorInput: String = "",
        val searchQuery: String = "",
        val searchState: BookSearchState = BookSearchState.Idle,
        val isOnboarding: Boolean,
        val error: String? = null,
    ) : ReadingPreferencesUiState

    data class Saving(val draft: Editing) : ReadingPreferencesUiState
}

sealed interface BookSearchState {
    data object Idle : BookSearchState

    data object Loading : BookSearchState

    data class Results(val books: List<Book>) : BookSearchState

    data object Empty : BookSearchState

    data class Error(val message: String) : BookSearchState
}

package org.bookie.app.presentation.search

import org.bookie.app.domain.model.MoodBook

sealed interface MoodSearchUiState {
    data object Idle : MoodSearchUiState
    data object Loading : MoodSearchUiState
    data class Success(
        val books: List<MoodBook>,
        val selectedBook: MoodBook? = null,
    ) : MoodSearchUiState
    data object Empty : MoodSearchUiState
    data class Error(val message: String) : MoodSearchUiState
}

sealed interface MoodSaveUiState {
    data object Idle : MoodSaveUiState
    data object Saving : MoodSaveUiState
    data object Saved : MoodSaveUiState
    data class Error(val message: String) : MoodSaveUiState
}

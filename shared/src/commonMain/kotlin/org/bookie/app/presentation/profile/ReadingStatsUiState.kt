package org.bookie.app.presentation.profile

import org.bookie.app.domain.model.ReadingStats

sealed interface ReadingStatsUiState {
    data object Idle : ReadingStatsUiState

    data object Loading : ReadingStatsUiState

    data object Empty : ReadingStatsUiState

    data class Success(val stats: ReadingStats) : ReadingStatsUiState

    data class Error(
        val message: String,
        val sessionExpired: Boolean = false,
    ) : ReadingStatsUiState
}

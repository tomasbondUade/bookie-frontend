package org.bookie.app.presentation.progress

import org.bookie.app.domain.model.ReadingSession

sealed interface ProgressOperationStatus {
    data object Idle : ProgressOperationStatus
    data object Loading : ProgressOperationStatus
    data class Success(val message: String) : ProgressOperationStatus
    data class Error(val message: String) : ProgressOperationStatus
}

/** Immutable screen state; snapshot and session have independent outcomes. */
sealed interface ReadingProgressUiState {
    data class Unavailable(val message: String) : ReadingProgressUiState

    data class Content(
        val bookTitle: String,
        val currentPageInput: String,
        val percentageInput: String,
        val privateNotesInput: String,
        val sessionNotesInput: String = "",
        val currentPageError: String? = null,
        val percentageError: String? = null,
        val snapshotStatus: ProgressOperationStatus = ProgressOperationStatus.Idle,
        val sessionStatus: ProgressOperationStatus = ProgressOperationStatus.Idle,
        val sessions: List<ReadingSession> = emptyList(),
        val historyLoading: Boolean = false,
        val historyError: String? = null,
    ) : ReadingProgressUiState {
        val isBusy: Boolean
            get() = snapshotStatus == ProgressOperationStatus.Loading ||
                sessionStatus == ProgressOperationStatus.Loading
    }
}

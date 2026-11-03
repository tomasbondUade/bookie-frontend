package org.bookie.app.presentation.library

import org.bookie.app.domain.model.LibraryBookState
import org.bookie.app.domain.model.LibraryStatus

/** Every displayed page explicitly declares whether it is server-confirmed or stale. */
data class LibrarySnapshot(
    val entries: List<LibraryBookState>,
    val page: Int,
    val totalPages: Int,
    val total: Int,
    val stale: Boolean,
    val isLoadingMore: Boolean = false,
    val warning: String? = null,
) {
    val canLoadMore: Boolean get() = page < totalPages
}

sealed interface LibraryContentState {
    data object Idle : LibraryContentState
    data class Loading(val previous: LibrarySnapshot? = null) : LibraryContentState
    data class Ready(val snapshot: LibrarySnapshot) : LibraryContentState
    data class Empty(val stale: Boolean = false, val warning: String? = null) : LibraryContentState
    data class Error(val message: String) : LibraryContentState
}

sealed interface LibraryActionState {
    data object Idle : LibraryActionState
    data class Working(val label: String) : LibraryActionState
    data class Succeeded(val message: String) : LibraryActionState
    data class Failed(val message: String) : LibraryActionState
}

data class LibraryExperienceUiState(
    val content: LibraryContentState = LibraryContentState.Idle,
    val statusFilter: LibraryStatus? = null,
    val favoritesOnly: Boolean = false,
    val action: LibraryActionState = LibraryActionState.Idle,
    val pendingRemoval: LibraryBookState? = null,
)

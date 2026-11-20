package org.bookie.app.presentation.lists

import org.bookie.app.domain.model.Book
import org.bookie.app.domain.model.BookList

data class BookListsSnapshot(
    val lists: List<BookList>,
    val page: Int,
    val totalPages: Int,
    val total: Int,
    val loadingMore: Boolean = false,
)

sealed interface BookListsContentState {
    data object Idle : BookListsContentState
    data class Loading(val previous: BookListsSnapshot? = null) : BookListsContentState
    data class Ready(val snapshot: BookListsSnapshot) : BookListsContentState
    data class Failed(val message: String, val signInRequired: Boolean = false) : BookListsContentState
}

sealed interface BookListActionState {
    data object Idle : BookListActionState
    data class Working(val message: String) : BookListActionState
    data class Succeeded(val message: String) : BookListActionState
    data class Failed(val message: String, val signInRequired: Boolean = false) : BookListActionState
}

sealed interface BookListSearchState {
    data object Idle : BookListSearchState
    data object Loading : BookListSearchState
    data class Results(val query: String, val books: List<Book>) : BookListSearchState
    data class Empty(val query: String) : BookListSearchState
    data class Failed(val message: String) : BookListSearchState
}

data class BookListsUiState(
    val content: BookListsContentState = BookListsContentState.Idle,
    val selectedList: BookList? = null,
    val action: BookListActionState = BookListActionState.Idle,
    val pendingDeleteList: BookList? = null,
    val pendingRemoveBookId: String? = null,
    val bookSearch: BookListSearchState = BookListSearchState.Idle,
)

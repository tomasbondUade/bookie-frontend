package org.bookie.app.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.bookie.app.domain.model.BookReview
import org.bookie.app.domain.model.BookReviewSort
import org.bookie.app.domain.model.BookReviewsPage
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.usecase.GetBookReviews

/** Keeps reviews separate from catalog detail and cancels obsolete book/page responses. */
class BookReviewsViewModel(private val getBookReviews: GetBookReviews) : ViewModel() {
    private val mutableState = MutableStateFlow<BookReviewsUiState>(BookReviewsUiState.Idle)
    val uiState = mutableState.asStateFlow()
    private var selectedBookId: String? = null
    private var sort = BookReviewSort.RECENT
    private var version = 0L

    fun selectBook(bookId: String) {
        if (bookId == selectedBookId) return
        selectedBookId = bookId.takeIf(String::isNotBlank)
        sort = BookReviewSort.RECENT
        version++
        mutableState.value = BookReviewsUiState.Idle
        reload()
    }

    fun changeSort(next: BookReviewSort) {
        if (next == sort) return
        sort = next
        version++
        mutableState.value = BookReviewsUiState.Idle
        reload()
    }

    fun retry() = reload()

    fun loadMore() {
        val current = mutableState.value as? BookReviewsUiState.Ready ?: return
        if (current.loadingMore || !current.hasMore) return
        fetch(page = current.page + 1, append = true)
    }

    private fun reload() {
        if (selectedBookId == null) return
        fetch(page = 1, append = false)
    }

    private fun fetch(page: Int, append: Boolean) {
        val bookId = selectedBookId ?: return
        val current = mutableState.value as? BookReviewsUiState.Ready
        if (append && (current == null || current.loadingMore)) return
        val expected = ++version
        if (append) mutableState.value = current!!.copy(loadingMore = true, pageError = null)
        else mutableState.value = BookReviewsUiState.Loading

        viewModelScope.launch {
            val result = try {
                getBookReviews(bookId = bookId, page = page, sort = sort)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                Resource.Error(error.message ?: "No se pudieron cargar las reseñas.")
            }
            if (expected != version || selectedBookId != bookId) return@launch

            when (result) {
                is Resource.Success -> {
                    val response = result.data
                    val items = if (append) {
                        (current?.reviews.orEmpty() + response.reviews).distinctBy(BookReview::id)
                    } else response.reviews
                    mutableState.value = if (!append && response.total == 0) {
                        BookReviewsUiState.Empty
                    } else {
                        BookReviewsUiState.Ready(
                            reviews = items,
                            page = response.page,
                            totalPages = response.totalPages,
                            total = response.total,
                            sort = sort,
                        )
                    }
                }
                is Resource.Error -> mutableState.value =
                    if (append && current != null) {
                        current.copy(pageError = result.message)
                    } else BookReviewsUiState.Error(result.message)
            }
        }
    }
}

sealed interface BookReviewsUiState {
    data object Idle : BookReviewsUiState
    data object Loading : BookReviewsUiState
    data object Empty : BookReviewsUiState
    data class Error(val message: String) : BookReviewsUiState
    data class Ready(
        val reviews: List<BookReview>,
        val page: Int,
        val totalPages: Int,
        val total: Int,
        val sort: BookReviewSort,
        val loadingMore: Boolean = false,
        val pageError: String? = null,
    ) : BookReviewsUiState {
        val hasMore: Boolean get() = page < totalPages
    }
}

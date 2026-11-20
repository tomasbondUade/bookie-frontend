package org.bookie.app.presentation.tinder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.bookie.app.domain.model.SwipeType
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind
import org.bookie.app.domain.usecase.GetBookTinderFeed
import org.bookie.app.domain.usecase.RecordBookSwipe

class BookTinderViewModel(
    private val getBookTinderFeed: GetBookTinderFeed,
    private val recordBookSwipe: RecordBookSwipe,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow<BookTinderUiState>(BookTinderUiState.Loading)
    val uiState = mutableUiState.asStateFlow()

    private var feedJob: Job? = null
    private var swipeJob: Job? = null

    init {
        loadFeed()
    }

    fun loadFeed() {
        if (feedJob?.isActive == true || swipeJob?.isActive == true) return
        mutableUiState.value = BookTinderUiState.Loading
        feedJob = viewModelScope.launch {
            when (val result = safely { getBookTinderFeed(FEED_SIZE) }) {
                is Resource.Success -> {
                    val feed = result.data
                    mutableUiState.value = if (feed.books.isEmpty()) {
                        BookTinderUiState.Empty(feed.source)
                    } else {
                        BookTinderUiState.Ready(feed)
                    }
                }
                is Resource.Error -> mutableUiState.value = result.toUiError()
            }
        }
    }

    fun swipe(type: SwipeType) {
        val current = mutableUiState.value as? BookTinderUiState.Ready ?: return
        if (current.submitting || swipeJob?.isActive == true) return
        val recommendation = current.feed.books.getOrNull(current.currentIndex) ?: return
        mutableUiState.value = current.copy(submitting = true, actionError = null)

        swipeJob = viewModelScope.launch {
            when (val result = safely { recordBookSwipe(recommendation.book.id, type) }) {
                is Resource.Success -> {
                    val nextIndex = current.currentIndex + 1
                    mutableUiState.value = if (nextIndex >= current.feed.books.size) {
                        BookTinderUiState.Exhausted(current.feed.source)
                    } else {
                        current.copy(currentIndex = nextIndex, submitting = false, actionError = null)
                    }
                }
                is Resource.Error -> {
                    mutableUiState.value = if (result.kind == ResourceErrorKind.UNAUTHORIZED) {
                        BookTinderUiState.SignInRequired(result.message)
                    } else {
                        current.copy(submitting = false, actionError = result.message)
                    }
                }
            }
        }
    }

    private suspend fun <T> safely(block: suspend () -> Resource<T>): Resource<T> = try {
        block()
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (error: Exception) {
        Resource.Error(error.message ?: "No se pudo completar la operación.")
    }

    private fun Resource.Error.toUiError(): BookTinderUiState =
        if (kind == ResourceErrorKind.UNAUTHORIZED) {
            BookTinderUiState.SignInRequired(message)
        } else {
            BookTinderUiState.Error(message)
        }

    private companion object {
        const val FEED_SIZE = 10
    }
}

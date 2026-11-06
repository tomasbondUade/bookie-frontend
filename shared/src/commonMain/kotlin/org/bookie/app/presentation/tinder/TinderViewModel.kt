package org.bookie.app.presentation.tinder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.bookie.app.domain.model.*
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind
import org.bookie.app.domain.usecase.GetTinderBooks
import org.bookie.app.domain.usecase.RegisterBookSwipe

class TinderViewModel(
    private val getTinderBooks: GetTinderBooks,
    private val registerSwipe: RegisterBookSwipe
) : ViewModel() {
    private val _uiState = MutableStateFlow<TinderUiState>(TinderUiState.Loading)
    val uiState = _uiState.asStateFlow()
    private var books: List<TinderRecommendedBook> = emptyList()
    private var index = 0
    private var loading = false
    private var submitting = false

    init { load() }

    fun load() {
        if (loading || submitting) return
        loading = true
        _uiState.value = TinderUiState.Loading
        viewModelScope.launch {
            try {
                when (val result = getTinderBooks()) {
                    is Resource.Success -> {
                        books = result.data.books
                        index = 0
                        showCurrent()
                    }
                    is Resource.Error -> _uiState.value = TinderUiState.Error(
                        result.message, result.kind == ResourceErrorKind.UNAUTHORIZED
                    )
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                _uiState.value = TinderUiState.Error(error.message ?: "No se pudo cargar el feed")
            } finally {
                loading = false
            }
        }
    }

    fun like() = submit(SwipeType.LIKE)
    fun dislike() = submit(SwipeType.DISLIKE)

    private fun submit(type: SwipeType) {
        if (submitting) return
        val current = _uiState.value as? TinderUiState.ShowingBook ?: return
        submitting = true
        _uiState.value = current.copy(sending = true, error = null)
        viewModelScope.launch {
            try {
                when (val result = registerSwipe(BookSwipe(current.book.id, type))) {
                    is Resource.Success -> {
                        if (result.data.success) {
                            index++
                            showCurrent()
                        } else {
                            _uiState.value = current.copy(error = result.data.message)
                        }
                    }
                    is Resource.Error -> {
                        _uiState.value = if (result.kind == ResourceErrorKind.UNAUTHORIZED) {
                            TinderUiState.Error(result.message, unauthorized = true)
                        } else current.copy(error = result.message)
                    }
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                _uiState.value = current.copy(error = error.message ?: "No se pudo guardar la decisión")
            } finally {
                submitting = false
            }
        }
    }

    private fun showCurrent() {
        val next = books.getOrNull(index)
        _uiState.value = if (next == null) TinderUiState.Finished else TinderUiState.ShowingBook(
            book = next.book, reasons = next.reasons, position = index + 1, total = books.size
        )
    }
}

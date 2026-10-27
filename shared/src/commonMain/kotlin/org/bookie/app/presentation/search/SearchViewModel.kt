package org.bookie.app.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.bookie.app.domain.model.Book
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.usecase.SearchBooks

class SearchViewModel(
    private val searchBooks: SearchBooks,
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private var latestRequestId = 0L

    fun onQueryChanged(query: String) {
        _query.value = query
        latestRequestId++
        _uiState.value = SearchUiState.Idle
    }

    fun search() {
        if (_uiState.value is SearchUiState.Loading) return

        val normalizedQuery = _query.value.trim()
        if (normalizedQuery.isEmpty()) {
            latestRequestId++
            _uiState.value = SearchUiState.Idle
            return
        }

        val requestId = ++latestRequestId
        _uiState.value = SearchUiState.Loading

        viewModelScope.launch {
            val resultState = try {
                when (val result = searchBooks(normalizedQuery)) {
                    is Resource.Success -> {
                        if (result.data.isEmpty()) {
                            SearchUiState.Empty(normalizedQuery)
                        } else {
                            SearchUiState.Success(books = result.data, query = normalizedQuery)
                        }
                    }

                    is Resource.Error -> SearchUiState.Error(result.message, normalizedQuery)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                SearchUiState.Error(
                    message = exception.message ?: "No se pudo completar la búsqueda.",
                    query = normalizedQuery,
                )
            }

            if (requestId == latestRequestId) {
                _uiState.value = resultState
            }
        }
    }

    fun selectBook(book: Book) {
        val currentState = _uiState.value
        if (currentState is SearchUiState.Success && book in currentState.books) {
            _uiState.value = currentState.copy(selectedBook = book)
        }
    }

    fun showResults() {
        val currentState = _uiState.value
        if (currentState is SearchUiState.Success && currentState.selectedBook != null) {
            _uiState.value = currentState.copy(selectedBook = null)
        }
    }
}

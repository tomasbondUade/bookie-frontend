package org.bookie.app.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
    private var searchJob: Job? = null

    fun onQueryChanged(query: String) {
        if (_query.value == query) return
        searchJob?.cancel()
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

        searchJob = viewModelScope.launch {
            val resultState = try {
                when (val result = searchBooks(normalizedQuery, page = 1)) {
                    is Resource.Success -> {
                        if (result.data.isEmpty()) {
                            SearchUiState.Empty(normalizedQuery)
                        } else {
                            SearchUiState.Success(
                                books = deduplicateBooks(result.data),
                                query = normalizedQuery,
                                hasMore = result.data.isNotEmpty(),
                            )
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

    fun loadMore() {
        val currentState = _uiState.value as? SearchUiState.Success ?: return
        if (!currentState.hasMore || currentState.loadingMore || currentState.selectedBook != null) return

        val nextPage = currentState.page + 1
        val requestId = latestRequestId
        _uiState.value = currentState.copy(loadingMore = true, loadMoreError = null)

        viewModelScope.launch {
            val pageResult = try {
                searchBooks(currentState.query, page = nextPage)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Resource.Error(exception.message ?: "No se pudieron cargar más libros.")
            }

            val latestState = _uiState.value as? SearchUiState.Success ?: return@launch
            if (
                requestId == latestRequestId &&
                latestState.query == currentState.query &&
                _query.value.trim() == currentState.query
            ) {
                _uiState.value = when (pageResult) {
                    is Resource.Success -> {
                        val books = deduplicateBooks(latestState.books + pageResult.data)
                        latestState.copy(
                            books = books,
                            page = nextPage,
                            hasMore = pageResult.data.isNotEmpty(),
                            loadingMore = false,
                            loadMoreError = null,
                        )
                    }
                    is Resource.Error -> latestState.copy(
                        loadingMore = false,
                        loadMoreError = pageResult.message,
                    )
                }
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

    private fun deduplicateBooks(books: List<Book>): List<Book> {
        val seen = mutableSetOf<String>()
        return books.filter { book ->
            val title = book.title.lowercase().filter(Char::isLetterOrDigit)
            val authors = book.authors
                .map { it.lowercase().filter(Char::isLetterOrDigit) }
                .filter(String::isNotEmpty)
                .sorted()
                .joinToString("|")
            val key = if (title.isEmpty()) book.id else "$title|$authors"
            seen.add(key)
        }
    }
}

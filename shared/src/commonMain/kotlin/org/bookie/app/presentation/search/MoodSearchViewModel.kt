package org.bookie.app.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.bookie.app.domain.model.LibraryStateInput
import org.bookie.app.domain.model.LibraryStatus
import org.bookie.app.domain.model.MoodBook
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.usecase.SaveLibraryState
import org.bookie.app.domain.usecase.SearchBooksByMood

class MoodSearchViewModel(
    private val searchBooksByMood: SearchBooksByMood,
    private val saveLibraryState: SaveLibraryState,
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _uiState = MutableStateFlow<MoodSearchUiState>(MoodSearchUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _saveState = MutableStateFlow<MoodSaveUiState>(MoodSaveUiState.Idle)
    val saveState = _saveState.asStateFlow()

    private var requestVersion = 0L

    fun onQueryChanged(value: String) {
        _query.value = value
        requestVersion++
        _uiState.value = MoodSearchUiState.Idle
        _saveState.value = MoodSaveUiState.Idle
    }

    fun search() {
        if (_uiState.value is MoodSearchUiState.Loading) return
        val normalized = _query.value.trim()
        if (normalized.isEmpty()) {
            requestVersion++
            _uiState.value = MoodSearchUiState.Idle
            return
        }

        val version = ++requestVersion
        _uiState.value = MoodSearchUiState.Loading
        _saveState.value = MoodSaveUiState.Idle
        viewModelScope.launch {
            val nextState = try {
                when (val result = searchBooksByMood(normalized)) {
                    is Resource.Success -> if (result.data.isEmpty()) {
                        MoodSearchUiState.Empty
                    } else {
                        MoodSearchUiState.Success(result.data)
                    }
                    is Resource.Error -> MoodSearchUiState.Error(result.message)
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                MoodSearchUiState.Error(error.message ?: "No se pudieron buscar libros.")
            }
            if (version == requestVersion) _uiState.value = nextState
        }
    }

    fun selectBook(book: MoodBook) {
        val current = _uiState.value as? MoodSearchUiState.Success ?: return
        if (book in current.books) {
            _uiState.value = current.copy(selectedBook = book)
            _saveState.value = MoodSaveUiState.Idle
        }
    }

    fun showResults() {
        val current = _uiState.value as? MoodSearchUiState.Success ?: return
        _uiState.value = current.copy(selectedBook = null)
        _saveState.value = MoodSaveUiState.Idle
    }

    fun saveSelectedBook() {
        val selected = (_uiState.value as? MoodSearchUiState.Success)?.selectedBook ?: return
        if (_saveState.value is MoodSaveUiState.Saving || _saveState.value is MoodSaveUiState.Saved) return
        val bookId = selected.book.id // Internal backend ID, not Google Books ID.
        _saveState.value = MoodSaveUiState.Saving

        viewModelScope.launch {
            val result = try {
                saveLibraryState(
                    LibraryStateInput(bookId = bookId, status = LibraryStatus.TO_READ),
                )
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                Resource.Error(error.message ?: "No se pudo guardar el libro.")
            }
            val stillSelected = (_uiState.value as? MoodSearchUiState.Success)
                ?.selectedBook?.book?.id == bookId
            if (stillSelected) {
                // Only report saved after the backend confirms the write.
                _saveState.value = when (result) {
                    is Resource.Success -> MoodSaveUiState.Saved
                    is Resource.Error -> MoodSaveUiState.Error(result.message)
                }
            }
        }
    }
}

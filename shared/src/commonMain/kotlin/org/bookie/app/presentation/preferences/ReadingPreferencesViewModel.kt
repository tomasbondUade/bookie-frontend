package org.bookie.app.presentation.preferences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.bookie.app.domain.model.Book
import org.bookie.app.domain.model.ReadingPreferences
import org.bookie.app.domain.model.ReadingPreferencesInput
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind
import org.bookie.app.domain.usecase.GetReadingPreferences
import org.bookie.app.domain.usecase.SaveReadingPreferences
import org.bookie.app.domain.usecase.SearchBooks

class ReadingPreferencesViewModel(
    private val getPreferences: GetReadingPreferences,
    private val savePreferences: SaveReadingPreferences,
    private val searchBooks: SearchBooks,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow<ReadingPreferencesUiState>(ReadingPreferencesUiState.Idle)
    val uiState = mutableUiState.asStateFlow()

    private var activeAccountId: String? = null
    private var searchRequestId = 0L
    private var accountGeneration = 0L

    fun loadForAccount(accountId: String) {
        if (accountId.isBlank()) return
        if (activeAccountId != accountId) {
            activeAccountId = accountId
            accountGeneration++
            searchRequestId++
            mutableUiState.value = ReadingPreferencesUiState.Idle
        }
        if (mutableUiState.value == ReadingPreferencesUiState.Idle) load()
    }

    fun clearForSignedOut() {
        activeAccountId = null
        accountGeneration++
        searchRequestId++
        mutableUiState.value = ReadingPreferencesUiState.Idle
    }

    fun load() {
        if (mutableUiState.value == ReadingPreferencesUiState.Loading) return
        val generation = accountGeneration
        mutableUiState.value = ReadingPreferencesUiState.Loading
        viewModelScope.launch {
            val result = try {
                getPreferences()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Resource.Error(exception.message ?: "No se pudieron cargar tus preferencias.")
            }
            if (generation != accountGeneration) return@launch
            mutableUiState.value = when (result) {
                is Resource.Success -> result.data.toUiState()
                is Resource.Error -> ReadingPreferencesUiState.LoadError(
                    message = if (result.kind == ResourceErrorKind.UNAUTHORIZED) {
                        "Tu sesión venció. Iniciá sesión nuevamente."
                    } else {
                        result.message
                    },
                    sessionExpired = result.kind == ResourceErrorKind.UNAUTHORIZED,
                )
            }
        }
    }

    fun retryLoad() = load()

    fun beginEdit() {
        val current = mutableUiState.value as? ReadingPreferencesUiState.Viewing ?: return
        mutableUiState.value = current.preferences.toEditing(isOnboarding = false)
    }

    fun cancelEdit() {
        val draft = mutableUiState.value.editingOrNull() ?: return
        if (!draft.isOnboarding) {
            mutableUiState.value = ReadingPreferencesUiState.Viewing(draft.baseline)
        }
    }

    fun updateGenreInput(value: String) = updateDraft { copy(genreInput = value, error = null) }

    fun updateAuthorInput(value: String) = updateDraft { copy(authorInput = value, error = null) }

    fun updateSearchQuery(value: String) {
        searchRequestId++
        updateDraft { copy(searchQuery = value, searchState = BookSearchState.Idle, error = null) }
    }

    fun addGenres() = addValues(
        input = { genreInput },
        current = { preferredGenres },
        onAdded = { values -> copy(preferredGenres = values, genreInput = "", error = null) },
    )

    fun addAuthors() = addValues(
        input = { authorInput },
        current = { preferredAuthors },
        onAdded = { values -> copy(preferredAuthors = values, authorInput = "", error = null) },
    )

    fun removeGenre(value: String) = updateDraft { copy(preferredGenres = preferredGenres - value) }

    fun removeAuthor(value: String) = updateDraft { copy(preferredAuthors = preferredAuthors - value) }

    fun search() {
        val draft = mutableUiState.value.editingOrNull() ?: return
        val query = draft.searchQuery.trim()
        if (query.isEmpty()) {
            updateDraft { copy(searchState = BookSearchState.Idle) }
            return
        }

        val requestId = ++searchRequestId
        updateDraft { copy(searchState = BookSearchState.Loading) }
        viewModelScope.launch {
            val result = try {
                searchBooks(query)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Resource.Error(exception.message ?: "No se pudo buscar el libro.")
            }
            if (requestId != searchRequestId) return@launch
            updateDraft {
                copy(
                    searchState = when (result) {
                        is Resource.Success -> result.data.takeIf { it.isNotEmpty() }
                            ?.let(BookSearchState::Results) ?: BookSearchState.Empty
                        is Resource.Error -> BookSearchState.Error(result.message)
                    },
                )
            }
        }
    }

    fun retrySearch() = search()

    fun toggleFavorite(book: Book) = updateDraft {
        if (book.id in favoriteBookIds) {
            copy(
                favoriteBookIds = favoriteBookIds - book.id,
                selectedBooks = selectedBooks.filterNot { it.id == book.id },
            )
        } else {
            copy(
                favoriteBookIds = favoriteBookIds + book.id,
                selectedBooks = selectedBooks + book,
            )
        }
    }

    fun removeUnresolvedFavorites() = updateDraft {
        val knownIds = selectedBooks.mapTo(mutableSetOf()) { it.id }
        copy(favoriteBookIds = favoriteBookIds.filter { it in knownIds })
    }

    fun save() {
        val draft = mutableUiState.value.editingOrNull() ?: return
        if (draft.preferredGenres.isEmpty() && draft.preferredAuthors.isEmpty() && draft.favoriteBookIds.isEmpty()) {
            mutableUiState.value = draft.copy(error = "Elegí al menos un género, autor o libro favorito.")
            return
        }

        mutableUiState.value = ReadingPreferencesUiState.Saving(draft)
        val generation = accountGeneration
        viewModelScope.launch {
            val result = try {
                savePreferences(
                    ReadingPreferencesInput(
                        preferredGenres = draft.preferredGenres,
                        preferredAuthors = draft.preferredAuthors,
                        favoriteBookIds = draft.favoriteBookIds,
                    ),
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Resource.Error(exception.message ?: "No se pudieron guardar tus preferencias.")
            }
            if (generation != accountGeneration) return@launch
            mutableUiState.value = when (result) {
                is Resource.Success -> {
                    if (result.data.onboardingCompleted) {
                        ReadingPreferencesUiState.Viewing(
                            preferences = result.data,
                            notice = "Tus preferencias se guardaron.",
                        )
                    } else {
                        draft.copy(error = "El servidor no confirmó el fin del onboarding.")
                    }
                }
                is Resource.Error -> draft.copy(error = result.message)
            }
        }
    }

    private fun ReadingPreferences.toEditing(isOnboarding: Boolean) = ReadingPreferencesUiState.Editing(
        baseline = this,
        preferredGenres = preferredGenres,
        preferredAuthors = preferredAuthors,
        favoriteBookIds = favoriteBookIds,
        selectedBooks = emptyList(),
        isOnboarding = isOnboarding,
    )

    private fun ReadingPreferences.toUiState(): ReadingPreferencesUiState =
        if (onboardingCompleted) {
            ReadingPreferencesUiState.Viewing(this)
        } else {
            toEditing(isOnboarding = true)
        }

    private fun ReadingPreferencesUiState.editingOrNull(): ReadingPreferencesUiState.Editing? = when (this) {
        is ReadingPreferencesUiState.Editing -> this
        is ReadingPreferencesUiState.Saving -> draft
        else -> null
    }

    private fun updateDraft(update: ReadingPreferencesUiState.Editing.() -> ReadingPreferencesUiState.Editing) {
        val draft = mutableUiState.value as? ReadingPreferencesUiState.Editing ?: return
        mutableUiState.value = draft.update()
    }

    private fun addValues(
        input: ReadingPreferencesUiState.Editing.() -> String,
        current: ReadingPreferencesUiState.Editing.() -> List<String>,
        onAdded: ReadingPreferencesUiState.Editing.(List<String>) -> ReadingPreferencesUiState.Editing,
    ) = updateDraft {
        val values = (input().split(',') + current())
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinctBy(String::lowercase)
        onAdded(values)
    }
}

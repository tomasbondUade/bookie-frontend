package org.bookie.app.presentation.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.bookie.app.domain.model.LibraryBookState
import org.bookie.app.domain.model.LibraryStateInput
import org.bookie.app.domain.model.ReadingSessionInput
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.usecase.CreateReadingSession
import org.bookie.app.domain.usecase.GetReadingSessions
import org.bookie.app.domain.usecase.SaveLibraryState

class ReadingProgressViewModel(
    private val libraryState: LibraryBookState,
    private val saveLibraryState: SaveLibraryState,
    private val createReadingSession: CreateReadingSession,
    private val getReadingSessions: GetReadingSessions,
    private val keyGenerator: SessionIdempotencyKeyGenerator = RandomSessionIdempotencyKeyGenerator(),
) : ViewModel() {
    private val _uiState = MutableStateFlow<ReadingProgressUiState>(
        if (libraryState.id.isBlank() || libraryState.bookId.isBlank()) {
            ReadingProgressUiState.Unavailable("No se encontró el libro en tu biblioteca.")
        } else {
            ReadingProgressUiState.Content(
                bookTitle = libraryState.book.title,
                currentPageInput = libraryState.currentPage?.toString().orEmpty(),
                percentageInput = libraryState.progressPercentage?.toString().orEmpty(),
                privateNotesInput = libraryState.privateNotes.orEmpty(),
            )
        },
    )
    val uiState = _uiState.asStateFlow()

    private var privateNotesEdited = false
    private var pendingSession: PendingSession? = null

    fun onCurrentPageChanged(value: String) {
        val current = content() ?: return
        if (current.isBusy) return
        pendingSession = null
        _uiState.value = current.copy(
            currentPageInput = value,
            currentPageError = null,
            snapshotStatus = ProgressOperationStatus.Idle,
            sessionStatus = ProgressOperationStatus.Idle,
        )
    }

    fun onPercentageChanged(value: String) {
        val current = content() ?: return
        if (current.isBusy) return
        pendingSession = null
        _uiState.value = current.copy(
            percentageInput = value,
            percentageError = null,
            snapshotStatus = ProgressOperationStatus.Idle,
            sessionStatus = ProgressOperationStatus.Idle,
        )
    }

    fun onPrivateNotesChanged(value: String) {
        val current = content() ?: return
        if (current.isBusy) return
        privateNotesEdited = true
        _uiState.value = current.copy(
            privateNotesInput = value,
            snapshotStatus = ProgressOperationStatus.Idle,
        )
    }

    fun onSessionNotesChanged(value: String) {
        val current = content() ?: return
        if (current.isBusy) return
        pendingSession = null
        _uiState.value = current.copy(
            sessionNotesInput = value,
            sessionStatus = ProgressOperationStatus.Idle,
        )
    }

    fun saveSnapshot() {
        val current = content() ?: return
        if (current.isBusy) return
        val parsed = validate(current)
        if (showValidationErrors(current, parsed)) return
        if (parsed.page == null && parsed.percentage == null && !privateNotesEdited) {
            _uiState.value = current.copy(
                snapshotStatus = ProgressOperationStatus.Error("Ingresá una página, un porcentaje o una nota privada."),
            )
            return
        }
        val input = LibraryStateInput(
            bookId = libraryState.bookId,
            currentPage = parsed.page,
            progressPercentage = parsed.percentage,
            // A null field means "unchanged" to the API. Do not erase private
            // notes when the initial model came from an offline cache.
            privateNotes = if (privateNotesEdited) current.privateNotesInput else null,
        )
        _uiState.value = current.copy(snapshotStatus = ProgressOperationStatus.Loading)
        viewModelScope.launch {
            val result = try {
                saveLibraryState(input)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (exception: Exception) {
                Resource.Error(exception.message ?: "No se pudo guardar el progreso.")
            }
            val latest = content() ?: return@launch
            when (result) {
                is Resource.Success -> {
                    privateNotesEdited = false
                    _uiState.value = latest.copy(
                        snapshotStatus = ProgressOperationStatus.Success("Progreso guardado en el servidor."),
                    )
                }
                is Resource.Error -> _uiState.value = latest.copy(
                    snapshotStatus = ProgressOperationStatus.Error(result.message),
                )
            }
        }
    }

    fun recordSession() {
        val current = content() ?: return
        if (current.isBusy) return
        val parsed = validate(current)
        if (showValidationErrors(current, parsed)) return
        val input = ReadingSessionInput(
            currentPage = parsed.page,
            progressPercentage = parsed.percentage,
            notes = current.sessionNotesInput.trim().takeIf(String::isNotEmpty),
        )
        if (input.currentPage == null && input.progressPercentage == null && input.notes == null) {
            _uiState.value = current.copy(
                sessionStatus = ProgressOperationStatus.Error("Ingresá un avance o una nota de sesión."),
            )
            return
        }
        val attempt = pendingSession?.takeIf { it.input == input }
            ?: PendingSession(input, keyGenerator.newKey()).also { pendingSession = it }
        _uiState.value = current.copy(sessionStatus = ProgressOperationStatus.Loading)
        viewModelScope.launch {
            val result = try {
                createReadingSession(libraryState.id, attempt.input, attempt.key)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (exception: Exception) {
                Resource.Error(exception.message ?: "No se pudo registrar la sesión.")
            }
            val latest = content() ?: return@launch
            when (result) {
                is Resource.Success -> {
                    pendingSession = null
                    _uiState.value = latest.copy(
                        sessionStatus = ProgressOperationStatus.Success("Sesión registrada en el servidor."),
                        sessionNotesInput = "",
                        sessions = listOf(result.data) + latest.sessions.filterNot { it.id == result.data.id },
                    )
                }
                is Resource.Error -> _uiState.value = latest.copy(
                    sessionStatus = ProgressOperationStatus.Error(result.message),
                )
            }
        }
    }

    /**
     * Explicitly abandon an uncertain failed request. Reusing its payload with
     * a new key could create a second session if the first reached the server.
     */
    fun startNewSessionAttempt() {
        val current = content() ?: return
        if (current.isBusy) return
        pendingSession = null
        _uiState.value = current.copy(sessionStatus = ProgressOperationStatus.Idle)
    }

    fun loadSessions() {
        val current = content() ?: return
        if (current.historyLoading) return
        _uiState.value = current.copy(historyLoading = true, historyError = null)
        viewModelScope.launch {
            val result = try {
                getReadingSessions(libraryState.id)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (exception: Exception) {
                Resource.Error(exception.message ?: "No se pudo consultar el historial.")
            }
            val latest = content() ?: return@launch
            _uiState.value = when (result) {
                is Resource.Success -> latest.copy(
                    sessions = result.data,
                    historyLoading = false,
                    historyError = null,
                )
                is Resource.Error -> latest.copy(
                    historyLoading = false,
                    historyError = result.message,
                )
            }
        }
    }

    private fun content(): ReadingProgressUiState.Content? =
        _uiState.value as? ReadingProgressUiState.Content

    private fun showValidationErrors(
        current: ReadingProgressUiState.Content,
        parsed: ParsedProgress,
    ): Boolean {
        if (parsed.pageError == null && parsed.percentageError == null) return false
        _uiState.value = current.copy(
            currentPageError = parsed.pageError,
            percentageError = parsed.percentageError,
        )
        return true
    }

    private fun validate(current: ReadingProgressUiState.Content): ParsedProgress {
        val pageText = current.currentPageInput.trim()
        val percentageText = current.percentageInput.trim()
        val page = pageText.takeIf(String::isNotEmpty)?.toIntOrNull()
        val percentage = percentageText.takeIf(String::isNotEmpty)?.toIntOrNull()
        return ParsedProgress(
            page = page,
            percentage = percentage,
            pageError = if (pageText.isNotEmpty() && (page == null || page < 0)) {
                "La página debe ser un número entero mayor o igual a 0."
            } else null,
            percentageError = if (percentageText.isNotEmpty() && (percentage == null || percentage !in 0..100)) {
                "El porcentaje debe ser un número entero entre 0 y 100."
            } else null,
        )
    }

    private data class ParsedProgress(
        val page: Int?,
        val percentage: Int?,
        val pageError: String?,
        val percentageError: String?,
    )

    private data class PendingSession(
        val input: ReadingSessionInput,
        val key: String,
    )
}

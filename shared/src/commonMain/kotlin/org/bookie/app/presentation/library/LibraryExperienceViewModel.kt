package org.bookie.app.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.bookie.app.domain.model.LibraryBookState
import org.bookie.app.domain.model.LibraryLoad
import org.bookie.app.domain.model.LibraryPage
import org.bookie.app.domain.model.LibraryStateInput
import org.bookie.app.domain.model.LibraryStatus
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.usecase.DeleteLibraryState
import org.bookie.app.domain.usecase.ObserveLibraryOnlineFirst
import org.bookie.app.domain.usecase.SaveLibraryState

/**
 * Integration boundary for #32's Compose screen. No HTTP, SQLDelight or DTOs here.
 * Mutations never optimistically modify the visible library or queue offline writes.
 */
class LibraryExperienceViewModel(
    private val observeLibrary: ObserveLibraryOnlineFirst,
    private val saveState: SaveLibraryState,
    private val deleteState: DeleteLibraryState,
) : ViewModel() {
    private val mutableState = MutableStateFlow(LibraryExperienceUiState())
    val uiState = mutableState.asStateFlow()

    private val pages = mutableMapOf<Int, LibraryPage>()
    private val pageFreshness = mutableMapOf<Int, Boolean>()
    private var readJob: Job? = null
    private var mutationJob: Job? = null
    private var readVersion = 0L
    private var lastPageAttempted = 1

    fun refresh() {
        if (mutationJob?.isActive == true) return
        requestPage(page = 1, append = false, preserveVisible = true)
    }

    fun setStatusFilter(status: LibraryStatus?) {
        if (mutableState.value.statusFilter == status) return
        mutableState.value = mutableState.value.copy(statusFilter = status, pendingRemoval = null)
        requestPage(page = 1, append = false, preserveVisible = false)
    }

    fun setFavoritesOnly(onlyFavorites: Boolean) {
        if (mutableState.value.favoritesOnly == onlyFavorites) return
        mutableState.value = mutableState.value.copy(favoritesOnly = onlyFavorites, pendingRemoval = null)
        requestPage(page = 1, append = false, preserveVisible = false)
    }

    /** Select/edit first, then commit one server-confirmed status/progress update. */
    fun selectDraftStatus(stateId: String, status: LibraryStatus) {
        updateDraft(stateId) { it.copy(status = status) }
    }

    fun editDraftPage(stateId: String, text: String) {
        if (text.all(Char::isDigit)) {
            updateDraft(stateId) {
                if (it.canEditProgress) it.copy(pageText = text) else it
            }
        }
    }

    fun editDraftPercentage(stateId: String, text: String) {
        if (text.all(Char::isDigit)) {
            updateDraft(stateId) {
                if (it.canEditProgress) it.copy(percentageText = text) else it
            }
        }
    }

    fun discardDraft(stateId: String) {
        mutableState.value = mutableState.value.copy(
            drafts = mutableState.value.drafts - stateId,
        )
    }

    fun saveDraft(stateId: String) {
        if (mutationJob?.isActive == true) return
        val entry = displayedEntries().firstOrNull { it.id == stateId } ?: return
        val draft = mutableState.value.drafts[stateId] ?: return
        if (!draft.hasChangesFrom(entry)) return

        val pageText = if (draft.canEditProgress) draft.pageText else ""
        val percentageText = if (draft.canEditProgress) draft.percentageText else ""
        val page = pageText.takeIf { it.isNotBlank() }?.toIntOrNull()
        val percentage = percentageText.takeIf { it.isNotBlank() }?.toIntOrNull()
        if ((pageText.isNotBlank() && page == null) ||
            (percentageText.isNotBlank() && (percentage == null || percentage !in 0..100))
        ) {
            actionError("Ingresá una página válida o un porcentaje entre 0 y 100.")
            return
        }

        val input = LibraryStateInput(
            bookId = entry.bookId,
            status = draft.status.takeIf { it != entry.status },
            currentPage = page?.takeIf { it != entry.currentPage },
            progressPercentage = percentage?.takeIf { it != entry.progressPercentage },
        )
        runMutation(
            "Guardando cambios…",
            "Estado y progreso actualizados.",
            onSuccess = { discardDraft(stateId) },
        ) { saveState(input) }
    }

    private fun updateDraft(stateId: String, transform: (LibraryEditDraft) -> LibraryEditDraft) {
        if (mutationJob?.isActive == true) return
        val entry = displayedEntries().firstOrNull { it.id == stateId } ?: return
        val old = mutableState.value.drafts[stateId] ?: LibraryEditDraft.from(entry)
        mutableState.value = mutableState.value.copy(
            drafts = mutableState.value.drafts + (stateId to transform(old)),
        )
    }

    fun loadNextPage() {
        val current = (mutableState.value.content as? LibraryContentState.Ready)?.snapshot ?: return
        if (!current.canLoadMore || current.isLoadingMore || readJob?.isActive == true ||
            mutationJob?.isActive == true
        ) return
        requestPage(page = current.page + 1, append = true, preserveVisible = true)
    }

    fun retry() {
        if (mutationJob?.isActive == true) return
        val current = (mutableState.value.content as? LibraryContentState.Ready)?.snapshot
        if (current?.warning != null && lastPageAttempted > 1 && pages.containsKey(1)) {
            requestPage(page = lastPageAttempted, append = true, preserveVisible = true)
        } else {
            refresh()
        }
    }

    /** Add an existing catalog book; backend owns catalog lookup and defaults. */
    fun addCatalogBook(bookId: String, status: LibraryStatus = LibraryStatus.TO_READ) {
        save(LibraryStateInput(bookId = bookId.trim(), status = status), "Libro agregado a tu biblioteca.")
    }

    fun changeStatus(bookId: String, status: LibraryStatus) {
        save(LibraryStateInput(bookId = bookId.trim(), status = status), "Estado de lectura actualizado.")
    }

    fun updateProgress(bookId: String, currentPage: Int? = null, percentage: Int? = null) {
        val current = displayedEntries().firstOrNull { it.bookId == bookId.trim() }
        if (current?.status != LibraryStatus.READING) {
            actionError("Solo podés modificar el progreso de un libro en estado Leyendo.")
            return
        }
        if (currentPage == null && percentage == null) {
            actionError("Indicá una página o un porcentaje para actualizar.")
            return
        }
        if (currentPage != null && currentPage < 0 || percentage != null && percentage !in 0..100) {
            actionError("La página debe ser positiva y el porcentaje debe estar entre 0 y 100.")
            return
        }
        save(
            LibraryStateInput(
                bookId = bookId.trim(),
                currentPage = currentPage,
                progressPercentage = percentage,
            ),
            "Progreso de lectura actualizado.",
        )
    }

    /**
     * For #58's detail actions: only fields explicitly supplied are sent.
     * A null optional field means 'unchanged', not 'clear on server'.
     */
    fun save(input: LibraryStateInput, successMessage: String = "Cambios guardados.") {
        if (input.bookId.isBlank()) {
            actionError("Seleccioná un libro del catálogo.")
            return
        }
        if (input.currentPage != null || input.progressPercentage != null) {
            val current = displayedEntries().firstOrNull { it.bookId == input.bookId }
            if ((input.status ?: current?.status) != LibraryStatus.READING) {
                actionError("Solo podés modificar el progreso de un libro en estado Leyendo.")
                return
            }
        }
        runMutation("Guardando…", successMessage) { saveState(input) }
    }

    /** Removal requires a second explicit confirmation and uses the personal state ID. */
    fun requestRemoval(stateId: String) {
        if (mutationJob?.isActive == true) return
        val state = displayedEntries().firstOrNull { it.id == stateId } ?: return
        mutableState.value = mutableState.value.copy(pendingRemoval = state)
    }

    fun cancelRemoval() {
        if (mutationJob?.isActive == true) return
        mutableState.value = mutableState.value.copy(pendingRemoval = null)
    }

    fun confirmRemoval() {
        val state = mutableState.value.pendingRemoval ?: return
        runMutation("Quitando libro…", "Libro quitado de tu biblioteca.") {
            deleteState(state.id)
        }
    }

    fun dismissAction() {
        if (mutableState.value.action !is LibraryActionState.Working) {
            mutableState.value = mutableState.value.copy(action = LibraryActionState.Idle)
        }
    }

    private fun requestPage(page: Int, append: Boolean, preserveVisible: Boolean) {
        val previous = snapshot()
        readJob?.cancel()
        val version = ++readVersion
        lastPageAttempted = page
        if (!append) {
            pages.clear()
            pageFreshness.clear()
            mutableState.value = mutableState.value.copy(
                content = LibraryContentState.Loading(
                    if (preserveVisible) previous?.copy(stale = true, isLoadingMore = false) else null,
                ),
            )
        } else {
            val visible = previous ?: return
            mutableState.value = mutableState.value.copy(
                content = LibraryContentState.Ready(
                    visible.copy(isLoadingMore = true, warning = null),
                ),
            )
        }
        val status = mutableState.value.statusFilter
        val favorites = mutableState.value.favoritesOnly
        readJob = viewModelScope.launch {
            try {
                observeLibrary(page, PAGE_SIZE, status, favorites).collect { result ->
                    if (version != readVersion) return@collect
                    when (result) {
                        is LibraryLoad.Cached -> applyPage(page, result.page, stale = true)
                        is LibraryLoad.Fresh -> applyPage(page, result.page, stale = false)
                        is LibraryLoad.Failed -> {
                            if (result.cachedPage != null && page !in pages) {
                                applyPage(page, result.cachedPage, stale = true)
                            }
                            showReadFailure(result.message)
                        }
                    }
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                if (version == readVersion) {
                    showReadFailure(error.message ?: "No se pudo cargar la biblioteca.")
                }
            }
        }
    }

    private fun applyPage(page: Int, result: LibraryPage, stale: Boolean) {
        pages[page] = result
        pageFreshness[page] = !stale
        val entries = pages.entries.sortedBy { it.key }
            .flatMap { it.value.books }
            .distinctBy(LibraryBookState::id)
        val snapshot = LibrarySnapshot(
            entries = entries,
            page = pages.keys.maxOrNull() ?: page,
            totalPages = result.totalPages,
            total = result.total,
            stale = pageFreshness.values.any { !it },
        )
        mutableState.value = mutableState.value.copy(
            content = if (entries.isEmpty()) {
                LibraryContentState.Empty(stale = snapshot.stale)
            } else {
                LibraryContentState.Ready(snapshot)
            },
        )
    }

    private fun showReadFailure(message: String) {
        val current = mutableState.value.content
        val visible = snapshot()
        mutableState.value = mutableState.value.copy(
            content = when {
                visible != null -> LibraryContentState.Ready(
                    visible.copy(
                        stale = visible.stale || current is LibraryContentState.Loading,
                        isLoadingMore = false,
                        warning = message,
                    ),
                )
                current is LibraryContentState.Empty ->
                    current.copy(stale = true, warning = message)
                else -> LibraryContentState.Error(message)
            },
        )
    }

    private fun snapshot(): LibrarySnapshot? = when (val content = mutableState.value.content) {
        is LibraryContentState.Ready -> content.snapshot
        is LibraryContentState.Loading -> content.previous
        else -> null
    }

    private fun displayedEntries(): List<LibraryBookState> = snapshot()?.entries.orEmpty()

    private fun <T> runMutation(
        workingMessage: String,
        successMessage: String,
        onSuccess: () -> Unit = {},
        block: suspend () -> Resource<T>,
    ) {
        if (mutationJob?.isActive == true) return
        mutationJob = viewModelScope.launch {
            mutableState.value = mutableState.value.copy(
                action = LibraryActionState.Working(workingMessage),
            )
            val result: Resource<T> = try {
                block()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                Resource.Error(error.message ?: "No se pudo guardar el cambio.")
            }
            when (result) {
                is Resource.Success -> {
                    onSuccess()
                    mutableState.value = mutableState.value.copy(
                        action = LibraryActionState.Succeeded(successMessage),
                        pendingRemoval = null,
                    )
                    // Clear the visible snapshot after confirmation so removed/changed
                    // entries are not displayed as current while the API reloads.
                    requestPage(page = 1, append = false, preserveVisible = false)
                }
                is Resource.Error -> mutableState.value = mutableState.value.copy(
                    action = LibraryActionState.Failed(result.message),
                )
            }
        }
    }

    private fun actionError(message: String) {
        if (mutationJob?.isActive == true) return
        mutableState.value = mutableState.value.copy(action = LibraryActionState.Failed(message))
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}

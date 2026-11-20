package org.bookie.app.presentation.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.bookie.app.domain.model.BookList
import org.bookie.app.domain.model.BookListPage
import org.bookie.app.domain.model.CreateBookListInput
import org.bookie.app.domain.model.UpdateBookListInput
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind
import org.bookie.app.domain.usecase.AddBookToList
import org.bookie.app.domain.usecase.CreateBookList
import org.bookie.app.domain.usecase.DeleteBookList
import org.bookie.app.domain.usecase.GetBookLists
import org.bookie.app.domain.usecase.RemoveBookFromList
import org.bookie.app.domain.usecase.SearchBooks
import org.bookie.app.domain.usecase.UpdateBookList

class BookListsViewModel(
    private val getBookLists: GetBookLists,
    private val createBookList: CreateBookList,
    private val updateBookList: UpdateBookList,
    private val deleteBookList: DeleteBookList,
    private val addBookToList: AddBookToList,
    private val removeBookFromList: RemoveBookFromList,
    private val searchBooksUseCase: SearchBooks,
) : ViewModel() {
    private val mutableState = MutableStateFlow(BookListsUiState())
    val uiState = mutableState.asStateFlow()

    private var loadJob: Job? = null
    private var mutationJob: Job? = null
    private var searchJob: Job? = null
    private var loadVersion = 0L
    private var searchVersion = 0L

    fun load(page: Int = 1) {
        if (mutationJob?.isActive == true || loadJob?.isActive == true) return
        val current = mutableState.value
        val previous = (current.content as? BookListsContentState.Ready)?.snapshot
            ?: (current.content as? BookListsContentState.Loading)?.previous
        val safePage = page.coerceAtLeast(1)
        val version = ++loadVersion
        mutableState.value = current.copy(
            content = if (safePage > 1 && previous != null) {
                BookListsContentState.Ready(previous.copy(loadingMore = true))
            } else {
                BookListsContentState.Loading(previous)
            },
        )
        loadJob = viewModelScope.launch {
            val result = safely { getBookLists(safePage, PAGE_SIZE) }
            if (version != loadVersion) return@launch
            when (result) {
                is Resource.Success -> {
                    val resultPage = result.data
                    val lists = if (safePage > 1 && previous != null) {
                        (previous.lists + resultPage.lists).distinctBy(BookList::id)
                    } else resultPage.lists
                    val snapshot = BookListsSnapshot(
                        lists = lists,
                        page = resultPage.page,
                        totalPages = resultPage.totalPages,
                        total = resultPage.total,
                    )
                    val selectedId = mutableState.value.selectedList?.id
                    mutableState.value = mutableState.value.copy(
                        content = BookListsContentState.Ready(snapshot),
                        selectedList = lists.firstOrNull { it.id == selectedId },
                    )
                }
                is Resource.Error -> {
                    mutableState.value = mutableState.value.copy(
                        content = BookListsContentState.Failed(
                            message = result.message,
                            signInRequired = result.kind == ResourceErrorKind.UNAUTHORIZED,
                        ),
                    )
                }
            }
        }
    }

    fun loadNextPage() {
        val snapshot = (mutableState.value.content as? BookListsContentState.Ready)?.snapshot ?: return
        if (snapshot.page < snapshot.totalPages && !snapshot.loadingMore) load(snapshot.page + 1)
    }

    fun selectList(listId: String) {
        val list = currentLists().firstOrNull { it.id == listId } ?: return
        mutableState.value = mutableState.value.copy(selectedList = list, pendingRemoveBookId = null)
    }

    fun closeList() {
        mutableState.value = mutableState.value.copy(selectedList = null, pendingRemoveBookId = null)
        dismissAction()
    }

    fun create(name: String, description: String, isPublic: Boolean) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) {
            failAction("El nombre de la lista es obligatorio.")
            return
        }
        mutate("Creando lista…", "Lista creada.") {
            createBookList(CreateBookListInput(cleanName, description.trim().ifEmpty { null }, isPublic))
        }
    }

    fun updateSelected(name: String, description: String, isPublic: Boolean) {
        val selected = mutableState.value.selectedList ?: return
        val cleanName = name.trim()
        if (cleanName.isEmpty()) {
            failAction("El nombre de la lista es obligatorio.")
            return
        }
        mutate("Guardando cambios…", "Lista actualizada.") {
            updateBookList(
                selected.id,
                UpdateBookListInput(cleanName, description.trim().ifEmpty { null }, isPublic),
            )
        }
    }

    fun requestDelete(listId: String) {
        if (mutationJob?.isActive == true) return
        val list = currentLists().firstOrNull { it.id == listId } ?: return
        mutableState.value = mutableState.value.copy(pendingDeleteList = list)
    }

    fun cancelDelete() {
        mutableState.value = mutableState.value.copy(pendingDeleteList = null)
    }

    fun confirmDelete() {
        val list = mutableState.value.pendingDeleteList ?: return
        mutate("Eliminando lista…", "Lista eliminada.", afterSuccess = {
            mutableState.value = mutableState.value.copy(
                selectedList = null,
                pendingDeleteList = null,
            )
        }) { deleteBookList(list.id) }
    }

    fun searchBooks(query: String) {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) {
            mutableState.value = mutableState.value.copy(bookSearch = BookListSearchState.Idle)
            return
        }
        val version = ++searchVersion
        searchJob?.cancel()
        mutableState.value = mutableState.value.copy(bookSearch = BookListSearchState.Loading)
        searchJob = viewModelScope.launch {
            val result = safely { searchBooksUseCase(cleanQuery) }
            if (version != searchVersion) return@launch
            mutableState.value = mutableState.value.copy(
                bookSearch = when (result) {
                    is Resource.Success -> if (result.data.isEmpty()) {
                        BookListSearchState.Empty(cleanQuery)
                    } else BookListSearchState.Results(cleanQuery, result.data)
                    is Resource.Error -> BookListSearchState.Failed(result.message)
                },
            )
        }
    }

    fun addBook(bookId: String) {
        val list = mutableState.value.selectedList ?: return
        if (bookId.isBlank()) return
        if (list.items.orEmpty().any { it.bookId == bookId }) {
            failAction("Ese libro ya está en la lista.")
            return
        }
        mutate("Agregando libro…", "Libro agregado a la lista.") {
            addBookToList(list.id, bookId)
        }
    }

    fun requestRemoveBook(bookId: String) {
        val list = mutableState.value.selectedList ?: return
        if (list.items.orEmpty().any { it.bookId == bookId }) {
            mutableState.value = mutableState.value.copy(pendingRemoveBookId = bookId)
        }
    }

    fun cancelRemoveBook() {
        mutableState.value = mutableState.value.copy(pendingRemoveBookId = null)
    }

    fun confirmRemoveBook() {
        val list = mutableState.value.selectedList ?: return
        val bookId = mutableState.value.pendingRemoveBookId ?: return
        mutate("Quitando libro…", "Libro quitado de la lista.", afterSuccess = {
            mutableState.value = mutableState.value.copy(pendingRemoveBookId = null)
        }) { removeBookFromList(list.id, bookId) }
    }

    fun dismissAction() {
        if (mutableState.value.action !is BookListActionState.Working) {
            mutableState.value = mutableState.value.copy(action = BookListActionState.Idle)
        }
    }

    private fun currentLists(): List<BookList> = when (val content = mutableState.value.content) {
        is BookListsContentState.Ready -> content.snapshot.lists
        is BookListsContentState.Loading -> content.previous?.lists.orEmpty()
        else -> emptyList()
    }

    private fun <T> mutate(
        working: String,
        success: String,
        afterSuccess: () -> Unit = {},
        operation: suspend () -> Resource<T>,
    ) {
        if (mutationJob?.isActive == true) return
        mutationJob = viewModelScope.launch {
            mutableState.value = mutableState.value.copy(action = BookListActionState.Working(working))
            when (val result = safely(operation)) {
                is Resource.Success -> {
                    afterSuccess()
                    mutableState.value = mutableState.value.copy(action = BookListActionState.Succeeded(success))
                    mutationJob = null
                    load(page = 1)
                }
                is Resource.Error -> {
                    mutableState.value = mutableState.value.copy(
                        action = BookListActionState.Failed(
                            result.message,
                            signInRequired = result.kind == ResourceErrorKind.UNAUTHORIZED,
                        ),
                    )
                }
            }
        }
    }

    private fun failAction(message: String) {
        mutableState.value = mutableState.value.copy(action = BookListActionState.Failed(message))
    }

    private suspend fun <T> safely(operation: suspend () -> Resource<T>): Resource<T> = try {
        operation()
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (error: Exception) {
        Resource.Error(error.message ?: "No se pudo completar la operación.")
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}

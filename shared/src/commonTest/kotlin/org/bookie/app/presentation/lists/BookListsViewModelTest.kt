package org.bookie.app.presentation.lists

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.bookie.app.domain.model.Book
import org.bookie.app.domain.model.BookList
import org.bookie.app.domain.model.BookListItem
import org.bookie.app.domain.model.BookListPage
import org.bookie.app.domain.model.CreateBookListInput
import org.bookie.app.domain.model.UpdateBookListInput
import org.bookie.app.domain.repository.BookListsRepository
import org.bookie.app.domain.repository.BookRepository
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind
import org.bookie.app.domain.usecase.AddBookToList
import org.bookie.app.domain.usecase.CreateBookList
import org.bookie.app.domain.usecase.DeleteBookList
import org.bookie.app.domain.usecase.GetBookLists
import org.bookie.app.domain.usecase.RemoveBookFromList
import org.bookie.app.domain.usecase.SearchBooks
import org.bookie.app.domain.usecase.UpdateBookList
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BookListsViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun `delete waits for confirmation and reloads only after success`() = runTest {
        val list = sampleList("list-1", "Favorites")
        val repository = FakeBookListsRepository().apply { page = pageOf(list) }
        val viewModel = viewModel(repository)
        viewModel.load()
        runCurrent()

        viewModel.requestDelete(list.id)
        assertTrue(repository.deletedIds.isEmpty())
        viewModel.confirmDelete()
        runCurrent()

        assertEquals(listOf(list.id), repository.deletedIds)
        assertEquals(2, repository.getListsCalls)
        assertTrue(assertIs<BookListsContentState.Ready>(viewModel.uiState.value.content).snapshot.lists.isEmpty())
        assertEquals(BookListActionState.Succeeded("Lista eliminada."), viewModel.uiState.value.action)
    }

    @Test
    fun `create sends trimmed values and refreshes visible lists`() = runTest {
        val repository = FakeBookListsRepository()
        val created = sampleList("new-list", "Lecturas")
        repository.onCreate = { input ->
            assertEquals(CreateBookListInput("Lecturas", "Para este año", true), input)
            repository.page = pageOf(created)
            Resource.Success(created)
        }
        val viewModel = viewModel(repository)
        viewModel.load()
        runCurrent()

        viewModel.create(" Lecturas ", " Para este año ", true)
        runCurrent()

        assertEquals(2, repository.getListsCalls)
        assertEquals(listOf(created), assertIs<BookListsContentState.Ready>(viewModel.uiState.value.content).snapshot.lists)
        assertEquals(BookListActionState.Succeeded("Lista creada."), viewModel.uiState.value.action)
    }

    @Test
    fun `unauthorized list load exposes sign-in state`() = runTest {
        val repository = FakeBookListsRepository().apply {
            onGet = { Resource.Error("Iniciá sesión", ResourceErrorKind.UNAUTHORIZED) }
        }
        val viewModel = viewModel(repository)
        viewModel.load()
        runCurrent()

        val failed = assertIs<BookListsContentState.Failed>(viewModel.uiState.value.content)
        assertTrue(failed.signInRequired)
    }

    @Test
    fun `adding a searched book sends its catalog id and refreshes list`() = runTest {
        val list = sampleList("list-1", "Favorites")
        val repository = FakeBookListsRepository().apply { page = pageOf(list) }
        val book = sampleBook()
        val books = FakeBookRepository().apply { results = listOf(book) }
        val viewModel = viewModel(repository, books)
        viewModel.load()
        runCurrent()
        viewModel.selectList(list.id)

        viewModel.searchBooks("Dune")
        runCurrent()
        assertEquals(BookListSearchState.Results("Dune", listOf(book)), viewModel.uiState.value.bookSearch)

        viewModel.addBook(book.id)
        runCurrent()

        assertEquals(listOf(list.id to book.id), repository.addedBooks)
        assertEquals(BookListActionState.Succeeded("Libro agregado a la lista."), viewModel.uiState.value.action)
    }

    private fun viewModel(
        lists: FakeBookListsRepository,
        books: FakeBookRepository = FakeBookRepository(),
    ) = BookListsViewModel(
        getBookLists = GetBookLists(lists),
        createBookList = CreateBookList(lists),
        updateBookList = UpdateBookList(lists),
        deleteBookList = DeleteBookList(lists),
        addBookToList = AddBookToList(lists),
        removeBookFromList = RemoveBookFromList(lists),
        searchBooksUseCase = SearchBooks(books),
    )
}

private class FakeBookListsRepository : BookListsRepository {
    var page = pageOf()
    var getListsCalls = 0
    val deletedIds = mutableListOf<String>()
    val addedBooks = mutableListOf<Pair<String, String>>()
    var onGet: (() -> Resource<BookListPage>)? = null
    var onCreate: (suspend (CreateBookListInput) -> Resource<BookList>)? = null

    override suspend fun getLists(page: Int, limit: Int): Resource<BookListPage> {
        getListsCalls++
        return onGet?.invoke() ?: Resource.Success(this.page)
    }

    override suspend fun createList(input: CreateBookListInput): Resource<BookList> =
        onCreate?.invoke(input) ?: Resource.Error("No implementado")

    override suspend fun updateList(listId: String, input: UpdateBookListInput): Resource<BookList> =
        Resource.Error("No implementado")

    override suspend fun deleteList(listId: String): Resource<Unit> {
        deletedIds += listId
        page = pageOf()
        return Resource.Success(Unit)
    }

    override suspend fun addBook(listId: String, bookId: String): Resource<BookListItem> {
        addedBooks += listId to bookId
        return Resource.Success(BookListItem("item-1", listId, bookId, "now", null))
    }

    override suspend fun removeBook(listId: String, bookId: String): Resource<Unit> = Resource.Success(Unit)
}

private class FakeBookRepository : BookRepository {
    var results = emptyList<Book>()

    override suspend fun searchBooks(query: String, page: Int): Resource<List<Book>> = Resource.Success(results)
}

private fun sampleList(id: String, name: String) = BookList(
    id = id,
    name = name,
    description = null,
    isPublic = false,
    createdAt = "2026-01-01T00:00:00Z",
    updatedAt = "2026-01-01T00:00:00Z",
    items = emptyList(),
)

private fun sampleBook() = Book(
    id = "book-internal-id",
    googleBookId = "google-id",
    title = "Dune",
    authors = listOf("Frank Herbert"),
    description = null,
    genres = listOf("Science fiction"),
    coverUrl = null,
    previewLink = null,
    infoLink = null,
    buyLink = null,
)

private fun pageOf(vararg lists: BookList) = BookListPage(
    lists = lists.toList(),
    page = 1,
    limit = 20,
    total = lists.size,
    totalPages = 1,
)

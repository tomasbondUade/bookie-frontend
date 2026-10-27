package org.bookie.app.presentation.search

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.bookie.app.domain.model.Book
import org.bookie.app.domain.repository.BookRepository
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.usecase.SearchBooks
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun searchReturnsBooksAndSupportsOpeningAndClosingDetail() = runTest {
        val book = sampleBook()
        val repository = FakeBookRepository { Resource.Success(listOf(book)) }
        val viewModel = SearchViewModel(SearchBooks(repository))

        viewModel.onQueryChanged("  dune  ")
        viewModel.search()
        runCurrent()

        assertEquals("dune", repository.receivedQuery)
        assertEquals(SearchUiState.Success(listOf(book), "dune"), viewModel.uiState.value)

        viewModel.selectBook(book)
        assertEquals(SearchUiState.Success(listOf(book), "dune", book), viewModel.uiState.value)

        viewModel.showResults()
        assertEquals(SearchUiState.Success(listOf(book), "dune"), viewModel.uiState.value)
    }

    @Test
    fun searchDistinguishesEmptyAndErrorResults() = runTest {
        val emptyViewModel = SearchViewModel(
            SearchBooks(FakeBookRepository { Resource.Success(emptyList()) }),
        )
        emptyViewModel.onQueryChanged("unknown")
        emptyViewModel.search()
        runCurrent()
        assertEquals(SearchUiState.Empty("unknown"), emptyViewModel.uiState.value)

        val errorViewModel = SearchViewModel(
            SearchBooks(FakeBookRepository { Resource.Error("Network unavailable") }),
        )
        errorViewModel.onQueryChanged("dune")
        errorViewModel.search()
        runCurrent()
        assertEquals(SearchUiState.Error("Network unavailable", "dune"), errorViewModel.uiState.value)
    }

    @Test
    fun blankQueryDoesNotCallRepository() = runTest {
        val repository = FakeBookRepository { Resource.Success(emptyList()) }
        val viewModel = SearchViewModel(SearchBooks(repository))

        viewModel.onQueryChanged("   ")
        viewModel.search()

        assertEquals(0, repository.calls)
        assertEquals(SearchUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun olderResponseDoesNotReplaceTheLatestSearch() = runTest {
        val olderResponse = CompletableDeferred<Resource<List<Book>>>()
        val latestBook = sampleBook().copy(id = "latest", title = "Latest result")
        val repository = FakeBookRepository { query ->
            if (query == "older") olderResponse.await() else Resource.Success(listOf(latestBook))
        }
        val viewModel = SearchViewModel(SearchBooks(repository))

        viewModel.onQueryChanged("older")
        viewModel.search()
        runCurrent()
        viewModel.onQueryChanged("latest")
        viewModel.search()
        runCurrent()

        assertEquals(SearchUiState.Success(listOf(latestBook), "latest"), viewModel.uiState.value)

        olderResponse.complete(Resource.Success(listOf(sampleBook())))
        runCurrent()

        assertEquals(SearchUiState.Success(listOf(latestBook), "latest"), viewModel.uiState.value)
    }

    private fun sampleBook() = Book(
        id = "book-1",
        googleBookId = "google-1",
        title = "Dune",
        authors = listOf("Frank Herbert"),
        description = "A science fiction novel.",
        genres = listOf("Science fiction"),
        coverUrl = null,
        previewLink = null,
        infoLink = null,
        buyLink = null,
    )

    private class FakeBookRepository(
        private val search: suspend (String) -> Resource<List<Book>>,
    ) : BookRepository {
        var receivedQuery: String? = null
            private set
        var calls: Int = 0
            private set

        override suspend fun searchBooks(query: String): Resource<List<Book>> {
            calls++
            receivedQuery = query
            return search(query)
        }
    }
}

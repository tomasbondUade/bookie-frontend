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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
        val repository = FakeBookRepository { _, _ -> Resource.Success(listOf(book)) }
        val viewModel = SearchViewModel(SearchBooks(repository))

        viewModel.onQueryChanged("  dune  ")
        viewModel.search()
        runCurrent()

        assertEquals("dune", repository.receivedQuery)
        assertEquals(SearchUiState.Success(listOf(book), "dune", hasMore = true), viewModel.uiState.value)

        viewModel.selectBook(book)
        assertEquals(SearchUiState.Success(listOf(book), "dune", book, hasMore = true), viewModel.uiState.value)

        viewModel.showResults()
        assertEquals(SearchUiState.Success(listOf(book), "dune", hasMore = true), viewModel.uiState.value)
    }

    @Test
    fun searchDistinguishesEmptyAndErrorResults() = runTest {
        val emptyViewModel = SearchViewModel(
            SearchBooks(FakeBookRepository { _, _ -> Resource.Success(emptyList()) }),
        )
        emptyViewModel.onQueryChanged("unknown")
        emptyViewModel.search()
        runCurrent()
        assertEquals(SearchUiState.Empty("unknown"), emptyViewModel.uiState.value)

        val errorViewModel = SearchViewModel(
            SearchBooks(FakeBookRepository { _, _ -> Resource.Error("Network unavailable") }),
        )
        errorViewModel.onQueryChanged("dune")
        errorViewModel.search()
        runCurrent()
        assertEquals(SearchUiState.Error("Network unavailable", "dune"), errorViewModel.uiState.value)
    }

    @Test
    fun blankQueryDoesNotCallRepository() = runTest {
        val repository = FakeBookRepository { _, _ -> Resource.Success(emptyList()) }
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
        val repository = FakeBookRepository { query, _ ->
            if (query == "older") olderResponse.await() else Resource.Success(listOf(latestBook))
        }
        val viewModel = SearchViewModel(SearchBooks(repository))

        viewModel.onQueryChanged("older")
        viewModel.search()
        runCurrent()
        viewModel.onQueryChanged("latest")
        viewModel.search()
        runCurrent()

        assertEquals(
            SearchUiState.Success(listOf(latestBook), "latest", hasMore = true),
            viewModel.uiState.value,
        )

        olderResponse.complete(Resource.Success(listOf(sampleBook())))
        runCurrent()

        assertEquals(
            SearchUiState.Success(listOf(latestBook), "latest", hasMore = true),
            viewModel.uiState.value,
        )
    }

    @Test
    fun loadMoreDeduplicatesEditionsAndContinuesAfterShortPages() = runTest {
        val firstPage = (1..10).map { sampleBook().copy(id = "book-$it", title = "Book $it") }
        val nextBook = sampleBook().copy(id = "book-11", title = "Book 11")
        val duplicateEdition = firstPage.last().copy(id = "another-source-id")
        val repository = FakeBookRepository { _, page ->
            if (page == 1) Resource.Success(firstPage)
            else if (page == 2) Resource.Success(listOf(duplicateEdition, nextBook))
            else Resource.Success(emptyList())
        }
        val viewModel = SearchViewModel(SearchBooks(repository))

        viewModel.onQueryChanged("fantasy")
        viewModel.search()
        runCurrent()
        viewModel.loadMore()
        runCurrent()
        viewModel.loadMore()
        runCurrent()

        val state = viewModel.uiState.value as SearchUiState.Success
        assertEquals(11, state.books.size)
        assertEquals("book-11", state.books.last().id)
        assertEquals(3, state.page)
        assertFalse(state.hasMore)
        assertEquals(listOf(1, 2, 3), repository.receivedPages)
    }

    @Test
    fun loadMoreCanBeRetriedAfterAnError() = runTest {
        val firstPage = (1..10).map {
            sampleBook().copy(id = "book-$it", title = "Book $it")
        }
        var failFirstAttempt = true
        val repository = FakeBookRepository { _, page ->
            when {
                page == 1 -> Resource.Success(firstPage)
                failFirstAttempt -> {
                    failFirstAttempt = false
                    Resource.Error("Network unavailable")
                }
                page == 2 -> Resource.Success(listOf(sampleBook().copy(id = "book-11")))
                else -> Resource.Success(emptyList())
            }
        }
        val viewModel = SearchViewModel(SearchBooks(repository))

        viewModel.onQueryChanged("fantasy")
        viewModel.search()
        runCurrent()
        viewModel.loadMore()
        runCurrent()

        val failedState = viewModel.uiState.value as SearchUiState.Success
        assertEquals("Network unavailable", failedState.loadMoreError)
        assertTrue(failedState.hasMore)

        viewModel.loadMore()
        runCurrent()
        viewModel.loadMore()
        runCurrent()

        val retriedState = viewModel.uiState.value as SearchUiState.Success
        assertEquals(11, retriedState.books.size)
        assertEquals(3, retriedState.page)
        assertFalse(retriedState.hasMore)
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
        private val search: suspend (String, Int) -> Resource<List<Book>>,
    ) : BookRepository {
        var receivedQuery: String? = null
            private set
        val receivedPages = mutableListOf<Int>()
        var calls: Int = 0
            private set

        override suspend fun searchBooks(query: String, page: Int): Resource<List<Book>> {
            calls++
            receivedQuery = query
            receivedPages += page
            return search(query, page)
        }
    }
}

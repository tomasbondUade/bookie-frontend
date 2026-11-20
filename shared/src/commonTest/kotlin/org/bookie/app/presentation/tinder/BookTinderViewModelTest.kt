package org.bookie.app.presentation.tinder

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.bookie.app.domain.model.Book
import org.bookie.app.domain.model.BookRecommendation
import org.bookie.app.domain.model.BookTinderFeed
import org.bookie.app.domain.model.RecommendationSource
import org.bookie.app.domain.model.SwipeType
import org.bookie.app.domain.repository.BookTinderRepository
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind
import org.bookie.app.domain.usecase.GetBookTinderFeed
import org.bookie.app.domain.usecase.RecordBookSwipe
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BookTinderViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads empty feed explicitly`() = runTest {
        val viewModel = viewModel(FakeRepository().apply { feed = Resource.Success(sampleFeed(emptyList())) })
        runCurrent()

        assertEquals(BookTinderUiState.Empty(RecommendationSource.FALLBACK), viewModel.uiState.value)
    }

    @Test
    fun `does not advance until the swipe is confirmed and prevents duplicate taps`() = runTest {
        val gate = CompletableDeferred<Resource<Unit>>()
        val repository = FakeRepository().apply { swipeResult = { gate.await() } }
        val viewModel = viewModel(repository)
        runCurrent()
        assertEquals("book-1", ready(viewModel).feed.books[0].book.id)

        viewModel.swipe(SwipeType.LIKE)
        viewModel.swipe(SwipeType.DISLIKE)
        runCurrent()

        assertEquals(listOf("book-1" to SwipeType.LIKE), repository.swipes)
        assertEquals(0, ready(viewModel).currentIndex)
        assertTrue(ready(viewModel).submitting)

        gate.complete(Resource.Success(Unit))
        runCurrent()

        assertEquals(1, ready(viewModel).currentIndex)
        assertEquals("book-2", ready(viewModel).feed.books[ready(viewModel).currentIndex].book.id)
    }

    @Test
    fun `failed swipe keeps the same book for retry`() = runTest {
        val repository = FakeRepository().apply {
            swipeResult = { Resource.Error("Sin conexión", ResourceErrorKind.NETWORK) }
        }
        val viewModel = viewModel(repository)
        runCurrent()

        viewModel.swipe(SwipeType.DISLIKE)
        runCurrent()

        val state = ready(viewModel)
        assertEquals(0, state.currentIndex)
        assertEquals("book-1", state.feed.books[state.currentIndex].book.id)
        assertEquals("Sin conexión", state.actionError)
        assertFalse(state.submitting)
    }

    @Test
    fun `expired session requests sign in without advancing`() = runTest {
        val repository = FakeRepository().apply {
            swipeResult = { Resource.Error("HTTP 401", ResourceErrorKind.UNAUTHORIZED) }
        }
        val viewModel = viewModel(repository)
        runCurrent()

        viewModel.swipe(SwipeType.LIKE)
        runCurrent()

        assertIs<BookTinderUiState.SignInRequired>(viewModel.uiState.value)
        assertEquals(listOf("book-1" to SwipeType.LIKE), repository.swipes)
    }

    private fun viewModel(repository: FakeRepository) = BookTinderViewModel(
        getBookTinderFeed = GetBookTinderFeed(repository),
        recordBookSwipe = RecordBookSwipe(repository),
    )

    private fun ready(viewModel: BookTinderViewModel): BookTinderUiState.Ready =
        assertIs(viewModel.uiState.value)

    private class FakeRepository : BookTinderRepository {
        var feed: Resource<BookTinderFeed> = Resource.Success(sampleFeed(listOf(book("book-1"), book("book-2"))))
        var swipeResult: suspend () -> Resource<Unit> = { Resource.Success(Unit) }
        val swipes = mutableListOf<Pair<String, SwipeType>>()

        override suspend fun getFeed(limit: Int): Resource<BookTinderFeed> {
            assertEquals(10, limit)
            return feed
        }

        override suspend fun recordSwipe(bookId: String, type: SwipeType): Resource<Unit> {
            swipes += bookId to type
            return swipeResult()
        }
    }

    private companion object {
        fun book(id: String) = BookRecommendation(
            book = Book(
                id = id,
                googleBookId = "google-$id",
                title = id,
                authors = listOf("Author"),
                description = null,
                genres = emptyList(),
                coverUrl = null,
                previewLink = null,
                infoLink = null,
                buyLink = null,
            ),
            reasons = emptyList(),
        )

        fun sampleFeed(books: List<BookRecommendation>) = BookTinderFeed(
            source = RecommendationSource.FALLBACK,
            count = books.size,
            books = books,
        )
    }
}

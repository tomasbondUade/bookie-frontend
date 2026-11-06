package org.bookie.app.presentation.tinder

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.bookie.app.domain.model.*
import org.bookie.app.domain.repository.BookTinderRepository
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.usecase.GetTinderBooks
import org.bookie.app.domain.usecase.RegisterBookSwipe
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class TinderViewModelTest {
    @BeforeTest fun setup() { Dispatchers.setMain(StandardTestDispatcher()) }
    @AfterTest fun cleanup() { Dispatchers.resetMain() }

    private fun book(id: String) = Book(
        id, "google-$id", "Book $id", listOf("Author"), null,
        emptyList(), null, null, null, null
    )

    private class FakeRepository(
        val feed: TinderFeed,
        val send: suspend (BookSwipe) -> Resource<SwipeResult> = {
            Resource.Success(SwipeResult(true, "OK"))
        }
    ) : BookTinderRepository {
        val requests = mutableListOf<BookSwipe>()
        override suspend fun getTinderBooks(limit: Int) = Resource.Success(feed)
        override suspend fun registerSwipe(swipe: BookSwipe): Resource<SwipeResult> {
            requests.add(swipe)
            return send(swipe)
        }
    }

    private fun viewModel(repository: FakeRepository) =
        TinderViewModel(GetTinderBooks(repository), RegisterBookSwipe(repository))

    @Test fun emptyFeedShowsFinished() = runTest {
        val repository = FakeRepository(TinderFeed(RecommendationSource.FALLBACK, 0, emptyList()))
        val vm = viewModel(repository)
        runCurrent()
        assertEquals(TinderUiState.Finished, vm.uiState.value)
    }

    @Test fun likeAdvancesOnlyAfterSuccess() = runTest {
        val pending = CompletableDeferred<Resource<SwipeResult>>()
        val repository = FakeRepository(TinderFeed(RecommendationSource.AI, 2,
            listOf(TinderRecommendedBook(book("1")), TinderRecommendedBook(book("2"))))) {
            pending.await()
        }
        val vm = viewModel(repository)
        runCurrent()
        vm.like()
        vm.like()
        runCurrent()
        assertEquals(1, repository.requests.size)
        assertEquals(true, assertIs<TinderUiState.ShowingBook>(vm.uiState.value).sending)
        pending.complete(Resource.Success(SwipeResult(true, "OK")))
        runCurrent()
        assertEquals("2", assertIs<TinderUiState.ShowingBook>(vm.uiState.value).book.id)
        assertEquals(SwipeType.LIKE, repository.requests.single().type)
    }

    @Test fun failedDislikeKeepsCurrentBook() = runTest {
        val repository = FakeRepository(TinderFeed(RecommendationSource.FALLBACK, 1,
            listOf(TinderRecommendedBook(book("1"))))) {
            Resource.Error("Network error")
        }
        val vm = viewModel(repository)
        runCurrent()
        vm.dislike()
        runCurrent()
        val state = assertIs<TinderUiState.ShowingBook>(vm.uiState.value)
        assertEquals("1", state.book.id)
        assertEquals("Network error", state.error)
        assertEquals(SwipeType.DISLIKE, repository.requests.single().type)
    }
}

package org.bookie.app.presentation.search

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.bookie.app.domain.model.BookReview
import org.bookie.app.domain.model.BookReviewSort
import org.bookie.app.domain.model.BookReviewsPage
import org.bookie.app.domain.repository.BookReviewsRepository
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.usecase.GetBookReviews
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BookReviewsViewModelTest {
    @BeforeTest fun prepare() { Dispatchers.setMain(StandardTestDispatcher()) }
    @AfterTest fun cleanup() { Dispatchers.resetMain() }

    @Test
    fun emptyAndErrorStatesAreExplicit() = runTest {
        val repo = FakeReviews { _, _, _ -> Resource.Success(page(emptyList())) }
        val vm = BookReviewsViewModel(GetBookReviews(repo))
        vm.selectBook("book-1")
        runCurrent()
        assertIs<BookReviewsUiState.Empty>(vm.uiState.value)

        repo.result = { _, _, _ -> Resource.Error("Sin conexión") }
        vm.retry()
        runCurrent()
        assertEquals(BookReviewsUiState.Error("Sin conexión"), vm.uiState.value)
    }

    @Test
    fun paginationKeepsExistingReviewsAndSortStartsAgain() = runTest {
        val repo = FakeReviews { _, p, _ ->
            Resource.Success(
                page(listOf(review("review-$p")), page = p, totalPages = 2),
            )
        }
        val vm = BookReviewsViewModel(GetBookReviews(repo))
        vm.selectBook("book-1")
        runCurrent()
        val first = assertIs<BookReviewsUiState.Ready>(vm.uiState.value)
        assertEquals(listOf("review-1"), first.reviews.map { it.id })
        assertTrue(first.hasMore)

        vm.loadMore()
        runCurrent()
        val second = assertIs<BookReviewsUiState.Ready>(vm.uiState.value)
        assertEquals(listOf("review-1", "review-2"), second.reviews.map { it.id })
        assertEquals(false, second.hasMore)

        vm.changeSort(BookReviewSort.TOP)
        runCurrent()
        val sorted = assertIs<BookReviewsUiState.Ready>(vm.uiState.value)
        assertEquals(BookReviewSort.TOP, sorted.sort)
        assertEquals(listOf("review-1"), sorted.reviews.map { it.id })
        assertEquals(
            listOf(
                Triple("book-1", 1, BookReviewSort.RECENT),
                Triple("book-1", 2, BookReviewSort.RECENT),
                Triple("book-1", 1, BookReviewSort.TOP),
            ),
            repo.calls,
        )
    }

    @Test
    fun oldBookResultDoesNotReplaceNewSelection() = runTest {
        val repo = FakeReviews { id, _, _ ->
            if (id == "book-1") Resource.Success(page(listOf(review("old"))))
            else Resource.Success(page(listOf(review("new"))))
        }
        val vm = BookReviewsViewModel(GetBookReviews(repo))
        vm.selectBook("book-1")
        vm.selectBook("book-2")
        runCurrent()
        val result = assertIs<BookReviewsUiState.Ready>(vm.uiState.value)
        assertEquals("new", result.reviews.single().id)
    }

    private fun review(id: String) = BookReview(
        id = id, rating = 4, comment = "Comentario", updatedAt = "2026-11-17T11:00:00Z",
        username = "Lectura", avatarUrl = null,
    )

    private fun page(
        reviews: List<BookReview>,
        page: Int = 1,
        totalPages: Int = if (reviews.isEmpty()) 0 else 1,
    ) = BookReviewsPage(
        reviews = reviews, page = page, limit = 10,
        total = if (totalPages == 2) 2 else reviews.size, totalPages = totalPages,
    )

    private class FakeReviews(
        var result: suspend (String, Int, BookReviewSort) -> Resource<BookReviewsPage>,
    ) : BookReviewsRepository {
        val calls = mutableListOf<Triple<String, Int, BookReviewSort>>()
        override suspend fun getReviews(
            bookId: String, page: Int, limit: Int, sort: BookReviewSort,
        ): Resource<BookReviewsPage> {
            calls += Triple(bookId, page, sort)
            return result(bookId, page, sort)
        }
    }
}

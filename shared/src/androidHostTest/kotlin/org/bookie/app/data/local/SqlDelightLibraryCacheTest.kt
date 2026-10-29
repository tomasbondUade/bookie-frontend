package org.bookie.app.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.test.runTest
import org.bookie.app.data.local.db.BookieCacheDatabase
import org.bookie.app.domain.model.Book
import org.bookie.app.domain.model.LibraryBookState
import org.bookie.app.domain.model.LibraryPage
import org.bookie.app.domain.model.LibraryStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SqlDelightLibraryCacheTest {
    @Test
    fun `caches pages and progress per account without storing private notes`() = runTest {
        withCache { cache ->
            val state = state("state-1", 125)
            val page = LibraryPage(listOf(state), 1, 20, 1, 1)
            cache.replacePage("alice", page, LibraryStatus.READING)
            assertEquals(page.copy(books = listOf(state.copy(privateNotes = null))),
                cache.readPage("alice", 1, 20, LibraryStatus.READING))
            assertEquals(125, cache.readState("alice", "state-1")?.currentPage)
            assertNull(cache.readState("alice", "state-1")?.privateNotes)
            assertNull(cache.readPage("bob", 1, 20, LibraryStatus.READING))
            assertNull(cache.readState("bob", "state-1"))
            assertNull(cache.readPage("alice", 1, 20))
        }
    }

    @Test
    fun `replaces an existing page and preserves its pagination metadata`() = runTest {
        withCache { cache ->
            cache.replacePage("alice", LibraryPage(listOf(state("old", 10)), 2, 10, 12, 2))
            cache.replacePage("alice", LibraryPage(listOf(state("new", 20)), 2, 10, 14, 2))
            val actual = cache.readPage("alice", 2, 10)
            assertEquals(listOf("new"), actual?.books?.map { it.id })
            assertEquals(14, actual?.total)
            assertEquals(2, actual?.totalPages)
            cache.replacePage("alice", LibraryPage(emptyList(), 2, 10, 0, 0))
            assertEquals(emptyList(), cache.readPage("alice", 2, 10)?.books)
        }
    }

    @Test
    fun `clears only one account and keeps the other account intact`() = runTest {
        withCache { cache ->
            cache.replacePage("alice", LibraryPage(listOf(state("state-1", 10)), 1, 20, 1, 1))
            cache.replacePage("bob", LibraryPage(listOf(state("state-1", 40)), 1, 20, 1, 1))
            cache.clearAccount("alice")
            assertNull(cache.readPage("alice", 1, 20))
            assertNull(cache.readState("alice", "state-1"))
            assertEquals(40, cache.readState("bob", "state-1")?.currentPage)
            assertEquals(1, cache.readPage("bob", 1, 20)?.books?.size)
        }
    }

    private fun withCache(block: suspend (LibraryCache) -> Unit) = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            BookieCacheDatabase.Schema.create(driver)
            block(SqlDelightLibraryCache(driver))
        } finally {
            driver.close()
        }
    }

    private fun state(id: String, page: Int) = LibraryBookState(
        id = id,
        bookId = "book-1",
        book = Book(
            id = "book-1", googleBookId = "google-1", title = "Dune",
            authors = listOf("Frank Herbert"), description = null,
            genres = listOf("Sci-Fi"), coverUrl = null, previewLink = null,
            infoLink = null, buyLink = null,
        ),
        status = LibraryStatus.READING,
        rating = 5,
        comment = "Great",
        isFavorite = true,
        currentPage = page,
        progressPercentage = 50,
        startedAt = null,
        finishedAt = null,
        isAbandoned = false,
        privateNotes = "Do not store this",
        updatedAt = "2026-10-29T00:00:00.000Z",
    )
}

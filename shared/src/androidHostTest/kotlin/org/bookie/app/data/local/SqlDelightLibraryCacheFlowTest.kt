package org.bookie.app.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.bookie.app.data.local.db.BookieCacheDatabase
import org.bookie.app.domain.model.Book
import org.bookie.app.domain.model.LibraryBookState
import org.bookie.app.domain.model.LibraryPage
import org.bookie.app.domain.model.LibraryStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SqlDelightLibraryCacheFlowTest {
    @Test
    fun `page observer emits initial empty cache then saved page and updated progress`() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            BookieCacheDatabase.Schema.create(driver)
            val cache = SqlDelightLibraryCache(driver)
            val values = async { cache.observePage("alice", 1, 20).take(3).toList() }
            kotlinx.coroutines.yield()
            cache.replacePage("alice", LibraryPage(listOf(state(10)), 1, 20, 1, 1))
            kotlinx.coroutines.yield()
            cache.replacePage("alice", LibraryPage(listOf(state(40)), 1, 20, 1, 1))
            val pages = values.await()
            assertNull(pages[0])
            assertEquals(10, pages[1]?.books?.single()?.currentPage)
            assertEquals(40, pages[2]?.books?.single()?.currentPage)
        } finally {
            driver.close()
        }
    }

    @Test
    fun `state observer emits null then progress and clears on logout`() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            BookieCacheDatabase.Schema.create(driver)
            val cache = SqlDelightLibraryCache(driver)
            val values = async { cache.observeState("alice", "state-1").take(3).toList() }
            kotlinx.coroutines.yield()
            cache.replacePage("alice", LibraryPage(listOf(state(15)), 1, 20, 1, 1))
            kotlinx.coroutines.yield()
            cache.clearAccount("alice")
            val states = values.await()
            assertNull(states[0])
            assertEquals(15, states[1]?.currentPage)
            assertNull(states[2])
        } finally {
            driver.close()
        }
    }

    private fun state(progress: Int) = LibraryBookState(
        id = "state-1",
        bookId = "book-1",
        book = Book(
            id = "book-1", googleBookId = "google-1", title = "Dune",
            authors = listOf("Frank Herbert"), description = null,
            genres = emptyList(), coverUrl = null, previewLink = null,
            infoLink = null, buyLink = null,
        ),
        status = LibraryStatus.READING,
        rating = null, comment = null, isFavorite = false,
        currentPage = progress, progressPercentage = null,
        startedAt = null, finishedAt = null, isAbandoned = false,
        privateNotes = null, updatedAt = "2026-10-29T00:00:00Z",
    )
}

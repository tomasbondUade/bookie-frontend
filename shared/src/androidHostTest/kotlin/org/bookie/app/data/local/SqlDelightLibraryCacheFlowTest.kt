package org.bookie.app.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
    fun `page observer emits initial empty cache then saved page and updated progress`() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            BookieCacheDatabase.Schema.create(driver)
            val cache = SqlDelightLibraryCache(driver)
            val events = Channel<LibraryPage?>(Channel.UNLIMITED)
            val observer = launch { cache.observePage("alice", 1, 20).collect { events.send(it) } }
            try {
                // Await each emission before mutating SQLDelight again: asFlow uses Dispatchers.Default.
                assertNull(withTimeout(5_000) { events.receive() })
                cache.replacePage("alice", LibraryPage(listOf(state(10)), 1, 20, 1, 1))
                assertEquals(10, withTimeout(5_000) { events.receive() }?.books?.single()?.currentPage)
                cache.replacePage("alice", LibraryPage(listOf(state(40)), 1, 20, 1, 1))
                assertEquals(40, withTimeout(5_000) { events.receive() }?.books?.single()?.currentPage)
            } finally {
                observer.cancelAndJoin()
            }
        } finally {
            driver.close()
        }
    }

    @Test
    fun `state observer emits null then progress and clears on logout`() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            BookieCacheDatabase.Schema.create(driver)
            val cache = SqlDelightLibraryCache(driver)
            val events = Channel<LibraryBookState?>(Channel.UNLIMITED)
            val observer = launch { cache.observeState("alice", "state-1").collect { events.send(it) } }
            try {
                assertNull(withTimeout(5_000) { events.receive() })
                cache.replacePage("alice", LibraryPage(listOf(state(15)), 1, 20, 1, 1))
                assertEquals(15, withTimeout(5_000) { events.receive() }?.currentPage)
                cache.clearAccount("alice")
                assertNull(withTimeout(5_000) { events.receive() })
            } finally {
                observer.cancelAndJoin()
            }
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

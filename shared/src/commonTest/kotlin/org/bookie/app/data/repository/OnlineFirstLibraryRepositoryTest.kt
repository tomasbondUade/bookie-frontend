package org.bookie.app.data.repository

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.bookie.app.data.local.LibraryCache
import org.bookie.app.data.remote.LibraryRemoteDataSource
import org.bookie.app.data.remote.dto.*
import org.bookie.app.domain.model.*
import org.bookie.app.domain.result.Resource
import kotlin.test.*

class OnlineFirstLibraryRepositoryTest {
    @Test fun `cached page is emitted before server page and server wins`() = runTest {
        val cache = FakeCache(page(10))
        val remote = FakeRemote(pageDto(40))
        val repository = OnlineFirstLibraryRepository(remote, cache, AccountIdProvider { "alice" })

        val states = repository.observeLibrary().toList()
        assertEquals(10, (states[0] as LibraryLoad.Cached).page.books.single().currentPage)
        assertEquals(40, (states[1] as LibraryLoad.Fresh).page.books.single().currentPage)
        assertEquals(40, cache.stored?.books?.single()?.currentPage)
    }

    @Test fun `offline read returns stale cache and exposes failure`() = runTest {
        val cache = FakeCache(page(10))
        val remote = FakeRemote(pageDto(40)).apply { failure = IllegalStateException("offline") }
        val repository = OnlineFirstLibraryRepository(remote, cache, AccountIdProvider { "alice" })
        val states = repository.observeLibrary().toList()
        assertIs<LibraryLoad.Cached>(states[0])
        assertEquals(page(10), (states[1] as LibraryLoad.Failed).cachedPage)
        assertEquals(Resource.Success(page(10)), repository.getLibrary())
    }

    @Test fun `without cache and without network reports error`() = runTest {
        val remote = FakeRemote(pageDto(40)).apply { failure = IllegalStateException("offline") }
        val repository = OnlineFirstLibraryRepository(remote, FakeCache(null), AccountIdProvider { "alice" })
        val states = repository.observeLibrary().toList()
        assertEquals(1, states.size)
        assertIs<LibraryLoad.Failed>(states.single())
        assertIs<Resource.Error>(repository.getLibrary())
    }

    @Test fun `failed write never clears cache or reports success`() = runTest {
        val cache = FakeCache(page(10))
        val remote = FakeRemote(pageDto(40)).apply { failure = IllegalStateException("server error") }
        val repository = OnlineFirstLibraryRepository(remote, cache, AccountIdProvider { "alice" })
        assertIs<Resource.Error>(repository.saveState(LibraryStateInput("book-1")))
        assertEquals(0, cache.clearCount)
        assertEquals(page(10), cache.stored)
    }

    @Test fun `confirmed delete invalidates account cache`() = runTest {
        val cache = FakeCache(page(10))
        val remote = FakeRemote(pageDto(40))
        val repository = OnlineFirstLibraryRepository(remote, cache, AccountIdProvider { "alice" })

        assertEquals(Resource.Success(Unit), repository.deleteState("state-1"))
        assertEquals(listOf("state-1"), remote.deletedIds)
        assertEquals("alice", cache.lastCleared)
        assertEquals(1, cache.clearCount)
        assertNull(cache.stored)
    }

    @Test fun `rejected delete leaves cached data untouched`() = runTest {
        val cache = FakeCache(page(10))
        val remote = FakeRemote(pageDto(40)).apply { failure = IllegalStateException("HTTP 500") }
        val repository = OnlineFirstLibraryRepository(remote, cache, AccountIdProvider { "alice" })

        assertIs<Resource.Error>(repository.deleteState("state-1"))
        assertEquals(0, cache.clearCount)
        assertEquals(page(10), cache.stored)
    }

    @Test fun `delete without a session does not call remote or clear cache`() = runTest {
        val cache = FakeCache(page(10))
        val remote = FakeRemote(pageDto(40))
        val repository = OnlineFirstLibraryRepository(remote, cache, AccountIdProvider { null })

        assertIs<Resource.Error>(repository.deleteState("state-1"))
        assertTrue(remote.deletedIds.isEmpty())
        assertEquals(0, cache.clearCount)
    }

    @Test fun `successful write invalidates account cache only`() = runTest {
        val cache = FakeCache(page(10))
        val repository = OnlineFirstLibraryRepository(
            FakeRemote(pageDto(40)), cache, AccountIdProvider { "alice" },
        )
        assertIs<Resource.Success<LibraryBookState>>(repository.saveState(LibraryStateInput("book-1")))
        assertEquals("alice", cache.lastCleared)
        assertEquals(1, cache.clearCount)
    }

    @Test fun `switching account during refresh prevents showing other account data`() = runTest {
        var account = "alice"
        val cache = FakeCache(page(10))
        val remote = FakeRemote(pageDto(40)).apply { onGet = { account = "bob" } }
        val repository = OnlineFirstLibraryRepository(remote, cache, AccountIdProvider { account })
        val states = repository.observeLibrary().toList()
        assertEquals(1, states.size)
        assertIs<LibraryLoad.Cached>(states.single())
        assertEquals(0, cache.replaceCount)
    }

    private class FakeCache(var stored: LibraryPage?) : LibraryCache {
        var replaceCount = 0
        var clearCount = 0
        var lastCleared: String? = null
        override suspend fun readPage(accountId: String, page: Int, limit: Int, status: LibraryStatus?, favoritesOnly: Boolean) = stored
        override suspend fun replacePage(accountId: String, page: LibraryPage, status: LibraryStatus?, favoritesOnly: Boolean) {
            replaceCount++
            stored = page
        }
        override suspend fun readState(accountId: String, stateId: String): LibraryBookState? = stored?.books?.firstOrNull()
        override suspend fun clearAccount(accountId: String) {
            clearCount++
            lastCleared = accountId
            stored = null
        }
    }

    private class FakeRemote(private val response: LibraryPageDto) : LibraryRemoteDataSource {
        var failure: Exception? = null
        var onGet: () -> Unit = {}
        val deletedIds = mutableListOf<String>()
        override suspend fun deleteState(stateId: String) {
            failure?.let { throw it }
            deletedIds += stateId
        }
        override suspend fun getLibrary(page: Int, limit: Int, status: LibraryStatusDto?, favoritesOnly: Boolean): LibraryPageDto {
            onGet()
            failure?.let { throw it }
            return response
        }
        override suspend fun saveState(input: SaveLibraryStateDto): LibraryBookStateDto {
            failure?.let { throw it }
            return response.data.single()
        }
        override suspend fun createReadingSession(stateId: String, input: CreateReadingSessionDto, idempotencyKey: String): ReadingSessionDto {
            failure?.let { throw it }
            error("not used")
        }
        override suspend fun getReadingSessions(stateId: String): List<ReadingSessionDto> {
            failure?.let { throw it }
            return emptyList()
        }
    }

    private fun page(progress: Int) = LibraryPage(listOf(state(progress)), 1, 20, 1, 1)
    private fun state(progress: Int) = LibraryBookState(
        id = "state-1", bookId = "book-1",
        book = Book("book-1", "google-1", "Dune", listOf("Frank Herbert"), null, emptyList(), null, null, null, null),
        status = LibraryStatus.READING, rating = null, comment = null,
        isFavorite = false, currentPage = progress, progressPercentage = null,
        startedAt = null, finishedAt = null, isAbandoned = false,
        privateNotes = null, updatedAt = "2026-10-29T00:00:00Z",
    )
    private fun pageDto(progress: Int) = LibraryPageDto(
        data = listOf(LibraryBookStateDto(
            id = "state-1", bookId = "book-1",
            book = org.bookie.app.data.remote.dto.BookDto(
                id = "book-1", googleBookId = "google-1", title = "Dune",
            ),
            status = LibraryStatusDto.READING,
            currentPage = progress, updatedAt = "2026-10-29T00:00:00Z",
        )),
        meta = LibraryPageMetaDto(1, 20, 1, 1),
    )
}

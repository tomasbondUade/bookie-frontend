package org.bookie.app.presentation.library

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.bookie.app.domain.model.Book
import org.bookie.app.domain.model.LibraryBookState
import org.bookie.app.domain.model.LibraryLoad
import org.bookie.app.domain.model.LibraryPage
import org.bookie.app.domain.model.LibraryStateInput
import org.bookie.app.domain.model.LibraryStatus
import org.bookie.app.domain.model.ReadingSession
import org.bookie.app.domain.model.ReadingSessionInput
import org.bookie.app.domain.repository.LibraryFeedRepository
import org.bookie.app.domain.repository.LibraryRepository
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.usecase.DeleteLibraryState
import org.bookie.app.domain.usecase.ObserveLibraryOnlineFirst
import org.bookie.app.domain.usecase.SaveLibraryState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryExperienceViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun `shows cached data as stale until the remote page arrives`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val feed = FakeFeed().apply {
            respond = {
                flow {
                    emit(LibraryLoad.Cached(libraryPage(1, 1, sampleState(currentPage = 10))))
                    gate.await()
                    emit(LibraryLoad.Fresh(libraryPage(1, 1, sampleState(currentPage = 45))))
                }
            }
        }
        val viewModel = vm(feed)
        viewModel.refresh()
        runCurrent()

        val cached = assertIs<LibraryContentState.Ready>(viewModel.uiState.value.content).snapshot
        assertTrue(cached.stale)
        assertEquals(10, cached.entries.single().currentPage)

        gate.complete(Unit)
        runCurrent()
        val fresh = assertIs<LibraryContentState.Ready>(viewModel.uiState.value.content).snapshot
        assertFalse(fresh.stale)
        assertEquals(45, fresh.entries.single().currentPage)
        assertNull(fresh.warning)
    }

    @Test
    fun `retains offline cache with a visible warning and errors without cache`() = runTest {
        val cachedPage = libraryPage(1, 1, sampleState())
        val feed = FakeFeed().apply {
            respond = { flowOf(LibraryLoad.Cached(cachedPage), LibraryLoad.Failed("Sin conexión", cachedPage)) }
        }
        val viewModel = vm(feed)
        viewModel.refresh()
        runCurrent()

        val ready = assertIs<LibraryContentState.Ready>(viewModel.uiState.value.content).snapshot
        assertTrue(ready.stale)
        assertEquals("Sin conexión", ready.warning)
        assertEquals("state-1", ready.entries.single().id)

        val emptyFeed = FakeFeed().apply {
            respond = { flowOf(LibraryLoad.Failed("Sin conexión")) }
        }
        val other = vm(emptyFeed)
        other.refresh()
        runCurrent()
        assertEquals(LibraryContentState.Error("Sin conexión"), other.uiState.value.content)
    }

    @Test
    fun `empty remote library has an explicit empty state`() = runTest {
        val viewModel = vm(FakeFeed())
        viewModel.refresh()
        runCurrent()
        assertEquals(LibraryContentState.Empty(), viewModel.uiState.value.content)
    }

    @Test
    fun `filters are passed to the use case and restart pagination at page one`() = runTest {
        val feed = FakeFeed().apply {
            respond = { flowOf(LibraryLoad.Fresh(libraryPage(1, 1, sampleState()))) }
        }
        val viewModel = vm(feed)

        viewModel.refresh()
        runCurrent()
        viewModel.setStatusFilter(LibraryStatus.READ)
        runCurrent()
        viewModel.setFavoritesOnly(true)
        runCurrent()

        assertEquals(
            listOf(
                LibraryQuery(1, 20, null, false),
                LibraryQuery(1, 20, LibraryStatus.READ, false),
                LibraryQuery(1, 20, LibraryStatus.READ, true),
            ),
            feed.queries,
        )
        assertEquals(LibraryStatus.READ, viewModel.uiState.value.statusFilter)
        assertTrue(viewModel.uiState.value.favoritesOnly)
    }

    @Test
    fun `paginates without duplicates and retries a failed stale second page`() = runTest {
        val first = sampleState()
        val second = sampleState(id = "state-2", bookId = "book-2")
        var pageTwoAttempts = 0
        val feed = FakeFeed().apply {
            respond = { query ->
                when (query.page) {
                    1 -> flowOf(LibraryLoad.Fresh(libraryPage(1, 2, first)))
                    else -> {
                        pageTwoAttempts++
                        if (pageTwoAttempts == 1) {
                            val cached = libraryPage(2, 2, first, second)
                            flowOf(LibraryLoad.Cached(cached), LibraryLoad.Failed("offline", cached))
                        } else {
                            flowOf(LibraryLoad.Fresh(libraryPage(2, 2, second)))
                        }
                    }
                }
            }
        }
        val viewModel = vm(feed)

        viewModel.refresh()
        runCurrent()
        viewModel.loadNextPage()
        runCurrent()

        val stale = assertIs<LibraryContentState.Ready>(viewModel.uiState.value.content).snapshot
        assertEquals(2, stale.entries.size)
        assertEquals(2, stale.page)
        assertTrue(stale.stale)
        assertEquals("offline", stale.warning)

        viewModel.retry()
        runCurrent()

        val fresh = assertIs<LibraryContentState.Ready>(viewModel.uiState.value.content).snapshot
        assertEquals(listOf("state-1", "state-2"), fresh.entries.map { it.id })
        assertFalse(fresh.stale)
        assertFalse(fresh.canLoadMore)
        assertEquals(2, pageTwoAttempts)
    }

    @Test
    fun `save does not change the visible state before server confirmation`() = runTest {
        val gate = CompletableDeferred<Resource<LibraryBookState>>()
        val repository = FakeRepository().apply { saveResponse = { gate.await() } }
        val feed = FakeFeed().apply {
            respond = {
                flowOf(
                    LibraryLoad.Fresh(
                        libraryPage(
                            1, 1,
                            sampleState(
                                status = if (repository.saveCalls.isEmpty()) LibraryStatus.READING else LibraryStatus.READ,
                            ),
                        ),
                    ),
                )
            }
        }
        val viewModel = vm(feed, repository)
        viewModel.refresh()
        runCurrent()

        viewModel.changeStatus("book-1", LibraryStatus.READ)
        runCurrent()

        assertIs<LibraryActionState.Working>(viewModel.uiState.value.action)
        assertEquals(LibraryStatus.READING, displayed(viewModel).single().status)
        assertEquals(
            LibraryStateInput(bookId = "book-1", status = LibraryStatus.READ),
            repository.saveCalls.single(),
        )

        gate.complete(Resource.Success(sampleState(status = LibraryStatus.READ)))
        runCurrent()

        assertEquals(LibraryStatus.READ, displayed(viewModel).single().status)
        assertIs<LibraryActionState.Succeeded>(viewModel.uiState.value.action)
        assertEquals(2, feed.queries.size)
    }

    @Test
    fun `failed save keeps previous snapshot and invalid progress never reaches backend`() = runTest {
        val repository = FakeRepository().apply {
            saveResponse = { Resource.Error("No se pudo conectar") }
        }
        val viewModel = vm(
            FakeFeed().apply { respond = { flowOf(LibraryLoad.Fresh(libraryPage(1, 1, sampleState()))) } },
            repository,
        )
        viewModel.refresh()
        runCurrent()

        viewModel.updateProgress("book-1", currentPage = 40, percentage = 50)
        runCurrent()

        assertEquals(10, displayed(viewModel).single().currentPage)
        assertEquals("No se pudo conectar", assertIs<LibraryActionState.Failed>(viewModel.uiState.value.action).message)
        assertEquals(
            LibraryStateInput(bookId = "book-1", currentPage = 40, progressPercentage = 50),
            repository.saveCalls.single(),
        )

        viewModel.updateProgress("book-1", currentPage = -1)
        assertIs<LibraryActionState.Failed>(viewModel.uiState.value.action)
        assertEquals(1, repository.saveCalls.size)
    }

    @Test
    fun `removal requires confirmation and deletes the personal state id only after success`() = runTest {
        var removed = false
        val repository = FakeRepository().apply {
            deleteResponse = { Resource.Error("Acceso denegado") }
        }
        val feed = FakeFeed().apply {
            respond = {
                flowOf(
                    LibraryLoad.Fresh(
                        if (removed) libraryPage(1, 1) else libraryPage(1, 1, sampleState()),
                    ),
                )
            }
        }
        val viewModel = vm(feed, repository)
        viewModel.refresh()
        runCurrent()

        viewModel.requestRemoval("book-1")
        assertNull(viewModel.uiState.value.pendingRemoval)
        viewModel.requestRemoval("state-1")
        assertEquals("state-1", viewModel.uiState.value.pendingRemoval?.id)
        assertTrue(repository.deleteCalls.isEmpty())

        viewModel.confirmRemoval()
        runCurrent()
        assertEquals(listOf("state-1"), repository.deleteCalls)
        assertIs<LibraryActionState.Failed>(viewModel.uiState.value.action)
        assertEquals("state-1", displayed(viewModel).single().id)

        repository.deleteResponse = { removed = true; Resource.Success(Unit) }
        viewModel.confirmRemoval()
        runCurrent()

        assertEquals(listOf("state-1", "state-1"), repository.deleteCalls)
        assertNull(viewModel.uiState.value.pendingRemoval)
        assertEquals(LibraryContentState.Empty(), viewModel.uiState.value.content)
        assertIs<LibraryActionState.Succeeded>(viewModel.uiState.value.action)
    }

    private fun vm(
        feed: FakeFeed,
        repository: FakeRepository = FakeRepository(),
    ): LibraryExperienceViewModel = LibraryExperienceViewModel(
        ObserveLibraryOnlineFirst(feed),
        SaveLibraryState(repository),
        DeleteLibraryState(repository),
    )

    private fun displayed(viewModel: LibraryExperienceViewModel): List<LibraryBookState> =
        assertIs<LibraryContentState.Ready>(viewModel.uiState.value.content).snapshot.entries

    private data class LibraryQuery(
        val page: Int,
        val limit: Int,
        val status: LibraryStatus?,
        val favoritesOnly: Boolean,
    )

    private class FakeFeed : LibraryFeedRepository {
        val queries = mutableListOf<LibraryQuery>()
        var respond: (LibraryQuery) -> Flow<LibraryLoad> = {
            flowOf(LibraryLoad.Fresh(libraryPage(1, 1)))
        }

        override fun observeLibrary(
            page: Int,
            limit: Int,
            status: LibraryStatus?,
            favoritesOnly: Boolean,
        ): Flow<LibraryLoad> {
            val query = LibraryQuery(page, limit, status, favoritesOnly)
            queries += query
            return respond(query)
        }
    }

    private class FakeRepository : LibraryRepository {
        val saveCalls = mutableListOf<LibraryStateInput>()
        val deleteCalls = mutableListOf<String>()
        var saveResponse: suspend (LibraryStateInput) -> Resource<LibraryBookState> = {
            Resource.Success(sampleState())
        }
        var deleteResponse: suspend (String) -> Resource<Unit> = { Resource.Success(Unit) }

        override suspend fun getLibrary(
            page: Int, limit: Int, status: LibraryStatus?, favoritesOnly: Boolean,
        ): Resource<LibraryPage> = Resource.Error("Not used")

        override suspend fun saveState(input: LibraryStateInput): Resource<LibraryBookState> {
            saveCalls += input
            return saveResponse(input)
        }

        override suspend fun deleteState(stateId: String): Resource<Unit> {
            deleteCalls += stateId
            return deleteResponse(stateId)
        }

        override suspend fun createReadingSession(
            stateId: String, input: ReadingSessionInput, idempotencyKey: String,
        ): Resource<ReadingSession> = Resource.Error("Not used")

        override suspend fun getReadingSessions(stateId: String): Resource<List<ReadingSession>> =
            Resource.Error("Not used")
    }
}

private fun libraryPage(page: Int, totalPages: Int, vararg entries: LibraryBookState): LibraryPage =
    LibraryPage(entries.toList(), page, 20, entries.size, totalPages)

private fun sampleState(
    id: String = "state-1",
    bookId: String = "book-1",
    status: LibraryStatus = LibraryStatus.READING,
    currentPage: Int = 10,
): LibraryBookState = LibraryBookState(
    id = id,
    bookId = bookId,
    book = Book(
        id = bookId,
        googleBookId = "google-$bookId",
        title = "Libro $bookId",
        authors = listOf("Autora"),
        description = null,
        genres = emptyList(),
        coverUrl = null,
        previewLink = null,
        infoLink = null,
        buyLink = null,
    ),
    status = status,
    rating = null,
    comment = null,
    isFavorite = false,
    currentPage = currentPage,
    progressPercentage = null,
    startedAt = null,
    finishedAt = null,
    isAbandoned = false,
    privateNotes = null,
    updatedAt = "2026-10-29T00:00:00Z",
)

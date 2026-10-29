package org.bookie.app.presentation.progress

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.bookie.app.domain.model.*
import org.bookie.app.domain.repository.LibraryRepository
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.usecase.CreateReadingSession
import org.bookie.app.domain.usecase.GetReadingSessions
import org.bookie.app.domain.usecase.SaveLibraryState
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ReadingProgressViewModelTest {
    @BeforeTest fun setup() { Dispatchers.setMain(StandardTestDispatcher()) }
    @AfterTest fun teardown() { Dispatchers.resetMain() }

    @Test fun `invalid page and percentage do not call backend`() = runTest {
        val repo = FakeLibraryRepository()
        val vm = vm(repo)
        vm.onCurrentPageChanged("-1")
        vm.onPercentageChanged("101")
        vm.saveSnapshot()
        runCurrent()
        val state = vm.uiState.value as ReadingProgressUiState.Content
        assertNotNull(state.currentPageError)
        assertNotNull(state.percentageError)
        assertEquals(0, repo.snapshotCalls)
    }

    @Test fun `snapshot success is separate from session failure`() = runTest {
        val repo = FakeLibraryRepository().apply { sessionResult = Resource.Error("Sin conexión") }
        val vm = vm(repo)
        vm.onCurrentPageChanged("42")
        vm.saveSnapshot()
        runCurrent()
        assertIs<ProgressOperationStatus.Success>((vm.uiState.value as ReadingProgressUiState.Content).snapshotStatus)
        vm.recordSession()
        runCurrent()
        val state = vm.uiState.value as ReadingProgressUiState.Content
        assertIs<ProgressOperationStatus.Success>(state.snapshotStatus)
        assertIs<ProgressOperationStatus.Error>(state.sessionStatus)
        assertEquals(1, repo.snapshotCalls)
        assertEquals(1, repo.sessionKeys.size)
    }

    @Test fun `retrying same session keeps idempotency key`() = runTest {
        val repo = FakeLibraryRepository().apply { sessionResult = Resource.Error("Sin conexión") }
        var counter = 0
        val vm = vm(repo, SessionIdempotencyKeyGenerator { "key-${++counter}" })
        vm.onCurrentPageChanged("8")
        vm.recordSession()
        runCurrent()
        vm.recordSession()
        runCurrent()
        assertEquals(listOf("key-1", "key-1"), repo.sessionKeys)
        vm.startNewSessionAttempt()
        vm.recordSession()
        runCurrent()
        assertEquals("key-2", repo.sessionKeys.last())
    }

    @Test fun `editing session payload generates new key`() = runTest {
        val repo = FakeLibraryRepository().apply { sessionResult = Resource.Error("timeout") }
        var counter = 0
        val vm = vm(repo, SessionIdempotencyKeyGenerator { "key-${++counter}" })
        vm.onSessionNotesChanged("primera")
        vm.recordSession()
        runCurrent()
        vm.onSessionNotesChanged("segunda")
        vm.recordSession()
        runCurrent()
        assertEquals(listOf("key-1", "key-2"), repo.sessionKeys)
    }

    @Test fun `failed snapshot never reports saved`() = runTest {
        val repo = FakeLibraryRepository().apply { snapshotResult = Resource.Error("HTTP 500") }
        val vm = vm(repo)
        vm.onCurrentPageChanged("12")
        vm.saveSnapshot()
        runCurrent()
        assertIs<ProgressOperationStatus.Error>((vm.uiState.value as ReadingProgressUiState.Content).snapshotStatus)
    }

    @Test fun `history is loaded separately`() = runTest {
        val repo = FakeLibraryRepository()
        val vm = vm(repo)
        vm.loadSessions()
        runCurrent()
        assertEquals(1, repo.historyCalls)
        assertFalse((vm.uiState.value as ReadingProgressUiState.Content).historyLoading)
    }

    private fun vm(
        repo: FakeLibraryRepository,
        keys: SessionIdempotencyKeyGenerator = SessionIdempotencyKeyGenerator { "key-1" },
    ) = ReadingProgressViewModel(
        libraryState = state(),
        saveLibraryState = SaveLibraryState(repo),
        createReadingSession = CreateReadingSession(repo),
        getReadingSessions = GetReadingSessions(repo),
        keyGenerator = keys,
    )

    private fun state() = LibraryBookState(
        id = "state-1", bookId = "book-1",
        book = Book("book-1", "google-1", "Dune", listOf("Frank Herbert"), null, emptyList(), null, null, null, null),
        status = LibraryStatus.READING, rating = null, comment = null,
        isFavorite = false, currentPage = null, progressPercentage = null,
        startedAt = null, finishedAt = null, isAbandoned = false,
        privateNotes = null, updatedAt = "2026-10-29T00:00:00Z",
    )

    private class FakeLibraryRepository : LibraryRepository {
        var snapshotCalls = 0
        var historyCalls = 0
        val sessionKeys = mutableListOf<String>()
        var snapshotResult: Resource<LibraryBookState> = Resource.Error("unused")
        var sessionResult: Resource<ReadingSession> = Resource.Error("unused")

        override suspend fun getLibrary(page: Int, limit: Int, status: LibraryStatus?, favoritesOnly: Boolean): Resource<LibraryPage> =
            Resource.Error("unused")

        override suspend fun saveState(input: LibraryStateInput): Resource<LibraryBookState> {
            snapshotCalls++
            return if (snapshotResult is Resource.Error && (snapshotResult as Resource.Error).message == "unused") {
                Resource.Success(
                    LibraryBookState(
                        id = "state-1", bookId = input.bookId,
                        book = Book("book-1", "google-1", "Dune", listOf("Frank Herbert"), null, emptyList(), null, null, null, null),
                        status = LibraryStatus.READING, rating = null, comment = null, isFavorite = false,
                        currentPage = input.currentPage, progressPercentage = input.progressPercentage,
                        startedAt = null, finishedAt = null, isAbandoned = false,
                        privateNotes = input.privateNotes, updatedAt = "2026-10-29T00:00:00Z",
                    ),
                )
            } else snapshotResult
        }

        override suspend fun createReadingSession(
            stateId: String, input: ReadingSessionInput, idempotencyKey: String,
        ): Resource<ReadingSession> {
            sessionKeys += idempotencyKey
            return sessionResult
        }

        override suspend fun getReadingSessions(stateId: String): Resource<List<ReadingSession>> {
            historyCalls++
            return Resource.Success(emptyList())
        }
    }
}

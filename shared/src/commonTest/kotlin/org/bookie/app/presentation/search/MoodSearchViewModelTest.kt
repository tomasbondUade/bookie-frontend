package org.bookie.app.presentation.search

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.bookie.app.domain.model.AuthState
import org.bookie.app.domain.model.Book
import org.bookie.app.domain.model.LibraryBookState
import org.bookie.app.domain.model.LibraryPage
import org.bookie.app.domain.model.LibraryStateInput
import org.bookie.app.domain.model.LibraryStatus
import org.bookie.app.domain.model.MoodBook
import org.bookie.app.domain.model.ReadingSession
import org.bookie.app.domain.model.ReadingSessionInput
import org.bookie.app.domain.repository.AuthRepository
import org.bookie.app.domain.repository.LibraryRepository
import org.bookie.app.domain.repository.MoodBookRepository
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind
import org.bookie.app.domain.usecase.SaveLibraryState
import org.bookie.app.domain.usecase.SearchBooksByMood
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MoodSearchViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun publicSearchDoesNotSendAnonymousLibrarySave() = runTest {
        val library = FakeLibraryRepository()
        val viewModel = viewModel(library, auth = null)
        selectBook(viewModel)

        viewModel.saveSelectedBook()
        runCurrent()

        assertEquals(MoodSaveUiState.SignInRequired, viewModel.saveState.value)
        assertTrue(library.inputs.isEmpty())
    }

    @Test
    fun signedOutAndExpiredSessionsDoNotSendLibrarySave() = runTest {
        val library = FakeLibraryRepository()
        val auth = FakeAuthRepository(AuthState.SignedOut)
        val viewModel = viewModel(library, auth)
        selectBook(viewModel)

        viewModel.saveSelectedBook()
        runCurrent()
        assertEquals(MoodSaveUiState.SignInRequired, viewModel.saveState.value)
        assertTrue(library.inputs.isEmpty())

        auth.setState(AuthState.Expired)
        viewModel.saveSelectedBook()
        runCurrent()
        assertEquals(MoodSaveUiState.SignInRequired, viewModel.saveState.value)
        assertTrue(library.inputs.isEmpty())
    }

    @Test
    fun signedInSavesOnlyAfterBackendConfirmation() = runTest {
        val gate = CompletableDeferred<Resource<LibraryBookState>>()
        val library = FakeLibraryRepository().apply { response = { gate.await() } }
        val viewModel = viewModel(library, FakeAuthRepository(signedIn()))
        selectBook(viewModel)

        viewModel.saveSelectedBook()
        runCurrent()

        assertIs<MoodSaveUiState.Saving>(viewModel.saveState.value)
        assertEquals(
            LibraryStateInput(bookId = "book-123", status = LibraryStatus.TO_READ),
            library.inputs.single(),
        )

        gate.complete(Resource.Success(libraryState()))
        runCurrent()
        assertIs<MoodSaveUiState.Saved>(viewModel.saveState.value)

        viewModel.saveSelectedBook()
        runCurrent()
        assertEquals(1, library.inputs.size)
    }

    @Test
    fun serverFailureDoesNotPretendBookWasSaved() = runTest {
        val library = FakeLibraryRepository().apply {
            response = { Resource.Error("No se pudo guardar") }
        }
        val viewModel = viewModel(library, FakeAuthRepository(signedIn()))
        selectBook(viewModel)

        viewModel.saveSelectedBook()
        runCurrent()

        assertEquals(MoodSaveUiState.Error("No se pudo guardar"), viewModel.saveState.value)
        assertEquals(1, library.inputs.size)
    }

    @Test
    fun unauthorizedSaveAsksUserToSignInAgain() = runTest {
        val library = FakeLibraryRepository().apply {
            response = { Resource.Error("El servidor respondió con HTTP 401.") }
        }
        val viewModel = viewModel(library, FakeAuthRepository(signedIn()))
        selectBook(viewModel)

        viewModel.saveSelectedBook()
        runCurrent()

        assertEquals(MoodSaveUiState.SignInRequired, viewModel.saveState.value)
        assertEquals(1, library.inputs.size)
    }

    @Test
    fun typedUnauthorizedSaveAlsoAsksForSignIn() = runTest {
        val library = FakeLibraryRepository().apply {
            response = {
                Resource.Error("No autorizado", kind = ResourceErrorKind.UNAUTHORIZED)
            }
        }
        val viewModel = viewModel(library, FakeAuthRepository(signedIn()))
        selectBook(viewModel)

        viewModel.saveSelectedBook()
        runCurrent()

        assertEquals(MoodSaveUiState.SignInRequired, viewModel.saveState.value)
    }

    private fun viewModel(
        library: FakeLibraryRepository,
        auth: AuthRepository?,
    ): MoodSearchViewModel = MoodSearchViewModel(
        searchBooksByMood = SearchBooksByMood(
            object : MoodBookRepository {
                override suspend fun searchByMood(query: String): Resource<List<MoodBook>> =
                    Resource.Success(listOf(moodBook()))
            },
        ),
        saveLibraryState = SaveLibraryState(library),
        authRepository = auth,
    )

    private suspend fun selectBook(viewModel: MoodSearchViewModel) {
        viewModel.onQueryChanged("Una aventura")
        viewModel.search()
        // Execute the pending Main coroutine before opening the book detail.
        kotlinx.coroutines.test.runCurrent()
        val success = assertIs<MoodSearchUiState.Success>(viewModel.uiState.value)
        viewModel.selectBook(success.books.single())
    }

    private fun signedIn() = AuthState.SignedIn("user-123", "reader@example.test")

    private fun moodBook() = MoodBook(
        book = Book(
            id = "book-123",
            googleBookId = "google-123",
            title = "Una aventura",
            authors = listOf("Autora"),
            description = null,
            genres = listOf("Fantasía"),
            coverUrl = null,
            previewLink = null,
            infoLink = null,
            buyLink = null,
        ),
        relevance = 0.87,
    )

    private fun libraryState(): LibraryBookState = LibraryBookState(
        id = "state-123",
        bookId = "book-123",
        book = moodBook().book,
        status = LibraryStatus.TO_READ,
        rating = null,
        comment = null,
        isFavorite = false,
        currentPage = null,
        progressPercentage = null,
        startedAt = null,
        finishedAt = null,
        isAbandoned = false,
        privateNotes = null,
        updatedAt = "2026-11-17T12:00:00Z",
    )

    private class FakeAuthRepository(initial: AuthState) : AuthRepository {
        private val mutable = MutableStateFlow(initial)
        override val state: StateFlow<AuthState> = mutable
        fun setState(next: AuthState) { mutable.value = next }
        override suspend fun restoreSession(): AuthState = mutable.value
        override suspend fun signIn(email: String, password: String): AuthState = mutable.value
        override suspend fun signUp(email: String, password: String, username: String): AuthState = mutable.value
        override suspend fun signOut(): AuthState {
            mutable.value = AuthState.SignedOut
            return mutable.value
        }
    }

    private class FakeLibraryRepository : LibraryRepository {
        val inputs = mutableListOf<LibraryStateInput>()
        var response: suspend (LibraryStateInput) -> Resource<LibraryBookState> = {
            Resource.Error("No configurado")
        }

        override suspend fun saveState(input: LibraryStateInput): Resource<LibraryBookState> {
            inputs += input
            return response(input)
        }

        override suspend fun getLibrary(
            page: Int,
            limit: Int,
            status: LibraryStatus?,
            favoritesOnly: Boolean,
        ): Resource<LibraryPage> = Resource.Error("No se usa")

        override suspend fun deleteState(stateId: String): Resource<Unit> =
            Resource.Error("No se usa")

        override suspend fun createReadingSession(
            stateId: String,
            input: ReadingSessionInput,
            idempotencyKey: String,
        ): Resource<ReadingSession> = Resource.Error("No se usa")

        override suspend fun getReadingSessions(stateId: String): Resource<List<ReadingSession>> =
            Resource.Error("No se usa")
    }
}

package org.bookie.app.presentation.preferences

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.bookie.app.domain.model.Book
import org.bookie.app.domain.model.ReadingPreferences
import org.bookie.app.domain.model.ReadingPreferencesInput
import org.bookie.app.domain.repository.BookRepository
import org.bookie.app.domain.repository.ReadingPreferencesRepository
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.usecase.GetReadingPreferences
import org.bookie.app.domain.usecase.SaveReadingPreferences
import org.bookie.app.domain.usecase.SearchBooks
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class ReadingPreferencesViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts onboarding with preferences returned by backend`() = runTest {
        val expected = samplePreferences(onboardingCompleted = false)
        val repository = FakePreferencesRepository(getResults = mutableListOf(Resource.Success(expected)))
        val viewModel = viewModel(repository)

        viewModel.loadForAccount("account-1")
        runCurrent()

        val state = assertIs<ReadingPreferencesUiState.Editing>(viewModel.uiState.value)
        assertEquals(true, state.isOnboarding)
        assertEquals(expected.preferredGenres, state.preferredGenres)
        assertEquals(expected.favoriteBookIds, state.favoriteBookIds)
    }

    @Test
    fun `completed preferences are shown and can be edited without losing their baseline`() = runTest {
        val expected = samplePreferences()
        val repository = FakePreferencesRepository(getResults = mutableListOf(Resource.Success(expected)))
        val viewModel = viewModel(repository)

        viewModel.load()
        runCurrent()
        viewModel.beginEdit()

        val state = assertIs<ReadingPreferencesUiState.Editing>(viewModel.uiState.value)
        assertEquals(false, state.isOnboarding)
        assertEquals(expected, state.baseline)
    }

    @Test
    fun `searches and toggles a book as a favorite`() = runTest {
        val book = sampleBook()
        val searchRepository = FakeBookRepository(Resource.Success(listOf(book)))
        val viewModel = viewModel(
            preferencesRepository = FakePreferencesRepository(
                getResults = mutableListOf(Resource.Success(emptyPreferences(onboardingCompleted = false))),
            ),
            bookRepository = searchRepository,
        )
        viewModel.load()
        runCurrent()

        viewModel.updateSearchQuery("  dune ")
        viewModel.search()
        runCurrent()
        assertEquals("dune", searchRepository.lastQuery)
        assertEquals(BookSearchState.Results(listOf(book)), assertIs<ReadingPreferencesUiState.Editing>(viewModel.uiState.value).searchState)

        viewModel.toggleFavorite(book)
        assertEquals(listOf(book.id), assertIs<ReadingPreferencesUiState.Editing>(viewModel.uiState.value).favoriteBookIds)
        viewModel.toggleFavorite(book)
        assertEquals(emptyList(), assertIs<ReadingPreferencesUiState.Editing>(viewModel.uiState.value).favoriteBookIds)
    }

    @Test
    fun `normalizes and deduplicates entered genres and authors`() = runTest {
        val viewModel = viewModel(
            preferencesRepository = FakePreferencesRepository(
                getResults = mutableListOf(Resource.Success(emptyPreferences(onboardingCompleted = false))),
            ),
        )
        viewModel.load()
        runCurrent()

        viewModel.updateGenreInput(" Fantasy, Mystery, fantasy ")
        viewModel.addGenres()
        viewModel.updateAuthorInput(" Ursula Le Guin, Ursula Le Guin ")
        viewModel.addAuthors()

        val state = assertIs<ReadingPreferencesUiState.Editing>(viewModel.uiState.value)
        assertEquals(listOf("Fantasy", "Mystery"), state.preferredGenres)
        assertEquals(listOf("Ursula Le Guin"), state.preferredAuthors)
    }

    @Test
    fun `save failure keeps draft and only transitions after backend confirms onboarding`() = runTest {
        val repository = FakePreferencesRepository(
            getResults = mutableListOf(Resource.Success(samplePreferences(onboardingCompleted = false))),
            saveResults = mutableListOf(
                Resource.Error("Sin conexión"),
                Resource.Success(samplePreferences(onboardingCompleted = true)),
            ),
        )
        val viewModel = viewModel(repository)
        viewModel.load()
        runCurrent()
        viewModel.updateGenreInput("Misterio")
        viewModel.addGenres()

        viewModel.save()
        runCurrent()
        val failedDraft = assertIs<ReadingPreferencesUiState.Editing>(viewModel.uiState.value)
        assertEquals("Sin conexión", failedDraft.error)
        assertEquals(listOf("Misterio", "Fantasia"), failedDraft.preferredGenres)
        assertEquals(1, repository.saveCalls)

        viewModel.save()
        runCurrent()
        assertEquals(2, repository.saveCalls)
        assertIs<ReadingPreferencesUiState.Viewing>(viewModel.uiState.value)
    }

    @Test
    fun `changing account clears previous preferences before loading the next account`() = runTest {
        val repository = FakePreferencesRepository(
            getResults = mutableListOf(
                Resource.Success(samplePreferences().copy(preferredGenres = listOf("Fantasía"))),
                Resource.Success(samplePreferences().copy(preferredGenres = listOf("Misterio"))),
            ),
        )
        val viewModel = viewModel(repository)

        viewModel.loadForAccount("account-1")
        runCurrent()
        assertEquals(
            listOf("Fantasía"),
            assertIs<ReadingPreferencesUiState.Viewing>(viewModel.uiState.value).preferences.preferredGenres,
        )

        viewModel.loadForAccount("account-2")
        assertEquals(ReadingPreferencesUiState.Loading, viewModel.uiState.value)
        runCurrent()
        assertEquals(
            listOf("Misterio"),
            assertIs<ReadingPreferencesUiState.Viewing>(viewModel.uiState.value).preferences.preferredGenres,
        )
    }

    @Test
    fun `late response from previous account cannot replace current preferences`() = runTest {
        val firstAccountResponse = CompletableDeferred<Resource<ReadingPreferences>>()
        val repository = object : ReadingPreferencesRepository {
            var calls = 0

            override suspend fun getPreferences(): Resource<ReadingPreferences> {
                calls++
                return if (calls == 1) {
                    firstAccountResponse.await()
                } else {
                    Resource.Success(samplePreferences().copy(preferredGenres = listOf("Misterio")))
                }
            }

            override suspend fun savePreferences(input: ReadingPreferencesInput): Resource<ReadingPreferences> =
                Resource.Error("Not used")
        }
        val viewModel = ReadingPreferencesViewModel(
            getPreferences = GetReadingPreferences(repository),
            savePreferences = SaveReadingPreferences(repository),
            searchBooks = SearchBooks(FakeBookRepository(Resource.Success(emptyList()))),
        )

        viewModel.loadForAccount("account-1")
        runCurrent()
        viewModel.loadForAccount("account-2")
        runCurrent()
        assertEquals(
            listOf("Misterio"),
            assertIs<ReadingPreferencesUiState.Viewing>(viewModel.uiState.value).preferences.preferredGenres,
        )

        firstAccountResponse.complete(
            Resource.Success(samplePreferences().copy(preferredGenres = listOf("Fantasía"))),
        )
        runCurrent()

        assertEquals(
            listOf("Misterio"),
            assertIs<ReadingPreferencesUiState.Viewing>(viewModel.uiState.value).preferences.preferredGenres,
        )
    }

    private fun viewModel(
        preferencesRepository: FakePreferencesRepository,
        bookRepository: FakeBookRepository = FakeBookRepository(Resource.Success(emptyList())),
    ) = ReadingPreferencesViewModel(
        getPreferences = GetReadingPreferences(preferencesRepository),
        savePreferences = SaveReadingPreferences(preferencesRepository),
        searchBooks = SearchBooks(bookRepository),
    )

    private fun samplePreferences(onboardingCompleted: Boolean = true) = ReadingPreferences(
        preferredGenres = listOf("Fantasia"),
        preferredAuthors = listOf("Ursula Le Guin"),
        favoriteBookIds = listOf("book-1"),
        onboardingCompleted = onboardingCompleted,
    )

    private fun emptyPreferences(onboardingCompleted: Boolean) = ReadingPreferences(
        preferredGenres = emptyList(),
        preferredAuthors = emptyList(),
        favoriteBookIds = emptyList(),
        onboardingCompleted = onboardingCompleted,
    )

    private fun sampleBook() = Book(
        id = "book-1",
        googleBookId = "google-1",
        title = "Dune",
        authors = listOf("Frank Herbert"),
        description = null,
        genres = listOf("Sci-fi"),
        coverUrl = null,
        previewLink = null,
        infoLink = null,
        buyLink = null,
    )

    private class FakePreferencesRepository(
        private val getResults: MutableList<Resource<ReadingPreferences>>,
        private val saveResults: MutableList<Resource<ReadingPreferences>> = mutableListOf(),
    ) : ReadingPreferencesRepository {
        var saveCalls = 0
            private set
        var lastInput: ReadingPreferencesInput? = null
            private set

        override suspend fun getPreferences(): Resource<ReadingPreferences> = getResults.removeAt(0)

        override suspend fun savePreferences(input: ReadingPreferencesInput): Resource<ReadingPreferences> {
            saveCalls++
            lastInput = input
            return saveResults.removeAt(0)
        }
    }

    private class FakeBookRepository(
        private val result: Resource<List<Book>>,
    ) : BookRepository {
        var lastQuery: String? = null
            private set

        override suspend fun searchBooks(query: String, page: Int): Resource<List<Book>> {
            lastQuery = query
            return result
        }
    }
}

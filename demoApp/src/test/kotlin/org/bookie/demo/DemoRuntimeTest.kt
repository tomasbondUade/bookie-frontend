package org.bookie.demo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.bookie.app.presentation.auth.AuthUiState
import org.bookie.app.presentation.library.LibraryContentState
import org.bookie.app.presentation.lists.BookListsContentState
import org.bookie.app.presentation.search.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class DemoRuntimeTest {
    @Test fun signInRetainsTheBookBeingBrowsedAndLoadsPrivateData() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val runtime = DemoRuntime()
        try {
            val before = runtime.features.value
            val auth = requireNotNull(before.auth)
            auth.restore(); advanceUntilIdle()
            val search = requireNotNull(before.bookSearch)
            search.onQueryChanged("Dune"); search.search(); advanceUntilIdle()
            val book = assertIs<SearchUiState.Success>(search.uiState.value).books.single()
            search.selectBook(book)
            auth.updateEmail(DEMO_EMAIL); auth.submit(DEMO_PASSWORD); advanceUntilIdle()
            assertIs<AuthUiState.SignedIn>(auth.uiState.value)
            val after = runtime.features.value
            assertSame(search, after.bookSearch)
            assertEquals(book.id, assertIs<SearchUiState.Success>(search.uiState.value).selectedBook?.id)
            val library = requireNotNull(after.libraryExperience)
            library.refresh(); runCurrent()
            assertTrue(assertIs<LibraryContentState.Ready>(library.uiState.value.content).snapshot.entries.isNotEmpty())
        } finally { runtime.close(); Dispatchers.resetMain() }
    }

    @Test fun signInRetainsMoodDetailAndChangingAccountRebuildsPersonalLists() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val runtime = DemoRuntime()
        try {
            val before = runtime.features.value
            val mood = requireNotNull(before.moodSearch)
            mood.onQueryChanged("aventura"); mood.search(); advanceUntilIdle()
            val result = assertIs<MoodSearchUiState.Success>(mood.uiState.value).books.first()
            mood.selectBook(result)
            val auth = requireNotNull(before.auth)
            auth.restore(); advanceUntilIdle()
            auth.updateEmail(DEMO_EMAIL); auth.submit(DEMO_PASSWORD); advanceUntilIdle()
            val restored = requireNotNull(runtime.features.value.moodSearch)
            assertEquals(result.book.id, assertIs<MoodSearchUiState.Success>(restored.uiState.value).selectedBook?.book?.id)
            val lists = requireNotNull(runtime.features.value.bookLists)
            lists.load(); advanceUntilIdle()
            assertTrue(assertIs<BookListsContentState.Ready>(lists.uiState.value.content).snapshot.lists.isNotEmpty())
            auth.signOut(); advanceUntilIdle()
            runtime.data.signUp("nueva@example.com", "prueba123", "Nueva Demo"); advanceUntilIdle()
            val newLists = requireNotNull(runtime.features.value.bookLists)
            assertNotSame(lists, newLists)
            newLists.load(); advanceUntilIdle()
            assertTrue(assertIs<BookListsContentState.Ready>(newLists.uiState.value.content).snapshot.lists.isEmpty())
        } finally { runtime.close(); Dispatchers.resetMain() }
    }
}

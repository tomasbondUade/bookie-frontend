package org.bookie.app.presentation.search

import org.bookie.app.domain.model.Book
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SearchUiStateTest {
    @Test
    fun emptyResultsAreRepresentedAsSuccessRatherThanIdle() {
        val state: SearchUiState = SearchUiState.Success(emptyList())

        assertNotEquals(SearchUiState.Idle, state)
        assertTrue(state is SearchUiState.Success)
        assertTrue(state.books.isEmpty())
    }

    @Test
    fun successCarriesTheExistingDomainBook() {
        val book = Book(
            id = "book-1",
            googleBookId = "google-1",
            title = "Ficciones",
            authors = listOf("Jorge Luis Borges"),
            description = null,
            genres = listOf("Fiction"),
            coverUrl = null,
            previewLink = null,
            infoLink = null,
            buyLink = null,
        )

        val state = SearchUiState.Success(listOf(book))

        assertEquals(listOf(book), state.books)
    }
}

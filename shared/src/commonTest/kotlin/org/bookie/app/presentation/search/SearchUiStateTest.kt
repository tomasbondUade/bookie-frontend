package org.bookie.app.presentation.search

import org.bookie.app.domain.model.Book
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchUiStateTest {
    @Test
    fun emptyResultsHaveAnExplicitState() {
        val state = SearchUiState.Empty("unknown")

        assertEquals(SearchUiState.Empty("unknown"), state)
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

        val state = SearchUiState.Success(listOf(book), "ficciones")

        assertEquals(listOf(book), state.books)
    }
}

package org.bookie.app.domain.usecase

import kotlinx.coroutines.test.runTest
import org.bookie.app.domain.model.Book
import org.bookie.app.domain.repository.BookRepository
import org.bookie.app.domain.result.Resource
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchBooksTest {
    @Test
    fun returnsBooksFromRepository() = runTest {
        val expectedBooks = listOf(
            Book(
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
            ),
        )
        val repository = FakeBookRepository(Resource.Success(expectedBooks))
        val searchBooks = SearchBooks(repository)

        val result = searchBooks("ficciones")

        assertEquals("ficciones", repository.receivedQuery)
        assertEquals(Resource.Success(expectedBooks), result)
    }

    @Test
    fun returnsRepositoryError() = runTest {
        val expectedError = Resource.Error("Search failed")
        val searchBooks = SearchBooks(FakeBookRepository(expectedError))

        assertEquals(expectedError, searchBooks("ficciones"))
    }

    private class FakeBookRepository(
        private val result: Resource<List<Book>>,
    ) : BookRepository {
        var receivedQuery: String? = null
            private set

        override suspend fun searchBooks(query: String): Resource<List<Book>> {
            receivedQuery = query
            return result
        }
    }
}

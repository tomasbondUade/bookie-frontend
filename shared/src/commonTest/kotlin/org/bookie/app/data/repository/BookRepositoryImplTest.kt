package org.bookie.app.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.bookie.app.data.remote.BookieHttpClientConfig
import org.bookie.app.data.remote.KtorBookSearchRemoteDataSource
import org.bookie.app.data.remote.createBookieHttpClient
import org.bookie.app.domain.model.Book
import org.bookie.app.domain.result.Resource
import kotlin.test.Test
import kotlin.test.assertEquals

class BookRepositoryImplTest {
    @Test
    fun `searches the public endpoint and maps its response to domain books`() = runTest {
        var requestUrl = ""
        var authorization: String? = "unexpected"
        val client = createBookieHttpClient(
            engine = MockEngine { request ->
                requestUrl = request.url.toString()
                authorization = request.headers[HttpHeaders.Authorization]
                respond(
                    content = """[
                        {
                            "id":"book-1",
                            "googleBookId":"google-1",
                            "title":"Dune",
                            "authors":["Frank Herbert"],
                            "description":"A science fiction novel",
                            "genres":["Science Fiction"],
                            "coverUrl":"https://books.example.test/dune.jpg",
                            "previewLink":"https://books.example.test/dune/preview",
                            "infoLink":"https://books.example.test/dune",
                            "buyLink":null,
                            "futureField":"ignored"
                        }
                    ]""".trimIndent(),
                    headers = jsonHeaders(),
                )
            },
            config = BookieHttpClientConfig(baseUrl = "https://api.example.test/v1"),
        )
        val repository = BookRepositoryImpl(KtorBookSearchRemoteDataSource(client))

        val result = repository.searchBooks("  Dune  ")

        assertEquals("https://api.example.test/v1/books/search?q=Dune", requestUrl)
        assertEquals(null, authorization)
        assertEquals(
            Resource.Success(
                listOf(
                    Book(
                        id = "book-1",
                        googleBookId = "google-1",
                        title = "Dune",
                        authors = listOf("Frank Herbert"),
                        description = "A science fiction novel",
                        genres = listOf("Science Fiction"),
                        coverUrl = "https://books.example.test/dune.jpg",
                        previewLink = "https://books.example.test/dune/preview",
                        infoLink = "https://books.example.test/dune",
                        buyLink = null,
                    ),
                ),
            ),
            result,
        )
        client.close()
    }

    @Test
    fun `blank query returns an empty result without making a request`() = runTest {
        var requestCount = 0
        val client = createBookieHttpClient(
            engine = MockEngine {
                requestCount++
                respond("[]", headers = jsonHeaders())
            },
            config = BookieHttpClientConfig(baseUrl = "https://api.example.test"),
        )
        val repository = BookRepositoryImpl(KtorBookSearchRemoteDataSource(client))

        assertEquals(Resource.Success(emptyList()), repository.searchBooks("  "))
        assertEquals(0, requestCount)
        client.close()
    }

    @Test
    fun `http errors become repository errors`() = runTest {
        val client = createBookieHttpClient(
            engine = MockEngine {
                respond(
                    content = "service unavailable",
                    status = HttpStatusCode.ServiceUnavailable,
                )
            },
            config = BookieHttpClientConfig(baseUrl = "https://api.example.test"),
        )
        val repository = BookRepositoryImpl(KtorBookSearchRemoteDataSource(client))

        assertEquals(
            Resource.Error("El servidor respondió con HTTP 503."),
            repository.searchBooks("Dune"),
        )
        client.close()
    }

    @Test
    fun `malformed json becomes a repository error`() = runTest {
        val client = createBookieHttpClient(
            engine = MockEngine {
                respond("{invalid-json}", headers = jsonHeaders())
            },
            config = BookieHttpClientConfig(baseUrl = "https://api.example.test"),
        )
        val repository = BookRepositoryImpl(KtorBookSearchRemoteDataSource(client))

        assertEquals(
            Resource.Error("No se pudo interpretar la respuesta del servidor."),
            repository.searchBooks("Dune"),
        )
        client.close()
    }

    private fun jsonHeaders() = Headers.build {
        append(HttpHeaders.ContentType, "application/json")
    }
}

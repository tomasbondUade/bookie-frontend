package org.bookie.app.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.bookie.app.data.remote.BookieHttpClientConfig
import org.bookie.app.data.remote.KtorLibraryRemoteDataSource
import org.bookie.app.data.remote.createBookieHttpClient
import org.bookie.app.domain.model.Book
import org.bookie.app.domain.model.LibraryBookState
import org.bookie.app.domain.model.LibraryPage
import org.bookie.app.domain.model.LibraryStateInput
import org.bookie.app.domain.model.LibraryStatus
import org.bookie.app.domain.model.ReadingSession
import org.bookie.app.domain.model.ReadingSessionInput
import org.bookie.app.domain.result.Resource
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryRepositoryImplTest {
    @Test
    fun `maps paginated library response and nested books to domain`() = runTest {
        val client = client {
            respond(pageJson(), headers = jsonHeaders())
        }
        val repository = LibraryRepositoryImpl(KtorLibraryRemoteDataSource(client))

        val result = repository.getLibrary(
            page = 1,
            limit = 20,
            status = LibraryStatus.READING,
            favoritesOnly = true,
        )

        assertEquals(
            Resource.Success(
                LibraryPage(
                    books = listOf(
                        LibraryBookState(
                            id = "state-1",
                            bookId = "book-1",
                            book = Book(
                                id = "book-1",
                                googleBookId = "google-1",
                                title = "Dune",
                                authors = listOf("Frank Herbert"),
                                description = null,
                                genres = listOf("Science Fiction"),
                                coverUrl = null,
                                previewLink = null,
                                infoLink = null,
                                buyLink = null,
                            ),
                            status = LibraryStatus.READING,
                            rating = null,
                            comment = null,
                            isFavorite = false,
                            currentPage = 110,
                            progressPercentage = 45,
                            startedAt = null,
                            finishedAt = null,
                            isAbandoned = false,
                            privateNotes = null,
                            updatedAt = "2026-09-01T15:30:00.000Z",
                        ),
                    ),
                    page = 1,
                    limit = 20,
                    total = 1,
                    totalPages = 1,
                ),
            ),
            result,
        )
        client.close()
    }

    @Test
    fun `saves state and maps the server-confirmed response`() = runTest {
        var requestBody = ""
        val client = client { request ->
            requestBody = (request.body as io.ktor.http.content.TextContent).text
            respond(stateJson(), HttpStatusCode.Created, jsonHeaders())
        }
        val repository = LibraryRepositoryImpl(KtorLibraryRemoteDataSource(client))

        val result = repository.saveState(
            LibraryStateInput(bookId = "book-1", status = LibraryStatus.READING, currentPage = 110),
        )

        assertEquals("""{"bookId":"book-1","status":"READING","currentPage":110}""", requestBody)
        assertEquals(
            Resource.Success(
                LibraryBookState(
                    id = "state-1",
                    bookId = "book-1",
                    book = Book(
                        id = "book-1",
                        googleBookId = "google-1",
                        title = "Dune",
                        authors = listOf("Frank Herbert"),
                        description = null,
                        genres = listOf("Science Fiction"),
                        coverUrl = null,
                        previewLink = null,
                        infoLink = null,
                        buyLink = null,
                    ),
                    status = LibraryStatus.READING,
                    rating = null,
                    comment = null,
                    isFavorite = false,
                    currentPage = 110,
                    progressPercentage = 45,
                    startedAt = null,
                    finishedAt = null,
                    isAbandoned = false,
                    privateNotes = null,
                    updatedAt = "2026-09-01T15:30:00.000Z",
                ),
            ),
            result,
        )
        client.close()
    }

    @Test
    fun `deletes a personal state after HTTP 204 confirmation`() = runTest {
        var method: io.ktor.http.HttpMethod? = null
        var path = ""
        val client = client { request ->
            method = request.method
            path = request.url.encodedPath
            respond("", HttpStatusCode.NoContent)
        }
        val repository = LibraryRepositoryImpl(KtorLibraryRemoteDataSource(client))

        assertEquals(Resource.Success(Unit), repository.deleteState("state-1"))
        assertEquals(io.ktor.http.HttpMethod.Delete, method)
        assertEquals("/v1/library/state/state-1", path)
        client.close()
    }

    @Test
    fun `rejects deletion when backend denies ownership`() = runTest {
        val client = client { respond("", HttpStatusCode.Forbidden) }
        val repository = LibraryRepositoryImpl(KtorLibraryRemoteDataSource(client))

        assertEquals(
            Resource.Error("El servidor respondió con HTTP 403."),
            repository.deleteState("another-users-state"),
        )
        client.close()
    }

    @Test
    fun `passes through a stable key for session retries and maps response`() = runTest {
        var capturedKey: String? = null
        val client = client { request ->
            capturedKey = request.headers["Idempotency-Key"]
            respond(sessionJson(), HttpStatusCode.Created, jsonHeaders())
        }
        val repository = LibraryRepositoryImpl(KtorLibraryRemoteDataSource(client))

        val result = repository.createReadingSession(
            stateId = "state-1",
            input = ReadingSessionInput(currentPage = 110, progressPercentage = 45),
            idempotencyKey = "retry-me",
        )

        assertEquals("retry-me", capturedKey)
        assertEquals(
            Resource.Success(
                ReadingSession(
                    id = "session-1",
                    libraryStateId = "state-1",
                    idempotencyKey = "retry-me",
                    currentPage = 110,
                    progressPercentage = 45,
                    notes = null,
                    createdAt = "2026-09-01T15:30:00.000Z",
                ),
            ),
            result,
        )
        client.close()
    }

    @Test
    fun `maps session history and preserves missing legacy keys`() = runTest {
        val client = client {
            respond(
                """[{"id":"legacy","userBookStateId":"state-1","currentPage":90,"createdAt":"2026-09-02T10:00:00.000Z"}]""",
                headers = jsonHeaders(),
            )
        }
        val repository = LibraryRepositoryImpl(KtorLibraryRemoteDataSource(client))

        assertEquals(
            Resource.Success(
                listOf(
                    ReadingSession(
                        id = "legacy",
                        libraryStateId = "state-1",
                        idempotencyKey = null,
                        currentPage = 90,
                        progressPercentage = null,
                        notes = null,
                        createdAt = "2026-09-02T10:00:00.000Z",
                    ),
                ),
            ),
            repository.getReadingSessions("state-1"),
        )
        client.close()
    }

    @Test
    fun `maps authorization and idempotency conflicts to repository errors`() = runTest {
        val client = client {
            respond("", HttpStatusCode.Conflict)
        }
        val repository = LibraryRepositoryImpl(KtorLibraryRemoteDataSource(client))

        val result = repository.createReadingSession(
            stateId = "state-1",
            input = ReadingSessionInput(currentPage = 111),
            idempotencyKey = "already-used",
        )

        assertEquals(Resource.Error("El servidor respondió con HTTP 409."), result)
        client.close()
    }

    @Test
    fun `maps authentication failures and malformed responses to repository errors`() = runTest {
        val unauthorizedClient = client {
            respond("", HttpStatusCode.Unauthorized)
        }
        val unauthorizedRepository = LibraryRepositoryImpl(
            KtorLibraryRemoteDataSource(unauthorizedClient),
        )
        assertEquals(
            Resource.Error("El servidor respondió con HTTP 401."),
            unauthorizedRepository.getLibrary(),
        )
        unauthorizedClient.close()

        val malformedClient = client {
            respond("{invalid-json}", headers = jsonHeaders())
        }
        val malformedRepository = LibraryRepositoryImpl(
            KtorLibraryRemoteDataSource(malformedClient),
        )
        assertEquals(
            Resource.Error("No se pudo interpretar la respuesta del servidor."),
            malformedRepository.getLibrary(),
        )
        malformedClient.close()
    }

    private fun client(
        handler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(io.ktor.client.request.HttpRequestData) -> io.ktor.client.request.HttpResponseData,
    ) = createBookieHttpClient(
        engine = MockEngine(handler),
        config = BookieHttpClientConfig(baseUrl = "https://api.example.test/v1"),
    )

    private fun jsonHeaders() = Headers.build {
        append(HttpHeaders.ContentType, "application/json")
    }

    private fun stateJson() = """{"id":"state-1","bookId":"book-1","status":"READING","isFavorite":false,"currentPage":110,"progressPercentage":45,"isAbandoned":false,"updatedAt":"2026-09-01T15:30:00.000Z","book":{"id":"book-1","googleBookId":"google-1","title":"Dune","authors":["Frank Herbert"],"genres":["Science Fiction"]}}"""

    private fun pageJson() = """{"data":[${stateJson()}],"meta":{"page":1,"limit":20,"total":1,"totalPages":1}}"""

    private fun sessionJson() = """{"id":"session-1","userBookStateId":"state-1","idempotencyKey":"retry-me","currentPage":110,"progressPercentage":45,"createdAt":"2026-09-01T15:30:00.000Z"}"""
}

package org.bookie.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import org.bookie.app.data.remote.dto.CreateReadingSessionDto
import org.bookie.app.data.remote.dto.LibraryStatusDto
import org.bookie.app.data.remote.dto.SaveLibraryStateDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class LibraryRemoteDataSourceTest {
    @Test
    fun `requests a paginated filtered library with the current bearer token`() = runTest {
        var captured: io.ktor.client.request.HttpRequestData? = null
        val client = createClient(AccessTokenProvider { "session-token" }) { request ->
            captured = request
            respond(LIBRARY_PAGE_JSON, headers = jsonHeaders())
        }

        val result = KtorLibraryRemoteDataSource(client).getLibrary(
            page = 2,
            limit = 10,
            status = LibraryStatusDto.READING,
            favoritesOnly = true,
        )

        val request = assertNotNull(captured)
        assertEquals(HttpMethod.Get, request.method)
        assertEquals("/v1/library", request.url.encodedPath)
        assertEquals("2", request.url.parameters["page"])
        assertEquals("10", request.url.parameters["limit"])
        assertEquals("READING", request.url.parameters["status"])
        assertEquals("fav", request.url.parameters["filter"])
        assertEquals("Bearer session-token", request.headers[HttpHeaders.Authorization])
        assertEquals(1, result.data.size)
        assertEquals(1, result.meta.totalPages)
        client.close()
    }

    @Test
    fun `deletes a personal library state without deleting the catalog book`() = runTest {
        var captured: io.ktor.client.request.HttpRequestData? = null
        val client = createClient(AccessTokenProvider { "session-token" }) { request ->
            captured = request
            respond("{}", headers = jsonHeaders())
        }

        KtorLibraryRemoteDataSource(client).deleteState("state-1")

        val request = assertNotNull(captured)
        assertEquals(HttpMethod.Delete, request.method)
        assertEquals("/v1/library/state/state-1", request.url.encodedPath)
        assertEquals("Bearer session-token", request.headers[HttpHeaders.Authorization])
        client.close()
    }

    @Test
    fun `saves only personal state fields and never sends a user id`() = runTest {
        var captured: io.ktor.client.request.HttpRequestData? = null
        val client = createClient { request ->
            captured = request
            respond(LIBRARY_STATE_JSON, HttpStatusCode.Created, jsonHeaders())
        }

        KtorLibraryRemoteDataSource(client).saveState(
            SaveLibraryStateDto(bookId = "book-1", status = LibraryStatusDto.READING),
        )

        val request = assertNotNull(captured)
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/v1/library/state", request.url.encodedPath)
        val bodyContent = request.body as TextContent
        assertEquals("application/json", bodyContent.contentType.toString())
        val body = bodyContent.text
        assertEquals("""{"bookId":"book-1","status":"READING"}""", body)
        client.close()
    }

    @Test
    fun `creates reading sessions with a caller-owned idempotency key`() = runTest {
        var captured: io.ktor.client.request.HttpRequestData? = null
        val client = createClient(AccessTokenProvider { "session-token" }) { request ->
            captured = request
            respond(SESSION_JSON, HttpStatusCode.Created, jsonHeaders())
        }

        val result = KtorLibraryRemoteDataSource(client).createReadingSession(
            stateId = "state-1",
            input = CreateReadingSessionDto(currentPage = 110, progressPercentage = 45),
            idempotencyKey = "attempt-1",
        )

        val request = assertNotNull(captured)
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/v1/library/state/state-1/sessions", request.url.encodedPath)
        assertEquals("Bearer session-token", request.headers[HttpHeaders.Authorization])
        assertEquals("attempt-1", request.headers["Idempotency-Key"])
        assertEquals("session-1", result.id)
        assertEquals("attempt-1", result.idempotencyKey)
        client.close()
    }

    @Test
    fun `reads session history including legacy sessions without idempotency keys`() = runTest {
        var requestPath = ""
        val client = createClient { request ->
            requestPath = request.url.encodedPath
            respond("""[$SESSION_JSON,{"id":"legacy","userBookStateId":"state-1","createdAt":"2026-09-02T10:00:00.000Z"}]""", headers = jsonHeaders())
        }

        val sessions = KtorLibraryRemoteDataSource(client).getReadingSessions("state-1")

        assertEquals(2, sessions.size)
        assertEquals("/v1/library/state/state-1/sessions", requestPath)
        assertEquals("attempt-1", sessions.first().idempotencyKey)
        assertEquals(null, sessions.last().idempotencyKey)
        client.close()
    }

    @Test
    fun `rejects invalid page bounds and idempotency keys before making requests`() = runTest {
        var requestCount = 0
        val client = createClient {
            requestCount++
            respond("{}")
        }
        val dataSource = KtorLibraryRemoteDataSource(client)

        assertFailsWith<IllegalArgumentException> {
            dataSource.getLibrary(page = 0, limit = 20, status = null, favoritesOnly = false)
        }
        assertFailsWith<IllegalArgumentException> {
            dataSource.getLibrary(page = 1, limit = 51, status = null, favoritesOnly = false)
        }
        assertFailsWith<IllegalArgumentException> {
            dataSource.createReadingSession("state-1", CreateReadingSessionDto(), " ")
        }
        assertFailsWith<IllegalArgumentException> {
            dataSource.createReadingSession("state-1", CreateReadingSessionDto(), "x".repeat(256))
        }

        assertFailsWith<IllegalArgumentException> {
            dataSource.deleteState("  ")
        }

        assertEquals(0, requestCount)
        client.close()
    }

    private fun createClient(
        tokenProvider: AccessTokenProvider? = null,
        handler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(io.ktor.client.request.HttpRequestData) -> io.ktor.client.request.HttpResponseData,
    ): HttpClient = createBookieHttpClient(
        engine = MockEngine(handler),
        config = BookieHttpClientConfig(
            baseUrl = "https://api.example.test/v1",
            accessTokenProvider = tokenProvider,
        ),
    )

    private fun jsonHeaders() = Headers.build {
        append(HttpHeaders.ContentType, "application/json")
    }

    private companion object {
        const val LIBRARY_STATE_JSON = """{"id":"state-1","userId":"user-1","bookId":"book-1","status":"READING","rating":null,"comment":null,"isFavorite":false,"currentPage":110,"progressPercentage":45,"startedAt":null,"finishedAt":null,"isAbandoned":false,"privateNotes":null,"updatedAt":"2026-09-01T15:30:00.000Z","book":{"id":"book-1","googleBookId":"google-1","title":"Dune","authors":["Frank Herbert"],"genres":["Science Fiction"]}}"""
        const val LIBRARY_PAGE_JSON = """{"data":[$LIBRARY_STATE_JSON],"meta":{"page":2,"limit":10,"total":1,"totalPages":1}}"""
        const val SESSION_JSON = """{"id":"session-1","userBookStateId":"state-1","idempotencyKey":"attempt-1","currentPage":110,"progressPercentage":45,"notes":null,"createdAt":"2026-09-01T15:30:00.000Z"}"""
    }
}

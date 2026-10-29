package org.bookie.app.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.bookie.app.data.remote.AccessTokenProvider
import org.bookie.app.data.remote.BookieHttpClientConfig
import org.bookie.app.data.remote.KtorBookListsRemoteDataSource
import org.bookie.app.data.remote.createBookieHttpClient
import org.bookie.app.domain.model.CreateBookListInput
import org.bookie.app.domain.model.UpdateBookListInput
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BookListsRepositoryImplTest {
    private fun headers() = Headers.build { append(HttpHeaders.ContentType, "application/json") }
    private val listJson = """{"id":"list-1","name":"Favoritos","isPublic":false,"createdAt":"2026-10-29T00:00:00Z","updatedAt":"2026-10-29T00:00:00Z"}"""
    private val itemJson = """{"id":"item-1","listId":"list-1","bookId":"book-1","addedAt":"2026-10-29T00:00:00Z","book":{"id":"book-1","googleBookId":"g-1","title":"Dune","authors":["Frank Herbert"]}}"""

    @Test fun `reads paginated personal lists with nested books and JWT`() = runTest {
        var path = ""
        var bearer = ""
        val client = createBookieHttpClient(
            MockEngine { request ->
                path = request.url.encodedPath
                bearer = request.headers[HttpHeaders.Authorization].orEmpty()
                assertEquals("2", request.url.parameters["page"])
                assertEquals("10", request.url.parameters["limit"])
                respond("""{"data":[{"id":"list-1","name":"Favoritos","isPublic":false,"createdAt":"2026-10-29T00:00:00Z","updatedAt":"2026-10-29T00:00:00Z","items":[$itemJson]}],"meta":{"page":2,"limit":10,"total":1,"totalPages":1}}""", headers = headers())
            },
            BookieHttpClientConfig("https://api.example.test/v1", AccessTokenProvider { "token" }),
        )
        val result = BookListsRepositoryImpl(KtorBookListsRemoteDataSource(client)).getLists(2, 10)
        assertEquals("/v1/lists", path)
        assertEquals("Bearer token", bearer)
        val page = assertIs<Resource.Success<org.bookie.app.domain.model.BookListPage>>(result).data
        assertEquals("Dune", page.lists.single().items?.single()?.book?.title)
        assertEquals(2, page.page)
        client.close()
    }

    @Test fun `creates a list with only permitted fields`() = runTest {
        var payload = ""
        val client = createBookieHttpClient(
            MockEngine { request ->
                payload = (request.body as io.ktor.http.content.TextContent).text
                respond(listJson, HttpStatusCode.Created, headers())
            },
            BookieHttpClientConfig("https://api.example.test/v1"),
        )
        val result = BookListsRepositoryImpl(KtorBookListsRemoteDataSource(client)).createList(
            CreateBookListInput("Favoritos", isPublic = false),
        )
        assertIs<Resource.Success<org.bookie.app.domain.model.BookList>>(result)
        assertEquals("""{"name":"Favoritos"}""", payload)
        client.close()
    }

    @Test fun `updates lists and maps missing items as not loaded`() = runTest {
        val client = createBookieHttpClient(
            MockEngine { request ->
                assertEquals(io.ktor.http.HttpMethod.Patch, request.method)
                assertEquals("/v1/lists/list-1", request.url.encodedPath)
                respond(listJson, headers = headers())
            },
            BookieHttpClientConfig("https://api.example.test/v1"),
        )
        val result = BookListsRepositoryImpl(KtorBookListsRemoteDataSource(client))
            .updateList("list-1", UpdateBookListInput(isPublic = false))
        val list = assertIs<Resource.Success<org.bookie.app.domain.model.BookList>>(result).data
        assertEquals(null, list.items)
        client.close()
    }

    @Test fun `adds a book without passing user ID`() = runTest {
        var body = ""
        val client = createBookieHttpClient(
            MockEngine { request ->
                assertEquals("/v1/lists/list-1/books", request.url.encodedPath)
                body = (request.body as io.ktor.http.content.TextContent).text
                respond(itemJson, headers = headers())
            },
            BookieHttpClientConfig("https://api.example.test/v1"),
        )
        val result = BookListsRepositoryImpl(KtorBookListsRemoteDataSource(client)).addBook("list-1", "book-1")
        assertEquals("""{"bookId":"book-1"}""", body)
        assertEquals("Dune", assertIs<Resource.Success<org.bookie.app.domain.model.BookListItem>>(result).data.book?.title)
        client.close()
    }

    @Test fun `deletions require success confirmed by server`() = runTest {
        val client = createBookieHttpClient(
            MockEngine { request ->
                assertEquals(io.ktor.http.HttpMethod.Delete, request.method)
                respond("""{"success":true}""", headers = headers())
            },
            BookieHttpClientConfig("https://api.example.test/v1"),
        )
        val repo = BookListsRepositoryImpl(KtorBookListsRemoteDataSource(client))
        assertEquals(Resource.Success(Unit), repo.deleteList("list-1"))
        assertEquals(Resource.Success(Unit), repo.removeBook("list-1", "book-1"))
        client.close()
    }

    @Test fun `conflicts authorization and network errors are classified`() = runTest {
        for ((code, kind) in listOf(
            HttpStatusCode.Conflict to ResourceErrorKind.CONFLICT,
            HttpStatusCode.Unauthorized to ResourceErrorKind.UNAUTHORIZED,
            HttpStatusCode.Forbidden to ResourceErrorKind.FORBIDDEN,
            HttpStatusCode.UnprocessableEntity to ResourceErrorKind.VALIDATION,
        )) {
            val client = createBookieHttpClient(
                MockEngine { respond("", code) },
                BookieHttpClientConfig("https://api.example.test/v1"),
            )
            val result = BookListsRepositoryImpl(KtorBookListsRemoteDataSource(client))
                .getLists()
            assertEquals(kind, assertIs<Resource.Error>(result).kind)
            client.close()
        }
    }

    @Test fun `rejects invalid page before sending HTTP`() = runTest {
        var calls = 0
        val client = createBookieHttpClient(
            MockEngine {
                calls++
                respond("{}")
            },
            BookieHttpClientConfig("https://api.example.test/v1"),
        )
        val result = BookListsRepositoryImpl(KtorBookListsRemoteDataSource(client)).getLists(0, 20)
        assertIs<Resource.Error>(result)
        assertEquals(0, calls)
        client.close()
    }
}

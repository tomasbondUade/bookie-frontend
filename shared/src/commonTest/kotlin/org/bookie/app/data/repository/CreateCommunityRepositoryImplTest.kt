package org.bookie.app.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.bookie.app.data.remote.AccessTokenProvider
import org.bookie.app.data.remote.BookieHttpClientConfig
import org.bookie.app.data.remote.KtorCommunityRemoteDataSource
import org.bookie.app.data.remote.createBookieHttpClient
import org.bookie.app.domain.model.CreateCommunityInput
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CreateCommunityRepositoryImplTest {
    @Test
    fun postUsesExistingBearerAndOnlyWritableFields() = runTest {
        var path = ""
        var method: HttpMethod? = null
        var auth: String? = null
        var body = ""
        val client = createBookieHttpClient(
            MockEngine { request ->
                path = request.url.encodedPath
                method = request.method
                auth = request.headers[HttpHeaders.Authorization]
                body = (request.body as TextContent).text
                respond(
                    """{"id":"new-1","name":"Lectores","description":"Novelas",
                    "ownerId":"owner-from-server","tags":[],"imageUrl":null,
                    "createdAt":"2026-11-17T13:00:00Z"}""",
                    status = HttpStatusCode.Created,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
            BookieHttpClientConfig(
                baseUrl = "https://bookie.example.test",
                accessTokenProvider = AccessTokenProvider { "active-jwt" },
            ),
        )
        try {
            val repo = CommunityRepositoryImpl(KtorCommunityRemoteDataSource(client))
            val result = assertIs<Resource.Success<*>>(
                repo.createCommunity(CreateCommunityInput("  Lectores ", " Novelas  ")),
            )
            val created = result.data as org.bookie.app.domain.model.Community
            assertEquals(HttpMethod.Post, method)
            assertEquals("/communities", path)
            assertEquals("Bearer active-jwt", auth)
            assertEquals("""{"name":"Lectores","description":"Novelas"}""", body)
            assertEquals("new-1", created.id)
            assertEquals("owner-from-server", created.ownerId)
        } finally { client.close() }
    }

    @Test
    fun duplicateNameAndUnauthorizedAreDistinctErrors() = runTest {
        for ((status, expectedKind) in listOf(
            HttpStatusCode.Conflict to ResourceErrorKind.CONFLICT,
            HttpStatusCode.Unauthorized to ResourceErrorKind.UNAUTHORIZED,
            HttpStatusCode.BadRequest to ResourceErrorKind.VALIDATION,
        )) {
            val client = createBookieHttpClient(
                MockEngine { respond("error", status = status) },
                BookieHttpClientConfig("https://bookie.example.test"),
            )
            try {
                val repo = CommunityRepositoryImpl(KtorCommunityRemoteDataSource(client))
                val result = assertIs<Resource.Error>(
                    repo.createCommunity(CreateCommunityInput("Lectores", "Novelas")),
                )
                assertEquals(expectedKind, result.kind)
            } finally { client.close() }
        }
    }
}

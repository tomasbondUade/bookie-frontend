package org.bookie.app.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.bookie.app.data.remote.*
import org.bookie.app.domain.model.CreateCommunityPostInput
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind
import kotlin.test.*

class CommunityPostsRepositoryImplTest {
    private val postJson = """{"id":"post-1","title":"Lecturas","content":"Hola","communityId":"community-1","authorId":"user-1","createdAt":"2026-10-29T10:00:00Z","author":{"id":"user-1","username":"lectora","avatarUrl":null}}"""
    private fun headers() = Headers.build { append(HttpHeaders.ContentType, "application/json") }

    @Test fun `reads feed with author and server limit`() = runTest {
        var path = ""
        val client = createBookieHttpClient(MockEngine { request ->
            path = request.url.encodedPath
            assertEquals("20", request.url.parameters["limit"])
            respond("[$postJson]", headers = headers())
        }, BookieHttpClientConfig("https://api.example.test/v1"))
        val result = CommunityPostsRepositoryImpl(KtorCommunityPostsRemoteDataSource(client), AccessTokenProvider { null })
            .getPosts("community-1")
        assertEquals("/v1/communities/community-1/posts", path)
        val post = assertIs<Resource.Success<List<org.bookie.app.domain.model.CommunityPost>>>(result).data.single()
        assertEquals("lectora", post.author?.username)
        assertEquals("Hola", post.content)
        client.close()
    }

    @Test fun `publishing sends JWT and only title and content`() = runTest {
        var body = ""
        var bearer = ""
        val client = createBookieHttpClient(MockEngine { request ->
            bearer = request.headers[HttpHeaders.Authorization].orEmpty()
            body = (request.body as io.ktor.http.content.TextContent).text
            respond(postJson, HttpStatusCode.Created, headers())
        }, BookieHttpClientConfig("https://api.example.test/v1", AccessTokenProvider { "jwt" }))
        val result = CommunityPostsRepositoryImpl(
            KtorCommunityPostsRemoteDataSource(client), AccessTokenProvider { "jwt" },
        ).createPost("community-1", CreateCommunityPostInput(" Lecturas ", " Hola "))
        assertIs<Resource.Success<org.bookie.app.domain.model.CommunityPost>>(result)
        assertEquals("Bearer jwt", bearer)
        assertEquals("""{"title":"Lecturas","content":"Hola"}""", body)
        client.close()
    }

    @Test fun `missing session rejects publication before HTTP`() = runTest {
        var calls = 0
        val client = createBookieHttpClient(MockEngine { calls++; respond("{}") },
            BookieHttpClientConfig("https://api.example.test/v1"))
        val result = CommunityPostsRepositoryImpl(
            KtorCommunityPostsRemoteDataSource(client), AccessTokenProvider { null },
        ).createPost("community-1", CreateCommunityPostInput("Título", "Texto"))
        assertEquals(ResourceErrorKind.UNAUTHORIZED, assertIs<Resource.Error>(result).kind)
        assertEquals(0, calls)
        client.close()
    }

    @Test fun `server rejection never returns success`() = runTest {
        val client = createBookieHttpClient(MockEngine { respond("", HttpStatusCode.Unauthorized) },
            BookieHttpClientConfig("https://api.example.test/v1"))
        val result = CommunityPostsRepositoryImpl(
            KtorCommunityPostsRemoteDataSource(client), AccessTokenProvider { "expired" },
        ).createPost("community-1", CreateCommunityPostInput("Título", "Texto"))
        assertEquals(ResourceErrorKind.UNAUTHORIZED, assertIs<Resource.Error>(result).kind)
        client.close()
    }
}

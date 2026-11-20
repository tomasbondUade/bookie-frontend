package org.bookie.app.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.bookie.app.data.remote.*
import org.bookie.app.domain.model.CreateCommunityCommentInput
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

    @Test fun `comment sends JWT and only the accepted fields then maps server response`() = runTest {
        val requests = mutableListOf<io.ktor.client.request.HttpRequestData>()
        val client = createBookieHttpClient(MockEngine { request ->
            requests += request
            respond(
                """{"id":"comment-1","content":"Hola","postId":"post-1","authorId":"user-1","parentId":null,"createdAt":"2026-11-20T10:00:00Z"}""",
                HttpStatusCode.Created,
                headers(),
            )
        }, BookieHttpClientConfig("https://api.example.test/v1", AccessTokenProvider { "jwt" }))
        val result = CommunityPostsRepositoryImpl(
            KtorCommunityPostsRemoteDataSource(client), AccessTokenProvider { "jwt" },
        ).createComment("post-1", CreateCommunityCommentInput(" Hola "))

        val comment = assertIs<Resource.Success<org.bookie.app.domain.model.CommunityComment>>(result).data
        assertEquals("comment-1", comment.id)
        assertEquals("post-1", comment.postId)
        assertEquals(null, comment.parentId)
        assertEquals("/v1/posts/post-1/comments", requests.single().url.encodedPath)
        assertEquals("Bearer jwt", requests.single().headers[HttpHeaders.Authorization])
        assertEquals(
            """{"content":"Hola"}""",
            (requests.single().body as io.ktor.http.content.TextContent).text,
        )
        client.close()
    }

    @Test fun `reply includes parent id and reports missing comment as not found`() = runTest {
        var body = ""
        val client = createBookieHttpClient(MockEngine { request ->
            body = (request.body as io.ktor.http.content.TextContent).text
            respond("", HttpStatusCode.NotFound)
        }, BookieHttpClientConfig("https://api.example.test/v1", AccessTokenProvider { "jwt" }))
        val result = CommunityPostsRepositoryImpl(
            KtorCommunityPostsRemoteDataSource(client), AccessTokenProvider { "jwt" },
        ).createComment("post-1", CreateCommunityCommentInput("Respuesta", "parent-1"))

        assertEquals("""{"content":"Respuesta","parentId":"parent-1"}""", body)
        assertEquals(ResourceErrorKind.NOT_FOUND, assertIs<Resource.Error>(result).kind)
        client.close()
    }

    @Test fun `guest and invalid comments are rejected before HTTP`() = runTest {
        var calls = 0
        val client = createBookieHttpClient(
            MockEngine { calls++; respond("{}") },
            BookieHttpClientConfig("https://api.example.test/v1"),
        )
        val repository = CommunityPostsRepositoryImpl(
            KtorCommunityPostsRemoteDataSource(client), AccessTokenProvider { null },
        )

        val guest = repository.createComment("post-1", CreateCommunityCommentInput("Hola"))
        assertEquals(ResourceErrorKind.UNAUTHORIZED, assertIs<Resource.Error>(guest).kind)

        val signedIn = CommunityPostsRepositoryImpl(
            KtorCommunityPostsRemoteDataSource(client), AccessTokenProvider { "jwt" },
        )
        val invalid = signedIn.createComment("post-1", CreateCommunityCommentInput("  "))
        assertEquals(ResourceErrorKind.VALIDATION, assertIs<Resource.Error>(invalid).kind)
        assertEquals(0, calls)
        client.close()
    }
}

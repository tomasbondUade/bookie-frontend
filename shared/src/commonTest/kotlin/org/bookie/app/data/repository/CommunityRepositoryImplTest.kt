package org.bookie.app.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.bookie.app.data.remote.BookieHttpClientConfig
import org.bookie.app.data.remote.KtorCommunityRemoteDataSource
import org.bookie.app.data.remote.createBookieHttpClient
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CommunityRepositoryImplTest {
    @Test
    fun publicCatalogMapsPageAndQuery() = runTest {
        var path = ""
        var page = ""
        var limit = ""
        val client = createBookieHttpClient(
            MockEngine { request ->
                path = request.url.encodedPath
                page = request.url.parameters["page"].orEmpty()
                limit = request.url.parameters["limit"].orEmpty()
                respond(
                    """{
                        "data":[{
                            "id":"community-1",
                            "name":"Lectores de fantasía",
                            "description":"Para hablar de libros",
                            "tags":["Fantasía"],
                            "imageUrl":null,
                            "ownerId":"owner-1",
                            "createdAt":"2026-11-17T10:00:00.000Z"
                        }],
                        "meta":{"page":2,"limit":5,"total":6,"totalPages":2}
                    }""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
            BookieHttpClientConfig("https://bookie.example.test"),
        )
        try {
            val data = assertIs<Resource.Success<*>>(
                CommunityRepositoryImpl(KtorCommunityRemoteDataSource(client))
                    .listCommunities(2, 5),
            ).data as org.bookie.app.domain.model.CommunityPage
            assertEquals("/communities", path)
            assertEquals("2", page)
            assertEquals("5", limit)
            assertEquals("community-1", data.communities.single().id)
            assertEquals(listOf("Fantasía"), data.communities.single().tags)
            assertEquals(6, data.total)
            assertEquals(2, data.totalPages)
        } finally { client.close() }
    }

    @Test
    fun recommendedListAndPublicPostsUseBackendShape() = runTest {
        val requestedPaths = mutableListOf<String>()
        val client = createBookieHttpClient(
            MockEngine { request ->
                requestedPaths += request.url.encodedPath
                val json = if (request.url.encodedPath.endsWith("recommended")) {
                    """[{
                        "id":"community-1","name":"Fantasía","description":"Libros",
                        "ownerId":"owner-1","createdAt":"2026-11-17T10:00:00Z",
                        "tags":["fantasia"],"score":1,"matchedTags":["fantasia"],
                        "reason":"Coincide con tus intereses: fantasia"
                    }]"""
                } else {
                    """[{
                        "id":"post-1","title":"Hola","content":"Una lectura",
                        "communityId":"community-1",
                        "createdAt":"2026-11-17T12:00:00Z",
                        "author":{"id":"author-1","username":"Lectora","avatarUrl":null}
                    }]"""
                }
                respond(json, headers = headersOf(HttpHeaders.ContentType, "application/json"))
            },
            BookieHttpClientConfig("https://bookie.example.test"),
        )
        try {
            val repo = CommunityRepositoryImpl(KtorCommunityRemoteDataSource(client))
            val rec = assertIs<Resource.Success<*>>(repo.recommended()).data
                as List<*>
            assertEquals(1, rec.size)
            val community = rec.single() as org.bookie.app.domain.model.Community
            assertEquals(1, community.score)
            assertEquals(listOf("fantasia"), community.matchedTags)
            val posts = assertIs<Resource.Success<*>>(repo.posts("community-1")).data
                as List<*>
            assertEquals("Lectora", (posts.single() as org.bookie.app.domain.model.CommunityPost).authorName)
            assertEquals(
                listOf("/communities/recommended", "/communities/community-1/posts"),
                requestedPaths,
            )
        } finally { client.close() }
    }

    @Test
    fun emptyCatalogAndUnauthorizedRecommendationsAreDistinct() = runTest {
        val client = createBookieHttpClient(
            MockEngine { request ->
                if (request.url.encodedPath.endsWith("recommended")) {
                    respond("Unauthorized", status = HttpStatusCode.Unauthorized)
                } else {
                    respond(
                        """{"data":[],"meta":{"page":1,"limit":20,"total":0,"totalPages":0}}""",
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            },
            BookieHttpClientConfig("https://bookie.example.test"),
        )
        try {
            val repo = CommunityRepositoryImpl(KtorCommunityRemoteDataSource(client))
            val empty = assertIs<Resource.Success<*>>(repo.listCommunities()).data
                as org.bookie.app.domain.model.CommunityPage
            assertTrue(empty.communities.isEmpty())
            val blocked = assertIs<Resource.Error>(repo.recommended())
            assertEquals(ResourceErrorKind.UNAUTHORIZED, blocked.kind)
        } finally { client.close() }
    }
}

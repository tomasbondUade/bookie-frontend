package org.bookie.app.data.auth

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import org.bookie.app.data.remote.BookieHttpClientConfig
import org.bookie.app.data.remote.createBookieHttpClient
import org.bookie.app.domain.model.AuthState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SupabaseAuthHttpTest {
    private fun jsonHeaders() = Headers.build { append(HttpHeaders.ContentType, "application/json") }
    private val config = SupabaseAuthConfig("https://supabase.example.test", "sb_publishable_test")

    @Test fun `Supabase sign in and refresh send expected grant type and public key`() = runTest {
        val requests = mutableListOf<Pair<String, String>>()
        val client = createSupabaseHttpClient(MockEngine { request ->
            assertEquals("supabase.example.test", request.url.host)
            assertEquals("sb_publishable_test", request.headers["apikey"])
            assertEquals(HttpMethod.Post, request.method)
            requests += request.url.toString() to (request.body as TextContent).text
            respond(
                """{"access_token":"jwt","refresh_token":"refresh","expires_in":3600,"user":{"id":"user-1","email":"reader@example.test","user_metadata":{"username":"reader"}}}""",
                headers = jsonHeaders(),
            )
        })
        val remote = KtorSupabaseAuthRemoteDataSource(client, config)
        assertEquals("jwt", remote.signIn("reader@example.test", "pass123").accessToken)
        assertEquals("refresh", remote.refresh("refresh-old").refreshToken)
        assertEquals("https://supabase.example.test/auth/v1/token?grant_type=password", requests[0].first)
        assertEquals("""{"email":"reader@example.test","password":"pass123"}""", requests[0].second)
        assertEquals("https://supabase.example.test/auth/v1/token?grant_type=refresh_token", requests[1].first)
        assertEquals("""{"refresh_token":"refresh-old"}""", requests[1].second)
        client.close()
    }

    @Test fun `Supabase sign up stores username only in user metadata`() = runTest {
        var body = ""
        val client = createSupabaseHttpClient(MockEngine { request ->
            assertEquals("/auth/v1/signup", request.url.encodedPath)
            body = (request.body as TextContent).text
            respond("""{"user":{"id":"user-1","email":"reader@example.test"}}""", headers = jsonHeaders())
        })
        val response = KtorSupabaseAuthRemoteDataSource(client, config)
            .signUp("reader@example.test", "pass123", "reader")
        assertEquals("""{"email":"reader@example.test","password":"pass123","data":{"username":"reader"}}""", body)
        assertEquals(null, response.accessToken)
        client.close()
    }

    @Test fun `Supabase logout sends JWT only to Supabase origin`() = runTest {
        var authorization = ""
        val client = createSupabaseHttpClient(MockEngine { request ->
            assertEquals("/auth/v1/logout", request.url.encodedPath)
            authorization = request.headers[HttpHeaders.Authorization].orEmpty()
            respond("", status = HttpStatusCode.NoContent)
        })
        KtorSupabaseAuthRemoteDataSource(client, config).signOut("jwt")
        assertEquals("Bearer jwt", authorization)
        client.close()
    }

    @Test fun `Bookie sync sends bearer and username without userId or password`() = runTest {
        var path = ""
        var authorization = ""
        var body = ""
        val client = createBookieUserSyncHttpClient(
            MockEngine { request ->
                path = request.url.encodedPath
                authorization = request.headers[HttpHeaders.Authorization].orEmpty()
                body = (request.body as TextContent).text
                respond("""{"id":"user-1"}""", headers = jsonHeaders())
            },
            BookieHttpClientConfig("https://api.example.test/v1"),
        )
        KtorBookieUserSyncRemoteDataSource(client).sync("reader", "jwt")
        assertEquals("/v1/users/sync", path)
        assertEquals("Bearer jwt", authorization)
        assertEquals("""{"username":"reader"}""", body)
        assertTrue("userId" !in body && "password" !in body)
        client.close()
    }

    @Test fun `rejects Supabase secret key in app configuration`() {
        assertFailsWith<IllegalArgumentException> {
            SupabaseAuthConfig("https://supabase.example.test", "sb_secret_example")
        }
        assertFailsWith<IllegalArgumentException> {
            SupabaseAuthConfig("http://supabase.example.test", "sb_publishable_test")
        }
    }
}

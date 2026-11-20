package org.bookie.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class ApiErrorResponseDetailsTest {
    @Test fun `not found shows backend reason and route instead of generic status`() = runTest {
        val client = HttpClient(MockEngine {
            respond(
                """{"statusCode":404,"message":"Usuario con id no encontrado."}""",
                status = HttpStatusCode.NotFound,
            )
        }) { expectSuccess = true }
        try {
            try {
                client.get("https://api.example.test/users/preferences")
                fail("Expected HTTP 404")
            } catch (error: ResponseException) {
                assertEquals(
                    "HTTP 404 en /users/preferences: Usuario con id no encontrado.",
                    error.toBookieErrorMessage(),
                )
            }
        } finally {
            client.close()
        }
    }

    @Test fun `conflict shows reason but omits query parameter`() = runTest {
        val client = HttpClient(MockEngine {
            respond(
                """{"statusCode":409,"message":"El email ya pertenece a otro usuario."}""",
                status = HttpStatusCode.Conflict,
            )
        }) { expectSuccess = true }
        try {
            try {
                client.get("https://api.example.test/users/sync?token=secret")
                fail("Expected HTTP 409")
            } catch (error: ResponseException) {
                assertEquals(
                    "HTTP 409 en /users/sync: El email ya pertenece a otro usuario.",
                    error.toBookieErrorMessage(),
                )
            }
        } finally {
            client.close()
        }
    }
}

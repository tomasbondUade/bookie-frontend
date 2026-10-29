package org.bookie.app.data.auth

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.bookie.app.data.remote.BookieHttpClientConfig

interface BookieUserSyncRemoteDataSource {
    suspend fun sync(username: String, accessToken: String)
}

/**
 * Separate one-purpose client: the bearer token is explicitly attached to Bookie's
 * configured origin, not to Supabase or a third-party redirect.
 */
fun createBookieUserSyncHttpClient(
    engine: HttpClientEngine,
    config: BookieHttpClientConfig,
): HttpClient = HttpClient(engine) {
    expectSuccess = true
    followRedirects = false
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    defaultRequest { url(config.requestBaseUrl) }
}

class KtorBookieUserSyncRemoteDataSource(
    private val client: HttpClient,
) : BookieUserSyncRemoteDataSource {
    override suspend fun sync(username: String, accessToken: String) {
        require(accessToken.isNotBlank()) { "Falta el token de sesión." }
        client.post("users/sync") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            contentType(ContentType.Application.Json)
            setBody(SyncUserDto(username))
        }
    }
}

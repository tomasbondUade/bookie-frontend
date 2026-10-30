package org.bookie.app.data.auth

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

interface SupabaseAuthRemoteDataSource {
    suspend fun signIn(email: String, password: String): SupabaseSessionDto
    suspend fun signUp(email: String, password: String, username: String): SupabaseSessionDto
    suspend fun refresh(refreshToken: String): SupabaseSessionDto
    suspend fun signOut(accessToken: String)
}

/** Dedicated Supabase client: Bookie's JWT plugin must not attach tokens to another origin. */
fun createSupabaseHttpClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
    expectSuccess = true
    followRedirects = false
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

fun <T : HttpClientEngineConfig> createSupabaseHttpClient(
    engine: HttpClientEngineFactory<T>,
): HttpClient = HttpClient(engine) {
    expectSuccess = true
    followRedirects = false
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

class KtorSupabaseAuthRemoteDataSource(
    private val client: HttpClient,
    private val config: SupabaseAuthConfig,
) : SupabaseAuthRemoteDataSource {
    override suspend fun signIn(email: String, password: String): SupabaseSessionDto =
        client.post("${config.authBaseUrl}/auth/v1/token") {
            header("apikey", config.publishableKey)
            parameter("grant_type", "password")
            contentType(ContentType.Application.Json)
            setBody(PasswordGrantDto(email, password))
        }.body()

    override suspend fun signUp(
        email: String, password: String, username: String,
    ): SupabaseSessionDto =
        client.post("${config.authBaseUrl}/auth/v1/signup") {
            header("apikey", config.publishableKey)
            contentType(ContentType.Application.Json)
            setBody(SignUpDto(email, password, UserMetadataDto(username)))
        }.body()

    override suspend fun refresh(refreshToken: String): SupabaseSessionDto =
        client.post("${config.authBaseUrl}/auth/v1/token") {
            header("apikey", config.publishableKey)
            parameter("grant_type", "refresh_token")
            contentType(ContentType.Application.Json)
            setBody(RefreshGrantDto(refreshToken))
        }.body()

    override suspend fun signOut(accessToken: String) {
        client.post("${config.authBaseUrl}/auth/v1/logout") {
            header("apikey", config.publishableKey)
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
    }
}

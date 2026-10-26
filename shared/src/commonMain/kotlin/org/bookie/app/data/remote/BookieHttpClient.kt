package org.bookie.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.api.SendingRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.url
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

data class HttpTimeouts(
    val requestMillis: Long = DEFAULT_REQUEST_TIMEOUT_MILLIS,
    val connectMillis: Long = DEFAULT_CONNECT_TIMEOUT_MILLIS,
    val socketMillis: Long = DEFAULT_SOCKET_TIMEOUT_MILLIS,
) {
    init {
        require(requestMillis > 0) { "Request timeout must be positive." }
        require(connectMillis > 0) { "Connect timeout must be positive." }
        require(socketMillis > 0) { "Socket timeout must be positive." }
    }
}

data class BookieHttpClientConfig(
    val baseUrl: String,
    val accessTokenProvider: AccessTokenProvider? = null,
    val timeouts: HttpTimeouts = HttpTimeouts(),
    val json: Json = Json { ignoreUnknownKeys = true },
) {
    internal val baseUri: Url = Url(baseUrl).also { url ->
        require(url.protocol == URLProtocol.HTTP || url.protocol == URLProtocol.HTTPS) {
            "Base URL must use HTTP or HTTPS."
        }
        require(url.host.isNotBlank()) { "Base URL must include a host." }
        require(url.user.isNullOrEmpty() && url.password.isNullOrEmpty()) {
            "Base URL cannot include credentials."
        }
        require(url.parameters.isEmpty() && url.fragment.isEmpty() && !url.trailingQuery) {
            "Base URL cannot include query parameters or a fragment."
        }
    }
    internal val requestBaseUrl: String = baseUri.toString().let { url ->
        if (url.endsWith('/')) url else "$url/"
    }

    init {
        require(baseUrl.isNotBlank()) { "Base URL cannot be blank." }
    }
}

fun createBookieHttpClient(
    engine: HttpClientEngine,
    config: BookieHttpClientConfig,
): HttpClient = HttpClient(engine) {
    configureBookieHttpClient(config)
}

fun <T : HttpClientEngineConfig> createBookieHttpClient(
    engine: HttpClientEngineFactory<T>,
    config: BookieHttpClientConfig,
): HttpClient = HttpClient(engine) {
    configureBookieHttpClient(config)
}

private fun io.ktor.client.HttpClientConfig<*>.configureBookieHttpClient(
    config: BookieHttpClientConfig,
) {
    expectSuccess = true

    install(ContentNegotiation) {
        json(config.json)
    }

    install(HttpTimeout) {
        requestTimeoutMillis = config.timeouts.requestMillis
        connectTimeoutMillis = config.timeouts.connectMillis
        socketTimeoutMillis = config.timeouts.socketMillis
    }

    defaultRequest {
        url(config.requestBaseUrl)
    }

    install(AccessTokenPlugin) {
        backendUrl = config.baseUri
        tokenProvider = config.accessTokenProvider
    }
}

expect fun createPlatformHttpClient(config: BookieHttpClientConfig): HttpClient

private class AccessTokenPluginConfig {
    lateinit var backendUrl: Url
    var tokenProvider: AccessTokenProvider? = null
}

private val AccessTokenPlugin = createClientPlugin(
    "BookieAccessToken",
    ::AccessTokenPluginConfig,
) {
    val backendUrl = pluginConfig.backendUrl
    val tokenProvider = pluginConfig.tokenProvider

    on(SendingRequest) { request, _ ->
        request.headers.remove(HttpHeaders.Authorization)
        if (request.url.build().hasSameOriginAs(backendUrl)) {
            tokenProvider?.getAccessToken()
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?.let { token ->
                    request.headers.append(HttpHeaders.Authorization, "Bearer $token")
                }
        }
    }
}

private fun Url.hasSameOriginAs(other: Url): Boolean =
    protocol == other.protocol && host.equals(other.host, ignoreCase = true) && port == other.port

private const val DEFAULT_REQUEST_TIMEOUT_MILLIS = 15_000L
private const val DEFAULT_CONNECT_TIMEOUT_MILLIS = 10_000L
private const val DEFAULT_SOCKET_TIMEOUT_MILLIS = 15_000L

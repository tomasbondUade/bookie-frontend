package org.bookie.app.data.remote

import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.serialization.JsonConvertException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlin.coroutines.cancellation.CancellationException

sealed interface ApiError {
    data object Network : ApiError
    data object Timeout : ApiError
    data class Http(val statusCode: Int) : ApiError
    data object Parsing : ApiError
    data object Unknown : ApiError
}

fun Throwable.toApiError(): ApiError {
    if (this is CancellationException) throw this

    return when (this) {
        is HttpRequestTimeoutException,
        is ConnectTimeoutException,
        is SocketTimeoutException,
        -> ApiError.Timeout

        is ResponseException -> ApiError.Http(response.status.value)
        is SerializationException,
        is NoTransformationFoundException,
        is JsonConvertException,
        -> ApiError.Parsing

        is IOException -> ApiError.Network
        else -> ApiError.Unknown
    }
}

fun ApiError.toMessage(): String = when (this) {
    ApiError.Network -> "No se pudo conectar con el servidor."
    ApiError.Timeout -> "La solicitud tardó demasiado."
    is ApiError.Http -> "El servidor respondió con HTTP $statusCode."
    ApiError.Parsing -> "No se pudo interpretar la respuesta del servidor."
    ApiError.Unknown -> "No se pudo completar la solicitud."
}

/**
 * For debugging Bookie backend contract failures, keep NestJS's 404/409 reason
 * and requested route instead of replacing every response with "HTTP 404".
 * Query strings, request bodies, bearer tokens and server 5xx details are
 * intentionally not displayed.
 */
suspend fun Throwable.toBookieErrorMessage(): String {
    val mapped = toApiError()
    if (this !is ResponseException || mapped !is ApiError.Http ||
        mapped.statusCode !in setOf(404, 409)) return mapped.toMessage()

    val route = response.call.request.url.encodedPath
    val reason = try {
        val value = Json.parseToJsonElement(response.bodyAsText())
            .jsonObject["message"]
        (value as? JsonPrimitive)?.contentOrNull
            ?.take(200)
            ?.takeIf(String::isNotBlank)
    } catch (_: Exception) {
        null
    }
    return buildString {
        append("HTTP ")
        append(mapped.statusCode)
        append(" en ")
        append(route)
        reason?.let {
            append(": ")
            append(it)
        }
    }
}

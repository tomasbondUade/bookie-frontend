package org.bookie.app.data.remote

import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
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

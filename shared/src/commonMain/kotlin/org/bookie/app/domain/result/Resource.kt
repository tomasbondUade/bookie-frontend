package org.bookie.app.domain.result

sealed interface Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>

    data class Error(
        val message: String,
        val kind: ResourceErrorKind = ResourceErrorKind.GENERAL,
    ) : Resource<Nothing>
}

enum class ResourceErrorKind {
    GENERAL,
    UNAUTHORIZED,
    VALIDATION,
    FORBIDDEN,
    NOT_FOUND,
    CONFLICT,
    NETWORK,
}

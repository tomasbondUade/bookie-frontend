package org.bookie.app.domain.model

/** Freshness is explicit: cached data is never presented as server-confirmed. */
sealed interface LibraryLoad {
    data class Cached(val page: LibraryPage) : LibraryLoad
    data class Fresh(val page: LibraryPage) : LibraryLoad
    data class Failed(val message: String, val cachedPage: LibraryPage? = null) : LibraryLoad
}

package org.bookie.app.domain.model

enum class LibraryStatus {
    TO_READ,
    READING,
    READ,
}

data class LibraryBookState(
    val id: String,
    val bookId: String,
    val book: Book,
    val status: LibraryStatus,
    val rating: Int?,
    val comment: String?,
    val isFavorite: Boolean,
    val currentPage: Int?,
    val progressPercentage: Int?,
    val startedAt: String?,
    val finishedAt: String?,
    val isAbandoned: Boolean,
    val privateNotes: String?,
    val updatedAt: String,
)

data class LibraryPage(
    val books: List<LibraryBookState>,
    val page: Int,
    val limit: Int,
    val total: Int,
    val totalPages: Int,
)

data class LibraryStateInput(
    val bookId: String,
    val status: LibraryStatus? = null,
    val rating: Int? = null,
    val comment: String? = null,
    val isFavorite: Boolean? = null,
    val currentPage: Int? = null,
    val progressPercentage: Int? = null,
    val startedAt: String? = null,
    val finishedAt: String? = null,
    val isAbandoned: Boolean? = null,
    val privateNotes: String? = null,
)

data class ReadingSession(
    val id: String,
    val libraryStateId: String,
    val idempotencyKey: String?,
    val currentPage: Int?,
    val progressPercentage: Int?,
    val notes: String?,
    val createdAt: String,
)

data class ReadingSessionInput(
    val currentPage: Int? = null,
    val progressPercentage: Int? = null,
    val notes: String? = null,
)

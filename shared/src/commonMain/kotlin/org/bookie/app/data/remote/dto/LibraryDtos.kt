package org.bookie.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
enum class LibraryStatusDto {
    TO_READ,
    READING,
    READ,
}

@Serializable
data class LibraryPageDto(
    val data: List<LibraryBookStateDto>,
    val meta: LibraryPageMetaDto,
)

@Serializable
data class LibraryPageMetaDto(
    val page: Int,
    val limit: Int,
    val total: Int,
    val totalPages: Int,
)

@Serializable
data class LibraryBookStateDto(
    val id: String,
    val userId: String? = null,
    val bookId: String,
    val status: LibraryStatusDto,
    val rating: Int? = null,
    val comment: String? = null,
    val isFavorite: Boolean = false,
    val currentPage: Int? = null,
    val progressPercentage: Int? = null,
    val startedAt: String? = null,
    val finishedAt: String? = null,
    val isAbandoned: Boolean = false,
    val privateNotes: String? = null,
    val updatedAt: String,
    val book: BookDto,
)

@Serializable
data class SaveLibraryStateDto(
    val bookId: String,
    val status: LibraryStatusDto? = null,
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

@Serializable
data class ReadingSessionDto(
    val id: String,
    val userBookStateId: String,
    val idempotencyKey: String? = null,
    val currentPage: Int? = null,
    val progressPercentage: Int? = null,
    val notes: String? = null,
    val createdAt: String,
)

@Serializable
data class CreateReadingSessionDto(
    val currentPage: Int? = null,
    val progressPercentage: Int? = null,
    val notes: String? = null,
)

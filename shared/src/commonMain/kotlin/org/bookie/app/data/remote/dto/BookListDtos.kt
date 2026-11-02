package org.bookie.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class BookListPageDto(
    val data: List<BookListDto>,
    val meta: BookListPageMetaDto,
)

@Serializable
data class BookListPageMetaDto(
    val page: Int,
    val limit: Int,
    val total: Int,
    val totalPages: Int,
)

@Serializable
data class BookListDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val isPublic: Boolean = false,
    val createdAt: String,
    val updatedAt: String,
    // POST/PATCH return only the list metadata; GET includes the items.
    val items: List<BookListItemDto>? = null,
)

@Serializable
data class BookListItemDto(
    val id: String,
    val listId: String,
    val bookId: String,
    val addedAt: String,
    val book: BookDto? = null,
)

@Serializable
data class CreateBookListDto(
    val name: String,
    val description: String? = null,
    val isPublic: Boolean = false,
)

@Serializable
data class UpdateBookListDto(
    val name: String? = null,
    val description: String? = null,
    val isPublic: Boolean? = null,
)

@Serializable
data class AddBookToListDto(val bookId: String)

@Serializable
data class BookListMutationResultDto(val success: Boolean)

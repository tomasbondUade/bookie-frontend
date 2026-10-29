package org.bookie.app.domain.model

/**
 * A personal list. Items are null when the API returns list metadata only
 * (create/update); an empty list means the server explicitly returned no items.
 */
data class BookList(
    val id: String,
    val name: String,
    val description: String?,
    val isPublic: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val items: List<BookListItem>?,
)

data class BookListItem(
    val id: String,
    val listId: String,
    val bookId: String,
    val addedAt: String,
    val book: Book?,
)

data class BookListPage(
    val lists: List<BookList>,
    val page: Int,
    val limit: Int,
    val total: Int,
    val totalPages: Int,
)

data class CreateBookListInput(
    val name: String,
    val description: String? = null,
    val isPublic: Boolean = false,
)

data class UpdateBookListInput(
    val name: String? = null,
    val description: String? = null,
    val isPublic: Boolean? = null,
)

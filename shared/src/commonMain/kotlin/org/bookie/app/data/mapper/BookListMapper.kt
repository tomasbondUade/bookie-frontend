package org.bookie.app.data.mapper

import org.bookie.app.data.remote.dto.AddBookToListDto
import org.bookie.app.data.remote.dto.BookListDto
import org.bookie.app.data.remote.dto.BookListItemDto
import org.bookie.app.data.remote.dto.BookListPageDto
import org.bookie.app.data.remote.dto.CreateBookListDto
import org.bookie.app.data.remote.dto.UpdateBookListDto
import org.bookie.app.domain.model.BookList
import org.bookie.app.domain.model.BookListItem
import org.bookie.app.domain.model.BookListPage
import org.bookie.app.domain.model.CreateBookListInput
import org.bookie.app.domain.model.UpdateBookListInput

fun BookListItemDto.toDomain() = BookListItem(
    id = id,
    listId = listId,
    bookId = bookId,
    addedAt = addedAt,
    book = book?.toDomain(),
)

fun BookListDto.toDomain() = BookList(
    id = id,
    name = name,
    description = description,
    isPublic = isPublic,
    createdAt = createdAt,
    updatedAt = updatedAt,
    items = items?.map(BookListItemDto::toDomain),
)

fun BookListPageDto.toDomain() = BookListPage(
    lists = data.map(BookListDto::toDomain),
    page = meta.page,
    limit = meta.limit,
    total = meta.total,
    totalPages = meta.totalPages,
)

fun CreateBookListInput.toDto() = CreateBookListDto(
    name = name,
    description = description,
    isPublic = isPublic,
)

fun UpdateBookListInput.toDto() = UpdateBookListDto(
    name = name,
    description = description,
    isPublic = isPublic,
)

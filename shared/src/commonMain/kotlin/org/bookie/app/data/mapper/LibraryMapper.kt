package org.bookie.app.data.mapper

import org.bookie.app.data.remote.dto.CreateReadingSessionDto
import org.bookie.app.data.remote.dto.LibraryBookStateDto
import org.bookie.app.data.remote.dto.LibraryPageDto
import org.bookie.app.data.remote.dto.LibraryStatusDto
import org.bookie.app.data.remote.dto.ReadingSessionDto
import org.bookie.app.data.remote.dto.SaveLibraryStateDto
import org.bookie.app.domain.model.LibraryBookState
import org.bookie.app.domain.model.LibraryPage
import org.bookie.app.domain.model.LibraryStateInput
import org.bookie.app.domain.model.LibraryStatus
import org.bookie.app.domain.model.ReadingSession
import org.bookie.app.domain.model.ReadingSessionInput

fun LibraryBookStateDto.toDomain() = LibraryBookState(
    id = id,
    bookId = bookId,
    book = book.toDomain(),
    status = LibraryStatus.valueOf(status.name),
    rating = rating,
    comment = comment,
    isFavorite = isFavorite,
    currentPage = currentPage,
    progressPercentage = progressPercentage,
    startedAt = startedAt,
    finishedAt = finishedAt,
    isAbandoned = isAbandoned,
    privateNotes = privateNotes,
    updatedAt = updatedAt,
)

fun LibraryPageDto.toDomain() = LibraryPage(
    books = data.map(LibraryBookStateDto::toDomain),
    page = meta.page,
    limit = meta.limit,
    total = meta.total,
    totalPages = meta.totalPages,
)

fun LibraryStateInput.toDto() = SaveLibraryStateDto(
    bookId = bookId,
    status = status?.toDto(),
    rating = rating,
    comment = comment,
    isFavorite = isFavorite,
    currentPage = currentPage,
    progressPercentage = progressPercentage,
    startedAt = startedAt,
    finishedAt = finishedAt,
    isAbandoned = isAbandoned,
    privateNotes = privateNotes,
)

fun ReadingSessionDto.toDomain() = ReadingSession(
    id = id,
    libraryStateId = userBookStateId,
    idempotencyKey = idempotencyKey,
    currentPage = currentPage,
    progressPercentage = progressPercentage,
    notes = notes,
    createdAt = createdAt,
)

fun ReadingSessionInput.toDto() = CreateReadingSessionDto(
    currentPage = currentPage,
    progressPercentage = progressPercentage,
    notes = notes,
)

private fun LibraryStatus.toDto() = LibraryStatusDto.valueOf(name)

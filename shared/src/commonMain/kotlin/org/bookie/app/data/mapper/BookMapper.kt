package org.bookie.app.data.mapper

import org.bookie.app.data.remote.dto.BookDto
import org.bookie.app.domain.model.Book

fun BookDto.toDomain(): Book = Book(
    id = id,
    googleBookId = googleBookId,
    title = title,
    authors = authors,
    description = description,
    genres = genres,
    coverUrl = coverUrl,
    previewLink = previewLink,
    infoLink = infoLink,
    buyLink = buyLink,
)

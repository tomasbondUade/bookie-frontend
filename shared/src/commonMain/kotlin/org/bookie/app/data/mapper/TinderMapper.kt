package org.bookie.app.data.mapper

import org.bookie.app.data.remote.dto.TinderBookDto
import org.bookie.app.data.remote.dto.TinderFeedDto
import org.bookie.app.domain.model.*

fun TinderBookDto.toTinderDomain(): TinderRecommendedBook = TinderRecommendedBook(
    book = Book(
        id = id, googleBookId = googleBookId, title = title,
        authors = authors, description = description, genres = genres,
        coverUrl = coverUrl, previewLink = previewLink,
        infoLink = infoLink, buyLink = buyLink
    ),
    reasons = reasons
)

fun TinderFeedDto.toTinderDomain(): TinderFeed = TinderFeed(
    source = when (source) {
        "ai" -> RecommendationSource.AI
        "fallback" -> RecommendationSource.FALLBACK
        else -> error("Unknown recommendation source: $source")
    },
    count = count,
    books = books.map { it.toTinderDomain() }
)

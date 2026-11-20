package org.bookie.app.data.mapper

import org.bookie.app.data.remote.dto.BookTinderFeedDto
import org.bookie.app.data.remote.dto.CreateSwipeDto
import org.bookie.app.data.remote.dto.SwipeTypeDto
import org.bookie.app.domain.model.BookRecommendation
import org.bookie.app.domain.model.BookTinderFeed
import org.bookie.app.domain.model.RecommendationSource
import org.bookie.app.domain.model.SwipeType

fun BookTinderFeedDto.toDomain(): BookTinderFeed = BookTinderFeed(
    source = if (source == "ai") RecommendationSource.AI else RecommendationSource.FALLBACK,
    count = count,
    books = books.map { dto ->
        BookRecommendation(
            book = dto.toDomain(),
            reasons = dto.reasons,
        )
    },
)

fun SwipeType.toDto(): SwipeTypeDto = when (this) {
    SwipeType.LIKE -> SwipeTypeDto.LIKE
    SwipeType.DISLIKE -> SwipeTypeDto.DISLIKE
}

fun SwipeType.toCreateSwipeDto(bookId: String): CreateSwipeDto = CreateSwipeDto(
    bookId = bookId,
    type = toDto(),
)

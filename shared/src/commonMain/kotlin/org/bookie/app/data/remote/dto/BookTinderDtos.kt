package org.bookie.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class BookTinderFeedDto(
    val source: String,
    val count: Int,
    val books: List<BookDto>,
)

@Serializable
enum class SwipeTypeDto {
    LIKE,
    DISLIKE,
}

@Serializable
data class CreateSwipeDto(
    val bookId: String,
    val type: SwipeTypeDto,
)

@Serializable
data class SwipeResultDto(
    val success: Boolean,
    val message: String? = null,
)

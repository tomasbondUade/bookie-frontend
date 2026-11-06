package org.bookie.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TinderFeedDto(val source: String, val count: Int, val books: List<TinderBookDto>)

@Serializable
data class TinderBookDto(
    val id: String,
    val googleBookId: String,
    val title: String,
    val authors: List<String> = emptyList(),
    val description: String? = null,
    val genres: List<String> = emptyList(),
    val coverUrl: String? = null,
    val previewLink: String? = null,
    val infoLink: String? = null,
    val buyLink: String? = null,
    val reasons: List<String> = emptyList()
)

@Serializable
data class CreateSwipeDto(val bookId: String, val type: String)

@Serializable
data class SwipeResponseDto(val success: Boolean, val message: String)

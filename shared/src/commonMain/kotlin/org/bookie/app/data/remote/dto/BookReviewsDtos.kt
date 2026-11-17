package org.bookie.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class BookReviewsPageDto(
    val data: List<BookReviewDto>,
    val meta: BookReviewsMetaDto,
)

@Serializable
data class BookReviewsMetaDto(
    val page: Int,
    val limit: Int,
    val total: Int,
    val totalPages: Int,
)

@Serializable
data class BookReviewDto(
    val id: String,
    val rating: Int? = null,
    val comment: String? = null,
    val updatedAt: String,
    val user: BookReviewUserDto,
)

@Serializable
data class BookReviewUserDto(
    val id: String,
    val username: String,
    val avatarUrl: String? = null,
)

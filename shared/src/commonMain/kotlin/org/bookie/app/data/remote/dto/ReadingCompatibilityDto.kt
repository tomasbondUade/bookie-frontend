package org.bookie.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReadingCompatibilityDto(
    val user: CompatibilityUserDto,
    val compatibilityPercentage: Int,
    val sharedBooks: List<CompatibilityBookDto> = emptyList(),
    val sharedRatings: List<CompatibilityRatingDto> = emptyList(),
    val sharedGenres: List<String> = emptyList(),
    val sharedAuthors: List<String> = emptyList(),
    val reasons: List<String> = emptyList(),
    val dataQuality: String = "limited",
)

@Serializable
data class CompatibilityUserDto(
    val id: String,
    val username: String? = null,
    val avatarUrl: String? = null,
)

@Serializable
data class CompatibilityBookDto(
    val id: String,
    val title: String,
)

@Serializable
data class CompatibilityRatingDto(
    val bookId: String,
    val title: String,
    val userRating: Double,
    val targetRating: Double,
)

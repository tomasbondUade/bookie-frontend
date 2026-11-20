package org.bookie.app.domain.model

data class ReadingCompatibility(
    val user: CompatibilityUser,
    val compatibilityPercentage: Int,
    val sharedBooks: List<CompatibilityBook> = emptyList(),
    val sharedRatings: List<CompatibilityRating> = emptyList(),
    val sharedGenres: List<String> = emptyList(),
    val sharedAuthors: List<String> = emptyList(),
    val reasons: List<String> = emptyList(),
    val dataQuality: String = "limited",
)

data class CompatibilityUser(
    val id: String,
    val username: String?,
    val avatarUrl: String?,
)

data class CompatibilityBook(val id: String, val title: String)

data class CompatibilityRating(
    val bookId: String,
    val title: String,
    val userRating: Double,
    val targetRating: Double,
)

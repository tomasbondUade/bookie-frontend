package org.bookie.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReadingStatsDto(
    val mostReadGenres: List<GenreReadingCountDto> = emptyList(),
    val favoriteAuthors: List<AuthorFavoriteCountDto> = emptyList(),
    val averageRating: AverageRatingDto = AverageRatingDto(),
    val booksReadByMonth: List<MonthlyReadingCountDto> = emptyList(),
    val booksReadByYear: List<YearlyReadingCountDto> = emptyList(),
    val readingStreak: ReadingStreakDto = ReadingStreakDto(),
)

@Serializable
data class GenreReadingCountDto(
    val genre: String,
    val count: Int,
)

@Serializable
data class AuthorFavoriteCountDto(
    val author: String,
    val count: Int,
)

@Serializable
data class AverageRatingDto(
    val average: Double = 0.0,
    val count: Int = 0,
)

@Serializable
data class MonthlyReadingCountDto(
    val year: Int,
    val month: Int,
    val count: Int,
)

@Serializable
data class YearlyReadingCountDto(
    val year: Int,
    val count: Int,
)

@Serializable
data class ReadingStreakDto(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
)

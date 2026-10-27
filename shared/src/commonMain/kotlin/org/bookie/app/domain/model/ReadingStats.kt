package org.bookie.app.domain.model

data class ReadingStats(
    val mostReadGenres: List<GenreReadingCount> = emptyList(),
    val favoriteAuthors: List<AuthorFavoriteCount> = emptyList(),
    val averageRating: AverageRating = AverageRating(),
    val booksReadByMonth: List<MonthlyReadingCount> = emptyList(),
    val booksReadByYear: List<YearlyReadingCount> = emptyList(),
    val readingStreak: ReadingStreak = ReadingStreak(),
) {
    val hasData: Boolean
        get() = mostReadGenres.isNotEmpty() ||
            favoriteAuthors.isNotEmpty() ||
            averageRating.count > 0 ||
            averageRating.average != 0.0 ||
            booksReadByMonth.isNotEmpty() ||
            booksReadByYear.isNotEmpty() ||
            readingStreak.currentStreak > 0 ||
            readingStreak.longestStreak > 0
}

data class GenreReadingCount(
    val genre: String,
    val count: Int,
)

data class AuthorFavoriteCount(
    val author: String,
    val count: Int,
)

data class AverageRating(
    val average: Double = 0.0,
    val count: Int = 0,
)

data class MonthlyReadingCount(
    val year: Int,
    val month: Int,
    val count: Int,
)

data class YearlyReadingCount(
    val year: Int,
    val count: Int,
)

data class ReadingStreak(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
)

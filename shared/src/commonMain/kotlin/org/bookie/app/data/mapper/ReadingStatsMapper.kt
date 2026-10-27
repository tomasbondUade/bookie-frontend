package org.bookie.app.data.mapper

import org.bookie.app.data.remote.dto.ReadingStatsDto
import org.bookie.app.domain.model.AverageRating
import org.bookie.app.domain.model.AuthorFavoriteCount
import org.bookie.app.domain.model.GenreReadingCount
import org.bookie.app.domain.model.MonthlyReadingCount
import org.bookie.app.domain.model.ReadingStats
import org.bookie.app.domain.model.ReadingStreak
import org.bookie.app.domain.model.YearlyReadingCount

fun ReadingStatsDto.toDomain() = ReadingStats(
    mostReadGenres = mostReadGenres.map { GenreReadingCount(it.genre, it.count) },
    favoriteAuthors = favoriteAuthors.map { AuthorFavoriteCount(it.author, it.count) },
    averageRating = AverageRating(averageRating.average, averageRating.count),
    booksReadByMonth = booksReadByMonth.map {
        MonthlyReadingCount(year = it.year, month = it.month, count = it.count)
    },
    booksReadByYear = booksReadByYear.map { YearlyReadingCount(it.year, it.count) },
    readingStreak = ReadingStreak(
        currentStreak = readingStreak.currentStreak,
        longestStreak = readingStreak.longestStreak,
    ),
)

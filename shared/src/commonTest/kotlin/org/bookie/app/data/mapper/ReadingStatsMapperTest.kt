package org.bookie.app.data.mapper

import org.bookie.app.data.remote.dto.AverageRatingDto
import org.bookie.app.data.remote.dto.AuthorFavoriteCountDto
import org.bookie.app.data.remote.dto.GenreReadingCountDto
import org.bookie.app.data.remote.dto.MonthlyReadingCountDto
import org.bookie.app.data.remote.dto.ReadingStatsDto
import org.bookie.app.data.remote.dto.ReadingStreakDto
import org.bookie.app.data.remote.dto.YearlyReadingCountDto
import org.bookie.app.domain.model.AverageRating
import org.bookie.app.domain.model.AuthorFavoriteCount
import org.bookie.app.domain.model.GenreReadingCount
import org.bookie.app.domain.model.MonthlyReadingCount
import org.bookie.app.domain.model.ReadingStats
import org.bookie.app.domain.model.ReadingStreak
import org.bookie.app.domain.model.YearlyReadingCount
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadingStatsMapperTest {
    @Test
    fun `maps the backend statistics contract to domain models`() {
        val result = ReadingStatsDto(
            mostReadGenres = listOf(GenreReadingCountDto("Fantasy", 6)),
            favoriteAuthors = listOf(AuthorFavoriteCountDto("Ursula K. Le Guin", 2)),
            averageRating = AverageRatingDto(4.5, 8),
            booksReadByMonth = listOf(MonthlyReadingCountDto(2026, 9, 3)),
            booksReadByYear = listOf(YearlyReadingCountDto(2026, 7)),
            readingStreak = ReadingStreakDto(3, 5),
        ).toDomain()

        assertEquals(
            ReadingStats(
                mostReadGenres = listOf(GenreReadingCount("Fantasy", 6)),
                favoriteAuthors = listOf(AuthorFavoriteCount("Ursula K. Le Guin", 2)),
                averageRating = AverageRating(4.5, 8),
                booksReadByMonth = listOf(MonthlyReadingCount(2026, 9, 3)),
                booksReadByYear = listOf(YearlyReadingCount(2026, 7)),
                readingStreak = ReadingStreak(3, 5),
            ),
            result,
        )
    }

    @Test
    fun `defaults omitted statistics to empty values`() {
        assertEquals(ReadingStats(), ReadingStatsDto().toDomain())
    }
}

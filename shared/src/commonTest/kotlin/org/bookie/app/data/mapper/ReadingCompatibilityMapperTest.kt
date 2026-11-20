package org.bookie.app.data.mapper

import org.bookie.app.data.remote.dto.CompatibilityBookDto
import org.bookie.app.data.remote.dto.CompatibilityRatingDto
import org.bookie.app.data.remote.dto.CompatibilityUserDto
import org.bookie.app.data.remote.dto.ReadingCompatibilityDto
import org.bookie.app.domain.model.CompatibilityBook
import org.bookie.app.domain.model.CompatibilityRating
import org.bookie.app.domain.model.CompatibilityUser
import org.bookie.app.domain.model.ReadingCompatibility
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadingCompatibilityMapperTest {
    @Test
    fun `preserves profile score shared data reasons and data quality`() {
        val result = ReadingCompatibilityDto(
            user = CompatibilityUserDto("user-2", "reader", "https://example.test/avatar.png"),
            compatibilityPercentage = 84,
            sharedBooks = listOf(CompatibilityBookDto("book-1", "The Hobbit")),
            sharedRatings = listOf(CompatibilityRatingDto("book-1", "The Hobbit", 5.0, 4.0)),
            sharedGenres = listOf("Fantasy"),
            sharedAuthors = listOf("Tolkien"),
            reasons = listOf("Comparten el libro The Hobbit"),
            dataQuality = "good",
        ).toDomain()

        assertEquals(
            ReadingCompatibility(
                user = CompatibilityUser("user-2", "reader", "https://example.test/avatar.png"),
                compatibilityPercentage = 84,
                sharedBooks = listOf(CompatibilityBook("book-1", "The Hobbit")),
                sharedRatings = listOf(CompatibilityRating("book-1", "The Hobbit", 5.0, 4.0)),
                sharedGenres = listOf("Fantasy"),
                sharedAuthors = listOf("Tolkien"),
                reasons = listOf("Comparten el libro The Hobbit"),
                dataQuality = "good",
            ),
            result,
        )
    }
}

package org.bookie.app.data.mapper

import org.bookie.app.data.remote.dto.ReadingChallengeDto
import org.bookie.app.data.remote.dto.ReadingChallengeProgressDto
import org.bookie.app.domain.model.ReadingChallenge
import org.bookie.app.domain.model.ReadingChallengeProgress
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadingChallengeMapperTest {
    @Test
    fun `preserves optional challenge fields and type`() {
        assertEquals(
            ReadingChallenge(
                id = "challenge-1",
                title = "Leer fantasía",
                description = null,
                type = "GENRE",
                target = 5,
                genre = "Fantasy",
                createdAt = "2026-11-20T12:00:00.000Z",
                joined = true,
            ),
            ReadingChallengeDto(
                id = "challenge-1",
                title = "Leer fantasía",
                type = "GENRE",
                target = 5,
                genre = "Fantasy",
                createdAt = "2026-11-20T12:00:00.000Z",
                joined = true,
            ).toDomain(),
        )
    }

    @Test
    fun `preserves progress percentage and joined date`() {
        assertEquals(
            ReadingChallengeProgress(
                challengeId = "challenge-1",
                title = "Leer 12 libros",
                description = "Meta anual",
                type = "BOOK_COUNT",
                target = 12,
                genre = null,
                completed = 4,
                percentage = 33.33,
                joinedAt = "2026-11-01T12:00:00.000Z",
            ),
            ReadingChallengeProgressDto(
                challengeId = "challenge-1",
                title = "Leer 12 libros",
                description = "Meta anual",
                type = "BOOK_COUNT",
                target = 12,
                genre = null,
                completed = 4,
                percentage = 33.33,
                joinedAt = "2026-11-01T12:00:00.000Z",
            ).toDomain(),
        )
    }
}

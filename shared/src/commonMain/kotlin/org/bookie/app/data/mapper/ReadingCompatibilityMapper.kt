package org.bookie.app.data.mapper

import org.bookie.app.data.remote.dto.ReadingCompatibilityDto
import org.bookie.app.domain.model.CompatibilityBook
import org.bookie.app.domain.model.CompatibilityRating
import org.bookie.app.domain.model.CompatibilityUser
import org.bookie.app.domain.model.ReadingCompatibility

fun ReadingCompatibilityDto.toDomain() = ReadingCompatibility(
    user = CompatibilityUser(user.id, user.username, user.avatarUrl),
    compatibilityPercentage = compatibilityPercentage,
    sharedBooks = sharedBooks.map { CompatibilityBook(it.id, it.title) },
    sharedRatings = sharedRatings.map {
        CompatibilityRating(it.bookId, it.title, it.userRating, it.targetRating)
    },
    sharedGenres = sharedGenres,
    sharedAuthors = sharedAuthors,
    reasons = reasons,
    dataQuality = dataQuality,
)

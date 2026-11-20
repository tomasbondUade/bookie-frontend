package org.bookie.app.data.mapper

import org.bookie.app.data.remote.dto.ReadingChallengeDto
import org.bookie.app.data.remote.dto.ReadingChallengeProgressDto
import org.bookie.app.domain.model.ReadingChallenge
import org.bookie.app.domain.model.ReadingChallengeProgress

fun ReadingChallengeDto.toDomain() = ReadingChallenge(
    id = id,
    title = title,
    description = description,
    type = type,
    target = target,
    genre = genre,
    createdAt = createdAt,
    joined = joined,
)

fun ReadingChallengeProgressDto.toDomain() = ReadingChallengeProgress(
    challengeId = challengeId,
    title = title,
    description = description,
    type = type,
    target = target,
    genre = genre,
    completed = completed,
    percentage = percentage,
    joinedAt = joinedAt,
)

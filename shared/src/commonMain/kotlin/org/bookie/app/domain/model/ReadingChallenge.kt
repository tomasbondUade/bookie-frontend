package org.bookie.app.domain.model

data class ReadingChallenge(
    val id: String,
    val title: String,
    val description: String?,
    val type: String,
    val target: Int,
    val genre: String?,
    val createdAt: String,
    val joined: Boolean,
)

data class ReadingChallengeProgress(
    val challengeId: String,
    val title: String,
    val description: String?,
    val type: String,
    val target: Int,
    val genre: String?,
    val completed: Int,
    val percentage: Double,
    val joinedAt: String,
)

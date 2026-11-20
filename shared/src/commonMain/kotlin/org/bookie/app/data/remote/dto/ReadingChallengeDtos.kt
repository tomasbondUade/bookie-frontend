package org.bookie.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReadingChallengeDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val type: String,
    val target: Int,
    val genre: String? = null,
    val createdAt: String,
    val joined: Boolean,
)

@Serializable
data class ReadingChallengeProgressDto(
    val challengeId: String,
    val title: String,
    val description: String? = null,
    val type: String,
    val target: Int,
    val genre: String? = null,
    val completed: Int,
    val percentage: Double,
    val joinedAt: String,
)

@Serializable
data class JoinReadingChallengeDto(val challengeId: String)

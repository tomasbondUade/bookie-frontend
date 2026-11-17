package org.bookie.app.data.remote.dto

import kotlinx.serialization.Serializable

/** Only accepted writable fields. Never send ownerId, JWT or userId in request body. */
@Serializable
data class CreateCommunityDto(
    val name: String,
    val description: String,
)

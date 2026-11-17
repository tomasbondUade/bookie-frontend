package org.bookie.app.domain.model

/** New community fields accepted by POST /communities. Owner comes from JWT, not input. */
data class CreateCommunityInput(
    val name: String,
    val description: String,
)

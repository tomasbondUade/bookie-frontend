package org.bookie.app.domain.model

/** Public backend fields; no private user or session data in the UI model. */
data class Community(
    val id: String,
    val name: String,
    val description: String,
    val tags: List<String>,
    val imageUrl: String?,
    val ownerId: String,
    val createdAt: String,
    val score: Int? = null,
    val matchedTags: List<String> = emptyList(),
    val reason: String? = null,
)

data class CommunityPage(
    val communities: List<Community>,
    val page: Int,
    val limit: Int,
    val total: Int,
    val totalPages: Int,
)

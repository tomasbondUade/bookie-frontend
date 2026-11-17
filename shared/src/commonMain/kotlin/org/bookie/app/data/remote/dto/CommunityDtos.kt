package org.bookie.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CommunityDto(
    val id: String,
    val name: String,
    val description: String,
    val tags: List<String> = emptyList(),
    val imageUrl: String? = null,
    val ownerId: String,
    val createdAt: String,
    val score: Int? = null,
    val matchedTags: List<String> = emptyList(),
    val reason: String? = null,
)

@Serializable
data class CommunityPageDto(
    val data: List<CommunityDto>,
    val meta: CommunityMetaDto,
)

@Serializable
data class CommunityMetaDto(
    val page: Int,
    val limit: Int,
    val total: Int,
    val totalPages: Int,
)

@Serializable
data class CommunityPostDto(
    val id: String,
    val title: String,
    val content: String,
    val communityId: String,
    val createdAt: String,
    val author: CommunityPostAuthorDto? = null,
)

@Serializable
data class CommunityPostAuthorDto(
    val id: String,
    val username: String,
    val avatarUrl: String? = null,
)

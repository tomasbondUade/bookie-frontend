package org.bookie.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PostAuthorDto(val id: String, val username: String? = null, val avatarUrl: String? = null)
@Serializable
data class CommunityPostDto(
    val id: String, val title: String, val content: String,
    val communityId: String, val authorId: String, val createdAt: String,
    val author: PostAuthorDto? = null,
)
@Serializable
data class CreateCommunityPostDto(val title: String, val content: String)

package org.bookie.app.data.mapper

import org.bookie.app.data.remote.dto.*
import org.bookie.app.domain.model.*

fun CommunityPostDto.toDomain() = CommunityPost(
    id, title, content, communityId, authorId, createdAt,
    author?.let { PostAuthor(it.id, it.username, it.avatarUrl) },
)
fun CreateCommunityPostInput.toDto() = CreateCommunityPostDto(title.trim(), content.trim())

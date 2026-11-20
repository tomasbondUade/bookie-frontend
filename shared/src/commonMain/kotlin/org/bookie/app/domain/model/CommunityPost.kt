package org.bookie.app.domain.model

data class PostAuthor(val id: String, val username: String?, val avatarUrl: String?)
data class CommunityPost(
    val id: String, val title: String, val content: String,
    val communityId: String, val authorId: String, val createdAt: String,
    val author: PostAuthor? = null,
)
data class CreateCommunityPostInput(val title: String, val content: String)

data class CommunityComment(
    val id: String,
    val content: String,
    val postId: String,
    val authorId: String,
    val parentId: String?,
    val createdAt: String,
)

data class CreateCommunityCommentInput(val content: String, val parentId: String? = null)

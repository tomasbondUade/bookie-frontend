package org.bookie.app.domain.usecase

import org.bookie.app.domain.model.CreateCommunityCommentInput
import org.bookie.app.domain.repository.CommunityPostsRepository

class CreateCommunityComment(private val repository: CommunityPostsRepository) {
    suspend operator fun invoke(postId: String, input: CreateCommunityCommentInput) =
        repository.createComment(postId, input)
}

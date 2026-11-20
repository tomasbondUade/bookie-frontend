package org.bookie.app.domain.usecase

import org.bookie.app.domain.repository.CommunityPostsRepository

class GetCommunityComments(private val repository: CommunityPostsRepository) {
    suspend operator fun invoke(postId: String, limit: Int = 50) =
        repository.getComments(postId, limit)
}

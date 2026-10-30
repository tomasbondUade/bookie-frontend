package org.bookie.app.domain.usecase

import org.bookie.app.domain.repository.CommunityPostsRepository

class GetCommunityPosts(private val repository: CommunityPostsRepository) {
    suspend operator fun invoke(communityId: String, limit: Int = 20) =
        repository.getPosts(communityId, limit)
}

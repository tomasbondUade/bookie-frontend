package org.bookie.app.domain.usecase

import org.bookie.app.domain.model.CreateCommunityPostInput
import org.bookie.app.domain.repository.CommunityPostsRepository

class CreateCommunityPost(private val repository: CommunityPostsRepository) {
    suspend operator fun invoke(communityId: String, input: CreateCommunityPostInput) =
        repository.createPost(communityId, input)
}

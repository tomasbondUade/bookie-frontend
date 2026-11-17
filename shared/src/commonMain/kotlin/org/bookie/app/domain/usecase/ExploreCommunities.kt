package org.bookie.app.domain.usecase

import org.bookie.app.domain.repository.CommunityRepository

class GetCommunities(private val repository: CommunityRepository) {
    suspend operator fun invoke(page: Int = 1, limit: Int = 20) =
        repository.listCommunities(page, limit)
}

class GetRecommendedCommunities(private val repository: CommunityRepository) {
    suspend operator fun invoke() = repository.recommended()
}

class GetCommunityPosts(private val repository: CommunityRepository) {
    suspend operator fun invoke(communityId: String, limit: Int = 20) =
        repository.posts(communityId, limit)
}

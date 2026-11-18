package org.bookie.app.domain.usecase

import org.bookie.app.domain.repository.CommunityRepository

class GetCommunities(private val repository: CommunityRepository) {
    suspend operator fun invoke(page: Int = 1, limit: Int = 20) =
        repository.listCommunities(page, limit)
}

class GetRecommendedCommunities(private val repository: CommunityRepository) {
    suspend operator fun invoke() = repository.recommended()
}

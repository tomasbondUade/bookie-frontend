package org.bookie.app.domain.repository

import org.bookie.app.domain.model.Community
import org.bookie.app.domain.model.CommunityPage
import org.bookie.app.domain.model.CreateCommunityInput
import org.bookie.app.domain.result.Resource

interface CommunityRepository {
    suspend fun listCommunities(page: Int = 1, limit: Int = 20): Resource<CommunityPage>
    suspend fun recommended(): Resource<List<Community>>
    suspend fun createCommunity(input: CreateCommunityInput): Resource<Community>
}

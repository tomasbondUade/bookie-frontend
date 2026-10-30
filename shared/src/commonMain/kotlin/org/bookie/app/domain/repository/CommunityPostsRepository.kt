package org.bookie.app.domain.repository

import org.bookie.app.domain.model.*
import org.bookie.app.domain.result.Resource

interface CommunityPostsRepository {
    suspend fun getPosts(communityId: String, limit: Int = 20): Resource<List<CommunityPost>>
    suspend fun createPost(communityId: String, input: CreateCommunityPostInput): Resource<CommunityPost>
}

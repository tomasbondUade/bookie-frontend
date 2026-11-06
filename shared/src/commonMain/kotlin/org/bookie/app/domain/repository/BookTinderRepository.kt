package org.bookie.app.domain.repository

import org.bookie.app.domain.model.*
import org.bookie.app.domain.result.Resource

interface BookTinderRepository {
    suspend fun getTinderBooks(limit: Int): Resource<TinderFeed>
    suspend fun registerSwipe(swipe: BookSwipe): Resource<SwipeResult>
}

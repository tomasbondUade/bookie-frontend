package org.bookie.app.domain.repository

import org.bookie.app.domain.model.ReadingCompatibility
import org.bookie.app.domain.result.Resource

interface ReadingCompatibilityRepository {
    suspend fun getCompatibility(targetUserId: String): Resource<ReadingCompatibility>
}

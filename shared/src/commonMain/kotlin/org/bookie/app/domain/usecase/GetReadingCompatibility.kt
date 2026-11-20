package org.bookie.app.domain.usecase

import org.bookie.app.domain.repository.ReadingCompatibilityRepository

class GetReadingCompatibility(
    private val repository: ReadingCompatibilityRepository,
) {
    suspend operator fun invoke(targetUserId: String) = repository.getCompatibility(targetUserId)
}

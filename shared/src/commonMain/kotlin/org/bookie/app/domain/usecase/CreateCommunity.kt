package org.bookie.app.domain.usecase

import org.bookie.app.domain.model.CreateCommunityInput
import org.bookie.app.domain.repository.CommunityRepository

class CreateCommunity(private val repository: CommunityRepository) {
    suspend operator fun invoke(input: CreateCommunityInput) =
        repository.createCommunity(input)
}

package org.bookie.app.domain.usecase

import org.bookie.app.domain.repository.BookTinderRepository

class GetTinderBooks(private val repository: BookTinderRepository) {
    suspend operator fun invoke(limit: Int = 10) = repository.getTinderBooks(limit.coerceIn(1, 50))
}

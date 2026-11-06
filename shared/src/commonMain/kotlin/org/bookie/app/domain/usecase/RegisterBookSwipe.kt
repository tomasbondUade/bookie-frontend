package org.bookie.app.domain.usecase

import org.bookie.app.domain.model.BookSwipe
import org.bookie.app.domain.repository.BookTinderRepository

class RegisterBookSwipe(private val repository: BookTinderRepository) {
    suspend operator fun invoke(swipe: BookSwipe) = repository.registerSwipe(swipe)
}

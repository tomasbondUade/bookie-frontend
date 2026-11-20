package org.bookie.app.domain.usecase

import org.bookie.app.domain.model.SwipeType
import org.bookie.app.domain.repository.BookTinderRepository
import org.bookie.app.domain.result.Resource

class RecordBookSwipe(private val repository: BookTinderRepository) {
    suspend operator fun invoke(bookId: String, type: SwipeType): Resource<Unit> =
        repository.recordSwipe(bookId, type)
}

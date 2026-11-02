package org.bookie.app.domain.model

/**
 * Optional progress values used by both library snapshots and reading sessions.
 * The API does not provide a total page count, so page and percentage are independent.
 * Notes belong to the caller's context (private library notes or session notes).
 */
data class ReadingProgress(
    val currentPage: Int? = null,
    val progressPercentage: Int? = null,
    val notes: String? = null,
) {
    init {
        requireValidReadingProgress(currentPage, progressPercentage)
    }
}

internal fun requireValidReadingProgress(currentPage: Int?, progressPercentage: Int?) {
    require(currentPage == null || currentPage >= 0) {
        "Current page must be zero or greater."
    }
    require(progressPercentage == null || progressPercentage in 0..100) {
        "Progress percentage must be between 0 and 100."
    }
}

package org.bookie.app.domain.model

/** Public catalog reviews: never expose private library notes or user email. */
data class BookReview(
    val id: String,
    val rating: Int?,
    val comment: String?,
    val updatedAt: String,
    val username: String,
    val avatarUrl: String?,
)

enum class BookReviewSort(val apiValue: String) {
    RECENT("recent"),
    TOP("top"),
}

data class BookReviewsPage(
    val reviews: List<BookReview>,
    val page: Int,
    val limit: Int,
    val total: Int,
    val totalPages: Int,
)

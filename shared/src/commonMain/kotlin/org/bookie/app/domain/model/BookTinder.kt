package org.bookie.app.domain.model

data class BookTinderFeed(
    val source: RecommendationSource,
    val count: Int,
    val books: List<BookRecommendation>,
)

data class BookRecommendation(
    val book: Book,
    val reasons: List<String>,
)

enum class RecommendationSource {
    AI,
    FALLBACK,
}

enum class SwipeType {
    LIKE,
    DISLIKE,
}

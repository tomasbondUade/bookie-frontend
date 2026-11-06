package org.bookie.app.domain.model

enum class RecommendationSource { AI, FALLBACK }
enum class SwipeType { LIKE, DISLIKE }

data class TinderRecommendedBook(val book: Book, val reasons: List<String> = emptyList())
data class TinderFeed(val source: RecommendationSource, val count: Int, val books: List<TinderRecommendedBook>)
data class BookSwipe(val bookId: String, val type: SwipeType)
data class SwipeResult(val success: Boolean, val message: String)

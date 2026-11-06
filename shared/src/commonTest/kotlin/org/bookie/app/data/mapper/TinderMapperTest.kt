package org.bookie.app.data.mapper

import org.bookie.app.data.remote.dto.TinderBookDto
import org.bookie.app.data.remote.dto.TinderFeedDto
import org.bookie.app.domain.model.RecommendationSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TinderMapperTest {
    private fun book() = TinderBookDto(
        id = "internal-123",
        googleBookId = "google-456",
        title = "Dune",
        authors = listOf("Frank Herbert"),
        reasons = listOf("Based on your preferences")
    )

    @Test fun mapsAiFeedAndPreservesInternalBookId() {
        val feed = TinderFeedDto("ai", 1, listOf(book())).toTinderDomain()
        assertEquals(RecommendationSource.AI, feed.source)
        assertEquals("internal-123", feed.books.single().book.id)
        assertEquals("google-456", feed.books.single().book.googleBookId)
        assertEquals(listOf("Based on your preferences"), feed.books.single().reasons)
    }

    @Test fun mapsEmptyFallbackFeed() {
        val feed = TinderFeedDto("fallback", 0, emptyList()).toTinderDomain()
        assertEquals(RecommendationSource.FALLBACK, feed.source)
        assertEquals(emptyList(), feed.books)
    }

    @Test fun rejectsUnknownSource() {
        assertFailsWith<IllegalStateException> {
            TinderFeedDto("unexpected", 0, emptyList()).toTinderDomain()
        }
    }
}

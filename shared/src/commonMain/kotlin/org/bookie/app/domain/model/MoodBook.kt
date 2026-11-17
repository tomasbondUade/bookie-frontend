package org.bookie.app.domain.model

/** Reuses the catalog Book model; similarity is specific to this mood query. */
data class MoodBook(
    val book: Book,
    val relevance: Double?,
)

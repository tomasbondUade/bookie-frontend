package org.bookie.app.domain.model

enum class ReadingPath { RECOMMENDED, PUBLICATION, CHRONOLOGICAL }
enum class SeriesNodeStatus { COMPLETED, IN_PROGRESS, UP_NEXT, LOCKED, PENDING }

data class SeriesNodeProgress(
    val nodeId: String, val bookId: String, val title: String,
    val nodeType: String, val sequenceOrder: Double,
    val status: SeriesNodeStatus,
    val progressPercentage: Int? = null, val currentPage: Int? = null,
)
data class SeriesProgress(
    val seriesId: String, val title: String, val totalBooks: Int,
    val readBooks: Int, val completionPercentage: Double,
    val isCompleted: Boolean, val nodes: List<SeriesNodeProgress>,
)
data class SeriesMapNode(
    val nodeId: String, val bookId: String, val title: String,
    val nodeType: String, val sequenceOrder: Double,
    val publicationOrder: Double?, val chronologicalOrder: Double?,
    val recommendedOrder: Double?, val status: SeriesNodeStatus? = null,
    val progressPercentage: Int? = null, val currentPage: Int? = null,
)
data class SeriesMapEdge(
    val id: String, val sourceNodeId: String,
    val targetNodeId: String, val relationType: String,
)
data class SeriesReadingMap(
    val seriesId: String, val title: String, val path: ReadingPath,
    val nodes: List<SeriesMapNode>, val edges: List<SeriesMapEdge>,
)

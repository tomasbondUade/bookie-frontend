package org.bookie.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
enum class ReadingPathDto { RECOMMENDED, PUBLICATION, CHRONOLOGICAL }
@Serializable
enum class SeriesNodeStatusDto { COMPLETED, IN_PROGRESS, UP_NEXT, LOCKED, PENDING }
@Serializable
data class SeriesNodeProgressDto(
    val nodeId: String, val bookId: String, val title: String,
    val nodeType: String, val sequenceOrder: Double,
    val status: SeriesNodeStatusDto,
    val progressPercentage: Int? = null, val currentPage: Int? = null,
)
@Serializable
data class SeriesProgressDto(
    val seriesId: String, val title: String, val totalBooks: Int,
    val readBooks: Int, val completionPercentage: Double,
    val isCompleted: Boolean, val nodes: List<SeriesNodeProgressDto>,
)
@Serializable
data class SeriesMapNodeDto(
    val nodeId: String, val bookId: String, val title: String,
    val nodeType: String, val sequenceOrder: Double,
    val publicationOrder: Double? = null, val chronologicalOrder: Double? = null,
    val recommendedOrder: Double? = null, val status: SeriesNodeStatusDto? = null,
    val progressPercentage: Int? = null, val currentPage: Int? = null,
)
@Serializable
data class SeriesMapEdgeDto(
    val id: String, val sourceNodeId: String,
    val targetNodeId: String, val relationType: String,
)
@Serializable
data class SeriesReadingMapDto(
    val seriesId: String, val title: String, val path: ReadingPathDto,
    val nodes: List<SeriesMapNodeDto>, val edges: List<SeriesMapEdgeDto>,
)

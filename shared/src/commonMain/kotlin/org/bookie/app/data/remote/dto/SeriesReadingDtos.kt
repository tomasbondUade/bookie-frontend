package org.bookie.app.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

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

@Serializable
data class SeriesCatalogPageDto(
    val data: List<SeriesCatalogDto>,
    val meta: SeriesCatalogPageMetaDto,
)

@Serializable
data class SeriesCatalogPageMetaDto(
    val page: Int,
    val limit: Int,
    val total: Int,
    val totalPages: Int,
)

@Serializable
data class UniverseReferenceDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val author: String? = null,
)

@Serializable
data class UniverseProgressDto(
    val universeId: String,
    val name: String,
    val totalBooks: Int,
    val readBooks: Int,
    val completionPercentage: Double,
    val isCompleted: Boolean,
)

@Serializable
data class UniverseCatalogDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val author: String? = null,
    @SerialName("_count") val counts: UniverseSeriesCountDto? = null,
    val progress: UniverseProgressDto? = null,
)

@Serializable
data class UniverseSeriesCountDto(val series: Int)

@Serializable
data class SeriesCatalogDto(
    val id: String,
    val universeId: String? = null,
    val title: String,
    val description: String? = null,
    val coverUrl: String? = null,
    val totalBooks: Int = 0,
    val universe: UniverseReferenceDto? = null,
    @SerialName("_count") val counts: SeriesNodeCountDto? = null,
    val nodes: List<SeriesCatalogNodeDto> = emptyList(),
    val edges: List<SeriesCatalogEdgeDto> = emptyList(),
)

@Serializable
data class SeriesNodeCountDto(val nodes: Int)

@Serializable
data class SeriesCatalogNodeDto(
    val id: String,
    val seriesId: String,
    val bookId: String,
    val nodeType: String,
    val sequenceOrder: Double,
    val publicationOrder: Double? = null,
    val chronologicalOrder: Double? = null,
    val recommendedOrder: Double? = null,
    val label: String? = null,
    val book: SeriesCatalogBookDto,
)

@Serializable
data class SeriesCatalogBookDto(
    val id: String,
    val title: String,
)

@Serializable
data class SeriesCatalogEdgeDto(
    val id: String,
    val seriesId: String,
    val sourceNodeId: String,
    val targetNodeId: String,
    val relationType: String,
)

@Serializable
data class UniverseDetailDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val author: String? = null,
    val series: List<SeriesCatalogDto> = emptyList(),
)

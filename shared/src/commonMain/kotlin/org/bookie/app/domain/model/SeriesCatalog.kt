package org.bookie.app.domain.model

data class SeriesCatalogPage(
    val series: List<SeriesCatalogEntry>,
    val page: Int,
    val limit: Int,
    val total: Int,
    val totalPages: Int,
)

data class SeriesCatalogEntry(
    val id: String,
    val universeId: String?,
    val title: String,
    val description: String?,
    val coverUrl: String?,
    val totalBooks: Int,
    val nodeCount: Int,
    val universe: UniverseReference?,
)

data class UniverseReference(
    val id: String,
    val name: String,
    val description: String?,
    val imageUrl: String?,
    val author: String?,
)

data class UniverseProgress(
    val universeId: String,
    val name: String,
    val totalBooks: Int,
    val readBooks: Int,
    val completionPercentage: Double,
    val isCompleted: Boolean,
)

data class UniverseCatalogEntry(
    val id: String,
    val name: String,
    val description: String?,
    val imageUrl: String?,
    val author: String?,
    val seriesCount: Int,
    val progress: UniverseProgress?,
)

data class UniverseDetail(
    val id: String,
    val name: String,
    val description: String?,
    val imageUrl: String?,
    val author: String?,
    val series: List<SeriesCatalogEntry>,
)

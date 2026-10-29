package org.bookie.app.data.mapper

import org.bookie.app.data.remote.dto.*
import org.bookie.app.domain.model.*

fun ReadingPath.toDto(): ReadingPathDto = ReadingPathDto.valueOf(name)
fun ReadingPathDto.toDomain(): ReadingPath = ReadingPath.valueOf(name)
fun SeriesNodeStatusDto.toDomain(): SeriesNodeStatus = SeriesNodeStatus.valueOf(name)

fun SeriesNodeProgressDto.toDomain() = SeriesNodeProgress(
    nodeId, bookId, title, nodeType, sequenceOrder, status.toDomain(),
    progressPercentage, currentPage,
)
fun SeriesProgressDto.toDomain() = SeriesProgress(
    seriesId, title, totalBooks, readBooks, completionPercentage,
    isCompleted, nodes.map { it.toDomain() },
)
fun SeriesMapNodeDto.toDomain() = SeriesMapNode(
    nodeId, bookId, title, nodeType, sequenceOrder,
    publicationOrder, chronologicalOrder, recommendedOrder,
    status?.toDomain(), progressPercentage, currentPage,
)
fun SeriesMapEdgeDto.toDomain() = SeriesMapEdge(
    id, sourceNodeId, targetNodeId, relationType,
)
fun SeriesReadingMapDto.toDomain() = SeriesReadingMap(
    seriesId, title, path.toDomain(),
    nodes.map { it.toDomain() }, edges.map { it.toDomain() },
)

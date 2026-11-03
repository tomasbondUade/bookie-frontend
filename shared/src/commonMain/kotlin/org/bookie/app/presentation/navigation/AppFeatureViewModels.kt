package org.bookie.app.presentation.navigation

import org.bookie.app.presentation.profile.ReadingStatsViewModel
import org.bookie.app.presentation.series.SeriesExplorerViewModel
import org.bookie.app.presentation.series.SeriesReadingViewModel

/** ViewModels are supplied by the platform composition root using its authenticated API client. */
data class AppFeatureViewModels(
    val readingStats: ReadingStatsViewModel,
    val seriesExplorer: SeriesExplorerViewModel,
    val seriesReading: SeriesReadingViewModel,
    val onSignIn: (() -> Unit)? = null,
    val onOpenBook: (String) -> Unit = {},
)

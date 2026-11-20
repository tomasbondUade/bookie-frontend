package org.bookie.app.presentation.navigation

import org.bookie.app.presentation.auth.AuthViewModel
import org.bookie.app.presentation.community.CommunityViewModel
import org.bookie.app.presentation.library.LibraryExperienceViewModel
import org.bookie.app.presentation.profile.ReadingStatsViewModel
import org.bookie.app.presentation.preferences.ReadingPreferencesViewModel
import org.bookie.app.presentation.search.BookReviewsViewModel
import org.bookie.app.presentation.search.MoodSearchViewModel
import org.bookie.app.presentation.series.SeriesExplorerViewModel
import org.bookie.app.presentation.series.SeriesReadingViewModel

/** ViewModels are supplied by the platform composition root using its authenticated API client. */
data class AppFeatureViewModels(
    val readingStats: ReadingStatsViewModel,
    val seriesExplorer: SeriesExplorerViewModel,
    val seriesReading: SeriesReadingViewModel,
    val onSignIn: (() -> Unit)? = null,
    val onOpenBook: (String) -> Unit = {},
    val moodSearch: MoodSearchViewModel? = null,
    val bookReviews: BookReviewsViewModel? = null,
    val community: CommunityViewModel? = null,
    val auth: AuthViewModel? = null,
    val readingPreferences: ReadingPreferencesViewModel? = null,
    val libraryExperience: LibraryExperienceViewModel? = null,
)

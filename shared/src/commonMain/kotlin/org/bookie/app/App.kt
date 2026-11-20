package org.bookie.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bookie.app.presentation.auth.AuthScreen
import org.bookie.app.presentation.auth.AuthUiState
import org.bookie.app.presentation.challenges.ReadingChallengesScreen
import org.bookie.app.presentation.community.CommunityScreen
import org.bookie.app.presentation.components.BookieComponentsScreen
import org.bookie.app.presentation.library.LibraryExperienceScreen
import org.bookie.app.presentation.lists.BookListsScreen
import org.bookie.app.presentation.lists.LibraryWorkspaceScreen
import org.bookie.app.presentation.navigation.AppDestination
import org.bookie.app.presentation.navigation.AppFeatureViewModels
import org.bookie.app.presentation.profile.ReadingStatsScreen
import org.bookie.app.presentation.profile.AccountScreen
import org.bookie.app.presentation.search.MoodSearchViewModel
import org.bookie.app.presentation.preferences.ReadingPreferencesScreen
import org.bookie.app.presentation.preferences.ReadingPreferencesUiState
import org.bookie.app.presentation.search.MoodSearchScreen
import org.bookie.app.presentation.series.SeriesExplorerScreen
import org.bookie.app.presentation.series.SeriesReadingScreen
import org.bookie.app.presentation.tinder.BookTinderScreen
import org.bookie.app.theme.BookieTheme

@Composable
@Preview
fun App(featureViewModels: AppFeatureViewModels? = null) {
    BookieTheme {
        val configuredAccount = if (featureViewModels?.auth == null) {
            rememberConfiguredAccountExperience()
        } else null
        val authViewModel = featureViewModels?.auth ?: configuredAccount?.authViewModel
        val configuredSearch = if (configuredAccount != null) {
            remember(configuredAccount) { configuredAccount.moodSearchViewModel() }
        } else null
        val configuredLibrary = if (configuredAccount != null) {
            remember(configuredAccount) { configuredAccount.libraryViewModel() }
        } else null
        val configuredPreferences = if (configuredAccount != null) {
            remember(configuredAccount) { configuredAccount.readingPreferencesViewModel() }
        } else null
        val configuredStats = if (configuredAccount != null) {
            remember(configuredAccount) { configuredAccount.readingStatsViewModel() }
        } else null
        if (authViewModel == null) {
            MainAppContent(featureViewModels)
        } else {
            val authState by authViewModel.uiState.collectAsStateWithLifecycle()
            val preferencesViewModel = featureViewModels?.readingPreferences
            val preferencesState = preferencesViewModel?.uiState?.collectAsStateWithLifecycle()

            LaunchedEffect(authViewModel, authState, preferencesViewModel) {
                if (preferencesViewModel != null) {
                    val userId = (authState as? AuthUiState.SignedIn)?.userId
                    if (userId == null) preferencesViewModel.clearForSignedOut()
                    else preferencesViewModel.loadForAccount(userId)
                }
            }

            if (authState !is AuthUiState.SignedIn) {
                AuthScreen(viewModel = authViewModel)
            } else if (
                preferencesViewModel != null &&
                preferencesState?.value !is ReadingPreferencesUiState.Viewing
            ) {
                ReadingPreferencesScreen(
                    viewModel = preferencesViewModel,
                    onSignIn = featureViewModels?.onSignIn,
                )
            } else {
                val account = authState as AuthUiState.SignedIn
                key(account.userId) {
                    MainAppContent(
                        featureViewModels = featureViewModels,
                        currentUserId = account.userId,
                        accountViewModel = authViewModel,
                        configuredMoodSearch = configuredSearch,
                        configuredLibraryViewModel = configuredLibrary,
                        configuredReadingPreferencesViewModel = configuredPreferences,
                        configuredReadingStatsViewModel = configuredStats,
                    )
                }
            }
        }
    }
}

@Composable
private fun MainAppContent(
    featureViewModels: AppFeatureViewModels?,
    currentUserId: String? = null,
    accountViewModel: org.bookie.app.presentation.auth.AuthViewModel? = null,
    configuredMoodSearch: MoodSearchViewModel? = null,
    configuredLibraryViewModel: org.bookie.app.presentation.library.LibraryExperienceViewModel? = null,
    configuredReadingPreferencesViewModel: org.bookie.app.presentation.preferences.ReadingPreferencesViewModel? = null,
    configuredReadingStatsViewModel: org.bookie.app.presentation.profile.ReadingStatsViewModel? = null,
) {
    var selectedRoute by rememberSaveable {
            mutableStateOf(AppDestination.HOME.route)
        }
    var selectedSurface by rememberSaveable {
            mutableStateOf(AppSurface.MAIN.name)
        }
    val profilePreferences = featureViewModels?.readingPreferences ?: configuredReadingPreferencesViewModel
    val profileStats = featureViewModels?.readingStats ?: configuredReadingStatsViewModel
    val fallbackMoodSearch = if (featureViewModels?.moodSearch == null && configuredMoodSearch == null) {
            rememberConfiguredMoodSearchViewModel()
        } else null
    val moodSearchViewModel = featureViewModels?.moodSearch ?: configuredMoodSearch ?: fallbackMoodSearch
    val fallbackBookReviews = if (featureViewModels?.bookReviews == null) {
            rememberConfiguredBookReviewsViewModel()
        } else null
    val bookReviewsViewModel = featureViewModels?.bookReviews ?: fallbackBookReviews
    val fallbackCommunity = if (featureViewModels?.community == null) {
            rememberConfiguredCommunityViewModel()
        } else null
    val communityViewModel = featureViewModels?.community ?: fallbackCommunity
    val selectedDestination =
            AppDestination.fromRoute(selectedRoute) ?: AppDestination.HOME
    val surface = AppSurface.entries.firstOrNull {
            it.name == selectedSurface
        } ?: AppSurface.MAIN

    Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar {
                    AppDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = selectedDestination == destination,
                            onClick = {
                                selectedRoute = destination.route
                                selectedSurface = AppSurface.MAIN.name
                            },
                            icon = {
                                Icon(
                                    imageVector = destination.icon(),
                                    contentDescription = null,
                                )
                            },
                            label = {
                                Text(destination.label)
                            },
                        )
                    }
                }
            },
    ) { contentPadding ->
        val contentModifier =
                Modifier.fillMaxSize().padding(contentPadding)

        when {
                surface == AppSurface.COMPONENTS_SHOWCASE -> {
                    BookieComponentsScreen(
                        modifier = contentModifier,
                    )
                }

                surface == AppSurface.BOOK_TINDER && featureViewModels?.bookTinder != null -> {
                    BookTinderScreen(
                        viewModel = featureViewModels.bookTinder,
                        onSignIn = featureViewModels.onSignIn,
                        onBack = { selectedSurface = AppSurface.MAIN.name },
                        modifier = contentModifier,
                    )
                }

                surface == AppSurface.READING_CHALLENGES &&
                        featureViewModels?.readingChallenges != null -> {
                    ReadingChallengesScreen(
                        viewModel = featureViewModels.readingChallenges,
                        onSignIn = featureViewModels.onSignIn,
                        onBack = { selectedSurface = AppSurface.MAIN.name },
                        modifier = contentModifier,
                    )
                }

                surface == AppSurface.SERIES_DISCOVERY &&
                        featureViewModels != null -> {
                    SeriesExplorerScreen(
                        viewModel = featureViewModels.seriesExplorer,
                        onOpenSeries = { seriesId ->
                            featureViewModels.seriesReading.selectSeries(seriesId)
                            selectedSurface = AppSurface.SERIES_READING.name
                        },
                        onSignIn = featureViewModels.onSignIn,
                        modifier = contentModifier,
                    )
                }

                surface == AppSurface.SERIES_READING &&
                        featureViewModels != null -> {
                    SeriesReadingScreen(
                        viewModel = featureViewModels.seriesReading,
                        onOpenBook = featureViewModels.onOpenBook,
                        onSignIn = featureViewModels.onSignIn,
                        onBack = {
                            selectedSurface = AppSurface.SERIES_DISCOVERY.name
                        },
                        modifier = contentModifier,
                    )
                }

                selectedDestination == AppDestination.SEARCH -> {
                    if (moodSearchViewModel != null) {
                        MoodSearchScreen(
                            viewModel = moodSearchViewModel,
                            modifier = contentModifier,
                            reviewsViewModel = bookReviewsViewModel,
                        )
                    } else {
                        Column(modifier = contentModifier.padding(24.dp)) {
                            Text("Explorar por estado de ánimo", style = MaterialTheme.typography.headlineSmall)
                            Text("Falta configurar la URL del backend Bookie (bookie_api_base_url).")
                        }
                    }
                }

                selectedDestination == AppDestination.COMMUNITY -> {
                    if (communityViewModel != null) {
                        CommunityScreen(
                            viewModel = communityViewModel,
                            compatibilityViewModel = featureViewModels?.readingCompatibility,
                            currentUserId = currentUserId,
                            onSignIn = featureViewModels?.onSignIn,
                            modifier = contentModifier,
                        )
                    } else {
                        Column(modifier = contentModifier.padding(24.dp)) {
                            Text("Comunidades", style = MaterialTheme.typography.headlineSmall)
                            Text("Falta configurar la URL del backend Bookie (bookie_api_base_url).")
                        }
                    }
                }

                selectedDestination == AppDestination.LIBRARY && featureViewModels?.bookLists != null &&
                        featureViewModels.libraryExperience != null -> {
                    LibraryWorkspaceScreen(
                        libraryViewModel = featureViewModels.libraryExperience,
                        listsViewModel = featureViewModels.bookLists,
                        onSignIn = featureViewModels.onSignIn,
                        modifier = contentModifier,
                    )
                }

                selectedDestination == AppDestination.LIBRARY && featureViewModels?.bookLists != null -> {
                    BookListsScreen(
                        viewModel = featureViewModels.bookLists,
                        onSignIn = featureViewModels.onSignIn,
                        modifier = contentModifier,
                    )
                }

                selectedDestination == AppDestination.LIBRARY &&
                        featureViewModels?.libraryExperience != null -> {
                    LibraryExperienceScreen(featureViewModels.libraryExperience, contentModifier)
                }

                selectedDestination == AppDestination.LIBRARY &&
                        featureViewModels?.libraryExperience == null &&
                        configuredLibraryViewModel != null -> {
                    LibraryExperienceScreen(configuredLibraryViewModel, contentModifier)
                }

                surface == AppSurface.READING_STATS && profileStats != null -> {
                    ReadingStatsScreen(
                        viewModel = profileStats,
                        modifier = contentModifier,
                        onSessionExpired = featureViewModels.onSignIn,
                        onEditPreferences = profilePreferences?.let { preferences ->
                            {
                                preferences.beginEdit()
                                selectedSurface = AppSurface.PREFERENCES.name
                            }
                        },
                    )
                }

                selectedDestination == AppDestination.PROFILE && accountViewModel != null &&
                        surface == AppSurface.MAIN -> {
                    AccountScreen(
                        authViewModel = accountViewModel,
                        onOpenStatistics = if (profileStats != null) {
                            { selectedSurface = AppSurface.READING_STATS.name }
                        } else null,
                        onOpenPreferences = profilePreferences?.let { preferences ->
                            {
                                currentUserId?.let(preferences::loadForAccount)
                                selectedSurface = AppSurface.PREFERENCES.name
                            }
                        },
                        modifier = contentModifier,
                    )
                }

                surface == AppSurface.PREFERENCES &&
                        profilePreferences != null -> {
                    ReadingPreferencesScreen(
                        viewModel = profilePreferences,
                        modifier = contentModifier,
                        onClose = {
                            selectedRoute = AppDestination.PROFILE.route
                            selectedSurface = AppSurface.MAIN.name
                        },
                        onSignIn = featureViewModels?.onSignIn,
                    )
                }

                selectedDestination == AppDestination.PROFILE &&
                        featureViewModels != null -> {
                    ReadingStatsScreen(
                        viewModel = featureViewModels.readingStats,
                        modifier = contentModifier,
                        onSessionExpired = featureViewModels.onSignIn,
                        onEditPreferences = featureViewModels.readingPreferences?.let { preferencesViewModel ->
                            {
                                preferencesViewModel.beginEdit()
                                selectedSurface = AppSurface.PREFERENCES.name
                            }
                        },
                    )
                }

                selectedDestination == AppDestination.HOME -> {
                    HomePlaceholder(
                        modifier = contentModifier,
                        onExploreSeries = {
                            if (featureViewModels != null) {
                                selectedSurface = AppSurface.SERIES_DISCOVERY.name
                            }
                        },
                        onExploreTinder = if (featureViewModels?.bookTinder != null) {
                            { selectedSurface = AppSurface.BOOK_TINDER.name }
                        } else null,
                        onExploreChallenges = if (featureViewModels?.readingChallenges != null) {
                            { selectedSurface = AppSurface.READING_CHALLENGES.name }
                        } else null,
                        onShowComponents = {
                            selectedSurface = AppSurface.COMPONENTS_SHOWCASE.name
                        },
                    )
                }

                else -> DestinationPlaceholder(
                    selectedDestination,
                    contentModifier,
                )
        }
    }
}

private enum class AppSurface {
    MAIN,
    SERIES_DISCOVERY,
    SERIES_READING,
    BOOK_TINDER,
    READING_CHALLENGES,
    PREFERENCES,
    READING_STATS,
    COMPONENTS_SHOWCASE,
}

@Composable
private fun HomePlaceholder(
    modifier: Modifier = Modifier,
    onExploreSeries: () -> Unit,
    onExploreTinder: (() -> Unit)?,
    onExploreChallenges: (() -> Unit)?,
    onShowComponents: () -> Unit,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Home,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Inicio",
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "Pantalla en preparación",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onExploreSeries,
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Text("Explorar sagas")
        }
        onExploreTinder?.let { openTinder ->
            Button(
                onClick = openTinder,
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Text("Tinder de libros")
            }
        }
        onExploreChallenges?.let { openChallenges ->
            Button(
                onClick = openChallenges,
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Text("Retos de lectura")
            }
        }
        Button(
            onClick = onShowComponents,
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Text("Ver componentes")
        }
    }
}

@Composable
private fun DestinationPlaceholder(
    destination: AppDestination,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = destination.icon(),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = destination.label,
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "Pantalla en preparación",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun AppDestination.icon(): ImageVector = when (this) {
    AppDestination.HOME -> Icons.Default.Home
    AppDestination.SEARCH -> Icons.Default.Search
    AppDestination.LIBRARY -> Icons.AutoMirrored.Filled.MenuBook
    AppDestination.COMMUNITY -> Icons.Default.Groups
    AppDestination.PROFILE -> Icons.Default.AccountCircle
}

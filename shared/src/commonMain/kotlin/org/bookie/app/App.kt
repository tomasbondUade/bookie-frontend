package org.bookie.app

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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
import org.bookie.app.presentation.library.LibraryScreen
import org.bookie.app.presentation.library.LibraryContentState
import org.bookie.app.presentation.library.LibraryNotConnectedScreen
import org.bookie.app.presentation.components.BookieComponentsScreen
import org.bookie.app.presentation.lists.BookListsScreen
import org.bookie.app.presentation.lists.LibraryWorkspaceScreen
import org.bookie.app.presentation.navigation.AppDestination
import org.bookie.app.presentation.navigation.AppFeatureViewModels
import org.bookie.app.presentation.profile.ReadingStatsScreen
import org.bookie.app.presentation.progress.ReadingProgressScreen
import org.bookie.app.presentation.preferences.ReadingPreferencesScreen
import org.bookie.app.presentation.search.DiscoverScreen
import org.bookie.app.presentation.search.SearchUiState
import org.bookie.app.presentation.search.MoodSearchUiState
import org.bookie.app.presentation.series.SeriesExplorerScreen
import org.bookie.app.presentation.series.SeriesReadingScreen
import org.bookie.app.presentation.tinder.BookTinderScreen
import org.bookie.app.domain.model.LibraryStateInput
import org.bookie.app.theme.BookieTheme
import org.bookie.app.presentation.components.BookieSplash
import org.bookie.app.presentation.home.BookieHomeScreen

@Composable
@Preview
fun App(featureViewModels: AppFeatureViewModels? = null) {
    BookieTheme {
        var opening by rememberSaveable { mutableStateOf(true) }
        val authViewModel = featureViewModels?.auth
        val authState = authViewModel?.uiState?.collectAsStateWithLifecycle()?.value
        LaunchedEffect(authViewModel) { authViewModel?.restore() }
        Crossfade(targetState = opening, animationSpec = tween(320), label = "Bookie opening") { splash ->
            if (splash) BookieSplash(onFinished = { opening = false })
            else MainAppContent(featureViewModels, authState)
        }
    }
}

@Composable
private fun MainAppContent(
    featureViewModels: AppFeatureViewModels?,
    authState: AuthUiState?,
) {
    var selectedRoute by rememberSaveable {
            mutableStateOf(AppDestination.HOME.route)
        }
    var selectedSurface by rememberSaveable {
            mutableStateOf(AppSurface.MAIN.name)
        }
    var returnRoute by rememberSaveable { mutableStateOf(AppDestination.HOME.route) }
    var returnSurface by rememberSaveable { mutableStateOf(AppSurface.MAIN.name) }
    var selectedProgressEntryId by rememberSaveable { mutableStateOf<String?>(null) }
    var hadSession by rememberSaveable { mutableStateOf(false) }
    val authViewModel = featureViewModels?.auth
    val preferencesViewModel = featureViewModels?.readingPreferences

    LaunchedEffect(authState, preferencesViewModel) {
        val userId = (authState as? AuthUiState.SignedIn)?.userId
        if (preferencesViewModel != null) {
            if (userId == null) preferencesViewModel.clearForSignedOut()
            else preferencesViewModel.loadForAccount(userId)
        }
        if (userId != null) {
            hadSession = true
            if (selectedSurface == AppSurface.AUTH.name) {
                selectedRoute = returnRoute
                selectedSurface = returnSurface
            }
        } else if (hadSession && authState is AuthUiState.Form) {
            hadSession = false
            selectedRoute = AppDestination.HOME.route
            selectedSurface = AppSurface.MAIN.name
        }
    }
    val requestSignIn: () -> Unit = {
        if (authViewModel != null) {
            returnRoute = selectedRoute
            returnSurface = if (selectedSurface == AppSurface.AUTH.name) {
                AppSurface.MAIN.name
            } else selectedSurface
            selectedSurface = AppSurface.AUTH.name
        }
    }
    // Both Biblioteca and Buscar use the same authenticated client and session.
    // The public-only fallback is retained when Supabase is not configured.
    val configuredLibrary = if (featureViewModels == null) {
        rememberConfiguredLibraryExperience()
    } else null
    val authenticatedMoodSearch = if (
        featureViewModels?.moodSearch == null && configuredLibrary != null
    ) {
        remember(configuredLibrary) { configuredLibrary.moodSearchViewModel() }
    } else null
    val fallbackMoodSearch = if (
        featureViewModels?.moodSearch == null && configuredLibrary == null
    ) {
        rememberConfiguredMoodSearchViewModel()
    } else null
    val moodSearchViewModel =
        featureViewModels?.moodSearch ?: authenticatedMoodSearch ?: fallbackMoodSearch
    val searchViewModel = featureViewModels?.bookSearch
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
    val searchState = searchViewModel?.uiState?.collectAsStateWithLifecycle()?.value
    val moodState = moodSearchViewModel?.uiState?.collectAsStateWithLifecycle()?.value
    val bookDetailOpen = selectedDestination == AppDestination.SEARCH && (
        (searchState as? SearchUiState.Success)?.selectedBook != null ||
            (moodState as? MoodSearchUiState.Success)?.selectedBook != null
    )
    val libraryContent = featureViewModels?.libraryExperience?.uiState
        ?.collectAsStateWithLifecycle()?.value?.content
    val libraryAction = featureViewModels?.libraryExperience?.uiState?.value?.action
    val progressEntry = when (libraryContent) {
        is LibraryContentState.Ready -> libraryContent.snapshot.entries
        is LibraryContentState.Loading -> libraryContent.previous?.entries.orEmpty()
        else -> emptyList()
    }.firstOrNull { it.id == selectedProgressEntryId }
    LaunchedEffect(selectedSurface, selectedProgressEntryId, libraryContent) {
        if (selectedSurface == AppSurface.READING_PROGRESS.name &&
            selectedProgressEntryId != null && libraryContent == LibraryContentState.Idle
        ) {
            featureViewModels?.libraryExperience?.refresh()
        }
    }

    val canHandleSystemBack = surface != AppSurface.MAIN ||
        selectedDestination != AppDestination.HOME || bookDetailOpen
    SystemBackHandler(enabled = canHandleSystemBack) {
        when {
            surface == AppSurface.AUTH -> {
                selectedRoute = returnRoute
                selectedSurface = returnSurface
            }
            surface == AppSurface.SERIES_READING -> {
                selectedSurface = AppSurface.SERIES_DISCOVERY.name
            }
            surface == AppSurface.SERIES_DISCOVERY ||
                surface == AppSurface.BOOK_TINDER ||
                surface == AppSurface.READING_CHALLENGES ||
                surface == AppSurface.COMPONENTS_SHOWCASE -> {
                selectedSurface = AppSurface.MAIN.name
            }
            surface == AppSurface.READING_PROGRESS -> {
                featureViewModels?.libraryExperience?.refresh()
                selectedProgressEntryId = null
                selectedSurface = AppSurface.MAIN.name
            }
            surface == AppSurface.PREFERENCES -> {
                selectedRoute = AppDestination.PROFILE.route
                selectedSurface = AppSurface.MAIN.name
            }
            bookDetailOpen -> {
                searchViewModel?.showResults()
                moodSearchViewModel?.showResults()
            }
            selectedDestination != AppDestination.HOME -> {
                selectedRoute = AppDestination.HOME.route
            }
        }
    }

    Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (surface == AppSurface.MAIN && !bookDetailOpen) NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                ) {
                    listOf(AppDestination.HOME, AppDestination.LIBRARY, AppDestination.SEARCH, AppDestination.COMMUNITY, AppDestination.PROFILE).forEach { destination ->
                        NavigationBarItem(
                            selected = selectedDestination == destination,
                            onClick = {
                                if (destination == AppDestination.LIBRARY ||
                                    destination == AppDestination.PROFILE
                                ) {
                                    val userId = (authState as? AuthUiState.SignedIn)?.userId
                                    if (userId == null && authViewModel != null) {
                                        returnRoute = destination.route
                                        returnSurface = AppSurface.MAIN.name
                                        selectedSurface = AppSurface.AUTH.name
                                        return@NavigationBarItem
                                    }
                                }
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
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = .16f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            },
    ) { contentPadding ->
        val contentModifier =
                Modifier.fillMaxSize().padding(contentPadding)

        when {
                surface == AppSurface.AUTH -> {
                    if (authViewModel != null) {
                        AuthScreen(
                            viewModel = authViewModel,
                            modifier = contentModifier,
                            onBack = {
                                selectedSurface = returnSurface
                                selectedRoute = returnRoute
                            },
                        )
                    } else {
                        Column(modifier = contentModifier.padding(24.dp)) {
                            Text("Autenticación no configurada", style = MaterialTheme.typography.headlineSmall)
                            Text("Faltan la URL pública y la clave publicable de Supabase en la configuración local de la app.")
                        }
                    }
                }

                surface == AppSurface.COMPONENTS_SHOWCASE -> {
                    BookieComponentsScreen(
                        modifier = contentModifier,
                    )
                }

                surface == AppSurface.BOOK_TINDER && featureViewModels?.bookTinder != null -> {
                    BookTinderScreen(
                        viewModel = featureViewModels.bookTinder,
                        onSignIn = requestSignIn,
                        onBack = { selectedSurface = AppSurface.MAIN.name },
                        modifier = contentModifier,
                    )
                }

                surface == AppSurface.READING_CHALLENGES &&
                        featureViewModels?.readingChallenges != null -> {
                    ReadingChallengesScreen(
                        viewModel = featureViewModels.readingChallenges,
                        onSignIn = requestSignIn,
                        onBack = { selectedSurface = AppSurface.MAIN.name },
                        modifier = contentModifier,
                    )
                }

                surface == AppSurface.READING_PROGRESS -> {
                    val factory = featureViewModels?.createReadingProgressViewModel
                    if (progressEntry != null && factory != null) {
                        val progressViewModel = remember(progressEntry.id, factory) {
                            factory(progressEntry)
                        }
                        ReadingProgressScreen(
                            viewModel = progressViewModel,
                            modifier = contentModifier,
                            onBack = {
                                featureViewModels?.libraryExperience?.refresh()
                                selectedProgressEntryId = null
                                selectedSurface = AppSurface.MAIN.name
                            },
                        )
                    } else {
                        Column(modifier = contentModifier.padding(24.dp)) {
                            CircularProgressIndicator()
                            Text("Cargando el progreso de lectura...")
                            androidx.compose.material3.TextButton(onClick = {
                                selectedProgressEntryId = null
                                selectedSurface = AppSurface.MAIN.name
                            }) { Text("Volver a biblioteca") }
                        }
                    }
                }

                surface == AppSurface.SERIES_DISCOVERY &&
                        featureViewModels != null -> {
                    SeriesExplorerScreen(
                        viewModel = featureViewModels.seriesExplorer,
                        onOpenSeries = { seriesId ->
                            featureViewModels.seriesReading.selectSeries(seriesId)
                            selectedSurface = AppSurface.SERIES_READING.name
                        },
                        onSignIn = requestSignIn,
                        modifier = contentModifier,
                    )
                }

                surface == AppSurface.SERIES_READING &&
                        featureViewModels != null -> {
                    SeriesReadingScreen(
                        viewModel = featureViewModels.seriesReading,
                        onOpenBook = featureViewModels.onOpenBook,
                        onSignIn = requestSignIn,
                        onBack = {
                            selectedSurface = AppSurface.SERIES_DISCOVERY.name
                        },
                        modifier = contentModifier,
                    )
                }

                selectedDestination == AppDestination.SEARCH -> {
                    if (moodSearchViewModel != null && searchViewModel != null) {
                        DiscoverScreen(
                            searchViewModel = searchViewModel,
                            moodViewModel = moodSearchViewModel,
                            tinderViewModel = featureViewModels?.bookTinder,
                            modifier = contentModifier,
                            reviewsViewModel = bookReviewsViewModel,
                            onSignIn = requestSignIn,
                            onSaveBook = { book ->
                                if (authState is AuthUiState.SignedIn) {
                                    featureViewModels?.libraryExperience?.addCatalogBook(book.id)
                                } else requestSignIn()
                            },
                            onRateBook = { book, rating ->
                                if (authState is AuthUiState.SignedIn) {
                                    featureViewModels?.libraryExperience?.save(
                                        LibraryStateInput(bookId = book.id, rating = rating),
                                        "Calificación guardada.",
                                    )
                                } else requestSignIn()
                            },
                            libraryAction = libraryAction,
                        )
                    } else {
                        Column(modifier = contentModifier.padding(24.dp)) {
                            Text("Explorar por estado de ánimo", style = MaterialTheme.typography.headlineSmall)
                            Text("Falta configurar la URL del backend Bookie (bookie_api_base_url).")
                        }
                    }
                }

                selectedDestination == AppDestination.LIBRARY -> {
                    val libraryViewModel = featureViewModels?.libraryExperience
                    val listsViewModel = featureViewModels?.bookLists
                    when {
                        libraryViewModel != null && listsViewModel != null -> LibraryWorkspaceScreen(
                            libraryViewModel = libraryViewModel,
                            listsViewModel = listsViewModel,
                            onSignIn = requestSignIn,
                            modifier = contentModifier,
                            onOpenProgress = { entry ->
                                selectedProgressEntryId = entry.id
                                selectedSurface = AppSurface.READING_PROGRESS.name
                            },
                        )
                        listsViewModel != null -> BookListsScreen(
                            viewModel = listsViewModel,
                            onSignIn = requestSignIn,
                            modifier = contentModifier,
                        )
                        libraryViewModel != null -> LibraryScreen(
                            viewModel = libraryViewModel,
                            onExploreBooks = {
                                selectedRoute = AppDestination.SEARCH.route
                                selectedSurface = AppSurface.MAIN.name
                            },
                            modifier = contentModifier,
                        )
                        configuredLibrary != null -> {
                            val authState by configuredLibrary.authViewModel.uiState
                                .collectAsStateWithLifecycle()
                            val signedIn = authState as? AuthUiState.SignedIn
                            if (signedIn != null) {
                                // A fresh ViewModel per user prevents leaking previous
                                // user's visible items across account switches.
                                val accountLibrary = remember(configuredLibrary, signedIn.userId) {
                                    configuredLibrary.libraryViewModel()
                                }
                                LibraryScreen(
                                    viewModel = accountLibrary,
                                    onExploreBooks = {
                                        selectedRoute = AppDestination.SEARCH.route
                                        selectedSurface = AppSurface.MAIN.name
                                    },
                                    modifier = contentModifier,
                                )
                            } else {
                                AuthScreen(configuredLibrary.authViewModel, contentModifier)
                            }
                        }
                        else -> LibraryNotConnectedScreen(
                            onSignIn = requestSignIn.takeIf { authViewModel != null },
                            modifier = contentModifier,
                        )
                    }
                }

                selectedDestination == AppDestination.COMMUNITY -> {
                    if (communityViewModel != null) {
                        CommunityScreen(
                            viewModel = communityViewModel,
                            compatibilityViewModel = featureViewModels?.readingCompatibility,
                            postsViewModelFactory = featureViewModels?.createCommunityPostsViewModel,
                            currentUserId = (authState as? AuthUiState.SignedIn)?.userId,
                            onSignIn = requestSignIn.takeIf { authViewModel != null },
                            onBack = { selectedRoute = AppDestination.HOME.route },
                            modifier = contentModifier,
                        )
                    } else {
                        Column(modifier = contentModifier.padding(24.dp)) {
                            Text("Comunidades", style = MaterialTheme.typography.headlineSmall)
                            Text("Falta configurar la URL del backend Bookie (bookie_api_base_url).")
                        }
                    }
                }

                surface == AppSurface.PREFERENCES &&
                        featureViewModels?.readingPreferences != null -> {
                    ReadingPreferencesScreen(
                        viewModel = featureViewModels.readingPreferences,
                        modifier = contentModifier,
                        onClose = {
                            selectedRoute = AppDestination.PROFILE.route
                            selectedSurface = AppSurface.MAIN.name
                        },
                        onSignIn = requestSignIn,
                    )
                }

                selectedDestination == AppDestination.PROFILE &&
                        featureViewModels != null -> {
                    if (authState !is AuthUiState.SignedIn && authViewModel != null) {
                        AuthScreen(authViewModel, contentModifier)
                    } else ReadingStatsScreen(
                        viewModel = featureViewModels.readingStats,
                        modifier = contentModifier,
                        onSessionExpired = requestSignIn,
                        onEditPreferences = featureViewModels.readingPreferences?.let { preferencesViewModel ->
                            {
                                preferencesViewModel.beginEdit()
                                selectedSurface = AppSurface.PREFERENCES.name
                            }
                        },
                        onSignOut = authViewModel?.let { viewModel -> ({ viewModel.signOut() }) },
                        onOpenLibrary = { selectedRoute = AppDestination.LIBRARY.route },
                        email = (authState as? AuthUiState.SignedIn)?.email,
                    )
                }

                selectedDestination == AppDestination.HOME -> {
                    BookieHomeScreen(
                        modifier = contentModifier,
                        onSearch = { selectedRoute = AppDestination.SEARCH.route },
                        onCommunity = { selectedRoute = AppDestination.COMMUNITY.route },
                        onLibrary = {
                            val signedIn = authState is AuthUiState.SignedIn
                            if (!signedIn && authViewModel != null) {
                                returnRoute = AppDestination.LIBRARY.route
                                returnSurface = AppSurface.MAIN.name
                                selectedSurface = AppSurface.AUTH.name
                            } else selectedRoute = AppDestination.LIBRARY.route
                        },
                        onExploreSeries = featureViewModels?.let {
                            {
                                selectedSurface = AppSurface.SERIES_DISCOVERY.name
                            }
                        },
                        onExploreTinder = if (featureViewModels?.bookTinder != null) {
                            {
                                if (authState is AuthUiState.SignedIn) selectedSurface = AppSurface.BOOK_TINDER.name
                                else requestSignIn()
                            }
                        } else null,
                        onExploreChallenges = if (featureViewModels?.readingChallenges != null) {
                            {
                                if (authState is AuthUiState.SignedIn) selectedSurface = AppSurface.READING_CHALLENGES.name
                                else requestSignIn()
                            }
                        } else null,
                        isSignedIn = authState is AuthUiState.SignedIn,
                        accountId = (authState as? AuthUiState.SignedIn)?.userId,
                        onSignIn = requestSignIn.takeIf { authViewModel != null },
                        onProfile = {
                            if (authState is AuthUiState.SignedIn || authViewModel == null) selectedRoute = AppDestination.PROFILE.route
                            else requestSignIn()
                        },
                        tinderViewModel = featureViewModels?.bookTinder,
                        libraryViewModel = featureViewModels?.libraryExperience,
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
    AUTH,
    SERIES_DISCOVERY,
    SERIES_READING,
    BOOK_TINDER,
    READING_CHALLENGES,
    READING_PROGRESS,
    PREFERENCES,
    COMPONENTS_SHOWCASE,
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

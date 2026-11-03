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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.bookie.app.presentation.navigation.AppDestination
import org.bookie.app.presentation.navigation.AppFeatureViewModels
import org.bookie.app.presentation.profile.ReadingStatsScreen
import org.bookie.app.presentation.series.SeriesExplorerScreen
import org.bookie.app.presentation.series.SeriesReadingScreen
import org.bookie.app.theme.BookieTheme

@Composable
@Preview
fun App(featureViewModels: AppFeatureViewModels? = null) {
    BookieTheme {
        var selectedRoute by rememberSaveable {
            mutableStateOf(AppDestination.HOME.route)
        }
        var selectedSurface by rememberSaveable {
            mutableStateOf(AppSurface.MAIN.name)
        }
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

                selectedDestination == AppDestination.PROFILE &&
                        featureViewModels != null -> {
                    ReadingStatsScreen(
                        viewModel = featureViewModels.readingStats,
                        modifier = contentModifier,
                        onSessionExpired = featureViewModels.onSignIn,
                    )
                }

                selectedDestination == AppDestination.HOME &&
                        featureViewModels != null -> {
                    HomePlaceholder(
                        modifier = contentModifier,
                        onExploreSeries = {
                            selectedSurface = AppSurface.SERIES_DISCOVERY.name
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
}

private enum class AppSurface {
    MAIN,
    SERIES_DISCOVERY,
    SERIES_READING,
}

@Composable
private fun HomePlaceholder(
    modifier: Modifier = Modifier,
    onExploreSeries: () -> Unit,
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

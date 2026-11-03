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
import org.bookie.app.theme.BookieTheme

@Composable
@Preview
fun App() {
    BookieTheme {
        var selectedRoute by rememberSaveable {
            mutableStateOf(AppDestination.HOME.route)
        }
        val selectedDestination =
            AppDestination.fromRoute(selectedRoute) ?: AppDestination.HOME

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar {
                    AppDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = selectedDestination == destination,
                            onClick = {
                                selectedRoute = destination.route
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
            DestinationPlaceholder(
                destination = selectedDestination,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
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

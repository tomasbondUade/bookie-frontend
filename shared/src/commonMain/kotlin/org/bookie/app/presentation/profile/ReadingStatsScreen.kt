package org.bookie.app.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bookie.app.domain.model.ReadingStats

@Composable
fun ReadingStatsScreen(
    viewModel: ReadingStatsViewModel,
    modifier: Modifier = Modifier,
    onSessionExpired: (() -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.load()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Estadísticas de lectura", style = MaterialTheme.typography.headlineSmall)

        when (val currentState = state) {
            ReadingStatsUiState.Idle,
            ReadingStatsUiState.Loading -> Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator()
                Text("Cargando estadísticas…")
            }

            ReadingStatsUiState.Empty -> Text(
                "Todavía no hay estadísticas. Marcá un libro como leído para empezar.",
                style = MaterialTheme.typography.bodyLarge,
            )

            is ReadingStatsUiState.Error -> Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    currentState.message,
                    color = MaterialTheme.colorScheme.error,
                )
                if (currentState.sessionExpired) {
                    onSessionExpired?.let { onSignIn ->
                        Button(onClick = onSignIn) {
                            Text("Iniciar sesión")
                        }
                    }
                } else {
                    Button(onClick = viewModel::retry) {
                        Text("Reintentar")
                    }
                }
            }

            is ReadingStatsUiState.Success -> ReadingStatsContent(currentState.stats)
        }
    }
}

@Composable
private fun ReadingStatsContent(stats: ReadingStats) {
    StatsSection("Géneros más leídos") {
        if (stats.mostReadGenres.isEmpty()) {
            NoStatsData()
        } else {
            stats.mostReadGenres.forEach { item ->
                Text("${item.genre} · ${item.count}")
            }
        }
    }

    StatsSection("Autores favoritos") {
        if (stats.favoriteAuthors.isEmpty()) {
            NoStatsData()
        } else {
            stats.favoriteAuthors.forEach { item ->
                Text("${item.author} · ${item.count}")
            }
        }
    }

    StatsSection("Calificaciones") {
        val rating = stats.averageRating
        if (rating.count == 0) {
            NoStatsData()
        } else {
            val label = if (rating.count == 1) "calificación" else "calificaciones"
            Text("${rating.average} / 5 · ${rating.count} $label")
        }
    }

    StatsSection("Libros terminados por mes") {
        if (stats.booksReadByMonth.isEmpty()) {
            NoStatsData()
        } else {
            stats.booksReadByMonth.forEach { item ->
                val month = item.month.toString().padStart(2, '0')
                Text("$month/${item.year} · ${item.count}")
            }
        }
    }

    StatsSection("Libros terminados por año") {
        if (stats.booksReadByYear.isEmpty()) {
            NoStatsData()
        } else {
            stats.booksReadByYear.forEach { item ->
                Text("${item.year} · ${item.count}")
            }
        }
    }

    StatsSection("Racha de lectura") {
        val streak = stats.readingStreak
        Text("Racha actual: ${streak.currentStreak} días")
        Text("Racha más larga: ${streak.longestStreak} días")
    }
}

@Composable
private fun StatsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun NoStatsData() {
    Text("Sin datos todavía.", style = MaterialTheme.typography.bodyMedium)
}

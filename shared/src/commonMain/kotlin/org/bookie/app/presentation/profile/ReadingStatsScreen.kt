package org.bookie.app.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import org.bookie.app.presentation.components.BookieActionButton as Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import org.bookie.app.presentation.components.BookiePageHeading
import org.bookie.app.presentation.components.BookieFeatureBanner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bookie.app.domain.model.ReadingStats
import org.bookie.app.presentation.components.*

@Composable
fun ReadingStatsScreen(
    viewModel: ReadingStatsViewModel,
    modifier: Modifier = Modifier,
    onSessionExpired: (() -> Unit)? = null,
    onEditPreferences: (() -> Unit)? = null,
    onSignOut: (() -> Unit)? = null,
    onOpenLibrary: (() -> Unit)? = null,
    email: String? = null,
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
        BookiePageHeading("Mi perfil", "Tu historia también se escribe leyendo.")
        BookieProfileHeader(email)
        onEditPreferences?.let { onEdit ->
            Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("Editar preferencias") }
        }
        onOpenLibrary?.let { action ->
            Button(onClick = action, modifier = Modifier.fillMaxWidth()) { Text("Ver mi biblioteca") }
        }
        Text("Mi actividad lectora", style = MaterialTheme.typography.titleMedium)
        when (val currentState = state) {
            ReadingStatsUiState.Idle,
            ReadingStatsUiState.Loading -> Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator()
                Text("Cargando estadísticas…")
            }

            ReadingStatsUiState.Empty -> BookieEmptyState(
                message = "Tu historia lectora empieza acá. Marcá un libro como leído para ver tu actividad.")

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
        onSignOut?.let { signOut ->
            androidx.compose.material3.TextButton(onClick = signOut) { Text("Cerrar sesión") }
        }
    }
}

@Composable
private fun ReadingStatsContent(stats: ReadingStats) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        BookieMetric(stats.booksReadByYear.sumOf { it.count }.toString(), "Leídos", Modifier.weight(1f))
        BookieMetric(if (stats.averageRating.count > 0) "${kotlin.math.round(stats.averageRating.average * 10) / 10} ★" else "—", "Promedio", Modifier.weight(1f))
        BookieMetric(stats.readingStreak.currentStreak.toString(), "Días de racha", Modifier.weight(1f))
    }
    StatsSection("Géneros más leídos") {
        if (stats.mostReadGenres.isEmpty()) {
            NoStatsData()
        } else {
            stats.mostReadGenres.forEach { item ->
                StatsBar(item.genre, item.count, stats.mostReadGenres.maxOf { it.count })
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
                StatsBar("$month/${item.year}", item.count, stats.booksReadByMonth.maxOf { it.count })
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
    BookiePanelCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun NoStatsData() {
    Text("Sin datos todavía.", style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun BookieProfileHeader(email: String?) {
    BookiePanelCard(Modifier.fillMaxWidth()) {
        BookieFeatureBanner("Un mundo entre páginas", "Lee. Conecta. Recomienda.")
        Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            BookieAvatar(email ?: "Bookie")
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(email?.substringBefore('@') ?: "Tu perfil lector", style = MaterialTheme.typography.titleLarge)
                Text(email ?: "Tu espacio en Bookie", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StatsBar(label: String, count: Int, max: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text("$count", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        androidx.compose.material3.LinearProgressIndicator(progress = { count.toFloat() / max.coerceAtLeast(1) },
            modifier = Modifier.fillMaxWidth())
    }
}

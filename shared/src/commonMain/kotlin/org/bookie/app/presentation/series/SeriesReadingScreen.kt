package org.bookie.app.presentation.series

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bookie.app.domain.model.*

@Composable
fun SeriesReadingScreen(
    viewModel: SeriesReadingViewModel,
    onOpenBook: (String) -> Unit,
    onSignIn: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.loadActive() }

    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Sagas y órdenes de lectura", style = MaterialTheme.typography.headlineSmall)
        if (state.selectedSeriesId == null) {
            Text("Mis sagas activas", style = MaterialTheme.typography.titleMedium)
            when (val active = state.active) {
                SeriesLoad.Idle, SeriesLoad.Loading -> CircularProgressIndicator()
                is SeriesLoad.Failed -> Failure(active, onRetry = viewModel::loadActive, onSignIn = onSignIn)
                is SeriesLoad.Ready -> {
                    if (active.value.isEmpty()) Text("No tenés sagas activas. Empezá a leer un libro de una saga para verla acá.")
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(active.value, key = SeriesProgress::seriesId) { series ->
                            Card(
                                onClick = { viewModel.selectSeries(series.seriesId) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(series.title, style = MaterialTheme.typography.titleMedium)
                                    Text("${series.readBooks} de ${series.totalBooks} libros principales leídos")
                                    Text("Completado: ${series.completionPercentage}%")
                                }
                            }
                        }
                    }
                }
            }
        } else {
            TextButton(onClick = viewModel::clearSelection) { Text("Volver a mis sagas") }
            when (val progress = state.progress) {
                SeriesLoad.Idle, SeriesLoad.Loading -> CircularProgressIndicator()
                is SeriesLoad.Failed -> Failure(progress, viewModel::loadProgress, onSignIn)
                is SeriesLoad.Ready -> {
                    Text(progress.value.title, style = MaterialTheme.typography.titleLarge)
                    Text("${progress.value.readBooks} / ${progress.value.totalBooks} libros principales")
                    Text("Progreso de saga: ${progress.value.completionPercentage}%")
                    if (progress.value.nodes.isEmpty()) Text("La saga todavía no tiene libros configurados.")
                }
            }
            HorizontalDivider()
            Text("Orden de lectura", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ReadingPath.entries.forEach { path ->
                    TextButton(
                        onClick = { viewModel.changePath(path) },
                        enabled = state.selectedPath != path,
                    ) { Text(path.displayName()) }
                }
            }
            when (val map = state.map) {
                SeriesLoad.Idle, SeriesLoad.Loading -> CircularProgressIndicator()
                is SeriesLoad.Failed -> Failure(map, viewModel::loadMap, onSignIn)
                is SeriesLoad.Ready -> {
                    if (map.value.nodes.isEmpty()) Text("No hay un recorrido disponible para este orden.")
                    val edges = map.value.edges
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(map.value.nodes, key = SeriesMapNode::nodeId) { node ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(node.title, style = MaterialTheme.typography.titleMedium)
                                    Text(node.status?.displayName() ?: "Sin progreso registrado")
                                    node.progressPercentage?.let { Text("Avance: $it%") }
                                    node.currentPage?.let { Text("Página actual: $it") }
                                    Text(node.nodeType, style = MaterialTheme.typography.bodySmall)
                                    val prerequisites = edges.filter { it.targetNodeId == node.nodeId }
                                    if (prerequisites.isNotEmpty()) {
                                        val titles = map.value.nodes.associateBy { it.nodeId }
                                        Text(
                                            "Depende de: " + prerequisites.joinToString {
                                                titles[it.sourceNodeId]?.title ?: it.sourceNodeId
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                    Button(onClick = { onOpenBook(node.bookId) }) { Text("Ver libro") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Failure(
    error: SeriesLoad.Failed,
    onRetry: () -> Unit,
    onSignIn: (() -> Unit)?,
) {
    Text(error.message, color = MaterialTheme.colorScheme.error)
    if (error.signInRequired) {
        onSignIn?.let { Button(onClick = it) { Text("Iniciar sesión") } }
    } else {
        Button(onClick = onRetry) { Text("Reintentar") }
    }
}

private fun ReadingPath.displayName(): String = when (this) {
    ReadingPath.RECOMMENDED -> "Recomendado"
    ReadingPath.PUBLICATION -> "Publicación"
    ReadingPath.CHRONOLOGICAL -> "Cronológico"
}

private fun SeriesNodeStatus.displayName(): String = when (this) {
    SeriesNodeStatus.COMPLETED -> "Leído"
    SeriesNodeStatus.IN_PROGRESS -> "En lectura"
    SeriesNodeStatus.UP_NEXT -> "Siguiente recomendado"
    SeriesNodeStatus.LOCKED -> "Bloqueado"
    SeriesNodeStatus.PENDING -> "Pendiente"
}

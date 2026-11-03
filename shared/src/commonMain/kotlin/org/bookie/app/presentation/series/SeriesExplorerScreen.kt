package org.bookie.app.presentation.series

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bookie.app.domain.model.SeriesCatalogEntry
import org.bookie.app.domain.model.UniverseCatalogEntry

@Composable
fun SeriesExplorerScreen(
    viewModel: SeriesExplorerViewModel,
    onOpenSeries: (String) -> Unit,
    onSignIn: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.load() }

    Column(
        modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Explorar sagas", style = MaterialTheme.typography.headlineSmall)
        if (state.selectedUniverseId != null) {
            TextButton(onClick = viewModel::clearUniverseSelection) { Text("Volver a universos") }
            UniverseDetailContent(
                load = state.universeDetail,
                onRetry = viewModel::retryUniverseDetail,
                onSignIn = onSignIn,
                onOpenSeries = onOpenSeries,
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { viewModel.selectTab(SeriesExplorerTab.SERIES) },
                    enabled = state.tab != SeriesExplorerTab.SERIES,
                ) { Text("Sagas") }
                TextButton(
                    onClick = { viewModel.selectTab(SeriesExplorerTab.UNIVERSES) },
                    enabled = state.tab != SeriesExplorerTab.UNIVERSES,
                ) { Text("Universos") }
            }
            when (state.tab) {
                SeriesExplorerTab.SERIES -> {
                    UniverseFilter(
                        universes = state.universes,
                        selectedId = state.universeFilterId,
                        onSelect = viewModel::setUniverseFilter,
                    )
                    SeriesCatalogContent(
                        load = state.series,
                        page = state.page,
                        onRetry = { viewModel.loadSeries() },
                        onPageChange = viewModel::loadSeries,
                        onOpenSeries = onOpenSeries,
                        onSignIn = onSignIn,
                    )
                }
                SeriesExplorerTab.UNIVERSES -> UniverseCatalogContent(
                    load = state.universes,
                    onRetry = viewModel::loadUniverses,
                    onSelect = viewModel::selectUniverse,
                    onSignIn = onSignIn,
                )
            }
        }
    }
}

@Composable
private fun UniverseFilter(
    universes: CatalogLoad<List<UniverseCatalogEntry>>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
) {
    val options = (universes as? CatalogLoad.Ready)?.value.orEmpty()
    var expanded by remember { mutableStateOf(false) }
    val selectedName = options.firstOrNull { it.id == selectedId }?.name ?: "Todos los universos"

    Box {
        OutlinedButton(onClick = { expanded = true }, enabled = options.isNotEmpty()) {
            Text("Universo: $selectedName")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Todos los universos") },
                onClick = { onSelect(null); expanded = false },
            )
            options.forEach { universe ->
                DropdownMenuItem(
                    text = { Text(universe.name) },
                    onClick = { onSelect(universe.id); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.SeriesCatalogContent(
    load: CatalogLoad<org.bookie.app.domain.model.SeriesCatalogPage>,
    page: Int,
    onRetry: () -> Unit,
    onPageChange: (Int) -> Unit,
    onOpenSeries: (String) -> Unit,
    onSignIn: (() -> Unit)?,
) {
    when (load) {
        CatalogLoad.Idle, CatalogLoad.Loading -> CircularProgressIndicator()
        is CatalogLoad.Failed -> ExplorerFailure(load, onRetry, onSignIn)
        is CatalogLoad.Ready -> {
            if (load.value.series.isEmpty()) Text("No hay sagas para mostrar con este filtro.")
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(load.value.series, key = SeriesCatalogEntry::id) { series ->
                    Card(onClick = { onOpenSeries(series.id) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(series.title, style = MaterialTheme.typography.titleMedium)
                            series.universe?.name?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            series.description?.takeIf(String::isNotBlank)?.let { Text(it, maxLines = 3) }
                            Text("${series.nodeCount} libros en el mapa · ${series.totalBooks} libros")
                        }
                    }
                }
            }
            if (load.value.totalPages > 1) {
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = { onPageChange(page - 1) }, enabled = page > 1) {
                        Text("Anterior")
                    }
                    Text("Página $page de ${load.value.totalPages}")
                    TextButton(
                        onClick = { onPageChange(page + 1) },
                        enabled = page < load.value.totalPages,
                    ) { Text("Siguiente") }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.UniverseCatalogContent(
    load: CatalogLoad<List<UniverseCatalogEntry>>,
    onRetry: () -> Unit,
    onSelect: (String) -> Unit,
    onSignIn: (() -> Unit)?,
) {
    when (load) {
        CatalogLoad.Idle, CatalogLoad.Loading -> CircularProgressIndicator()
        is CatalogLoad.Failed -> ExplorerFailure(load, onRetry, onSignIn)
        is CatalogLoad.Ready -> {
            if (load.value.isEmpty()) Text("No hay universos disponibles.")
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(load.value, key = UniverseCatalogEntry::id) { universe ->
                    Card(onClick = { onSelect(universe.id) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(universe.name, style = MaterialTheme.typography.titleMedium)
                            universe.author?.let { Text("Autor: $it") }
                            universe.description?.takeIf(String::isNotBlank)?.let { Text(it, maxLines = 3) }
                            Text("${universe.seriesCount} sagas")
                            universe.progress?.let {
                                Text("${it.readBooks} de ${it.totalBooks} libros leídos · ${it.completionPercentage}%")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.UniverseDetailContent(
    load: CatalogLoad<org.bookie.app.domain.model.UniverseDetail>,
    onRetry: () -> Unit,
    onSignIn: (() -> Unit)?,
    onOpenSeries: (String) -> Unit,
) {
    when (load) {
        CatalogLoad.Idle, CatalogLoad.Loading -> CircularProgressIndicator()
        is CatalogLoad.Failed -> ExplorerFailure(load, onRetry, onSignIn)
        is CatalogLoad.Ready -> {
            val universe = load.value
            Text(universe.name, style = MaterialTheme.typography.titleLarge)
            universe.author?.let { Text("Autor: $it") }
            universe.description?.takeIf(String::isNotBlank)?.let { Text(it) }
            if (universe.series.isEmpty()) Text("Este universo todavía no tiene sagas cargadas.")
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(universe.series, key = SeriesCatalogEntry::id) { series ->
                    Card(onClick = { onOpenSeries(series.id) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(series.title, style = MaterialTheme.typography.titleMedium)
                            series.description?.takeIf(String::isNotBlank)?.let { Text(it, maxLines = 3) }
                            Text("${series.nodeCount} libros en el mapa · ${series.totalBooks} libros")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExplorerFailure(
    error: CatalogLoad.Failed,
    onRetry: () -> Unit,
    onSignIn: (() -> Unit)?,
) {
    Text(error.message, color = MaterialTheme.colorScheme.error)
    if (error.signInRequired) {
        onSignIn?.let { Button(onClick = it) { Text("Iniciar sesión") } }
    } else if (!error.accessDenied) {
        Button(onClick = onRetry) { Text("Reintentar") }
    }
}

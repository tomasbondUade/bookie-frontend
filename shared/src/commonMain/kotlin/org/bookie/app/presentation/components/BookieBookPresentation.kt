package org.bookie.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.bookie.app.domain.model.Book
import org.bookie.app.domain.model.LibraryBookState
import org.bookie.app.domain.model.LibraryStatus

@Composable
fun BookieCatalogCard(book: Book, onClick: () -> Unit, modifier: Modifier = Modifier) {
    BookiePanelCard(modifier.fillMaxWidth(), onClick) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            BookCover(book, Modifier.size(72.dp, 106.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(book.title, style = MaterialTheme.typography.titleMedium, maxLines = 3,
                    overflow = TextOverflow.Ellipsis)
                Text(book.authors.joinToString().ifBlank { "Autor desconocido" },
                    style = MaterialTheme.typography.bodySmall, maxLines = 2,
                    overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (book.genres.isNotEmpty()) Text(book.genres.take(3).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall, maxLines = 2,
                    overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.primary)
                Text("Ver libro →", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
fun BookieBookHero(book: Book, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Brush.verticalGradient(
        listOf(MaterialTheme.colorScheme.primary.copy(alpha = .2f), MaterialTheme.colorScheme.surface)))) {
        Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BookCover(book, Modifier.size(136.dp, 196.dp))
            Text("ENTRE PÁGINAS", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun BookieLibraryEntryCard(entry: LibraryBookState, onFavorite: () -> Unit, onEdit: () -> Unit,
    onRemove: () -> Unit, onOpenProgress: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    BookiePanelCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                BookCover(entry.book, Modifier.size(70.dp, 104.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(entry.book.title, style = MaterialTheme.typography.titleMedium)
                    Text(entry.book.authors.joinToString().ifBlank { "Autor desconocido" },
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(when (entry.status) {
                        LibraryStatus.TO_READ -> "Por leer"
                        LibraryStatus.READING -> "Leyendo"
                        LibraryStatus.READ -> "Leído"
                    }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    entry.rating?.let { Text("★ $it / 5", color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.labelMedium) }
                    if (entry.isAbandoned) Text("Abandonado", style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = onFavorite) {
                    Icon(if (entry.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (entry.isFavorite) "Quitar favorito" else "Marcar favorito",
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
            if (entry.progressPercentage != null || entry.currentPage != null) {
                Text(listOfNotNull(entry.progressPercentage?.let { "$it%" }, entry.currentPage?.let { "Página $it" })
                    .joinToString(" · "), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                entry.progressPercentage?.let { percentage ->
                    LinearProgressIndicator(progress = { percentage.coerceIn(0, 100) / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(8.dp)))
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (entry.status == LibraryStatus.READING && onOpenProgress != null) {
                    BookieActionButton(onClick = onOpenProgress) { Text("Registrar progreso") }
                }
                OutlinedButton(onClick = onEdit) { Text("Editar") }
                TextButton(onClick = onRemove) { Text("Quitar de biblioteca") }
            }
        }
    }
}

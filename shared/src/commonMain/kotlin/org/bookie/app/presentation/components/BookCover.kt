package org.bookie.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.bookie.app.domain.model.Book

@Composable
fun BookCover(book: Book, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier.clip(shape).background(Brush.verticalGradient(listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = .22f), MaterialTheme.colorScheme.surfaceVariant)))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .25f), shape),
        contentAlignment = Alignment.Center,
    ) {
        val rawUrl = book.coverUrl?.trim()
        val imageUrl = when {
            rawUrl == null -> null
            rawUrl.startsWith("https://", ignoreCase = true) -> rawUrl
            rawUrl.startsWith("http://books.google.com/", ignoreCase = true) ->
                "https://${rawUrl.substringAfter("://")}"
            else -> null
        }
        // Keep a visible fallback under network images while loading or after a failure.
            Icon(
                Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = if (imageUrl == null) "Sin portada" else null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Portada de ${book.title}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

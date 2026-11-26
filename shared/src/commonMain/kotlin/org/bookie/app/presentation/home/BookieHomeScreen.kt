package org.bookie.app.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bookie.app.domain.model.Book
import org.bookie.app.domain.model.LibraryStatus
import org.bookie.app.presentation.components.BookCover
import org.bookie.app.presentation.components.BookieGradient
import org.bookie.app.presentation.components.BookieGradientButton
import org.bookie.app.presentation.components.BookieLibraryBackdrop
import org.bookie.app.presentation.components.BookieMark
import org.bookie.app.presentation.library.LibraryContentState
import org.bookie.app.presentation.library.LibraryExperienceViewModel
import org.bookie.app.presentation.tinder.BookTinderUiState
import org.bookie.app.presentation.tinder.BookTinderViewModel

@Composable
fun BookieHomeScreen(
    modifier: Modifier = Modifier,
    onSearch: () -> Unit,
    onCommunity: () -> Unit,
    onLibrary: () -> Unit,
    onExploreSeries: (() -> Unit)?,
    onExploreTinder: (() -> Unit)?,
    onExploreChallenges: (() -> Unit)?,
    isSignedIn: Boolean,
    accountId: String?,
    onSignIn: (() -> Unit)?,
    onProfile: () -> Unit,
    tinderViewModel: BookTinderViewModel?,
    libraryViewModel: LibraryExperienceViewModel?,
) {
    val feed = tinderViewModel?.uiState?.collectAsStateWithLifecycle()?.value
    val library = libraryViewModel?.uiState?.collectAsStateWithLifecycle()?.value
    LaunchedEffect(libraryViewModel, isSignedIn) {
        if (isSignedIn && libraryViewModel?.uiState?.value?.content == LibraryContentState.Idle) {
            libraryViewModel.refresh()
        }
    }
    LaunchedEffect(accountId, tinderViewModel) {
        if (accountId != null) tinderViewModel?.loadFeed()
    }
    val readyLibrary = (library?.content as? LibraryContentState.Ready)?.snapshot
    val reading = readyLibrary?.entries?.firstOrNull { it.status == LibraryStatus.READING }
    val recommended = (feed as? BookTinderUiState.Ready)?.feed?.books?.firstOrNull()?.book
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BookieMark(Modifier.size(29.dp))
            Text("Bookie", Modifier.padding(start = 8.dp).weight(1f), fontSize = 23.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.6).sp)
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .5f))) {
                IconButton(onClick = onProfile) {
                    Icon(Icons.Default.AccountCircle, "Abrir mi perfil", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(27.dp))
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(if (isSignedIn) "QUÉ BUENO VERTE DE NUEVO" else "UN LUGAR PARA QUIENES LEEN", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.8.sp, color = MaterialTheme.colorScheme.primary)
            Text("¿Qué historia\nte espera hoy?", fontSize = 31.sp, lineHeight = 37.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-.8).sp)
            Text("Descubrí libros. Encontrá a tu gente.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
        Surface(onClick = onSearch, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .65f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .55f))) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary)
                Text("Libros, autores y nuevas historias", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Surface(onClick = onSearch, shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.surface) {
            Box(Modifier.fillMaxWidth().height(220.dp)) {
                BookieLibraryBackdrop(Modifier.fillMaxSize(), darken = .2f)
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFF121934).copy(alpha = .8f), Color.Transparent))))
                Column(Modifier.align(Alignment.CenterStart).padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.AutoAwesome, null, Modifier.size(14.dp), tint = Color(0xFFD8C5FF))
                        Text("PERDETE EN UNA BUENA HISTORIA", style = MaterialTheme.typography.labelSmall, letterSpacing = .7.sp, color = Color(0xFFE4D5FF))
                    }
                    Text("Entre páginas,\notros mundos.", fontFamily = FontFamily.Serif, fontSize = 31.sp, lineHeight = 35.sp, color = Color.White)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Explorar libros", color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp), tint = Color(0xFFC2B2FF))
                    }
                }
            }
        }
        if (isSignedIn && reading != null) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeading("Seguí tu historia", "Mi biblioteca", onLibrary)
                HomeBookCard(reading.book, "LEYENDO AHORA", onLibrary) {
                    val progress = (reading.progressPercentage ?: 0).coerceIn(0, 100)
                    Text("$progress % leído", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.outline.copy(alpha = .4f))
                    if (readyLibrary.stale) Text("Últimos datos guardados", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (isSignedIn && onExploreTinder != null) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeading("Elegido para vos", "Explorar", onExploreTinder)
                when {
                    recommended != null -> HomeBookCard(recommended, "UNA NUEVA CONEXIÓN", onExploreTinder)
                    feed is BookTinderUiState.Loading -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("Buscando tu próxima lectura…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    feed is BookTinderUiState.Error -> TextButton(onClick = { tinderViewModel.loadFeed() }) { Text("No pudimos cargar tus lecturas. Reintentar") }
                    else -> Text("Tus próximas lecturas aparecen acá. Explorá recomendaciones para encontrar una nueva historia.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Más que leer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ExploreCard("Descubrí", "Tu próximo favorito", Icons.AutoMirrored.Filled.MenuBook, onSearch, Modifier.weight(1f))
                ExploreCard("Conectá", "Lecturas en compañía", Icons.Default.Groups, onCommunity, Modifier.weight(1f))
            }
            onExploreSeries?.let { action ->
                TextButton(onClick = action, modifier = Modifier.fillMaxWidth()) {
                    Text("Sagas y universos por descubrir", Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Start)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp))
                }
            }
            onExploreChallenges?.let { action -> TextButton(onClick = action) { Text("Un nuevo reto de lectura te espera") } }
        }
        if (!isSignedIn && onSignIn != null) {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .4f))) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Las buenas historias\nse disfrutan más juntos.", style = MaterialTheme.typography.titleLarge)
                    Text("Guardá tus libros, seguí tus lecturas y encontrá personas con tus mismos gustos.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    BookieGradientButton("Entrar a Bookie", onSignIn, Modifier.fillMaxWidth())
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
            Text("  Tu próxima lectura te espera", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionHeading(title: String, actionLabel: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        TextButton(onClick = onClick) { Text(actionLabel, style = MaterialTheme.typography.labelMedium) }
    }
}

@Composable
private fun ExploreCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier) {
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .35f))) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(13.dp)).background(BookieGradient), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(21.dp))
            }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HomeBookCard(book: Book, eyebrow: String, onClick: () -> Unit, detail: @Composable () -> Unit = {}) {
    Surface(onClick = onClick, shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .4f))) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
            BookCover(book, Modifier.size(86.dp, 125.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(eyebrow, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = .8.sp)
                Text(book.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Text(book.authors.joinToString().ifBlank { "Autor desconocido" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                detail()
            }
        }
    }
}

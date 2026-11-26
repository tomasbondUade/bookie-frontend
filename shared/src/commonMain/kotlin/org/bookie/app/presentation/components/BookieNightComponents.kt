package org.bookie.app.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

val BookieInputShape = RoundedCornerShape(18.dp)

@Composable
fun bookieInputColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
)

/** Shared presentation primitives; actions and state remain owned by each screen. */
@Composable
fun BookiePageHeading(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        BookieMark(Modifier.size(28.dp))
    }
}

@Composable
fun BookieTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
        .background(MaterialTheme.colorScheme.surface).padding(4.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        labels.forEachIndexed { index, label ->
            val active = selected == index
            Box(Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(14.dp))
                .background(if (active) BookieGradient else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
                .selectable(selected = active, role = Role.Tab, onClick = { onSelect(index) })
                .padding(horizontal = 6.dp, vertical = 13.dp), contentAlignment = Alignment.Center) {
                Text(label, style = MaterialTheme.typography.labelMedium,
                    color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun BookieFeatureBanner(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))) {
        BookieLibraryBackdrop(Modifier.matchParentSize(), darken = .55f)
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("TU UNIVERSO LECTOR", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun BookiePanelCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit) {
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .3f))
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    if (onClick == null) {
        Card(modifier = modifier, shape = RoundedCornerShape(22.dp), border = border,
            colors = colors, content = content)
    } else {
        Card(onClick = onClick, enabled = enabled, modifier = modifier, shape = RoundedCornerShape(22.dp),
            border = border, colors = colors, content = content)
    }
}

@Composable
fun BookieActionButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit) {
    Button(onClick = onClick, enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(18.dp))
            .background(if (enabled) BookieGradient else Brush.linearGradient(listOf(
                MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant))),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White,
            disabledContainerColor = Color.Transparent, disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp), content = content)
}

@Composable
fun BookieAvatar(name: String, modifier: Modifier = Modifier) {
    Box(modifier.size(44.dp).clip(CircleShape).background(BookieGradient), contentAlignment = Alignment.Center) {
        Text(name.trim().firstOrNull()?.uppercase() ?: "B", color = Color.White,
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BookieMetric(value: String, label: String, modifier: Modifier = Modifier) {
    BookiePanelCard(modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

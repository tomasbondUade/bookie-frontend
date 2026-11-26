package org.bookie.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BookieLoadingState(
    modifier: Modifier = Modifier,
    message: String = "Cargando..."
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun BookieEmptyState(
    modifier: Modifier = Modifier,
    message: String = "No hay contenido para mostrar"
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BookieMark(Modifier.size(42.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            text = message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun BookieErrorState(
    modifier: Modifier = Modifier,
    message: String = "Ocurrió un error",
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null,
            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            text = message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )

        if (onRetry != null) {
            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onRetry
            ) {
                Text("Reintentar")
            }
        }
    }
}

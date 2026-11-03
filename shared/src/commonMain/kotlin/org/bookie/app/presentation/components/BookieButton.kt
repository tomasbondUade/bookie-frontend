package org.bookie.app.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme

enum class BookieButtonType {
    PRIMARY,
    SECONDARY,
    TEXT
}

@Composable
fun BookieButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    type: BookieButtonType = BookieButtonType.PRIMARY
) {
    when (type) {
        BookieButtonType.PRIMARY -> {
            Button(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        BookieButtonType.SECONDARY -> {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                border = ButtonDefaults.outlinedButtonBorder(enabled = enabled)
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        BookieButtonType.TEXT -> {
            TextButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
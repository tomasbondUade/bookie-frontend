
package org.bookie.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Configuración de colores para el tema oscuro
private val BookieDarkColorScheme = darkColorScheme(

    primary = BookieColors.DarkPrimary,
    onPrimary = Color.White,

    secondary = BookieColors.DarkSecondary,
    onSecondary = Color.White,

    primaryContainer = BookieColors.DarkSurfaceVariant,
    onPrimaryContainer = BookieColors.DarkTextPrimary,

    background = BookieColors.DarkBackground,
    onBackground = BookieColors.DarkTextPrimary,

    surface = BookieColors.DarkSurface,
    onSurface = BookieColors.DarkTextPrimary,

    surfaceVariant = BookieColors.DarkSurfaceVariant,
    onSurfaceVariant = BookieColors.DarkTextSecondary,

    outline = BookieColors.DarkOutline,

    error = BookieColors.DarkError,
    onError = Color.White
)

// Configuración de colores para el tema claro
private val BookieLightColorScheme = lightColorScheme(

    primary = BookieColors.LightPrimary,
    onPrimary = Color.White,

    secondary = BookieColors.LightSecondary,
    onSecondary = Color.White,

    primaryContainer = BookieColors.LightSurfaceVariant,
    onPrimaryContainer = BookieColors.LightTextPrimary,

    background = BookieColors.LightBackground,
    onBackground = BookieColors.LightTextPrimary,

    surface = BookieColors.LightSurface,
    onSurface = BookieColors.LightTextPrimary,

    surfaceVariant = BookieColors.LightSurfaceVariant,
    onSurfaceVariant = BookieColors.LightTextSecondary,

    outline = BookieColors.LightOutline,

    error = BookieColors.LightError,
    onError = Color.White
)

// Tema principal de Bookie
@Composable
fun BookieTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {

    val colorScheme = if (darkTheme) {
        BookieDarkColorScheme
    } else {
        BookieLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BookieTypography,
        content = content
    )
}

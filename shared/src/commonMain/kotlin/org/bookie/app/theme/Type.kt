
package org.bookie.app.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val BookieFont = FontFamily.SansSerif

val BookieTypography = Typography(

    // Títulos principales
    displayLarge = TextStyle(
        fontFamily = BookieFont,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp
    ),

    displayMedium = TextStyle(
        fontFamily = BookieFont,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 38.sp
    ),

    // Encabezados de pantalla y secciones
    headlineLarge = TextStyle(
        fontFamily = BookieFont,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 34.sp
    ),

    headlineMedium = TextStyle(
        fontFamily = BookieFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),

    // Títulos de cards y componentes
    titleLarge = TextStyle(
        fontFamily = BookieFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),

    titleMedium = TextStyle(
        fontFamily = BookieFont,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),

    // Textos y descripciones
    bodyLarge = TextStyle(
        fontFamily = BookieFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),

    bodyMedium = TextStyle(
        fontFamily = BookieFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),

    // Botones y etiquetas
    labelLarge = TextStyle(
        fontFamily = BookieFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),

    labelMedium = TextStyle(
        fontFamily = BookieFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

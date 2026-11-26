package org.bookie.app.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.bookie.app.resources.Res
import org.bookie.app.resources.bookie_library_night
import kotlinx.coroutines.delay
import org.bookie.app.theme.BookieColors
import org.jetbrains.compose.resources.painterResource

val BookieGradient: Brush
    get() = Brush.horizontalGradient(listOf(BookieColors.GradientBlue, BookieColors.GradientViolet))

/** A native vector mark that stays crisp on Android and iOS. */
@Composable
fun BookieMark(modifier: Modifier = Modifier) {
    Canvas(modifier.semantics { contentDescription = "Bookie" }) {
        val w = size.width
        val h = size.height
        val bookmark = Path().apply {
            moveTo(w * .2f, h * .1f)
            quadraticTo(w * .2f, 0f, w * .3f, 0f)
            lineTo(w * .7f, 0f)
            quadraticTo(w * .8f, 0f, w * .8f, h * .1f)
            lineTo(w * .8f, h * .87f)
            quadraticTo(w * .8f, h, w * .7f, h * .92f)
            lineTo(w * .5f, h * .73f)
            lineTo(w * .3f, h * .92f)
            quadraticTo(w * .2f, h, w * .2f, h * .87f)
            close()
        }
        drawPath(bookmark, Brush.linearGradient(listOf(Color(0xFF55B6FF), Color(0xFF8A67FF), Color(0xFFCA8BFF))))
        val fold = Path().apply {
            moveTo(w * .5f, h * .12f); lineTo(w * .5f, h * .63f)
            lineTo(w * .28f, h * .85f); lineTo(w * .28f, h * .12f); close()
        }
        drawPath(fold, Color.White.copy(alpha = .13f))
    }
}

@Composable
fun BookieLibraryBackdrop(modifier: Modifier = Modifier, darken: Float = .45f) {
    Box(modifier) {
        Image(
            painter = painterResource(Res.drawable.bookie_library_night),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(Modifier.fillMaxSize().background(Color(0xFF070B20).copy(alpha = darken)))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            listOf(Color(0xFF070B20).copy(alpha = .15f), Color.Transparent, Color(0xFF070B20).copy(alpha = .85f)),
        )))
    }
}

@Composable
fun BookieGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val shape = RoundedCornerShape(18.dp)
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = shape,
        modifier = modifier.heightIn(min = 56.dp).clip(shape)
            .background(if (enabled && !loading) BookieGradient else Brush.horizontalGradient(
                listOf(BookieColors.DarkSurfaceVariant, BookieColors.DarkSurfaceVariant),
            )),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = BookieColors.DarkTextSecondary,
        ),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (loading) "Un momento…" else text, fontWeight = FontWeight.SemiBold)
            if (!loading) Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
fun BookieSplash(onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        reveal.animateTo(1f, tween(780, easing = FastOutSlowInEasing))
        delay(650)
        onFinished()
    }
    Box(modifier.fillMaxSize().background(BookieColors.DarkBackground)) {
        BookieLibraryBackdrop(Modifier.fillMaxSize(), darken = .25f)
        Column(
            modifier = Modifier.align(Alignment.Center).padding(bottom = 40.dp).graphicsLayer {
                alpha = reveal.value
                scaleX = .88f + reveal.value * .12f
                scaleY = scaleX
                translationY = (1f - reveal.value) * 32f
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            BookieMark(Modifier.size(76.dp))
            Text("Bookie", fontSize = 48.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1.5).sp, color = Color.White)
            Text("Leé. Conectá. Recomendá.", color = Color(0xFFE3DDF5), style = MaterialTheme.typography.bodyLarge)
        }
        Text(
            "TU PRÓXIMA HISTORIA TE ESPERA",
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp),
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 2.sp,
            color = Color(0xFFD1C9E6),
        )
    }
}

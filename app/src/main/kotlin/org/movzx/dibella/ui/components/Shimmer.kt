package org.movzx.dibella.ui.components

import androidx.compose.animation.core.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

fun Modifier.shimmerBackground(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")

    val translateAnim: Float by
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "shimmerPrimary",
        )

    val secondaryTranslateAnim: Float by
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 1800, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "shimmerSecondary",
        )

    val baseColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val highlightColor = MaterialTheme.colorScheme.surfaceVariant
    val shimmerColors = listOf(baseColor, highlightColor, baseColor)

    val secondaryHighlight = Color.White.copy(alpha = 0.05f)
    val secondaryColors = listOf(Color.Transparent, secondaryHighlight, Color.Transparent)

    this.drawBehind {
        val width = size.width
        val height = size.height
        val xPos = (translateAnim / 1000f) * (width + 500f) - 250f

        val primaryBrush =
            Brush.linearGradient(
                colors = shimmerColors,
                start = Offset(xPos, 0f),
                end = Offset(xPos + 250f, height),
            )

        drawRect(brush = primaryBrush)

        val xPosSec = (secondaryTranslateAnim / 1000f) * (width + 400f) - 200f

        val secondaryBrush =
            Brush.linearGradient(
                colors = secondaryColors,
                start = Offset(xPosSec, height),
                end = Offset(xPosSec + 150f, 0f),
            )

        drawRect(brush = secondaryBrush)
    }
}

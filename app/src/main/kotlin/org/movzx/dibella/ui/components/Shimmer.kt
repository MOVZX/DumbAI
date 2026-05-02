package org.movzx.dibella.ui.components

import androidx.compose.animation.core.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

fun Modifier.shimmerBackground(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "shimmer",
        )

    val baseColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val highlightColor = MaterialTheme.colorScheme.surfaceVariant
    val shimmerColors = listOf(baseColor, highlightColor, baseColor)

    this.drawBehind {
        val width = size.width
        val height = size.height
        val xPos = (translateAnim / 1000f) * (width + 500f) - 250f

        val brush =
            Brush.linearGradient(
                colors = shimmerColors,
                start = Offset(xPos, 0f),
                end = Offset(xPos + 250f, height),
            )

        drawRect(brush = brush)
    }
}

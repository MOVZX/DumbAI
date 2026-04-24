package org.movzx.dumbai.ui.components

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
            initialValue = -200f,
            targetValue = 1000f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 1500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "shimmer",
        )

    val baseColor = MaterialTheme.colorScheme.surface
    val highlightColor = MaterialTheme.colorScheme.surfaceVariant

    val shimmerColors =
        listOf(
            baseColor.copy(alpha = 0.9f),
            highlightColor.copy(alpha = 0.4f),
            baseColor.copy(alpha = 0.9f),
        )

    this.drawBehind {
        val brush =
            Brush.linearGradient(
                colors = shimmerColors,
                start = Offset(translateAnim, translateAnim),
                end = Offset(translateAnim + 500f, translateAnim + 500f),
            )

        drawRect(brush = brush)
    }
}

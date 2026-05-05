package org.movzx.dibella.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.movzx.dibella.R

@Composable
fun SplashScreen(onSplashFinished: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    val logoAlpha by
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(800, easing = EaseOutBack),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "logoAlpha",
        )

    val logoScale by
        infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(600, easing = EaseOutBack),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "logoScale",
        )

    val logoRotation by
        infiniteTransition.animateFloat(
            initialValue = -10f,
            targetValue = 0f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(600, easing = EaseOutBack),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "logoRotation",
        )

    val fadeOut by
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(500, delayMillis = 2000, easing = EaseIn),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "fadeOut",
        )

    LaunchedEffect(Unit) {
        delay(2500)
        onSplashFinished()
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors =
                            listOf(
                                colorResource(R.color.primary).copy(alpha = 0.3f),
                                colorResource(R.color.secondary).copy(alpha = 0.2f),
                                colorResource(R.color.background),
                            )
                    )
                ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.alpha(fadeOut),
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_app_logo),
                contentDescription = stringResource(R.string.app_name),
                modifier =
                    Modifier.size(120.dp).scale(logoScale).alpha(logoAlpha).graphicsLayer {
                        rotationZ = logoRotation
                    },
                contentScale = ContentScale.Fit,
            )

            Spacer(modifier = Modifier.height(24.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                strokeWidth = 2.dp,
                color = colorResource(R.color.primary),
                trackColor = colorResource(R.color.primary).copy(alpha = 0.2f),
            )
        }
    }
}

package org.movzx.dibella.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp

data class SpeedDialItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val containerColor: Color? = null,
    val contentColor: Color? = null,
)

@Composable
fun SpeedDialFab(
    items: List<SpeedDialItem>,
    modifier: Modifier = Modifier,
    mainIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Add,
) {
    val view = LocalView.current
    var expanded by remember { mutableStateOf(false) }

    val rotation by
        animateFloatAsState(
            targetValue = if (expanded) 45f else 0f,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "Rotation",
        )

    val scale by
        animateFloatAsState(
            targetValue = if (expanded) 1.1f else 1f,
            animationSpec =
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            label = "Scale",
        )

    Column(
        modifier = modifier.fillMaxWidth().wrapContentHeight(),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEachIndexed { index, item ->
            AnimatedVisibility(
                visible = expanded,
                enter =
                    fadeIn(animationSpec = tween(150, delayMillis = (items.size - index) * 50)) +
                        scaleIn(
                            animationSpec =
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow,
                                ),
                            initialScale = 0.5f,
                        ) +
                        slideInVertically(initialOffsetY = { it / 2 }),
                exit =
                    fadeOut(animationSpec = tween(100)) +
                        scaleOut(targetScale = 0.8f) +
                        slideOutVertically(targetOffsetY = { it / 2 }),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        tonalElevation = 4.dp,
                    ) {
                        Text(
                            text = item.label,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }

                    SmallFloatingActionButton(
                        onClick = {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                            expanded = false
                            item.onClick()
                        },
                        containerColor = item.containerColor ?: MaterialTheme.colorScheme.primary,
                        contentColor = item.contentColor ?: MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }

        SmallFloatingActionButton(
            onClick = {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                expanded = !expanded
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation =
                FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 16.dp,
                ),
            modifier =
                Modifier.rotate(rotation).size(48.dp).graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
        ) {
            Icon(
                imageVector = mainIcon,
                contentDescription = "Menu",
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

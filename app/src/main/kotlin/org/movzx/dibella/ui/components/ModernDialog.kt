package org.movzx.dibella.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.movzx.dibella.R

@Composable
fun ModernDialog(
    title: String,
    message: String? = null,
    icon: ImageVector? = null,
    onConfirm: () -> Unit,
    confirmText: String = stringResource(R.string.btn_confirm),
    confirmIcon: ImageVector? = Icons.Default.Check,
    onDismiss: () -> Unit,
    dismissText: String = stringResource(R.string.btn_cancel),
    dismissIcon: ImageVector? = Icons.Default.Close,
    showConfirm: Boolean = true,
    showDismiss: Boolean = true,
    confirmEnabled: Boolean = true,
    confirmColor: Color? = null,
    dismissColor: Color? = null,
    customContent: @Composable ColumnScope.() -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by
        animateFloatAsState(
            targetValue = if (isPressed) 0.98f else 1f,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 150f),
            label = "DialogScale",
        )

    val confirmButtonColor = confirmColor ?: colorResource(R.color.success)
    val dismissButtonColor = dismissColor ?: colorResource(R.color.error)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null)
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )

                Text(
                    text = title,
                    style =
                        MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (message != null)
                    Text(text = message, style = MaterialTheme.typography.bodyMedium)

                customContent()
            }
        },
        confirmButton = {
            if (showConfirm) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)

                            if (confirmEnabled) onConfirm()
                        },
                        modifier = Modifier.weight(1f),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = confirmButtonColor,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        shape = MaterialTheme.shapes.medium,
                        enabled = confirmEnabled,
                    ) {
                        if (confirmIcon != null) {
                            Icon(confirmIcon, null, modifier = Modifier.size(18.dp))

                            Spacer(Modifier.width(4.dp))
                        }

                        Text(text = confirmText)
                    }

                    if (showDismiss) {
                        Button(
                            onClick = {
                                view.performHapticFeedback(
                                    android.view.HapticFeedbackConstants.CONFIRM
                                )
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = dismissButtonColor,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            if (dismissIcon != null) {
                                Icon(dismissIcon, null, modifier = Modifier.size(18.dp))

                                Spacer(Modifier.width(4.dp))
                            }

                            Text(text = dismissText)
                        }
                    }
                }
            }
        },
        modifier =
            modifier.shadow(8.dp, RoundedCornerShape(16.dp)).graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(16.dp),
    )
}

package org.movzx.dibella.ui.components

import androidx.compose.animation.core.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import org.movzx.dibella.R

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val view = LocalView.current

    val scale by
        animateFloatAsState(
            targetValue = 1f,
            animationSpec =
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            label = "dialogScale",
        )

    ModernDialog(
        title = title,
        message = message,
        icon = Icons.Default.Warning,
        onConfirm = {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
            onConfirm()
        },
        confirmText = stringResource(R.string.btn_confirm),
        confirmIcon = Icons.Default.Check,
        onDismiss = {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
            onDismiss()
        },
        dismissText = stringResource(R.string.btn_cancel),
        dismissIcon = Icons.Default.Close,
    )
}

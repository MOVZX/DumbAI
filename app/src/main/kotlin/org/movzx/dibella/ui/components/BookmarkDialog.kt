package org.movzx.dibella.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import org.movzx.dibella.R

@Composable
fun BookmarkDialog(onApply: (String) -> Unit, onDismiss: () -> Unit) {
    val view = LocalView.current
    var title by remember { mutableStateOf("") }

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
        title = stringResource(R.string.dialog_bookmark_title),
        icon = Icons.Default.Bookmark,
        onConfirm = {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)

            if (title.isNotBlank()) onApply(title)
        },
        confirmText = stringResource(R.string.btn_apply),
        confirmIcon = Icons.Default.Check,
        confirmEnabled = title.isNotBlank(),
        onDismiss = {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
            onDismiss()
        },
        dismissText = stringResource(R.string.btn_cancel),
        dismissIcon = Icons.Default.Close,
        customContent = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text(stringResource(R.string.placeholder_bookmark_title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        modifier =
            Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    )
}

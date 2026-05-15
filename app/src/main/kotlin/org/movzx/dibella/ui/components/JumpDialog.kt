package org.movzx.dibella.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import org.movzx.dibella.R

@Composable
fun JumpDialog(currentCursor: String?, onApply: (String) -> Unit, onDismiss: () -> Unit) {
    val view = LocalView.current
    var targetCursor by remember { mutableStateOf("") }

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
        title = stringResource(R.string.dialog_jump_title),
        icon = Icons.Default.ArrowForward,
        message = String.format(stringResource(R.string.dialog_jump_msg), currentCursor ?: "N/A"),
        onConfirm = {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)

            if (targetCursor.isNotBlank()) onApply(targetCursor)
        },
        confirmText = stringResource(R.string.btn_apply),
        confirmIcon = Icons.Default.Check,
        confirmEnabled = targetCursor.isNotBlank(),
        onDismiss = {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
            onDismiss()
        },
        dismissText = stringResource(R.string.btn_cancel),
        dismissIcon = Icons.Default.Close,
        customContent = {
            OutlinedTextField(
                value = targetCursor,
                onValueChange = { targetCursor = it },
                placeholder = { Text(stringResource(R.string.placeholder_target_cursor)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

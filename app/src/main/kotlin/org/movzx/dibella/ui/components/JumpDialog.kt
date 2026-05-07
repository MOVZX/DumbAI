package org.movzx.dibella.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_jump_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text =
                        String.format(
                            stringResource(R.string.dialog_jump_msg),
                            currentCursor ?: "N/A",
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                )

                OutlinedTextField(
                    value = targetCursor,
                    onValueChange = { targetCursor = it },
                    placeholder = { Text(stringResource(R.string.placeholder_target_cursor)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = {
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(text = stringResource(R.string.btn_cancel))
                }

                Button(
                    onClick = {
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)

                        if (targetCursor.isNotBlank()) onApply(targetCursor)
                    },
                    modifier = Modifier.weight(1f),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.tertiary),
                            contentColor = androidx.compose.ui.graphics.Color.White,
                        ),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(text = stringResource(R.string.btn_apply))
                }
            }
        },
        modifier =
            Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    )
}

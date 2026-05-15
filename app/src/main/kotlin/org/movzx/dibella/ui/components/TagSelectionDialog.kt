package org.movzx.dibella.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.movzx.dibella.R

data class TagOption(val id: Int?, val name: String)

@Composable
fun TagSelectionDialog(
    allTags: List<TagOption>,
    initialTags: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val view = LocalView.current
    var editTags by remember(initialTags) { mutableStateOf(initialTags) }

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
        title = "Select Tags",
        icon = Icons.Default.Tag,
        onConfirm = {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
            onConfirm(editTags)
        },
        confirmText = stringResource(R.string.btn_confirm),
        confirmIcon = Icons.Default.Check,
        onDismiss = {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
            onDismiss()
        },
        dismissText = stringResource(R.string.btn_cancel),
        dismissIcon = Icons.Default.Close,
        customContent = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState()),
            ) {
                allTags.forEach { tag ->
                    val selectedIds =
                        editTags.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()

                    val isSelected = selectedIds.contains(tag.id)

                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            val currentIds =
                                editTags
                                    .split(",")
                                    .mapNotNull { it.trim().toIntOrNull() }
                                    .toMutableSet()

                            if (isSelected) currentIds.remove(tag.id) else currentIds.add(tag.id!!)

                            editTags = currentIds.joinToString(",")
                        },
                        label = { Text(tag.name) },
                    )
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

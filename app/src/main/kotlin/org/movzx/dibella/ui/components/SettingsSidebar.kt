package org.movzx.dibella.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.movzx.dibella.R
import org.movzx.dibella.util.scrollbar

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsSidebar(
    cacheSize: String,
    apiKey: String,
    downloadPath: String?,
    debugEnabled: Boolean,
    hidePlayerControls: Boolean,
    alwaysEnableHD: Boolean,
    alwaysMuteVideo: Boolean,
    feedVideoAutoplay: Boolean,
    onDismiss: () -> Unit,
    onClearCache: () -> Unit,
    onSaveApiKey: (String) -> Unit,
    onUpdateDownloadPath: (String?) -> Unit,
    onPickDirectory: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onToggleDebug: (Boolean) -> Unit,
    onHidePlayerControls: (Boolean) -> Unit,
    onAlwaysEnableHD: (Boolean) -> Unit,
    onAlwaysMuteVideo: (Boolean) -> Unit,
    onFeedVideoAutoplay: (Boolean) -> Unit,
) {
    var key by remember(apiKey) { mutableStateOf(apiKey) }
    var path by remember(downloadPath) { mutableStateOf(downloadPath ?: "") }
    val scrollState = rememberScrollState()
    var showAutoplayDialog by remember { mutableStateOf(false) }

    if (showAutoplayDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.dialog_autoplay_title),
            message = stringResource(R.string.dialog_autoplay_msg),
            onConfirm = {
                onFeedVideoAutoplay(true)
                showAutoplayDialog = false
            },
            onDismiss = { showAutoplayDialog = false },
        )
    }

    BaseSidebar(title = stringResource(R.string.settings), onDismiss = onDismiss) {
        Column(
            modifier = Modifier.fillMaxSize().scrollbar(scrollState).verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            SidebarSection(title = stringResource(R.string.section_player)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.label_hide_player_controls),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Switch(checked = hidePlayerControls, onCheckedChange = onHidePlayerControls)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.label_always_enable_hd),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Switch(checked = alwaysEnableHD, onCheckedChange = onAlwaysEnableHD)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.label_always_mute_video),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Switch(checked = alwaysMuteVideo, onCheckedChange = onAlwaysMuteVideo)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.label_feed_video_autoplay),
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    Switch(
                        checked = feedVideoAutoplay,
                        onCheckedChange = { checked ->
                            if (checked) showAutoplayDialog = true else onFeedVideoAutoplay(false)
                        },
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            SidebarSection(title = stringResource(R.string.section_api)) {
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text(stringResource(R.string.label_api_key)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                )

                Button(
                    onClick = { onSaveApiKey(key) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(stringResource(R.string.btn_save_api_key))
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            SidebarSection(title = stringResource(R.string.section_storage)) {
                OutlinedTextField(
                    value = path,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.label_gallery_location)) },
                    placeholder = { Text(stringResource(R.string.placeholder_default_gallery)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    trailingIcon = {
                        IconButton(onClick = onPickDirectory) {
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = stringResource(R.string.btn_choose),
                            )
                        }
                    },
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onPickDirectory,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Icon(Icons.Default.Folder, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_choose))
                    }

                    OutlinedButton(
                        onClick = {
                            path = ""
                            onUpdateDownloadPath(null)
                        },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text(stringResource(R.string.btn_reset))
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            SidebarSection(title = stringResource(R.string.section_backup)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onImport,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = colorResource(R.color.success),
                                contentColor = androidx.compose.ui.graphics.Color.White,
                            ),
                    ) {
                        Text(stringResource(R.string.btn_import))
                    }

                    Button(
                        onClick = onExport,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = colorResource(R.color.error),
                                contentColor = androidx.compose.ui.graphics.Color.White,
                            ),
                    ) {
                        Text(stringResource(R.string.btn_export))
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            SidebarSection(title = stringResource(R.string.section_developer)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.label_debug_logging),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Switch(checked = debugEnabled, onCheckedChange = onToggleDebug)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            SidebarSection(title = stringResource(R.string.section_cache)) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(
                        stringResource(R.string.label_image_cache_size, cacheSize),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    )
                }

                Button(
                    onClick = { onClearCache() },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.error),
                            contentColor = androidx.compose.ui.graphics.Color.White,
                        ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(stringResource(R.string.btn_clear_image_cache))
                }
            }
        }
    }
}

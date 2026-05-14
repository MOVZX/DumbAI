package org.movzx.dibella.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.movzx.dibella.R

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsSidebar(
    cacheSize: String,
    apiKey: String,
    searchApiKey: String,
    downloadPath: String?,
    favoritesPath: String?,
    debugEnabled: Boolean,
    hidePlayerControls: Boolean,
    alwaysEnableHD: Boolean,
    alwaysMuteVideo: Boolean,
    feedVideoAutoplay: Boolean,
    backendEnabled: Boolean,
    backendUrl: String,
    backendApiKey: String,
    onDismiss: () -> Unit,
    onClearCache: () -> Unit,
    onSaveApiKey: (String) -> Unit,
    onSaveBearerToken: (String) -> Unit,
    onSaveBackendUrl: (String) -> Unit,
    onSaveBackendApiKey: (String) -> Unit,
    onUpdateDownloadPath: (String?) -> Unit,
    onUpdateFavoritesPath: (String?) -> Unit,
    onPickDirectory: () -> Unit,
    onPickFavoritesDirectory: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onToggleDebug: (Boolean) -> Unit,
    onToggleBackend: (Boolean) -> Unit,
    onHidePlayerControls: (Boolean) -> Unit,
    onAlwaysEnableHD: (Boolean) -> Unit,
    onAlwaysMuteVideo: (Boolean) -> Unit,
    onFeedVideoAutoplay: (Boolean) -> Unit,
    amoledMode: Boolean = false,
    onToggleAmoled: (Boolean) -> Unit = {},
) {
    var key by remember(apiKey) { mutableStateOf(apiKey) }
    var bearerToken by remember(searchApiKey) { mutableStateOf(searchApiKey) }
    var bUrl by remember(backendUrl) { mutableStateOf(backendUrl) }
    var bKey by remember(backendApiKey) { mutableStateOf(backendApiKey) }
    var path by remember(downloadPath) { mutableStateOf(downloadPath ?: "") }
    var favPath by remember(favoritesPath) { mutableStateOf(favoritesPath ?: "") }
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

    BaseSidebar(
        title = stringResource(R.string.settings),
        onDismiss = onDismiss,
        amoledMode = amoledMode,
    ) {
        SidebarSection(
            title = stringResource(R.string.section_appearance),
            icon = Icons.Outlined.Palette,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.label_amoled_mode),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Switch(checked = amoledMode, onCheckedChange = onToggleAmoled)
            }
        }

        SidebarSection(
            title = stringResource(R.string.section_player),
            icon = Icons.Outlined.PlayCircle,
        ) {
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

        SidebarSection(title = stringResource(R.string.section_api), icon = Icons.Outlined.Key) {
            OutlinedTextField(
                value = key,
                onValueChange = { key = it },
                label = { Text(stringResource(R.string.label_api_key)) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
            )

            Button(
                onClick = { onSaveApiKey(key) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.success),
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            ) {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.btn_save_api_key))
            }

            OutlinedTextField(
                value = bearerToken,
                onValueChange = { bearerToken = it },
                label = { Text(stringResource(R.string.label_civitai_bearer_token)) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
            )

            Button(
                onClick = { onSaveBearerToken(bearerToken) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.success),
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            ) {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.btn_save_bearer_token))
            }
        }

        SidebarSection(
            title = stringResource(R.string.section_backend),
            icon = Icons.Outlined.Cloud,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.label_backend_enabled),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Switch(checked = backendEnabled, onCheckedChange = onToggleBackend)
            }

            OutlinedTextField(
                value = bKey,
                onValueChange = { bKey = it },
                label = { Text(stringResource(R.string.label_backend_api_key)) },
                placeholder = { Text(stringResource(R.string.placeholder_backend_api_key)) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                enabled = backendEnabled,
            )

            Button(
                onClick = { onSaveBackendApiKey(bKey) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                enabled = backendEnabled,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.success),
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            ) {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.btn_save_backend_api_key))
            }

            OutlinedTextField(
                value = bUrl,
                onValueChange = { bUrl = it },
                label = { Text(stringResource(R.string.label_backend_url)) },
                placeholder = { Text(stringResource(R.string.placeholder_backend_url)) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                enabled = backendEnabled,
            )

            Button(
                onClick = { onSaveBackendUrl(bUrl) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                enabled = backendEnabled,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.success),
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            ) {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.btn_save_backend_url))
            }
        }

        SidebarSection(
            title = stringResource(R.string.section_storage),
            icon = Icons.Outlined.Folder,
        ) {
            OutlinedTextField(
                value = path,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.label_gallery_location)) },
                placeholder = { Text(stringResource(R.string.placeholder_default_gallery)) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
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
                    shape = MaterialTheme.shapes.small,
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
                    shape = MaterialTheme.shapes.small,
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_reset))
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = favPath,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.label_favorite_location)) },
                placeholder = { Text(stringResource(R.string.placeholder_default_favorite)) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                trailingIcon = {
                    IconButton(onClick = onPickFavoritesDirectory) {
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
                    onClick = onPickFavoritesDirectory,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Icon(Icons.Default.Folder, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_choose))
                }

                OutlinedButton(
                    onClick = {
                        favPath = ""
                        onUpdateFavoritesPath(null)
                    },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_reset))
                }
            }
        }

        SidebarSection(
            title = stringResource(R.string.section_backup),
            icon = Icons.Outlined.Backup,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onImport,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.success),
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                ) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_import))
                }

                Button(
                    onClick = onExport,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.error),
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                ) {
                    Icon(Icons.Default.Upload, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_export))
                }
            }
        }

        SidebarSection(
            title = stringResource(R.string.section_developer),
            icon = Icons.Outlined.Code,
        ) {
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

        SidebarSection(
            title = stringResource(R.string.section_cache),
            icon = Icons.Outlined.Storage,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
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
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
            ) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.btn_clear_image_cache))
            }
        }
    }
}

package org.movzx.dibella.ui.components

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@Composable
fun AppBackHandler(
    enabled: Boolean,
    isSelectionMode: Boolean = false,
    clearSelection: () -> Unit = {},
    leftDrawerState: DrawerState,
    rightDrawerState: DrawerState,
    scope: kotlinx.coroutines.CoroutineScope,
    backPressedTime: Long,
    onUpdateBackPressedTime: (Long) -> Unit,
    exitConfirmMsg: String,
) {
    val context = LocalContext.current
    BackHandler(enabled = enabled) {
        if (isSelectionMode) {
            clearSelection()
        } else if (leftDrawerState.isOpen) {
            scope.launch { leftDrawerState.close() }
        } else if (rightDrawerState.isOpen) {
            scope.launch { rightDrawerState.close() }
        } else {
            val currentTime = System.currentTimeMillis()

            if (currentTime - backPressedTime < 2000) {
                (context as? Activity)?.finish()
            } else {
                onUpdateBackPressedTime(currentTime)

                android.widget.Toast.makeText(
                        context,
                        org.movzx.dibella.R.string.msg_exit_confirm,
                        android.widget.Toast.LENGTH_SHORT,
                    )
                    .show()
            }
        }
    }
}

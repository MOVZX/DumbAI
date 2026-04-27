package org.movzx.dibella.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseSidebar(
    title: String,
    onDismiss: () -> Unit,
    footer: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalDrawerSheet(
        modifier =
            Modifier.width(320.dp).fillMaxHeight().windowInsetsPadding(WindowInsets.safeDrawing),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerShape = RectangleShape,
    ) {
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(top = 24.dp, bottom = 32.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 24.dp),
            )

            Column(modifier = Modifier.weight(1f), content = content)

            Spacer(modifier = Modifier.height(24.dp))

            footer()
        }
    }
}

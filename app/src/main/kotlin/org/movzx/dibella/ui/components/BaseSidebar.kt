package org.movzx.dibella.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import org.movzx.dibella.util.scrollbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseSidebar(
    title: String,
    onDismiss: () -> Unit,
    footer: @Composable ColumnScope.() -> Unit = {},
    amoledMode: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollState = rememberScrollState()

    ModalDrawerSheet(
        modifier =
            Modifier.width(320.dp).fillMaxHeight().windowInsetsPadding(WindowInsets.safeDrawing),
        drawerContainerColor =
            if (amoledMode) androidx.compose.ui.graphics.Color.Black
            else MaterialTheme.colorScheme.surface,
        drawerShape = RectangleShape,
    ) {
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(top = 24.dp, bottom = 24.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 24.dp),
                color = MaterialTheme.colorScheme.primary,
            )

            Column(
                modifier = Modifier.weight(1f).scrollbar(scrollState).verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                content = content,
            )

            Spacer(modifier = Modifier.height(24.dp))

            footer()
        }
    }
}

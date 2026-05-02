package org.movzx.dibella

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import coil3.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.movzx.dibella.ui.screens.MainScreen
import org.movzx.dibella.ui.theme.DibellaTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var imageLoader: ImageLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { DibellaTheme { MainScreen(imageLoader) } }
    }
}

enum class RightSidebarType {
    FILTERS,
    SETTINGS,
}

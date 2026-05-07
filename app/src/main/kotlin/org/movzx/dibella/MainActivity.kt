package org.movzx.dibella

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import coil3.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.movzx.dibella.data.UserPreferencesRepository
import org.movzx.dibella.ui.components.SplashScreen
import org.movzx.dibella.ui.screens.MainScreen
import org.movzx.dibella.ui.screens.OnboardingScreen
import org.movzx.dibella.ui.theme.DibellaTheme

enum class RightSidebarType {
    FILTERS,
    SETTINGS,
}

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject lateinit var imageLoader: ImageLoader
    @Inject lateinit var preferencesRepository: UserPreferencesRepository

    private val permissionsToRequest = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.READ_MEDIA_IMAGES)
            add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permissions ->
            val allGranted = permissions.values.all { it }

            if (allGranted) {
                _permissionsGranted.value = true
            } else {
                val denied = permissions.filterValues { !it }.keys.joinToString(", ")

                Toast.makeText(
                        this,
                        "Missing permissions: $denied. Gallery and favorites won't work.",
                        Toast.LENGTH_LONG,
                    )
                    .show()

                _permissionsGranted.value = true
            }
        }

    private val _permissionsGranted = mutableStateOf(false)
    private val _showOnboarding = mutableStateOf(false)
    private val _showMain = mutableStateOf(false)

    val permissionsGranted: State<Boolean>
        get() = _permissionsGranted

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val allGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) _permissionsGranted.value = true
        else permissionLauncher.launch(permissionsToRequest.toTypedArray())

        lifecycleScope.launch {
            preferencesRepository.onboardingCompleted.collect { completed ->
                if (!completed) _showOnboarding.value = true
            }
        }

        setContent {
            DibellaTheme {
                if (_permissionsGranted.value) {
                    when {
                        _showOnboarding.value -> {
                            OnboardingScreen(
                                onSkip = { _showOnboarding.value = false },
                                onFinish = {
                                    lifecycleScope.launch {
                                        preferencesRepository.updateOnboardingCompleted(true)
                                        _showOnboarding.value = false
                                    }
                                },
                            )
                        }
                        _showMain.value -> {
                            MainScreen(imageLoader)
                        }
                        else -> {
                            SplashScreen(
                                onSplashFinished = {
                                    lifecycleScope.launch {
                                        delay(500)
                                        _showMain.value = true
                                    }
                                }
                            )
                        }
                    }
                } else
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
            }
        }
    }
}

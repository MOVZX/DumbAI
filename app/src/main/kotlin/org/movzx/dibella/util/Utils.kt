package org.movzx.dibella.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.io.File
import org.movzx.dibella.data.UserPreferencesRepository
import org.movzx.dibella.model.CivitaiImage
import org.movzx.dibella.model.FavoriteImage

@EntryPoint
@InstallIn(SingletonComponent::class)
interface UtilsEntryPoint {
    fun userPreferencesRepository(): UserPreferencesRepository
}

fun Modifier.scrollbar(state: ScrollState, width: Dp = 4.dp, color: Color? = null): Modifier =
    composed {
        val barColor = color ?: MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        val targetAlpha = if (state.isScrollInProgress) 1f else 0f
        val duration = if (state.isScrollInProgress) 150 else 500

        val alpha by
            animateFloatAsState(
                targetValue = targetAlpha,
                animationSpec = tween(durationMillis = duration),
                label = "scrollbar_alpha",
            )

        drawWithContent {
            drawContent()

            val viewHeight = size.height
            val totalHeight = state.maxValue.toFloat() + viewHeight
            val barHeight = (viewHeight / totalHeight) * viewHeight
            val barOffset = (state.value.toFloat() / totalHeight) * viewHeight

            drawRect(
                color = barColor.copy(alpha = alpha * barColor.alpha),
                topLeft = Offset(size.width - width.toPx(), barOffset),
                size = Size(width.toPx(), barHeight),
            )
        }
    }

fun hasLocalCache(context: Context, imageId: Long, isVideo: Boolean): Boolean {
    val favoritesDir = File(context.filesDir, "favorites")

    if (!favoritesDir.exists()) return false

    val extensions = listOf("jpg", "png", "webp", "gif", "avif")

    val found = extensions.any { ext ->
        val file = File(favoritesDir, "$imageId.$ext")

        file.exists() && file.length() > 100
    }

    if (found) Logger.v("Dibella_IO", "[$imageId] Local thumbnail cache found")

    return found
}

fun hasFullCache(context: Context, imageId: Long, isVideo: Boolean): Boolean {
    val favoritesDir = File(context.filesDir, "favorites")

    if (!favoritesDir.exists()) return false

    val found =
        if (isVideo) {
            val extensions = listOf("mp4", "webm", "mkv")

            extensions.any { ext ->
                val file = File(favoritesDir, "$imageId.$ext")

                file.exists() && file.length() > 100
            }
        } else {
            val extensions = listOf("jpg", "png", "webp", "gif", "avif")

            extensions.any { ext ->
                val file = File(favoritesDir, "${imageId}_full.$ext")

                file.exists() && file.length() > 100
            }
        }

    if (found) Logger.v("Dibella_IO", "[$imageId] Full resolution cache found")

    return found
}

fun resolveImageData(
    context: Context,
    image: CivitaiImage,
    favoriteInfo: FavoriteImage?,
    thumbnailWidth: Int = 320,
    useVideoPath: Boolean = false,
): String {
    val favoritesDir = File(context.filesDir, "favorites")

    val primaryLocalPath =
        if (useVideoPath && image.type == "video") {
            favoriteInfo?.localVideoPath ?: File(favoritesDir, "${image.id}.mp4").absolutePath
        } else if (thumbnailWidth > 400) {
            favoriteInfo?.localFullImagePath
                ?: File(favoritesDir, "${image.id}_full.jpg").absolutePath
        } else {
            favoriteInfo?.localPath ?: File(favoritesDir, "${image.id}.jpg").absolutePath
        }

    val primaryFile = File(primaryLocalPath)

    if (primaryFile.exists() && primaryFile.length() > 100) {
        Logger.d("Dibella_Res", "ID: ${image.id} | Local (Primary) | Path: ${primaryFile.name}")

        return primaryFile.absolutePath
    }

    val extensions =
        if (image.type == "video") listOf("mp4", "webm", "mkv")
        else listOf("jpg", "png", "webp", "gif", "avif")

    val baseName =
        if (thumbnailWidth > 400 && image.type != "video") "${image.id}_full" else "${image.id}"

    for (ext in extensions) {
        val file = File(favoritesDir, "$baseName.$ext")

        if (file.exists() && file.length() > 100) {
            Logger.d("Dibella_Res", "ID: ${image.id} | Local (Ext Search) | Path: ${file.name}")

            return file.absolutePath
        }
    }

    if (thumbnailWidth > 400) {
        val fallbackPath =
            favoriteInfo?.localPath ?: File(favoritesDir, "${image.id}.jpg").absolutePath

        val fallbackFile = File(fallbackPath)

        if (fallbackFile.exists() && fallbackFile.length() > 100) {
            Logger.d(
                "Dibella_Res",
                "ID: ${image.id} | Local (Fallback) | Path: ${fallbackFile.name}",
            )

            return fallbackFile.absolutePath
        }

        for (ext in listOf("jpg", "png", "webp", "gif", "avif")) {
            val file = File(favoritesDir, "${image.id}.$ext")

            if (file.exists() && file.length() > 100) {
                Logger.d(
                    "Dibella_Res",
                    "ID: ${image.id} | Local (Fallback Ext) | Path: ${file.name}",
                )

                return file.absolutePath
            }
        }
    }

    if (image.url.startsWith("http")) {
        val remoteUrl =
            if (image.type == "video") {
                if (useVideoPath) getVideoPreviewUrl(image.url) else getVideoThumbnailUrl(image.url)
            } else getThumbnailUrl(image.url, thumbnailWidth)

        Logger.d("Dibella_Res", "ID: ${image.id} | Remote | URL: $remoteUrl")

        return remoteUrl
    }

    Logger.e("Dibella_Res", "[${image.id}] Failed to resolve any valid path/URL, returning raw URL")

    return image.url
}

fun modifyCivitaiUrl(url: String, variant: String): String {
    if (!url.contains("image.civitai.com")) return url

    return when {
        url.contains("/original=true/") -> url.replace("/original=true/", "/$variant/")
        url.contains("/original=false/") -> url.replace("/original=false/", "/$variant/")
        url.contains(Regex("/width=\\d+/")) -> url.replace(Regex("/width=\\d+/"), "/$variant/")
        else -> {
            val lastSlashIndex = url.lastIndexOf('/')

            if (lastSlashIndex != -1) {
                val prefix = url.substring(0, lastSlashIndex)
                val fileName = url.substring(lastSlashIndex + 1)

                "$prefix/$variant/$fileName"
            } else url
        }
    }
}

fun getThumbnailUrl(url: String, width: Int): String {
    return modifyCivitaiUrl(url, "width=$width")
}

fun getVideoThumbnailUrl(url: String): String {
    if (!url.contains("image.civitai.com")) return url

    val baseUrl =
        if (url.contains("/original=true/")) url.substringBefore("/original=true/")
        else url.substringBeforeLast("/")

    return "$baseUrl/anim=false,transcode=true,width=450,original=false,optimized=true"
}

fun getVideoPreviewUrl(url: String): String {
    if (!url.contains("image.civitai.com")) return url

    val baseUrl =
        if (url.contains("/original=true/")) url.substringBefore("/original=true/")
        else url.substringBeforeLast("/")

    return "$baseUrl/transcode=true,width=450,optimized=true"
}

fun getVideoOriginalUrl(url: String): String {
    if (!url.contains("image.civitai.com")) return url

    val baseUrl =
        if (url.contains("/original=true/")) url.substringBefore("/original=true/")
        else url.substringBeforeLast("/")

    return "$baseUrl/original=true"
}

fun getOriginalUrl(url: String): String {
    if (!url.contains("image.civitai.com")) return url

    return when {
        url.contains(Regex("/width=\\d+/")) -> url.replace(Regex("/width=\\d+/"), "/original=true/")
        url.contains("/original=false/") -> url.replace("/original=false/", "/original=true/")
        else -> url
    }
}

fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val totalMinutes = totalSeconds / 60
    val minutes = totalMinutes % 60
    val hours = totalMinutes / 60
    val centiseconds = (ms % 1000) / 10

    return when {
        hours > 0 -> String.format("%d:%02d:%02d:%02d", hours, minutes, seconds, centiseconds)
        minutes > 0 -> String.format("%02d:%02d:%02d", minutes, seconds, centiseconds)
        else -> String.format("%02d:%02d", seconds, centiseconds)
    }
}

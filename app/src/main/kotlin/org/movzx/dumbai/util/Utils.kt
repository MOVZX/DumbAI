package org.movzx.dumbai.util

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
import java.io.File
import org.movzx.dumbai.model.CivitaiImage
import org.movzx.dumbai.model.FavoriteImage

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
    val extensions =
        if (isVideo) listOf("mp4", "webm", "mkv") else listOf("jpg", "png", "webp", "gif", "avif")

    return extensions.any { ext ->
        val file = File(favoritesDir, "$imageId.$ext")

        file.exists() && file.length() > 100
    }
}

fun hasFullCache(context: Context, imageId: Long, isVideo: Boolean): Boolean {
    if (isVideo) return hasLocalCache(context, imageId, true)

    val favoritesDir = File(context.filesDir, "favorites")
    val extensions = listOf("jpg", "png", "webp", "gif", "avif")

    return extensions.any { ext ->
        val file = File(favoritesDir, "${imageId}_full.$ext")

        file.exists() && file.length() > 100
    }
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
        Logger.d("DumbAI_Res", "ID: ${image.id} | Local (Primary) | Path: ${primaryFile.name}")

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
            Logger.d("DumbAI_Res", "ID: ${image.id} | Local (Ext Search) | Path: ${file.name}")

            return file.absolutePath
        }
    }

    if (thumbnailWidth > 400) {
        val fallbackPath =
            favoriteInfo?.localPath ?: File(favoritesDir, "${image.id}.jpg").absolutePath

        val fallbackFile = File(fallbackPath)

        if (fallbackFile.exists() && fallbackFile.length() > 100) {
            Logger.d(
                "DumbAI_Res",
                "ID: ${image.id} | Local (Fallback) | Path: ${fallbackFile.name}",
            )

            return fallbackFile.absolutePath
        }

        for (ext in listOf("jpg", "png", "webp", "gif", "avif")) {
            val file = File(favoritesDir, "${image.id}.$ext")

            if (file.exists() && file.length() > 100) {
                Logger.d(
                    "DumbAI_Res",
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

        Logger.d("DumbAI_Res", "ID: ${image.id} | Remote | URL: $remoteUrl")

        return remoteUrl
    }

    return image.url
}

fun getThumbnailUrl(url: String, width: Int): String {
    if (!url.contains("image.civitai.com") && !url.contains("image.civitai.red")) return url

    val baseUrl =
        when {
            url.contains("/original=true/") -> url.replace("/original=true/", "/width=$width/")
            url.contains(Regex("/width=\\d+/")) ->
                url.replace(Regex("/width=\\d+/"), "/width=$width/")
            else -> {
                val lastSlashIndex = url.lastIndexOf('/')

                if (lastSlashIndex != -1) {
                    val prefix = url.substring(0, lastSlashIndex)
                    val fileName = url.substring(lastSlashIndex + 1)

                    "$prefix/width=$width/$fileName"
                } else url
            }
        }

    return baseUrl
}

fun getVideoThumbnailUrl(url: String): String {
    if (!url.contains("image.civitai.com") && !url.contains("image.civitai.red")) return url

    val result =
        when {
            url.contains("/original=false/") -> url
            url.contains("/original=true/") -> url.replace("/original=true/", "/original=false/")
            url.contains(Regex("/width=\\d+/")) ->
                url.replace(Regex("/width=\\d+/"), "/original=false/")
            else -> {
                val lastSlashIndex = url.lastIndexOf('/')

                if (lastSlashIndex != -1) {
                    val prefix = url.substring(0, lastSlashIndex)
                    val fileName = url.substring(lastSlashIndex + 1)

                    "$prefix/original=false/$fileName"
                } else url
            }
        }

    return result
}

fun getVideoPreviewUrl(url: String): String {
    return url
}

fun getRequiredStoragePermissions(): List<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

fun hasStoragePermissions(context: Context): Boolean {
    return getRequiredStoragePermissions().all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}

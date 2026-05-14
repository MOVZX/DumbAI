package org.movzx.dibella.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.movzx.dibella.R

val DibellaTypography =
    androidx.compose.material3.Typography(
        displayLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.SemiBold,
                fontSize = 48.sp,
                lineHeight = 56.sp,
            ),
        displayMedium =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 32.sp,
                lineHeight = 40.sp,
            ),
        displaySmall =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 28.sp,
                lineHeight = 36.sp,
            ),
        headlineLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 24.sp,
                lineHeight = 32.sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                lineHeight = 28.sp,
            ),
        headlineSmall =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        titleSmall =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        bodySmall =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                lineHeight = 14.sp,
            ),
    )

val DibellaShapes =
    androidx.compose.material3.Shapes(
        extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        large = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
    )

@Composable
fun DibellaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val darkFallback =
        darkColorScheme(
            primary = colorResource(R.color.primary),
            onPrimary = colorResource(R.color.onPrimary),
            primaryContainer = colorResource(R.color.primaryContainer),
            onPrimaryContainer = colorResource(R.color.onPrimaryContainer),
            secondary = colorResource(R.color.secondary),
            onSecondary = colorResource(R.color.onSecondary),
            secondaryContainer = colorResource(R.color.secondaryContainer),
            onSecondaryContainer = colorResource(R.color.onSecondaryContainer),
            tertiary = colorResource(R.color.tertiary),
            onTertiary = colorResource(R.color.onTertiary),
            tertiaryContainer = colorResource(R.color.tertiaryContainer),
            onTertiaryContainer = colorResource(R.color.onTertiaryContainer),
            background = Color(0xFF0A0A0F),
            onBackground = Color(0xFFF4F4F5),
            surface = Color(0xFF141418),
            onSurface = Color(0xFFF4F4F5),
            surfaceVariant = Color(0xFF1E1E24),
            onSurfaceVariant = Color(0xFFA1A1AA),
            outline = Color(0xFF3F3F46),
            outlineVariant = Color(0xFF27272A),
            error = colorResource(R.color.error),
        )

    val lightFallback =
        lightColorScheme(
            primary = colorResource(R.color.primary),
            onPrimary = colorResource(R.color.onPrimary),
            primaryContainer = colorResource(R.color.primaryContainer),
            onPrimaryContainer = colorResource(R.color.onPrimaryContainer),
            secondary = colorResource(R.color.secondary),
            onSecondary = colorResource(R.color.onSecondary),
            secondaryContainer = colorResource(R.color.secondaryContainer),
            onSecondaryContainer = colorResource(R.color.onSecondaryContainer),
            tertiary = colorResource(R.color.tertiary),
            onTertiary = colorResource(R.color.onTertiary),
            tertiaryContainer = colorResource(R.color.tertiaryContainer),
            onTertiaryContainer = colorResource(R.color.onTertiaryContainer),
            background = colorResource(R.color.background),
            onBackground = colorResource(R.color.onBackground),
            surface = colorResource(R.color.surface),
            onSurface = colorResource(R.color.onSurface),
            surfaceVariant = colorResource(R.color.surfaceVariant),
            onSurfaceVariant = colorResource(R.color.onSurfaceVariant),
            outline = colorResource(R.color.outline),
            error = colorResource(R.color.error),
        )

    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> darkFallback
            else -> lightFallback
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DibellaTypography,
        shapes = DibellaShapes,
        content = content,
    )
}

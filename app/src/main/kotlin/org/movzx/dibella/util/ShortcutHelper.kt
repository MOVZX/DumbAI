package org.movzx.dibella.util

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import org.movzx.dibella.MainActivity
import org.movzx.dibella.R

object ShortcutHelper {
    const val EXTRA_ROUTE = "org.movzx.dibella.SHORTCUT_ROUTE"

    fun pushShortcuts(context: Context) {
        val shortcuts =
            listOf(
                ShortcutInfoCompat.Builder(context, "shortcut_feed")
                    .setShortLabel(context.getString(R.string.nav_feed))
                    .setLongLabel(context.getString(R.string.nav_feed))
                    .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_feed))
                    .setIntent(
                        Intent(context, MainActivity::class.java).apply {
                            action = Intent.ACTION_VIEW
                            putExtra(EXTRA_ROUTE, "feed")
                            flags =
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                    )
                    .build(),
                ShortcutInfoCompat.Builder(context, "shortcut_favorites")
                    .setShortLabel(context.getString(R.string.nav_favorites))
                    .setLongLabel(context.getString(R.string.nav_favorites))
                    .setIcon(
                        IconCompat.createWithResource(context, R.drawable.ic_shortcut_favorites)
                    )
                    .setIntent(
                        Intent(context, MainActivity::class.java).apply {
                            action = Intent.ACTION_VIEW
                            putExtra(EXTRA_ROUTE, "favorites")
                            flags =
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                    )
                    .build(),
                ShortcutInfoCompat.Builder(context, "shortcut_gallery")
                    .setShortLabel(context.getString(R.string.nav_gallery))
                    .setLongLabel(context.getString(R.string.nav_gallery))
                    .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_gallery))
                    .setIntent(
                        Intent(context, MainActivity::class.java).apply {
                            action = Intent.ACTION_VIEW
                            putExtra(EXTRA_ROUTE, "gallery")
                            flags =
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                    )
                    .build(),
            )

        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }
}

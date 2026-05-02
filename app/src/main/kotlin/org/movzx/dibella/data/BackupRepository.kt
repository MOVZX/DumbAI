package org.movzx.dibella.data

import android.content.Context
import android.net.Uri
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source
import org.movzx.dibella.model.AppBackup
import org.movzx.dibella.model.FavoriteImage
import org.movzx.dibella.model.FavoriteImageBackup
import org.movzx.dibella.util.CivitaiUrlBuilder

@Singleton
class BackupRepository
@Inject
constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: UserPreferencesRepository,
    private val favoritesRepository: FavoritesRepository,
    private val moshi: Moshi,
) {
    suspend fun exportData(uri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val favorites =
                    favoritesRepository.getAllFavoritesSync().map { fav ->
                        FavoriteImageBackup(
                            id = fav.id,
                            url = CivitaiUrlBuilder.compressUrl(fav.url),
                            nsfw = fav.nsfw,
                            type = fav.type,
                            timestamp = fav.timestamp,
                        )
                    }

                val settings = repository.getCurrentSettings()
                val backup = AppBackup(version = 2, settings = settings, favorites = favorites)
                val adapter = moshi.adapter(AppBackup::class.java)
                val json = adapter.toJson(backup)

                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }

                true
            } catch (e: Exception) {
                false
            }
        }

    suspend fun importData(uri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val source = inputStream.source().buffer()
                    val adapter = moshi.adapter(AppBackup::class.java)
                    val backup = adapter.fromJson(source) ?: return@withContext false

                    backup.settings?.let { repository.importSettings(it) }

                    val favoriteImages =
                        backup.favorites.map { back ->
                            FavoriteImage(
                                id = back.id,
                                url = CivitaiUrlBuilder.expandUrl(back.url, back.type),
                                width = null,
                                height = null,
                                nsfw = back.nsfw,
                                type = back.type,
                                timestamp = back.timestamp,
                                isSynced = false,
                            )
                        }

                    favoritesRepository.importFavorites(favoriteImages)

                    true
                } ?: false
            } catch (e: Exception) {
                false
            }
        }
}

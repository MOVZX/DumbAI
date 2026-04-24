package org.movzx.dumbai.data

import android.content.Context
import android.net.Uri
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.movzx.dumbai.model.AppBackup

@Singleton
class BackupRepository
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val repository: UserPreferencesRepository,
    private val favoritesRepository: FavoritesRepository,
    private val moshi: Moshi,
) {
    suspend fun exportData(uri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val favorites = favoritesRepository.getAllFavoritesSync()
                val settings = repository.getCurrentSettings()

                val backup =
                    AppBackup(
                        version = 1,
                        settings = settings,
                        favorites = favorites.map { it.copy(localPath = null) },
                    )

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
                val json =
                    context.contentResolver.openInputStream(uri)?.use {
                        it.bufferedReader().readText()
                    } ?: return@withContext false

                val adapter = moshi.adapter(AppBackup::class.java)
                val backup = adapter.fromJson(json) ?: return@withContext false

                backup.settings?.let { repository.importSettings(it) }
                favoritesRepository.importFavorites(backup.favorites)

                true
            } catch (e: Exception) {
                false
            }
        }
}

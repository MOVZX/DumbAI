package org.movzx.dumbai.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.movzx.dumbai.model.AppSettingsBackup

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferencesRepository(private val context: Context) {
    private object PreferencesKeys {
        val NSFW = stringPreferencesKey("nsfw")
        val SORT = stringPreferencesKey("sort")
        val PERIOD = stringPreferencesKey("period")
        val TYPE = stringPreferencesKey("type")
        val TAG_IDS = stringPreferencesKey("tag_ids")
        val API_KEY = stringPreferencesKey("api_key")
        val FEED_SCROLL_INDEX_IMAGE = intPreferencesKey("feed_scroll_index_image")
        val FEED_SCROLL_OFFSET_IMAGE = intPreferencesKey("feed_scroll_offset_image")
        val FEED_SCROLL_INDEX_VIDEO = intPreferencesKey("feed_scroll_index_video")
        val FEED_SCROLL_OFFSET_VIDEO = intPreferencesKey("feed_scroll_offset_video")
        val FAVORITES_SCROLL_INDEX = intPreferencesKey("favorites_scroll_index")
        val FAVORITES_SCROLL_OFFSET = intPreferencesKey("favorites_scroll_offset")
        val GALLERY_SCROLL_INDEX = intPreferencesKey("gallery_scroll_index")
        val GALLERY_SCROLL_OFFSET = intPreferencesKey("gallery_scroll_offset")
        val NEXT_CURSOR_IMAGE = stringPreferencesKey("next_cursor_image")
        val NEXT_CURSOR_VIDEO = stringPreferencesKey("next_cursor_video")
        val PAGE_LIMIT = intPreferencesKey("page_limit")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val DOWNLOAD_PATH = stringPreferencesKey("download_path")
        val DEBUG_ENABLED = booleanPreferencesKey("debug_enabled")
    }

    val nsfw: Flow<String> =
        context.dataStore.data.map { preferences -> preferences[PreferencesKeys.NSFW] ?: "None" }

    val sort: Flow<String> =
        context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.SORT] ?: "Most Reactions"
        }

    val period: Flow<String> =
        context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.PERIOD] ?: "AllTime"
        }

    val type: Flow<String> =
        context.dataStore.data.map { preferences -> preferences[PreferencesKeys.TYPE] ?: "image" }

    val tagIds: Flow<String?> =
        context.dataStore.data.map { preferences -> preferences[PreferencesKeys.TAG_IDS] }

    val apiKey: Flow<String> =
        context.dataStore.data.map { preferences -> preferences[PreferencesKeys.API_KEY] ?: "" }

    fun feedScrollIndex(type: String): Flow<Int> =
        context.dataStore.data.map { preferences ->
            if (type == "video") preferences[PreferencesKeys.FEED_SCROLL_INDEX_VIDEO] ?: 0
            else preferences[PreferencesKeys.FEED_SCROLL_INDEX_IMAGE] ?: 0
        }

    fun feedScrollOffset(type: String): Flow<Int> =
        context.dataStore.data.map { preferences ->
            if (type == "video") preferences[PreferencesKeys.FEED_SCROLL_OFFSET_VIDEO] ?: 0
            else preferences[PreferencesKeys.FEED_SCROLL_OFFSET_IMAGE] ?: 0
        }

    val favoritesScrollIndex: Flow<Int> =
        context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.FAVORITES_SCROLL_INDEX] ?: 0
        }

    val favoritesScrollOffset: Flow<Int> =
        context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.FAVORITES_SCROLL_OFFSET] ?: 0
        }

    val galleryScrollIndex: Flow<Int> =
        context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.GALLERY_SCROLL_INDEX] ?: 0
        }

    val galleryScrollOffset: Flow<Int> =
        context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.GALLERY_SCROLL_OFFSET] ?: 0
        }

    fun nextCursor(type: String): Flow<String?> =
        context.dataStore.data.map { preferences ->
            if (type == "video") preferences[PreferencesKeys.NEXT_CURSOR_VIDEO]
            else preferences[PreferencesKeys.NEXT_CURSOR_IMAGE]
        }

    val pageLimit: Flow<Int> =
        context.dataStore.data.map { preferences -> preferences[PreferencesKeys.PAGE_LIMIT] ?: 100 }

    val gridColumns: Flow<Int> =
        context.dataStore.data.map { preferences -> preferences[PreferencesKeys.GRID_COLUMNS] ?: 3 }

    val downloadPath: Flow<String?> =
        context.dataStore.data.map { preferences -> preferences[PreferencesKeys.DOWNLOAD_PATH] }

    val debugEnabled: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.DEBUG_ENABLED] ?: false
        }

    suspend fun getCurrentSettings(): AppSettingsBackup {
        val prefs = context.dataStore.data.first()

        return AppSettingsBackup(
            nsfw = prefs[PreferencesKeys.NSFW] ?: "None",
            sort = prefs[PreferencesKeys.SORT] ?: "Most Reactions",
            period = prefs[PreferencesKeys.PERIOD] ?: "AllTime",
            type = prefs[PreferencesKeys.TYPE] ?: "image",
            tagIds = prefs[PreferencesKeys.TAG_IDS],
            pageLimit = prefs[PreferencesKeys.PAGE_LIMIT] ?: 100,
            gridColumns = prefs[PreferencesKeys.GRID_COLUMNS] ?: 3,
            apiKey = prefs[PreferencesKeys.API_KEY]?.takeIf { it.isNotBlank() },
        )
    }

    suspend fun importSettings(settings: AppSettingsBackup) {
        android.util.Log.d("DumbAI_Prefs", "importSettings triggered")

        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NSFW] = settings.nsfw
            preferences[PreferencesKeys.SORT] = settings.sort
            preferences[PreferencesKeys.PERIOD] = settings.period
            preferences[PreferencesKeys.TYPE] = settings.type

            if (settings.tagIds != null) preferences[PreferencesKeys.TAG_IDS] = settings.tagIds
            else preferences.remove(PreferencesKeys.TAG_IDS)

            preferences[PreferencesKeys.PAGE_LIMIT] = settings.pageLimit
            preferences[PreferencesKeys.GRID_COLUMNS] = settings.gridColumns
            settings.apiKey?.let { preferences[PreferencesKeys.API_KEY] = it }
        }
    }

    suspend fun updateFilters(
        nsfw: String,
        sort: String,
        period: String,
        type: String,
        tagIds: String?,
    ) {
        android.util.Log.d(
            "DumbAI_Prefs",
            "updateFilters: nsfw=$nsfw sort=$sort period=$period type=$type tags=$tagIds",
        )

        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NSFW] = nsfw
            preferences[PreferencesKeys.SORT] = sort
            preferences[PreferencesKeys.PERIOD] = period
            preferences[PreferencesKeys.TYPE] = type

            if (tagIds == null) preferences.remove(PreferencesKeys.TAG_IDS)
            else preferences[PreferencesKeys.TAG_IDS] = tagIds
        }
    }

    suspend fun updateScrollPosition(type: String, index: Int, offset: Int) {
        context.dataStore.edit { preferences ->
            if (type == "video") {
                preferences[PreferencesKeys.FEED_SCROLL_INDEX_VIDEO] = index
                preferences[PreferencesKeys.FEED_SCROLL_OFFSET_VIDEO] = offset
            } else {
                preferences[PreferencesKeys.FEED_SCROLL_INDEX_IMAGE] = index
                preferences[PreferencesKeys.FEED_SCROLL_OFFSET_IMAGE] = offset
            }
        }
    }

    suspend fun updateFavoritesScrollPosition(index: Int, offset: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FAVORITES_SCROLL_INDEX] = index
            preferences[PreferencesKeys.FAVORITES_SCROLL_OFFSET] = offset
        }
    }

    suspend fun updateGalleryScrollPosition(index: Int, offset: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GALLERY_SCROLL_INDEX] = index
            preferences[PreferencesKeys.GALLERY_SCROLL_OFFSET] = offset
        }
    }

    suspend fun updateNextCursor(type: String, cursor: String?) {
        android.util.Log.d("DumbAI_Prefs", "updateNextCursor for $type: $cursor")

        context.dataStore.edit { preferences ->
            val key =
                if (type == "video") PreferencesKeys.NEXT_CURSOR_VIDEO
                else PreferencesKeys.NEXT_CURSOR_IMAGE

            if (cursor == null) preferences.remove(key) else preferences[key] = cursor
        }
    }

    suspend fun updatePageLimit(limit: Int) {
        android.util.Log.d("DumbAI_Prefs", "updatePageLimit: $limit")

        context.dataStore.edit { preferences -> preferences[PreferencesKeys.PAGE_LIMIT] = limit }
    }

    suspend fun updateGridColumns(columns: Int) {
        android.util.Log.d("DumbAI_Prefs", "updateGridColumns: $columns")

        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GRID_COLUMNS] = columns
        }
    }

    suspend fun updateDownloadPath(path: String?) {
        android.util.Log.d("DumbAI_Prefs", "updateDownloadPath: $path")

        context.dataStore.edit { preferences ->
            if (path == null) preferences.remove(PreferencesKeys.DOWNLOAD_PATH)
            else preferences[PreferencesKeys.DOWNLOAD_PATH] = path
        }
    }

    suspend fun updateApiKey(key: String) {
        android.util.Log.d(
            "DumbAI_Prefs",
            "updateApiKey: ${if (key.isBlank()) "cleared" else "set"}",
        )

        context.dataStore.edit { preferences -> preferences[PreferencesKeys.API_KEY] = key }
    }

    suspend fun updateDebugEnabled(enabled: Boolean) {
        android.util.Log.d("DumbAI_Prefs", "updateDebugEnabled: $enabled")

        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEBUG_ENABLED] = enabled
        }
    }

    suspend fun getInterceptorSettings() =
        context.dataStore.data
            .map { prefs ->
                Triple(
                    prefs[PreferencesKeys.NSFW] ?: "None",
                    prefs[PreferencesKeys.API_KEY] ?: "",
                    prefs[PreferencesKeys.DEBUG_ENABLED] ?: false,
                )
            }
            .first()
}

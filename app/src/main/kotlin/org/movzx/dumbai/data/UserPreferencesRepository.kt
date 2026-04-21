package org.movzx.dumbai.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.movzx.dumbai.model.AppSettingsBackup
import kotlinx.coroutines.flow.first

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferencesRepository(private val context: Context) {
    private object PreferencesKeys {
        val NSFW = stringPreferencesKey("nsfw")
        val SORT = stringPreferencesKey("sort")
        val PERIOD = stringPreferencesKey("period")
        val TYPE = stringPreferencesKey("type")
        val TAG_ID = intPreferencesKey("tag_id")
        val CACHE_LIMIT_GB = intPreferencesKey("cache_limit_gb")
        val API_KEY = stringPreferencesKey("api_key")
        val FEED_SCROLL_INDEX_IMAGE = intPreferencesKey("feed_scroll_index_image")
        val FEED_SCROLL_OFFSET_IMAGE = intPreferencesKey("feed_scroll_offset_image")
        val FEED_SCROLL_INDEX_VIDEO = intPreferencesKey("feed_scroll_index_video")
        val FEED_SCROLL_OFFSET_VIDEO = intPreferencesKey("feed_scroll_offset_video")
        val NEXT_CURSOR_IMAGE = stringPreferencesKey("next_cursor_image")
        val NEXT_CURSOR_VIDEO = stringPreferencesKey("next_cursor_video")
        val PAGE_LIMIT = intPreferencesKey("page_limit")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val DOWNLOAD_PATH = stringPreferencesKey("download_path")
    }

    val nsfw: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.NSFW] ?: "None"
    }

    val sort: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SORT] ?: "Most Reactions"
    }

    val period: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.PERIOD] ?: "AllTime"
    }

    val type: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.TYPE] ?: "image"
    }

    val tagId: Flow<Int?> = context.dataStore.data.map { preferences ->
        val id = preferences[PreferencesKeys.TAG_ID] ?: -1

        if (id == -1)
            null
        else
            id
    }

    val cacheLimitGb: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CACHE_LIMIT_GB] ?: 10
    }

    val apiKey: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.API_KEY] ?: ""
    }

    fun feedScrollIndex(type: String): Flow<Int> = context.dataStore.data.map { preferences ->
        if (type == "video")
            preferences[PreferencesKeys.FEED_SCROLL_INDEX_VIDEO] ?: 0
        else
            preferences[PreferencesKeys.FEED_SCROLL_INDEX_IMAGE] ?: 0
    }

    fun feedScrollOffset(type: String): Flow<Int> = context.dataStore.data.map { preferences ->
        if (type == "video")
            preferences[PreferencesKeys.FEED_SCROLL_OFFSET_VIDEO] ?: 0
        else
            preferences[PreferencesKeys.FEED_SCROLL_OFFSET_IMAGE] ?: 0
    }

    fun nextCursor(type: String): Flow<String?> = context.dataStore.data.map { preferences ->
        if (type == "video")
            preferences[PreferencesKeys.NEXT_CURSOR_VIDEO]
        else
            preferences[PreferencesKeys.NEXT_CURSOR_IMAGE]
    }

    val pageLimit: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.PAGE_LIMIT] ?: 60
    }

    val gridColumns: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.GRID_COLUMNS] ?: 2
    }

    val downloadPath: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DOWNLOAD_PATH]
    }

    suspend fun getCurrentSettings(): AppSettingsBackup {
        val prefs = context.dataStore.data.first()

        return AppSettingsBackup(
            nsfw = prefs[PreferencesKeys.NSFW] ?: "None",
            sort = prefs[PreferencesKeys.SORT] ?: "Most Reactions",
            period = prefs[PreferencesKeys.PERIOD] ?: "AllTime",
            type = prefs[PreferencesKeys.TYPE] ?: "image",
            tagId = prefs[PreferencesKeys.TAG_ID]?.takeIf { it != -1 },
            pageLimit = prefs[PreferencesKeys.PAGE_LIMIT] ?: 60,
            gridColumns = prefs[PreferencesKeys.GRID_COLUMNS] ?: 2
        )
    }

    suspend fun importSettings(settings: AppSettingsBackup) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NSFW] = settings.nsfw
            preferences[PreferencesKeys.SORT] = settings.sort
            preferences[PreferencesKeys.PERIOD] = settings.period
            preferences[PreferencesKeys.TYPE] = settings.type
            preferences[PreferencesKeys.TAG_ID] = settings.tagId ?: -1
            preferences[PreferencesKeys.PAGE_LIMIT] = settings.pageLimit
            preferences[PreferencesKeys.GRID_COLUMNS] = settings.gridColumns
        }
    }

    suspend fun updateFilters(nsfw: String, sort: String, period: String, type: String, tagId: Int?) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NSFW] = nsfw
            preferences[PreferencesKeys.SORT] = sort
            preferences[PreferencesKeys.PERIOD] = period
            preferences[PreferencesKeys.TYPE] = type
            preferences[PreferencesKeys.TAG_ID] = tagId ?: -1
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

    suspend fun updateNextCursor(type: String, cursor: String?) {
        context.dataStore.edit { preferences ->
            val key = if (type == "video") PreferencesKeys.NEXT_CURSOR_VIDEO else PreferencesKeys.NEXT_CURSOR_IMAGE

            if (cursor == null)
                preferences.remove(key)
            else
                preferences[key] = cursor
        }
    }

    suspend fun updateCacheLimit(limit: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CACHE_LIMIT_GB] = limit
        }
    }

    suspend fun updatePageLimit(limit: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PAGE_LIMIT] = limit
        }
    }

    suspend fun updateGridColumns(columns: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GRID_COLUMNS] = columns
        }
    }

    suspend fun updateDownloadPath(path: String?) {
        context.dataStore.edit { preferences ->
            if (path == null)
                preferences.remove(PreferencesKeys.DOWNLOAD_PATH)
            else
                preferences[PreferencesKeys.DOWNLOAD_PATH] = path
        }
    }

    suspend fun updateApiKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.API_KEY] = key
        }
    }
}

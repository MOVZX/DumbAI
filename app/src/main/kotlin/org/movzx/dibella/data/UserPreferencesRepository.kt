package org.movzx.dibella.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import org.movzx.dibella.model.AppSettingsBackup
import org.movzx.dibella.util.Logger

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferencesRepository(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
        val FAVORITES_PATH = stringPreferencesKey("favorites_path")
        val DEBUG_ENABLED = booleanPreferencesKey("debug_enabled")
        val FAVORITES_TYPE = stringPreferencesKey("favorites_type")
        val GALLERY_TYPE = stringPreferencesKey("gallery_type")
        val LAST_ROUTE = stringPreferencesKey("last_route")
        val HIDE_PLAYER_CONTROLS = booleanPreferencesKey("hide_player_controls")
        val ALWAYS_ENABLE_HD = booleanPreferencesKey("always_enable_hd")
        val ALWAYS_MUTE_VIDEO = booleanPreferencesKey("always_mute_video")
        val FEED_VIDEO_AUTOPLAY = booleanPreferencesKey("feed_video_autoplay")
        val AMOLED_MODE = booleanPreferencesKey("amoled_mode")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val BACKEND_ENABLED = booleanPreferencesKey("backend_enabled")
        val BACKEND_URL = stringPreferencesKey("backend_url")
        val BACKEND_API_KEY = stringPreferencesKey("backend_api_key")
        val SHOW_NSFW_FAVORITES = booleanPreferencesKey("show_nsfw_favorites")
        val SEARCH_API_KEY = stringPreferencesKey("SEARCH_API_KEY")
        val SEARCH_QUERY = stringPreferencesKey("search_query")
        val SEARCH_TYPE = stringPreferencesKey("search_type")
        val SEARCH_SORT = stringPreferencesKey("search_sort")
        val SEARCH_SCROLL_INDEX = intPreferencesKey("search_scroll_index")
        val SEARCH_SCROLL_OFFSET = intPreferencesKey("search_scroll_offset")
        val SEARCH_HISTORY_COUNT = intPreferencesKey("search_history_count")
        val SEARCH_OFFSET = intPreferencesKey("search_offset")
    }

    val nsfw: Flow<String> =
        context.dataStore.data.map { preferences -> preferences[PreferencesKeys.NSFW] ?: "None" }

    val backendEnabled: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.BACKEND_ENABLED] ?: false
        }

    val backendUrl: Flow<String> =
        context.dataStore.data.map { preferences -> preferences[PreferencesKeys.BACKEND_URL] ?: "" }

    val backendApiKey: Flow<String> =
        context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.BACKEND_API_KEY] ?: ""
        }

    val lastRoute: Flow<String> =
        context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.LAST_ROUTE] ?: "feed"
        }

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

    val searchApiKey: Flow<String> =
        context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.SEARCH_API_KEY]
                ?: "8c46eb2508e21db1e9828a97968d91ab1ca1caa5f70a00e88a2ba1e286603b61"
        }

    val searchQuery: Flow<String> =
        context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.SEARCH_QUERY] ?: ""
        }

    val searchType: Flow<String> =
        context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.SEARCH_TYPE] ?: "image"
        }

    val searchSort: Flow<String> =
        context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.SEARCH_SORT] ?: "Relevancy"
        }

    fun feedScrollIndex(type: String): Flow<Int> =
        context.dataStore.data.map { preferences ->
            when (type) {
                "video" -> preferences[PreferencesKeys.FEED_SCROLL_INDEX_VIDEO] ?: 0
                "favorites" -> preferences[PreferencesKeys.FAVORITES_SCROLL_INDEX] ?: 0
                "gallery" -> preferences[PreferencesKeys.GALLERY_SCROLL_INDEX] ?: 0
                else -> preferences[PreferencesKeys.FEED_SCROLL_INDEX_IMAGE] ?: 0
            }
        }

    fun feedScrollOffset(type: String): Flow<Int> =
        context.dataStore.data.map { preferences ->
            when (type) {
                "video" -> preferences[PreferencesKeys.FEED_SCROLL_OFFSET_VIDEO] ?: 0
                "favorites" -> preferences[PreferencesKeys.FAVORITES_SCROLL_OFFSET] ?: 0
                "gallery" -> preferences[PreferencesKeys.GALLERY_SCROLL_OFFSET] ?: 0
                else -> preferences[PreferencesKeys.FEED_SCROLL_OFFSET_IMAGE] ?: 0
            }
        }

    fun nextCursor(type: String): Flow<String?> =
        context.dataStore.data.map { preferences ->
            if (type == "video") preferences[PreferencesKeys.NEXT_CURSOR_VIDEO]
            else preferences[PreferencesKeys.NEXT_CURSOR_IMAGE]
        }

    val pageLimit: Flow<Int> =
        context.dataStore.data.map { preferences -> preferences[PreferencesKeys.PAGE_LIMIT] ?: 200 }

    val gridColumns: Flow<Int> =
        context.dataStore.data.map { preferences -> preferences[PreferencesKeys.GRID_COLUMNS] ?: 3 }

    val downloadPath: Flow<String?> =
        context.dataStore.data.map { preferences -> preferences[PreferencesKeys.DOWNLOAD_PATH] }

    val favoritesPath: Flow<String?> =
        context.dataStore.data.map { preferences -> preferences[PreferencesKeys.FAVORITES_PATH] }

    val effectiveFavoritesPath: Flow<String> =
        context.dataStore.data.map { preferences ->
            val favPath = preferences[PreferencesKeys.FAVORITES_PATH]

            if (!favPath.isNullOrBlank()) {
                favPath
            } else {
                File(context.getExternalFilesDir(null), "Favorites").absolutePath
            }
        }

    val debugEnabled: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.DEBUG_ENABLED] ?: false
        }

    val favoritesType: Flow<String> =
        context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.FAVORITES_TYPE] ?: "all"
        }

    val galleryType: Flow<String> =
        context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.GALLERY_TYPE] ?: "all"
        }

    val hidePlayerControls: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.HIDE_PLAYER_CONTROLS] ?: false
        }

    val alwaysEnableHD: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.ALWAYS_ENABLE_HD] ?: false
        }

    val alwaysMuteVideo: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.ALWAYS_MUTE_VIDEO] ?: false
        }

    val feedVideoAutoplay: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.FEED_VIDEO_AUTOPLAY] ?: false
        }

    val amoledMode: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.AMOLED_MODE] ?: false
        }

    val onboardingCompleted: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
        }

    data class InterceptorSettings(
        val apiKey: String,
        val debugEnabled: Boolean,
        val backendEnabled: Boolean,
        val backendUrl: String,
        val backendApiKey: String,
    )

    private val _interceptorSettings =
        MutableStateFlow(InterceptorSettings("", false, false, "", ""))
    val interceptorSettings: StateFlow<InterceptorSettings> = _interceptorSettings.asStateFlow()

    init {
        combine(apiKey, debugEnabled, backendEnabled, backendUrl, backendApiKey) {
                key,
                debug,
                bEnabled,
                bUrl,
                bKey ->
                InterceptorSettings(key, debug, bEnabled, bUrl, bKey)
            }
            .onEach { settings ->
                _interceptorSettings.value = settings
                org.movzx.dibella.util.CivitaiUrlBuilder.backendEnabled = settings.backendEnabled
                org.movzx.dibella.util.CivitaiUrlBuilder.backendUrl = settings.backendUrl
                org.movzx.dibella.util.CivitaiUrlBuilder.backendApiKey = settings.backendApiKey
            }
            .launchIn(scope)
    }

    suspend fun getCurrentSettings(): AppSettingsBackup {
        val prefs = context.dataStore.data.first()

        return AppSettingsBackup(
            nsfw = prefs[PreferencesKeys.NSFW] ?: "None",
            sort = prefs[PreferencesKeys.SORT] ?: "Most Reactions",
            period = prefs[PreferencesKeys.PERIOD] ?: "AllTime",
            type = prefs[PreferencesKeys.TYPE] ?: "image",
            tagIds = prefs[PreferencesKeys.TAG_IDS],
            pageLimit = prefs[PreferencesKeys.PAGE_LIMIT] ?: 200,
            gridColumns = prefs[PreferencesKeys.GRID_COLUMNS] ?: 3,
            apiKey = prefs[PreferencesKeys.API_KEY]?.takeIf { it.isNotBlank() },
            favoritesPath = prefs[PreferencesKeys.FAVORITES_PATH],
            hidePlayerControls = prefs[PreferencesKeys.HIDE_PLAYER_CONTROLS] ?: false,
            alwaysEnableHD = prefs[PreferencesKeys.ALWAYS_ENABLE_HD] ?: false,
            alwaysMuteVideo = prefs[PreferencesKeys.ALWAYS_MUTE_VIDEO] ?: false,
            feedVideoAutoplay = prefs[PreferencesKeys.FEED_VIDEO_AUTOPLAY] ?: false,
            backendEnabled = prefs[PreferencesKeys.BACKEND_ENABLED] ?: false,
            backendUrl = prefs[PreferencesKeys.BACKEND_URL] ?: "",
            backendApiKey = prefs[PreferencesKeys.BACKEND_API_KEY] ?: "",
            feedScrollIndexImage = prefs[PreferencesKeys.FEED_SCROLL_INDEX_IMAGE] ?: 0,
            feedScrollOffsetImage = prefs[PreferencesKeys.FEED_SCROLL_OFFSET_IMAGE] ?: 0,
            feedScrollIndexVideo = prefs[PreferencesKeys.FEED_SCROLL_INDEX_VIDEO] ?: 0,
            feedScrollOffsetVideo = prefs[PreferencesKeys.FEED_SCROLL_OFFSET_VIDEO] ?: 0,
            favoritesScrollIndex = prefs[PreferencesKeys.FAVORITES_SCROLL_INDEX] ?: 0,
            favoritesScrollOffset = prefs[PreferencesKeys.FAVORITES_SCROLL_OFFSET] ?: 0,
            galleryScrollIndex = prefs[PreferencesKeys.GALLERY_SCROLL_INDEX] ?: 0,
            galleryScrollOffset = prefs[PreferencesKeys.GALLERY_SCROLL_OFFSET] ?: 0,
            nextCursorImage = prefs[PreferencesKeys.NEXT_CURSOR_IMAGE],
            nextCursorVideo = prefs[PreferencesKeys.NEXT_CURSOR_VIDEO],
            searchApiKey = prefs[PreferencesKeys.SEARCH_API_KEY],
        )
    }

    suspend fun importSettings(settings: AppSettingsBackup) {
        Logger.d("Dibella_Prefs", "importSettings triggered")

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
            settings.favoritesPath?.let { preferences[PreferencesKeys.FAVORITES_PATH] = it }
            preferences[PreferencesKeys.HIDE_PLAYER_CONTROLS] = settings.hidePlayerControls
            preferences[PreferencesKeys.ALWAYS_ENABLE_HD] = settings.alwaysEnableHD
            preferences[PreferencesKeys.ALWAYS_MUTE_VIDEO] = settings.alwaysMuteVideo
            preferences[PreferencesKeys.FEED_VIDEO_AUTOPLAY] = settings.feedVideoAutoplay
            preferences[PreferencesKeys.BACKEND_ENABLED] = settings.backendEnabled
            preferences[PreferencesKeys.BACKEND_URL] = settings.backendUrl
            preferences[PreferencesKeys.BACKEND_API_KEY] = settings.backendApiKey
            preferences[PreferencesKeys.FEED_SCROLL_INDEX_IMAGE] = settings.feedScrollIndexImage
            preferences[PreferencesKeys.FEED_SCROLL_OFFSET_IMAGE] = settings.feedScrollOffsetImage
            preferences[PreferencesKeys.FEED_SCROLL_INDEX_VIDEO] = settings.feedScrollIndexVideo
            preferences[PreferencesKeys.FEED_SCROLL_OFFSET_VIDEO] = settings.feedScrollOffsetVideo
            preferences[PreferencesKeys.FAVORITES_SCROLL_INDEX] = settings.favoritesScrollIndex
            preferences[PreferencesKeys.FAVORITES_SCROLL_OFFSET] = settings.favoritesScrollOffset
            preferences[PreferencesKeys.GALLERY_SCROLL_INDEX] = settings.galleryScrollIndex
            preferences[PreferencesKeys.GALLERY_SCROLL_OFFSET] = settings.galleryScrollOffset

            val imageCursorKey = PreferencesKeys.NEXT_CURSOR_IMAGE

            if (settings.nextCursorImage != null)
                preferences[imageCursorKey] = settings.nextCursorImage
            else preferences.remove(imageCursorKey)

            val videoCursorKey = PreferencesKeys.NEXT_CURSOR_VIDEO

            if (settings.nextCursorVideo != null)
                preferences[videoCursorKey] = settings.nextCursorVideo
            else preferences.remove(videoCursorKey)

            settings.searchApiKey?.let { preferences[PreferencesKeys.SEARCH_API_KEY] = it }
        }
    }

    suspend fun updateFilters(
        nsfw: String,
        sort: String,
        period: String,
        type: String,
        tagIds: String?,
    ) {
        Logger.d(
            "Dibella_Prefs",
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
            when (type) {
                "video" -> {
                    preferences[PreferencesKeys.FEED_SCROLL_INDEX_VIDEO] = index
                    preferences[PreferencesKeys.FEED_SCROLL_OFFSET_VIDEO] = offset
                }
                "favorites" -> {
                    preferences[PreferencesKeys.FAVORITES_SCROLL_INDEX] = index
                    preferences[PreferencesKeys.FAVORITES_SCROLL_OFFSET] = offset
                }
                "gallery" -> {
                    preferences[PreferencesKeys.GALLERY_SCROLL_INDEX] = index
                    preferences[PreferencesKeys.GALLERY_SCROLL_OFFSET] = offset
                }
                else -> {
                    preferences[PreferencesKeys.FEED_SCROLL_INDEX_IMAGE] = index
                    preferences[PreferencesKeys.FEED_SCROLL_OFFSET_IMAGE] = offset
                }
            }
        }
    }

    suspend fun updateNextCursor(type: String, cursor: String?) {
        Logger.d("Dibella_Prefs", "updateNextCursor for $type: $cursor")

        context.dataStore.edit { preferences ->
            val key =
                if (type == "video") PreferencesKeys.NEXT_CURSOR_VIDEO
                else PreferencesKeys.NEXT_CURSOR_IMAGE

            if (cursor == null) preferences.remove(key) else preferences[key] = cursor
        }
    }

    suspend fun updatePageLimit(limit: Int) {
        Logger.d("Dibella_Prefs", "updatePageLimit: $limit")

        context.dataStore.edit { preferences -> preferences[PreferencesKeys.PAGE_LIMIT] = limit }
    }

    suspend fun updateGridColumns(columns: Int) {
        Logger.d("Dibella_Prefs", "updateGridColumns: $columns")

        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GRID_COLUMNS] = columns
        }
    }

    suspend fun updateDownloadPath(path: String?) {
        Logger.d("Dibella_Prefs", "updateDownloadPath: $path")

        context.dataStore.edit { preferences ->
            if (path == null) preferences.remove(PreferencesKeys.DOWNLOAD_PATH)
            else preferences[PreferencesKeys.DOWNLOAD_PATH] = path
        }
    }

    suspend fun updateFavoritesPath(path: String?) {
        Logger.d("Dibella_Prefs", "updateFavoritesPath: $path")

        context.dataStore.edit { preferences ->
            if (path == null) preferences.remove(PreferencesKeys.FAVORITES_PATH)
            else preferences[PreferencesKeys.FAVORITES_PATH] = path
        }
    }

    suspend fun updateApiKey(key: String) {
        Logger.d("Dibella_Prefs", "updateApiKey: ${if (key.isBlank()) "cleared" else "set"}")

        context.dataStore.edit { preferences -> preferences[PreferencesKeys.API_KEY] = key }
    }

    suspend fun updateSearchApiKey(token: String) {
        Logger.d(
            "Dibella_Prefs",
            "updateSearchApiKey: ${if (token.isBlank()) "cleared" else "set"}",
        )

        context.dataStore.edit { preferences ->
            if (token.isBlank()) preferences.remove(PreferencesKeys.SEARCH_API_KEY)
            else preferences[PreferencesKeys.SEARCH_API_KEY] = token
        }
    }

    suspend fun updateBackendEnabled(enabled: Boolean) {
        Logger.d("Dibella_Prefs", "updateBackendEnabled: $enabled")

        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BACKEND_ENABLED] = enabled
        }
    }

    suspend fun updateBackendUrl(url: String) {
        Logger.d("Dibella_Prefs", "updateBackendUrl: $url")

        context.dataStore.edit { preferences -> preferences[PreferencesKeys.BACKEND_URL] = url }
    }

    suspend fun updateBackendApiKey(key: String) {
        Logger.d("Dibella_Prefs", "updateBackendApiKey: ${if (key.isBlank()) "cleared" else "set"}")

        context.dataStore.edit { preferences -> preferences[PreferencesKeys.BACKEND_API_KEY] = key }
    }

    suspend fun updateDebugEnabled(enabled: Boolean) {
        Logger.d("Dibella_Prefs", "updateDebugEnabled: $enabled")

        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEBUG_ENABLED] = enabled
        }
    }

    suspend fun updateFavoritesType(type: String) {
        Logger.d("Dibella_Prefs", "updateFavoritesType: $type")

        context.dataStore.edit { preferences -> preferences[PreferencesKeys.FAVORITES_TYPE] = type }
    }

    suspend fun updateGalleryType(type: String) {
        Logger.d("Dibella_Prefs", "updateGalleryType: $type")

        context.dataStore.edit { preferences -> preferences[PreferencesKeys.GALLERY_TYPE] = type }
    }

    suspend fun updateLastRoute(route: String) {
        Logger.d("Dibella_Prefs", "updateLastRoute: $route")

        context.dataStore.edit { preferences -> preferences[PreferencesKeys.LAST_ROUTE] = route }
    }

    suspend fun updateHidePlayerControls(enabled: Boolean) {
        Logger.d("Dibella_Prefs", "updateHidePlayerControls: $enabled")

        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HIDE_PLAYER_CONTROLS] = enabled
        }
    }

    suspend fun updateAlwaysEnableHD(enabled: Boolean) {
        Logger.d("Dibella_Prefs", "updateAlwaysEnableHD: $enabled")

        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ALWAYS_ENABLE_HD] = enabled
        }
    }

    suspend fun updateAlwaysMuteVideo(enabled: Boolean) {
        Logger.d("Dibella_Prefs", "updateAlwaysMuteVideo: $enabled")

        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ALWAYS_MUTE_VIDEO] = enabled
        }
    }

    suspend fun updateFeedVideoAutoplay(enabled: Boolean) {
        Logger.d("Dibella_Prefs", "updateFeedVideoAutoplay: $enabled")

        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FEED_VIDEO_AUTOPLAY] = enabled
        }
    }

    suspend fun updateAmoledMode(enabled: Boolean) {
        Logger.d("Dibella_Prefs", "updateAmoledMode: $enabled")

        context.dataStore.edit { preferences -> preferences[PreferencesKeys.AMOLED_MODE] = enabled }
    }

    suspend fun updateOnboardingCompleted(completed: Boolean) {
        Logger.d("Dibella_Prefs", "updateOnboardingCompleted: $completed")

        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    val showNsfwFavorites: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.SHOW_NSFW_FAVORITES] ?: true
        }

    suspend fun setShowNsfwFavorites(show: Boolean) {
        Logger.d("Dibella_Prefs", "setShowNsfwFavorites: $show")

        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_NSFW_FAVORITES] = show
        }
    }

    suspend fun updateSearchQuery(query: String) {
        Logger.d("Dibella_Prefs", "updateSearchQuery: ${if (query.isBlank()) "cleared" else "set"}")

        context.dataStore.edit { preferences ->
            if (query.isBlank()) {
                preferences.remove(PreferencesKeys.SEARCH_QUERY)
            } else {
                preferences[PreferencesKeys.SEARCH_QUERY] = query
            }
        }
    }

    suspend fun updateSearchFilters(type: String, sort: String) {
        Logger.d("Dibella_Prefs", "updateSearchFilters: type=$type sort=$sort")

        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SEARCH_TYPE] = type
            preferences[PreferencesKeys.SEARCH_SORT] = sort
        }
    }

    suspend fun updateSearchScrollPosition(index: Int, offset: Int) {
        Logger.d("Dibella_Prefs", "updateSearchScrollPosition: index=$index offset=$offset")

        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SEARCH_SCROLL_INDEX] = index
            preferences[PreferencesKeys.SEARCH_SCROLL_OFFSET] = offset
        }
    }

    fun searchScrollIndex(): Flow<Int> =
        context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.SEARCH_SCROLL_INDEX] ?: 0
        }

    fun searchScrollOffset(): Flow<Int> =
        context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.SEARCH_SCROLL_OFFSET] ?: 0
        }

    val searchHistoryCount: Flow<Int> =
        context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.SEARCH_HISTORY_COUNT] ?: 0
        }

    val searchOffset: Flow<Int> =
        context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.SEARCH_OFFSET] ?: 0
        }

    suspend fun incrementSearchHistoryCount() {
        val current = searchHistoryCount.first()

        Logger.d("Dibella_Prefs", "incrementSearchHistoryCount: $current -> ${current + 1}")

        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SEARCH_HISTORY_COUNT] = current + 1
        }
    }

    suspend fun updateSearchOffset(offset: Int) {
        Logger.d("Dibella_Prefs", "updateSearchOffset: $offset")

        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SEARCH_OFFSET] = offset
        }
    }
}

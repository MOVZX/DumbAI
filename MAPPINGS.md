# Function Mappings: Dibella

This document provides a comprehensive mapping of all function and method names to their respective line numbers and files within the Dibella project.

## app/src/main/kotlin/org/movzx/dibella/

### DibellaApplication.kt

- `L14`: `class DibellaApplication` - Main application class implementing SingletonImageLoader.Factory
- `L18`: `override fun newImageLoader(context: PlatformContext)` - Provides singleton Coil image loader
- `L22`: `override fun onTerminate()` - Called when the application is terminating

### MainActivity.kt

- `L14`: `class MainActivity` - Main activity entry point
- `L17`: `override fun onCreate(savedInstanceState: Bundle?)` - Sets up edge-to-edge UI and content
- `L24`: `enum class RightSidebarType` - Defines right sidebar modes (FILTERS, SETTINGS)

## app/src/main/kotlin/org/movzx/dibella/ui/screens/

### MainScreen.kt

- `L43`: `fun UiMessageEffect()` - Collects and displays UI messages as SnackBar
- `L55`: `fun InteractiveTopBar()` - Dynamic TopBar for Gallery/Favorites screens
- `L101`: `fun MainScreen()` - Root composable with shared transition scope
- `L483`: `fun AppNavigation()` - NavHost with feed/favorites/gallery destinations

### FeedScreen.kt

- `L19`: `fun FeedScreen()` - Feed composable with ViewModel and UI state

### FavoritesScreen.kt

- `L21`: `fun FavoritesScreen()` - Favorites list composable

### GalleryScreen.kt

- `L22`: `fun GalleryScreen()` - Local gallery browser composable

## app/src/main/kotlin/org/movzx/dibella/api/

### CivitaiApi.kt

- `L7`: `interface CivitaiApi` - Retrofit service interface
- `L9`: `suspend fun getImages()` - Fetches paginated images from Civitai API

### CivitaiInterceptor.kt

- `L12`: `class CivitaiInterceptor` - OkHttp interceptor for auth and logging
- `L15`: `override fun intercept()` - Injects Bearer token and adds User-Agent; reads settings via StateFlow

### CivitaiThumbnailInterceptor.kt

- `L15`: `class CivitaiThumbnailInterceptor` - Handles 404/403 fallbacks for image thumbnails
- `L17`: `companion object` - Constants for timeout and retry configuration
- `L22`: `val videoThumbnailTimeout` - Timeout for video thumbnail requests
- `L23`: `val imageThumbnailTimeout` - Timeout for image thumbnail requests
- `L24`: `val maxRetries` - Maximum retry attempts
- `L28`: `private fun cacheable()` - Adds custom Cache-Control headers to successful responses
- `L47`: `private fun Response?.closeQuietly()` - Extension to safely close OkHttp responses
- `L55`: `private fun safeProceed()` - Helper for executing network requests with timeout
- `L72`: `override fun intercept()` - Main interceptor logic with retry/fallback chain
- `L115`: `private fun isValidMedia()` - Verifies if response contains valid image or video data

## app/src/main/kotlin/org/movzx/dibella/data/

### AppDatabase.kt

- `L15`: `abstract class AppDatabase` - Room database with entities
- `L16`: `abstract fun favoriteImageDao()` - DAO for favorite images
- `L18`: `abstract fun feedCacheDao()` - DAO for feed cache
- `L23`: `fun getDatabase()` - Singleton database instance provider

### FavoriteImageDao.kt

- `L9`: `interface FavoriteImageDao` - Room DAO for favorite_images table
- `L11`: `fun getAllFavorites()` - Returns Flow of all favorites
- `L14`: `suspend fun _getAllFavoritesSync()` - Blocking fetch of all favorites (internal)
- `L16`: `suspend fun getAllFavoritesSync()` - Blocking fetch of all favorites
- `L25`: `suspend fun _insertFavorite()` - Inserts or replaces a favorite (internal)
- `L28`: `suspend fun insertFavorite()` - Inserts or replaces a favorite
- `L35`: `suspend fun _insertFavorites()` - Bulk insert of favorites (internal)
- `L38`: `suspend fun insertFavorites()` - Bulk insert of favorites
- `L44`: `suspend fun _deleteFavorite()` - Deletes a favorite (internal)
- `L47`: `suspend fun deleteFavorite()` - Deletes a favorite
- `L54`: `fun isFavorite()` - Flow checking if ID is favorited
- `L57`: `suspend fun isFavoriteDirect()` - Blocking favorite check
- `L59`: `fun getAllFavoriteIds()` - Flow of all favorite IDs
- `L62`: `suspend fun getFavorite()` - Fetch single favorite by ID
- `L65`: `fun getFavoriteFlow()` - Flow of single favorite by ID

### FeedCacheDao.kt

- `L8`: `interface FeedCacheDao` - Room DAO for feed_cache table
- `L10`: `suspend fun getFeed()` - Fetch cached feed by type
- `L13`: `suspend fun insertFeed()` - Cache feed items
- `L16`: `suspend fun clearFeed()` - Clear cache for feed type
- `L19`: `suspend fun replaceFeed()` - Replace cache atomically

### BackupRepository.kt

- `L17`: `class BackupRepository` - Handles JSON import/export of app data
- `L26`: `suspend fun exportData()` - Streams favorites and settings to JSON via Okio
- `L80`: `suspend fun importData()` - Imports JSON backup data

### FavoritesRepository.kt

- `L28`: `class FavoritesRepository` - Manages favorite images and local file caching
- `L30`: `val resourceCheckTimestamps` - Tracks timestamps for stale check cleanup
- `L31`: `val resourceChecksInProgress` - Tracks in-progress resource checks
- `L32`: `val toggleMutexes` - Per-image mutexes for thread-safe toggle operations
- `L33`: `val repositoryJob` - Supervisor job for repository coroutines
- `L34`: `val repositoryScope` - CoroutineScope for repository background work
- `L36`: `private var _okHttpClient` - Mutable OkHttpClient for auth updates
- `L38`: `private fun getToggleMutex()` - Gets or creates per-image toggle mutex
- `L41`: `init block` - Launches temp file cleanup and stale check cleanup tasks
- `L44`: `private fun cleanupStaleResourceChecks()` - Periodic cleanup of stale resource checks
- `L66`: `private fun cleanupTempFiles()` - Cleans orphaned temp files from cache
- `L88`: `suspend fun repairSync()` - Repairs local file sync and triggers metadata extraction
- `L113`: `private suspend fun getFavoritesDir()` - Resolves favorites directory path
- `L127`: `private fun ensureNomedia()` - Creates .nomedia file to hide from gallery
- `L140`: `private suspend fun getMediaDir()` - Resolves media subdirectory for type/content
- `L152`: `fun updateOkHttpClient()` - Updates HTTP client with new auth token
- `L156`: `override fun close()` - Cancels repository job scope
- `L160`: `val allFavorites` - Flow of all favorites as CivitaiImage
- `L162`: `val favoriteIds` - Flow of favorite IDs
- `L164`: `fun getFavoriteFlow()` - Returns Flow of favorite by ID
- `L166`: `suspend fun manualFetch()` - Manually fetches image metadata from URL
- `L185`: `suspend fun toggleFavorite()` - Adds/removes favorite, manages local resources
- `L193`: `private suspend fun evictFromCoilCache()` - Evicts URL from Coil cache
- `L206`: `private fun extractVideoFrame(videoFile: File, outputFile: File): Boolean` - Extracts thumbnail from video file
- `L249`: `suspend fun ensureFavoriteResources()` - Downloads thumbnail/preview if favorited
- `L357`: `private fun finalizeFile()` - Renames temp file to final destination
- `L377`: `private fun copyFile()` - Copies source to destination file
- `L399`: `private suspend fun downloadFile()` - Internal helper for downloading files with progress
- `L500`: `private suspend fun addFavorite()` - Adds item to database and triggers resource sync
- `L535`: `private suspend fun removeFavorite()` - Removes item from database and purges local files
- `L564`: `suspend fun clearUnusedResources()` - Deletes orphaned cached files
- `L595`: `suspend fun findDuplicateGroups()` - Finds duplicate images in favorites (parallel hash)
- `L625`: `suspend fun removeDuplicates()` - Removes duplicate groups
- `L660`: `suspend fun getAllFavoritesSync()` - Blocking fetch all favorites
- `L662`: `suspend fun importFavorites()` - Bulk import favorites
- `L670`: `fun isFavorite()` - Flow of favorite status for single ID

### GalleryRepository.kt

- `L13`: `class GalleryRepository` - Scans and manages local downloaded files
- `L18`: `val videoMetadataDispatcher` - Dispatcher for video metadata extraction
- `L20`: `val _downloadedIds` - Mutable state flow of downloaded IDs
- `L21`: `val downloadedIds` - Flow of downloaded IDs
- `L22`: `val refreshMutex` - Mutex for refresh operations
- `L23`: `val downloadMutexes` - ConcurrentHashMap of per-image download mutexes
- `L25`: `private fun getDownloadMutex()` - Gets or creates per-image download mutex
- `L30`: `suspend fun refreshDownloadedIds()` - Rescans download directory for IDs (uses withLock)
- `L55`: `suspend fun scanDirectory()` - Scans for media files in directory, extracts metadata
- `L160`: `suspend fun deleteLocalFile()` - Deletes local file and updates state
- `L184`: `suspend fun downloadImage()` - Downloads image/video with progress
- `L313`: `suspend fun findDuplicateGroups()` - Finds duplicate images in download directory
- `L354`: `suspend fun removeDuplicates()` - Removes duplicate groups
- `L385`: `private fun getDownloadDir()` - Resolves download directory from path

### UserPreferencesRepository.kt

- `L15`: `val Context.dataStore` - Extension property providing DataStore instance
- `L17`: `class UserPreferencesRepository` - Jetpack DataStore for settings persistence
- `L19`: `val scope` - CoroutineScope for StateFlow operations
- `L21`: `object PreferencesKeys` - Object holding all preference keys
- `L50`: `val nsfw` - Flow of NSFW filter setting
- `L54`: `val lastRoute` - Flow of last visited navigation route
- `L58`: `val sort` - Flow of sort setting
- `L64`: `val period` - Flow of period setting
- `L70`: `val type` - Flow of media type setting
- `L74`: `val tagIds` - Flow of tag IDs setting
- `L78`: `val apiKey` - Flow of API key setting
- `L82`: `fun feedScrollIndex(type: String): Flow<Int>` - Flow of scroll index for feed type
- `L92`: `fun feedScrollOffset(type: String): Flow<Int>` - Flow of scroll offset for feed type
- `L102`: `fun nextCursor(type: String): Flow<String?>` - Flow of pagination cursor for feed type
- `L108`: `val pageLimit` - Flow of page limit setting
- `L112`: `val gridColumns` - Flow of grid column count
- `L116`: `val downloadPath` - Flow of download directory path
- `L120`: `val favoritesPath` - Flow of favorites directory path
- `L124`: `val effectiveFavoritesPath` - Flow of effective favorites path (fallback logic)
- `L145`: `val debugEnabled` - Flow of debug logging setting
- `L151`: `val favoritesType` - Flow of favorites media type filter
- `L157`: `val galleryType` - Flow of gallery media type filter
- `L163`: `val hidePlayerControls` - Flow of player controls visibility
- `L169`: `val alwaysEnableHD` - Flow of HD media preference
- `L175`: `val alwaysMuteVideo` - Flow of video mute preference
- `L181`: `val feedVideoAutoplay` - Flow of video autoplay in feed
- `L187`: `val amoledMode` - Flow of AMOLED mode setting
- `L193`: `data class InterceptorSettings` - Data class holding API key and debug flag
- `L195`: `val interceptorSettings` - StateFlow of combined interceptor settings
- `L199`: `init block` - Combines apiKey and debugEnabled flows into interceptorSettings
- `L204`: `suspend fun getCurrentSettings()` - Snapshot of all settings for backup
- `L234`: `suspend fun importSettings()` - Bulk update settings from backup
- `L277`: `suspend fun updateFilters()` - Updates NSFW, sort, period, type, tags
- `L300`: `suspend fun updateScrollPosition()` - Debounced scroll position save
- `L323`: `suspend fun updateNextCursor()` - Updates pagination cursor
- `L335`: `suspend fun updatePageLimit()` - Updates page limit setting
- `L341`: `suspend fun updateGridColumns()` - Updates grid column count
- `L349`: `suspend fun updateDownloadPath()` - Updates download directory path
- `L358`: `suspend fun updateFavoritesPath()` - Updates favorites directory path
- `L367`: `suspend fun updateApiKey()` - Updates Civitai API key
- `L373`: `suspend fun updateDebugEnabled()` - Toggles debug logging
- `L381`: `suspend fun updateFavoritesType()` - Updates favorites media type filter
- `L387`: `suspend fun updateGalleryType()` - Updates gallery media type filter
- `L393`: `suspend fun updateLastRoute()` - Tracks last visited navigation route
- `L399`: `suspend fun updateHidePlayerControls()` - Toggles video player controls visibility
- `L407`: `suspend fun updateAlwaysEnableHD()` - Toggles high-definition media preference
- `L415`: `suspend fun updateAlwaysMuteVideo()` - Toggles video player mute preference
- `L423`: `suspend fun updateFeedVideoAutoplay()` - Toggles video autoplay in feed
- `L431`: `suspend fun updateAmoledMode()` - Toggles AMOLED black background mode

## app/src/main/kotlin/org/movzx/dibella/di/

### AppModule.kt

- `L26`: `object AppModule` - Hilt dependency injection module
- `L29`: `fun provideMoshi()` - Moshi JSON adapter with Kotlin support
- `L35`: `fun provideOkHttpClient()` - OkHttp with 10s timeouts and interceptors
- `L54`: `fun provideRetrofit()` - Retrofit with Civitai base URL
- `L64`: `fun provideCivitaiApi()` - Creates CivitaiApi service
- `L70`: `fun provideImageLoader()` - Coil image loader with OkHttp components
- `L97`: `fun provideDatabase()` - Room database singleton
- `L102`: `fun provideFavoriteImageDao()` - FavoriteImageDao provider
- `L107`: `fun provideFeedCacheDao()` - FeedCacheDao provider
- `L113`: `fun provideUserPreferencesRepository()` - DataStore repo provider
- `L121`: `fun provideFavoritesRepository()` - Favorites repo provider

## app/src/main/kotlin/org/movzx/dibella/ui/components/

### AppBackHandler.kt

- `L11`: `fun AppBackHandler()` - Handles system back button and selection mode

### AppFab.kt

- `L17`: `fun AppFab()` - Smart FAB with conditional scroll/refresh/load-more actions

### AppScaffold.kt

- `L19`: `fun AppScaffold()` - Wrapper managing TopBar, BottomBar, FAB
- `L48`: `fun EmptyState()` - Empty grid placeholder with icon and text

### BaseSidebar.kt

- `L12`: `fun BaseSidebar()` - Foundation for sidebars with RectangleShape

### ConfirmationDialog.kt

- `L13`: `fun ConfirmationDialog()` - Custom dialog with destructive action emphasis

### DisplaySidebar.kt

- `L18`: `fun DisplaySidebar()` - Config for columns, page limit, and media type filter

### FilterSidebar.kt

- `L19`: `fun FilterSidebar()` - NSFW, sort, period, type, tag filters

### FullScreenImage.kt

- `L41`: `fun FullScreenImage()` - Pager with zoom, gestures, favorite toggle

### ImageCard.kt

- `L43`: `fun ImageCard()` - Dynamic resolution, heart animation, error icon handling
- `L578`: `fun HeartParticles()` - Visual particles for favorite animation

### ImageGrid.kt

- `L24`: `fun ImageGrid()` - Regular grid with shared transitions

### MainBottomBar.kt

- `L19`: `fun MainBottomBar()` - Navigation between feed/favorites/gallery

### MainTopBar.kt

- `L22`: `fun SelectionTopBar()` - TopBar for batch selection mode
- `L80`: `fun MainTopBar()` - App logo, display/filter/settings triggers

### SettingsSidebar.kt

- `L20`: `fun SettingsSidebar()` - API key, download path, cache, import/export

### Shimmer.kt

- `L12`: `fun Modifier.shimmerBackground()` - Animated loading shimmer effect

### SidebarSection.kt

- `L10`: `fun SidebarSection()` - Titled section wrapper for sidebar content

### SkeletonGrid.kt

- `L15`: `fun SkeletonGrid()` - Static placeholder grid

### SpeedDialFab.kt

- `L26`: `fun SpeedDialFab()` - Expandable FAB menu

### VideoPlayer.kt

- `L28`: `fun VideoPlayer()` - ExoPlayer with controls, scaling, looping
- `L156`: `private fun ExoVideoPlayer()` - Lower-level Media3 player wrapper

### VideoPlayerManager.kt

- `L14`: `class VideoPlayerManager` - Pooled ExoPlayer management with thread-safe activeCount
- `L15`: `val pool` - Pool of reusable ExoPlayer instances
- `L16`: `val activePlayers` - Set of currently active players
- `L18`: `var maxPoolSize` - Configurable pool size limit
- `L21`: `var activeCount` - Synchronized counter of active players
- `L24`: `fun updateLimit()` - Updates max pool size and shrinks if needed
- `L34`: `fun acquirePlayer()` - Gets an available player from pool
- `L54`: `fun releasePlayer()` - Returns player to pool
- `L70`: `private fun createPlayer()` - Creates a new ExoPlayer with codec filter
- `L89`: `fun releaseAll()` - Releases all players and clears pool
- `L100`: `val LocalVideoPlayerManager` - CompositionLocal for VideoPlayerManager

### VideoPlayerModels.kt

- `L3`: `enum class ScaleMode` - NORMAL, CROP, FULL scaling options

### ZoomableImage.kt

- `L24`: `fun ZoomableImage()` - Pinch-to-zoom with double-tap

### MpvPlayer.kt

- `L15`: `fun MpvPlayer()` - MPV-based video player alternative

### Theme.kt

- `L15`: `fun DibellaTheme()` - Material 3 theme with dynamic colors

## app/src/main/kotlin/org/movzx/dibella/util/

### CivitaiUrlBuilder.kt

- `L3`: `object CivitaiUrlBuilder` - Logic for URL compression and variant building
- `L4`: `val CDN_HOST` - Civitai CDN hostname constant
- `L5`: `val FALLBACK_WIDTHS` - Default fallback width for thumbnails
- `L6`: `val WIDTH_REGEX` - Regex for matching width variants
- `L7`: `val URL_PREFIX` - URL prefix for original files
- `L9`: `fun compressUrl()` - Shortens Civitai URLs for database storage
- `L18`: `fun expandUrl()` - Restores full URLs from compressed format
- `L27`: `fun isCivitaiUrl()` - Checks if URL belongs to Civitai CDN
- `L29`: `fun getThumbnailUrl()` - Builds thumbnail variant URL
- `L33`: `fun getVideoThumbnailUrl()` - Builds video thumbnail variant URL
- `L39`: `fun getVideoPreviewUrl()` - Builds video preview variant URL
- `L47`: `fun getOriginalUrl()` - Builds original file variant URL
- `L56`: `fun getFallbackChain()` - Returns list of URLs to try for thumbnails
- `L83`: `private fun getBaseUrl()` - Extracts base URL from variant URL
- `L92`: `private fun modifyUrl()` - Applies variant to URL

### FileUtils.kt

- `L9`: `object FileUtils` - File magic byte detection and utility
- `L10`: `val IMAGE_EXTENSIONS` - List of supported image extensions
- `L11`: `val VIDEO_EXTENSIONS` - List of supported video extensions
- `L12`: `val JPEG_HEADER` - JPEG magic bytes constant
- `L13`: `val WEBP_HEADER` - WebP magic bytes constant
- `L15`: `fun detectExtension()` - Sniffs magic bytes for file type
- `L53`: `fun getExtensionFromBytes()` - Magic byte to extension mapping
- `L65`: `fun isRealMedia()` - Validates media file signatures
- `L107`: `fun calculateHash()` - Generates SHA-256 hash for duplicate detection
- `L139`: `suspend fun findDuplicateGroups()` - Groups files by content hash

### Logger.kt

- `L5`: `object Logger` - Conditional debug logging
- `L8`: `fun d()` - Debug log
- `L12`: `fun e()` - Error log
- `L16`: `fun w()` - Warning log
- `L20`: `fun v()` - Verbose log
- `L24`: `fun i()` - Info log

### UriUtils.kt

- `L8`: `fun resolveUriToPath()` - Converts document URI to file path

### Utils.kt

- `L5`: `fun Modifier.scrollbar()` - Custom scrollbar for scrollable layouts
- `L34`: `fun Modifier.gridScrollbar()` - Optimized scrollbar for LazyGrids
- `L69`: `private fun getEffectiveFavoritesDir()` - Resolves effective favorites directory
- `L81`: `suspend fun hasLocalCache()` - Checks thumbnail cache existence
- `L107`: `suspend fun hasFullCache()` - Checks full resolution media cache
- `L135`: `fun resolveImageData()` - Selects local/remote URL with optimization
- `L182`: `fun getThumbnailUrl()` - Appends width param to image URL
- `L186`: `fun getVideoThumbnailUrl()` - Video thumbnail URL builder
- `L190`: `fun getVideoPreviewUrl()` - Video preview URL builder
- `L194`: `fun getVideoOriginalUrl()` - Video original file URL builder
- `L198`: `fun getOriginalUrl()` - Appends original flag to image URL
- `L202`: `fun modifyCivitaiUrl()` - Generic URL variant modifier
- `L210`: `fun formatDuration()` - Formats milliseconds to human readable duration
- `L233`: `fun playerPoolSizeForColumns()` - Calculates optimal player pool size

## app/src/main/kotlin/org/movzx/dibella/viewmodel/

### BaseUiState.kt

- `L5`: `interface BaseUiState` - Common UI state properties

### FeedUiState.kt

- `L5`: `data class FeedUiState` - UI state for the main feed

### FavoritesUiState.kt

- `L5`: `data class FavoritesUiState` - UI state for the favorites screen

### GalleryUiState.kt

- `L5`: `data class GalleryUiState` - UI state for the local gallery

### SettingsUiState.kt

- `L3`: `data class SettingsUiState` - UI state for application settings

### BaseViewModel.kt

- `L17`: `abstract class BaseViewModel` - Base with shared actions
- `L26`: `fun updateGridColumns()` - Persists column preference
- `L30`: `fun updatePageLimit()` - Persists page limit preference
- `L34`: `fun saveScrollPosition()` - Debounced scroll position save
- `L38`: `protected fun sendMessage()` - Emits toast message
- `L42`: `fun toggleFavorite()` - Delegates to FavoritesRepository
- `L46`: `suspend fun ensureFavoriteResources()` - Downloads thumbnail/preview
- `L54`: `suspend fun ensureFavoriteResourcesThrottled()` - Throttled resource synchronization
- `L62`: `abstract fun downloadImage()` - Initiates image download (abstract)
- `L66`: `protected fun performDownload()` - Download with progress tracking

### FeedViewModel.kt

- `L23`: `class FeedViewModel` - Main feed with pagination and filters
- `L28`: `val uiState` - StateFlow of FeedUiState
- `L30`: `var imageFeed` - List of cached image feed items
- `L31`: `var videoFeed` - List of cached video feed items
- `L32`: `var imageCursor` - Pagination cursor for images
- `L33`: `var videoCursor` - Pagination cursor for videos
- `L34`: `var loadingJob` - Current loading job reference
- `L35`: `var isFirstSettingsLoad` - Flag for initial settings load
- `L37`: `init block` - Combines repository flows, initializes state from cache
- `L142`: `fun refresh()` - Clears cursor and reloads feed
- `L174`: `fun loadMore()` - Loads next page with cursor
- `L184`: `private fun loadImages()` - Fetches and caches feed items
- `L280`: `fun updateFilters()` - Updates filter params, invalidates restoration
- `L286`: `fun resetFilters()` - Resets all filters to defaults
- `L293`: `override fun downloadImage()` - Initiates image download
- `L305`: `fun markRestored()` - Sets isRestored flag after scroll restoration

### FavoritesViewModel.kt

- `L20`: `class FavoritesViewModel` - Manages favorites list
- `L24`: `val uiState` - StateFlow of FavoritesUiState
- `L28`: `init block` - Combines favorites flow, applies type filter
- `L67`: `fun updateType()` - Updates media type filter
- `L75`: `fun toggleSelection()` - Toggles selection for batch operations
- `L90`: `fun clearSelection()` - Clears all selections
- `L96`: `fun selectAll()` - Selects all visible items
- `L107`: `fun batchUnfavorite()` - Removes selected favorites
- `L135`: `fun getFavoriteFlow()` - Returns Flow of favorite by ID
- `L137`: `fun forceRedownload()` - Re-downloads favorite resource
- `L155`: `override fun downloadImage()` - Initiates image download
- `L170`: `fun findDuplicates()` - Finds duplicate images in favorites
- `L204`: `fun clearDuplicatesMode()` - Exits duplicate detection mode
- `L210`: `fun removeDuplicates()` - Removes duplicate groups
- `L244`: `fun markRestored()` - Sets isRestored flag after scroll restoration

### GalleryViewModel.kt

- `L16`: `class GalleryViewModel` - Manages local gallery browsing
- `L20`: `val uiState` - StateFlow of GalleryUiState
- `L24`: `init block` - Combines gallery flow, applies type filter
- `L57`: `fun refresh()` - Rescans download directory with filtering
- `L61`: `private suspend fun performRefresh()` - Executes gallery scan
- `L78`: `fun updateType()` - Updates media type filter
- `L86`: `fun toggleSelection()` - Toggles selection for batch operations
- `L98`: `fun clearSelection()` - Clears all selections
- `L104`: `fun selectAll()` - Selects all visible items
- `L116`: `fun batchDelete()` - Deletes selected local files
- `L138`: `fun deleteLocalFile()` - Deletes local media file
- `L146`: `override fun downloadImage()` - Initiates image download
- `L159`: `fun findDuplicates()` - Finds duplicate images in gallery
- `L193`: `fun clearDuplicatesMode()` - Exits duplicate detection mode
- `L199`: `fun removeDuplicates()` - Removes duplicate groups
- `L238`: `fun markRestored()` - Sets isRestored flag after scroll restoration

### SettingsViewModel.kt

- `L18`: `class SettingsViewModel` - Manages app settings and import/export
- `L22`: `val uiState` - StateFlow of SettingsUiState
- `L24`: `val exitEvent` - SharedFlow for app exit trigger
- `L28`: `init block` - Combines repository flows, loads settings
- `L73`: `override fun downloadImage()` - No-op override (gallery handles downloads)
- `L75`: `fun updateLastRoute()` - Tracks last visited navigation route
- `L79`: `fun updateApiKey()` - Updates Civitai API key
- `L86`: `fun updateDownloadPath()` - Updates download directory
- `L90`: `fun updateFavoritesPath()` - Updates favorites directory
- `L94`: `fun updateDebugEnabled()` - Toggles debug logging
- `L98`: `fun updateHidePlayerControls()` - Toggles video player controls visibility
- `L102`: `fun updateAlwaysEnableHD()` - Toggles high-definition media preference
- `L106`: `fun updateAlwaysMuteVideo()` - Toggles video player mute preference
- `L110`: `fun updateFeedVideoAutoplay()` - Toggles video autoplay in feed
- `L114`: `fun updateAmoledMode()` - Toggles AMOLED black background mode
- `L118`: `fun clearImageCache()` - Clears Coil image cache
- `L126`: `fun updateCacheSize()` - Computes and updates cache size
- `L133`: `private fun formatSize()` - Formats bytes to human-readable string
- `L142`: `fun exportData()` - Exports backup to URI
- `L151`: `fun importData()` - Imports backup from URI

## app/src/main/kotlin/org/movzx/dibella/model/

### FavoriteImage.kt

- `L9`: `data class FavoriteImage` - Room entity for favorite images
- `L19`: `fun toCivitaiImage()` - Converts entity to CivitaiImage model
- `L32`: `fun fromCivitaiImage()` - Factory method creating entity from model

### FeedItemCache.kt

- `L9`: `data class FeedItemCache` - Room entity for feed item caching
- `L19`: `fun toCivitaiImage()` - Converts entity to CivitaiImage model
- `L32`: `fun fromCivitaiImage()` - Factory method creating entity from model

### CivitaiImage.kt

- `L8`: `data class CivitaiImage` - Primary model for Civitai media items

### AppBackup.kt

- `L6`: `data class AppSettingsBackup` - Model for settings backup
- `L54`: `data class AppBackup` - Root model for full application backup

## app/src/main/kotlin/is/xyz/mpv/

### MPVLib.kt

- `L11`: `object MPVLib` - JNI wrapper for libmpv
- `L13`: `val initialized` - Whether MPV has been initialized
- `L15`: `var lastOwnerId` - ID of the last owner of MPV instance
- `L17`: `init block` - Loads native libplayer library
- `L19`: `external fun create()` - Native method to create MPV instance
- `L21`: `external fun init()` - Native method to initialize MPV
- `L23`: `external fun destroy()` - Native method to destroy MPV instance
- `L25`: `external fun attachSurface()` - Native method to attach surface
- `L27`: `external fun detachSurface()` - Native method to detach surface
- `L29`: `external fun command()` - Native method to execute MPV command
- `L31`: `external fun setPropertyString()` - Native method to set string property
- `L33`: `external fun setPropertyInt()` - Native method to set int property
- `L35`: `external fun setPropertyBoolean()` - Native method to set boolean property
- `L37`: `external fun setPropertyDouble()` - Native method to set double property
- `L39`: `external fun getPropertyString()` - Native method to get string property
- `L41`: `external fun getPropertyInt()` - Native method to get int property
- `L43`: `external fun getPropertyBoolean()` - Native method to get boolean property
- `L45`: `external fun getPropertyDouble()` - Native method to get double property
- `L47`: `external fun observeProperty()` - Native method to observe property changes
- `L49`: `external fun setOptionString()` - Native method to set option string
- `L51`: `external fun grabThumbnail()` - Native method to grab frame thumbnail
- `L55`: `fun initialize()` - Initializes MPV instance with context
- `L69`: `fun safeAttachSurface()` - Thread-safe surface attachment
- `L71`: `fun safeDetachSurface()` - Thread-safe surface detachment
- `L73`: `fun safeCommand()` - Thread-safe command execution
- `L75`: `fun safeSetPropertyString()` - Thread-safe string property setter
- `L79`: `fun safeSetPropertyInt()` - Thread-safe int property setter
- `L83`: `fun safeSetPropertyBoolean()` - Thread-safe boolean property setter
- `L87`: `fun safeSetPropertyDouble()` - Thread-safe double property setter
- `L91`: `fun safeSetOptionString()` - Thread-safe option setter
- `L95`: `fun safeGetPropertyInt()` - Thread-safe int property getter
- `L97`: `fun safeGetPropertyDouble()` - Thread-safe double property getter
- `L99`: `fun safeGetPropertyString()` - Thread-safe string property getter
- `L101`: `fun safeGetPropertyBoolean()` - Thread-safe boolean property getter
- `L105`: `fun safeGetPropertyLong()` - Thread-safe long property getter (converts int)
- `L109`: `@JvmStatic fun eventProperty()` - Event property overloads (Long, Boolean, String, Double)
- `L114`: `@JvmStatic fun event()` - Event handler
- `L116`: `@JvmStatic fun logMessage()` - Bridge for MPV log messages to Android log

# Function Mappings: Dibella

This document provides a comprehensive mapping of all function and method names to their respective line numbers and files within the Dibella project.

## app/src/main/kotlin/org/movzx/dibella/

### DibellaApplication.kt

- `L11`: `class DibellaApplication` - Main application class implementing SingletonImageLoader.Factory
- `L14`: `override fun newImageLoader(context: PlatformContext)` - Provides singleton Coil image loader

### MainActivity.kt

- `L49`: `class MainActivity` - Main activity with Hilt injection
- `L52`: `override fun onCreate()` - Sets up edge-to-edge UI and content
- `L190`: `enum class RightSidebarType` - Defines right sidebar modes (FILTERS, SETTINGS)
- `L197`: `fun MainScreen()` - Root composable with shared transition scope
- `L592`: `fun AppNavigation()` - NavHost with feed/favorites/gallery destinations
- `L705`: `fun FeedScreen()` - Feed composable with ViewModel and UI state
- `L807`: `fun FavoritesScreen()` - Favorites list composable
- `L955`: `fun GalleryScreen()` - Local gallery browser composable

## app/src/main/kotlin/org/movzx/dibella/api/

### CivitaiApi.kt

- `L7`: `interface CivitaiApi` - Retrofit service interface
- `L9`: `suspend fun getImages()` - Fetches paginated images from Civitai API

### CivitaiInterceptor.kt

- `L12`: `class CivitaiInterceptor` - OkHttp interceptor for auth and logging
- `L14`: `override fun intercept()` - Injects Bearer token, logs requests/responses, adds User-Agent

### CivitaiThumbnailInterceptor.kt

- `L9`: `class CivitaiThumbnailInterceptor` - Handles 404/403 fallbacks for image thumbnails
- `L12`: `override fun intercept()` - Retries with different widths and verifies magic bytes
- `L84`: `private fun isRealImage()` - Verifies if response contains valid image data
- `L24`: `val newUrl = getThumbnailUrl(url, width)` - Internal thumbnail URL builder (via Utils)
- `L67`: `val originalUrl = getOriginalUrl(url)` - Internal original URL builder (via Utils)

## app/src/main/kotlin/org/movzx/dibella/data/

### AppDatabase.kt

- `L15`: `abstract class AppDatabase` - Room database with entities
- `L16`: `abstract fun favoriteImageDao()` - DAO for favorite images
- `L18`: `abstract fun feedCacheDao()` - DAO for feed cache
- `L23`: `fun getDatabase()` - Singleton database instance provider

### BackupRepository.kt

- `L16`: `class BackupRepository` - Handles JSON import/export of app data
- `L24`: `suspend fun exportData()` - Streams favorites and settings to JSON via Okio
- `L55`: `suspend fun importData()` - Imports JSON backup data

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

### FavoritesRepository.kt

- `L25`: `class FavoritesRepository` - Manages favorite images and local file caching
- `L34`: `fun updateOkHttpClient()` - Updates HTTP client with new auth token
- `L43`: `suspend fun toggleFavorite()` - Adds/removes favorite, manages local resources
- `L45`: `val allFavorites` - Flow of all favorites as CivitaiImage
- `L53`: `private fun extractVideoFrame()` - Extracts thumbnail from video file
- `L89`: `fun getFavoriteFlow()` - Returns Flow of favorite by ID
- `L91`: `suspend fun ensureFavoriteResources()` - Downloads thumbnail/preview if favorited
- `L356`: `suspend fun clearUnusedResources()` - Deletes orphaned cached files
- `L374`: `suspend fun getAllFavoritesSync()` - Blocking fetch all favorites
- `L376`: `suspend fun importFavorites()` - Bulk import favorites
- `L90`: `val favoriteIds` - Flow of all favorite IDs

### GalleryRepository.kt

- `L26`: `class GalleryRepository` - Scans and manages local downloaded files
- `L37`: `suspend fun refreshDownloadedIds()` - Rescans download directory for IDs
- `L62`: `suspend fun scanDirectory()` - Scans for media files in directory, extracts metadata
- `L163`: `suspend fun deleteLocalFile()` - Deletes local file and updates state
- `L176`: `suspend fun downloadImage()` - Downloads image/video with progress
- `L228`: `private fun getDownloadDir()` - Resolves download directory from path

### UserPreferencesRepository.kt

- `L15`: `class UserPreferencesRepository` - Jetpack DataStore for settings persistence
- `L63`: `fun feedScrollIndex()` - Flow of scroll index for feed type
- `L73`: `fun feedScrollOffset()` - Flow of scroll offset for feed type
- `L103`: `fun nextCursor()` - Flow of pagination cursor for feed type
- `L133`: `suspend fun getCurrentSettings()` - Snapshot of all settings for backup
- `L148`: `suspend fun importSettings()` - Bulk update settings from backup
- `L166`: `suspend fun updateFilters()` - Updates NSFW, sort, period, type, tags
- `L189`: `suspend fun updateScrollPosition()` - Debounced scroll position save
- `L212`: `suspend fun updateNextCursor()` - Updates pagination cursor
- `L224`: `suspend fun updatePageLimit()` - Updates page limit setting
- `L230`: `suspend fun updateGridColumns()` - Updates grid column count
- `L238`: `suspend fun updateDownloadPath()` - Updates download directory path
- `L247`: `suspend fun updateApiKey()` - Updates Civitai API key
- `L253`: `suspend fun updateDebugEnabled()` - Toggles debug logging
- `L261`: `suspend fun updateFavoritesType()` - Updates favorites media type filter
- `L267`: `suspend fun updateGalleryType()` - Updates gallery media type filter
- `L273`: `suspend fun getInterceptorSettings()` - Optimized auth+debug for interceptor

## app/src/main/kotlin/org/movzx/dibella/di/

### AppModule.kt

- `L26`: `object AppModule` - Hilt dependency injection module
- `L29`: `fun provideMoshi()` - Moshi JSON adapter with Kotlin support
- `L35`: `fun provideOkHttpClient()` - OkHttp with 30s timeouts and interceptors
- `L48`: `fun provideRetrofit()` - Retrofit with Civitai base URL
- `L58`: `fun provideCivitaiApi()` - Creates CivitaiApi service
- `L64`: `fun provideImageLoader()` - Coil image loader with OkHttp components
- `L93`: `fun provideDatabase()` - Room database singleton
- `L102`: `fun provideFavoriteImageDao()` - FavoriteImageDao provider
- `L107`: `fun provideFeedCacheDao()` - FeedCacheDao provider
- `L112`: `fun provideUserPreferencesRepository()` - DataStore repo provider
- `L120`: `fun provideFavoritesRepository()` - Favorites repo provider

### CivitaiImage.kt

- `L8`: `data class CivitaiImage` - API response model for images/videos
- `L18`: `data class VideoMeta` - Optional video metadata (size)
- `L21`: `data class CivitaiApiResponse` - Paginated API response wrapper
- `L23`: `data class Metadata` - Pagination cursor

### FavoriteImage.kt

- `L9`: `data class FavoriteImage` - Room entity for favorited items
- `L21`: `fun toCivitaiImage()` - Converts to API model
- `L33`: `companion object` - Factory methods
- `L34`: `fun fromCivitaiImage()` - Creates from API model with local paths

### FeedItemCache.kt

- `L9`: `data class FeedItemCache` - Room entity for cached feed items
- `L19`: `fun toCivitaiImage()` - Converts to API model
- `L31`: `companion object` - Factory methods
- `L32`: `fun fromCivitaiImage()` - Creates from API model with ordering

### AppBackup.kt

- `L6`: `data class AppSettingsBackup` - Serializable settings snapshot
- `L17`: `data class AppBackup` - Complete backup (settings + favorites)

### CivitaiImage.kt

- `L8`: `data class CivitaiImage` - API response model for images/videos
- `L18`: `data class VideoMeta` - Optional video metadata (size)
- `L21`: `data class CivitaiApiResponse` - Paginated API response wrapper
- `L23`: `data class Metadata` - Pagination cursor

### FavoriteImage.kt

- `L9`: `data class FavoriteImage` - Room entity for favorited items
- `L21`: `fun toCivitaiImage()` - Converts to API model
- `L33`: `companion object` - Factory methods
- `L34`: `fun fromCivitaiImage()` - Creates from API model with local paths

### FeedItemCache.kt

- `L9`: `data class FeedItemCache` - Room entity for cached feed items
- `L19`: `fun toCivitaiImage()` - Converts to API model
- `L31`: `companion object` - Factory methods
- `L32`: `fun fromCivitaiImage()` - Creates from API model with ordering

## app/src/main/kotlin/org/movzx/dibella/ui/components/

### AppFab.kt

- `L22`: `fun AppFab()` - Smart FAB with conditional scroll/refresh/load-more actions

### AppScaffold.kt

- `L19`: `fun AppScaffold()` - Wrapper managing TopBar, BottomBar, FAB
- `L48`: `fun EmptyState()` - Empty grid placeholder with icon and text

### BaseSidebar.kt

- `L12`: `fun BaseSidebar()` - Foundation for sidebars with RectangleShape

### ConfirmationDialog.kt

- `L13`: `fun ConfirmationDialog()` - Custom dialog with destructive action emphasis

### DisplaySidebar.kt

- `L17`: `fun DisplaySidebar()` - Config for columns, page limit, and media type filter

### FilterSidebar.kt

- `L15`: `data class CivitaiTag` - Tag ID/name pair
- `L19`: `fun FilterSidebar()` - NSFW, sort, period, type, tag filters

### FullScreenImage.kt

- `L39`: `fun FullScreenImage()` - Pager with zoom, gestures, favorite toggle

### ImageCard.kt

- `L38`: `fun ImageCard()` - Dynamic resolution, heart animation, error icon handling

### ImageGrid.kt

- `L24`: `fun ImageGrid()` - Regular grid with shared transitions

### MainBottomBar.kt

- `L19`: `fun MainBottomBar()` - Navigation between feed/favorites/gallery

### MainTopBar.kt

- `L21`: `fun MainTopBar()` - App logo, display/filter/settings triggers

### SettingsSidebar.kt

- `L20`: `fun SettingsSidebar()` - API key, download path, cache, import/export

### Shimmer.kt

- `L12`: `fun Modifier.shimmerBackground()` - Animated loading shimmer effect (legacy)

### SidebarSection.kt

- `L10`: `fun SidebarSection()` - Titled section wrapper for sidebar content

### SkeletonGrid.kt

- `L14`: `fun SkeletonGrid()` - Static placeholder grid

### SpeedDialFab.kt

- `L17`: `data class SpeedDialItem` - Speed dial action item
- `L26`: `fun SpeedDialFab()` - Expandable FAB menu

### VideoPlayer.kt

- `L32`: `@OptIn UnstableApi` - Media3 experimental API opt-in
- `L33`: `fun VideoPlayer()` - ExoPlayer with controls, scaling, looping
- `L41`: `object : Player.Listener` - Player event callbacks
- `L42`: `override fun onTracksChanged()` - Detects audio for mute toggle
- `L51`: `override fun onPlayerError()` - Handles playback errors

### VideoPlayerModels.kt

- `L3`: `enum class ScaleMode` - NORMAL, CROP, FULL scaling options

### ZoomableImage.kt

- `L24`: `fun ZoomableImage()` - Pinch-to-zoom with double-tap

### MpvPlayer.kt

- `L15`: `fun MpvPlayer()` - MPV-based video player alternative

### Theme.kt

- `L15`: `fun DibellaTheme()` - Material 3 theme with dynamic colors

## app/src/main/kotlin/org/movzx/dibella/util/

### FileUtils.kt

- `L8`: `object FileUtils` - File encryption and media type detection
- `L16`: `fun detectExtension()` - Sniffs magic bytes for file type
- `L65`: `fun getExtensionFromBytes()` - Magic byte to extension
- `L83`: `fun isRealMedia()` - Validates media file signatures

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

- `L35`: `fun Modifier.scrollbar()` - Custom scrollbar composable
- `L64`: `fun hasLocalCache()` - Checks thumbnail cache existence
- `L76`: `fun hasFullCache()` - Checks full image cache existence
- `L89`: `fun resolveImageData()` - Selects local/remote URL with optimization
- `L175`: `fun getThumbnailUrl()` - Appends width param to image URL
- `L198`: `fun getVideoThumbnailUrl()` - Video thumbnail URL builder
- `L222`: `fun getVideoPreviewUrl()` - Video preview URL builder
- `L226`: `fun getRequiredStoragePermissions()` - Runtime permission list
- `L234`: `fun hasStoragePermissions()` - Permission check

## app/src/main/kotlin/org/movzx/dibella/viewmodel/

### BaseUiState.kt

- `L5`: `interface BaseUiState` - Common UI state properties

### BaseViewModel.kt

- `L15`: `abstract class BaseViewModel` - Base with shared actions
- `L23`: `fun updateGridColumns()` - Persists column preference
- `L27`: `fun updatePageLimit()` - Persists page limit preference
- `L31`: `fun saveScrollPosition()` - Debounced scroll position save
- `L35`: `protected fun sendMessage()` - Emits toast message
- `L39`: `fun toggleFavorite()` - Delegates to FavoritesRepository
- `L43`: `protected fun performDownload()` - Download with progress tracking
- `L51`: `abstract fun downloadImage()` - Abstract download implementation

### FeedUiState.kt

- `L5`: `data class FeedUiState` - Feed screen state with filter params

### FeedViewModel.kt

- `L22`: `class FeedViewModel` - Main feed with pagination and filters
- `L160`: `fun refresh()` - Clears cursor and reloads feed
- `L172`: `fun loadMore()` - Loads next page with cursor
- `L178`: `private fun loadImages()` - Fetches and caches feed items
- `L258`: `fun updateFilters()` - Updates filter params, invalidates restoration
- `L262`: `fun resetFilters()` - Resets all filters to defaults
- `L268`: `suspend fun ensureFavoriteResources()` - Pre-fetches favorited media
- `L276`: `fun downloadImage()` - Initiates image download
- `L286`: `fun markRestored()` - Sets isRestored flag after scroll restoration
- `L296`: `fun prefetchThumbnails()` - Prefetches upcoming thumbnails using Coil
- `L321`: `fun cleanupExpiredTempCache()` - Removes expired temp cache files

### FavoritesUiState.kt

- `L5`: `data class FavoritesUiState` - Favorites screen state

### FavoritesViewModel.kt

- `L17`: `class FavoritesViewModel` - Manages favorites list
- `L59`: `fun updateType()` - Updates media type filter
- `L63`: `fun toggleSelection()` - Toggles selection for batch operations
- `L73`: `fun clearSelection()` - Clears all selections
- `L77`: `fun selectAll()` - Selects all visible items
- `L86`: `fun batchUnfavorite()` - Removes selected favorites
- `L100`: `fun getFavoriteFlow()` - Returns Flow of favorite by ID
- `L102`: `suspend fun ensureFavoriteResources()` - Pre-fetches favorited media
- `L110`: `fun forceRedownload()` - Re-downloads favorite resource
- `L127`: `fun downloadImage()` - Initiates image download
- `L137`: `fun findDuplicates()` - Finds duplicate images in favorites
- `L162`: `fun clearDuplicatesMode()` - Exits duplicate detection mode
- `L166`: `fun removeDuplicates()` - Removes duplicate groups
- `L193`: `fun markRestored()` - Sets isRestored flag after scroll restoration

### GalleryUiState.kt

- `L5`: `data class GalleryUiState` - Gallery screen state

### GalleryViewModel.kt

- `L15`: `class GalleryViewModel` - Manages local gallery browsing
- `L53`: `fun refresh()` - Rescans download directory with filtering
- `L70`: `fun updateType()` - Updates media type filter
- `L74`: `fun toggleSelection()` - Toggles selection for batch operations
- `L84`: `fun clearSelection()` - Clears all selections
- `L88`: `fun selectAll()` - Selects all visible items
- `L97`: `fun batchDelete()` - Deletes selected local files
- `L112`: `fun deleteLocalFile()` - Deletes local media file
- `L116`: `suspend fun ensureFavoriteResources()` - Pre-fetches favorited media
- `L124`: `fun downloadImage()` - Initiates image download
- `L135`: `fun findDuplicates()` - Finds duplicate images in gallery
- `L160`: `fun clearDuplicatesMode()` - Exits duplicate detection mode
- `L164`: `fun removeDuplicates()` - Removes duplicate groups
- `L196`: `fun markRestored()` - Sets isRestored flag after scroll restoration

### SettingsUiState.kt

- `L5`: `data class SettingsUiState` - Settings screen state

### SettingsViewModel.kt

- `L18`: `class SettingsViewModel` - Manages app settings and import/export
- `L56`: `fun updateApiKey()` - Updates Civitai API key
- `L63`: `fun updateDownloadPath()` - Updates download directory
- `L67`: `fun updateDebugEnabled()` - Toggles debug logging
- `L71`: `fun clearImageCache()` - Clears Coil image cache
- `L80`: `fun updateCacheSize()` - Computes and updates cache size
- `L98`: `fun exportData()` - Exports backup to URI
- `L107`: `fun importData()` - Imports backup from URI

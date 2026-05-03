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
- `L15`: `override fun intercept()` - Injects Bearer token and adds User-Agent

### CivitaiThumbnailInterceptor.kt

- `L11`: `class CivitaiThumbnailInterceptor` - Handles 404/403 fallbacks for image thumbnails
- `L16`: `private fun cacheable()` - Adds custom Cache-Control headers to successful responses
- `L35`: `private fun Response?.closeQuietly()` - Extension to safely close OkHttp responses
- `L43`: `private fun safeProceed()` - Helper for executing network requests with timeout
- `L63`: `override fun intercept()` - Main interceptor logic with retry/fallback chain
- `L103`: `private fun isValidMedia()` - Verifies if response contains valid image or video data

## app/src/main/kotlin/org/movzx/dibella/data/

### AppDatabase.kt

- `L15`: `abstract class AppDatabase` - Room database with entities
- `L16`: `abstract fun favoriteImageDao()` - DAO for favorite images
- `L18`: `abstract fun feedCacheDao()` - DAO for feed cache
- `L23`: `fun getDatabase()` - Singleton database instance provider

### BackupRepository.kt

- `L17`: `class BackupRepository` - Handles JSON import/export of app data
- `L26`: `suspend fun exportData()` - Streams favorites and settings to JSON via Okio
- `L80`: `suspend fun importData()` - Imports JSON backup data

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

- `L39`: `class FavoritesRepository` - Manages favorite images and local file caching
- `L83`: `suspend fun repairSync()` - Repairs local file sync and triggers metadata extraction
- `L149`: `fun updateOkHttpClient()` - Updates HTTP client with new auth token
- `L164`: `fun getFavoriteFlow()` - Returns Flow of favorite by ID
- `L166`: `suspend fun manualFetch()` - Manually fetches image metadata from URL
- `L185`: `suspend fun toggleFavorite()` - Adds/removes favorite, manages local resources
- `L210`: `private fun extractVideoFrame()` - Extracts thumbnail from video file
- `L266`: `suspend fun ensureFavoriteResources()` - Downloads thumbnail/preview if favorited
- `L572`: `private suspend fun downloadFile()` - Internal helper for downloading files with progress
- `L693`: `private suspend fun addFavorite()` - Adds item to database and triggers resource sync
- `L703`: `private suspend fun removeFavorite()` - Removes item from database and purges local files
- `L728`: `suspend fun clearUnusedResources()` - Deletes orphaned cached files
- `L760`: `suspend fun findDuplicateGroups()` - Finds duplicate images in favorites
- `L789`: `suspend fun removeDuplicates()` - Removes duplicate groups
- `L832`: `suspend fun getAllFavoritesSync()` - Blocking fetch all favorites
- `L834`: `suspend fun importFavorites()` - Bulk import favorites
- `L840`: `fun isFavorite()` - Flow of favorite status for single ID

### GalleryRepository.kt

- `L30`: `class GalleryRepository` - Scans and manages local downloaded files
- `L47`: `suspend fun refreshDownloadedIds()` - Rescans download directory for IDs
- `L72`: `suspend fun scanDirectory()` - Scans for media files in directory, extracts metadata
- `L177`: `suspend fun deleteLocalFile()` - Deletes local file and updates state
- `L201`: `suspend fun downloadImage()` - Downloads image/video with progress
- `L330`: `suspend fun findDuplicateGroups()` - Finds duplicate images in download directory
- `L371`: `suspend fun removeDuplicates()` - Removes duplicate groups
- `L402`: `private fun getDownloadDir()` - Resolves download directory from path

### UserPreferencesRepository.kt

- `L17`: `class UserPreferencesRepository` - Jetpack DataStore for settings persistence
- `L77`: `fun feedScrollIndex()` - Flow of scroll index for feed type
- `L87`: `fun feedScrollOffset()` - Flow of scroll offset for feed type
- `L97`: `fun nextCursor()` - Flow of pagination cursor for feed type
- `L178`: `suspend fun getCurrentSettings()` - Snapshot of all settings for backup
- `L208`: `suspend fun importSettings()` - Bulk update settings from backup
- `L251`: `suspend fun updateFilters()` - Updates NSFW, sort, period, type, tags
- `L274`: `suspend fun updateScrollPosition()` - Debounced scroll position save
- `L297`: `suspend fun updateNextCursor()` - Updates pagination cursor
- `L309`: `suspend fun updatePageLimit()` - Updates page limit setting
- `L315`: `suspend fun updateGridColumns()` - Updates grid column count
- `L323`: `suspend fun updateDownloadPath()` - Updates download directory path
- `L332`: `suspend fun updateFavoritesPath()` - Updates favorites directory path
- `L341`: `suspend fun updateApiKey()` - Updates Civitai API key
- `L347`: `suspend fun updateDebugEnabled()` - Toggles debug logging
- `L355`: `suspend fun updateFavoritesType()` - Updates favorites media type filter
- `L361`: `suspend fun updateGalleryType()` - Updates gallery media type filter
- `L367`: `suspend fun updateLastRoute()` - Tracks last visited navigation route
- `L373`: `suspend fun updateHidePlayerControls()` - Toggles video player controls visibility
- `L381`: `suspend fun updateAlwaysEnableHD()` - Toggles high-definition media preference
- `L389`: `suspend fun updateAlwaysMuteVideo()` - Toggles video player mute preference
- `L397`: `suspend fun updateFeedVideoAutoplay()` - Toggles video autoplay in feed
- `L405`: `suspend fun updateAmoledMode()` - Toggles AMOLED black background mode
- `L411`: `suspend fun getInterceptorSettings()` - Optimized auth+debug for interceptor

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

- `L35`: `fun VideoPlayer()` - ExoPlayer with controls, scaling, looping
- `L161`: `private fun ExoVideoPlayer()` - Lower-level Media3 player wrapper

### VideoPlayerManager.kt

- `L14`: `object VideoPlayerManager` - Pooled ExoPlayer management
- `L19`: `fun acquirePlayer()` - Gets an available player from pool
- `L36`: `fun releasePlayer()` - Returns player to pool
- `L42`: `fun releaseAll()` - Releases all players and clears pool

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
- `L9`: `fun compressUrl()` - Shortens Civitai URLs for database storage
- `L18`: `fun expandUrl()` - Restores full URLs from compressed format
- `L27`: `fun isCivitaiUrl()` - Checks if URL belongs to Civitai CDN
- `L29`: `fun getThumbnailUrl()` - Builds thumbnail variant URL
- `L33`: `fun getVideoThumbnailUrl()` - Builds video thumbnail variant URL
- `L41`: `fun getVideoPreviewUrl()` - Builds video preview variant URL
- `L49`: `fun getOriginalUrl()` - Builds original file variant URL
- `L59`: `fun getFallbackChain()` - Returns list of URLs to try for thumbnails

### FileUtils.kt

- `L8`: `object FileUtils` - File encryption and media type detection
- `L18`: `fun detectExtension()` - Sniffs magic bytes for file type
- `L72`: `fun getExtensionFromBytes()` - Magic byte to extension mapping
- `L91`: `fun isRealMedia()` - Validates media file signatures
- `L133`: `fun calculateHash()` - Generates SHA-256 hash for duplicate detection
- `L165`: `fun findDuplicateGroups()` - Groups files by content hash

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

- `L29`: `fun Modifier.scrollbar()` - Custom scrollbar for scrollable layouts
- `L58`: `fun Modifier.gridScrollbar()` - Optimized scrollbar for LazyGrids
- `L115`: `suspend fun hasLocalCache()` - Checks thumbnail cache existence
- `L141`: `suspend fun hasFullCache()` - Checks full resolution media cache
- `L174`: `fun resolveImageData()` - Selects local/remote URL with optimization
- `L221`: `fun getThumbnailUrl()` - Appends width param to image URL
- `L225`: `fun getVideoThumbnailUrl()` - Video thumbnail URL builder
- `L229`: `fun getVideoPreviewUrl()` - Video preview URL builder
- `L233`: `fun getVideoOriginalUrl()` - Video original file URL builder
- `L237`: `fun getOriginalUrl()` - Appends original flag to image URL
- `L241`: `fun modifyCivitaiUrl()` - Generic URL variant modifier
- `L249`: `fun formatDuration()` - Formats milliseconds to human readable duration
- `L272`: `fun playerPoolSizeForColumns()` - Calculates optimal player pool size

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

- `L24`: `class FeedViewModel` - Main feed with pagination and filters
- `L162`: `fun refresh()` - Clears cursor and reloads feed
- `L194`: `fun loadMore()` - Loads next page with cursor
- `L204`: `private fun loadImages()` - Fetches and caches feed items
- `L323`: `fun updateFilters()` - Updates filter params, invalidates restoration
- `L334`: `fun resetFilters()` - Resets all filters to defaults
- `L354`: `fun markRestored()` - Sets isRestored flag after scroll restoration

### FavoritesViewModel.kt

- `L17`: `class FavoritesViewModel` - Manages favorites list
- `L73`: `fun updateType()` - Updates media type filter
- `L81`: `fun toggleSelection()` - Toggles selection for batch operations
- `L96`: `fun clearSelection()` - Clears all selections
- `L102`: `fun selectAll()` - Selects all visible items
- `L113`: `fun batchUnfavorite()` - Removes selected favorites
- `L146`: `fun forceRedownload()` - Re-downloads favorite resource
- `L182`: `fun findDuplicates()` - Finds duplicate images in favorites
- `L216`: `fun clearDuplicatesMode()` - Exits duplicate detection mode
- `L222`: `fun removeDuplicates()` - Removes duplicate groups
- `L256`: `fun markRestored()` - Sets isRestored flag after scroll restoration

### GalleryViewModel.kt

- `L16`: `class GalleryViewModel` - Manages local gallery browsing
- `L62`: `fun refresh()` - Rescans download directory with filtering
- `L83`: `fun updateType()` - Updates media type filter
- `L91`: `fun toggleSelection()` - Toggles selection for batch operations
- `L103`: `fun clearSelection()` - Clears all selections
- `L109`: `fun selectAll()` - Selects all visible items
- `L121`: `fun batchDelete()` - Deletes selected local files
- `L138`: `fun deleteLocalFile()` - Deletes local media file
- `L146`: `override fun downloadImage()` - Initiates image download
- `L159`: `fun findDuplicates()` - Finds duplicate images in gallery
- `L193`: `fun clearDuplicatesMode()` - Exits duplicate detection mode
- `L199`: `fun removeDuplicates()` - Removes duplicate groups
- `L238`: `fun markRestored()` - Sets isRestored flag after scroll restoration

### SettingsViewModel.kt

- `L18`: `class SettingsViewModel` - Manages app settings and import/export
- `L66`: `fun updateLastRoute()` - Tracks last visited navigation route
- `L70`: `fun updateApiKey()` - Updates Civitai API key
- `L77`: `fun updateDownloadPath()` - Updates download directory
- `L81`: `fun updateFavoritesPath()` - Updates favorites directory
- `L86`: `fun updateDebugEnabled()` - Toggles debug logging
- `L90`: `fun updateHidePlayerControls()` - Toggles video player controls visibility
- `L104`: `fun updateAlwaysEnableHD()` - Toggles high-definition media preference
- `L108`: `fun updateAlwaysMuteVideo()` - Toggles video player mute preference
- `L112`: `fun updateFeedVideoAutoplay()` - Toggles video autoplay in feed
- `L116`: `fun updateAmoledMode()` - Toggles AMOLED black background mode
- `L120`: `fun clearImageCache()` - Clears Coil image cache
- `L129`: `fun updateCacheSize()` - Computes and updates cache size
- `L147`: `fun exportData()` - Exports backup to URI
- `L156`: `fun importData()` - Imports backup from URI

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

- `L9`: `object MPVLib` - JNI wrapper for libmpv
- `L55`: `fun initialize()` - Initializes MPV instance with context
- `L69`: `fun safeAttachSurface()` - Thread-safe surface attachment
- `L71`: `fun safeDetachSurface()` - Thread-safe surface detachment
- `L73`: `fun safeCommand()` - Thread-safe command execution
- `L114`: `fun logMessage()` - Bridge for MPV log messages to Android log

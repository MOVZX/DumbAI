# Function Mappings: Dibella

This document provides a comprehensive mapping of all function and method names to their respective line numbers and files within the Dibella project.

## app/src/main/kotlin/org/movzx/dibella/

### DibellaApplication.kt

- `L11`: `class DibellaApplication` - Main application class implementing SingletonImageLoader.Factory
- `L14`: `override fun newImageLoader(context: PlatformContext)` - Provides singleton Coil image loader

### MainActivity.kt

- `L49`: `class MainActivity` - Main activity with Hilt injection
- `L52`: `override fun onCreate()` - Sets up edge-to-edge UI and content
- `L60`: `fun UiMessageEffect()` - Collects and displays UI messages as SnackBar
- `L76`: `fun AppBackHandler()` - Handles system back button and selection mode
- `L114`: `fun ScrollRestorationEffect()` - Manages scroll restoration for LazyGrids
- `L145`: `fun InteractiveTopBar()` - Dynamic TopBar for Gallery/Favorites screens
- `L190`: `enum class RightSidebarType` - Defines right sidebar modes (FILTERS, SETTINGS)
- `L197`: `fun MainScreen()` - Root composable with shared transition scope
- `L602`: `fun AppNavigation()` - NavHost with feed/favorites/gallery destinations
- `L715`: `fun FeedScreen()` - Feed composable with ViewModel and UI state
- `L820`: `fun FavoritesScreen()` - Favorites list composable
- `L971`: `fun GalleryScreen()` - Local gallery browser composable

## app/src/main/kotlin/org/movzx/dibella/api/

### CivitaiApi.kt

- `L7`: `interface CivitaiApi` - Retrofit service interface
- `L9`: `suspend fun getImages()` - Fetches paginated images from Civitai API

### CivitaiInterceptor.kt

- `L12`: `class CivitaiInterceptor` - OkHttp interceptor for auth and logging
- `L14`: `override fun intercept()` - Injects Bearer token, logs requests/responses, adds User-Agent

### CivitaiThumbnailInterceptor.kt

- `L9`: `class CivitaiThumbnailInterceptor` - Handles 404/403 fallbacks for image thumbnails
- `L15`: `override fun intercept()` - Retries with different widths and verifies magic bytes
- `L27`: `fun tryUrlWithRetry()` - Internal helper for retrying network requests
- `L150`: `private fun withTimeout()` - Executes request with custom timeout wrapper
- `L181`: `private fun isRealImage()` - Verifies if response contains valid image data

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
- `L45`: `suspend fun toggleFavorite()` - Adds/removes favorite, manages local resources
- `L53`: `private fun extractVideoFrame()` - Extracts thumbnail from video file
- `L89`: `fun getFavoriteFlow()` - Returns Flow of favorite by ID
- `L91`: `suspend fun ensureFavoriteResources()` - Downloads thumbnail/preview if favorited
- `L144`: `fun updateTotalProgress()` - Internal helper for calculating parallel progress
- `L357`: `private suspend fun downloadFile()` - Internal helper for downloading files with progress
- `L440`: `private suspend fun addFavorite()` - Adds item to database and triggers resource sync
- `L450`: `private suspend fun removeFavorite()` - Removes item from database and purges local files
- `L469`: `suspend fun clearUnusedResources()` - Deletes orphaned cached files
- `L487`: `suspend fun findDuplicateGroups()` - Finds duplicate images in favorites
- `L507`: `suspend fun removeDuplicates()` - Removes duplicate groups
- `L535`: `private fun internalCalculateDuplicates()` - Internal logic for duplicate detection
- `L553`: `suspend fun getAllFavoritesSync()` - Blocking fetch all favorites
- `L555`: `suspend fun importFavorites()` - Bulk import favorites
- `L561`: `fun isFavorite()` - Flow of favorite status for single ID

### GalleryRepository.kt

- `L26`: `class GalleryRepository` - Scans and manages local downloaded files
- `L37`: `suspend fun refreshDownloadedIds()` - Rescans download directory for IDs
- `L62`: `suspend fun scanDirectory()` - Scans for media files in directory, extracts metadata
- `L183`: `suspend fun deleteLocalFile()` - Deletes local file and updates state
- `L207`: `suspend fun downloadImage()` - Downloads image/video with progress
- `L286`: `suspend fun findDuplicateGroups()` - Finds duplicate images in download directory
- `L327`: `suspend fun removeDuplicates()` - Removes duplicate groups
- `L358`: `private fun internalCalculateDuplicates()` - Internal logic for duplicate detection
- `L384`: `private fun getDownloadDir()` - Resolves download directory from path

### UserPreferencesRepository.kt

- `L15`: `class UserPreferencesRepository` - Jetpack DataStore for settings persistence
- `L69`: `fun feedScrollIndex()` - Flow of scroll index for feed type
- `L79`: `fun feedScrollOffset()` - Flow of scroll offset for feed type
- `L109`: `fun nextCursor()` - Flow of pagination cursor for feed type
- `L139`: `suspend fun getCurrentSettings()` - Snapshot of all settings for backup
- `L154`: `suspend fun importSettings()` - Bulk update settings from backup
- `L172`: `suspend fun updateFilters()` - Updates NSFW, sort, period, type, tags
- `L195`: `suspend fun updateScrollPosition()` - Debounced scroll position save
- `L218`: `suspend fun updateNextCursor()` - Updates pagination cursor
- `L230`: `suspend fun updatePageLimit()` - Updates page limit setting
- `L236`: `suspend fun updateGridColumns()` - Updates grid column count
- `L244`: `suspend fun updateDownloadPath()` - Updates download directory path
- `L253`: `suspend fun updateApiKey()` - Updates Civitai API key
- `L259`: `suspend fun updateDebugEnabled()` - Toggles debug logging
- `L267`: `suspend fun updateFavoritesType()` - Updates favorites media type filter
- `L273`: `suspend fun updateGalleryType()` - Updates gallery media type filter
- `L279`: `suspend fun updateLastRoute()` - Tracks last visited navigation route
- `L285`: `suspend fun getInterceptorSettings()` - Optimized auth+debug for interceptor

## app/src/main/kotlin/org/movzx/dibella/di/

### AppModule.kt

- `L27`: `object AppModule` - Hilt dependency injection module
- `L30`: `fun provideMoshi()` - Moshi JSON adapter with Kotlin support
- `L36`: `fun provideOkHttpClient()` - OkHttp with 10s timeouts and interceptors
- `L50`: `fun provideRetrofit()` - Retrofit with Civitai base URL
- `L60`: `fun provideCivitaiApi()` - Creates CivitaiApi service
- `L66`: `fun provideImageLoader()` - Coil image loader with OkHttp components
- `L95`: `fun provideDatabase()` - Room database singleton
- `L102`: `fun provideFavoriteImageDao()` - FavoriteImageDao provider
- `L107`: `fun provideFeedCacheDao()` - FeedCacheDao provider
- `L113`: `fun provideUserPreferencesRepository()` - DataStore repo provider
- `L121`: `fun provideFavoritesRepository()` - Favorites repo provider

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

- `L18`: `fun DisplaySidebar()` - Config for columns, page limit, and media type filter

### FilterSidebar.kt

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

- `L18`: `fun SelectionTopBar()` - TopBar for batch selection mode
- `L76`: `fun MainTopBar()` - App logo, display/filter/settings triggers

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

- `L33`: `fun VideoPlayer()` - ExoPlayer with controls, scaling, looping
- `L138`: `private fun ExoVideoPlayer()` - Lower-level Media3 player wrapper

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
- `L84`: `fun getExtensionFromBytes()` - Magic byte to extension
- `L106`: `fun isRealMedia()` - Validates media file signatures
- `L148`: `fun calculateHash()` - Generates SHA-256 hash for duplicate detection

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
- `L82`: `fun hasFullCache()` - Checks full resolution media cache
- `L111`: `fun resolveImageData()` - Selects local/remote URL with optimization
- `L220`: `fun getThumbnailUrl()` - Appends width param to image URL
- `L224`: `fun getVideoThumbnailUrl()` - Video thumbnail URL builder
- `L234`: `fun getVideoPreviewUrl()` - Video preview URL builder
- `L244`: `fun getOriginalUrl()` - Appends original flag to image URL
- `L254`: `fun getRequiredStoragePermissions()` - Runtime permission list
- `L262`: `fun hasStoragePermissions()` - Permission check
- `L273`: `fun formatDuration()` - Formats milliseconds to human readable duration

## app/src/main/kotlin/org/movzx/dibella/viewmodel/

### BaseUiState.kt

- `L5`: `interface BaseUiState` - Common UI state properties

### BaseViewModel.kt

- `L17`: `abstract class BaseViewModel` - Base with shared actions
- `L26`: `fun updateGridColumns()` - Persists column preference
- `L30`: `fun updatePageLimit()` - Persists page limit preference
- `L34`: `fun saveScrollPosition()` - Debounced scroll position save
- `L38`: `protected fun sendMessage()` - Emits toast message
- `L42`: `fun toggleFavorite()` - Delegates to FavoritesRepository
- `L46`: `suspend fun ensureFavoriteResources()` - Downloads thumbnail/preview
- `L54`: `suspend fun ensureFavoriteResourcesThrottled()` - Throttled resource synchronization
- `L66`: `protected fun performDownload()` - Download with progress tracking

### FeedViewModel.kt

- `L23`: `class FeedViewModel` - Main feed with pagination and filters
- `L160`: `fun refresh()` - Clears cursor and reloads feed
- `L192`: `fun loadMore()` - Loads next page with cursor
- `L202`: `private fun loadImages()` - Fetches and caches feed items
- `L321`: `fun updateFilters()` - Updates filter params, invalidates restoration
- `L332`: `fun resetFilters()` - Resets all filters to defaults
- `L352`: `fun markRestored()` - Sets isRestored flag after scroll restoration

### FavoritesViewModel.kt

- `L17`: `class FavoritesViewModel` - Manages favorites list
- `L71`: `fun updateType()` - Updates media type filter
- `L79`: `fun toggleSelection()` - Toggles selection for batch operations
- `L94`: `fun clearSelection()` - Clears all selections
- `L100`: `fun selectAll()` - Selects all visible items
- `L111`: `fun batchUnfavorite()` - Removes selected favorites
- `L129`: `fun forceRedownload()` - Re-downloads favorite resource
- `L160`: `fun findDuplicates()` - Finds duplicate images in favorites
- `L194`: `fun clearDuplicatesMode()` - Exits duplicate detection mode
- `L200`: `fun removeDuplicates()` - Removes duplicate groups
- `L234`: `fun markRestored()` - Sets isRestored flag after scroll restoration

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
- `L81`: `fun updateDebugEnabled()` - Toggles debug logging
- `L85`: `fun clearImageCache()` - Clears Coil image cache
- `L94`: `fun updateCacheSize()` - Computes and updates cache size
- `L112`: `fun exportData()` - Exports backup to URI
- `L121`: `fun importData()` - Imports backup from URI

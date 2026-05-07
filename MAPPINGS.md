# Function Mappings: Dibella

This document provides a comprehensive mapping of all functions and method names to their respective line numbers and files within the Dibella project.

## Project Structure

```
Dibella/
├── app/src/main/kotlin/org/movzx/dibella/
│   ├── DibellaApplication.kt          # Application entry point
│   ├── MainActivity.kt                 # Main activity with splash/onboarding flow
│   ├── api/                             # API layer
│   │   ├── CivitaiApi.kt               # Retrofit interface
│   │   ├── CivitaiInterceptor.kt       # OkHttp interceptor for auth/backend
│   │   └── CivitaiThumbnailInterceptor.kt  # Thumbnail fallback handler
│   ├── data/                            # Data layer
│   │   ├── AppDatabase.kt              # Room database
│   │   ├── FavoriteImageDao.kt         # Favorites DAO
│   │   ├── FeedCacheDao.kt             # Feed cache DAO
│   │   ├── BookmarkDao.kt              # Bookmark DAO
│   │   ├── BookmarkRepository.kt       # Bookmark repository
│   │   ├── BackupRepository.kt         # JSON import/export
│   │   ├── FeedRepository.kt           # Feed fetch + cache
│   │   ├── FavoritesRepository.kt      # Favorites with local caching
│   │   ├── GalleryRepository.kt        # Local gallery scanning
│   │   └── UserPreferencesRepository.kt  # Jetpack DataStore
│   ├── di/                              # Dependency injection (Hilt)
│   │   └── AppModule.kt
│   ├── model/                           # Data models
│   │   ├── CivitaiImage.kt             # Core media model
│   │   ├── FavoriteImage.kt            # Room entity for favorites
│   │   ├── FeedItemCache.kt            # Room entity for feed cache
│   │   ├── Bookmark.kt                 # Bookmark entity
│   │   └── AppBackup.kt                # Backup serialization models
│   ├── ui/
│   │   ├── screens/                    # Screen composables
│   │   │   ├── MainScreen.kt           # Root navigation + dual drawer layout
│   │   │   ├── FeedScreen.kt           # Feed screen
│   │   │   ├── FavoritesScreen.kt      # Favorites screen
│   │   │   ├── GalleryScreen.kt        # Gallery screen
│   │   │   ├── BookmarkScreen.kt       # Bookmark screen
│   │   │   └── OnboardingScreen.kt     # Onboarding wizard
│   │   ├── components/                 # Reusable UI components
│   │   │   ├── AppBackHandler.kt       # System back handler
│   │   │   ├── AppFab.kt               # FAB with scroll/jump actions
│   │   │   ├── AppScaffold.kt          # Scaffold with pull-to-refresh
│   │   │   ├── BaseSidebar.kt          # Sidebar base layout
│   │   │   ├── BookmarkDialog.kt       # Bookmark creation dialog
│   │   │   ├── ConfirmationDialog.kt   # Confirmation dialog
│   │   │   ├── DisplaySidebar.kt       # Display settings sidebar
│   │   │   ├── FilterSidebar.kt        # Filter sidebar (NSFW/sort/period)
│   │   │   ├── FullScreenImage.kt      # Full-screen image viewer
│   │   │   ├── ImageCard.kt            # Grid card with heart animation
│   │   │   ├── ImageGrid.kt            # Staggered grid layout
│   │   │   ├── JumpDialog.kt           # Jump-to-cursor dialog
│   │   │   ├── MainBottomBar.kt        # Bottom navigation bar
│   │   │   ├── MainTopBar.kt           # Top app bar
│   │   │   ├── SettingsSidebar.kt      # Settings sidebar
│   │   │   ├── Shimmer.kt              # Loading shimmer effect
│   │   │   ├── SidebarSection.kt       # Collapsible sidebar section
│   │   │   ├── SkeletonGrid.kt         # Placeholder grid
│   │   │   ├── SpeedDialFab.kt         # Expandable FAB menu
│   │   │   ├── SplashScreen.kt         # Splash screen
│   │   │   ├── VideoPlayer.kt          # ExoPlayer wrapper
│   │   │   ├── VideoPlayerManager.kt   # Pooled ExoPlayer manager
│   │   │   ├── VideoPlayerModels.kt    # ScaleMode enum + BackendConfig
│   │   │   ├── ZoomableImage.kt        # Pinch-to-zoom image
│   │   │   └── BookmarkDialog.kt       # Bookmark creation dialog
│   │   └── theme/
│   │       └── Theme.kt                # Material 3 theme
│   ├── util/                            # Utility functions
│   │   ├── CivitaiUrlBuilder.kt        # URL compression/variant building
│   │   ├── Constants.kt                # App-wide constants
│   │   ├── DuplicateDetector.kt        # Duplicate detection utility
│   │   ├── FileUtils.kt                # File I/O and magic byte detection
│   │   ├── Logger.kt                   # Conditional debug logging
│   │   ├── MediaProcessor.kt           # Video frame extraction + WebP
│   │   ├── UriUtils.kt                 # URI to path resolution
│   │   └── Utils.kt                    # Helper functions
│   └── viewmodel/                       # ViewModels
│       ├── BaseViewModel.kt            # Base with shared actions
│       ├── BaseUiState.kt              # Common UI state interface
│       ├── FeedUiState.kt              # Feed UI state
│       ├── FavoritesUiState.kt         # Favorites UI state
│       ├── GalleryUiState.kt           # Gallery UI state
│       ├── SettingsUiState.kt          # Settings UI state
│       ├── BookmarkUiState.kt          # Bookmark UI state
│       ├── FeedViewModel.kt            # Feed with pagination
│       ├── FavoritesViewModel.kt       # Favorites management
│       ├── GalleryViewModel.kt         # Gallery management
│       ├── SettingsViewModel.kt        # Settings management
│       └── BookmarkViewModel.kt        # Bookmark management
└── app/src/test/kotlin/               # Unit tests
```

## app/src/main/kotlin/org/movzx/dibella/

### DibellaApplication.kt

- `L13`: `class DibellaApplication` - Application class with Hilt and SingletonImageLoader factory
- `L14`: `imageLoader: ImageLoader` - Injected Coil image loader
- `L15`: `favoritesRepository: FavoritesRepository` - Injected favorites repository
- `L17`: `override fun newImageLoader(context: PlatformContext)` - Returns singleton Coil image loader
- `L21`: `override fun onTerminate()` - Closes FavoritesRepository on app termination

### MainActivity.kt

- `L30`: `enum class RightSidebarType` - Right sidebar modes (FILTERS, SETTINGS)
- `L36`: `class MainActivity` - Main activity entry point with FragmentActivity
- `L37`: `imageLoader: ImageLoader` - Injected Coil image loader
- `L38`: `preferencesRepository: UserPreferencesRepository` - Injected preferences repository
- `L40`: `permissionsToRequest` - List of runtime permissions to request
- `L49`: `permissionLauncher` - Activity result launcher for permissions
- `L70-72`: `_permissionsGranted`, `_showOnboarding`, `_showMain` - Mutable state flows
- `L74`: `permissionsGranted: State<Boolean>` - Public permission state
- `L77`: `override fun onCreate(savedInstanceState: Bundle?)` - Sets up edge-to-edge UI, permissions, onboarding, and content
- `L87`: `lifecycleScope.launch` - Collects onboardingCompleted flow to show onboarding
- `L93`: `setContent { ... }` - Compose content with theme, splash, onboarding, and main screen

## app/src/main/kotlin/org/movzx/dibella/api/

### CivitaiApi.kt

- `L7`: `interface CivitaiApi` - Retrofit service interface for Civitai API
- `L9`: `suspend fun getImages(...)` - Fetches paginated images with NSFW, sort, period, type, tags, cursor params

### CivitaiInterceptor.kt

- `L12`: `class CivitaiInterceptor` - OkHttp interceptor for auth, logging, and backend URL redirection
- `L14`: `override fun intercept(chain: Interceptor.Chain): Response` - Injects Bearer token, User-Agent, redirects to backend, logs requests/responses

### CivitaiThumbnailInterceptor.kt

- `L11`: `class CivitaiThumbnailInterceptor` - Handles 404/403 fallbacks for image thumbnails with retry chain
- `L12-16`: `companion object` - Constants: VIDEO_TIMEOUT_MS, IMAGE_TIMEOUT_MS, MAX_RETRIES
- `L18-20`: `videoThumbnailTimeout`, `imageThumbnailTimeout`, `maxRetries` - Instance property copies
- `L22`: `private fun cacheable(response: Response): Response` - Adds Cache-Control header to successful responses
- `L28`: `private fun Response?.closeQuietly()` - Safely closes OkHttp response
- `L36`: `private fun safeProceed(chain, request, url, timeoutMs): Response?` - Executes request with timeout, returns null on failure
- `L56`: `override fun intercept(chain: Interceptor.Chain): Response` - Main logic: tries fallback chain with retry, caches valid responses
- `L96`: `private fun isValidMedia(response: Response): Boolean` - Validates response contains real image/video data (checks magic bytes, content type)

## app/src/main/kotlin/org/movzx/dibella/data/

### AppDatabase.kt

- `L16`: `abstract class AppDatabase` - Room database with FavoriteImage, FeedItemCache, and Bookmark entities
- `L17`: `abstract fun favoriteImageDao(): FavoriteImageDao` - DAO for favorite_images table
- `L19`: `abstract fun feedCacheDao(): FeedCacheDao` - DAO for feed_cache table
- `L21`: `abstract fun bookmarkDao(): BookmarkDao` - DAO for bookmarks table
- `L23-43`: `companion object` - Singleton database instance with volatile INSTANCE and getDatabase()
- `L26`: `fun getDatabase(context: Context): AppDatabase` - Thread-safe singleton database provider

### FavoriteImageDao.kt

- `L9`: `interface FavoriteImageDao` - Room DAO for favorite_images table
- `L10-11`: `fun getAllFavorites(): Flow<List<FavoriteImage>>` - Flow of all favorites ordered by timestamp DESC
- `L13-14`: `suspend fun _getAllFavoritesSync(): List<FavoriteImage>` - Blocking fetch of all favorites (internal)
- `L16-22`: `suspend fun getAllFavoritesSync(): List<FavoriteImage>` - Blocking fetch with logging
- `L24-25`: `suspend fun _insertFavorite(image: FavoriteImage)` - Insert or replace single favorite (internal)
- `L27-32`: `suspend fun insertFavorite(image: FavoriteImage)` - Insert with logging
- `L34-35`: `suspend fun _insertFavorites(images: List<FavoriteImage>)` - Bulk insert (internal)
- `L37-42`: `suspend fun insertFavorites(images: List<FavoriteImage>)` - Bulk insert with logging
- `L44`: `suspend fun _deleteFavorite(image: FavoriteImage)` - Delete single favorite (internal)
- `L46-51`: `suspend fun deleteFavorite(image: FavoriteImage)` - Delete with logging
- `L53-54`: `fun isFavorite(id: Long): Flow<Boolean>` - Flow checking if ID is favorited
- `L56-57`: `suspend fun isFavoriteDirect(id: Long): Boolean` - Blocking favorite check
- `L59`: `fun getAllFavoriteIds(): Flow<List<Long>>` - Flow of all favorite IDs
- `L61-62`: `suspend fun getFavorite(id: Long): FavoriteImage?` - Fetch single favorite by ID
- `L64-65`: `fun getFavoriteFlow(id: Long): Flow<FavoriteImage?>` - Flow of single favorite by ID

### FeedCacheDao.kt

- `L8`: `interface FeedCacheDao` - Room DAO for feed_cache table
- `L9-10`: `suspend fun getFeed(feedType: String): List<FeedItemCache>` - Fetch cached feed by type
- `L12-13`: `suspend fun insertFeed(items: List<FeedItemCache>)` - Cache feed items
- `L15-16`: `suspend fun clearFeed(feedType: String)` - Clear cache for feed type
- `L18-24`: `suspend fun replaceFeed(feedType: String, items: List<FeedItemCache>)` - Atomic replace (clear + insert)
- `L26-29`: `suspend fun insertOrUpdateFeed(items: List<FeedItemCache>)` - Insert or update existing items

### BookmarkDao.kt

- `L8`: `interface BookmarkDao` - Room DAO for bookmarks table
- `L9-10`: `fun getAllBookmarks(): Flow<List<Bookmark>>` - Flow of all bookmarks
- `L12-13`: `suspend fun getAllBookmarksSync(): List<Bookmark>` - Blocking fetch
- `L15`: `suspend fun insertBookmark(bookmark: Bookmark)` - Insert or replace bookmark
- `L17`: `suspend fun updateBookmark(bookmark: Bookmark)` - Update existing bookmark
- `L19`: `suspend fun deleteBookmark(bookmark: Bookmark)` - Delete bookmark
- `L21`: `suspend fun clearAll()` - Delete all bookmarks

### BookmarkRepository.kt

- `L9`: `class BookmarkRepository` - Repository wrapping BookmarkDao
- `L10`: `fun getAllBookmarks(): Flow<List<Bookmark>>` - Flow of all bookmarks
- `L12`: `suspend fun getAllBookmarksSync(): List<Bookmark>` - Blocking fetch
- `L14`: `suspend fun addBookmark(bookmark: Bookmark)` - Insert bookmark
- `L18`: `suspend fun updateBookmark(bookmark: Bookmark)` - Update bookmark
- `L22`: `suspend fun deleteBookmark(bookmark: Bookmark)` - Delete bookmark

### FeedRepository.kt

- `L12`: `class FeedRepository` - Handles feed fetching with caching and retry logic
- `L19`: `suspend fun getCachedFeed(type: String): List<CivitaiImage>` - Returns cached feed items
- `L23`: `suspend fun clearCache(type: String)` - Clears cache for feed type
- `L27`: `suspend fun fetchImages(type, nsfw, sort, period, tagIds, limit, cursor, isNew, startOrderIndex): Pair<List<CivitaiImage>, String?>` - Fetches images from API with 3-attempt retry, caches results

### BackupRepository.kt

- `L18`: `class BackupRepository` - Handles JSON import/export of favorites, bookmarks, and settings
- `L28`: `suspend fun exportData(uri: Uri): Boolean` - Streams favorites, bookmarks, settings to JSON via Okio
- `L79`: `suspend fun importData(uri: Uri): Boolean` - Imports JSON backup data (settings, favorites, bookmarks)

### FavoritesRepository.kt

- `L39`: `class FavoritesRepository` - Manages favorite images with local file caching and background sync
- `L47-48`: `resourceChecksInProgress`, `resourceCheckTimestamps` - Track in-progress resource checks with staleness threshold
- `L49`: `toggleMutexes` - Per-image mutexes for thread-safe toggle operations
- `L50-51`: `repositoryJob`, `repositoryScope` - Supervisor job and IO coroutine scope
- `L53`: `syncDispatcher` - Limited parallelism (16) for sync operations
- `L56`: `_okHttpClient: OkHttpClient` - Mutable HTTP client for auth updates
- `L59`: `private fun getToggleMutex(id: Long): Mutex` - Gets or creates per-image mutex
- `L61-71`: `init block` - Launches temp file cleanup and stale check cleanup tasks
- `L74`: `private fun cleanupStaleResourceChecks()` - Periodic cleanup of stale resource checks
- `L88`: `private fun cleanupTempFiles()` - Cleans orphaned temp files from cache directory
- `L110`: `suspend fun repairSync(onProgress)` - Repairs local file sync for unsynced items
- `L136`: `private suspend fun getFavoritesDir(): File` - Resolves favorites directory path
- `L145`: `private suspend fun getMediaDir(type, contentType): File` - Resolves media subdirectory
- `L156`: `fun updateOkHttpClient(newClient: OkHttpClient)` - Updates HTTP client with new auth token
- `L160`: `override fun close()` - Cancels repository job scope
- `L166`: `val allFavorites: Flow<List<CivitaiImage>>` - Flow of all favorites as CivitaiImage
- `L169`: `val favoriteIds: Flow<Set<Long>>` - Flow of favorite IDs
- `L171`: `fun getFavoriteFlow(id: Long): Flow<FavoriteImage?>` - Returns Flow of favorite by ID
- `L173`: `suspend fun manualFetch(url: String): Boolean` - Manually fetches image metadata from URL
- `L192`: `suspend fun toggleFavorite(image: CivitaiImage)` - Adds/removes favorite with per-image mutex
- `L203`: `private suspend fun evictFromCoilCache(url: String)` - Evicts URL from Coil disk cache
- `L215`: `private fun finalizeFile(tempFile: File, finalFile: File): String?` - Renames temp file or copies as fallback
- `L231`: `suspend fun ensureFavoriteResources(image, force, onProgress)` - Downloads thumbnail/preview for favorited items
- `L572`: `private fun copyFile(source: File, destination: File): Boolean` - Copies source to destination
- `L588`: `private suspend fun downloadFile(url, destination, extractFrame, skipExtraction, webpQuality, expectedType, onProgress): Boolean` - Downloads files with video frame extraction and WebP conversion
- `L824`: `private suspend fun addFavorite(image: CivitaiImage)` - Adds item to database and triggers resource sync
- `L834`: `private suspend fun removeFavorite(image: CivitaiImage)` - Removes item from database and purges local files
- `L859`: `suspend fun clearUnusedResources(favoriteIds: Set<Long>)` - Deletes orphaned cached files
- `L891`: `suspend fun findDuplicateGroups(): List<List<CivitaiImage>>` - Finds duplicate images in favorites by content hash
- `L920`: `suspend fun removeDuplicates(duplicateGroups: List<List<CivitaiImage>>): Int` - Removes duplicate groups
- `L963`: `suspend fun getAllFavoritesSync(): List<FavoriteImage>` - Blocking fetch all favorites
- `L965`: `suspend fun importFavorites(favorites: List<FavoriteImage>)` - Bulk import favorites
- `L971`: `fun isFavorite(id: Long): Flow<Boolean>` - Flow of favorite status for single ID

### GalleryRepository.kt

- `L31`: `class GalleryRepository` - Scans and manages local downloaded files
- `L39`: `videoMetadataDispatcher` - Limited parallelism (16) for video metadata extraction
- `L40`: `_downloadedIds: MutableStateFlow<Set<Long>>` - Mutable state flow of downloaded IDs
- `L41`: `downloadedIds: StateFlow<Set<Long>>` - Flow of downloaded IDs
- `L42`: `refreshMutex` - Mutex for refresh operations
- `L43`: `downloadMutexes: ConcurrentHashMap<Long, Mutex>` - Per-image download mutexes
- `L45`: `private fun getDownloadMutex(id: Long): Mutex` - Gets or creates per-image download mutex
- `L49`: `suspend fun refreshDownloadedIds()` - Rescans download directory for IDs
- `L76`: `suspend fun scanDirectory(path: String?): List<CivitaiImage>` - Scans for media files, extracts metadata (dimensions, rotation)
- `L181`: `suspend fun deleteLocalFile(image: CivitaiImage): Boolean` - Deletes local file and updates state
- `L205`: `suspend fun downloadImage(image: CivitaiImage, onProgress): Result<String>` - Downloads image/video with progress and fallback URLs
- `L353`: `suspend fun findDuplicateGroups(): List<List<CivitaiImage>>` - Finds duplicate images in download directory
- `L372`: `suspend fun removeDuplicates(duplicateGroups: List<List<CivitaiImage>>): Int` - Removes duplicate groups
- `L379`: `private fun getDownloadDir(path: String?): File` - Resolves download directory

### UserPreferencesRepository.kt

- `L15`: `val Context.dataStore` - Extension property providing DataStore instance
- `L17`: `class UserPreferencesRepository` - Jetpack DataStore for settings persistence
- `L18`: `scope: CoroutineScope` - CoroutineScope for StateFlow operations
- `L20-55`: `object PreferencesKeys` - All preference keys (NSFW, SORT, PERIOD, TYPE, TAG_IDS, API_KEY, scroll positions, cursors, page limit, grid columns, paths, debug, player settings, backend, onboarding, NSFW favorites)
- `L57`: `val nsfw: Flow<String>` - Flow of NSFW filter setting
- `L60`: `val backendEnabled: Flow<Boolean>` - Flow of backend enabled setting
- `L65`: `val backendUrl: Flow<String>` - Flow of backend URL setting
- `L68`: `val backendApiKey: Flow<String>` - Flow of backend API key setting
- `L73`: `val lastRoute: Flow<String>` - Flow of last visited navigation route
- `L78`: `val sort: Flow<String>` - Flow of sort setting
- `L83`: `val period: Flow<String>` - Flow of period setting
- `L88`: `val type: Flow<String>` - Flow of media type setting
- `L91`: `val tagIds: Flow<String?>` - Flow of tag IDs setting
- `L94`: `val apiKey: Flow<String>` - Flow of API key setting
- `L97`: `fun feedScrollIndex(type: String): Flow<Int>` - Flow of scroll index for feed type
- `L107`: `fun feedScrollOffset(type: String): Flow<Int>` - Flow of scroll offset for feed type
- `L117`: `fun nextCursor(type: String): Flow<String?>` - Flow of pagination cursor for feed type
- `L123`: `val pageLimit: Flow<Int>` - Flow of page limit setting
- `L126`: `val gridColumns: Flow<Int>` - Flow of grid column count
- `L129`: `val downloadPath: Flow<String?>` - Flow of download directory path
- `L132`: `val favoritesPath: Flow<String?>` - Flow of favorites directory path
- `L135`: `val effectiveFavoritesPath: Flow<String>` - Flow with fallback to external files dir
- `L146`: `val debugEnabled: Flow<Boolean>` - Flow of debug logging setting
- `L151`: `val favoritesType: Flow<String>` - Flow of favorites media type filter
- `L156`: `val galleryType: Flow<String>` - Flow of gallery media type filter
- `L161`: `val hidePlayerControls: Flow<Boolean>` - Flow of player controls visibility
- `L166`: `val alwaysEnableHD: Flow<Boolean>` - Flow of HD media preference
- `L171`: `val alwaysMuteVideo: Flow<Boolean>` - Flow of video mute preference
- `L176`: `val feedVideoAutoplay: Flow<Boolean>` - Flow of video autoplay in feed
- `L181`: `val amoledMode: Flow<Boolean>` - Flow of AMOLED mode setting
- `L186`: `val onboardingCompleted: Flow<Boolean>` - Flow of onboarding completed
- `L191`: `data class InterceptorSettings` - Data class holding API key, debug, backend settings
- `L199-201`: `_interceptorSettings`, `interceptorSettings: StateFlow<InterceptorSettings>` - Combined interceptor settings
- `L203-219`: `init block` - Combines flows into interceptorSettings and updates CivitaiUrlBuilder
- `L221`: `suspend fun getCurrentSettings(): AppSettingsBackup` - Snapshot of all settings for backup
- `L254`: `suspend fun importSettings(settings: AppSettingsBackup)` - Bulk update settings from backup
- `L300`: `suspend fun updateFilters(nsfw, sort, period, type, tagIds)` - Updates NSFW, sort, period, type, tags
- `L323`: `suspend fun updateScrollPosition(type, index, offset)` - Debounced scroll position save
- `L346`: `suspend fun updateNextCursor(type, cursor)` - Updates pagination cursor
- `L358`: `suspend fun updatePageLimit(limit: Int)` - Updates page limit setting
- `L364`: `suspend fun updateGridColumns(columns: Int)` - Updates grid column count
- `L372`: `suspend fun updateDownloadPath(path: String?)` - Updates download directory path
- `L381`: `suspend fun updateFavoritesPath(path: String?)` - Updates favorites directory path
- `L390`: `suspend fun updateApiKey(key: String)` - Updates Civitai API key
- `L396`: `suspend fun updateBackendEnabled(enabled: Boolean)` - Toggles backend enabled
- `L404`: `suspend fun updateBackendUrl(url: String)` - Updates backend URL
- `L410`: `suspend fun updateBackendApiKey(key: String)` - Updates backend API key
- `L416`: `suspend fun updateDebugEnabled(enabled: Boolean)` - Toggles debug logging
- `L424`: `suspend fun updateFavoritesType(type: String)` - Updates favorites media type filter
- `L430`: `suspend fun updateGalleryType(type: String)` - Updates gallery media type filter
- `L436`: `suspend fun updateLastRoute(route: String)` - Tracks last visited navigation route
- `L442`: `suspend fun updateHidePlayerControls(enabled: Boolean)` - Toggles player controls visibility
- `L450`: `suspend fun updateAlwaysEnableHD(enabled: Boolean)` - Toggles HD media preference
- `L458`: `suspend fun updateAlwaysMuteVideo(enabled: Boolean)` - Toggles video mute preference
- `L466`: `suspend fun updateFeedVideoAutoplay(enabled: Boolean)` - Toggles video autoplay in feed
- `L474`: `suspend fun updateAmoledMode(enabled: Boolean)` - Toggles AMOLED black background mode
- `L480`: `suspend fun updateOnboardingCompleted(completed: Boolean)` - Marks onboarding as completed
- `L488`: `val showNsfwFavorites: Flow<Boolean>` - Flow of show NSFW favorites setting
- `L493`: `suspend fun setShowNsfwFavorites(show: Boolean)` - Sets show NSFW favorites

## app/src/main/kotlin/org/movzx/dibella/di/

### AppModule.kt

- `L26`: `object AppModule` - Hilt module for dependency injection
- `L29`: `fun provideMoshi(): Moshi` - Moshi JSON adapter with Kotlin support
- `L35`: `fun provideOkHttpClient(interceptor): OkHttpClient` - OkHttp with dispatcher, timeouts, and interceptors
- `L54`: `fun provideRetrofit(okHttpClient, moshi): Retrofit` - Retrofit with Civitai base URL
- `L64`: `fun provideCivitaiApi(retrofit): CivitaiApi` - Creates CivitaiApi service
- `L70`: `fun provideImageLoader(context, okHttpClient): ImageLoader` - Coil image loader with OkHttp and video frame decoder
- `L97`: `fun provideDatabase(context): AppDatabase` - Room database singleton
- `L102`: `fun provideFavoriteImageDao(database): FavoriteImageDao` - FavoriteImageDao provider
- `L107`: `fun provideFeedCacheDao(database): FeedCacheDao` - FeedCacheDao provider
- `L113`: `fun provideBookmarkDao(database): BookmarkDao` - BookmarkDao provider
- `L118`: `fun provideBookmarkRepository(bookmarkDao): BookmarkRepository` - BookmarkRepository provider
- `L126`: `fun provideUserPreferencesRepository(context): UserPreferencesRepository` - DataStore repo provider
- `L134`: `fun provideFavoritesRepository(...): FavoritesRepository` - Favorites repo with all dependencies

## app/src/main/kotlin/org/movzx/dibella/ui/screens/

### MainScreen.kt

- `L47`: `fun UiMessageEffect(uiMessage: Flow<Int>)` - Collects and displays UI messages as Toast
- `L59`: `fun InteractiveTopBar(...)` - Dynamic TopBar for selection mode, duplicate mode, and normal mode
- `L105`: `fun MainScreen(imageLoader: ImageLoader)` - Root composable with dual drawer layout, navigation, and full-screen image overlay
- `L636`: `fun AppNavigation(navController, startDestination, ...)` - NavHost with feed/favorites/gallery/bookmark destinations

### FeedScreen.kt

- `L19`: `fun FeedScreen(...)` - Feed screen with ViewModel, search/jump dialogs, and ImageGrid

### FavoritesScreen.kt

- `L21`: `fun FavoritesScreen(...)` - Favorites screen with batch operations and ImageGrid

### GalleryScreen.kt

- `L22`: `fun GalleryScreen(...)` - Gallery screen with batch delete and ImageGrid

### BookmarkScreen.kt

- `L36`: `fun BookmarkScreen(...)` - Bookmark screen with edit/delete dialogs and bookmark list
- `L346`: `fun BookmarkCard(bookmark, onLoad, onEdit, onDelete)` - Single bookmark card with tag display

### OnboardingScreen.kt

- `L30`: `data class OnboardingPage` - Page with title, description, icon, and gradient colors
- `L38`: `fun OnboardingScreen(onSkip, onFinish, modifier)` - 3-page onboarding with pager and dot indicators
- `L137`: `fun OnboardingPageContent(page: OnboardingPage)` - Page content with logo, title, and description

## app/src/main/kotlin/org/movzx/dibella/ui/components/

### AppBackHandler.kt

- `L11`: `fun AppBackHandler(...)` - Handles system back button: selection mode, drawer open, or double-press exit

### AppFab.kt

- `L22`: `fun AppFab(gridState, isLoading, hasMore, showBookmarkJump, onJumpClicked, onBookmarkClicked)` - FABs for scroll-to-top, scroll-to-bottom, jump-to-cursor, and bookmark

### AppScaffold.kt

- `L30`: `fun AppScaffold(...)` - Scaffold with pull-to-refresh, bottom progress bar, animated bars, and FAB
- `L307`: `fun EmptyState(viewMode: String)` - Empty grid placeholder with floating icon and gradient text

### BaseSidebar.kt

- `L12`: `fun BaseSidebar(title, onDismiss, footer, amoledMode, content)` - Foundation for sidebars with RectangleShape

### BookmarkDialog.kt

- `L16`: `fun BookmarkDialog(onApply, onDismiss)` - Dialog for creating a bookmark with title input

### ConfirmationDialog.kt

- `L17`: `fun ConfirmationDialog(title, message, onConfirm, onDismiss)` - Dialog with cancel/confirm buttons and haptic feedback

### DisplaySidebar.kt

- `L18`: `fun DisplaySidebar(currentRoute, pageLimit, gridColumns, type, onDismiss, ...)` - Settings for columns, page limit, media type filter, and duplicate scan

### FilterSidebar.kt

- `L24`: `data class CivitaiTag(val id: Int?, val name: String)` - Tag data class
- `L28`: `fun FilterSidebar(nsfw, sort, period, type, tagIds, onDismiss, onFilterChange, onResetFilters)` - NSFW, sort, period, type, and tag filters

### FullScreenImage.kt

- `L44`: `fun FullScreenImage(...)` - Pager with zoom, gestures, video player, favorite toggle, download, and delete
- `L795`: `fun AnimatedIconButton(onClick, modifier, enabled, content)` - IconButton with press animation

### ImageCard.kt

- `L49`: `fun ImageCard(...)` - Dynamic resolution card with heart animation, video autoplay, and error handling
- `L658`: `fun HeartParticles(modifier, onFinished)` - Visual particles for favorite animation

### ImageGrid.kt

- `L18`: `fun ImageGrid(...)` - Staggered grid with pinch-to-zoom column adjustment and visible item tracking

### MainBottomBar.kt

- `L31`: `fun MainBottomBar(currentRoute, onNavigate, feedCount, favoritesCount, galleryCount, bookmarkCount)` - Navigation between feed/favorites/gallery/bookmarks
- `L109`: `fun BottomNavItem(selected, onClick, icon, selectedIcon, label, count, selectedColor)` - Individual nav item with scale animation
- `L210`: `fun formatCount(count: Int): String` - Formats count with locale-aware number formatting

### MainTopBar.kt

- `L22`: `fun SelectionTopBar(selectedCount, onClose, onSelectAll, onAction, actionIcon, title)` - TopBar for batch selection mode
- `L81`: `fun MainTopBar(gridColumns, onShowDisplayOptions, onUpdateGridColumns, onShowFilters, onShowSettings)` - App logo, display/filter/settings triggers

### SettingsSidebar.kt

- `L21`: `fun SettingsSidebar(...)` - API key, paths, cache, import/export, backend, player, and appearance settings

### Shimmer.kt

- `L13`: `fun Modifier.shimmerBackground(): Modifier` - Animated loading shimmer effect with primary and secondary gradients

### SidebarSection.kt

- `L17`: `fun SidebarSection(title, modifier, icon, initiallyExpanded, content)` - Titled section wrapper with collapse/expand

### SkeletonGrid.kt

- `L13`: `fun SkeletonGrid(columnCount)` - Static placeholder grid with varying aspect ratios

### SpeedDialFab.kt

- `L18`: `data class SpeedDialItem(icon, label, onClick, containerColor, contentColor)` - Speed dial menu item
- `L27`: `fun SpeedDialFab(items, modifier, mainIcon)` - Expandable FAB menu with staggered animations

### VideoPlayer.kt

- `L34`: `fun VideoPlayer(...)` - ExoPlayer wrapper with zoom, pan, play/pause, speed, scale mode, and seek
- `L135`: `fun ExoVideoPlayer(...)` - Lower-level Media3 player with pool/dedicated modes, lifecycle binding, and FPS tracking

### VideoPlayerManager.kt

- `L15`: `class VideoPlayerManager(context)` - Pooled ExoPlayer management with thread-safe activeCount
- `L16`: `pool: MutableList<ExoPlayer>` - Pool of reusable ExoPlayer instances
- `L17`: `activePlayers: MutableSet<ExoPlayer>` - Set of currently active players
- `L19`: `var maxPoolSize: Int` - Configurable pool size limit
- `L23`: `var activeCount: Int` - Synchronized counter of active players
- `L27`: `fun updateLimit(newLimit: Int)` - Updates max pool size and shrinks if needed
- `L43`: `fun acquirePlayer(dataSourceFactory): ExoPlayer?` - Gets an available player from pool
- `L70`: `fun releasePlayer(player: ExoPlayer)` - Returns player to pool
- `L87`: `private fun createPlayer(dataSourceFactory): ExoPlayer` - Creates new ExoPlayer with codec filter
- `L121`: `fun releaseAll()` - Releases all players and clears pool
- `L130`: `val LocalVideoPlayerManager` - CompositionLocal for VideoPlayerManager

### VideoPlayerModels.kt

- `L5`: `enum class ScaleMode` - NORMAL, CROP, FULL scaling options
- `L11`: `data class BackendConfig(url: String, apiKey: String)` - Backend configuration
- `L13`: `val LocalBackendConfig` - CompositionLocal for BackendConfig

### ZoomableImage.kt

- `L24`: `fun ZoomableImage(model, imageLoader, modifier, thumbnailModel, onZoomChange, onTap)` - Pinch-to-zoom with double-tap

### SplashScreen.kt

- `L25`: `fun SplashScreen(onSplashFinished, modifier)` - Splash screen with animated logo and loading indicator

## app/src/main/kotlin/org/movzx/dibella/util/

### CivitaiUrlBuilder.kt

- `L5`: `object CivitaiUrlBuilder` - Logic for URL compression, expansion, and variant building
- `L16`: `var backendEnabled: Boolean` - Runtime backend enabled flag
- `L17`: `var backendUrl: String` - Runtime backend URL
- `L18`: `var backendApiKey: String` - Runtime backend API key
- `L19`: `const val BACKEND_DOWNLOAD_TIMEOUT_SECONDS` - Backend download timeout (60s)
- `L21`: `fun compressUrl(url: String): String` - Shortens Civitai URLs for database storage
- `L27`: `fun isCivitaiMediaUrl(url: String): Boolean` - Checks if URL belongs to Civitai CDN
- `L34`: `fun expandUrl(compressed: String, type: String?): String` - Restores full URLs from compressed format
- `L47`: `fun extractCivitaiUuid(url: String): String?` - Extracts UUID from URL with caching
- `L70`: `private fun wrapBackendUrl(type, quality, url, fallback): String` - Wraps URL with backend if enabled
- `L96`: `fun toBackendUrl(type, quality, uuid): String` - Builds backend URL
- `L100`: `fun buildBackendFeedUrl(originalUrl): String` - Builds backend feed URL from original
- `L109`: `fun getThumbnailUrl(url: String, width: Int): String` - Builds thumbnail variant URL
- `L115`: `fun getVideoThumbnailUrl(url: String): String` - Builds video thumbnail variant URL
- `L125`: `fun getVideoPreviewUrl(url: String): String` - Builds video preview variant URL
- `L135`: `fun getVideoOriginalUrl(url: String): String` - Builds video original file URL
- `L145`: `fun getImageOriginalUrl(url: String): String` - Builds original image file URL
- `L155`: `fun getFallbackChain(url: String): List<String>` - Returns list of fallback URLs for thumbnails
- `L182`: `fun getBaseUrl(url: String): String` - Extracts base URL from variant URL
- `L187`: `fun modifyUrl(url: String, variant: String): String` - Applies variant to URL
- `L197`: `private fun parseCivitaiUrl(url: String): ParseResult?` - Parses Civitai URL into components

### FileUtils.kt

- `L11`: `object FileUtils` - File magic byte detection and utility
- `L12`: `val IMAGE_EXTENSIONS` - List of supported image extensions (jpeg, jpg, webp)
- `L13`: `val VIDEO_EXTENSIONS` - List of supported video extensions (mp4, webm)
- `L17`: `fun saveBitmapAsWebP(bitmap, outputFile, quality): Boolean` - Saves bitmap as WebP
- `L31`: `fun convertFileToWebP(inputFile, outputFile, quality): Boolean` - Converts file to WebP via bitmap
- `L55`: `fun detectExtension(contentType, source, url): String` - Detects file extension from magic bytes, content-type, or URL
- `L105`: `fun getExtensionFromBytes(bytes: ByteArray): String?` - Magic byte to extension mapping
- `L120`: `fun isVideoFile(bytes: ByteArray): Boolean` - Checks if bytes represent a video file
- `L128`: `fun isImageFile(bytes: ByteArray): Boolean` - Checks if bytes represent an image file
- `L136`: `fun isRealMedia(file: File): Boolean` - Validates media file signatures
- `L178`: `fun calculateHash(file: File): String?` - Generates SHA-256 hash for duplicate detection
- `L210`: `suspend fun findDuplicateGroups(files: List<File>): List<List<File>>` - Groups files by content hash

### Logger.kt

- `L5`: `object Logger` - Conditional debug logging
- `L6`: `var debugEnabled: Boolean` - Global debug flag
- `L8`: `fun d(tag, message)` - Debug log (conditional)
- `L12`: `fun e(tag, message, throwable)` - Error log (always)
- `L16`: `fun w(tag, message)` - Warning log (conditional)
- `L20`: `fun v(tag, message)` - Verbose log (conditional)
- `L24`: `fun i(tag, message)` - Info log (conditional)

### UriUtils.kt

- `L8`: `fun resolveUriToPath(context: Context, uri: Uri): String?` - Converts document tree URI to file path

### MediaProcessor.kt

- `L9`: `class MediaProcessor` - Video frame extraction and image conversion
- `L10`: `fun extractVideoFrame(videoFile, outputFile, quality): Boolean` - Extracts single frame from video as WebP
- `L55`: `fun convertToWebP(inputFile, outputFile, quality): Boolean` - Converts image to WebP

### DuplicateDetector.kt

- `L10`: `object DuplicateDetector` - Duplicate detection for gallery files
- `L11`: `suspend fun findDuplicateGroups(files, context): List<List<CivitaiImage>>` - Finds duplicates by size + hash
- `L48`: `suspend fun removeDuplicateGroups(groups, context, onRefreshDownloadedIds): Int` - Removes duplicate files
- `L75`: `private fun extractId(file: File): Long` - Extracts image ID from filename

### Constants.kt

- `L3`: `object Constants` - App-wide constants
- `L4`: `AUTOPLAY_DEBOUNCE_DELAY_MS = 150L` - Autoplay debounce delay
- `L5`: `BACKGROUND_GLOW_DURATION_MS = 8000` - Background glow animation duration
- `L6`: `BOOKMARK_LOAD_DELAY_MS = 500L` - Bookmark load delay
- `L7`: `CACHE_BUFFER_SIZE = 8192` - Cache buffer size
- `L8`: `CLEANUP_INTERVAL_MS = 60000L` - Cleanup interval
- `L9`: `DEBOUNCE_DELAY_MS = 150L` - General debounce delay
- `L10`: `MAGIC_BYTE_BUFFER_SIZE = 64` - Magic byte buffer size
- `L11`: `MIN_THUMBNAIL_SIZE = 100L` - Minimum thumbnail size
- `L12`: `RESTORE_SCROLL_DELAY_MS = 500L` - Scroll restore delay
- `L13`: `STALE_CHECK_INTERVAL_MS = 30000L` - Stale check interval
- `L14`: `SWIPE_EDGE_THRESHOLD_DP = 150f` - Swipe edge threshold
- `L15`: `TEMP_FILE_MAX_AGE_MS = 3600000L` - Temp file max age
- `L16-19`: `VIDEO_POOL_SIZE_*` - Video player pool sizes per column count

### Utils.kt

- `L28`: `fun Modifier.scrollbar(state: ScrollState, width, color): Modifier` - Custom scrollbar for scrollable layouts
- `L57`: `fun Modifier.gridScrollbar(state: LazyStaggeredGridState, width, color, endOffset): Modifier` - Optimized scrollbar for LazyGrids
- `L103`: `private fun getEffectiveFavoritesDir(context, favoritesDir): File` - Resolves effective favorites directory
- `L107`: `suspend fun hasLocalCache(context, imageId, isVideo, favoritesDir): Boolean` - Checks thumbnail cache existence
- `L143`: `suspend fun hasFullCache(context, imageId, isVideo, favoritesDir): Boolean` - Checks full resolution media cache
- `L176`: `suspend fun resolveImageData(context, image, favoriteInfo, thumbnailWidth, useVideoPath, viewMode, favoritesDir): String` - Selects local/remote URL with optimization
- `L259`: `fun getThumbnailUrl(url: String, width: Int): String` - Appends width param to image URL (delegates to CivitaiUrlBuilder)
- `L263`: `fun getVideoThumbnailUrl(url: String): String` - Video thumbnail URL builder
- `L267`: `fun getVideoPreviewUrl(url: String): String` - Video preview URL builder
- `L271`: `fun getVideoOriginalUrl(url: String): String` - Video original file URL builder
- `L275`: `fun getOriginalUrl(url: String): String` - Appends original flag to image URL
- `L279`: `fun modifyCivitaiUrl(url: String, variant: String): String` - Generic URL variant modifier
- `L287`: `fun formatDuration(ms: Long): String` - Formats milliseconds to human readable duration (HH:MM:SS:CS)
- `L310`: `fun playerPoolSizeForColumns(columns: Int): Int` - Calculates optimal player pool size based on columns

## app/src/main/kotlin/org/movzx/dibella/model/

### CivitaiImage.kt

- `L8`: `data class CivitaiImage(id, url, width, height, nsfw, type, meta)` - Primary model for Civitai media items
- `L18`: `data class VideoMeta(size: Long?)` - Video metadata with size
- `L21`: `data class CivitaiApiResponse(items: List<CivitaiImage>, metadata: Metadata)` - API response wrapper
- `L23`: `data class Metadata(nextCursor: String?)` - Pagination metadata

### FavoriteImage.kt

- `L9`: `data class FavoriteImage(id, url, width, height, nsfw, type, timestamp, isSynced)` - Room entity for favorite images
- `L19`: `fun toCivitaiImage(): CivitaiImage` - Converts entity to CivitaiImage model
- `L32`: `companion object fun fromCivitaiImage(image, isSynced): FavoriteImage` - Factory method creating entity from model

### FeedItemCache.kt

- `L9`: `data class FeedItemCache(id, url, width, height, nsfw, type, feedType, orderIndex)` - Room entity for feed item caching
- `L19`: `fun toCivitaiImage(): CivitaiImage` - Converts entity to CivitaiImage model
- `L32`: `companion object fun fromCivitaiImage(image, feedType, index): FeedItemCache` - Factory method

### Bookmark.kt

- `L9`: `data class Bookmark(id, title, type, sort, period, nsfw, cursor, tags, timestamp)` - Room entity for bookmarks

### AppBackup.kt

- `L6`: `data class AppSettingsBackup(...)` - Model for settings backup with all preference fields
- `L36`: `data class FavoriteImageBackup(id, url, nsfw, type, timestamp)` - Model for favorite backup
- `L45`: `data class FeedItemBackup(id, url, width, height, nsfw, type, feedType, orderIndex)` - Model for feed item backup
- `L57`: `data class BookmarkBackup(id, title, type, sort, period, nsfw, cursor, tags, timestamp)` - Model for bookmark backup
- `L70`: `data class AppBackup(version, settings, favorites, bookmarks, feedItems)` - Root model for full application backup

## app/src/main/kotlin/org/movzx/dibella/viewmodel/

### BaseViewModel.kt

- `L17`: `abstract class BaseViewModel(repository, favoritesRepository, galleryRepository)` - Base ViewModel with shared actions
- `L22-24`: `companion object isImporting: Boolean` - Static flag to prevent scroll reset during import
- `L26-27`: `_uiMessage: MutableSharedFlow<Int>`, `uiMessage: SharedFlow<Int>` - UI message flow
- `L30`: `fun updateGridColumns(columns: Int)` - Persists column preference
- `L34`: `fun updatePageLimit(limit: Int)` - Persists page limit preference
- `L38`: `fun saveScrollPosition(type, index, offset)` - Debounced scroll position save
- `L42`: `protected fun sendMessage(@StringRes resId: Int)` - Emits toast message
- `L46`: `fun toggleFavorite(image: CivitaiImage)` - Delegates to FavoritesRepository
- `L50`: `fun retryThumbnail(url: String, onComplete: () -> Unit)` - Retries thumbnail fetch with delay
- `L60`: `suspend fun ensureFavoriteResources(image, force, onProgress)` - Downloads thumbnail/preview
- `L68`: `suspend fun ensureFavoriteResourcesThrottled(image, force, onProgress)` - Throttled resource synchronization
- `L78`: `abstract fun downloadImage(image: CivitaiImage)` - Initiates image download (abstract)
- `L80`: `protected fun performDownload(image, onUpdateProgress, onSuccess)` - Download with progress tracking

### FeedViewModel.kt

- `L21`: `class FeedViewModel` - Main feed with pagination and filters
- `L30-31`: `_uiState: MutableStateFlow<FeedUiState>`, `uiState: StateFlow<FeedUiState>` - UI state flow
- `L32-33`: `imageFeed`, `videoFeed` - Cached image and video feeds
- `L34-37`: `imageCursor`, `videoCursor`, `currentImageCursor`, `currentVideoCursor` - Pagination cursors
- `L38`: `loadingJob: Job?` - Current loading job reference
- `L39`: `isFirstSettingsLoad: Boolean` - Flag for initial settings load
- `L40`: `isJumping: Boolean` - Flag for cursor jump in progress
- `L42-150`: `init block` - Combines repository flows, initializes state from cache, loads initial images
- `L152`: `fun refresh()` - Clears cursor and reloads feed
- `L183`: `fun loadMore()` - Loads next page with cursor
- `L193`: `fun jumpToCursor(cursor: String)` - Jumps to specific cursor
- `L206`: `private fun loadImages(isNew, cursorOverride)` - Fetches and caches feed items
- `L272`: `fun updateFilters(nsfw, sort, period, type, tagIds)` - Updates filter params, invalidates restoration
- `L276`: `fun resetFilters()` - Resets all filters to defaults
- `L282`: `override fun downloadImage(image: CivitaiImage)` - Initiates image download with progress
- `L299`: `fun saveBookmark(title: String)` - Saves current feed state as bookmark
- `L328`: `fun loadBookmark(bookmark: Bookmark)` - Loads bookmark settings and jumps to cursor
- `L348`: `fun markRestored()` - Sets isRestored flag after scroll restoration

### FavoritesViewModel.kt

- `L17`: `class FavoritesViewModel` - Manages favorites list
- `L24-26`: `_uiState: MutableStateFlow<FavoritesUiState>`, `uiState: StateFlow<FavoritesUiState>` - UI state flow
- `L28-80`: `init block` - Combines favorites flow, applies type filter
- `L82`: `fun updateType(type: String)` - Updates media type filter
- `L90`: `fun toggleSelection(id: Long)` - Toggles selection for batch operations
- `L105`: `fun clearSelection()` - Clears all selections
- `L111`: `fun selectAll()` - Selects all visible items
- `L122`: `fun batchUnfavorite()` - Removes selected favorites
- `L153`: `fun getFavoriteFlow(id: Long): Flow<FavoriteImage?>` - Returns Flow of favorite by ID
- `L155`: `fun forceRedownload(image: CivitaiImage)` - Re-downloads favorite resource
- `L174`: `override fun downloadImage(image: CivitaiImage)` - Initiates image download with progress
- `L191`: `fun findDuplicates()` - Finds duplicate images in favorites
- `L225`: `fun clearDuplicatesMode()` - Exits duplicate detection mode
- `L231`: `fun removeDuplicates()` - Removes duplicate groups
- `L265`: `fun markRestored()` - Sets isRestored flag after scroll restoration

### GalleryViewModel.kt

- `L16`: `class GalleryViewModel` - Manages local gallery browsing
- `L23-24`: `_uiState: MutableStateFlow<GalleryUiState>`, `uiState: StateFlow<GalleryUiState>` - UI state flow
- `L26-53`: `init block` - Combines gallery flow, applies type filter, monitors downloaded IDs
- `L55`: `fun refresh()` - Rescans download directory with filtering
- `L59`: `private suspend fun performRefresh()` - Executes gallery scan
- `L74`: `fun updateType(type: String)` - Updates media type filter
- `L78`: `fun toggleSelection(id: Long)` - Toggles selection for batch operations
- `L88`: `fun clearSelection()` - Clears all selections
- `L92`: `fun selectAll()` - Selects all visible items
- `L102`: `fun batchDelete()` - Deletes selected local files
- `L126`: `fun deleteLocalFile(image: CivitaiImage)` - Deletes local media file
- `L134`: `override fun downloadImage(image: CivitaiImage)` - Initiates image download with progress
- `L152`: `fun findDuplicates()` - Finds duplicate images in gallery
- `L186`: `fun clearDuplicatesMode()` - Exits duplicate detection mode
- `L190`: `fun removeDuplicates()` - Removes duplicate groups
- `L227`: `fun markRestored()` - Sets isRestored flag after scroll restoration

### SettingsViewModel.kt

- `L19`: `class SettingsViewModel` - Manages app settings and import/export
- `L29-30`: `_uiState: MutableStateFlow<SettingsUiState>`, `uiState: StateFlow<SettingsUiState>` - UI state flow
- `L31-32`: `_exitEvent: MutableSharedFlow<Unit>`, `exitEvent: SharedFlow<Unit>` - SharedFlow for app exit trigger
- `L34-122`: `init block` - Combines repository flows, loads settings, monitors player settings
- `L124`: `override fun downloadImage(image: CivitaiImage)` - No-op override (gallery handles downloads)
- `L126`: `fun updateLastRoute(route: String)` - Tracks last visited navigation route
- `L130`: `fun updateApiKey(key: String)` - Updates Civitai API key
- `L137`: `fun updateDownloadPath(path: String?)` - Updates download directory
- `L141`: `fun updateFavoritesPath(path: String?)` - Updates favorites directory
- `L145`: `fun updateDebugEnabled(enabled: Boolean)` - Toggles debug logging
- `L149`: `fun updateBackendEnabled(enabled: Boolean)` - Toggles backend enabled
- `L153`: `fun updateBackendUrl(url: String)` - Updates backend URL
- `L157`: `fun updateBackendApiKey(key: String)` - Updates backend API key
- `L161`: `fun updateHidePlayerControls(enabled: Boolean)` - Toggles video player controls visibility
- `L165`: `fun updateAlwaysEnableHD(enabled: Boolean)` - Toggles high-definition media preference
- `L169`: `fun updateAlwaysMuteVideo(enabled: Boolean)` - Toggles video player mute preference
- `L173`: `fun updateFeedVideoAutoplay(enabled: Boolean)` - Toggles video autoplay in feed
- `L177`: `fun updateAmoledMode(enabled: Boolean)` - Toggles AMOLED black background mode
- `L181`: `fun clearImageCache()` - Clears Coil image cache
- `L190`: `fun updateCacheSize()` - Computes and updates cache size
- `L196`: `private fun formatSize(bytes: Long): String` - Formats bytes to human-readable string
- `L208`: `fun exportData(uri: Uri)` - Exports backup to URI
- `L217`: `fun importData(uri: Uri)` - Imports backup from URI (triggers app exit on success)

### BookmarkViewModel.kt

- `L12`: `data class BookmarkUiState(bookmarks, isLoading, bookmarkCount)` - UI state for bookmarks
- `L19`: `class BookmarkViewModel` - Manages bookmarks
- `L22-23`: `_uiState: MutableStateFlow<BookmarkUiState>`, `uiState: StateFlow<BookmarkUiState>` - UI state flow
- `L25-37`: `init block` - Collects bookmarks from repository
- `L39`: `fun deleteBookmark(bookmark: Bookmark)` - Deletes a bookmark
- `L43`: `fun updateBookmarkTitle(bookmark, newTitle)` - Updates bookmark title and metadata

### BaseUiState.kt / FeedUiState.kt / FavoritesUiState.kt / GalleryUiState.kt / SettingsUiState.kt

- `L5`: `interface BaseUiState` - Common UI state properties (isLoading, isRestored)
- `L5`: `data class FeedUiState` - UI state for feed (images, cursors, filters, scroll position)
- `L5`: `data class FavoritesUiState` - UI state for favorites (images, selection, duplicates)
- `L5`: `data class GalleryUiState` - UI state for gallery (images, selection, duplicates)
- `L3`: `data class SettingsUiState` - UI state for settings (API key, paths, player settings)

## app/src/main/kotlin/org/movzx/dibella/ui/theme/

### Theme.kt

- `L20`: `val DibellaTypography` - Custom typography with SemiBold/Medium/Normal weights
- `L129`: `val DibellaShapes` - Custom shapes (extraLarge: 20dp, large: 16dp, medium: 12dp, small: 8dp, extraSmall: 4dp)
- `L139`: `fun DibellaTheme(darkTheme, dynamicColor, content)` - Material 3 theme with dynamic colors and dark fallback

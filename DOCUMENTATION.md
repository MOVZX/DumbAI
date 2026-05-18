# Dibella — Complete Reference

This document is the **single source of truth** for the Dibella Android codebase. It combines architecture, file structure, and per-file function mappings with descriptions. Use it to understand the system, navigate files, and find any function quickly.

---

## 1. Project Structure

```
Dibella/
├── app/src/main/kotlin/org/movzx/dibella/
│   ├── DibellaApplication.kt          # Application entry point (Hilt + Coil singleton)
│   ├── MainActivity.kt                 # Main activity: permissions → splash → onboarding → main
│   ├── api/                             # API layer: Retrofit interfaces + OkHttp interceptors
│   │   ├── CivitaiApi.kt               # Feed image fetching
│   │   ├── CivitaiSearchApi.kt         # Multi-search API
│   │   ├── CivitaiInterceptor.kt       # Auth headers, backend redirect, logging
│   │   ├── CivitaiThumbnailInterceptor.kt  # Thumbnail fallback + magic byte validation
│   │   └── CivitaiBackendRetryInterceptor.kt  # Backend retry on 5xx/4xx errors
│   ├── data/                            # Data layer: Room DAOs + repositories
│   │   ├── AppDatabase.kt              # Room DB (3 entities: favorites, feed cache, bookmarks)
│   │   ├── FavoriteImageDao.kt         # Favorite images CRUD
│   │   ├── FeedCacheDao.kt             # Feed cache CRUD
│   │   ├── BookmarkDao.kt              # Bookmarks CRUD
│   │   ├── BookmarkRepository.kt       # Bookmark repository
│   │   ├── BackupRepository.kt         # JSON import/export
│   │   ├── FeedRepository.kt           # Feed fetching + caching
│   │   ├── FavoritesRepository.kt      # Favorites + local file caching
│   │   ├── GalleryRepository.kt        # Local gallery scanning + downloads
│   │   ├── SearchRepository.kt         # Search API queries
│   │   └── UserPreferencesRepository.kt  # Jetpack DataStore (all preferences)
│   ├── di/                              # Dependency injection (Hilt)
│   │   └── AppModule.kt                # All singleton providers
│   ├── model/                           # Data models
│   │   ├── CivitaiImage.kt             # Core media model (id, url, width, height, nsfw, type, meta)
│   │   ├── SearchModels.kt             # Search models (CivitaiSearchResult, SearchQuery, SearchRequest, SearchResponse)
│   │   ├── FavoriteImage.kt            # Room entity for favorites
│   │   ├── FeedItemCache.kt            # Room entity for feed cache
│   │   ├── Bookmark.kt                 # Room entity (supports feed + search bookmarks)
│   │   └── AppBackup.kt                # Backup serialization models
│   ├── ui/
│   │   ├── screens/                    # Screen composables
│   │   │   ├── MainScreen.kt           # Root navigation + dual drawer layout
│   │   │   ├── FeedScreen.kt           # Feed screen
│   │   │   ├── SearchScreen.kt         # Search screen
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
│   │   │   ├── BookmarkEditDialog.kt   # Bookmark edit dialog
│   │   │   ├── ConfirmationDialog.kt   # Confirmation dialog
│   │   │   ├── DisplaySidebar.kt       # Display settings sidebar
│   │   │   ├── FilterSidebar.kt        # Filter sidebar (NSFW/sort/period)
│   │   │   ├── SearchFilterSidebar.kt  # Search filter sidebar
│   │   │   ├── FullScreenImage.kt      # Full-screen image viewer
│   │   │   ├── ImageCard.kt            # Grid card with heart animation
│   │   │   ├── ImageGrid.kt            # Staggered grid layout
│   │   │   ├── JumpDialog.kt           # Jump-to-cursor dialog
│   │   │   ├── MainBottomBar.kt        # Bottom navigation bar
│   │   │   ├── MainTopBar.kt           # Top app bar
│   │   │   ├── ModernDialog.kt         # Modern styled dialog
│   │   │   ├── ModernEmptyState.kt     # Animated empty state
│   │   │   ├── SettingsSidebar.kt      # Settings sidebar
│   │   │   ├── Shimmer.kt              # Loading shimmer effect
│   │   │   ├── SidebarSection.kt       # Collapsible sidebar section
│   │   │   ├── SkeletonGrid.kt         # Placeholder grid
│   │   │   ├── SpeedDialFab.kt         # Expandable FAB menu
│   │   │   ├── SplashScreen.kt         # Splash screen
│   │   │   ├── TagSelectionDialog.kt   # Multi-select tag picker
│   │   │   ├── VideoPlayer.kt          # ExoPlayer wrapper
│   │   │   ├── VideoPlayerManager.kt   # Pooled ExoPlayer manager
│   │   │   ├── VideoPlayerModels.kt    # ScaleMode enum + BackendConfig
│   │   │   └── ZoomableImage.kt        # Pinch-to-zoom image
│   │   └── theme/
│   │       └── Theme.kt                # Material 3 theme
│   ├── util/                            # Utility functions
│   │   ├── CivitaiUrlBuilder.kt        # URL compression/variant building
│   │   ├── Constants.kt                # App-wide constants
│   │   ├── DuplicateDetector.kt        # Duplicate detection utility
│   │   ├── FileUtils.kt                # File I/O and magic byte detection
│   │   ├── Logger.kt                   # Conditional debug logging
│   │   ├── MediaProcessor.kt           # Video frame extraction + WebP
│   │   ├── OkHttpExtensions.kt         # OkHttp timeout extensions
│   │   ├── UriUtils.kt                 # URI to path resolution
│   │   └── Utils.kt                    # Helper functions
│   └── viewmodel/                       # ViewModels + UI states
│       ├── BaseViewModel.kt            # Base with shared actions
│       ├── BaseUiState.kt              # Common UI state interface
│       ├── FeedUiState.kt              # Feed UI state
│       ├── SearchUiState.kt            # Search UI state
│       ├── FavoritesUiState.kt         # Favorites UI state
│       ├── GalleryUiState.kt           # Gallery UI state
│       ├── SettingsUiState.kt          # Settings UI state
│       ├── FeedViewModel.kt            # Feed with pagination
│       ├── SearchViewModel.kt          # Search with pagination
│       ├── FavoritesViewModel.kt       # Favorites management
│       ├── GalleryViewModel.kt         # Gallery management
│       ├── SettingsViewModel.kt        # Settings management
│       ├── BookmarkViewModel.kt        # Bookmark management
│       └── BookmarkUiState.kt          # Bookmark UI state (defined inline in BookmarkViewModel.kt)
└── app/src/test/kotlin/               # Unit tests
    ├── api/CivitaiBackendRetryInterceptorTest.kt
    ├── api/CivitaiThumbnailInterceptorTest.kt
    ├── model/AppBackupTest.kt
    ├── util/CivitaiUrlBuilderTest.kt
    └── util/FileUtilsTest.kt
```

---

## 2. Architecture Overview

### MVVM with Centralized Logic

Most ViewModels extend `BaseViewModel`, which centralizes shared behavior (`SearchViewModel` and `BookmarkViewModel` extend `ViewModel` directly but still expose the same shared action pattern):

| Shared Feature             | Location                                                                                           |
| -------------------------- | -------------------------------------------------------------------------------------------------- |
| **Shared Actions**         | `toggleFavorite`, `ensureFavoriteResources`, `ensureFavoriteResourcesThrottled`, `performDownload` |
| **Persistent Preferences** | Via `UserPreferencesRepository` for grid, request, scroll settings                                 |
| **Scroll Management**      | `saveScrollPosition(type, index, offset)` — debounced (500ms) save to DataStore                    |
| **Message Bus**            | `sendMessage(resId)` — emits via `SharedFlow<Int>` for Toasts                                      |
| **Route Awareness**        | `setActiveRoute(route)` — gates background operations by active screen                             |
| **Concurrency**            | `Dispatchers.IO.limitedParallelism(16)` for sync-heavy operations                                  |

### State Management

Unidirectional Data Flow (UDF):

1. Each screen has a single `UiState` data class (implements `BaseUiState` or is dedicated).
2. UI observes via `collectAsState()`.
3. User actions call ViewModel functions → ViewModel updates `MutableStateFlow<UiState>`.

### Data Layer

| Component          | Technology                | Purpose                                                         |
| ------------------ | ------------------------- | --------------------------------------------------------------- |
| **Room DB**        | 3 entities                | `favorite_images`, `feed_cache`, `bookmarks`                    |
| **DataStore**      | Preferences               | All settings, scroll positions, cursors, search state           |
| **Repositories**   | 8 repos                   | Favorites, Feed, Gallery, Bookmark, Search, Preferences, Backup |
| **Network**        | Retrofit + OkHttp + Moshi | Feed API, Search API, backend redirect, thumbnail retry         |
| **Image Loading**  | Coil3                     | Thumbnails, previews, video frames                              |
| **Video Playback** | Media3 / ExoPlayer        | Pooled players, pinch-to-zoom, autoplay                         |

### Key Features

| Feature                | Key Components                                                                                |
| ---------------------- | --------------------------------------------------------------------------------------------- |
| **Search**             | `SearchRepository`, `SearchViewModel`, `SearchScreen`, `SearchFilterSidebar`, `SearchUiState` |
| **Video Engine**       | `VideoPlayerManager` (pooled ExoPlayer), `VideoPlayer`, `ExoVideoPlayer`, dynamic pool sizing |
| **Dual Sidebar**       | Left: `DisplaySidebar`, Right: `FilterSidebar` / `SearchFilterSidebar` / `SettingsSidebar`    |
| **Local Caching**      | `FavoritesRepository.ensureFavoriteResources()` — thumbnails/previews in WebP                 |
| **Duplicates**         | `FileUtils.calculateHash()` (SHA-256), groups by size→hash, in Favorites + Gallery            |
| **Image Verification** | `CivitaiThumbnailInterceptor` — magic byte validation, fallback chain                         |
| **Bookmarks**          | Feed bookmarks (cursor-based) + Search bookmarks (query+offset-based)                         |
| **Full-Screen Viewer** | `FullScreenImage` — swipe pager, zoom, video controls, favorite/download                      |

---

## 3. Function Mappings

### Application Entry

#### `DibellaApplication.kt`

- `L13`: `class DibellaApplication` — Application class with Hilt and `SingletonImageLoader` factory
- `L14`: `imageLoader: ImageLoader` — Injected Coil image loader
- `L15`: `favoritesRepository: FavoritesRepository` — Injected favorites repository
- `L17`: `override fun newImageLoader(context: PlatformContext)` — Returns singleton Coil image loader
- `L21`: `override fun onTerminate()` — Closes `FavoritesRepository` on app termination

#### `MainActivity.kt`

- `L28`: `enum class RightSidebarType` — Right sidebar modes: `FILTERS`, `SEARCH_FILTERS`, `SETTINGS`
- `L35`: `class MainActivity` — Main activity entry point (FragmentActivity + Hilt)
- `L36`: `imageLoader: ImageLoader` — Injected Coil image loader
- `L37`: `preferencesRepository: UserPreferencesRepository` — Injected preferences repository
- `L39`: `permissionsToRequest` — Runtime permissions list (READ_MEDIA_IMAGES/VIDEO or READ_EXTERNAL_STORAGE)
- `L48`: `permissionLauncher` — Activity result launcher for permissions
- `L69-70`: `_permissionsGranted`, `_showOnboarding` — Mutable state flows
- `L72`: `permissionsGranted: State<Boolean>` — Public permission state
- `L75`: `override fun onCreate(savedInstanceState: Bundle?)` — Edge-to-edge UI, permissions, onboarding, content
- `L85`: `lifecycleScope.launch` — Collects `onboardingCompleted` flow to show onboarding
- `L91`: `setContent { ... }` — Compose content: theme → splash → onboarding → MainScreen

---

### API Layer

#### `CivitaiApi.kt`

- `L7`: `interface CivitaiApi` — Retrofit service for feed images
- `L9`: `suspend fun getImages(limit, nsfw, sort, period, type, tags, withMeta, useIndex, cursor)` — Fetches paginated images

#### `CivitaiSearchApi.kt`

- `L9`: `interface CivitaiSearchApi` — Retrofit service for multi-search API
- `L11`: `suspend fun search(request, authorization)` — POSTs `SearchRequest` to multi-search endpoint

#### `CivitaiInterceptor.kt`

- `L12`: `class CivitaiInterceptor` — OkHttp interceptor for auth, backend redirect, logging
- `L14`: `override fun intercept(chain)` — Injects Bearer token + User-Agent, redirects Civitai media/feeds/search to backend, adjusts timeouts

#### `CivitaiThumbnailInterceptor.kt`

- `L11`: `class CivitaiThumbnailInterceptor` — Handles 404/403 fallbacks for thumbnails
- `L13-15`: `companion object` — Constants: `VIDEO_TIMEOUT_MS`, `IMAGE_TIMEOUT_MS`, `MAX_RETRIES`
- `L18-20`: `videoThumbnailTimeout`, `imageThumbnailTimeout`, `maxRetries` — Instance copies
- `L22`: `private fun cacheable(response)` — Adds Cache-Control to successful responses
- `L28`: `private fun Response?.closeQuietly()` — Safely closes OkHttp response
- `L36`: `private fun safeProceed(chain, request, url, timeoutMs)` — Executes with timeout, returns null on failure
- `L56`: `override fun intercept(chain)` — Tries fallback chain with retry, caches valid responses
- `L96`: `private fun isValidMedia(response)` — Validates real image/video data (magic bytes + content-type)

#### `CivitaiBackendRetryInterceptor.kt`

- `L12`: `class CivitaiBackendRetryInterceptor` — Retries backend requests on 5xx/4xx errors
- `L15`: `companion object` — Constants: `RETRY_DELAY_SECONDS = 15L`, `MAX_RETRIES = 5`
- `L20`: `override fun intercept(chain)` — Retries backend requests with 15s delay between attempts
- `L87`: `private fun isBackendRequest(url)` — Checks if URL matches backend URL prefix

---

### Data Layer

#### `AppDatabase.kt`

- `L16`: `abstract class AppDatabase` — Room DB with 3 entities: `FavoriteImage`, `FeedItemCache`, `Bookmark`
- `L17`: `abstract fun favoriteImageDao()` — DAO for `favorite_images`
- `L19`: `abstract fun feedCacheDao()` — DAO for `feed_cache`
- `L21`: `abstract fun bookmarkDao()` — DAO for `bookmarks`
- `L23-42`: `companion object` — Singleton with volatile `INSTANCE` and `getDatabase()`
- `L26`: `fun getDatabase(context)` — Thread-safe singleton DB provider

#### `FavoriteImageDao.kt`

- `L9`: `interface FavoriteImageDao` — Room DAO for `favorite_images`
- `L11`: `fun getAllFavorites()` — Flow of all favorites ordered by timestamp DESC
- `L13`: `suspend fun _getAllFavoritesSync()` — Blocking fetch (internal)
- `L16`: `suspend fun getAllFavoritesSync()` — Blocking fetch with logging
- `L24`: `suspend fun _insertFavorite(image)` — Insert/replace single favorite (internal)
- `L27`: `suspend fun insertFavorite(image)` — Insert with logging
- `L34`: `suspend fun _insertFavorites(images)` — Bulk insert (internal)
- `L37`: `suspend fun insertFavorites(images)` — Bulk insert with logging
- `L44`: `suspend fun _deleteFavorite(image)` — Delete single favorite (internal)
- `L46`: `suspend fun deleteFavorite(image)` — Delete with logging
- `L53`: `fun isFavorite(id)` — Flow<Boolean> checking if ID is favorited
- `L56`: `suspend fun isFavoriteDirect(id)` — Blocking favorite check
- `L59`: `fun getAllFavoriteIds()` — Flow of all favorite IDs
- `L61`: `suspend fun getFavorite(id)` — Fetch single favorite by ID
- `L64`: `fun getFavoriteFlow(id)` — Flow of single favorite by ID

#### `FeedCacheDao.kt`

- `L8`: `interface FeedCacheDao` — Room DAO for `feed_cache`
- `L10`: `suspend fun getFeed(feedType)` — Fetch cached feed by type
- `L13`: `suspend fun getAllFeedItemsSync()` — Blocking fetch of all cached items
- `L18`: `suspend fun clearFeed(feedType)` — Clear cache for feed type
- `L21`: `suspend fun replaceFeed(feedType, items)` — Atomic replace (clear + insert)
- `L29`: `suspend fun insertOrUpdateFeed(items)` — Upsert existing items

#### `BookmarkDao.kt`

- `L8`: `interface BookmarkDao` — Room DAO for `bookmarks`
- `L10`: `fun getAllBookmarks()` — Flow of all bookmarks
- `L12`: `suspend fun getAllBookmarksSync()` — Blocking fetch
- `L15`: `suspend fun insertBookmark(bookmark)` — Insert or replace bookmark
- `L17`: `suspend fun updateBookmark(bookmark)` — Update existing bookmark
- `L19`: `suspend fun deleteBookmark(bookmark)` — Delete bookmark
- `L21`: `suspend fun clearAll()` — Delete all bookmarks

#### `BookmarkRepository.kt`

- `L9`: `class BookmarkRepository` — Wraps `BookmarkDao`
- `L10`: `fun getAllBookmarks()` — Flow of all bookmarks
- `L12`: `suspend fun getAllBookmarksSync()` — Blocking fetch
- `L14`: `suspend fun addBookmark(bookmark)` — Insert bookmark
- `L18`: `suspend fun updateBookmark(bookmark)` — Update bookmark
- `L22`: `suspend fun deleteBookmark(bookmark)` — Delete bookmark

#### `FeedRepository.kt`

- `L12`: `class FeedRepository` — Feed fetching with caching and retry
- `L19`: `suspend fun getCachedFeed(type)` — Returns cached feed items
- `L23`: `suspend fun clearCache(type)` — Clears cache for feed type
- `L27`: `suspend fun fetchImages(type, nsfw, sort, period, tagIds, limit, cursor, isNew, startOrderIndex)` — Fetches with 3-attempt retry, caches results

#### `GalleryRepository.kt`

- `L32`: `class GalleryRepository` — Scans and manages local downloaded files
- `L40`: `videoMetadataDispatcher` — Limited parallelism (16) for video metadata extraction
- `L41`: `_downloadedIds: MutableStateFlow<Set<Long>>` — Mutable state flow of downloaded IDs
- `L42`: `downloadedIds: StateFlow<Set<Long>>` — Flow of downloaded IDs
- `L43`: `refreshMutex` — Mutex for refresh operations
- `L44`: `downloadMutexes: ConcurrentHashMap<Long, Mutex>` — Per-image download mutexes
- `L46`: `private fun getDownloadMutex(id)` — Gets or creates per-image download mutex
- `L50`: `suspend fun refreshDownloadedIds()` — Rescans download directory for IDs
- `L77`: `suspend fun scanDirectory(path)` — Scans for media files, extracts metadata (dimensions, rotation)
- `L184`: `suspend fun deleteLocalFile(image)` — Deletes local file and updates state
- `L208`: `suspend fun downloadImage(image, onProgress)` — Downloads image/video with progress and fallback URLs
- `L356`: `suspend fun findDuplicateGroups()` — Finds duplicate images in download directory
- `L375`: `suspend fun removeDuplicates(duplicateGroups)` — Removes duplicate groups
- `L382`: `private fun getDownloadDir(path)` — Resolves download directory

#### `SearchRepository.kt`

- `L13`: `class SearchRepository` — Handles search API queries with retry
- `L19`: `suspend fun search(query, type, sort, limit, offset, bearerToken)` — Executes multi-search with 3-attempt retry, returns `(List<CivitaiSearchResult>, totalHits)`

#### `BackupRepository.kt`

- `L18`: `class BackupRepository` — JSON import/export of favorites, bookmarks, settings
- `L28`: `suspend fun exportData(uri)` — Streams favorites, bookmarks, settings to JSON via Okio
- `L96`: `suspend fun importData(uri)` — Imports JSON backup (settings, favorites, bookmarks)

#### `UserPreferencesRepository.kt`

- `L15`: `val Context.dataStore` — Extension property providing DataStore instance
- `L17`: `class UserPreferencesRepository` — Jetpack DataStore for all settings
- `L18`: `scope: CoroutineScope` — CoroutineScope for StateFlow operations
- `L20-63`: `object PreferencesKeys` — All preference keys (NSFW, SORT, PERIOD, TYPE, TAG_IDS, API_KEY, SEARCH_API_KEY, scroll positions, cursors, page limit, grid columns, paths, debug, player settings, backend, onboarding, NSFW favorites, search state)
- `L65`: `val nsfw: Flow<String>` — NSFW filter setting
- `L68`: `val backendEnabled: Flow<Boolean>` — Backend enabled setting
- `L73`: `val backendUrl: Flow<String>` — Backend URL setting
- `L76`: `val backendApiKey: Flow<String>` — Backend API key setting
- `L81`: `val lastRoute: Flow<String>` — Last visited navigation route
- `L86`: `val sort: Flow<String>` — Sort setting
- `L91`: `val period: Flow<String>` — Period setting
- `L96`: `val type: Flow<String>` — Media type setting
- `L99`: `val tagIds: Flow<String?>` — Tag IDs setting
- `L102`: `val apiKey: Flow<String>` — API key setting
- `L105`: `val searchApiKey: Flow<String>` — Search API bearer token
- `L111`: `val searchQuery: Flow<String>` — Current search query
- `L116`: `val searchType: Flow<String>` — Search content type
- `L121`: `val searchSort: Flow<String>` — Search sort order
- `L126`: `fun feedScrollIndex(type)` — Flow of scroll index for feed type
- `L136`: `fun feedScrollOffset(type)` — Flow of scroll offset for feed type
- `L146`: `fun nextCursor(type)` — Flow of pagination cursor for feed type
- `L152`: `val pageLimit: Flow<Int>` — Page limit setting
- `L155`: `val gridColumns: Flow<Int>` — Grid column count
- `L158`: `val downloadPath: Flow<String?>` — Download directory path
- `L161`: `val favoritesPath: Flow<String?>` — Favorites directory path
- `L164`: `val effectiveFavoritesPath: Flow<String>` — With fallback to external files dir
- `L175`: `val debugEnabled: Flow<Boolean>` — Debug logging setting
- `L180`: `val favoritesType: Flow<String>` — Favorites media type filter
- `L185`: `val galleryType: Flow<String>` — Gallery media type filter
- `L190`: `val hidePlayerControls: Flow<Boolean>` — Player controls visibility
- `L195`: `val alwaysEnableHD: Flow<Boolean>` — HD media preference
- `L200`: `val alwaysMuteVideo: Flow<Boolean>` — Video mute preference
- `L205`: `val feedVideoAutoplay: Flow<Boolean>` — Video autoplay in feed
- `L210`: `val amoledMode: Flow<Boolean>` — AMOLED black background mode
- `L215`: `val onboardingCompleted: Flow<Boolean>` — Onboarding completion
- `L220`: `data class InterceptorSettings` — Combined interceptor settings (apiKey, debug, backend)
- `L228-230`: `_interceptorSettings`, `interceptorSettings: StateFlow<InterceptorSettings>` — Combined settings
- `L232-248`: `init block` — Combines flows into interceptorSettings, updates `CivitaiUrlBuilder`
- `L250`: `suspend fun getCurrentSettings()` — Snapshot of all settings for backup
- `L284`: `suspend fun importSettings(settings)` — Bulk update settings from backup
- `L332`: `suspend fun updateFilters(nsfw, sort, period, type, tagIds)` — Updates feed filter params
- `L355`: `suspend fun updateScrollPosition(type, index, offset)` — Debounced scroll position save
- `L378`: `suspend fun updateNextCursor(type, cursor)` — Updates pagination cursor
- `L390`: `suspend fun updatePageLimit(limit)` — Updates page limit
- `L396`: `suspend fun updateGridColumns(columns)` — Updates grid column count
- `L404`: `suspend fun updateDownloadPath(path)` — Updates download directory
- `L413`: `suspend fun updateFavoritesPath(path)` — Updates favorites directory
- `L422`: `suspend fun updateApiKey(key)` — Updates Civitai API key
- `L428`: `suspend fun updateSearchApiKey(token)` — Updates search API bearer token
- `L440`: `suspend fun updateBackendEnabled(enabled)` — Toggles backend enabled
- `L448`: `suspend fun updateBackendUrl(url)` — Updates backend URL
- `L454`: `suspend fun updateBackendApiKey(key)` — Updates backend API key
- `L460`: `suspend fun updateDebugEnabled(enabled)` — Toggles debug logging
- `L468`: `suspend fun updateFavoritesType(type)` — Updates favorites media type filter
- `L474`: `suspend fun updateGalleryType(type)` — Updates gallery media type filter
- `L480`: `suspend fun updateLastRoute(route)` — Tracks last visited navigation route
- `L486`: `suspend fun updateHidePlayerControls(enabled)` — Toggles player controls visibility
- `L494`: `suspend fun updateAlwaysEnableHD(enabled)` — Toggles HD media preference
- `L502`: `suspend fun updateAlwaysMuteVideo(enabled)` — Toggles video mute preference
- `L510`: `suspend fun updateFeedVideoAutoplay(enabled)` — Toggles video autoplay in feed
- `L518`: `suspend fun updateAmoledMode(enabled)` — Toggles AMOLED black background mode
- `L524`: `suspend fun updateOnboardingCompleted(completed)` — Marks onboarding as completed
- `L532`: `val showNsfwFavorites: Flow<Boolean>` — Show NSFW favorites setting
- `L537`: `suspend fun setShowNsfwFavorites(show)` — Sets show NSFW favorites
- `L545`: `suspend fun updateSearchQuery(query)` — Updates search query
- `L557`: `suspend fun updateSearchFilters(type, sort)` — Updates search type and sort
- `L566`: `suspend fun updateSearchScrollPosition(index, offset)` — Updates search scroll position
- `L575`: `fun searchScrollIndex()` — Flow of search scroll index
- `L580`: `fun searchScrollOffset()` — Flow of search scroll offset
- `L585`: `val searchHistoryCount: Flow<Int>` — Search history count
- `L590`: `val searchOffset: Flow<Int>` — Search offset
- `L595`: `suspend fun incrementSearchHistoryCount()` — Increments search history counter
- `L605`: `suspend fun updateSearchOffset(offset)` — Updates search offset
- `L613`: `suspend fun resetSearch()` — Clears search query/offset, resets type/sort defaults

---

### DI

#### `AppModule.kt`

- `L29`: `object AppModule` — Hilt module for dependency injection
- `L32`: `fun provideMoshi()` — Moshi JSON adapter with Kotlin support
- `L38`: `fun provideOkHttpClient(interceptor)` — OkHttp with dispatcher, timeouts, interceptors
- `L70`: `fun provideRetrofit(okHttpClient, moshi)` — Retrofit with Civitai base URL
- `L80`: `fun provideCivitaiApi(retrofit)` — Creates `CivitaiApi` service
- `L103`: `fun provideImageLoader(context, okHttpClient)` — Coil image loader with OkHttp + video frame decoder
- `L130`: `fun provideDatabase(context)` — Room database singleton
- `L135`: `fun provideFavoriteImageDao(database)` — `FavoriteImageDao` provider
- `L140`: `fun provideFeedCacheDao(database)` — `FeedCacheDao` provider
- `L145`: `fun provideBookmarkDao(database)` — `BookmarkDao` provider
- `L151`: `fun provideBookmarkRepository(bookmarkDao)` — `BookmarkRepository` provider
- `L159`: `fun provideUserPreferencesRepository(context)` — DataStore repo provider
- `L167`: `fun provideFavoritesRepository(...)` — Favorites repo with all dependencies

---

### Models

#### `CivitaiImage.kt`

- `L8`: `data class CivitaiImage(id, url, width, height, nsfw, type, meta)` — Core media model
- `L18`: `data class VideoMeta(size: Long? = null)` — Video metadata
- `L21`: `data class CivitaiApiResponse(items, metadata)` — API response wrapper
- `L23`: `data class Metadata(nextCursor)` — Pagination metadata

#### `SearchModels.kt`

- `L8`: `data class CivitaiSearchResult(id, url, width, height, nsfwLevel, type)` — Search result from multi-search API
- `L18`: `data class SearchQuery(q, indexUid, facets, limit, offset, filter, sort)` — Search request query
- `L31`: `data class SearchRequest(queries)` — Search request wrapper
- `L34`: `data class SearchResponseItem(indexUid, hits, query, processingTimeMs, limit, offset, estimatedTotalHits)` — Single search response item
- `L44`: `data class SearchResponse(results)` — Search response wrapper

#### `FavoriteImage.kt`

- `L9`: `data class FavoriteImage(id, url, width, height, nsfw, type, timestamp, isSynced)` — Room entity
- `L19`: `fun toCivitaiImage()` — Converts entity to `CivitaiImage`
- `L32`: `companion object fun fromCivitaiImage(image, isSynced)` — Factory method

#### `FeedItemCache.kt`

- `L9`: `data class FeedItemCache(id, url, width, height, nsfw, type, feedType, orderIndex)` — Room entity
- `L19`: `fun toCivitaiImage()` — Converts entity to `CivitaiImage`
- `L32`: `companion object fun fromCivitaiImage(image, feedType, index)` — Factory method

#### `Bookmark.kt`

- `L9`: `data class Bookmark(id, title, type, sort, period, nsfw, cursor, tags, query, offset, timestamp)` — Room entity
- Supports **feed bookmarks** (with `cursor`) and **search bookmarks** (with `query` + `offset`)

#### `AppBackup.kt`

- `L6`: `data class AppSettingsBackup(...)` — All settings including `searchApiKey`
- `L37`: `data class FavoriteImageBackup(id, url, nsfw, type, timestamp)` — Favorite backup
- `L46`: `data class FeedItemBackup(id, url, width, height, nsfw, type, feedType, orderIndex)` — Feed item backup
- `L58`: `data class BookmarkBackup(id, title, type, sort, period, nsfw, cursor, tags, query, offset, timestamp)` — Bookmark backup (supports search)
- `L73`: `data class AppBackup(version, settings, favorites, bookmarks, feedItems)` — Root backup model

---

### UI Screens

#### `MainScreen.kt`

- `L43`: `fun UiMessageEffect(uiMessage)` — Collects and displays UI messages as Toast
- `L55`: `fun InteractiveTopBar(...)` — Dynamic TopBar for selection/duplicate/normal modes
- `L101`: `fun MainScreen(imageLoader)` — Root composable: dual drawer layout, navigation, full-screen overlay
- `L658`: `fun AppNavigation(navController, startDestination, ...)` — NavHost with feed/search/favorites/gallery/bookmark destinations

#### `FeedScreen.kt`

- `L21`: `fun FeedScreen(...)` — Feed screen with ViewModel, search/jump dialogs, ImageGrid

#### `SearchScreen.kt`

- `L42`: `fun SearchScreen(...)` — Search screen with ViewModel, jump/bookmark dialogs, ImageGrid
- `L204`: `fun SearchTopBar(...)` — Search top bar with query input, grid controls, filter/settings triggers
- `L327`: `fun SearchResultsGrid(...)` — Delegates to ImageGrid with search result conversion
- `L380`: `fun CivitaiSearchResult.toCivitaiImage()` — Extension: converts search result to `CivitaiImage`

#### `FavoritesScreen.kt`

- `L22`: `fun FavoritesScreen(...)` — Favorites screen with batch operations, ImageGrid

#### `GalleryScreen.kt`

- `L23`: `fun GalleryScreen(...)` — Gallery screen with batch delete, ImageGrid

#### `BookmarkScreen.kt`

- `L38`: `fun BookmarkScreen(...)` — Bookmark screen with search filter, edit/delete dialogs, tag selection
- `L307`: `fun BookmarkCard(bookmark, onLoad, onEdit, onDelete)` — Bookmark card with metadata + tags
- `L472`: `private fun buildMetadataString(bookmark)` — Builds metadata string for display

#### `OnboardingScreen.kt`

- `L30`: `data class OnboardingPage` — Page with title, description, icon, gradient colors
- `L38`: `fun OnboardingScreen(onSkip, onFinish, modifier)` — 3-page onboarding with pager + dot indicators
- `L137`: `fun OnboardingPageContent(page)` — Page content with logo, title, description

---

### UI Components

#### `AppBackHandler.kt`

- `L11`: `fun AppBackHandler(...)` — System back: selection mode → close left drawer → close right drawer → double-tap exit

#### `AppFab.kt`

- `L23`: `fun AppNavigationFab(gridState)` — FABs for scroll-to-top, scroll-to-bottom, jump-to-cursor
- `L71`: `fun AppActionFab(...)` — Individual action FAB with label and icon

#### `AppScaffold.kt`

- `L38`: `fun AppScaffold(...)` — Scaffold: pull-to-refresh, bottom progress bar, animated bars, FAB
- `L430`: `fun EmptyState(viewMode)` — Empty grid placeholder with floating icon + gradient text

#### `BaseSidebar.kt`

- `L15`: `fun BaseSidebar(title, onDismiss, footer, amoledMode, content)` — Sidebar base layout with RectangleShape

#### `BookmarkDialog.kt`

- `L16`: `fun BookmarkDialog(onApply, onDismiss)` — Dialog for creating a bookmark with title input

#### `BookmarkEditDialog.kt`

- `L21`: `fun BookmarkEditDialog(bookmark, onConfirm, onDismiss, showTagSelection)` — Edit bookmark: title, cursor/query, tags, sort, period, NSFW. Uses `ModernDialog`.

#### `ConfirmationDialog.kt`

- `L15`: `fun ConfirmationDialog(title, message, onConfirm, onDismiss)` — Cancel/confirm dialog with haptic feedback

#### `DisplaySidebar.kt`

- `L19`: `fun DisplaySidebar(currentRoute, pageLimit, gridColumns, type, onDismiss, ...)` — Grid columns, page limit, media type filter, duplicate scan. Adapts per screen.

#### `FilterSidebar.kt`

- `L17`: `data class CivitaiTag(id, name)` — Tag data class
- `L21`: `fun FilterSidebar(nsfw, sort, period, type, tagIds, onDismiss, onFilterChange, onResetFilters)` — NSFW, sort, period, type, tags

#### `SearchFilterSidebar.kt`

- `L18`: `fun SearchFilterSidebar(type, sort, onDismiss, onFilterChange, onResetFilters, amoledMode)` — Content type + sort chips for search

#### `FullScreenImage.kt`

- `L42`: `fun FullScreenImage(...)` — Fullscreen pager: swipe dismiss, zoom, video controls, favorite, download
- `L787`: `fun AnimatedIconButton(onClick, ...)` — Scale-animated icon button

#### `ImageCard.kt`

- `L50`: `fun ImageCard(...)` — Grid card: local/remote image, cache status, long-press, favorite animation, video autoplay
- `L682`: `fun HeartParticles(modifier, onFinished)` — Heart particle animation on favorite

#### `ImageGrid.kt`

- `L26`: `fun ImageGrid(...)` — Staggered grid: visible item tracking for autoplay, pinch-to-zoom column adjustment, shimmer placeholders

#### `MainBottomBar.kt`

- `L31`: `fun MainBottomBar(currentRoute, onNavigate, feedCount, favoritesCount, searchCount, galleryCount, bookmarkCount)` — Nav between feed/favorites/gallery/search/bookmarks
- `L99`: `fun BottomNavItem(selected, onClick, icon, selectedIcon, label, count, selectedColor)` — Nav item with scale animation
- `L203`: `fun formatCount(count)` — Locale-aware number formatting

#### `MainTopBar.kt`

- `L22`: `fun SelectionTopBar(selectedCount, onClose, onSelectAll, onAction, actionIcon, title)` — TopBar for batch selection/duplicate modes
- `L81`: `fun MainTopBar(gridColumns, onShowDisplayOptions, onUpdateGridColumns, onShowFilters, onShowSettings)` — Logo, display/filter/settings triggers

#### `ModernDialog.kt`

- `L26`: `fun ModernDialog(title, message, icon, onConfirm, confirmText, confirmIcon, onDismiss, dismissText, dismissIcon, showConfirm, showDismiss, confirmEnabled, confirmColor, dismissColor, customContent, modifier)` — Modern AlertDialog: animated scale on press, dual buttons, icon, custom content area, rounded corners.

#### `ModernEmptyState.kt`

- `L24`: `enum class EmptyStateType` — Types: `FEED`, `FAVORITES`, `GALLERY`, `SEARCH`, `DUPLICATES`, `BOOKMARKS`
- `L62`: `fun ModernEmptyState(type, onAction, modifier)` — Animated empty state: floating icon, glow effect, entry animation, optional action button.

#### `SettingsSidebar.kt`

- `L18`: `fun SettingsSidebar(...)` — API key, search API key, paths, cache, import/export, backend, player settings, appearance

#### `Shimmer.kt`

- `L13`: `fun Modifier.shimmerBackground()` — Animated loading shimmer with primary/secondary gradients

#### `SidebarSection.kt`

- `L17`: `fun SidebarSection(title, modifier, icon, initiallyExpanded, content)` — Expandable section with icon

#### `SkeletonGrid.kt`

- `L13`: `fun SkeletonGrid(columnCount, modifier)` — Placeholder grid with varying aspect ratios

#### `SpeedDialFab.kt`

- `L18`: `data class SpeedDialItem(icon, label, onClick, containerColor, contentColor)` — Speed dial menu item
- `L27`: `fun SpeedDialFab(items, modifier, mainIcon)` — Expandable FAB with staggered animations

#### `TagSelectionDialog.kt`

- `L18`: `data class TagOption(id, name)` — Tag option for multi-select
- `L21`: `fun TagSelectionDialog(allTags, initialTags, onConfirm, onDismiss)` — Multi-select tag picker with FilterChips inside `ModernDialog`

#### `VideoPlayer.kt`

- `L34`: `fun VideoPlayer(...)` — ExoPlayer wrapper: zoom, pan, play/pause, speed, scale mode, seek
- `L261`: `object : Player.Listener` — ExoPlayer listener for lifecycle events

#### `VideoPlayerManager.kt`

- `L15`: `class VideoPlayerManager(context)` — Pooled ExoPlayer management with thread-safe `activeCount`
- `L27`: `fun updateLimit(newLimit)` — Updates max pool size, shrinks if needed
- `L43`: `fun acquirePlayer(dataSourceFactory)` — Gets available player from pool
- `L70`: `fun releasePlayer(player)` — Returns player to pool
- `L121`: `fun releaseAll()` — Releases all players, clears pool

#### `VideoPlayerModels.kt`

- `L5`: `enum class ScaleMode` — NORMAL, CROP, FULL
- `L11`: `data class BackendConfig(url, apiKey)` — Backend configuration
- `L13`: `val LocalBackendConfig` — CompositionLocal for `BackendConfig`

#### `ZoomableImage.kt`

- `L24`: `fun ZoomableImage(model, imageLoader, modifier, thumbnailModel, onZoomChange, onTap)` — Pinch-to-zoom + double-tap

#### `SplashScreen.kt`

- `L25`: `fun SplashScreen(onSplashFinished, modifier)` — Animated logo + loading indicator

---

### Utilities

#### `CivitaiUrlBuilder.kt`

- `L5`: `object CivitaiUrlBuilder` — URL compression, expansion, variant building
- `L16`: `var backendEnabled: Boolean` — Runtime backend enabled flag
- `L17`: `var backendUrl: String` — Runtime backend URL
- `L18`: `var backendApiKey: String` — Runtime backend API key
- `L19`: `const val BACKEND_DOWNLOAD_TIMEOUT_SECONDS` — Backend download timeout (60s)
- `L21`: `fun compressUrl(url)` — Shortens Civitai URLs for DB storage
- `L27`: `fun isCivitaiMediaUrl(url)` — Checks if URL is from Civitai CDN
- `L34`: `fun expandUrl(compressed, type)` — Restores full URLs from compressed format
- `L47`: `fun extractCivitaiUuid(url)` — Extracts UUID from URL (with caching)
- `L70`: `private fun wrapBackendUrl(type, quality, url, fallback)` — Wraps URL with backend if enabled
- `L96`: `fun toBackendUrl(type, quality, uuid)` — Builds backend media URL
- `L102`: `fun buildBackendFeedUrl(originalUrl)` — Builds backend feed URL
- `L111`: `fun buildBackendSearchUrl(originalUrl)` — Builds backend search URL
- `L117`: `fun getThumbnailUrl(url, width)` — Thumbnail variant URL
- `L123`: `fun getVideoThumbnailUrl(url)` — Video thumbnail variant URL
- `L133`: `fun getVideoPreviewUrl(url)` — Video preview variant URL
- `L143`: `fun getVideoOriginalUrl(url)` — Video original file URL
- `L153`: `fun getImageOriginalUrl(url)` — Original image file URL
- `L163`: `fun getFallbackChain(url)` — List of fallback URLs for thumbnails
- `L190`: `fun getBaseUrl(url)` — Extracts base URL from variant
- `L195`: `fun modifyUrl(url, variant)` — Applies variant to URL
- `L205`: `private fun parseCivitaiUrl(url)` — Parses Civitai URL into components

#### `FileUtils.kt`

- `L11`: `object FileUtils` — File magic byte detection and utility
- `L12`: `val IMAGE_EXTENSIONS` — jpeg, jpg, webp
- `L13`: `val VIDEO_EXTENSIONS` — mp4, webm
- `L17`: `fun saveBitmapAsWebP(bitmap, outputFile, quality)` — Saves bitmap as WebP
- `L31`: `fun convertFileToWebP(inputFile, outputFile, quality)` — Converts file to WebP via bitmap
- `L55`: `fun detectExtension(contentType, source, url)` — Detects extension from magic bytes, content-type, or URL
- `L105`: `fun getExtensionFromBytes(bytes)` — Magic byte to extension mapping
- `L120`: `fun isVideoFile(bytes)` — Checks if bytes represent a video
- `L128`: `fun isImageFile(bytes)` — Checks if bytes represent an image
- `L136`: `fun isRealMedia(file)` — Validates media file signatures
- `L178`: `fun calculateHash(file)` — SHA-256 hash for duplicate detection
- `L210`: `suspend fun findDuplicateGroups(files)` — Groups files by content hash

#### `Logger.kt`

- `L5`: `object Logger` — Conditional debug logging
- `L6`: `var debugEnabled: Boolean` — Global debug flag (mirrors `UserPreferencesRepository`)
- `L8`: `fun d(tag, message)` — Debug log (conditional)
- `L12`: `fun e(tag, message, throwable)` — Error log (always)
- `L16`: `fun w(tag, message)` — Warning log (conditional)
- `L20`: `fun v(tag, message)` — Verbose log (conditional)
- `L24`: `fun i(tag, message)` — Info log (conditional)

#### `UriUtils.kt`

- `L8`: `fun resolveUriToPath(context, uri)` — Converts DocumentFile tree URI to filesystem path

#### `MediaProcessor.kt`

- `L9`: `class MediaProcessor` — Video frame extraction + image conversion
- `L10`: `fun extractVideoFrame(videoFile, outputFile, quality)` — Extracts single frame as WebP
- `L55`: `fun convertToWebP(inputFile, outputFile, quality)` — Converts image to WebP

#### `DuplicateDetector.kt`

- `L10`: `object DuplicateDetector` — Duplicate detection for gallery files
- `L11`: `suspend fun findDuplicateGroups(files, context)` — Finds duplicates by size + hash
- `L48`: `suspend fun removeDuplicateGroups(groups, context, onRefreshDownloadedIds)` — Removes duplicates, keeps newest
- `L75`: `private fun extractId(file)` — Extracts image ID from filename

#### `OkHttpExtensions.kt`

- `L8`: `fun Interceptor.Chain.withReadTimeout(timeout, unit)` — Returns chain wrapper with custom read timeout
- `L12`: `fun Interceptor.Chain.withConnectTimeout(timeout, unit)` — Returns chain wrapper with custom connect timeout
- `L16`: `fun Interceptor.Chain.withWriteTimeout(timeout, unit)` — Returns chain wrapper with custom write timeout
- `L20`: `private class TimeoutChainWrapper` — Wrapper implementing `Interceptor.Chain` with custom timeout overrides

#### `Utils.kt`

- `L28`: `fun Modifier.scrollbar(state, width, color)` — Custom scrollbar for scrollable layouts
- `L57`: `fun Modifier.gridScrollbar(state, width, color, endOffset)` — Optimized scrollbar for LazyGrids
- `L103`: `private fun getEffectiveFavoritesDir(context, favoritesDir)` — Resolves effective favorites directory
- `L107`: `suspend fun hasLocalCache(context, imageId, isVideo, favoritesDir)` — Checks thumbnail cache
- `L143`: `suspend fun hasFullCache(context, imageId, isVideo, favoritesDir)` — Checks full-res media cache
- `L176`: `suspend fun resolveImageData(context, image, favoriteInfo, thumbnailWidth, useVideoPath, viewMode, favoritesDir)` — Resolves local file or remote URL
- `L259`: `fun getThumbnailUrl(url, width)` — Appends width param (delegates to `CivitaiUrlBuilder`)
- `L263`: `fun getVideoThumbnailUrl(url)` — Video thumbnail URL builder
- `L267`: `fun getVideoPreviewUrl(url)` — Video preview URL builder
- `L271`: `fun getVideoOriginalUrl(url)` — Video original file URL builder
- `L275`: `fun getOriginalUrl(url)` — Appends original flag to image URL
- `L279`: `fun modifyCivitaiUrl(url, variant)` — Generic URL variant modifier
- `L287`: `fun formatDuration(ms)` — Formats ms to HH:MM:SS:CS
- `L310`: `fun playerPoolSizeForColumns(columns)` — Maps columns to optimal player pool size

#### `Constants.kt`

- `L3`: `object Constants` — App-wide constants
- `L4`: `AUTOPLAY_DEBOUNCE_DELAY_MS = 150L` — Autoplay debounce delay
- `L5`: `BACKGROUND_GLOW_DURATION_MS = 8000` — Background glow animation duration
- `L6`: `BOOKMARK_LOAD_DELAY_MS = 500L` — Bookmark load delay
- `L7`: `CACHE_BUFFER_SIZE = 8192` — Cache buffer size
- `L8`: `CLEANUP_INTERVAL_MS = 60000L` — Cleanup interval
- `L9`: `DEBOUNCE_DELAY_MS = 150L` — General debounce delay
- `L10`: `MAGIC_BYTE_BUFFER_SIZE = 64` — Magic byte buffer size
- `L11`: `MIN_THUMBNAIL_SIZE = 100L` — Minimum thumbnail size
- `L12`: `RESTORE_SCROLL_DELAY_MS = 500L` — Scroll restore delay
- `L13`: `STALE_CHECK_INTERVAL_MS = 30000L` — Stale check interval
- `L14`: `SWIPE_EDGE_THRESHOLD_DP = 40f` — Swipe edge threshold
- `L15`: `TEMP_FILE_MAX_AGE_MS = 3600000L` — Temp file max age
- `L16-19`: `VIDEO_POOL_SIZE_*` — Video player pool sizes per column count

---

### ViewModels

#### `BaseViewModel.kt`

- `L17`: `abstract class BaseViewModel(repository, favoritesRepository, galleryRepository)` — Base ViewModel with shared actions
- `L22`: `companion object isImporting: Boolean` — Static flag to prevent scroll reset during import
- `L28`: `setActiveRoute(route)` → Sets the active navigation route
- `L39`: `updateGridColumns(columns)` — Persists column preference
- `L43`: `updatePageLimit(limit)` — Persists page limit
- `L47`: `saveScrollPosition(type, index, offset)` — Debounced scroll position save
- `L51`: `sendMessage(resId)` — Emits toast message
- `L55`: `toggleFavorite(image)` — Delegates to `FavoritesRepository`
- `L59`: `retryThumbnail(url, onComplete)` — Retries thumbnail fetch with delay
- `L69`: `ensureFavoriteResources(image, force, onProgress)` — Downloads thumbnail/preview
- `L77`: `ensureFavoriteResourcesThrottled(image, force, onProgress)` — Throttled resource sync
- `L87`: `downloadImage(image)` — Abstract; implemented by each child
- `L89`: `performDownload(image, onUpdateProgress, onSuccess)` — Download with progress tracking

#### `BaseUiState.kt`

- `L5`: `interface BaseUiState` — Common properties: `images`, `isLoading`, `isRefreshing`, `gridColumns`, `scrollIndex`, `scrollOffset`, `downloadProgresses`

#### `FeedUiState.kt`

- `L5`: `data class FeedUiState` — Images, cursors, filters (nsfw/sort/period/type/tags), pageLimit, gridColumns, scroll, favoriteIds, favoritesPath, nextCursor, isRestored

#### `SearchUiState.kt`

- `L5`: `data class SearchUiState` — query, results, isLoading, hasMore, totalHits, type, sort, gridColumns, error, isRestored, scrollIndex, scrollOffset, currentOffset, currentPageStartOffset

#### `FavoritesUiState.kt`

- `L5`: `data class FavoritesUiState` — Images, selection, duplicates, favoriteIds, favoritesPath, type, isRestored

#### `GalleryUiState.kt`

- `L5`: `data class GalleryUiState` — Images, selection, duplicates, downloadedIds, downloadPath, type, isRestored

#### `SettingsUiState.kt`

- `L3`: `data class SettingsUiState` — apiKey, searchApiKey, downloadPath, favoritesPath, effectiveFavoritesPath, debugEnabled, backend settings, player settings, amoledMode, cacheSize, lastRoute

#### `FeedViewModel.kt`

- `L21`: `class FeedViewModel` — Main feed with pagination and filters
- `L30-31`: `_uiState`, `uiState: StateFlow<FeedUiState>` — UI state flow
- `L32-37`: `imageFeed`, `videoFeed`, `imageCursor`, `videoCursor`, `currentImageCursor`, `currentVideoCursor` — Cached feeds and cursors
- `L38`: `loadingJob: Job?` — Current loading job
- `L39`: `isFirstSettingsLoad: Boolean` — Initial settings load flag
- `L40`: `isJumping: Boolean` — Cursor jump in progress flag
- `L42-150`: `init block` — Combines repository flows, initializes from cache, loads initial images
- `L160`: `fun refresh()` — Clears cursor and reloads feed
- `L191`: `fun loadMore()` — Loads next page with cursor
- `L201`: `fun jumpToCursor(cursor)` — Jumps to specific cursor
- `L214`: `private fun loadImages(isNew, cursorOverride)` — Fetches and caches feed items
- `L281`: `fun updateFilters(nsfw, sort, period, type, tagIds)` — Updates filter params, invalidates restoration
- `L285`: `fun resetFilters()` — Resets all filters to defaults
- `L291`: `override fun downloadImage(image)` — Triggers download via `performDownload`
- `L308`: `fun saveBookmark(title)` — Saves current feed state as bookmark
- `L337`: `fun loadBookmark(bookmark)` — Loads bookmark settings and jumps to cursor
- `L360`: `fun markRestored()` — Sets isRestored flag

#### `SearchViewModel.kt`

- `L31`: `class SearchViewModel` — Search with pagination and filter management
- `L38-39`: `_uiState`, `uiState: StateFlow<SearchUiState>` — UI state flow
- `L40`: `_uiMessage`, `uiMessage: SharedFlow<Int>` — UI message flow
- `L43`: `searchResultCount: StateFlow<Int>` — Eagerly shared count of search results
- `L46-50`: `searchJob`, `currentOffset`, `currentPageStartOffset`, `pageSize`, `isJumping` — Search state
- `L52-164`: `init block` — Combines preference flows, initializes state, restores search from DataStore
- `L166`: `private fun restoreSearch(query, offset)` — Restores search from saved state
- `L236`: `fun search(query, forceNew, startOffset)` — Executes search with 3-attempt retry
- `L345`: `fun loadMore()` — Loads next page
- `L353`: `fun saveScrollPosition(type, index, offset)` — Saves search scroll position
- `L357`: `fun updateSearchFilters(type, sort)` — Updates filters and re-searches
- `L370`: `fun updateGridColumns(columns)` — Persists grid column preference
- `L374`: `fun clearSearch()` — Resets all search state
- `L384`: `fun markRestored()` — Sets isRestored flag
- `L388`: `private fun sendMessage(resId)` — Emits toast message
- `L392`: `fun saveSearchBookmark(title)` — Saves search config as bookmark
- `L414`: `fun loadSearchBookmark(bookmark)` — Applies bookmark query and offset
- `L426`: `fun jumpToOffset(targetOffset)` — Jumps to specific offset with retry

#### `FavoritesViewModel.kt`

- `L17`: `class FavoritesViewModel` — Manages favorites list
- `L24-26`: `_uiState`, `uiState: StateFlow<FavoritesUiState>` — UI state flow
- `L28-80`: `init block` — Combines favorites flow, applies type filter
- `L82`: `fun updateType(type)` — Updates media type filter
- `L90`: `fun toggleSelection(id)` / `clearSelection()` / `selectAll()` — Selection management
- `L122`: `fun batchUnfavorite()` — Removes selected favorites
- `L153`: `fun getFavoriteFlow(id)` — Returns Flow of favorite by ID
- `L155`: `fun forceRedownload(image)` — Re-downloads favorite resource
- `L174`: `override fun downloadImage(image)` — Initiates download with progress
- `L191`: `fun findDuplicates()` / `clearDuplicatesMode()` / `removeDuplicates()` — Duplicate detection
- `L275`: `fun markRestored()` — Sets isRestored flag

#### `GalleryViewModel.kt`

- `L16`: `class GalleryViewModel` — Manages local gallery browsing
- `L23-24`: `_uiState`, `uiState: StateFlow<GalleryUiState>` — UI state flow
- `L26-53`: `init block` — Combines gallery flow, applies type filter, monitors downloaded IDs
- `L63`: `fun refresh()` / `performRefresh()` — Rescans directory
- `L84`: `fun updateType(type)` — Updates media type filter
- `L88`: `fun toggleSelection(id)` / `clearSelection()` / `selectAll()` — Selection management
- `L102`: `fun batchDelete()` — Deletes selected local files
- `L126`: `fun deleteLocalFile(image)`
- `L134`: `override fun downloadImage(image)` — Initiates download with progress
- `L152`: `fun findDuplicates()` / `clearDuplicatesMode()` / `removeDuplicates()` — Duplicate detection
- `L227`: `fun markRestored()` — Sets isRestored flag

#### `SettingsViewModel.kt`

- `L19`: `class SettingsViewModel` — Manages app settings and import/export
- `L29-30`: `_uiState`, `uiState: StateFlow<SettingsUiState>` — UI state flow
- `L31-32`: `_exitEvent`, `exitEvent: SharedFlow<Unit>` — App exit trigger after import
- `L34-126`: `init block` — Combines repository flows, loads settings, monitors player settings
- `L128`: `override fun downloadImage(image)` — No-op (gallery handles downloads)
- `L130`: `fun updateLastRoute(route)` — Tracks last visited route
- `L134`: `fun updateApiKey(key)` — Updates Civitai API key
- `L141`: `fun updateSearchApiKey(token)` — Updates search API bearer token
- `L148`: `fun updateDownloadPath(path)` — Updates download directory
- `L152`: `fun updateFavoritesPath(path)` — Updates favorites directory
- `L156`: `fun updateDebugEnabled(enabled)` — Toggles debug logging
- `L160`: `fun updateBackendEnabled(enabled)` — Toggles backend enabled
- `L164`: `fun updateBackendUrl(url)` — Updates backend URL
- `L171`: `fun updateBackendApiKey(key)` — Updates backend API key
- `L178`: `fun updateHidePlayerControls(enabled)` — Toggles video player controls
- `L182`: `fun updateAlwaysEnableHD(enabled)` — Toggles HD media preference
- `L186`: `fun updateAlwaysMuteVideo(enabled)` — Toggles video mute
- `L190`: `fun updateFeedVideoAutoplay(enabled)` — Toggles video autoplay in feed
- `L194`: `fun updateAmoledMode(enabled)` — Toggles AMOLED black background
- `L198`: `fun clearImageCache()` — Clears Coil image cache
- `L207`: `fun updateCacheSize()` — Computes and updates cache size
- `L213`: `private fun formatSize(bytes)` — Formats bytes to human-readable string
- `L225`: `fun exportData(uri)` — Exports backup to URI
- `L234`: `fun importData(uri)` — Imports backup from URI (triggers app exit on success)

#### `BookmarkViewModel.kt` (BookmarkUiState defined inline at L14)

- `L14`: `data class BookmarkUiState(bookmarks, isLoading, bookmarkCount)` — UI state for bookmarks
- `L21`: `class BookmarkViewModel` — Manages bookmarks
- `L24-25`: `_uiState`, `uiState: StateFlow<BookmarkUiState>` — UI state flow
- `L30-42`: `init block` — Collects bookmarks from repository
- `L44`: `fun deleteBookmark(bookmark)` — Deletes a bookmark
- `L51`: `fun updateBookmarkTitle(bookmark, newTitle)` — Updates bookmark title and metadata

---

### Theme

#### `Theme.kt`

- `L148`: `fun DibellaTheme(darkTheme, dynamicColor, content)` — Material 3 theme with dynamic colors and dark fallback

---

### Unit Tests

- `api/CivitaiBackendRetryInterceptorTest.kt` — Tests backend retry interceptor behavior
- `api/CivitaiThumbnailInterceptorTest.kt` — Tests thumbnail interceptor retry and recovery
- `model/AppBackupTest.kt` — Tests AppBackup serialization/deserialization
- `util/CivitaiUrlBuilderTest.kt` — Tests URL compression, expansion, and variant building
- `util/FileUtilsTest.kt` — Tests magic byte detection and file utilities

---

## 4. Navigation & Screen Flow

```
MainActivity
├── SplashScreen (animated logo + loader)
├── OnboardingScreen (3-page pager, skippable)
└── MainScreen
    ├── BottomBar: Feed | Favorites | Gallery | Search | Bookmarks
    ├── Left Drawer (DisplaySidebar): adapts per screen
    ├── Right Drawer:
    │   ├── FILTERS → FilterSidebar (feed)
    │   ├── SEARCH_FILTERS → SearchFilterSidebar (search)
    │   └── SETTINGS → SettingsSidebar (all screens)
    └── FullScreenImage overlay (swipe pager, zoom, video)
```

---

## 5. Technical Stack

| Layer           | Technology                       |
| --------------- | -------------------------------- |
| **UI**          | Jetpack Compose (Material 3)     |
| **Pattern**     | MVVM + Unidirectional Data Flow  |
| **DI**          | Hilt                             |
| **Network**     | Retrofit / OkHttp / Moshi        |
| **Database**    | Room (3 entities)                |
| **Persistence** | Jetpack DataStore (Preferences)  |
| **Images**      | Coil3 (with Video Frame support) |
| **Video**       | Media3 / ExoPlayer (pooled)      |
| **Concurrency** | Kotlin Coroutines & Flow         |
| **Min SDK**     | Android 11 (API 30)              |
| **Target SDK**  | Android 16 (API 36)              |

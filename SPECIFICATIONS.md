# Technical Specifications: Dibella

This document provides a detailed technical overview of the Dibella Android application. It is intended for software engineers and LLMs to understand the system architecture, file structure, state management patterns, and all functions implemented across the codebase.

## 1. Architectural Patterns

### MVVM with Centralized Logic

The application uses the Model-View-ViewModel (MVVM) pattern. A `BaseViewModel` provides shared implementations to reduce boilerplate:

- **Persistent Preferences**: Interface with `UserPreferencesRepository` for grid, request, and scroll settings.
- **Scroll Management**: `saveScrollPosition(type, index, offset)` writes positions to DataStore.
- **Message Bus**: `sendMessage(resId)` emits a toast resource ID via `SharedFlow<Int>`.
- **Shared Actions**: `toggleFavorite`, `ensureFavoriteResources`, `ensureFavoriteResourcesThrottled`, `performDownload`.
- **Concurrency**: Uses `Dispatchers.IO.limitedParallelism(16)` for heavy sync operations.

### Dependency Injection

Hilt is used for DI. All repositories and network services are provided as Singletons in `AppModule`. ViewModels are injected with their required repositories.

## 2. State Management and Data Flow

### Unidirectional Data Flow (UDF)

Each screen is driven by a single `UiState` data class emitted as a `StateFlow`.

- **BaseUiState**: Interface defining `images`, `isLoading`, `isRefreshing`, `gridColumns`, `scrollIndex`, `scrollOffset`, `downloadProgresses`.
- **User Actions**: UI calls functions on the ViewModel.
- **State Updates**: ViewModel updates the private `MutableStateFlow` via `.update { }`.
- **Observation**: The UI observes via `collectAsState()`.

### Scroll Restoration Logic

Each screen manages scroll position across refreshes:

1. Scroll positions (index + offset) are debounced (500ms) and saved to DataStore.
2. On content load, if `isRestored == false` and images are not empty, the grid scrolls to the saved position.
3. After a successful restore, `markRestored()` is called to prevent re-scrolling.
4. Feed restoration is reset to `false` when filter parameters change.

## 3. Functions Reference

Below is a concise mapping of each file and its public functions.

### Application Entry

- **DibellaApplication.kt**
    - `newImageLoader(context)` → Returns the Hilt-injected Coil3 ImageLoader as the app singleton.
    - `onTerminate()` → Closes `FavoritesRepository`.

- **MainActivity.kt**
    - `onCreate()` → Requests media permissions, checks onboarding, shows SplashScreen → OnboardingScreen → MainScreen in sequence.
    - `RightSidebarType` enum: `FILTERS`, `SETTINGS`.

### API Layer

- **CivitaiApi.kt**
    - `getImages(limit, nsfw, sort, period, type, tags, withMeta, useIndex, cursor)` → Fetches images from Civitai API.

- **CivitaiInterceptor.kt**
    - `intercept(chain)` → OkHttp interceptor that:
        - Injects User-Agent and Authorization headers.
        - Redirects Civitai media URLs to a custom backend if backend is enabled.
        - Redirects feed API requests to backend feed endpoint if backend is enabled.
        - Adjusts timeouts for backend vs Civitai requests.

- **CivitaiThumbnailInterceptor.kt**
    - `intercept(chain)` → OkHttp interceptor that:
        - Verifies media by magic bytes.
        - On failure, retries with fallback URLs (alt widths, transcode, original).
        - Adds caching headers to valid media responses.

### Data Layer

- **AppDatabase.kt**
    - `getDatabase(context)` → Singleton Room database builder for `dibella_database`.
    - Exposes `favoriteImageDao()`, `feedCacheDao()`, `bookmarkDao()`.

- **UserPreferencesRepository.kt**
    - Flows:
        - `nsfw`, `sort`, `period`, `type`, `tagIds`, `apiKey`, `pageLimit`, `gridColumns`, `downloadPath`, `favoritesPath`, `effectiveFavoritesPath`, `debugEnabled`, `favoritesType`, `galleryType`, `lastRoute`, `hidePlayerControls`, `alwaysEnableHD`, `alwaysMuteVideo`, `feedVideoAutoplay`, `amoledMode`, `onboardingCompleted`, `backendEnabled`, `backendUrl`, `backendApiKey`, `showNsfwFavorites`.
        - `feedScrollIndex(type)`, `feedScrollOffset(type)`, `nextCursor(type)` → type-dependent scroll/cursor flows.
        - `interceptorSettings` → Combined settings for interceptor.
    - `updateFilters(nsfw, sort, period, type, tagIds)`
    - `updateScrollPosition(type, index, offset)`
    - `updateNextCursor(type, cursor)`
    - `updatePageLimit(limit)`
    - `updateGridColumns(columns)`
    - `updateDownloadPath(path)`
    - `updateFavoritesPath(path)`
    - `updateApiKey(key)`
    - `updateBackendEnabled(enabled)`
    - `updateBackendUrl(url)`
    - `updateBackendApiKey(key)`
    - `updateDebugEnabled(enabled)`
    - `updateFavoritesType(type)`
    - `updateGalleryType(type)`
    - `updateLastRoute(route)`
    - `updateHidePlayerControls(enabled)`
    - `updateAlwaysEnableHD(enabled)`
    - `updateAlwaysMuteVideo(enabled)`
    - `updateFeedVideoAutoplay(enabled)`
    - `updateAmoledMode(enabled)`
    - `updateOnboardingCompleted(completed)`
    - `setShowNsfwFavorites(show)`
    - `getCurrentSettings()` → Returns `AppSettingsBackup`.
    - `importSettings(settings)` → Restores settings from backup.

- **FavoritesRepository.kt**
    - `allFavorites` → Flow of all favorite images as `CivitaiImage`.
    - `favoriteIds` → Flow of all favorite IDs.
    - `getFavoriteFlow(id)` → Flow for a single favorite's sync status.
    - `isFavorite(id)` → Flow<Boolean> for favorite check.
    - `toggleFavorite(image)` → Add or remove a favorite.
    - `ensureFavoriteResources(image, force, onProgress)` → Downloads thumbnail and preview for a favorite; sets `isSynced` when done.
    - `repairSync(onProgress)` → Resyncs all unsynced favorites.
    - `clearUnusedResources(favoriteIds)` → Deletes files no longer associated with favorites.
    - `findDuplicateGroups()` → Groups duplicate favorites by content hash.
    - `removeDuplicates(duplicateGroups)` → Removes duplicates, keeps newest.
    - `getAllFavoritesSync()` → Synchronous read of all favorites.
    - `importFavorites(favorites)` → Bulk inserts favorites from backup.
    - `manualFetch(url)` → Single HTTP GET to validate a URL.
    - `updateOkHttpClient(newClient)` → Swaps internal OkHttp client.
    - `close()` → Cancels repository coroutine scope.

- **FeedRepository.kt**
    - `getCachedFeed(type)` → Returns cached feed items for type.
    - `clearCache(type)` → Clears feed cache for type.
    - `fetchImages(type, nsfw, sort, period, tagIds, limit, cursor, isNew, startOrderIndex)` → Fetches images from API with retry and cursor recovery; stores in cache.

- **GalleryRepository.kt**
    - `refreshDownloadedIds()` → Scans download directory and refreshes set of downloaded image IDs.
    - `scanDirectory(path)` → Scans local folder; returns `List<CivitaiImage>` from files.
    - `downloadImage(image, onProgress)` → Downloads image/video to gallery directory with fallback URLs.
    - `deleteLocalFile(image)` → Deletes a local file and refreshes IDs.
    - `findDuplicateGroups()` → Finds duplicate files in gallery.
    - `removeDuplicates(duplicateGroups)` → Removes duplicates.
    - `downloadedIds` → StateFlow of downloaded image IDs.

- **BackupRepository.kt**
    - `exportData(uri)` → Exports settings, favorites, bookmarks as JSON.
    - `importData(uri)` → Imports settings, favorites, bookmarks from JSON.

- **BookmarkRepository.kt**
    - `getAllBookmarks()` → Flow of all bookmarks.
    - `getAllBookmarksSync()` → Sync read of all bookmarks.
    - `addBookmark(bookmark)`
    - `updateBookmark(bookmark)`
    - `deleteBookmark(bookmark)`

- **FavoriteImageDao.kt**
    - `getAllFavorites()` → Flow of all favorites.
    - `getAllFavoritesSync()` → Sync read of all favorites.
    - `insertFavorite(image)`
    - `insertFavorites(images)`
    - `deleteFavorite(image)`
    - `isFavorite(id)` → Flow<Boolean>.
    - `isFavoriteDirect(id)` → Sync Boolean.
    - `getAllFavoriteIds()` → Flow of all IDs.
    - `getFavorite(id)` / `getFavoriteFlow(id)` → By-ID access.

- **FeedCacheDao.kt**
    - `getFeed(feedType)` → Read cached feed.
    - `insertFeed(items)` → Insert into cache.
    - `clearFeed(feedType)` → Clear cache.
    - `replaceFeed(feedType, items)` → Clear then insert.
    - `insertOrUpdateFeed(items)` → Upsert.

- **BookmarkDao.kt**
    - `getAllBookmarks()` / `getAllBookmarksSync()`
    - `insertBookmark(bookmark)`
    - `updateBookmark(bookmark)`
    - `deleteBookmark(bookmark)`
    - `clearAll()`

### ViewModels

- **BaseViewModel.kt**
    - `updateGridColumns(columns)`
    - `updatePageLimit(limit)`
    - `saveScrollPosition(type, index, offset)`
    - `sendMessage(resId)`
    - `toggleFavorite(image)`
    - `retryThumbnail(url, onComplete)`
    - `ensureFavoriteResources(image, force, onProgress)`
    - `ensureFavoriteResourcesThrottled(image, force, onProgress)`
    - `performDownload(image, onUpdateProgress, onSuccess)`
    - `downloadImage(image)` → Abstract; implemented by each child.

- **FeedViewModel.kt**
    - `refresh()` → Clears feed and reloads.
    - `loadMore()` → Loads next page using current cursor.
    - `jumpToCursor(cursor)` → Jumps to a specific cursor.
    - `updateFilters(nsfw, sort, period, type, tagIds)`
    - `resetFilters()`
    - `saveBookmark(title)` → Saves current feed config as bookmark.
    - `loadBookmark(bookmark)` → Applies bookmark settings and jumps to its cursor.
    - `downloadImage(image)` → Triggers download via `performDownload`.
    - `markRestored()`

- **FavoritesViewModel.kt**
    - `updateType(type)` → all/image/video filter.
    - `toggleSelection(id)` / `clearSelection()` / `selectAll()`
    - `batchUnfavorite()` → Removes selected favorites.
    - `getFavoriteFlow(id)` → Exposes favorite sync flow.
    - `forceRedownload(image)` → Force re-download of favorite resources.
    - `findDuplicates()` / `clearDuplicatesMode()` / `removeDuplicates()`
    - `downloadImage(image)`
    - `markRestored()`

- **GalleryViewModel.kt**
    - `refresh()` / `performRefresh()` → Rescans directory.
    - `updateType(type)` → all/image/video filter.
    - `toggleSelection(id)` / `clearSelection()` / `selectAll()`
    - `batchDelete()` → Deletes selected local files.
    - `deleteLocalFile(image)`
    - `findDuplicates()` / `clearDuplicatesMode()` / `removeDuplicates()`
    - `downloadImage(image)`
    - `markRestored()`

- **SettingsViewModel.kt**
    - `updateLastRoute(route)`
    - `updateApiKey(key)`
    - `updateDownloadPath(path)`
    - `updateFavoritesPath(path)`
    - `updateDebugEnabled(enabled)`
    - `updateBackendEnabled(enabled)`
    - `updateBackendUrl(url)`
    - `updateBackendApiKey(key)`
    - `updateHidePlayerControls(enabled)`
    - `updateAlwaysEnableHD(enabled)`
    - `updateAlwaysMuteVideo(enabled)`
    - `updateFeedVideoAutoplay(enabled)`
    - `updateAmoledMode(enabled)`
    - `clearImageCache()`
    - `updateCacheSize()`
    - `exportData(uri)`
    - `importData(uri)`
    - `exitEvent` → SharedFlow to restart app after import.

- **BookmarkViewModel.kt**
    - `deleteBookmark(bookmark)`
    - `updateBookmarkTitle(bookmark, newTitle)`

### UI Components

- **AppScaffold.kt**
    - `AppScaffold(...)` → Wraps content with:
        - TopBar and BottomBar with scroll-based hide/show.
        - Pull-to-refresh and pull-to-load-more via nested scroll.
        - Scroll progress bar along bottom.
        - Integration with `AppFab`.
    - `EmptyState(viewMode)` → Animated empty-state view for feed/favorites/gallery/bookmarks.

- **AppFab.kt**
    - `AppFab(...)` → Floating action buttons for:
        - Bookmark current position.
        - Jump to cursor.
        - Scroll to top / scroll to bottom.

- **ImageGrid.kt**
    - `ImageGrid(...)` → LazyVerticalStaggeredGrid that:
        - Renders `ImageCard` for each image.
        - Tracks visible items for autoplay.
        - Supports pinch-to-zoom grid (adjusts columns).
        - Shows shimmer loading placeholders.

- **ImageCard.kt**
    - `ImageCard(...)` → Single grid item that:
        - Loads thumbnail or video preview via Coil3.
        - Shows cache status indicator (yellow/partial, green/full).
        - Handles long-press for selection or video preview.
        - Triggers favorite/unfavorite with animations.
        - Integrates with `VideoPlayer` for autoplay.
    - `HeartParticles(...)` → Animation on favorite.

- **VideoPlayer.kt**
    - `VideoPlayer(...)` → ExoPlayer wrapper with:
        - Pinch-to-zoom and pan gestures.
        - Double-tap to zoom/reset.
        - Tap/long-press callbacks.
        - Pool-based or dedicated player mode.
    - `ExoVideoPlayer(...)` → Internal composable that manages ExoPlayer lifecycle, audio detection, FPS tracking, and lifecycle-aware play/pause.

- **VideoPlayerManager.kt**
    - `VideoPlayerManager(context)` → Manages a pool of ExoPlayer instances:
        - `updateLimit(newLimit)` → Adjust max pool size based on grid columns.
        - `acquirePlayer(dataSourceFactory)` → Acquires or creates a player.
        - `releasePlayer(player)` → Returns player to pool or releases.
        - `releaseAll()` → Releases all players.
    - `LocalVideoPlayerManager` → CompositionLocal.

- **FullScreenImage.kt**
    - `FullScreenImage(...)` → Fullscreen pager for images and videos:
        - Swipe to dismiss.
        - Video controls: play/pause, mute, speed, HD, scale mode.
        - Image zoom via `ZoomableImage`.
        - Favorite and download buttons.
    - `AnimatedIconButton(...)` → Scale-animated icon button.

- **ZoomableImage.kt**
    - `ZoomableImage(...)` → Image with:
        - Double-tap to zoom.
        - Pinch-to-zoom and pan.

- **MainTopBar.kt**
    - `SelectionTopBar(...)` → Top bar for selection/duplicate modes.
    - `MainTopBar(...)` → Normal top bar with grid toggle, filters, and settings.

- **MainBottomBar.kt**
    - `MainBottomBar(...)` → Navigation bar with Feed, Favorites, Gallery, Bookmarks tabs and counts.

- **BaseSidebar.kt**
    - `BaseSidebar(...)` → Reusable sidebar container with title, dismiss, and optional footer.

- **FilterSidebar.kt**
    - `FilterSidebar(...)` → Right sidebar with controls for NSFW, sort, period, type, and tags.

- **DisplaySidebar.kt**
    - `DisplaySidebar(...)` → Left sidebar for page limit, grid columns, content type filter, and duplicate scan.

- **SettingsSidebar.kt**
    - `SettingsSidebar(...)` → Right sidebar with:
        - Appearance (Amoled mode).
        - Player settings (hide controls, HD, mute, autoplay).
        - API key.
        - Storage paths.
        - Backup import/export.
        - Backend configuration.
        - Debug toggle.
        - Cache info and clear.

- **SidebarSection.kt**
    - `SidebarSection(...)` → Expandable section header with optional icon.

- **SpeedDialFab.kt**
    - `SpeedDialFab(...)` → Expandable FAB with multiple action items.

- **AppBackHandler.kt**
    - `AppBackHandler(...)` → Handles back press:
        - Clears selection → closes left drawer → closes right drawer → double-tap to exit.

- **SplashScreen.kt**
    - `SplashScreen(onSplashFinished)` → Animated logo and loader; calls callback after delay.

- **OnboardingScreen.kt**
    - `OnboardingScreen(onSkip, onFinish)` → Multi-page pager for first-time users.

- **BookmarkDialog.kt**
    - `BookmarkDialog(onApply, onDismiss)` → Input dialog to save a bookmark title.

- **JumpDialog.kt**
    - `JumpDialog(currentCursor, onApply, onDismiss)` → Input dialog to jump to a specific cursor.

- **ConfirmationDialog.kt**
    - `ConfirmationDialog(title, message, onConfirm, onDismiss)` → Generic confirmation dialog.

- **Shimmer.kt**
    - `shimmerBackground()` → Modifier for loading shimmer effect.

- **SkeletonGrid.kt**
    - `SkeletonGrid(columnCount)` → Skeleton loading grid for initial load.

- **VideoPlayerModels.kt**
    - `ScaleMode` enum: NORMAL, CROP, FULL.
    - `BackendConfig` data class.
    - `LocalBackendConfig` CompositionLocal.

### Utilities

- **CivitaiUrlBuilder.kt**
    - `compressUrl(url)` / `expandUrl(compressed, type)` → Compress and expand URLs.
    - `isCivitaiMediaUrl(url)` → Check if URL is from Civitai.
    - `extractCivitaiUuid(url)` → Extract UUID from Civitai URL.
    - `toBackendUrl(type, quality, uuid)` → Build backend media URL.
    - `buildBackendFeedUrl(originalUrl)` → Build backend feed URL.
    - `getThumbnailUrl(url, width)` / `getVideoThumbnailUrl(url)` / `getVideoPreviewUrl(url)` / `getVideoOriginalUrl(url)` / `getImageOriginalUrl(url)` → Quality-specific URL builders.
    - `getFallbackChain(url)` → Generate fallback URLs for failed requests.
    - `getBaseUrl(url)` / `modifyUrl(url, variant)` → URL manipulation helpers.

- **FileUtils.kt**
    - `saveBitmapAsWebP(bitmap, outputFile, quality)`
    - `convertFileToWebP(inputFile, outputFile, quality)`
    - `detectExtension(contentType, source, url)` → Detect file extension via magic bytes, content-type, or URL.
    - `getExtensionFromBytes(bytes)` → Sniff extension from first bytes.
    - `isVideoFile(bytes)` / `isImageFile(bytes)` → Type checks.
    - `isRealMedia(file)` → Validates a file is actual media.
    - `calculateHash(file)` → SHA-256 hash of file.
    - `findDuplicateGroups(files)` → Groups files by size then hash.

- **DuplicateDetector.kt**
    - `findDuplicateGroups(files, context)` → Finds duplicate files in gallery.
    - `removeDuplicateGroups(duplicateGroups, context, onRefreshDownloadedIds)` → Removes duplicates keeping newest.

- **MediaProcessor.kt**
    - `extractVideoFrame(videoFile, outputFile, quality)` → Extracts frame from video as WebP.
    - `convertToWebP(inputFile, outputFile, quality)` → Converts image to WebP.

- **Utils.kt**
    - `scrollbar(state, width, color)` / `gridScrollbar(state, width, color, endOffset)` → Custom scrollbar modifiers.
    - `hasLocalCache(context, imageId, isVideo, favoritesDir)` → Checks if thumbnail exists locally.
    - `hasFullCache(context, imageId, isVideo, favoritesDir)` → Checks if full-resolution file exists locally.
    - `resolveImageData(context, image, favoriteInfo, ...)` → Resolves image source: local file or remote URL.
    - `getThumbnailUrl(url, width)` / `getVideoThumbnailUrl(url)` / `getVideoPreviewUrl(url)` / `getVideoOriginalUrl(url)` / `getOriginalUrl(url)` / `modifyCivitaiUrl(url, variant)` → Delegates to `CivitaiUrlBuilder`.
    - `formatDuration(ms)` → Formats milliseconds as time string.
    - `playerPoolSizeForColumns(columns)` → Maps column count to recommended player pool size.

- **Constants.kt**
    - Defines constants: debounce delays, pool sizes per column, cleanup intervals, etc.

- **Logger.kt**
    - `d(tag, message)` / `e(tag, message, throwable)` / `w(tag, message)` / `v(tag, message)` / `i(tag, message)` → Conditional logging gated by `debugEnabled`.

- **UriUtils.kt**
    - `resolveUriToPath(context, uri)` → Converts a DocumentFile tree URI to a real filesystem path.

### Models

- **CivitaiImage.kt**
    - `CivitaiImage(id, url, width, height, nsfw, type, meta)` → Core image/video data.
    - `CivitaiApiResponse(items, metadata)` → API response wrapper.
    - `Metadata(nextCursor)` → Pagination metadata.
    - `VideoMeta(size)` → Video metadata.

- **FavoriteImage.kt**
    - `FavoriteImage(id, url, width, height, nsfw, type, timestamp, isSynced)` → Room entity.
    - `toCivitaiImage()` / `fromCivitaiImage(image, isSynced)` → Conversion helpers.

- **FeedItemCache.kt**
    - `FeedItemCache(id, url, width, height, nsfw, type, feedType, orderIndex)` → Caches feed items.
    - `toCivitaiImage()` / `fromCivitaiImage(image, feedType, index)` → Conversion helpers.

- **Bookmark.kt**
    - `Bookmark(id, title, type, sort, period, nsfw, cursor, tags, timestamp)` → Saved feed configuration.

- **AppBackup.kt**
    - `AppBackup(version, settings, favorites, bookmarks, feedItems)` → Top-level backup model.
    - `AppSettingsBackup(...)` → All user settings.
    - `FavoriteImageBackup(...)`, `BookmarkBackup(...)`, `FeedItemBackup(...)` → Individual backup models.

## 4. UI Implementation Details

### Dual-Sidebar Architecture

- Left Sidebar (LTR): `ModalNavigationDrawer` for Display options.
- Right Sidebar (RTL): `ModalNavigationDrawer` nested inside left drawer; used for Filters or Settings.
- Gesture control:
    - A `pointerInput` on the content detects drag start near left/right edges.
    - Each drawer's `gesturesEnabled` is set based on where the drag started and whether the other drawer is open, avoiding conflicts.

### Video Engine

- `VideoPlayerManager` maintains a pool of ExoPlayer instances.
- Pool size is dynamically adjusted based on grid column count via `playerPoolSizeForColumns(columns)`.
- A custom `DefaultRenderersFactory` filters problematic hardware decoders (e.g., `c2.qti.avc.decoder`).
- Features:
    - Interactive seekbar with live seeking.
    - Pinch-to-zoom and pan on videos.
    - Double-tap to reset zoom.

### Shared UI Components

- `AppScaffold`: Wraps TopBar, BottomBar, and FAB; handles pull-to-refresh and pull-to-load-more.
- `ImageCard`:
    - Resolves local vs remote image.
    - Shows reactive cache status: yellow if partial, green if fully cached.
    - Triggers autoplay for videos when visible.
    - Animates heart on favorite.

## 5. Data Layer and Resource Management

### Smart Image Resolution & Content Verification

- `CivitaiThumbnailInterceptor`:
    - Validates media by magic bytes.
    - On failure, retries with fallback URLs (alt widths, transcode, original).
- `CivitaiInterceptor`:
    - Adds User-Agent and Authorization headers.
    - Optionally redirects media and feed requests through a custom backend.
- `resolveImageData(...)`:
    - Checks local favorites directory first.
    - Falls back to appropriate remote URL based on type and quality.

### Duplicate Detection

- Uses SHA-256 hashing via `FileUtils.calculateHash()`.
- `FileUtils.findDuplicateGroups(files)`:
    - Groups by file size.
    - Then by hash.
- Both `FavoritesRepository` and `GalleryRepository` expose `findDuplicateGroups()` and `removeDuplicates()`.

### Persistence & Backup

- Room:
    - `favorite_images`: Stores favorited images and their sync status.
    - `feed_cache`: Caches feed items to survive restarts.
    - `bookmarks`: Saved feed configurations.
- DataStore:
    - Stores preferences, scroll positions, and cursors.
- `BackupRepository`:
    - Uses Moshi to serialize/deserialize settings, favorites, and bookmarks into a single JSON.
    - Supports export to and import from a file via `ContentResolver`.

### Local Resource Caching

- `FavoritesRepository` manages a `favorites/` directory with subfolders:
    - `image/thumbnails`, `image/previews`, `video/thumbnails`, `video/previews`.
- `ensureFavoriteResources(image, force, onProgress)`:
    - Downloads thumbnail and preview in parallel with concurrency limits.
    - Uses `MediaMetadataRetriever` for video frame extraction.
    - Converts images to WebP to save space.
    - Marks `isSynced = true` when both thumbnail and preview exist.

## 6. Build and Deployment

- `build.sh`:
    - Runs Gradle assembleRelease.
    - Signs the APK with `keystore.jks`.
    - Installs via `adb`.

## 7. Technical Stack

- UI Framework: Jetpack Compose (Material 3)
- Dependency Injection: Hilt
- Networking: Retrofit / OkHttp / Moshi
- Database: Room (v1, 3 entities)
- Persistence: Jetpack DataStore (Preferences)
- Image Loading: Coil3 (with Video Frame support)
- Media Playback: Media3 / ExoPlayer
- Minimum SDK: Android 11 (API 30)
- Target SDK: Android 16 (API 36)

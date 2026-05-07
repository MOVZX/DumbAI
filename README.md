# Dibella

**Dibella** is a high-performance **Android** client for the **Civitai** platform, built with **Jetpack Compose**, **MVVM**, and a centralized architecture for browsing, favoriting, and managing AI-generated media.

## Core Architecture

### MVVM with Centralized Logic

All ViewModels extend `BaseViewModel`, which centralizes:

- **Shared Actions**: `toggleFavorite`, `ensureFavoriteResources`, `ensureFavoriteResourcesThrottled`, `performDownload`.
- **Persistent Preferences**: Grid columns, page limit, and scroll settings via `UserPreferencesRepository`.
- **Scroll Management**: Debounced scroll position save/restore per screen.
- **Message Bus**: `sendMessage(resId)` as a `SharedFlow<Int>` for Toasts.
- **Concurrency**: IO dispatchers with limited parallelism (16) for sync-heavy operations.

### State Management

Unidirectional Data Flow (UDF):

- Each screen is driven by a single `UiState` implementing `BaseUiState`.
- User actions call ViewModel functions.
- ViewModel updates `MutableStateFlow<UiState>`.
- UI observes via `collectAsState()`.

### Data Layer

- **Room Database**:
    - `favorite_images` → favorite images and sync status.
    - `feed_cache` → cached feed items.
    - `bookmarks` → saved feed configurations.
- **Repositories**:
    - `FavoritesRepository`: Favorites CRUD, local caching (thumbnails/previews), parallel sync, stale cleanup, duplicate detection.
    - `FeedRepository`: Feed fetching from Civitai with retry, cursor recovery, and caching.
    - `GalleryRepository`: Local gallery scanning, downloads, video metadata, duplicate detection.
    - `BookmarkRepository`: Bookmarks CRUD.
    - `UserPreferencesRepository`: Jetpack DataStore for all settings, filters, scroll positions, cursors, and backend config.
    - `BackupRepository`: JSON export/import of settings, favorites, bookmarks, and feed cache.

## Key Features

### Video Engine

- **ExoPlayer Pooling**:
    - `VideoPlayerManager` maintains a pool of ExoPlayer instances.
    - Pool size is dynamic based on grid columns:
        - 1 column → 4
        - 2 columns → 10
        - 3 columns → 18
        - 4 columns → 24
- **Hardware-Aware Decoding**:
    - Custom `DefaultRenderersFactory` excludes problematic decoders (e.g., `c2.qti.avc.decoder`).
- **Playback Features**:
    - Autoplay/mute controls.
    - Interactive seekbar with live seeking.
    - Pinch-to-zoom and pan.
    - Double-tap to zoom/reset.
    - Playback speed and scale modes (NORMAL, CROP, FULL).

### Dual-Sidebar Navigation

- **Left Sidebar** (LTR):
    - `DisplaySidebar`: Grid columns, page limit, media type filter, duplicate scan.
- **Right Sidebar** (RTL):
    - `FilterSidebar`: NSFW, sort, period, type, tags.
    - `SettingsSidebar`: API key, storage paths, backup import/export, backend config, player settings, appearance, cache info.
- **Gesture Control**:
    - `pointerInput` on content detects drag near edges.
    - Each drawer’s `gesturesEnabled` adapts to avoid conflicts.

### Local Resource Caching

- `FavoritesRepository` manages a `favorites/` directory with:
    - `image/thumbnails`, `image/previews`, `video/thumbnails`, `video/previews`.
- `ensureFavoriteResources(image, force, onProgress)`:
    - Downloads thumbnail and preview in parallel.
    - Uses `MediaMetadataRetriever` for video frames.
    - Converts images to WebP.
    - Marks `isSynced = true` when both files exist.

### Duplicate Detection & Management

- Uses SHA-256 hashing via `FileUtils.calculateHash()`.
- Groups files by size, then by hash.
- Available in:
    - `FavoritesRepository.findDuplicateGroups()` / `removeDuplicates()`.
    - `GalleryRepository.findDuplicateGroups()` / `removeDuplicates()`.
- Exposed via `FavoritesViewModel` and `GalleryViewModel`.

### Smart Image Resolution & Verification

- `CivitaiThumbnailInterceptor`:
    - Validates media via magic bytes.
    - On failure, retries with fallback URLs (alt widths, transcode, original).
- `CivitaiInterceptor`:
    - Injects User-Agent and Authorization headers.
    - Optionally redirects media and feed requests through a custom backend.
- `resolveImageData(...)`:
    - Checks local favorites directory first.
    - Falls back to appropriate remote URL based on type and quality.

### Dynamic Networking

- Custom OkHttp Interceptors:
    - `CivitaiInterceptor`:
        - Injects Bearer token, User-Agent.
        - Redirects to backend if enabled.
        - Adjusts timeouts per endpoint.
    - `CivitaiThumbnailInterceptor`:
        - Content-aware retries for 404/403.
        - Adds caching headers to valid media.

### Bookmarks

- `BookmarkRepository` + `BookmarkDao`:
    - Stores feed configurations (type, sort, period, NSFW, cursor, tags).
- `BookmarkViewModel`:
    - `deleteBookmark(bookmark)`
    - `updateBookmarkTitle(bookmark, newTitle)`
- `FeedViewModel`:
    - `saveBookmark(title)` → saves current feed config.
    - `loadBookmark(bookmark)` → applies settings and jumps to cursor.

### Full-Screen Viewer

- `FullScreenImage`:
    - Horizontal swipe pager.
    - `ZoomableImage` for pinch-to-zoom and double-tap.
    - Video controls: play/pause, mute, speed, HD, scale mode.
    - Favorite and download buttons.

### Onboarding

- `OnboardingScreen`:
    - Multi-page pager for first-time users.
    - Skippable; completion persisted in DataStore.

## Screenshots

![Main Page: 3 grids](screenshots/1.png)
![Main Page: 2 grids](screenshots/2.png)
![Main Page: 1 grids](screenshots/3.png)
![Favorite Page](screenshots/4.png)
![Gallery Page](screenshots/5.png)
![Filter Options](screenshots/6.png)
![View Options](screenshots/7.png)
![Settings](screenshots/8.png)

## Technical Stack

- **UI Framework**: Jetpack Compose (Material 3)
- **Dependency Injection**: Hilt
- **Networking**: Retrofit / OkHttp / Moshi
- **Database**: Room (v1, 3 entities)
- **Persistence**: Jetpack DataStore (Preferences)
- **Image Loading**: Coil3 (with Video Frame support)
- **Media Playback**: Media3 / ExoPlayer
- **Concurrency**: Kotlin Coroutines & Flow
- **Testing**: JUnit 4 / MockK (JVM Unit Tests)

## Testing

Dibella includes JVM-based unit tests for core utilities and networking:

- **URL Resolution**: `CivitaiUrlBuilder` transformations and fallback chains.
- **Media Integrity**: `FileUtils` magic byte sniffing and type detection.
- **Networking Interceptors**: `CivitaiThumbnailInterceptor` retry and recovery.
- **Data Integrity**: `AppBackup` serialization and deserialization.

Run:

```bash
./gradlew test
```

## Project Structure

- `api/`:
    - `CivitaiApi`: Retrofit interface.
    - `CivitaiInterceptor`: Auth, backend routing, logging.
    - `CivitaiThumbnailInterceptor`: Thumbnail fallback and validation.
- `data/`:
    - `AppDatabase`: Room database.
    - `FavoriteImageDao`, `FeedCacheDao`, `BookmarkDao`.
    - `FavoritesRepository`, `FeedRepository`, `GalleryRepository`, `BookmarkRepository`, `UserPreferencesRepository`, `BackupRepository`.
- `model/`:
    - `CivitaiImage`, `FavoriteImage`, `FeedItemCache`, `Bookmark`, `AppBackup`.
- `ui/screens/`:
    - `MainScreen`, `FeedScreen`, `FavoritesScreen`, `GalleryScreen`, `BookmarkScreen`, `OnboardingScreen`.
- `ui/components/`:
    - `AppScaffold`, `ImageGrid`, `ImageCard`, `VideoPlayer`, `VideoPlayerManager`, `FullScreenImage`, `ZoomableImage`, sidebars, FABs, dialogs.
- `util/`:
    - `CivitaiUrlBuilder`, `FileUtils`, `DuplicateDetector`, `MediaProcessor`, `Logger`, `UriUtils`, `Utils`, `Constants`.
- `viewmodel/`:
    - `BaseViewModel` and `BaseUiState`.
    - `FeedViewModel`, `FavoritesViewModel`, `GalleryViewModel`, `SettingsViewModel`, `BookmarkViewModel`.
- `di/`:
    - `AppModule`: Hilt dependency injection.

## Build and Installation

Build and install on a connected device:

```bash
./build.sh install
```

This compiles the release APK, signs it, and installs via ADB.

## License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

---

<p align="center">
  Made with ❤️ by <a href="https://github.com/MOVZX">MOVZX</a>
</p>

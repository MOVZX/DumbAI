# Technical Specifications: Dibella

This document provides a detailed technical overview of the Dibella Android application. It is intended for software engineers and LLMs to understand the system architecture, state management patterns, and specialized logic implementations.

## 1. Architectural Patterns

### MVVM with Centralized Logic

The application utilizes the Model-View-ViewModel (MVVM) pattern. To mitigate boilerplate, a `BaseViewModel` provides standardized implementations for:

- **Persistent Preferences**: Direct interface with `UserPreferencesRepository` for grid and request settings.
- **Scroll Management**: Centralized `saveScrollPosition` and restoration logic mapped to specific content types.
- **Message Bus**: A resource-ID based messaging system for decoupled UI notifications (Toasts) via `SharedFlow`.
- **Shared Actions**: Centralized logic for `toggleFavorite`, `ensureFavoriteResources`, and `performDownload`.
- **Concurrency Control**: Utilizes `Dispatchers.IO.limitedParallelism` to manage concurrent heavy operations (like resource synchronization) to avoid UI lag.

### Dependency Injection

**Hilt** is used for dependency injection. All repositories and network services are provided as Singletons within the `AppModule`. ViewModels are injected with specific repositories to maintain a clean separation of concerns.

## 2. State Management and Data Flow

### Unidirectional Data Flow (UDF)

Each screen is driven by a single `UiState` data class emitted as a `StateFlow`. These states implement the `BaseUiState` interface for consistency.

- **BaseUiState**: An interface defining common properties like `images`, `isLoading`, `gridColumns`, `scrollIndex`, `scrollOffset`, and `downloadProgresses`.
- **User Actions**: UI triggers functions in the ViewModel.
- **State Updates**: ViewModel updates the private `_uiState` using `MutableStateFlow.update`.
- **Observation**: The UI observes the `uiState` using `collectAsState`.

### Scroll Restoration Logic

A critical specialized implementation manages scroll positions during data refreshes and content type switches:

1.  **State Tracking**: Each ViewModel maintains an `isRestored` flag in its `UiState`.
2.  **Persistent Storage**: Scroll positions (index and offset) are debounced (500ms) and saved to **Jetpack DataStore**.
3.  **Keyed Invalidation**: The restoration flag for the feed is reset to `false` whenever filter parameters change.
4.  **Forced Positioning**: A `LaunchedEffect` in the UI monitors `images.isNotEmpty()`. If `isRestored` is `false`, it executes `gridState.scrollToItem(index, offset)` and calls `markRestored()` on the ViewModel.

## 3. UI Implementation Details

### Dual-Sidebar Architecture and Gesture Control

The application implements a robust dual-sidebar system that handles complex configuration without gesture conflicts:

- **Nesting**: The `ModalNavigationDrawer` for the Right Sidebar (RTL) is nested inside the `ModalNavigationDrawer` for the Left Sidebar (LTR).
- **Dynamic Gesture Enabling**: To prevent the inner drawer from intercepting all edge swipes, the `gesturesEnabled` flag for both drawers is toggled dynamically.
- **Edge Peeking**: A `pointerInput(PointerEventPass.Initial)` modifier on the content area peeks at touch events. It identifies if a swipe started near the left or right edge and updates a `gestureSide` state.
- **State-Based Priority**:
    - The Left Drawer's gestures are enabled only if it's already open OR if the touch started on the left edge while the Right Drawer is closed.
    - The Right Drawer's gestures are enabled only if it's already open OR if the touch started on the right edge while the Left Drawer is closed.

### Hybrid Video Engine

The application implements a high-performance media engine with advanced pooling and fallback logic:

- **ExoPlayer Pooling**: `VideoPlayerManager` maintains a pool of `ExoPlayer` instances (default max 12).
    - **Optimization**: The pool size is dynamically adjusted based on grid column count to balance memory usage and scrolling smoothness.
    - **Codec Filtering**: A custom `DefaultRenderersFactory` filters out known problematic hardware decoders (e.g., `c2.qti.avc.decoder`, `c2.qti.avc.decoder.low_latency`) to prevent playback stalls.
- **MPV Bridge**: `MpvPlayer` provides an alternative playback path using `libmpv` via JNI.
    - **Ownership Logic**: Since `MPVLib` is a singleton wrapper, a `lastOwnerId` system ensures only the visible player instance controls the global MPV state.
    - **Features**: Supports precise seeking, speed control, and hardware acceleration via Android's GPU context.
- **Interactive UI**: Custom seekbars with live frame seeking and automatic playback pause during user interaction.
- **Zoom and Pan**: Pinch-to-zoom and pan gestures on video players, with double-tap to reset.

### Shared UI Components

- **AppScaffold**: A high-level wrapper that manages the TopBar, BottomBar, and `AppFab`.
- **ImageCard**: A sophisticated component that handles dynamic image resolution, heart animations, and reactive cache status indicators.
    - **Reactive Cache Status**: Observes database changes and download progress completion to update cache indicators (Yellow for partial, Green for fully cached).
    - **Optimized Assets**: Resolves whether to show a remote URL or a local file from the `favorites/` directory.

## 4. Data Layer and Resource Management

### Smart Image Resolution & Content Verification

The networking layer is optimized for reliability:

- **Verification Interceptor**: `CivitaiThumbnailInterceptor` verifies content by peeking at magic bytes. If invalid data is returned, it automatically retries with alternative widths (450, 800, 1000, 1500px).
- **Fallback Chain**: Implements a progressive fallback system for thumbnails and video previews (using `transcode=true`).
- **Anti-Bot Headers**: All requests include standard `User-Agent` and `Referer` headers.

### Duplicate Detection Logic

- **Content Hashing**: Uses **SHA-256** to generate unique fingerprints for media files.
- **Grouping**: Files with identical hashes are grouped together.
- **Management**: Users can identify duplicate groups in both the `FavoritesRepository` (favorited resources) and `GalleryRepository` (downloaded files) and perform bulk deletion.

### Persistence & Backup

- **Room (v2)**: Manages `favorite_images` and `feed_cache` tables.
- **DataStore**: Manages settings and persistent scroll positions (index/offset per content type).
- **BackupRepository**: Uses **Moshi** to serialize/deserialize the full application state (Settings, Favorites, Cache) to a single JSON file for cross-device migration.

### Local Resource Caching

- **FavoritesRepository**: Manages a dedicated `favorites/` directory with automated `ensureFavoriteResources()` logic.
- **Parallel Sync**: Downloads thumbnails and previews concurrently using `Dispatchers.IO.limitedParallelism` (tuned for high-end SoCs).
- **Video Frame Extraction**: Uses `MediaMetadataRetriever` to extract thumbnails from local video files.

## 5. Build and Deployment

The project includes a `build.sh` script that automates:

- Gradle assembly of the Release APK.
- Code signing.
- Installation via `adb`.

## 6. Technical Stack

- **UI Framework**: Jetpack Compose (Material 3)
- **Dependency Injection**: Hilt
- **Networking**: Retrofit / OkHttp / Moshi
- **Database**: Room (v2)
- **Persistence**: Jetpack DataStore
- **Image Loading**: Coil3 (including Video Frame support)
- **Media Playback**: Media3 / ExoPlayer & MPV (libmpv via JNI)
- **Minimum SDK**: Android 11 (API 30)
- **Target SDK**: Android 16 (API 36)

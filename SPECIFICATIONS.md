# Technical Specifications: Dibella

This document provides a detailed technical overview of the Dibella Android application. It is intended for software engineers and LLMs to understand the system architecture, state management patterns, and specialized logic implementations.

## 1. Architectural Patterns

### MVVM with Centralized Logic

The application utilizes the Model-View-ViewModel (MVVM) pattern. To mitigate boilerplate, a `BaseViewModel` provides standardized implementations for:

- **Persistent Preferences**: Direct interface with `UserPreferencesRepository` for grid and request settings.
- **Scroll Management**: Centralized `saveScrollPosition` and restoration logic mapped to specific content types.
- **Message Bus**: A `SharedFlow`-based messaging system for decoupled UI notifications (Toasts) using resource IDs.
- **Shared Actions**: Centralized logic for `toggleFavorite`, `ensureFavoriteResources`, and `performDownload`.
- **Concurrency Control**: Utilizes `Dispatchers.IO.limitedParallelism` to manage concurrent heavy operations (like resource synchronization) to avoid UI lag on high-end hardware.

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
2.  **Persistent Storage**: Scroll positions (index and offset) are debounced (500ms) and saved to **Jetpack DataStore** to prevent data loss on app restart.
3.  **Keyed Invalidation**: The restoration flag for the feed is reset to `false` whenever filter parameters (NSFW, Sort, Type, etc.) change.
4.  **Forced Positioning**: A `LaunchedEffect` in the UI monitors `images.isNotEmpty()`. If `isRestored` is `false`, it executes `gridState.scrollToItem(index, offset)` and calls `markRestored()` on the ViewModel.

## 3. UI Implementation Details

### Dual-Sidebar Architecture and Gesture Control

The application implements a robust dual-sidebar system that handles complex configuration without gesture conflicts:

- **Nesting**: The `ModalNavigationDrawer` for the Right Sidebar (RTL) is nested inside the `ModalNavigationDrawer` for the Left Sidebar (LTR).
- **Dynamic Gesture Enabling**: To prevent the inner drawer from intercepting all edge swipes, the `gesturesEnabled` flag for both drawers is toggled dynamically.
- **Edge Peeking**: A `pointerInput(PointerEventPass.Initial)` modifier on the content area peeks at touch events before the drawers consume them. It identifies if a swipe started near the left or right edge and updates a `gestureSide` state.
- **State-Based Priority**:
    - The Left Drawer's gestures are enabled only if it's already open OR if the touch started on the left edge while the Right Drawer is closed.
    - The Right Drawer's gestures are enabled only if it's already open OR if the touch started on the right edge while the Left Drawer is closed.

### Shared UI Components

- **AppScaffold**: A high-level wrapper that manages the TopBar, BottomBar, and `AppFab`.
- **ImageCard**: A sophisticated component that handles dynamic image resolution, heart animations, and reactive cache status indicators.
    - **Reactive Cache Status**: Observes both database changes and download progress completion to instantly update the cloud icon (Yellow for pending/partial, Green for fully cached).
    - **Optimized Assets**: Uses localized logic to resolve whether to show a remote URL or a local file from the `favorites/` directory.
- **ImageGrid**: A regular grid implementation with shared transition support.
- **SkeletonGrid**: A non-animated loading grid using static surface colors to minimize CPU/GPU overhead during data fetch.

### Enhanced Media Playback (ExoPlayer)

The application utilizes **Media3/ExoPlayer** for an immersive video experience:

- **Interactive Seekbar**: A smooth, real-time seekbar with live frame seeking and automatic playback pause during user interaction.
- **Interactive Controls**: Play/Pause, Mute/Unmute, and dynamic Scaling Modes (**Normal**, **Crop**, **Full**).
- **Smart Mute**: Automatically detects audio tracks and disables the mute toggle for silent videos.
- **Automated Looping**: All video content is configured to loop infinitely.

## 4. Data Layer and Resource Management

### Smart Image Resolution & Content Verification

The networking layer is highly optimized for reliability and performance:

- **Verification Interceptor**: `CivitaiThumbnailInterceptor` verifies the actual content of successful responses by peeking at magic bytes (PNG, JPEG, etc.). If the server returns a successful status (200) but invalid data (e.g., a JSON error or empty file), the interceptor automatically retries with alternative widths.
- **Fallback Logic**: Retries failed requests with widths 450, 800, 1000, and 1500px. A high-efficiency "Video Preview" fallback (transcode=true) is attempted for videos before resorting to original video source.
- **Timeout Alignment**: Network timeouts are aligned (8s for retries, 10s for global read) to ensure fallback logic triggers before connection drops.
- **Anti-Bot Headers**: All Civitai requests include standard `User-Agent` and `Referer` headers to avoid CDN blocking.

### Persistence & Gallery Synchronization

- **Room (v2)**: Manages `favorite_images` and `feed_cache` tables.
- **DataStore**: Manages settings and persistent scroll positions.

### Local Resource Caching & Performance

The `FavoritesRepository` and `BaseViewModel` manage a dedicated `favorites/` directory:

- **Concurrency Optimization**: Performs thumbnail and full-content downloads concurrently using coroutines to maximize throughput on fast connections.
- **Restricted Threading**: Employs a dedicated dispatcher with `limitedParallelism` (tuned for Snapdragon 8 Gen 3) to balance background throughput with UI responsiveness.
- **Video Frame Extraction**: Uses `MediaMetadataRetriever` to extract thumbnails from downloaded videos.
- **Proactive Fetching**: `ensureFavoriteResources()` pre-downloads thumbnails and previews for all favorited items.
- **Orphan Cleanup**: `clearUnusedResources()` removes cached files for unfavorited items.

## 5. Build and Deployment

The project includes a `build.sh` script that automates:

- Gradle assembly of the Release APK
- Code signing with appropriate keystore
- Installation via `adb` to connected devices

## 6. Technical Stack

- **UI Framework**: Jetpack Compose (Material 3)
- **Dependency Injection**: Hilt
- **Networking**: Retrofit / OkHttp / Moshi
- **Database**: Room (v2)
- **Persistence**: Jetpack DataStore
- **Image Loading**: Coil3 (including Video Frame support)
- **Media Playback**: Media3 / ExoPlayer
- **Coroutines**: Structured concurrency with ViewModel scopes and optimized dispatchers.
- **Minimum SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 14 (API 34)

# Dibella

**Dibella** is a sophisticated **Android** client for the **Civitai** platform, built using modern software engineering principles and the latest **Android** development technologies. The application focuses on providing a high-performance, maintainable, and visually consistent experience for browsing and managing AI-generated media.

## Project Overview

The application follows the **Model-View-ViewModel** _(MVVM)_ architectural pattern, utilizing **Jetpack Compose** for a fully declarative user interface. A major architectural focus has been placed on logic centralization and UI consistency, resulting in a streamlined codebase that minimizes redundancy while maximizing scalability.

## Core Architecture

### ViewModel Layer

All business logic is driven by a centralized `BaseViewModel`, which standardizes:

- **Shared Actions**: Centralized logic for `toggleFavorite` and `performDownload` to ensure consistent behavior across all screens.
- **Persistent Preference Management**: Direct interface for grid layout, page limits, and content settings.
- **Scroll Management**: State-aware scroll position saving and restoration logic.
- **Message Bus**: A resource-ID based messaging flow for decoupled UI notifications _(Toasts)_.
- **Resource Synchronization**: Throttled background processing for media resources to maintain UI fluidity on flagship hardware.

### State Management

The application utilizes a **Unidirectional Data Flow** _(UDF)_ pattern. Each screen is driven by a `UiState` that implements a common `BaseUiState` interface, ensuring a consistent contract for images, loading states, and navigation metadata across the entire UI.

### Data Layer

The data layer is highly modular and utilizes specialized repositories:

- **GalleryRepository**: Manages local media scanning and public download operations.
- **FavoritesRepository**: Handles local caching of media resources _(thumbnails and previews)_ to ensure offline availability for favorited content. It features parallel resource synchronization for maximum throughput.
- **UserPreferencesRepository**: Leverages **Jetpack DataStore** for persistent storage of application settings, filters, and navigation states.
- **BackupRepository**: Provides robust JSON-based import and export capabilities for user data.
- **Room Database**: Manages `favorite_images` and `feed_cache` tables for offline persistence.

## Key Features

### Hybrid Video Engine

Dibella features a custom-built video engine that prioritizes performance and compatibility:

- **ExoPlayer Pooling**: A managed pool of **Media3/ExoPlayer** instances (up to 12) reduces instantiation overhead and ensures smooth playback across grids.
- **Hardware-Aware Decoding**: Custom renderer factory optimizes codec selection, bypassing problematic decoders on certain SoC architectures.
- **MPV Fallback**: An integrated **MPV (libmpv)** player provides a robust fallback for high-bitrate or complex formats that native Android decoders may struggle with.

### Dual-Sidebar Navigation

The application employs a unique navigation system based on nested **ModalNavigationDrawers** with intelligent gesture control:

- **Left Sidebar**: Dedicated to display configurations, including grid column adjustments, request limits, and per-screen media type filtering.
- **Right Sidebar**: A multi-mode panel that switches between advanced content filters and core application settings.
- **Gesture Reliability**: Dynamic edge-peeking logic ensures that left and right swipe gestures are correctly routed to the appropriate drawer without interception conflicts.

### Local Resource Caching

Dibella prioritizes accessibility by automatically downloading and managing local copies of favorited images and video previews. This system ensures that the user's collection remains browseable regardless of network status.

### Duplicate Detection & Management

The application includes advanced duplicate detection for managing large collections of AI-generated media. It utilizes **SHA-256 hashing** to identify identical files across both favorite resources and local gallery downloads, allowing for efficient bulk cleanup.

### Smart Image Resolution & Verification

A dedicated utility layer dynamically resolves the most appropriate media source based on context. The application verifies the integrity of downloaded media through **magic byte sniffing**, ensuring that only valid images are displayed and automatically retrying failed thumbnails with alternative resolutions and high-efficiency video previews.

### Dynamic Networking

The networking layer uses custom **OkHttp Interceptors** to handle:

- **Secure Auth**: Scoped API key injection for API requests only.
- **Anti-Bot Protection**: Browser-like headers (User-Agent/Referer) to ensure reliable CDN access.
- **Robust Fallbacks**: Content-aware retries for 404/403 errors and optimized timeouts for maximum responsiveness.

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

- **UI Framework**: Jetpack Compose _(Material 3)_
- **Dependency Injection**: Hilt
- **Networking**: Retrofit / OkHttp / Moshi
- **Database**: Room
- **Persistence**: Jetpack DataStore
- **Image Loading**: Coil3 _(including Video Frame support)_
- **Media Playback**: Media3 / ExoPlayer & MPV _(libmpv via JNI)_
- **Concurrency**: Kotlin Coroutines & Flow

## Project Structure

- `api/`: Retrofit interfaces (`CivitaiApi`) and OkHttp interceptors (`CivitaiInterceptor`, `CivitaiThumbnailInterceptor`).
- `data/`: Repositories (`FavoritesRepository`, `GalleryRepository`, `UserPreferencesRepository`, `BackupRepository`), Room DAOs (`FavoriteImageDao`, `FeedCacheDao`), and DataStore implementations.
- `model/`: Immutable data classes (`CivitaiImage`, `FavoriteImage`, `FeedItemCache`, `AppBackup`).
- `ui/screens/`: Navigation destinations (`MainScreen`, `FeedScreen`, `FavoritesScreen`, `GalleryScreen`).
- `ui/components/`: Shared UI elements (`VideoPlayer`, `MpvPlayer`, `ImageCard`, `ImageGrid`, sidebars, theme).
- `util/`: Extension functions and logic utilities (`CivitaiUrlBuilder`, `FileUtils`, `Logger`, `Utils`).
- `viewmodel/`: ViewModels inheriting from `BaseViewModel` (`FeedViewModel`, `FavoritesViewModel`, `GalleryViewModel`, `SettingsViewModel`) and their corresponding UI states.
- `di/`: Hilt dependency injection module (`AppModule`).
- `jniLibs/arm64-v8a/`: Native libmpv library for video playback fallback.

## Build and Installation

The project uses a custom build system for streamlined deployment. To build and install the application on a connected device, execute the following command:

```bash
./build.sh install
```

This will compile the release APK, sign it, and perform a streamed installation.

## License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

---

<p align="center">
  Made with ❤️ by <a href="https://github.com/MOVZX">MOVZX</a>
</p>

# Dibella

A high-performance **Android** client for the **Civitai** platform, built with **Jetpack Compose**, **MVVM**, and a centralized architecture for browsing, searching, favoriting, and managing AI-generated media.

## Key Features

- **Feed & Search** — Paginated feed with full-text search and filtering
- **Favorites** — Local favorites with parallel thumbnail/preview caching and duplicate detection
- **Gallery** — Local downloaded files with batch delete and duplicate detection
- **Video Engine** — Pooled ExoPlayer with dynamic sizing, pinch-to-zoom, autoplay, HD mode, and playback speed
- **Bookmarks** — Save & reload feed configurations and search queries
- **Dual-Sidebar Navigation** — Left: display options, Right: filters and settings
- **Smart Image Resolution** — Local-first caching with magic byte validation and automatic fallback chains
- **Backend Proxy Support** — Optional backend proxy URL redirect for media and API requests
- **Backup & Restore** — JSON export/import of all settings, favorites, bookmarks, and feed cache

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

- **UI**: Jetpack Compose (Material 3)
- **Pattern**: MVVM + Unidirectional Data Flow
- **DI**: Hilt
- **Network**: Retrofit / OkHttp / Moshi
- **Database**: Room (3 entities)
- **Persistence**: Jetpack DataStore
- **Images**: Coil3
- **Video**: Media3 / ExoPlayer
- **Min SDK**: Android 11 (API 30)
- **Target SDK**: Android 16 (API 36)

## Documentation

For complete architecture, file structure, and per-file function mappings, see [DOCUMENTATION.md](DOCUMENTATION.md).

## Build & Install

```bash
./build.sh install    # Assembles release, signs, installs via ADB
./gradlew test        # Runs unit tests
```

## License

MIT License — see [LICENSE](LICENSE) for details.

---

<p align="center">
  Made with ❤️ by <a href="https://github.com/MOVZX">MOVZX</a>
</p>

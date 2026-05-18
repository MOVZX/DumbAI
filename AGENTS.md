# Dibella — Agent Instructions

Single-module Android app (`:app`). Min SDK 30, target SDK 36. Package: `org.movzx.dibella`.

## Commands

- `./build.sh install` — assemble release, sign, install via ADB
- `./build.sh debug` — assemble debug APK
- `./build.sh clean install` — full clean build + install
- `./gradlew test` — unit tests only (5 test files, no instrumentation tests)
- `./gradlew :app:assembleRelease` / `:app:assembleDebug` — raw build without install

## Key Config

- **Signing**: reads `RELEASE_STORE_FILE`, `DIBELLA_RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `DIBELLA_RELEASE_KEY_PASSWORD` env vars; fallback defaults in `app/build.gradle.kts:25-29`
- **Lint**: `abortOnError = false`, baseline at `app/lint-baseline.xml` — don't treat lint errors as blockers
- **KSP**: used for both Room and Hilt annotation processing (`ksp.useKSP2=true` in `gradle.properties`)
- **ProGuard**: not minified for either debug or release (`isMinifyEnabled = false`)
- **Gradle cache**: config cache + parallel + daemon enabled in `gradle.properties`

## Architecture

- MVVM + unidirectional data flow; all ViewModels extend `BaseViewModel`
- Hilt DI via `AppModule.kt` in `di/` package
- Room DB with 3 entities: `favorite_images`, `feed_cache`, `bookmarks`
- All settings persisted in Jetpack DataStore via `UserPreferencesRepository`
- Coil3 for images (custom OkHttp client with interceptors), Media3/ExoPlayer for video (pooled via `VideoPlayerManager`)
- OkHttp interceptors: auth/redirect (`CivitatiInterceptor`), thumbnail fallback (`CivitatiThumbnailInterceptor`), backend retry (`CivitatiBackendRetryInterceptor`)

## Testing

- Unit tests only: `app/src/test/kotlin/org/movzx/dibella/{api,model,util}/`
- Uses JUnit 4 + Mockk
- No androidTest / instrumentation tests exist
- Test file: `./gradlew test`

## Notable Patterns

- `setActiveRoute(route)` gates background operations per screen
- Scroll positions debounced at 500ms to DataStore
- Duplicate detection uses SHA-256 hash (`FileUtils.calculateHash`)
- URLs compressed for DB storage via `CivitaiUrlBuilder.compressUrl`
- Magic byte validation in `CivitaiThumbnailInterceptor` + `FileUtils`

## Documentation

Full architecture reference in `DOCUMENTATION.md` (file tree, function mappings, component descriptions).

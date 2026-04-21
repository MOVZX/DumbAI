package org.movzx.dumbai.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import android.content.ContentValues
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import org.movzx.dumbai.api.CivitaiApi
import org.movzx.dumbai.data.FavoritesRepository
import org.movzx.dumbai.data.UserPreferencesRepository
import org.movzx.dumbai.model.CivitaiImage
import org.movzx.dumbai.model.VideoMeta
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import org.movzx.dumbai.data.FeedDao
import org.movzx.dumbai.model.CachedFeedImage
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

enum class ViewMode {
    FEED, FAVORITES, GALLERY
}

class MainViewModel(
    private val context: Context,
    private val repository: UserPreferencesRepository,
    private val favoritesRepository: FavoritesRepository,
    private val feedDao: FeedDao
) : ViewModel() {
    private val _feedImages = MutableStateFlow<List<CivitaiImage>>(emptyList())
    val feedImages: StateFlow<List<CivitaiImage>> = _feedImages.asStateFlow()

    val favoriteImages: StateFlow<List<CivitaiImage>> = favoritesRepository.allFavorites
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _galleryImages = MutableStateFlow<List<CivitaiImage>>(emptyList())
    val galleryImages: StateFlow<List<CivitaiImage>> = _galleryImages.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.FEED)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _nextCursor = MutableStateFlow<String?>(null)
    val hasMore: StateFlow<Boolean> = _nextCursor.map { it != null }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val nsfw = repository.nsfw.stateIn(viewModelScope, SharingStarted.Lazily, "None")
    val sort = repository.sort.stateIn(viewModelScope, SharingStarted.Lazily, "Most Reactions")
    val period = repository.period.stateIn(viewModelScope, SharingStarted.Lazily, "AllTime")
    val type = repository.type.stateIn(viewModelScope, SharingStarted.Lazily, "image")
    val tagId = repository.tagId.stateIn(viewModelScope, SharingStarted.Lazily, null)
    val cacheLimitGb = repository.cacheLimitGb.stateIn(viewModelScope, SharingStarted.Lazily, 10)
    val apiKey = repository.apiKey.stateIn(viewModelScope, SharingStarted.Lazily, "")
    val pageLimit = repository.pageLimit.stateIn(viewModelScope, SharingStarted.Lazily, 60)
    val gridColumns = repository.gridColumns.stateIn(viewModelScope, SharingStarted.Lazily, 2)

    val feedScrollIndex = type.flatMapLatest { repository.feedScrollIndex(it) }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)
    val feedScrollOffset = type.flatMapLatest { repository.feedScrollOffset(it) }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val favoriteIds = favoritesRepository.favoriteIds.stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    private val _cacheSize = MutableStateFlow("0 MB")
    val cacheSize: StateFlow<String> = _cacheSize.asStateFlow()

    private val _downloadStatus = MutableSharedFlow<String>()
    val downloadStatus = _downloadStatus.asSharedFlow()

    private val _downloadProgresses = MutableStateFlow<Map<Long, Float>>(emptyMap())
    val downloadProgresses: StateFlow<Map<Long, Float>> = _downloadProgresses.asStateFlow()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val civitaiApi = Retrofit.Builder()
        .baseUrl(CivitaiApi.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(CivitaiApi::class.java)

    val imageLoader = ImageLoader.Builder(context)
        .okHttpClient(okHttpClient)
        .components {
            add(VideoFrameDecoder.Factory())
        }
        .memoryCache {
            MemoryCache.Builder(context)
                .maxSizePercent(0.35)
                .strongReferencesEnabled(true)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .maxSizeBytes(10L * 1024 * 1024 * 1024) // Default 10GB, we'll make this adjustable later
                .build()
        }
        .allowHardware(true)
        .crossfade(true)
        .build()

    val downloadPath = repository.downloadPath.stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val defaultDownloadDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "DumbAI")
    private val rootDumbAiDir: File
        get() = downloadPath.value?.let { File(it as String) } ?: defaultDownloadDir

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

    private val videoMetadataDispatcher = Dispatchers.IO.limitedParallelism(3)
    private var imageFeed = emptyList<CivitaiImage>()
    private var videoFeed = emptyList<CivitaiImage>()
    private var imageCursor: String? = null
    private var videoCursor: String? = null

    init {
        viewModelScope.launch {
            imageFeed = feedDao.getCachedFeed("image").map { it.toCivitaiImage() }
            imageCursor = repository.nextCursor("image").first()

            videoFeed = feedDao.getCachedFeed("video").map { it.toCivitaiImage() }
            videoCursor = repository.nextCursor("video").first()

            val currentType = repository.type.first()

            if (currentType == "video") {
                _feedImages.value = videoFeed
                _nextCursor.value = videoCursor
            } else {
                _feedImages.value = imageFeed
                _nextCursor.value = imageCursor
            }

            if (_feedImages.value.isEmpty())
                refresh()
        }

        updateCacheSize()
    }

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode

        if (mode == ViewMode.GALLERY)
            loadGallery()
    }

    fun toggleFavoritesView() {
        _viewMode.value = if (_viewMode.value == ViewMode.FAVORITES) ViewMode.FEED else ViewMode.FAVORITES
    }

    fun loadGallery() {
        viewModelScope.launch(videoMetadataDispatcher) {
            if (!rootDumbAiDir.exists())
                return@launch

            val list = mutableListOf<CivitaiImage>()
            val files = rootDumbAiDir.listFiles() ?: emptyArray()

            for (file in files) {
                if (file.isFile) {
                    val ext = file.extension.lowercase()
                    val isVideo = ext == "mp4"
                    val isImage = ext == "jpg" || ext == "jpeg" || ext == "png" || ext == "webp"

                    if (isImage || isVideo) {
                        var w = 0
                        var h = 0

                        if (isImage) {
                            val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }

                            android.graphics.BitmapFactory.decodeFile(file.absolutePath, options)

                            w = options.outWidth
                            h = options.outHeight
                        } else if (isVideo) {
                            val retriever = android.media.MediaMetadataRetriever()

                            try {
                                retriever.setDataSource(file.absolutePath)

                                val rotation = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                                val vidW = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                                val vidH = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0

                                if (rotation == 90 || rotation == 270) {
                                    w = vidH
                                    h = vidW
                                } else {
                                    w = vidW
                                    h = vidH
                                }
                            } catch (e: Exception) { e.printStackTrace() }
                            finally {
                                retriever.release()
                            }
                        }

                        list.add(CivitaiImage(
                            id = file.nameWithoutExtension.filter { it.isDigit() }.toLongOrNull() ?: file.absolutePath.hashCode().toLong(),
                            url = file.absolutePath,
                            width = w,
                            height = h,
                            nsfw = false,
                            type = if (isVideo) "video" else "image",
                            meta = null
                        ))
                    }
                }
            }

            _galleryImages.value = list.sortedByDescending { File(it.url).lastModified() }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            if (_viewMode.value == ViewMode.GALLERY) {
                loadGallery()

                return@launch
            }

            _feedImages.value = emptyList()
            val currentType = type.value

            if (currentType == "video") {
                videoFeed = emptyList()
                videoCursor = null
            } else {
                imageFeed = emptyList()
                imageCursor = null
            }

            _nextCursor.value = null

            repository.updateNextCursor(currentType, null)
            feedDao.clearFeed(currentType)

            loadImages(isNew = true)

            updateCacheSize()
        }
    }

    fun loadMore() {
        if (_viewMode.value != ViewMode.FEED)
            return

        if (_nextCursor.value != null && !_isLoading.value)
            loadImages(isNew = false)
    }

    private fun loadImages(isNew: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            var success = false
            var attempt = 0

            while (!success) {
                try {
                    val currentType = type.value
                    val currentApiKey = if (apiKey.value.isBlank()) null else apiKey.value
                    val response = civitaiApi.getImages(
                        apiKey = currentApiKey,
                        limit = pageLimit.value,
                        nsfw = nsfw.value,
                        sort = sort.value,
                        period = period.value,
                        type = currentType,
                        tags = tagId.value,
                        cursor = _nextCursor.value
                    )

                    if (isNew) {
                        val items = response.items.distinctBy { it.id }
                        _feedImages.value = items

                        if (currentType == "video")
                            videoFeed = items
                        else
                            imageFeed = items
                    } else {
                        val currentList = _feedImages.value
                        val newList = (currentList + response.items).distinctBy { it.id }
                        _feedImages.value = newList

                        if (currentType == "video")
                            videoFeed = newList
                        else
                            imageFeed = newList
                    }

                    val cursor = response.metadata.nextCursor
                    _nextCursor.value = cursor

                    if (currentType == "video")
                        videoCursor = cursor
                    else
                        imageCursor = cursor

                    repository.updateNextCursor(currentType, cursor)

                    val entities = _feedImages.value.mapIndexed { index, img ->
                        CachedFeedImage.fromCivitaiImage(img, index, currentType)
                    }

                    feedDao.clearFeed(currentType)
                    feedDao.insertFeed(entities)

                    success = true
                    _error.value = null
                } catch (e: Exception) {
                    attempt++

                    if (attempt >= 3) {
                        _error.value = "Failed to load images after 3 attempts. Please try again."

                        break
                    }

                    _error.value = "Civitai is busy, retrying... (Attempt $attempt)"

                    delay((2000L * attempt).coerceAtMost(10000L))
                }
            }

            _isLoading.value = false
        }
    }

    fun updateFilters(nsfw: String, sort: String, period: String, newType: String, tagId: Int?) {
        viewModelScope.launch {
            val oldType = type.value
            val nsfwChanged = nsfw != repository.nsfw.first()
            val sortChanged = sort != repository.sort.first()
            val periodChanged = period != repository.period.first()
            val tagChanged = tagId != repository.tagId.first()

            repository.updateFilters(nsfw, sort, period, newType, tagId)

            if (newType != oldType && !nsfwChanged && !sortChanged && !periodChanged && !tagChanged) {
                if (newType == "video") {
                    _feedImages.value = videoFeed
                    _nextCursor.value = videoCursor
                } else {
                    _feedImages.value = imageFeed
                    _nextCursor.value = imageCursor
                }

                if (_feedImages.value.isEmpty())
                    loadImages(isNew = true)
            } else {
                refresh()
            }
        }
    }

    fun saveScrollPosition(index: Int, offset: Int) {
        viewModelScope.launch {
            repository.updateScrollPosition(type.value, index, offset)
        }
    }

    fun getLocalFavoriteFile(id: Long): File? = favoritesRepository.getLocalFile(id)
    fun updateApiKey(key: String) = viewModelScope.launch { repository.updateApiKey(key) }
    fun updatePageLimit(limit: Int) = viewModelScope.launch { repository.updatePageLimit(limit); refresh() }
    fun updateGridColumns(columns: Int) = viewModelScope.launch { repository.updateGridColumns(columns) }
    fun updateDownloadPath(path: String?) = viewModelScope.launch { repository.updateDownloadPath(path); loadGallery() }
    fun toggleFavorite(image: CivitaiImage) = viewModelScope.launch { favoritesRepository.toggleFavorite(image) }

    fun clearImageCache() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                imageLoader.diskCache?.clear()
                imageLoader.memoryCache?.clear()
            }
            updateCacheSize()
        }
    }

    private fun updateCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            val size = imageLoader.diskCache?.size ?: 0L
            val sizeMb = size.toDouble() / (1024 * 1024)

            val formattedSize = if (sizeMb >= 1024) {
                String.format("%.2f GB", sizeMb / 1024)
            } else {
                String.format("%.2f MB", sizeMb)
            }

            _cacheSize.value = formattedSize
        }
    }

    fun deleteLocalFile(image: CivitaiImage) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(image.url)

                if (file.exists() && file.delete()) {
                    _galleryImages.value = _galleryImages.value.filter { it.id != image.id }
                    _downloadStatus.emit("File deleted")
                } else {
                    _downloadStatus.emit("Failed to delete")
                }
            } catch (e: Exception) {
                _downloadStatus.emit("Error: ${e.localizedMessage}")
            }
        }
    }

    fun downloadImage(image: CivitaiImage) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!rootDumbAiDir.exists())
                    rootDumbAiDir.mkdirs()

                _downloadProgresses.update { it + (image.id to 0f) }

                val request = Request.Builder().url(image.url).build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful)
                        throw Exception("Failed to download")

                    val body = response.body ?: throw Exception("Null body")
                    val contentLength = body.contentLength()
                    val ext = if (image.type == "video" || image.url.endsWith(".mp4", ignoreCase = true)) "mp4" else "jpg"
                    val file = File(rootDumbAiDir, "DumbAI_${image.id}.$ext")

                    body.byteStream().use { input ->
                        java.io.FileOutputStream(file).use { output ->
                            val buffer = ByteArray(8192)
                            var totalBytesRead = 0L
                            var bytesRead: Int

                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                totalBytesRead += bytesRead

                                if (contentLength > 0) {
                                    val progress = totalBytesRead.toFloat() / contentLength

                                    _downloadProgresses.update { it + (image.id to progress) }
                                }
                            }
                        }
                    }

                    android.media.MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
                    _downloadStatus.emit("Saved to ${rootDumbAiDir.name}/${file.name}")
                    loadGallery()
                }
            } catch (e: Exception) {
                _downloadStatus.emit("Download failed: ${e.localizedMessage}")
            } finally {
                _downloadProgresses.update { it - image.id }
            }
        }
    }

    fun exportData(uri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val favorites = favoritesRepository.getAllFavoritesSync()
                val settings = repository.getCurrentSettings()

                val backup = org.movzx.dumbai.model.AppBackup(
                    version = 1,
                    settings = settings,
                    favorites = favorites
                )

                val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                val adapter = moshi.adapter(org.movzx.dumbai.model.AppBackup::class.java)
                val json = adapter.toJson(backup)

                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(json.toByteArray())
                }
                _downloadStatus.emit("Export successful")
            } catch (e: Exception) {
                _downloadStatus.emit("Export failed: ${e.localizedMessage}")
                e.printStackTrace()
            }
        }
    }

    fun importData(uri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader().readText()
                } ?: throw Exception("Could not read file")

                val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                val adapter = moshi.adapter(org.movzx.dumbai.model.AppBackup::class.java)
                val backup = adapter.fromJson(json) ?: throw Exception("Invalid backup file")

                backup.settings?.let { repository.importSettings(it) }
                favoritesRepository.importFavorites(backup.favorites)
                _downloadStatus.emit("Import successful")
                delay(1000)
                restartApp()
            } catch (e: Exception) {
                _downloadStatus.emit("Import failed: ${e.localizedMessage}")
                e.printStackTrace()
            }
        }
    }

    private fun restartApp() {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)

        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            0,
            intent,
            android.app.PendingIntent.FLAG_CANCEL_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager

        alarmManager.set(android.app.AlarmManager.RTC, System.currentTimeMillis() + 100, pendingIntent)

        android.os.Process.killProcess(android.os.Process.myPid())
        System.exit(0)
    }
}

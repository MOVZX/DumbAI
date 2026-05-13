package org.movzx.dibella.di

import android.content.Context
import coil3.request.crossfade
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath
import org.movzx.dibella.api.CivitaiApi
import org.movzx.dibella.api.CivitaiBackendRetryInterceptor
import org.movzx.dibella.api.CivitaiSearchApi
import org.movzx.dibella.data.AppDatabase
import org.movzx.dibella.data.FavoriteImageDao
import org.movzx.dibella.data.FavoritesRepository
import org.movzx.dibella.data.UserPreferencesRepository
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        civitaiInterceptor: org.movzx.dibella.api.CivitaiInterceptor,
        backendRetryInterceptor: CivitaiBackendRetryInterceptor,
    ): OkHttpClient {
        val dispatcher =
            okhttp3.Dispatcher().apply {
                maxRequests = 64
                maxRequestsPerHost = 15
            }

        return OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(civitaiInterceptor)
            .addInterceptor(backendRetryInterceptor)
            .addInterceptor(org.movzx.dibella.api.CivitaiThumbnailInterceptor())
            .build()
    }

    @Provides
    @Singleton
    fun provideCivitaiBackendRetryInterceptor(
        preferencesRepository: UserPreferencesRepository
    ): CivitaiBackendRetryInterceptor {
        return CivitaiBackendRetryInterceptor(preferencesRepository)
    }

    @Provides
    @Singleton
    @Named("default")
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://civitai.com/api/v1/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideCivitaiApi(@Named("default") retrofit: Retrofit): CivitaiApi {
        return retrofit.create(CivitaiApi::class.java)
    }

    @Provides
    @Singleton
    @Named("search")
    fun provideSearchRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://search-new.civitai.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideCivitaiSearchApi(@Named("search") searchRetrofit: Retrofit): CivitaiSearchApi {
        return searchRetrofit.create(CivitaiSearchApi::class.java)
    }

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient,
    ): coil3.ImageLoader {
        return coil3.ImageLoader.Builder(context)
            .components {
                add(coil3.network.okhttp.OkHttpNetworkFetcherFactory(okHttpClient))
                add(coil3.video.VideoFrameDecoder.Factory())
            }
            .memoryCache {
                coil3.memory.MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                coil3.disk.DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(10L * 1024 * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideFavoriteImageDao(database: AppDatabase): FavoriteImageDao {
        return database.favoriteImageDao()
    }

    @Provides
    fun provideFeedCacheDao(database: AppDatabase): org.movzx.dibella.data.FeedCacheDao {
        return database.feedCacheDao()
    }

    @Provides
    fun provideBookmarkDao(database: AppDatabase): org.movzx.dibella.data.BookmarkDao {
        return database.bookmarkDao()
    }

    @Provides
    @Singleton
    fun provideBookmarkRepository(
        bookmarkDao: org.movzx.dibella.data.BookmarkDao
    ): org.movzx.dibella.data.BookmarkRepository {
        return org.movzx.dibella.data.BookmarkRepository(bookmarkDao)
    }

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(
        @ApplicationContext context: Context
    ): UserPreferencesRepository {
        return UserPreferencesRepository(context)
    }

    @Provides
    @Singleton
    fun provideFavoritesRepository(
        @ApplicationContext context: Context,
        favoriteImageDao: FavoriteImageDao,
        preferencesRepository: UserPreferencesRepository,
        okHttpClient: OkHttpClient,
        imageLoader: coil3.ImageLoader,
        mediaProcessor: org.movzx.dibella.util.MediaProcessor,
    ): FavoritesRepository {
        return FavoritesRepository(
            context,
            favoriteImageDao,
            preferencesRepository,
            okHttpClient,
            imageLoader,
            mediaProcessor,
        )
    }
}

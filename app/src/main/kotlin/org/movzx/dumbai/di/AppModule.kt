package org.movzx.dumbai.di

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
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath
import org.movzx.dumbai.api.CivitaiApi
import org.movzx.dumbai.data.AppDatabase
import org.movzx.dumbai.data.FavoriteImageDao
import org.movzx.dumbai.data.FavoritesRepository
import org.movzx.dumbai.data.UserPreferencesRepository
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
    fun provideOkHttpClient(interceptor: org.movzx.dumbai.api.CivitaiInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(interceptor)
            .addInterceptor(org.movzx.dumbai.api.CivitaiThumbnailInterceptor())
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://civitai.com/api/v1/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideCivitaiApi(retrofit: Retrofit): CivitaiApi {
        return retrofit.create(CivitaiApi::class.java)
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
                    .maxSizeBytes(5L * 1024 * 1024 * 1024)
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
    fun provideFeedCacheDao(database: AppDatabase): org.movzx.dumbai.data.FeedCacheDao {
        return database.feedCacheDao()
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
        okHttpClient: OkHttpClient,
    ): FavoritesRepository {
        return FavoritesRepository(context, favoriteImageDao, okHttpClient)
    }
}

package org.movzx.dibella.api

import org.movzx.dibella.model.CivitaiApiResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface CivitaiApi {
    @GET("images")
    suspend fun getImages(
        @Query("limit") limit: Int = 100,
        @Query("nsfw") nsfw: String = "None",
        @Query("sort") sort: String = "Most Reactions",
        @Query("period") period: String = "AllTime",
        @Query("type") type: String = "image",
        @Query("tags") tags: String? = null,
        @Query("withMeta") withMeta: Boolean = false,
        @Query("useIndex") useIndex: Boolean = true,
        @Query("cursor") cursor: String? = null,
    ): CivitaiApiResponse
}

package org.movzx.dumbai.api

import org.movzx.dumbai.model.CivitaiApiResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface CivitaiApi {
    @GET("images")
    suspend fun getImages(
        @Query("apiKey") apiKey: String?,
        @Query("limit") limit: Int = 60,
        @Query("nsfw") nsfw: String = "None",
        @Query("sort") sort: String = "Most Reactions",
        @Query("period") period: String = "AllTime",
        @Query("type") type: String = "image",
        @Query("tags") tags: Int? = null,
        @Query("withMeta") withMeta: Boolean = false,
        @Query("useIndex") useIndex: Boolean = true,
        @Query("cursor") cursor: String? = null
    ): CivitaiApiResponse

    companion object {
        const val BASE_URL = "https://civitai.com/api/v1/"
    }
}

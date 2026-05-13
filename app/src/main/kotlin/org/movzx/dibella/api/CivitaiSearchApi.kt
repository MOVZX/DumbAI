package org.movzx.dibella.api

import org.movzx.dibella.model.SearchRequest
import org.movzx.dibella.model.SearchResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface CivitaiSearchApi {
    @POST("multi-search")
    suspend fun search(
        @Body request: SearchRequest,
        @Header("Authorization") authorization: String,
    ): SearchResponse
}

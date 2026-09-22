package com.example.myapplication.data.api

import com.example.myapplication.data.model.RecordRequest
import com.example.myapplication.data.model.RecordResponse
import com.example.myapplication.data.model.RecordDetailResponse
import com.example.myapplication.data.model.StoryResponse
import com.example.myapplication.data.model.RetrospectRequest
import com.example.myapplication.data.model.RetrospectResponse
import com.example.myapplication.data.model.ThemeSetupRequest
import com.example.myapplication.data.model.ThemeSetupResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * 스토리 관련 API 서비스 인터페이스
 */
interface StoryApiService {
    
    @GET("api/stories")
    suspend fun getStories(
        @Header("Authorization") token: String
    ): Response<StoryResponse>
    
    @POST("api/stories")
    suspend fun createTheme(
        @Header("Authorization") token: String,
        @Body request: ThemeSetupRequest
    ): Response<ThemeSetupResponse>
    
    @POST("api/stories/records")
    suspend fun createRecord(
        @Header("Authorization") token: String,
        @Body request: RecordRequest
    ): Response<RecordResponse>
    
    @POST("api/stories/retrospects")
    suspend fun saveRetrospect(
        @Header("Authorization") token: String,
        @Body request: RetrospectRequest
    ): Response<RetrospectResponse>
    
    @GET("api/records/{record_id}")
    suspend fun getRecordDetail(
        @Header("Authorization") token: String,
        @Path("record_id") recordId: Int
    ): Response<RecordDetailResponse>
}

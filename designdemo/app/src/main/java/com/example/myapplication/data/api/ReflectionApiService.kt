package com.example.myapplication.data.api

import com.example.myapplication.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * 회고 관련 API 서비스 인터페이스
 */
interface ReflectionApiService {
    
    @POST("questions")
    suspend fun generateQuestions(
        @Header("Authorization") token: String,
        @Body request: QuestionRequest
    ): Response<QuestionResponse>
    
    @POST("reflection")
    suspend fun submitReflection(
        @Header("Authorization") token: String,
        @Body request: ReflectionRequest
    ): Response<ReflectionResponse>
    
    @POST("api/stories/retrospects")
    suspend fun saveRetrospect(
        @Header("Authorization") token: String,
        @Body request: RetrospectRequest
    ): Response<RetrospectResponse>
}

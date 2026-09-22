package com.example.myapplication.data.api

import com.example.myapplication.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface LetterApiService {
    @GET("api/letters")
    suspend fun getLetters(
        @Header("Authorization") token: String
    ): Response<LetterResponse>
    
    @POST("api/letters")
    suspend fun saveLetter(
        @Header("Authorization") token: String,
        @Body request: LetterSaveRequest
    ): Response<LetterSaveResponse>
    
    @GET("api/letters/{letter_id}")
    suspend fun getLetterDetail(
        @Header("Authorization") token: String,
        @Path("letter_id") letterId: Int
    ): Response<LetterDetailResponse>
}

data class LetterDetailResponse(
    val status: Int,
    val code: Int,
    val message: String,
    val result: Letter
)

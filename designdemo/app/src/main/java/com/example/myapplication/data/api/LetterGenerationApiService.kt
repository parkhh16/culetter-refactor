package com.example.myapplication.data.api

import com.example.myapplication.data.model.LetterCreateRequest
import com.example.myapplication.data.model.LetterCreateResponse
import retrofit2.Response
import retrofit2.http.*

interface LetterGenerationApiService {
    @POST("letter")
    suspend fun createLetter(
        @Body request: LetterCreateRequest
    ): Response<LetterCreateResponse>
}

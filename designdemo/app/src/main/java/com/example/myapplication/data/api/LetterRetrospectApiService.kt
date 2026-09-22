package com.example.myapplication.data.api

import com.example.myapplication.data.model.LetterRetrospectResponse
import retrofit2.Response
import retrofit2.http.*

interface LetterRetrospectApiService {
    @GET("api/retrospects/all")
    suspend fun getRetrospects(
        @Header("Authorization") token: String
    ): Response<LetterRetrospectResponse>
}

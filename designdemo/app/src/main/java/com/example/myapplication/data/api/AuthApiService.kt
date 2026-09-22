package com.example.myapplication.data.api

import com.example.myapplication.data.model.ApiResponse
import com.example.myapplication.data.model.UserInfo
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthApiService {
    @POST("/api/auth/login")
    suspend fun login(@Header("Authorization") token: String): Response<ApiResponse<UserInfo>>
    
    @GET("/api/users")
    suspend fun searchUserByEmail(
        @Header("Authorization") authorization: String,
        @Query("email") email: String
    ): Response<ApiResponse<UserInfo>>
}

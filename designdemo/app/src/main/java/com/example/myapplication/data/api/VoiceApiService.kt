package com.example.myapplication.data.api

import com.example.myapplication.data.model.VoiceUploadRequest
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

/**
 * 음성 관련 API 서비스 인터페이스
 */
interface VoiceApiService {
    
    @Multipart
    @POST("voice-api/v1/upload_zip")
    suspend fun uploadZip(
        @Header("Authorization") authorization: String,
        @Part("user_id") userId: String,
        @Part("letter_id") letterId: String,
        @Part bundle: MultipartBody.Part
    ): Response<VoiceUploadResponse>
}

data class VoiceUploadResponse(
    val status: Int,
    val code: Int,
    val message: String,
    val result: Any? = null
)


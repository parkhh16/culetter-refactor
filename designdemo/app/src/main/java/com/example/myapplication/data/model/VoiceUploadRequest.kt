package com.example.myapplication.data.model

import okhttp3.MultipartBody
import retrofit2.http.Part

/**
 * 음성 업로드 요청 데이터 모델
 */
data class VoiceUploadRequest(
    @Part("user_id")
    val userId: String,
    
    @Part("letter_id")
    val letterId: String,
    
    @Part
    val bundle: MultipartBody.Part
)


package com.example.myapplication.data.model

import com.google.gson.annotations.SerializedName

/**
 * 테마 설정 요청 데이터 모델
 */
data class ThemeSetupRequest(
    @SerializedName("theme")
    val theme: String,
    
    @SerializedName("color")
    val color: String,
    
    @SerializedName("endedAt")
    val endedAt: String
)

/**
 * 테마 설정 응답 데이터 모델
 */
data class ThemeSetupResponse(
    @SerializedName("status")
    val status: Int,
    
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("result")
    val result: ThemeSetupResult
)

data class ThemeSetupResult(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("theme")
    val theme: String,
    
    @SerializedName("color")
    val color: String,
    
    @SerializedName("startedAt")
    val startedAt: String,
    
    @SerializedName("endedAt")
    val endedAt: String,
    
    @SerializedName("status")
    val status: String,
    
    @SerializedName("createdAt")
    val createdAt: String
)

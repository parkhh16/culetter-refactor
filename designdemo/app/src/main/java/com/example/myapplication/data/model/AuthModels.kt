package com.example.myapplication.data.model

import com.google.gson.annotations.SerializedName

// 백엔드 API 응답 모델들
data class ApiResponse<T>(
    val status: Int,
    val code: Int,
    val message: String,
    val result: T
)

data class LoginRequest(
    @SerializedName("idToken")
    val idToken: String
)

data class UserInfo(
    val id: Int,
    val firebaseUid: String,
    val nickname: String,
    val name: String,
    val email: String,
    val walletAddress: String?,
    val aiVoice: Boolean,
    val isSignUp: Boolean
)

// 로그인 결과 타입
sealed class LoginResult {
    data class Success(val userInfo: UserInfo, val isNewUser: Boolean) : LoginResult()
    data class Error(val message: String) : LoginResult()
    object Loading : LoginResult()
}


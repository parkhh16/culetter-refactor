// app/src/main/java/com/example/myapplication/data/api/UserApiService.kt
package com.example.myapplication.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.PATCH

/** PATCH 요청 바디 */
data class UpdateMeRequest(
    val walletAddress: String
)

/** 유저 관련 API */
interface UserApiService {

    @Headers(
        "Content-Type: application/json",
        "Accept: application/json"
    )
    @PATCH("api/users/me")
    suspend fun updateMeWallet(
        @Header("Authorization") token: String,     // "Bearer <idToken>"
        @Body request: UpdateMeRequest
    ): Response<Unit>                               // 바디가 있다면 적절한 모델로 교체
}

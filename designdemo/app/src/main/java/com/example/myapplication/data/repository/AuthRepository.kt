package com.example.myapplication.data.repository

import android.util.Log
import com.example.myapplication.data.api.AuthApiService
import com.example.myapplication.data.model.LoginResult
import com.example.myapplication.data.model.UserInfo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApiService: AuthApiService,
    private val firebaseAuth: FirebaseAuth
) {
    suspend fun signInWithGoogle(idToken: String): LoginResult {
        return try {
            // 1) Firebase 인증
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: return LoginResult.Error("Firebase 인증 실패")

            // 2) Firebase ID token -> 백엔드 로그인
            val token = firebaseUser.getIdToken(false).await().token
                ?: return LoginResult.Error("Firebase 토큰 획득 실패")
            Log.d("FirebaseToken", "Firebase Token ID: $token")

            val response = authApiService.login("Bearer $token")
            if (!response.isSuccessful) {
                return LoginResult.Error("서버 오류: ${response.code()}")
            }

            val apiResponse = response.body()
                ?: return LoginResult.Error("서버 응답이 비어있습니다")

            val isNewUser = (apiResponse.code == 1001) // 서버 규약: 1001 = 회원가입 성공
            LoginResult.Success(apiResponse.result, isNewUser)
        } catch (e: Exception) {
            LoginResult.Error("로그인 실패: ${e.message}")
        }
    }

    fun getCurrentUser() = firebaseAuth.currentUser
    fun signOut() = firebaseAuth.signOut()

    /** 현재 Firebase 사용자 기준 Bearer 토큰 생성 */
    private suspend fun getBearer(): String? {
        val user = firebaseAuth.currentUser ?: return null
        val token = user.getIdToken(false).await().token ?: return null
        return "Bearer $token"
    }

    /** 현재 Firebase 사용자 이메일 */
    private fun getEmail(): String? = firebaseAuth.currentUser?.email

    /** 서버에서 이메일로 사용자 조회 (code 1000=성공, 1003=없음) */
    suspend fun fetchUserByEmail(): Pair<UserInfo?, Int?> {
        val bearer = getBearer() ?: return Pair(null, null)
        val email = getEmail() ?: return Pair(null, null)

        return try {
            val resp = authApiService.searchUserByEmail(bearer, email)
            if (!resp.isSuccessful) return Pair(null, resp.code())
            val body = resp.body() ?: return Pair(null, null)
            // body.code == 1000 이면 result 유효, 1003이면 없음
            Pair(body.result, body.code)
        } catch (e: Exception) {
            Pair(null, null)
        }
    }

    /** 지갑 연동 여부 (walletAddress 가 null/blank 아니면 true) */
    suspend fun hasWallet(): Boolean? {
        val (user, code) = fetchUserByEmail()
        return when (code) {
            1000 -> !user?.walletAddress.isNullOrBlank()
            1003 -> false // 일치하는 회원 없음
            else -> null  // 판단불가(네트워크/서버 오류) → null로 둬서 UI에서 로딩 유지
        }
    }
}

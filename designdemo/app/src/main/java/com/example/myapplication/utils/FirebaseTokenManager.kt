package com.example.myapplication.utils

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

/**
 * Firebase 토큰 관리 유틸리티
 */
class FirebaseTokenManager {
    
    private val auth = FirebaseAuth.getInstance()
    
    /**
     * 현재 사용자의 Firebase ID 토큰을 가져옵니다.
     * @return Firebase ID 토큰 또는 null
     */
    suspend fun getCurrentUserToken(): String? {
        return try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val idTokenResult = currentUser.getIdToken(false).await()
                idTokenResult.token
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 현재 사용자가 로그인되어 있는지 확인합니다.
     * @return 로그인 상태
     */
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }
    
    /**
     * 현재 사용자의 Firebase UID를 가져옵니다.
     * @return Firebase UID 또는 null
     */
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
}

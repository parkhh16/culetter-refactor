// app/src/main/java/com/example/myapplication/data/repository/UserRepository.kt
package com.example.myapplication.data.repository

import android.util.Log
import com.example.myapplication.data.api.UpdateMeRequest
import com.example.myapplication.data.api.UserApiService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userApiService: UserApiService,
    private val firebaseAuth: FirebaseAuth
) {

    /**
     * 메타마스크 지갑 주소를 내 정보에 PATCH.
     * @return 성공 시 true, 실패 시 false
     */
    suspend fun updateWalletAddress(walletAddress: String): Boolean {
        return try {
            val firebaseUser = firebaseAuth.currentUser
                ?: return false.also { Log.e("UserRepository", "No current Firebase user") }

            val idToken = firebaseUser.getIdToken(false).await().token
                ?: return false.also { Log.e("UserRepository", "Failed to get Firebase ID token") }

            val bearer = "Bearer $idToken"
            val resp = userApiService.updateMeWallet(
                token = bearer,
                request = UpdateMeRequest(walletAddress = walletAddress)
            )

            if (resp.isSuccessful) {
                true
            } else {
                Log.e("UserRepository", "updateWalletAddress failed: ${resp.code()} ${resp.message()}")
                false
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "updateWalletAddress error", e)
            false
        }
    }
}

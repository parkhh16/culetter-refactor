package com.example.myapplication

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.myapplication.data.service.WearableDataService
import com.example.myapplication.ui.App
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
/**
 * 앱 엔트리 포인트.
 * UI는 전부 ui/App.kt의 App()에서 구성합니다.
 */
class MainActivity : ComponentActivity() {
    private lateinit var wearableDataService: WearableDataService
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))
        // Firebase 초기화
        FirebaseApp.initializeApp(this)
        
        // Wearable Data Service 초기화
        wearableDataService = WearableDataService(this)
        
        // 앱 시작 시 로그인된 사용자의 Firebase 토큰 ID 확인
        checkFirebaseToken()
        
        setContent { App() }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Wearable Data Service 정리
        wearableDataService.cleanup()
    }
    
    private fun checkFirebaseToken() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    val idTokenResult = currentUser.getIdToken(false).await()
                    val token = idTokenResult.token
                    
                    if (token != null) {
                        Log.d("FirebaseToken", "앱 시작 시 Firebase Token ID: $token")
                    } else {
                        Log.w("FirebaseToken", "앱 시작 시 Firebase 토큰 획득 실패")
                    }
                } else {
                    Log.d("FirebaseToken", "앱 시작 시 로그인된 사용자 없음")
                }
            } catch (e: Exception) {
                Log.e("FirebaseToken", "앱 시작 시 Firebase 토큰 확인 중 오류: ${e.message}")
            }
        }
    }
}

package com.example.myapplication

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import com.example.myapplication.data.Recorder
import com.example.myapplication.data.WearMessageRepository
import com.example.myapplication.ui.LiveRecordingScreen
import com.example.myapplication.wake.WakeWordService

class MainActivity : ComponentActivity() {

    companion object { private const val TAG = "WearMainActivity" }

    // Android 13+ 알림 권한 런처
    private val notifPerm = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> Log.d(TAG, "POST_NOTIFICATIONS granted: $granted") }

    // 마이크 권한 런처
    private val micPerm = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Log.d(TAG, "RECORD_AUDIO granted: $granted")
        if (granted) startWakeService()
    }

    @get:VisibleForTesting
    internal val recorder by lazy { Recorder(this) }

    @get:VisibleForTesting
    internal val repo by lazy { WearMessageRepository(this) }

    // 앱 포그라운드일 때 인앱 토스트로도 알려주기
    private val wakeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == com.example.myapplication.wake.WakeWordService.ACTION_WAKE_DETECTED) {
                Toast.makeText(this@MainActivity, "웨이크워드 감지됨", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Android 13+ 알림 권한
        if (Build.VERSION.SDK_INT >= 33) {
            val hasNotif = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasNotif) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // 마이크 권한 및 서비스 시작
        val hasMic = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasMic) startWakeService() else micPerm.launch(Manifest.permission.RECORD_AUDIO)

        // 알림 탭으로 냉시작 진입
        if (intent?.getBooleanExtra("fromWake", false) == true) {
            Log.d(TAG, "Launched from wake notification (cold start)")
        }

        // 실행 중 알림 탭 재진입 처리
        addOnNewIntentListener { newIntent ->
            if (newIntent.getBooleanExtra("fromWake", false)) {
                Log.d(TAG, "Launched from wake notification (while running)")
            }
        }

        setContent {
            LiveRecordingScreen(
                recorder = recorder,
                repo = repo,
                onFinished = { ok -> Log.d(TAG, "sendVoice result: $ok") },
                onCancel   = { Log.d(TAG, "recording canceled") }
            )
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(com.example.myapplication.wake.WakeWordService.ACTION_WAKE_DETECTED)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(wakeReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(wakeReceiver, filter)
        }
    }

    override fun onStop() {
        runCatching { unregisterReceiver(wakeReceiver) }
        super.onStop()
    }

    private fun startWakeService() {
        ContextCompat.startForegroundService(
            this, Intent(this, WakeWordService::class.java)
        )
    }
}

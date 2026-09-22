package com.example.myapplication.wake

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.myapplication.MainActivity
import ai.picovoice.porcupine.PorcupineManager
import com.example.myapplication.R
class WakeWordService : Service() {

    companion object {
        private const val TAG = "WakeWordService"

        // 포그라운드(상시 듣기) 알림 채널
        private const val CHANNEL_ID = "wake_listening"

        // 감지 알림(HUN) 채널 — 🔁 새 ID로 만들면 채널 설정(소리/팝업) 초기화됨
        private const val WAKE_CHANNEL_ID = "wake_alert_v5"  // ⬅️ v3 -> v5 로 변경

        private const val NOTI_ID = 7812
        private const val WAKE_NOTI_ID = 7813

        private const val ACTION_STOP = "STOP"

        // 앱 포그라운드일 때 인앱 알림(브로드캐스트)
        const val ACTION_WAKE_DETECTED = "com.example.myapplication.WAKE_DETECTED"
    }

    // ⛔ 민감정보: 리포지토리에 커밋 금지!
    private val ACCESS_KEY: String = "5VmDiGV/HM9Nl4xsuB5Ayxt1ITYG+KiBpY7ejqfQGYsU590Q6/vksg=="

    private var porcupineManager: PorcupineManager? = null

    override fun onCreate() {
        super.onCreate()
        createOrReplaceChannels() // 채널 새로 생성(소리/팝업 보장)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        // 포그라운드 서비스 시작(마이크 타입)
        ServiceCompat.startForeground(
            this,
            NOTI_ID,
            buildListeningNotification(),
            if (Build.VERSION.SDK_INT >= 34)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            else 0
        )

        if (porcupineManager == null) {
            try {
                // ▶ 한국어 커스텀 키워드 사용
                val keywordPath = assetFilePath(this, "porcupine/레티야_ko_android_v3_0_0.ppn")
                val modelPath   = assetFilePath(this, "porcupine/porcupine_params_ko.pv")

                porcupineManager = PorcupineManager.Builder()
                    .setAccessKey(ACCESS_KEY)
                    .setKeywordPaths(arrayOf(keywordPath))
                    .setModelPath(modelPath)
                    .setSensitivity(0.65f)
                    .build(applicationContext) {
                        Log.d(TAG, "Wake detected (KO)")
                        vibrateWake()
                        postWakeNotification()
                        sendBroadcast(Intent(ACTION_WAKE_DETECTED).setPackage(packageName))
                    }

            } catch (e: Exception) {
                Log.e(TAG, "Porcupine init failed", e)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        porcupineManager?.start()
        return START_STICKY
    }

    override fun onDestroy() {
        runCatching { porcupineManager?.stop() }
        runCatching { porcupineManager?.delete() }
        porcupineManager = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ---------- Notifications & Vibrations ----------

    /**
     * 채널은 생성 시 설정이 고정됨.
     * 소리/팝업 보장을 위해 항상 '삭제 후 새 ID로 재생성' 전략 사용.
     */
    private fun createOrReplaceChannels() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 상시 듣기 채널: 최소 노이즈
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                "Wake Word Listening",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            nm.createNotificationChannel(ch)
        }

        // 감지 채널: 소리 + 진동 + HIGH
        // 항상 삭제 후 재생성(기존 무음 설정을 확실히 제거)
        runCatching { nm.deleteNotificationChannel(WAKE_CHANNEL_ID) }

        val soundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val wake = NotificationChannel(
            WAKE_CHANNEL_ID,
            "Wake Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 220, 120, 220)
            enableLights(true)
            setSound(soundUri, attrs) // ✅ 채널 레벨 소리 지정
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(wake)
    }

    private fun buildListeningNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra("fromWake", true)
        val piOpen = PendingIntent.getActivity(
            this, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, WakeWordService::class.java).apply { action = ACTION_STOP }
        val piStop = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.recording_active)
            .setContentTitle("레티야")
            .setContentText("'레티야'를 불러주세요!")
            .setContentIntent(piOpen)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN) // pre-26
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(Notification.VISIBILITY_SECRET)
            .addAction(0, "중지", piStop)
            .build()
    }

    private fun postWakeNotification() {
        // 채널(소리/팝업) 보장
        createOrReplaceChannels()

        Handler(Looper.getMainLooper()).post {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val openIntent = Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra("fromWake", true)
            val piOpen = PendingIntent.getActivity(
                this, 100, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val soundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val builder = NotificationCompat.Builder(this, WAKE_CHANNEL_ID)
                .setSmallIcon(R.drawable.recording_active)
                .setContentTitle("녹음하기")
                .setContentText("앱을 열어 녹음해주세요!")
                .setAutoCancel(true)
                .setOnlyAlertOnce(false)
                .setCategory(Notification.CATEGORY_EVENT)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentIntent(piOpen)
                // 🟢 HUN(헤드업) 보장: 풀스크린 인텐트 (일부 OEM에서 필요)
                .setFullScreenIntent(piOpen, true)
                // pre-26 기기 호환(채널이 없는 경우에만 의미)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVibrate(longArrayOf(0, 180, 120, 180))
                .apply {
                    if (Build.VERSION.SDK_INT < 26) {
                        setSound(soundUri) // O 미만에서는 빌더 레벨 소리
                        setDefaults(Notification.DEFAULT_LIGHTS)
                    } else {
                        // O 이상에서는 채널 사운드가 우선
                        setDefaults(Notification.DEFAULT_LIGHTS)
                    }
                }

            nm.notify(WAKE_NOTI_ID, builder.build())
        }
    }

    // 화면 켜짐/꺼짐과 무관하게 확실한 진동
    private fun vibrateWake() {
        try {
            if (Build.VERSION.SDK_INT >= 31) {
                val vm = getSystemService(VibratorManager::class.java)
                val vib = vm?.defaultVibrator ?: return
                val effect = VibrationEffect.createWaveform(
                    longArrayOf(0, 220, 120, 220),
                    intArrayOf(0, 255, 0, 255),
                    -1
                )
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM) // 또는 USAGE_NOTIFICATION_EVENT
                    .build()
                vib.vibrate(effect, attrs)
            } else {
                val vib = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= 26) {
                    vib.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 220, 120, 220), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(longArrayOf(0, 220, 120, 220), -1)
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "vibrateWake failed: ${t.message}")
        }
    }

    // assets -> 내부저장소 복사 후 절대경로 반환
    private fun assetFilePath(context: Context, assetPath: String): String {
        val outFile = java.io.File(context.filesDir, assetPath) // filesDir/porcupine/...
        if (!outFile.exists()) {
            outFile.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input ->
                java.io.FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return outFile.absolutePath
    }
}

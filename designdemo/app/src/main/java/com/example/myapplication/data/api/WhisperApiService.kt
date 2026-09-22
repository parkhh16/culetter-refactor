package com.example.myapplication.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException

/**
 * Whisper API 서비스 구현체
 * GMS API를 통해 음성을 텍스트로 변환
 */
class WhisperApiService {
    companion object {
        private const val TAG = "WhisperApiService"
        private const val GMS_BASE_URL = "https://gms.ssafy.io/gmsapi/api.openai.com/v1/audio/transcriptions"
        private const val API_KEY = "S13P22A209-75339ea7-5d72-4869-8370-0a00b7853e3f"
    }
    
    /**
     * 오디오 파일을 텍스트로 변환
     * @param audioFile 오디오 파일 경로
     * @return 변환된 텍스트
     */
    suspend fun transcribeAudio(audioFile: File): Result<String> {
        return withContext(Dispatchers.IO) {
            val maxRetries = 3
            var lastException: Exception? = null
            
            repeat(maxRetries) { attempt ->
                try {
                    Log.d(TAG, "Starting Whisper transcription for file: ${audioFile.absolutePath} (attempt ${attempt + 1}/$maxRetries)")
                    
                    val client = OkHttpClient.Builder()
                        .protocols(listOf(Protocol.HTTP_1_1))
                        .retryOnConnectionFailure(true)
                        .callTimeout(java.time.Duration.ofSeconds(120)) // 2분으로 증가
                        .connectTimeout(java.time.Duration.ofSeconds(30)) // 연결 타임아웃
                        .readTimeout(java.time.Duration.ofSeconds(120)) // 읽기 타임아웃
                        .writeTimeout(java.time.Duration.ofSeconds(60)) // 쓰기 타임아웃
                        .build()

                    val body = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", audioFile.name, audioFile.asRequestBody("audio/m4a".toMediaType()))
                        .addFormDataPart("model", "whisper-1")
                        .addFormDataPart("language", "ko")
                        .build()

                    val request = Request.Builder()
                        .url(GMS_BASE_URL)
                        .addHeader("Authorization", "Bearer $API_KEY")
                        .post(body)
                        .build()

                    Log.d(TAG, "Sending request to: $GMS_BASE_URL")
                    
                    client.newCall(request).execute().use { response ->
                        val bodyString = response.body?.string().orEmpty()
                        Log.d(TAG, "Response code: ${response.code}")
                        Log.d(TAG, "Response body: $bodyString")
                        
                        if (!response.isSuccessful) {
                            Log.e(TAG, "Whisper API error: ${response.code} - $bodyString")
                            return@withContext Result.failure(IOException("Whisper HTTP ${response.code}: $bodyString"))
                        }
                        
                        val jsonResponse = JSONObject(bodyString)
                        val text = jsonResponse.optString("text", "")
                        
                        if (text.isNotEmpty()) {
                            Log.d(TAG, "Transcription successful: $text")
                            return@withContext Result.success(text.trim())
                        } else {
                            Log.e(TAG, "Empty transcription result")
                            return@withContext Result.failure(Exception("음성 인식 결과가 비어있습니다."))
                        }
                    }
                } catch (e: Exception) {
                    lastException = e
                    Log.e(TAG, "Transcription attempt ${attempt + 1} failed", e)
                    
                    if (attempt < maxRetries - 1) {
                        val delayMs = (attempt + 1) * 2000L // 2초, 4초, 6초 대기
                        Log.d(TAG, "Retrying in ${delayMs}ms...")
                        kotlinx.coroutines.delay(delayMs)
                    }
                }
            }
            
            Log.e(TAG, "All transcription attempts failed")
            Result.failure(lastException ?: Exception("음성 인식에 실패했습니다."))
        }
    }
}

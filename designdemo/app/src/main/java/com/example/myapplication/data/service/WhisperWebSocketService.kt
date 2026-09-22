package com.example.myapplication.data.service

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Whisper-1 모델과의 웹소켓 통신 서비스
 */
class WhisperWebSocketService {
    
    private var webSocket: WebSocket? = null
    private var client: OkHttpClient? = null
    
    private val _connectionState = MutableStateFlow(WebSocketState.DISCONNECTED)
    val connectionState: StateFlow<WebSocketState> = _connectionState.asStateFlow()
    
    private val _transcriptionResult = MutableStateFlow<String?>(null)
    val transcriptionResult: StateFlow<String?> = _transcriptionResult.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    /**
     * 웹소켓 연결 시작
     */
    fun connect(apiKey: String) {
        try {
            client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .writeTimeout(0, TimeUnit.SECONDS)
                .build()
            
            val request = Request.Builder()
                .url("wss://api.openai.com/v1/audio/transcriptions")
                .addHeader("Authorization", "Bearer $apiKey")
                .build()
            
            webSocket = client?.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d("WhisperWebSocket", "웹소켓 연결됨")
                    _connectionState.value = WebSocketState.CONNECTED
                    _error.value = null
                }
                
                override fun onMessage(webSocket: WebSocket, text: String) {
                    Log.d("WhisperWebSocket", "메시지 수신: $text")
                    try {
                        val json = JSONObject(text)
                        val transcription = json.optString("text", "")
                        if (transcription.isNotEmpty()) {
                            _transcriptionResult.value = transcription
                        }
                    } catch (e: Exception) {
                        Log.e("WhisperWebSocket", "JSON 파싱 오류", e)
                        _error.value = "응답 파싱 오류: ${e.message}"
                    }
                }
                
                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    Log.d("WhisperWebSocket", "바이너리 메시지 수신")
                }
                
                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d("WhisperWebSocket", "웹소켓 연결 종료 중: $code - $reason")
                    _connectionState.value = WebSocketState.DISCONNECTING
                }
                
                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d("WhisperWebSocket", "웹소켓 연결 종료됨: $code - $reason")
                    _connectionState.value = WebSocketState.DISCONNECTED
                }
                
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e("WhisperWebSocket", "웹소켓 오류", t)
                    _connectionState.value = WebSocketState.DISCONNECTED
                    _error.value = "연결 오류: ${t.message}"
                }
            })
            
        } catch (e: Exception) {
            Log.e("WhisperWebSocket", "웹소켓 연결 실패", e)
            _error.value = "연결 실패: ${e.message}"
        }
    }
    
    /**
     * 오디오 데이터 전송
     */
    fun sendAudio(audioData: ByteArray) {
        webSocket?.let { ws ->
            if (_connectionState.value == WebSocketState.CONNECTED) {
                try {
                    // Whisper API 형식에 맞게 오디오 데이터 전송
                    val requestBody = createAudioRequestBody(audioData)
                    ws.send(requestBody)
                } catch (e: Exception) {
                    Log.e("WhisperWebSocket", "오디오 전송 오류", e)
                    _error.value = "오디오 전송 오류: ${e.message}"
                }
            } else {
                _error.value = "웹소켓이 연결되지 않음"
            }
        }
    }
    
    /**
     * 오디오 요청 본문 생성
     */
    private fun createAudioRequestBody(audioData: ByteArray): String {
        val json = JSONObject().apply {
            put("model", "whisper-1")
            put("response_format", "json")
            put("language", "ko") // 한국어 설정
        }
        return json.toString()
    }
    
    /**
     * 웹소켓 연결 종료
     */
    fun disconnect() {
        webSocket?.close(1000, "정상 종료")
        client?.dispatcher?.executorService?.shutdown()
        _connectionState.value = WebSocketState.DISCONNECTED
    }
    
    /**
     * 결과 초기화
     */
    fun clearResult() {
        _transcriptionResult.value = null
        _error.value = null
    }
}

enum class WebSocketState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING
}

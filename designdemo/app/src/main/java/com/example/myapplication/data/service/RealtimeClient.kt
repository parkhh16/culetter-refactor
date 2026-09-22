package com.example.myapplication.data.service

import android.util.Base64
import android.util.Log
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Realtime WS (서버 VAD 신호기)
 *
 * - 마이크 PCM16 모노 프레임을 Base64로 인코딩해 input_audio_buffer.append 로 보냄
 * - 서버 VAD가 문장 종료를 감지하면 'final' 계열 이벤트를 수신 → onVadFinal() 콜백
 * - "시끄러운 곳" 프로파일: threshold=0.55, silence_duration_ms=1100 (≈ 900–1200 권장 범위의 중앙값)
 *
 * 사용 전제:
 *  - endpointWss 에 intent=transcription, server_vad=true, input_audio_format=pcm16,
 *    input_audio_transcription.model=whisper-1 (옵션) 등을 포함해 두는 것을 권장.
 *    예) wss://api.openai.com/v1/realtime?model=gpt-4o-realtime-preview
 *        &intent=transcription&server_vad=true&input_audio_format=pcm16
 *        &input_audio_transcription.model=whisper-1&input_audio_transcription.language=ko
 */
class RealtimeClient(
    private val endpointWss: String,
    private val apiKey: String,
    private val listener: Listener,
    // ▼ 시끄러운 환경 기본값 (필요시 생성자에서 덮어쓰기 가능)
    private val vadThreshold: Double = 0.75,
    private val vadSilenceMs: Int = 400
) {
    interface Listener {
        fun onConnected()
        fun onError(message: String)
        fun onVadFinal() // 문장 끝 감지(다음 질문으로 진행 신호)
    }

    private var ws: WebSocket? = null
    private val http = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    /** WebSocket 연결 */
    fun connect() {
        val req = Request.Builder()
            .url(endpointWss)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("OpenAI-Beta", "realtime=v1")
            .build()
        ws = http.newWebSocket(req, socketListener)
    }

    /** 종료 */
    fun close() {
        try { ws?.close(1000, "bye") } catch (_: Throwable) {}
        ws = null
    }

    /** 연결 여부 */
    fun isConnected(): Boolean = ws != null

    /** PCM16 모노 프레임을 Base64로 보내기 (바이트 배열 버전) */
    fun sendInputAudioChunk(pcm16: ByteArray, offset: Int = 0, length: Int = pcm16.size) {
        if (length <= 0) return
        val slice = if (offset == 0 && length == pcm16.size) pcm16 else pcm16.copyOfRange(offset, offset + length)
        val b64 = Base64.encodeToString(slice, Base64.NO_WRAP)
        sendJson(JSONObject().put("type", "input_audio_buffer.append").put("audio", b64))
    }

    /** 이미 Base64 인코딩된 오디오 청크를 보낼 때 사용 */
    fun sendInputAudioChunk(b64: String) {
        sendJson(JSONObject().put("type", "input_audio_buffer.append").put("audio", b64))
    }

    /** 세션의 서버 VAD 파라미터 갱신 (연결된 상태에서 호출 가능) */
    fun updateTurnDetection(
        threshold: Double = vadThreshold,
        silenceDurationMs: Int = vadSilenceMs
    ) {
        val td = JSONObject()
            .put("type", "server_vad")
            .put("threshold", threshold)
            .put("silence_duration_ms", silenceDurationMs)

        val payload = JSONObject()
            .put("type", "session.update")
            .put("session", JSONObject().put("turn_detection", td))

        sendJson(payload)
    }

    private fun sendJson(o: JSONObject) {
        try {
            ws?.send(o.toString())
        } catch (e: Exception) {
            Log.e("RealtimeClient", "sendJson error: ${e.message}")
        }
    }

    private val socketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            listener.onConnected()
            // 접속 즉시 "시끄러운 곳" 프로파일 적용
            updateTurnDetection(vadThreshold, vadSilenceMs)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            try { handleEvent(JSONObject(text)) }
            catch (e: Exception) {
                Log.e("RealtimeClient", "parse error: ${e.message}\n$text")
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) { /* no-op */ }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            listener.onError(t.message ?: "websocket failure")
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(1000, null)
        }
    }

    /** 수신 이벤트 처리 */
    private fun handleEvent(ev: JSONObject) {
        when (ev.optString("type", "")) {
            // 최신/구형 이벤트 이름 모두 대응: 서버 VAD가 "문장 끝"으로 판단했을 때
            "conversation.item.input_audio_transcription.completed",
            "input_audio.transcription.completed",
            "input_audio.transcription.final" -> {
                listener.onVadFinal()
            }

            // 에러류
            "response.error", "session.error", "error" -> {
                val msg = ev.optJSONObject("error")?.optString("message")
                    ?: ev.optString("message", "unknown error")
                listener.onError(msg)
            }
        }
    }
}
